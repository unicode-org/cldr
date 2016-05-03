package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Containment;
import org.unicode.cldr.util.DateTimeCanonicalizer.DateTimePatternType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.PreferredAndAllowedHour;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.With;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.UnicodeSet;

public class FindPreferredHours {
    private static CLDRConfig INFO = ToolConfig.getToolInstance();
    private static final CLDRFile ENGLISH = INFO.getEnglish();
    private static final UnicodeSet DIGITS = new UnicodeSet("[0-9]").freeze();

    private static final Set<Character> ONLY24 = Collections.unmodifiableSet(new LinkedHashSet<Character>(Arrays
        .asList('H')));

    private final static Map<String, Set<Character>> OVERRIDE_ALLOWED = Builder
        .with(new HashMap<String, Set<Character>>())
        .put("RU", ONLY24)
        .put("IL", ONLY24)
        .freeze();

    private final static Map<String, Character> CONFLICT_RESOLUTION = Builder.with(new HashMap<String, Character>())
        .put("DJ", 'h')
        .put("KM", 'H')
        .put("MG", 'H')
        .put("MU", 'H')
        .put("MZ", 'H')
        .put("SC", 'H')
        .put("CM", 'H')
        .put("TD", 'h')
        .put("DZ", 'h')
        .put("MA", 'h')
        .put("TN", 'h')
        .put("BW", 'h')
        .put("LS", 'h')
        .put("NA", 'h')
        .put("SZ", 'h')
        .put("ZA", 'h')
        .put("GH", 'h')
        .put("MR", 'h')
        .put("NG", 'h')
        .put("TG", 'H')
        .put("CA", 'h')
        .put("US", 'h')
        .put("CN", 'h')
        .put("MO", 'h')
        .put("PH", 'H')
        .put("IN", 'h')
        .put("LK", 'H')
        .put("CY", 'h')
        .put("IL", 'H')
        .put("SY", 'h')
        .put("MK", 'H')
        .put("VU", 'h')
        .put("TO", 'H')
        .put("001", 'H')
        .freeze();

    static final class Hours implements Comparable<Hours> {
        final DateTimePatternType type;
        final char variable;

        public Hours(DateTimePatternType type, String variable) {
            this.type = type;
            this.variable = variable.charAt(0);
        }

        @Override
        public int compareTo(Hours arg0) {
            // TODO Auto-generated method stub
            int result = type.compareTo(arg0.type);
            if (result != 0) return result;
            return variable < arg0.variable ? -1 : variable > arg0.variable ? 1 : 0;
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return type + ":" + variable;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Hours && compareTo((Hours) obj) == 0;
        }
    }

