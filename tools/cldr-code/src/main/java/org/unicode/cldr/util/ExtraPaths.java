package org.unicode.cldr.util;

import static org.unicode.cldr.util.StandardCodes.CodeType.currency;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.test.CheckMetazones;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

public class ExtraPaths {
    private static final boolean DEBUG = false;

    private static final SupplementalDataInfo supplementalData =
            CLDRConfig.getInstance().getSupplementalDataInfo();

    private static final ImmutableSet<String> casesNominativeOnly =
            ImmutableSet.of(GrammarInfo.GrammaticalFeature.grammaticalCase.getDefault(null));

    /**
     * A set of paths to be added. These are constant across locales, and don't have good fallback
     * values in root. NOTE: if this is changed, you'll need to modify
     * TestPaths.extraPathAllowsNullValue
     */
    private static final Set<String> CONST_EXTRA_PATHS =
            CharUtilities.internImmutableSet(
                    Set.of(
                            // Individual zone overrides
                            "//ldml/dates/timeZoneNames/zone[@type=\"Europe/Dublin\"]/long/daylight",
                            "//ldml/dates/timeZoneNames/zone[@type=\"Europe/London\"]/long/daylight",
                            "//ldml/dates/timeZoneNames/zone[@type=\"Etc/UTC\"]/long/standard",
                            "//ldml/dates/timeZoneNames/zone[@type=\"Etc/UTC\"]/short/standard",
                            // Person name paths
                            "//ldml/personNames/sampleName[@item=\"nativeG\"]/nameField[@type=\"given\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeGS\"]/nameField[@type=\"given\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeGS\"]/nameField[@type=\"surname\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeGGS\"]/nameField[@type=\"given\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeGGS\"]/nameField[@type=\"given2\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeGGS\"]/nameField[@type=\"surname\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeFull\"]/nameField[@type=\"title\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeFull\"]/nameField[@type=\"given\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeFull\"]/nameField[@type=\"given-informal\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeFull\"]/nameField[@type=\"given2\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeFull\"]/nameField[@type=\"surname-prefix\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeFull\"]/nameField[@type=\"surname-core\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeFull\"]/nameField[@type=\"surname2\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeFull\"]/nameField[@type=\"generation\"]",
                            "//ldml/personNames/sampleName[@item=\"nativeFull\"]/nameField[@type=\"credentials\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignG\"]/nameField[@type=\"given\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignGS\"]/nameField[@type=\"given\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignGS\"]/nameField[@type=\"surname\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignGGS\"]/nameField[@type=\"given\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignGGS\"]/nameField[@type=\"given2\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignGGS\"]/nameField[@type=\"surname\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignFull\"]/nameField[@type=\"title\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignFull\"]/nameField[@type=\"given\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignFull\"]/nameField[@type=\"given-informal\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignFull\"]/nameField[@type=\"given2\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignFull\"]/nameField[@type=\"surname-prefix\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignFull\"]/nameField[@type=\"surname-core\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignFull\"]/nameField[@type=\"surname2\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignFull\"]/nameField[@type=\"generation\"]",
                            "//ldml/personNames/sampleName[@item=\"foreignFull\"]/nameField[@type=\"credentials\"]",

                            // core/extension for language names (languages at modern or moderate)

                            "//ldml/localeDisplayNames/languages/language[@type=\"ku\"][@menu=\"core\"]",
                            "//ldml/localeDisplayNames/languages/language[@type=\"ku\"][@menu=\"extension\"]"));

    public static void addConstant(Collection<String> toAddTo) {
        toAddTo.addAll(SingletonHelper.INSTANCE.paths);
    }

    private static class SingletonHelper {
        private static final Singleton INSTANCE = new Singleton();
    }

    private static class Singleton {
        private static final String SCOPE_CORE = "[@scope=\"core\"]";
        private final Collection<String> paths;
        private Collection<String> pathsTemp;

        Singleton() {
            pathsTemp = new TreeSet<>();
            addPaths(NameType.LANGUAGE);
            addPaths(NameType.SCRIPT);
            addPaths(NameType.TERRITORY);
            addPaths(NameType.VARIANT);
            addMetazones();
            addBcp47Keys();
            pathsTemp.addAll(CONST_EXTRA_PATHS);
            paths = ImmutableSet.copyOf(pathsTemp); // preserves order (Sets.copyOf doesn't)
            pathsTemp = null;
        }

