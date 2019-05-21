package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.StringRange;
import org.unicode.cldr.util.StringRange.Adder;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.ICUUncheckedIOException;

@CLDRTool(
    alias = "generate-validity-data",
    url = "http://cldr.unicode.org/development/updating-codes/update-validity-xml")
public class GenerateValidityXml {

    private static final Validity VALIDITY = Validity.getInstance();
    private static Validity OLD_VALIDITY = Validity.getInstance(CLDRPaths.LAST_RELEASE_DIRECTORY + "common/validity/");

    private static final Map<LstrType, Map<String, Map<LstrField, String>>> LSTREG = StandardCodes.getEnumLstreg();
    private static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();

    private static class MyAdder implements Adder {
        Appendable target;
        boolean twoCodePoints = false;
        long lastCodePoint = -1;

        @Override
        public void add(String start, String end) {
            try {
                long firstCodePoint = start.codePointAt(0);
                if (twoCodePoints) {
                    firstCodePoint <<= 22;
                    firstCodePoint |= start.codePointAt(1);
                }
                if (firstCodePoint == lastCodePoint) {
                    target.append(' ');
                } else {
                    target.append("\n\t\t\t");
                }
                target.append(start);
                if (end != null) {
                    target.append('~').append(end);
                }
                lastCodePoint = firstCodePoint;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void reset(boolean b) {
            lastCodePoint = -1;
            twoCodePoints = b;
        }
    }

    static Set<String> containment = SDI.getContainers();
    static Map<String, Map<LstrField, String>> codeToData = LSTREG.get(LstrType.region);

    static class Info {
        String mainComment;
        //private Relation<Validity.Status, String> statusMap = Relation.of(new EnumMap<Validity.Status, Set<String>>(Validity.Status.class), TreeSet.class);
        Map<String, Validity.Status> codeToStatus = new TreeMap<>();
        Map<Validity.Status, String> statusComment = new EnumMap<>(Status.class);
        Set<String> newCodes = new TreeSet<>();

        static Map<String, Info> types = new LinkedHashMap<>();

        static Info getInfo(String myType) {
            Info info = types.get(myType);
            if (info == null) {
                types.put(myType, info = new Info());
            }
            return info;
        }
        public SetMultimap<Status, String> getStatusMap() {
            TreeMultimap<Status, String> result = TreeMultimap.create();
            Multimaps.invertFrom(Multimaps.forMap(codeToStatus), result);
            return ImmutableSetMultimap.copyOf(result);
        }
        public void put(String key, Status value) {
            codeToStatus.put(key, value);
        }
        public void remove(String key, Status value) {
            codeToStatus.remove(key, value);
        }
        public void clear() {
            codeToStatus.clear();
        }
        public Set<Entry<String, Status>> entrySet() {
            return codeToStatus.entrySet();
        }
        public Status get(String key) {
            return codeToStatus.get(key);
        }
        public void putBest(String currency, Status newStatus) {
            Status oldStatus = get(currency);
            if (oldStatus == null || newStatus.compareTo(oldStatus) < 0) {
                put(currency, newStatus);
            }            
        }
    }

    static final Map<String, Info> types = Info.types;

    public static void main(String[] args) throws IOException {

        doLstr(types);
        doSubdivisions(types);
        doCurrency(types);
        // write file
        MyAdder adder = new MyAdder();
        for (Entry<String, Info> entry : types.entrySet()) {
            String type = entry.getKey();
            final Info info = entry.getValue();
            Multimap<Status, String> subtypeMap = info.getStatusMap();
            try (PrintWriter output = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "validity/" + type + ".xml")) {
                adder.target = output;
                output.append(DtdType.supplementalData.header(MethodHandles.lookup().lookupClass())
                    + "\t<version number=\"$Revision" /*hack to stop SVN changing this*/ + "$\"/>\n"
                    + "\t<idValidity>\n");
                for (Entry<Status, Collection<String>> entry2 : subtypeMap.asMap().entrySet()) {
                    Validity.Status subtype = entry2.getKey();
                    Set<String> set = (Set<String>) entry2.getValue();
                    String comment = info.statusComment.get(entry2.getKey());
                    if (comment != null) {
                        output.append("\t\t<!-- " + comment.replace("\n", "\n\t\t\t ") + " -->\n");
                    }
                    output.append("\t\t<id type='" + type + "' idStatus='" + subtype + "'>");
                    final int size = set.size();
                    output.append("\t\t<!-- " + size + " item" + (size > 1 ? "s" : "") // we know it’s English ;-)
                        + " -->");
                    adder.reset(size > 600); //  || type.equals("subdivision")
                    StringRange.compact(set, adder, true);
                    output.append("\n\t\t</id>\n");
                }
//                if (!info.newCodes.isEmpty()) {
//                    output.append("\t\t<!-- Codes added this release:\n\t\t\t" + showCodes(info.newCodes, "\n\t\t\t") + "\n\t\t-->\n");
//                }
                output.append("\t</idValidity>\n</supplementalData>\n");
            }
        }
        // System.out.println("TODO: add Unknown subdivisions, add private_use currencies, ...");
    }

