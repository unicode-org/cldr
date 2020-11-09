package org.unicode.cldr.tool;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M5;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.StringRange;
import org.unicode.cldr.util.StringRange.Adder;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.lang.CharSequences;

@SuppressWarnings("deprecation")
public class CompareIso3166_1Status {

    private static final Joiner SPACE_JOINER = Joiner.on(" ");

    public enum Iso3166Status {
        officially_assigned, private_use, exceptionally_reserved, indeterminately_reserved, transitionally_reserved, formerly_used, out_of_scope
    }

    public enum CldrStatus {
        region, macroregion, deprecated, privateUse, unused,
    }

    public static void main(String[] args) {

        Map<String, Iso3166Status> isoStatus = new TreeMap<>();
        Map<String, String> isoDescription = new TreeMap<>();
        Splitter semi = Splitter.on(';').trimResults();
        for (String line : FileUtilities.in(StandardCodes.class, "data/external/iso_3166_status.txt")) {
            if (line.startsWith("#")) continue;
            List<String> parts = semi.splitToList(line);
            // AC ; Exceptionally reserved  ; Refers to the United Nations and reserved by the ISO 3166 Maintenance Agency.
            final String regionCode = parts.get(0);
            isoStatus.put(regionCode, Iso3166Status.valueOf(parts.get(1).toLowerCase(Locale.ROOT).replace(' ', '_')));
            isoDescription.put(regionCode, parts.get(1));
        }

        Map<String, Map<LstrField, String>> lstregRegions = StandardCodes.getEnumLstreg().get(LstrType.region);
        Map<String, Map<LstrField, String>> lstregRegionsRaw = StandardCodes.getLstregEnumRaw().get(LstrType.region);

        Set<String> seen = new HashSet<>();
        M5<CldrStatus, CldrStatus, Iso3166Status, String, Boolean> ordered = ChainedMap.of(
            new TreeMap<CldrStatus, Object>(),
            new TreeMap<CldrStatus, Object>(),
            new TreeMap<Iso3166Status, Object>(),
            new TreeMap<String, Object>(),
            Boolean.class);

        Map<String, CldrStatus> cldrStatus = new TreeMap<>();
        Map<String, CldrStatus> bcp47Status = new TreeMap<>();
        for (Entry<String, Map<LstrField, String>> entry : lstregRegions.entrySet()) {
            String regionCode = entry.getKey();
            final Map<LstrField, String> cldrData = entry.getValue();
            String description = setStatus(regionCode, cldrData, cldrStatus, true);

            final Map<LstrField, String> bcp47Data = lstregRegionsRaw.get(regionCode);
            setStatus(regionCode, bcp47Data, bcp47Status, false);

            final Iso3166Status isoStatus2 = CldrUtility.ifNull(isoStatus.get(regionCode), Iso3166Status.out_of_scope);
            ordered.put(cldrStatus.get(regionCode), bcp47Status.get(regionCode), isoStatus2, regionCode, Boolean.TRUE);
            System.out.println(regionCode
                + "\t" + cldrStatus.get(regionCode)
                + "\t" + bcp47Status.get(regionCode)
                + "\t" + isoStatus2
                //+ "\t" + description
                + "\t" + bcp47Data);
            seen.add(regionCode);
        }

        for (Entry<String, Map<LstrField, String>> entry : lstregRegionsRaw.entrySet()) {
            String regionCode = entry.getKey();
            final Map<LstrField, String> bcp47Data = entry.getValue();
            setStatus(regionCode, bcp47Data, bcp47Status, false);
        }

        Set<String> missing = new TreeSet<>(isoStatus.keySet());
        missing.removeAll(seen);
        for (String regionCode : missing) {
            Iso3166Status isoStatus2 = isoStatus.get(regionCode);
            cldrStatus.put(regionCode, CldrStatus.unused);
            bcp47Status.put(regionCode, CldrStatus.unused);
            ordered.put(cldrStatus.get(regionCode), bcp47Status.get(regionCode), isoStatus2, regionCode, Boolean.TRUE);
            System.out.println(regionCode
                + "\t" + cldrStatus.get(regionCode)
                + "\t" + bcp47Status.get(regionCode)
                + "\t" + isoStatus2
                + "\t" + isoDescription.get(regionCode));
        }
        System.out.println();

        for (Entry<CldrStatus, Map<CldrStatus, Map<Iso3166Status, Map<String, Boolean>>>> entry : ordered) {
            CldrStatus cldrStatus2 = entry.getKey();
            for (Entry<CldrStatus, Map<Iso3166Status, Map<String, Boolean>>> entry2 : entry.getValue().entrySet()) {
                CldrStatus bcp47Status2 = entry2.getKey();
                for (Entry<Iso3166Status, Map<String, Boolean>> entry3 : entry2.getValue().entrySet()) {
                    Iso3166Status isoStatus2 = entry3.getKey();
                    Set<String> codes = entry3.getValue().keySet();
                    System.out.println("||\t" + cldrStatus2
                        + "\t||\t" + bcp47Status2
                        + "\t||\t" + isoStatus2
                        + "\t||\t" + codes.size()
                        + "\t||\t" + compactDisplay(codes)
                        + "\t||");
                }
            }
        }
    }

    private static String compactDisplay(Set<String> codes) {
        final StringBuilder b = new StringBuilder();
        Adder myAdder = new Adder() { // for testing: doesn't do quoting, etc
            @Override
            public void add(String start, String end) {
                if (b.length() != 0) {
                    b.append(' ');
                }
                b.append(start);
                if (end != null) {
                    b.append('-').append(end);
                }
            }
        };
        StringRange.compact(codes, myAdder, false);
        return b.toString();
    }

    // guaranteed ascii!
    private static int toNumber(String code) {
        int num = 0;
        for (int cp : CharSequences.codePoints(code)) {
            num <<= 7;
            num += cp;
        }
        return num;
    }

    private static String fromNumber(int cp) {
        StringBuilder b = new StringBuilder();
        while (cp != 0) {
            int part = cp & 0x7F;
            b.insert(0, (char) part);
            cp >>= 7;
        }
        return b.toString();
    }

    static SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();

    private static String setStatus(String regionCode, final Map<LstrField, String> cldrData, Map<String, CldrStatus> cldrStatus, boolean showMacro) {
        String description = cldrData.get(LstrField.Description);

        if (cldrData.containsKey(LstrField.Deprecated)) {
            cldrStatus.put(regionCode, CldrStatus.deprecated);
        } else if (description.equalsIgnoreCase("Private use")) {
            cldrStatus.put(regionCode, CldrStatus.privateUse);
        } else if (showMacro && SDI.getContained(regionCode) != null) {
            cldrStatus.put(regionCode, CldrStatus.macroregion);
        } else {
            cldrStatus.put(regionCode, CldrStatus.region);
        }
        return description;
    }
}