        private void addPaths(NameType nameType) {
            StandardCodes.CodeType codeType = nameType.toCodeType();
            StandardCodes sc = StandardCodes.make();
            Set<String> codes = new TreeSet<>(sc.getGoodAvailableCodes(codeType));
            adjustCodeSet(codes, nameType);
            for (String code : codes) {
                pathsTemp.add(nameType.getKeyPath(code));
            }
            addAltPaths(nameType);
        }

        private void adjustCodeSet(Set<String> codes, NameType nameType) {
            switch (nameType) {
                case LANGUAGE:
                    codes.remove(LocaleNames.ROOT);
                    codes.addAll(
                            List.of(
                                    "ar_001", "de_AT", "de_CH", "en_AU", "en_CA", "en_GB", "en_US",
                                    "es_419", "es_ES", "es_MX", "fa_AF", "fr_CA", "fr_CH", "frc",
                                    "hi_Latn", "lou", "nds_NL", "nl_BE", "pt_BR", "pt_PT", "ro_MD",
                                    "sw_CD", "zh_Hans", "zh_Hant"));
                    break;
                case TERRITORY:
                    codes.addAll(List.of("XA", "XB"));
                    break;
            }
        }

        private void addAltPaths(NameType nameType) {
            switch (nameType) {
                case LANGUAGE:
                    addAltPath("en_GB", "short", nameType);
                    addAltPath("en_US", "short", nameType);
                    addAltPath("az", "short", nameType);
                    addAltPath("ckb", "menu", nameType);
                    addAltPath("ckb", "variant", nameType);
                    addAltPath("hi_Latn", "variant", nameType);
                    addAltPath("yue", "menu", nameType);
                    addAltPath("zh", "menu", nameType);
                    addAltPath("zh_Hans", "long", nameType);
                    addAltPath("zh_Hant", "long", nameType);
                    break;
                case SCRIPT:
                    addAltPath("Hans", "stand-alone", nameType);
                    addAltPath("Hant", "stand-alone", nameType);
                case TERRITORY:
                    addAltPath("GB", "short", nameType);
                    addAltPath("HK", "short", nameType);
                    addAltPath("MO", "short", nameType);
                    addAltPath("PS", "short", nameType);
                    addAltPath("US", "short", nameType);
                    addAltPath("CD", "variant", nameType);
                    addAltPath("CG", "variant", nameType);
                    addAltPath("CI", "variant", nameType);
                    addAltPath("CZ", "variant", nameType);
                    addAltPath("FK", "variant", nameType);
                    addAltPath("TL", "variant", nameType);
                    addAltPath("SZ", "variant", nameType);
                    addAltPath("IO", "biot", nameType);
                    addAltPath("IO", "chagos", nameType);
                    // new alternate name
                    addAltPath("NZ", "variant", nameType);
                    addAltPath("TR", "variant", nameType);
            }
        }

        private void addAltPath(String code, String alt, NameType nameType) {
            String fullpath = nameType.getKeyPath(code);
            // Insert the @alt= string after the last occurrence of "]"
            StringBuilder fullpathBuf = new StringBuilder(fullpath);
            String altPath =
                    fullpathBuf
                            .insert(fullpathBuf.lastIndexOf("]") + 1, "[@alt=\"" + alt + "\"]")
                            .toString();
            pathsTemp.add(altPath);
        }

        private void addMetazones() {
            for (String zone : supplementalData.getAllMetazones()) {
                final boolean metazoneUsesDST = CheckMetazones.metazoneUsesDST(zone);
                for (String width : new String[] {"long", "short"}) {
                    for (String type : new String[] {"generic", "standard", "daylight"}) {
                        if (metazoneUsesDST || type.equals("standard")) {
                            // Only add /standard for non-DST metazones
                            final String path =
                                    "//ldml/dates/timeZoneNames/metazone[@type=\""
                                            + zone
                                            + "\"]/"
                                            + width
                                            + "/"
                                            + type;
                            pathsTemp.add(path);
                        }
                    }
                }
            }
        }

        /** All BCP47 <key type="…"> entries which are based on aliases */
        private static final Set<String> existingBcp47KeyAliases =
                ImmutableSet.of(
                        // These were statically listed in ExtraPaths.java
                        "calendar",
                        "collation",
                        "currency",
                        "numbers",
                        // These were in en.xml, manually added
                        "colAlternate",
                        "colBackwards",
                        "colCaseFirst",
                        "colCaseLevel",
                        "colNormalization",
                        "colNumeric",
                        "colReorder",
                        "colStrength",
                        "timezone");

        // skip some very productive keys that are seen elsewhere
        private static final Set<String> skipKeys =
                ImmutableSet.of(
                        "currency", // ldml/numbers/currencies/currency/displayName
                        "timezone" // ldml/dates/timeZoneNames
                        );