    private static String showCodes(Set<String> newCodes, String linePrefix) {
        StringBuilder result = new StringBuilder();
        String last = "";
        for (String s : newCodes) {
            String newPrefix = s.substring(0, s.indexOf('-'));
            if (last.equals(newPrefix)) {
                result.append(" ");
            } else {
                if (!last.isEmpty()) {
                    result.append(linePrefix);
                }
                last = newPrefix;
            }
            result.append(s);
        }
        return result.toString();
    }

    private static void doCurrency(Map<String, Info> types) {
        Info info = Info.getInfo("currency");
        Date now = new Date();
        Date eoy = new Date(now.getYear() + 1, 0, 1); // Dec
        for (String region : SDI.getCurrencyTerritories()) {
            for (CurrencyDateInfo data : SDI.getCurrencyDateInfo(region)) {
                String currency = data.getCurrency();
                Date end = data.getEnd();
                boolean legalTender = data.isLegalTender();
                Status newStatus = end.after(eoy) && legalTender ? Status.regular : Status.deprecated;
                info.putBest(currency, newStatus);
            }
        }
        info.put(LstrType.currency.unknown, Status.unknown);
        // make sure we don't overlap.
        // we want to keep any code that is valid in any territory, so
        info.remove("XXX", Status.deprecated);
        info.remove("XXX", Status.regular);

        // just to make sure info never disappears
        Map<String, Status> oldCodes = OLD_VALIDITY.getCodeToStatus(LstrType.currency);
        for (Entry<String, Status> entry : oldCodes.entrySet()) {
            String key = entry.getKey();
            Status oldStatus = entry.getValue();
            Status newStatus = info.get(key);
            if (!Objects.equal(oldStatus, newStatus)) {
                System.out.println("Status changed: " + key + ", " + oldStatus + " => " + newStatus);
            }
        }

        info.statusComment.put(Status.deprecated,
            "Deprecated values are those that are not legal tender in some country after " + (1900 + now.getYear()) + ".\n"
                + "More detailed usage information needed for some implementations is in supplemental data.");
    }

    private static void doSubdivisions(Map<String, Info> types) {
        Info info = Info.getInfo("subdivision");
        Map<String, R2<List<String>, String>> aliases = SDI.getLocaleAliasInfo().get("subdivision");
        for (String container : SDI.getContainersForSubdivisions()) {
            for (String contained : SDI.getContainedSubdivisions(container)) {
                Status status = aliases.containsKey(contained) ? Validity.Status.deprecated : Validity.Status.regular;
                info.put(contained.toLowerCase(Locale.ROOT).replace("-", ""), status);
            }
        }

        // find out which items were valid, but are no longer in the containment map
        // add them as deprecated
        Map<Status, Set<String>> oldSubdivisionData = OLD_VALIDITY.getStatusToCodes(LstrType.subdivision);
        for (Entry<Status, Set<String>> entry : oldSubdivisionData.entrySet()) {
            for (String oldSdId : entry.getValue()) {
                if (info.get(oldSdId) == null) {
                    info.put(oldSdId, Status.deprecated);
                }
            }
        }

        info.statusComment.put(Status.deprecated,
            "Deprecated values include those that are not formally deprecated in the country in question, but have their own region codes.\n"
                + "It also include codes that were previously in CLDR, for compatibility.");
        info.statusComment.put(Status.unknown,
            "Unknown/Undetermined subdivision codes (ZZZZ) are defined for all regular region codes.");
    }

