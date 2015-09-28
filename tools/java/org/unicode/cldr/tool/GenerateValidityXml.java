package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.util.CLDRPaths;
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
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.ICUUncheckedIOException;

public class GenerateValidityXml {
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
        Relation<Validity.Status, String> statusMap = Relation.of(new EnumMap<Validity.Status, Set<String>>(Validity.Status.class), TreeSet.class);
        Map<Validity.Status, String> statusComment = new EnumMap<>(Status.class);

        static Map<String, Info> types = new LinkedHashMap<>();

        static Info getInfo(String myType) {
            Info info = types.get(myType);
            if (info == null) {
                types.put(myType, info = new Info());
            }
            return info;
        }
    }

    static Map<String, Info> types = Info.types;

    public static void main(String[] args) throws IOException {
        doLstr(types);
        doSubdivisions(types);
        doCurrency(types);
        // write file
        MyAdder adder = new MyAdder();
        for (Entry<String, Info> entry : types.entrySet()) {
            String type = entry.getKey();
            final Info info = entry.getValue();
            Relation<Status, String> subtypeMap = info.statusMap;
            try (PrintWriter output = BagFormatter.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "validity/" + type + ".xml")) {
                adder.target = output;
                output.append(DtdType.supplementalData.header()
                    + "\t<version number='$"
                    + "Revision$'/>\n"
                    + "\t\t<idValidity>\n");
                for (Entry<Validity.Status, Set<String>> entry2 : subtypeMap.keyValuesSet()) {
                    Validity.Status subtype = entry2.getKey();
                    Set<String> set = entry2.getValue();
                    String comment = info.statusComment.get(entry2.getKey());
                    if (comment != null) {
                        output.append("\t\t<!-- " + comment.replace("\n", "\n\t\t\t ") + " -->\n");
                    }
                    output.append("\t\t<id type='" + type + "' idStatus='" + subtype + "'>");
                    adder.reset(set.size() > 600);
                    StringRange.compact(set, adder, true);
                    output.append("\n\t\t</id>\n");
                }
                output.append("\t</idValidity>\n</supplementalData>\n");
            }
        }
        System.out.println("TODO: add Unknown subdivisions, add private_use currencies, ...");
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
                info.statusMap.put(end.after(eoy) && legalTender ? Status.regular : Status.deprecated, currency);
            }
        }
        info.statusMap.put(Status.unknown, LstrType.currency.unknown);
        // make sure we don't overlap.
        // we want to keep any code that is valid in any territory, so
        info.statusMap.removeAll(Status.deprecated, info.statusMap.get(Status.regular));
        info.statusMap.remove(Status.deprecated, "XXX");
        info.statusMap.remove(Status.regular, "XXX");
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
                info.statusMap.put(status, contained);
            }
        }

        info.statusComment.put(Status.deprecated,
            "Deprecated values include those that are not formally deprecated in the country in question, but have their own region codes.");
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
            info.statusMap.clear();
            for (Entry<String, Map<LstrField, String>> entry2 : entry.getValue().entrySet()) {
                String code = entry2.getKey();
                Map<LstrField, String> data = entry2.getValue();
                Validity.Status subtype = Validity.Status.regular;
                if (code.equals(type.unknown)) {
                    subtype = Validity.Status.unknown;
                } else if (type.specials.contains(code)) {
                    subtype = Validity.Status.special;
                } else if (aliases != null && aliases.containsKey(code)) {
                    subtype = Validity.Status.deprecated;
                } else if (data.get(LstrField.Description).startsWith("Private use")) {
                    subtype = Validity.Status.private_use;
                }
                switch (type) {
                case region:
                    if (containment.contains(code)) {
                        subtype = Validity.Status.macroregion;
                    }
                    if (subtype == Status.regular) {
                        Info subInfo = Info.getInfo("subdivision");
                        subInfo.statusMap.put(Status.special, code + "-" + "ZZZZ");
                    }
                    break;
                case script:
                    if (type == LstrType.script && subtype == Validity.Status.regular) {
                        ScriptMetadata.Info scriptInfo = ScriptMetadata.getInfo(code);
                        if (scriptInfo == null && !code.equals("Hrkt")) {
                            skippedScripts.add(code);
                            continue;
                        }
                    }
                    break;
                }
                info.statusMap.put(subtype, code);
            }

        }
        System.out.println("Skipping non-Unicode scripts: " + Joiner.on(' ').join(skippedScripts));
    }
}