        /** add BCP47 keys */
        private void addBcp47Keys() {
            final Relation<String, String> bcp47Keys = supplementalData.getBcp47Keys();
            // All top level keys, "ca", etc.
            final Set<String> allBcp47Keys = new TreeSet<>(bcp47Keys.keySet());

            // map from alias to key (of the aliases we care about)
            final Map<String, String> aliasToKey = new HashMap<>();

            // remove deprecated keys (kh, vt, ...)
            final Map<R2<String, String>, String> bcp47Deprecated =
                    supplementalData.getBcp47Deprecated();
            for (final Entry<R2<String, String>, String> e : bcp47Deprecated.entrySet()) {
                if (e.getValue().equals("true") && e.getKey().get1().isEmpty()) {
                    allBcp47Keys.remove(e.getKey().get0());
                }
            }

            final Relation<R2<String, String>, String> aliases = supplementalData.getBcp47Aliases();
            // Remove the existing aliases
            // otherwise we would have "collation" and "co"
            for (final Entry<R2<String, String>, String> e : aliases.entrySet()) {
                if (existingBcp47KeyAliases.contains(e.getValue())) {
                    final String k = e.getKey().get0();
                    final String alias = e.getValue();
                    // remove "co"
                    allBcp47Keys.remove(k);
                    // add "collation"
                    allBcp47Keys.add(alias);
                    aliasToKey.put(alias, k);
                }
            }
            // Add top level extensions, 't', 'u' (but remove "u"). This will catch any new
            // extensions added.
            allBcp47Keys.addAll(supplementalData.getBcp47Extension2Keys().keySet());
            allBcp47Keys.remove("u");

            // add "x"
            allBcp47Keys.add("x");

            final Set<String> SKIP_TYPES = supplementalData.getBcp47SkipTypes();

            // Add all remaining items
            for (final String k : allBcp47Keys) {
                final String path =
                        String.format("//ldml/localeDisplayNames/keys/key[@type=\"%s\"]", k);
                pathsTemp.add(path);

                // now, values
                if (skipKeys.contains(k)) continue;
                final String originalKey = aliasToKey.getOrDefault(k, k);
                final Set<String> s = bcp47Keys.get(originalKey);
                if (s == null) {
                    // doesn't have types (may be 'x', etc.)
                    continue;
                }
                final String typeKeyPath =
                        String.format(
                                "//ldml/localeDisplayNames/types/type[@key=\"%s\"]",
                                k); // not originalKey
                for (String t : s) {
                    if (SKIP_TYPES.contains(t)) continue;

                    if ("true".equals(bcp47Deprecated.get(R2.of(originalKey, t)))) {
                        // skip deprecated k/v (such as collation/big5han)
                        continue;
                    }
                    // remap some existing aliases that were found in locales
                    String type;
                    switch (t) {
                        case "gregory":
                            type = "gregorian";
                            break;
                        case "ethioaa":
                            type = "ethiopic-amete-alem";
                            break;
                        case "dict":
                            type = "dictionary";
                            break;
                        case "phonebk":
                            type = "phonebook";
                            break;
                        case "trad":
                            type = "traditional";
                            break;
                        case "noignore":
                            type = "non-ignorable";
                            break;
                        case "identic":
                            type = "identical";
                            break;
                        case "traditio":
                            type = "traditional";
                            break;
                        case "false":
                            type = "no";
                            break;
                        case "true":
                            type = "yes";
                            break;
                        case "level1":
                            type = "primary";
                            break;
                        case "level2":
                            type = "secondary";
                            break;
                        case "level3":
                            type = "tertiary";
                            break;
                        case "level4":
                            type = "quaternary";
                            break;
                        default:
                            type = t;
                    }

                    final String typeKeyTypePath =
                            typeKeyPath + String.format("[@type=\"%s\"]", type);
                    pathsTemp.add(typeKeyTypePath);
                    // add all of these with [@scope="core"]
                    pathsTemp.add(typeKeyTypePath + SCOPE_CORE);
                }
            }
        }
    }