    private static void doLstr(Map<String, Info> types) throws IOException {
        Set<String> skippedScripts = new TreeSet<>();
        for (Entry<LstrType, Map<String, Map<LstrField, String>>> entry : LSTREG.entrySet()) {
            LstrType type = entry.getKey();
            if (!type.isLstr || !type.isUnicode) {
                continue;
            }
            Info info = Info.getInfo(type.toString());
            Map<String, R2<List<String>, String>> aliases = SDI.getLocaleAliasInfo().get(type == LstrType.region ? "territory" : type.toString());
            if (aliases == null) {
                System.out.println("No aliases for: " + type);
            }
            // gather data
            info.clear();
            for (Entry<String, Map<LstrField, String>> entry2 : entry.getValue().entrySet()) {
                String code = entry2.getKey();
                if (type == LstrType.language && code.equals("aam")
                    || type == LstrType.variant && code.equals("arevela")
                    || type == LstrType.extlang && code.equals("lsg")
                    ) {
                    int debug = 0;
                }
                Map<LstrField, String> data = entry2.getValue();
                Validity.Status subtype = Validity.Status.regular;
                if (code.equals(type.unknown)) {
                    subtype = Validity.Status.unknown;
                } else if (type.specials.contains(code)) {
                    subtype = Validity.Status.special;
                } else if (aliases != null && aliases.containsKey(code)
                    || data.containsKey(LstrField.Deprecated)) {
                    subtype = Validity.Status.deprecated;
                } else if (data.get(LstrField.Description).startsWith("Private use")) {
                    subtype = Validity.Status.private_use;
                }
                switch (type) {
                case language:
                    if (subtype == Status.private_use && code.compareTo("qfz") < 0) {
                        subtype = Status.reserved;
                    } else if (code.equals("root")) {
                        continue;
                    }
                    break;
                case region:
                    if (containment.contains(code)) {
                        subtype = Validity.Status.macroregion;
                    } else if (code.equals("XA") || code.equals("XB")) {
                        subtype = Validity.Status.special;
                    }
                    switch (subtype) {
                    case regular:
                        Info subInfo = Info.getInfo("subdivision");
                        subInfo.put(code.toLowerCase(Locale.ROOT) + "zzzz", Status.unknown);
                        break;
                    case private_use:
                        if (code.compareTo("X") < 0) {
                            subtype = Status.reserved;
                        }
                        break;
                    default:
                        break;
                    }
                    break;
                case script:
                    switch (code) {
                    case "Qaag":
                    case "Zsye": 
                    case "Zanb":
                    case "Zinh":
                    case "Zyyy":
                        subtype = Status.special;
                        break;
                    default:
                        switch (subtype) {
                        case private_use: 
                            if (code.compareTo("Qaaq") < 0) {
                                subtype = Validity.Status.reserved;
                            }
                            break;
                        case regular:
                            ScriptMetadata.Info scriptInfo = ScriptMetadata.getInfo(code);
                            if (scriptInfo == null && !code.equals("Hrkt")) {
                                skippedScripts.add(code);
                                continue;
                            }
                            break;
                        default: // don't care about rest
                            break;
                        }
                        break;
                    }
                    break;
                case variant:
                    if (VARIANT_EXTRAS.contains(code)) {
                        continue;
                    }
                default:
                    break;
                }
                info.put(code, subtype);
            }
        }
        System.out.println("Skipping non-Unicode scripts: " + Joiner.on(' ').join(skippedScripts));
    }

    static final Set<String> VARIANT_EXTRAS = ImmutableSet.of("POSIX", "REVISED", "SAAHO");
}
