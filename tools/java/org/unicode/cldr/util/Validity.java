package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.unicode.cldr.util.StandardCodes.LstrType;

import com.google.common.base.Splitter;

public class Validity {
    
    public enum Status {
        regular,
        unknown,
        macroregion, // regions only (from M.49)
        deprecated,
        private_use, // (for clients of cldr)
        invalid, //  (anything else)
    }

    static final ConcurrentHashMap<String, Validity> cache = new ConcurrentHashMap<>();

    private final Map<LstrType, Map<Validity.Status, Set<String>>> mappedData;

    public static Validity getInstance(String commonDirectory) {
        Validity result = cache.get(commonDirectory);
        if (result == null) {
            final Validity value = new Validity(commonDirectory);
            result = cache.putIfAbsent(commonDirectory, value);
            if (result == null) {
                result = value;
            }
        }
        return result;
    }

    private Validity(String commonDirectory) {
        Splitter space = Splitter.on(Pattern.compile("\\s+")).trimResults().omitEmptyStrings();
        Map<LstrType, Map<Validity.Status, Set<String>>> data = new EnumMap<>(LstrType.class);
        for (LstrType type : LstrType.values()) {
            if (!type.inCldr) {
                continue;
            }
            List<Pair<String,String>> lineData = new ArrayList<>();
            Map<Validity.Status, Set<String>> submap = data.get(type);
            if (submap == null) {
                data.put(type, submap = new EnumMap<>(Validity.Status.class));
            }

            XMLFileReader.loadPathValues(commonDirectory + "validity/" + type + ".xml", lineData, true);
            for (Pair<String, String> item : lineData) {
                XPathParts parts = XPathParts.getFrozenInstance(item.getFirst());
                if (!"id".equals(parts.getElement(-1))) {
                    continue;
                }
                LstrType typeAttr = LstrType.valueOf(parts.getAttributeValue(-1, "type"));
                if (typeAttr != type) {
                    throw new IllegalArgumentException("Corrupt value for " + type);
                }
                Validity.Status subtypeAttr = Validity.Status.valueOf(parts.getAttributeValue(-1, "idStatus"));
                Set<String> set = submap.get(subtypeAttr);
                if (set == null) {
                    submap.put(subtypeAttr, set = new LinkedHashSet<>());
                }
                for (String value : space.split(item.getSecond())) {
                    int dashPos = value.indexOf('-');
                    if (dashPos < 0) {
                        set.add(value);
                    } else {
                        StringRange.expand(value.substring(0,dashPos), value.substring(dashPos+1), set);
                    }
                }
            }
        }
        mappedData = CldrUtility.protectCollectionX(data);
    }

    public Map<LstrType, Map<Validity.Status, Set<String>>> getData() {
        return mappedData;
    }
}