    public static void addLocaleDependent(
            Set<String> toAddTo, Iterable<String> file, String localeID) {
        SupplementalDataInfo.PluralInfo plurals =
                supplementalData.getPlurals(SupplementalDataInfo.PluralType.cardinal, localeID);
        SupplementalDataInfo.PluralInfo ordinals =
                supplementalData.getPlurals(SupplementalDataInfo.PluralType.ordinal, localeID);
        if (DEBUG && (plurals == null || ordinals == null)) {
            System.err.println(
                    "No "
                            + SupplementalDataInfo.PluralType.cardinal
                            + "  plurals for "
                            + localeID
                            + " in "
                            + supplementalData.getDirectory().getAbsolutePath());
        }

        addDayOfMonthPaths(toAddTo, ordinals);
        addMinimalPairs(toAddTo, plurals, ordinals);
        Set<SupplementalDataInfo.PluralInfo.Count> pluralCounts =
                Count.LOCALES_USING_OTHER_ONLY_HACK.contains(localeID)
                        ? Collections.emptySet()
                        : (plurals != null) ? plurals.getAdjustedCounts() : Collections.emptySet();
        addUnitPlurals(toAddTo, file, plurals, pluralCounts);
        addDayPlurals(toAddTo, localeID);
        addCurrencies(toAddTo, pluralCounts);
        addGrammar(toAddTo, pluralCounts, localeID);
    }

    private static void addDayOfMonthPaths(Set<String> toAddTo, PluralInfo ordinals) {
        if (ordinals != null) {
            ordinals.getCounts().stream()
                    .forEach(
                            ordinal -> {
                                for (String calendar : List.of("gregorian", "generic")) {
                                    for (String context : List.of("format", "stand-alone")) {
                                        toAddTo.add(
                                                dayOfMonthPath(
                                                        ordinal, calendar, context, "abbreviated"));
                                    }
                                }
                            });
        }
    }

    private static String dayOfMonthPath(
            Count ordinal, String calendar, String context, String width) {
        return "//ldml/dates/calendars/calendar[@type=\""
                + calendar
                + "\"]/dayOfMonthContext[@type=\""
                + context
                + "\"]/dayOfMonthWidth[@type=\""
                + width
                + "\"]/dayOfMonth[@ordinal=\""
                + ordinal
                + "\"]";
    }

    private static void addMinimalPairs(
            Set<String> toAddTo, PluralInfo plurals, PluralInfo ordinals) {
        if (plurals != null) {
            plurals.getCounts().stream()
                    .forEach(
                            x ->
                                    toAddTo.add(
                                            "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\""
                                                    + x
                                                    + "\"]"));
        }
        if (ordinals != null) {
            ordinals.getCounts().stream()
                    .forEach(
                            x ->
                                    toAddTo.add(
                                            "//ldml/numbers/minimalPairs/ordinalMinimalPairs[@ordinal=\""
                                                    + x
                                                    + "\"]"));
        }
    }

    private static void addUnitPlurals(
            Set<String> toAddTo,
            Iterable<String> file,
            SupplementalDataInfo.PluralInfo plurals,
            Set<SupplementalDataInfo.PluralInfo.Count> pluralCounts) {
        if (plurals != null) {
            Set<SupplementalDataInfo.PluralInfo.Count> pluralCountsRaw = plurals.getCounts();
            if (pluralCountsRaw.size() != 1) {
                // we get all the root paths with count
                addPluralCounts(toAddTo, pluralCounts, pluralCountsRaw, file);
            }
        }
    }

    private static void addPluralCounts(
            Collection<String> toAddTo,
            final Set<SupplementalDataInfo.PluralInfo.Count> pluralCounts,
            final Set<SupplementalDataInfo.PluralInfo.Count> pluralCountsRaw,
            Iterable<String> file) {
        for (String path : file) {
            String countAttr = "[@count=\"other\"]";
            int countPos = path.indexOf(countAttr);
            if (countPos < 0) {
                continue;
            }
            Set<SupplementalDataInfo.PluralInfo.Count> pluralCountsNeeded =
                    path.startsWith("//ldml/numbers/minimalPairs") ? pluralCountsRaw : pluralCounts;
            if (pluralCountsNeeded.size() > 1) {
                String start = path.substring(0, countPos) + "[@count=\"";
                String end = "\"]" + path.substring(countPos + countAttr.length());
                for (SupplementalDataInfo.PluralInfo.Count count : pluralCounts) {
                    if (count == SupplementalDataInfo.PluralInfo.Count.other) {
                        continue;
                    }
                    toAddTo.add(start + count + end);
                }
            }
        }
    }

