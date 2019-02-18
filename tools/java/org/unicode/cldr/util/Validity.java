package org.unicode.cldr.util;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.util.StandardCodes.LstrType;

import com.google.common.base.Splitter;

public class Validity {

    public enum Status {
        regular, special, // for languages only (special codes like mul)
        macroregion, // regions only (from M.49)
        deprecated, reserved, private_use,  // for clients of cldr with prior agreements
        unknown, invalid; //  (anything else)
    }

    private static final ConcurrentHashMap<String, Validity> cache = new ConcurrentHashMap<>();

    private final Map<LstrType, Map<Status, Set<String>>> typeToStatusToCodes;
    private final Map<LstrType, Map<String, Status>> typeToCodeToStatus;

    public static Validity getInstance() {
        return getInstance(CLDRPaths.VALIDITY_DIRECTORY);
    }

    public static Validity getInstance(String validityDirectory) {
        Validity result = cache.get(validityDirectory);
        if (result == null) {
            final Validity value = new Validity(validityDirectory);
            result = cache.putIfAbsent(validityDirectory, value);
            if (result == null) {
                result = value;
            }
        }
        return result;
    }

    private Validity(String validityDirectory) {
        Splitter space = Splitter.on(PatternCache.get("\\s+")).trimResults().omitEmptyStrings();
        Map<LstrType, Map<Status, Set<String>>> data = new EnumMap<>(LstrType.class);
        Map<LstrType, Map<String, Status>> codeToStatus = new EnumMap<>(LstrType.class);
        final String basePath = validityDirectory;
        for (String file : new File(basePath).list()) {
            if (!file.endsWith(".xml")) {
                continue;
            }
            LstrType type = null;
            try {
                type = LstrType.valueOf(file.substring(0, file.length() - 4));
            } catch (Exception e) {
                continue;
            }
            List<Pair<String, String>> lineData = new ArrayList<>();
            Map<Status, Set<String>> submap = data.get(type);
            if (submap == null) {
                data.put(type, submap = new EnumMap<>(Status.class));
            }
            Map<String, Status> subCodeToStatus = codeToStatus.get(type);
            if (subCodeToStatus == null) {
                codeToStatus.put(type, subCodeToStatus = new TreeMap<>());
            }

            XMLFileReader.loadPathValues(basePath + file, lineData, true);
            for (Pair<String, String> item : lineData) {
                XPathParts parts = XPathParts.getFrozenInstance(item.getFirst());
                if (!"id".equals(parts.getElement(-1))) {
                    continue;
                }
                LstrType typeAttr = LstrType.valueOf(parts.getAttributeValue(-1, "type"));
                if (typeAttr != type) {
                    throw new IllegalArgumentException("Corrupt value for " + type);
                }
                Status subtypeAttr = Status.valueOf(parts.getAttributeValue(-1, "idStatus"));
                Set<String> set = submap.get(subtypeAttr);
                if (set == null) {
                    submap.put(subtypeAttr, set = new LinkedHashSet<>());
                }
                for (String value : space.split(item.getSecond())) {
                    if (type == LstrType.subdivision) {
                        value = value.toLowerCase(Locale.ROOT).replace("-", "");
                    }
                    int dashPos = value.indexOf('~');
                    if (dashPos < 0) {
                        set.add(value);
                    } else {
                        StringRange.expand(value.substring(0, dashPos), value.substring(dashPos + 1), set);
                    }
                }
                for (String code : set) {
                    subCodeToStatus.put(code, subtypeAttr);
                }
            }
        }
        typeToStatusToCodes = CldrUtility.protectCollectionX(data);
        typeToCodeToStatus = CldrUtility.protectCollectionX(codeToStatus);
    }

    /**
     * 
     * @deprecated Use {@link #getStatusToCodes(LstrType)}
     */
    public Map<LstrType, Map<Status, Set<String>>> getData() {
        return typeToStatusToCodes;
    }

    public Map<Status, Set<String>> getStatusToCodes(LstrType type) {
        return typeToStatusToCodes.get(type);
    }

    public Map<String, Status> getCodeToStatus(LstrType type) {
        return typeToCodeToStatus.get(type);
    }
}
