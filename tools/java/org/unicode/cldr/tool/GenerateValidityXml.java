package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StringRange;
import org.unicode.cldr.util.StringRange.Adder;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.Relation;
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
                if (twoCodePoints){
                    firstCodePoint <<= 22;
                    firstCodePoint |= start.codePointAt(1);
                }
                if (firstCodePoint == lastCodePoint){
                    target.append(' ');
                } else {
                    target.append("\n\t\t\t");
                }
                target.append(start);
                if (end != null) {
                    target.append('-').append(end);
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

    public static void main(String[] args)  throws IOException {
        MyAdder adder = new MyAdder();
        Relation<Validity.Status, String> subtypeMap = Relation.of(new EnumMap<Validity.Status,Set<String>>(Validity.Status.class), TreeSet.class);
        for (Entry<LstrType, Map<String, Map<LstrField, String>>> entry : LSTREG.entrySet()) {
            LstrType type = entry.getKey();
            if (!type.inCldr) {
                continue;
            }
            
            Map<String, R2<List<String>, String>> aliases = SDI.getLocaleAliasInfo().get(type == LstrType.region ? "territory" : type.toString());
            if (aliases == null) {
                System.out.println("No aliases for: " + type);
            }
            // gather data
            subtypeMap.clear();
            for (Entry<String, Map<LstrField, String>> entry2 : entry.getValue().entrySet()) {
                String code = entry2.getKey();
                Map<LstrField, String> data = entry2.getValue();
                Validity.Status subtype = Validity.Status.regular;
                if (code.equals(type.unknown)) {
                    subtype = Validity.Status.unknown;
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
                }
                subtypeMap.put(subtype, code);
            }

            // write file
            try (PrintWriter output = BagFormatter.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "validity/" + type + ".xml")) {
                adder.target = output;
                output.append(DtdType.supplementalData.header()
                    + "\t<version number='$"
                    + "Revision$'/>\n"
                    + "\t<generation date='$"
                    + "Date$'/>\n"
                    + "\t\t<idValidity>\n");
                for (Entry<Validity.Status, Set<String>> entry2 : subtypeMap.keyValuesSet()) {
                    Validity.Status subtype = entry2.getKey();
                    Set<String> set = entry2.getValue();
                    output.append("\t\t<id type='" + type + "' idStatus='" + subtype + "'>");
                    adder.reset(set.size() > 600);
                    StringRange.compact(set, adder, true);
                    output.append("\n\t\t</id>\n");
                }
                output.append("\t</idValidity>\n</supplementalData>\n");
            }
        }
    }
}