    private static void addDayPlurals(Set<String> toAddTo, String localeID) {
        DayPeriodInfo dayPeriods =
                supplementalData.getDayPeriods(DayPeriodInfo.Type.format, localeID);
        if (dayPeriods != null) {
            LinkedHashSet<DayPeriodInfo.DayPeriod> items =
                    new LinkedHashSet<>(dayPeriods.getPeriods());
            items.add(DayPeriodInfo.DayPeriod.am);
            items.add(DayPeriodInfo.DayPeriod.pm);
            for (String context : new String[] {"format", "stand-alone"}) {
                for (String width : new String[] {"narrow", "abbreviated", "wide"}) {
                    for (DayPeriodInfo.DayPeriod dayPeriod : items) {
                        // ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="format"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="am"]
                        toAddTo.add(
                                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/"
                                        + "dayPeriodContext[@type=\""
                                        + context
                                        + "\"]/dayPeriodWidth[@type=\""
                                        + width
                                        + "\"]/dayPeriod[@type=\""
                                        + dayPeriod
                                        + "\"]");
                    }
                }
            }
        }
    }

    private static void addCurrencies(
            Set<String> toAddTo, Set<SupplementalDataInfo.PluralInfo.Count> pluralCounts) {
        // This code is locale-dependent due to pluralCounts.
        // Locale-independent currency paths are added elsewhere.
        if (!pluralCounts.isEmpty()) {
            for (String code : StandardCodes.make().getGoodAvailableCodes(currency)) {
                for (SupplementalDataInfo.PluralInfo.Count count : pluralCounts) {
                    toAddTo.add(
                            "//ldml/numbers/currencies/currency[@type=\""
                                    + code
                                    + "\"]/displayName[@count=\""
                                    + count.toString()
                                    + "\"]");
                }
            }
        }
    }

    private static void addGrammar(
            Set<String> toAddTo,
            Set<SupplementalDataInfo.PluralInfo.Count> pluralCounts,
            String localeID) {
        GrammarInfo grammarInfo = supplementalData.getGrammarInfo(localeID, true);
        if (grammarInfo != null) {
            if (grammarInfo.hasInfo(GrammarInfo.GrammaticalTarget.nominal)) {
                Collection<String> genders =
                        grammarInfo.get(
                                GrammarInfo.GrammaticalTarget.nominal,
                                GrammarInfo.GrammaticalFeature.grammaticalGender,
                                GrammarInfo.GrammaticalScope.units);
                Collection<String> rawCases =
                        grammarInfo.get(
                                GrammarInfo.GrammaticalTarget.nominal,
                                GrammarInfo.GrammaticalFeature.grammaticalCase,
                                GrammarInfo.GrammaticalScope.units);
                Collection<String> nomCases = rawCases.isEmpty() ? casesNominativeOnly : rawCases;
                // There was code here allowing fewer plurals to be used, but is retracted for now
                // (needs more thorough integration in logical groups, etc.)
                // This note is left for 'blame' to find the old code in case we revive that.

                // TODO use UnitPathType to get paths
                if (!genders.isEmpty()) {
                    for (String unit : GrammarInfo.getUnitsToAddGrammar()) {
                        toAddTo.add(
                                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\""
                                        + unit
                                        + "\"]/gender");
                    }
                    for (SupplementalDataInfo.PluralInfo.Count plural : pluralCounts) {
                        for (String gender : genders) {
                            for (String case1 : nomCases) {
                                final String grammaticalAttributes =
                                        GrammarInfo.getGrammaticalInfoAttributes(
                                                grammarInfo,
                                                UnitPathType.power,
                                                plural.toString(),
                                                gender,
                                                case1);
                                toAddTo.add(
                                        "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1"
                                                + grammaticalAttributes);
                                toAddTo.add(
                                        "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power3\"]/compoundUnitPattern1"
                                                + grammaticalAttributes);
                            }
                        }
                    }
                    //             <genderMinimalPairs gender="masculine">Der {0} ist
                    // …</genderMinimalPairs>
                    for (String gender : genders) {
                        toAddTo.add(
                                "//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\""
                                        + gender
                                        + "\"]");
                    }
                }
                if (!rawCases.isEmpty()) {
                    for (String case1 : rawCases) {
                        //          <caseMinimalPairs case="nominative">{0} kostet
                        // €3,50.</caseMinimalPairs>
                        toAddTo.add(
                                "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\""
                                        + case1
                                        + "\"]");

                        for (SupplementalDataInfo.PluralInfo.Count plural : pluralCounts) {
                            for (String unit : GrammarInfo.getUnitsToAddGrammar()) {
                                toAddTo.add(
                                        "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\""
                                                + unit
                                                + "\"]/unitPattern"
                                                + GrammarInfo.getGrammaticalInfoAttributes(
                                                        grammarInfo,
                                                        UnitPathType.unit,
                                                        plural.toString(),
                                                        null,
                                                        case1));
                            }
                        }
                    }
                }
            }
        }
    }
}