    public static void main(String[] args) {
        final Relation<String, Hours> lang2Hours = Relation.of(new TreeMap<String, Set<Hours>>(), TreeSet.class);
        final Factory factory = INFO.getCldrFactory();
        final FormatParser formatDateParser = new FormatParser();
        final LikelySubtags likely2Max = new LikelySubtags(INFO.getSupplementalDataInfo());

        for (final String locale : factory.getAvailable()) {
            if (locale.equals("root")) {
                continue;
            }
            // if (locale.charAt(0) > 'b') {
            // continue;
            // }
            final CLDRFile cldrFile = factory.make(locale, true);
            for (String path : With.in(cldrFile)) {
                // if (path.contains("/timeFormats")) {
                // System.out.println(path);
                // }
                DateTimePatternType type = DateTimePatternType.fromPath(path);
                if (type == DateTimePatternType.NA || type == DateTimePatternType.GMT) {
                    continue;
                }
                String value = cldrFile.getStringValue(path);
                formatDateParser.set(value);
                for (Object item : formatDateParser.getItems()) {
                    if (item instanceof VariableField) {
                        String itemString = item.toString();
                        if (PreferredAndAllowedHour.HourStyle.isHourCharacter(itemString)) {
                            lang2Hours.put(locale, new Hours(type, itemString));
                        }
                    }
                }
            }
            System.out.println(locale + "\t" + lang2Hours.get(locale));
            // for (Entry<String, Set<String>> e : lang2Hours.keyValuesSet()) {
            // System.out.println(e);
            // }
        }

        // gather data per region

        Map<String, Relation<Character, String>> region2Preferred2locales = new TreeMap<String, Relation<Character, String>>();
        Relation<String, Character> region2Allowed = Relation.of(new TreeMap<String, Set<Character>>(), TreeSet.class);
        final LanguageTagParser ltp = new LanguageTagParser();

        for (Entry<String, Set<Hours>> localeAndHours : lang2Hours.keyValuesSet()) {
            String locale = localeAndHours.getKey();
            String maxLocale = likely2Max.maximize(locale);
            if (maxLocale == null) {
                System.out.println("*** Missing likely for " + locale);
                continue;
            }
            String region = ltp.set(maxLocale).getRegion();
            if (region.isEmpty()) {
                System.out.println("*** Missing region for " + locale + ", " + maxLocale);
                continue;
            }
            if (DIGITS.containsSome(region) && !region.equals("001")) {
                System.out.println("*** Skipping multicountry region for " + locale + ", " + maxLocale);
                continue;
            }
            for (Hours hours : localeAndHours.getValue()) {
                region2Allowed.put(region, hours.variable);
                if (hours.type == DateTimePatternType.STOCK) {
                    Relation<Character, String> items = region2Preferred2locales.get(region);
                    if (items == null) {
                        region2Preferred2locales.put(region,
                            items = Relation.of(new TreeMap<Character, Set<String>>(), TreeSet.class));
                    }
                    items.put(hours.variable, locale);
                }
            }
        }

        // now invert
        Relation<PreferredAndAllowedHour, String> preferred2Region = Relation.of(
            new TreeMap<PreferredAndAllowedHour, Set<String>>(), TreeSet.class);
        StringBuilder overrides = new StringBuilder("\n");

        for (Entry<String, Relation<Character, String>> e : region2Preferred2locales.entrySet()) {
            String region = e.getKey();
            Set<Character> allowed = region2Allowed.get(region);
            Relation<Character, String> preferredSet = e.getValue();
            Character resolvedValue = CONFLICT_RESOLUTION.get(region);
            if (resolvedValue != null) {
                if (preferredSet.size() == 1) {
                    overrides.append(region + " didn't need override!!\n");
                } else {
                    LinkedHashSet<Entry<Character, String>> oldValues = new LinkedHashSet<Entry<Character, String>>();
                    StringBuilder oldValuesString = new StringBuilder();
                    for (Entry<Character, String> x : preferredSet.keyValueSet()) {
                        if (!x.getKey().equals(resolvedValue)) {
                            oldValues.add(x);
                            oldValuesString.append(x.getKey() + "=" + x.getValue() + "; ");
                        }
                    }
                    for (Entry<Character, String> x : oldValues) {
                        preferredSet.remove(x.getKey(), x.getValue());
                    }
                    overrides.append(region + " has multiple values. Overriding with CONFLICT_RESOLUTION to "
                        + resolvedValue + " and discarded values " + oldValuesString + "\n");
                }
            }

            Set<Character> allAllowed = new TreeSet<Character>();
            Character preferred = null;

            for (Entry<Character, Set<String>> pref : preferredSet.keyValuesSet()) {
                allAllowed.addAll(allowed);
                if (preferred == null) {
                    preferred = pref.getKey();
                } else {
                    overrides.append(region + " has multiple preferred values! " + preferredSet + "\n");
                }
                // else {
                // if (!haveFirst) {
                // System.out.print("*** Conflict in\t" + region + "\t" + ENGLISH.getName("territory", region) +
                // "\twith\t");
                // System.out.println(preferred + "\t" + locales);
                // haveFirst = true;
                // }
                // //System.out.println("\t" + pref.getKey() + "\t" + pref.getValue());
                // }
            }
            Set<Character> overrideAllowed = OVERRIDE_ALLOWED.get(region);
            if (overrideAllowed != null) {
                allAllowed = overrideAllowed;
                overrides.append(region + " overriding allowed to " + overrideAllowed + "\n");
            }
            try {
                preferred2Region.put(new PreferredAndAllowedHour(preferred, allAllowed), region);
            } catch (RuntimeException e1) {
                throw e1;
            }
            String subcontinent = Containment.getSubcontinent(region);
            String continent = Containment.getContinent(region);
            String tag = CollectionUtilities.join(preferredSet.keySet(), ",");
            if (tag.equals("h")) {
                tag += "*";
            }

            System.out.println(tag
                + "\t" + region
                + "\t" + ENGLISH.getName("territory", region)
                + "\t" + subcontinent
                + "\t" + ENGLISH.getName("territory", subcontinent)
                + "\t" + continent
                + "\t" + ENGLISH.getName("territory", continent)
                + "\t" + showInfo(preferredSet));
        }

        // now present

        System.out.println("    <timeData>");
        for (Entry<PreferredAndAllowedHour, Set<String>> e : preferred2Region.keyValuesSet()) {
            PreferredAndAllowedHour preferredAndAllowedHour = e.getKey();
            Set<String> regions = e.getValue();
            System.out.println("        <hours "
                + "preferred=\""
                + preferredAndAllowedHour.preferred
                + "\""
                + " allowed=\""
                + CollectionUtilities.join(preferredAndAllowedHour.allowed, " ")
                + "\""
                + " regions=\"" + CollectionUtilities.join(regions, " ") + "\""
                + "/>");
        }
        System.out.println("    </timeData>");
        System.out.println(overrides);
    }

    private static String showInfo(Relation<Character, String> preferredSet) {
        StringBuilder b = new StringBuilder();
        for (Character key : Arrays.asList('H', 'h')) {
            if (b.length() != 0) {
                b.append('\t');
            }
            b.append(key).append('\t');
            Set<String> value = preferredSet.get(key);
            if (value != null) {
                boolean needSpace = false;
                for (String locale : value) {
                    if (needSpace) {
                        b.append(" ");
                    } else {
                        needSpace = true;
                    }
                    b.append(locale);
                    boolean isOfficial = false;
                    isOfficial = isOfficial(locale, isOfficial);
                    if (isOfficial) {
                        b.append('°');
                    }
                }
            }
        }
        return b.toString();
    }

    private static boolean isOfficial(String locale, boolean isOfficial) {
        LanguageTagParser ltp = new LanguageTagParser().set(locale);
        PopulationData data = INFO.getSupplementalDataInfo().getLanguageAndTerritoryPopulationData(
            ltp.getLanguageScript(), ltp.getRegion());
        if (data == null) {
            data = INFO.getSupplementalDataInfo().getLanguageAndTerritoryPopulationData(
                ltp.getLanguage(), ltp.getRegion());
        }
        if (data != null) {
            OfficialStatus status = data.getOfficialStatus();
            if (status == OfficialStatus.official || status == OfficialStatus.de_facto_official) {
                isOfficial = true;
            }
        }
        return isOfficial;
    }
}
