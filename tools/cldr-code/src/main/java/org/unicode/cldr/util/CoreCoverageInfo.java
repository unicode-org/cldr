package org.unicode.cldr.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.draft.ScriptMetadata.Trinary;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.ExemplarSets.ExemplarType;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

public class CoreCoverageInfo {

    private static final CLDRConfig config = CLDRConfig.getInstance();
    private static final String CLDR_BASE_DIRECTORY = config.getCldrBaseDirectory().toString();
    private static final SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
    private static final LikelySubtags ls = new LikelySubtags();

    public enum Sublevel {
        /** Needs to be present at the start of that level's vetting */
        start,
        /** (default) Only to be present by the end start of that level's vetting */
        end
    }

    public enum CoreItems {
        default_content(Level.CORE),
        likely_subtags(Level.CORE),
        country_data(Level.CORE),
        orientation(Level.CORE),
        time_cycle(Level.CORE),

        own_language(Level.BASIC),
        own_regions(Level.BASIC),

        casing(Level.MODERATE, Sublevel.start),
        plurals(Level.MODERATE, Sublevel.start),
        collation(Level.MODERATE),

        spellout_cardinal(Level.MODERN),
        spellout_ordinal(Level.MODERN),
        digits_ordinals(Level.MODERN),

        ordinals(Level.MODERN),
        romanization(Level.MODERN),

        grammar(Level.MODERN, Sublevel.start),
        ;

        public static final Set<CoreItems> ALL = ImmutableSet.copyOf(CoreItems.values());
        public static final Multimap<Level, CoreItems> LEVEL_TO_ITEMS;

        static {
            final Multimap<Level, CoreItems> _levelToItems = TreeMultimap.create();
            ALL.forEach(
                    x -> {
                        for (Level level : Level.values()) {
                            if (level.compareTo(x.desiredLevel) <= 0) {
                                _levelToItems.put(x.desiredLevel, x);
                            }
                        }
                    });
            LEVEL_TO_ITEMS = ImmutableMultimap.copyOf(_levelToItems);
        }

        public final Level desiredLevel;
        public final Sublevel sublevel;

        CoreItems() {
            this(Level.CORE);
        }

        CoreItems(Level desiredLevel) {
            this(desiredLevel, Sublevel.end);
        }

        CoreItems(Level desiredLevel, Sublevel sublevel) {
            this.desiredLevel = desiredLevel;
            this.sublevel = sublevel;
        }

        @Override
        public String toString() {
            return desiredLevel.getAbbreviation() + " " + name();
        }
    }

    static UnicodeSet RTL = new UnicodeSet("[[:bc=R:][:bc=AL:]]").freeze();

    /** Only call on L1 locales (parent = root) */
    public static Set<CoreItems> getCoreCoverageInfo(
            CLDRFile resolvedFile, Multimap<CoreItems, String> detailedErrors) {
        detailedErrors.clear();
        if (!resolvedFile.isResolved()) {
            throw new IllegalArgumentException(
                    "Don't call on unresolved locales: " + resolvedFile.getLocaleID());
        }
        CLDRFile file = resolvedFile.getUnresolved();
        String locale = file.getLocaleID();
        LanguageTagParser ltp = new LanguageTagParser();
        locale = ltp.set(locale).getLanguageScript();
        final String baseLanguage = ltp.getLanguage();
        final String script = ltp.getScript();
        final String region = ltp.getRegion();

        // Set<CoreItems> result = EnumSet.noneOf(CoreItems.class);

        //      (02) Orientation (bidi writing systems only) [main/xxx.xml]
        UnicodeSet main = file.getExemplarSet(ExemplarType.main, null);
        boolean isRtl = main.containsSome(RTL);

        String path = "//ldml/layout/orientation/characterOrder";
        String value = file.getStringValue(path);
        if ("right-to-left".equals(value) != isRtl) {
            detailedErrors.put(CoreItems.orientation, path);
        }

        //      (01) Plural rules [supplemental/plurals.xml and ordinals.xml]
        //      For more information, see cldr-spec/plural-rules.
        if (!sdi.getPluralLocales(PluralType.cardinal).contains(baseLanguage)) {
            detailedErrors.put(
                    CoreItems.plurals,
                    "//supplementalData/plurals[@type=\"cardinal\"]/pluralRules[@locales=\""
                            + locale
                            + "\"]/pluralRule[@count=\"other\"]");
        }
        if (!sdi.getPluralLocales(PluralType.ordinal).contains(baseLanguage)) {
            detailedErrors.put(
                    CoreItems.ordinals,
                    "//supplementalData/plurals[@type=\"ordinal\"]/pluralRules[@locales=\""
                            + locale
                            + "\"]/pluralRule[@count=\"other\"]");
        }

        rbnfHelper(baseLanguage, "spellout-cardinal", detailedErrors, CoreItems.spellout_cardinal);
        rbnfHelper(baseLanguage, "spellout-ordinal", detailedErrors, CoreItems.spellout_ordinal);
        rbnfHelper(baseLanguage, "digits-ordinal", detailedErrors, CoreItems.digits_ordinals);

        //      (01) Default content script and region (normally: normally country with largest
        // population using that language, and normal script for that).
        // [supplemental/supplementalMetadata.xml]

        String defaultContent = sdi.getDefaultContentLocale(locale);
        if (defaultContent == null) { //  || locale.equals("no")
            detailedErrors.put(
                    CoreItems.default_content,
                    "//supplementalData/supplementalMetadata/defaultContent");
        }
        // likely subtags
        final String max = ls.maximize(locale);
        String maxLangScript = "";
        String maxScript = "";
        String maxRegion = "";
        if (max != null) {
            ltp.set(max);
            maxLangScript = ltp.getLanguageScript();
            maxScript = ltp.getScript();
            maxRegion = ltp.getRegion();
            if (maxRegion.equals("ZZ")
                    || maxRegion.equals("001")
                            && Iso639Data.getType(baseLanguage) != Type.Constructed) {
                maxRegion = "";
            }
        }
        if (maxScript.isEmpty() || maxRegion.isEmpty()) {
            detailedErrors.put(CoreItems.likely_subtags, "//supplementalData/likelySubtags");
        }

        String bestScript = script.isEmpty() ? maxScript : script;
        String bestRegion = region.isEmpty() ? maxRegion : region;

        String languagePath = NameType.LANGUAGE.getKeyPath(baseLanguage);
        String languageName = resolvedFile.getStringValue(languagePath);
        if (languageName == null) {
            detailedErrors.put(CoreItems.own_language, languagePath);
        } else {
            String localeWhereFound = resolvedFile.getSourceLocaleID(languagePath, null);
            if ("root".equals(localeWhereFound) || "code-fallback".equals(localeWhereFound)) {
                detailedErrors.put(CoreItems.own_language, languagePath);
            }
        }

        if (bestRegion.isEmpty()) {
            detailedErrors.put(CoreItems.own_regions, "//supplementalData/likelySubtags");
        } else {
            String regionPath = NameType.TERRITORY.getKeyPath(bestRegion);
            String regionName = file.getStringValue(regionPath);
            if (regionName == null) {
                detailedErrors.put(CoreItems.own_regions, regionPath);
            } else {
                String localeWhereFound = resolvedFile.getSourceLocaleID(regionPath, null);
                if (XMLSource.ROOT_ID.equals(localeWhereFound)
                        || XMLSource.CODE_FALLBACK_ID.equals(localeWhereFound)) {
                    detailedErrors.put(CoreItems.own_regions, regionPath);
                }
            }
        }
        // NOTE: other regions will be captured in the coverageLevels

        // (N) Verify the country data ( i.e. which territories in which the language is spoken
        // enough to create a locale ) [supplemental/supplementalData.xml]
        // we verify that there is at least one region
        // we try 3 cases: language, locale, maxLangScript
        Set<String> territories = sdi.getTerritoriesForPopulationData(locale);
        if (territories == null) {
            territories = sdi.getTerritoriesForPopulationData(baseLanguage);
        }
        if (territories == null && maxLangScript != null) {
            territories = sdi.getTerritoriesForPopulationData(maxLangScript);
        }
        if (territories == null || territories.isEmpty()) {
            detailedErrors.put(CoreItems.country_data, "//supplementalData/territoryInfo");
            sdi.getTerritoriesForPopulationData(locale); // for debugging
        }
        //      *(N) Romanization table (non-Latin writing systems only) [spreadsheet, we'll
        // translate into transforms/xxx-en.xml]
        //      If a spreadsheet, for each letter (or sequence) in the exemplars, what is the
        // corresponding Latin letter (or sequence).
        //      More sophisticated users can do a better job, supplying a file of rules like
        // transforms/Arabic-Latin-BGN.xml.

        if (!bestScript.equals("Latn")) {
            boolean found = false;
            Set<String> scriptLongCodes = getScriptNames(bestScript);
            if (scriptLongCodes != null) {
                Set<String> debugErrors = new LinkedHashSet<>();
                for (String scriptLongCode : scriptLongCodes) {
                    for (String[] pair : ROMANIZATION_PATHS) {
                        String filename = pair[0] + scriptLongCode + pair[1];
                        if (hasFile(SpecialDir.transforms, filename)) {
                            found = true;
                            break;
                        } else {
                            debugErrors.add(script);
                        }
                    }
                }
            }
            if (!found) {
                detailedErrors.put(
                        CoreItems.romanization,
                        "//supplementalData/transforms/transform"
                                + "[@source=\"und-"
                                + script
                                + "\"]"
                                + "[@target=\"und-Latn\"]"
                        // + "[@direction=\"forward\"]"
                        );
            }
        }

        //      (N) Casing information (cased scripts only, according to ScriptMetadata.txt)
        //      This will be in common/casing
        Info scriptData = ScriptMetadata.getInfo(bestScript);
        if (scriptData != null
                && scriptData.hasCase == Trinary.YES
                && !hasFile(SpecialDir.casing, baseLanguage)) {
            detailedErrors.put(
                    CoreItems.casing, "//ldml/metadata/casingData/casingItem[@type=\"*\"]");
        }
        //      (N) Collation rules [non-Survey Tool]
        //      For details, see cldr-spec/collation-guidelines.
        //      The result will be a file like: common/collation/ar.xml or common/collation/da.xml.
        //      Note that the "search" collators (which tend to be large) are not needed initially.

        // check for file cldr/collation/<language>.xml
        if (!hasFile(SpecialDir.collation, baseLanguage)) {
            detailedErrors.put(
                    CoreItems.collation, "//ldml/collations/collation[@type=\"standard\"]");
        }

        Map<String, PreferredAndAllowedHour> timeData = sdi.getTimeData();
        if (timeData.get(bestRegion) == null) {
            detailedErrors.put(CoreItems.time_cycle, "//supplementalData/timeData/hours");
        }

        GrammarInfo grammarInfo = sdi.getGrammarInfo(locale);
        if (grammarInfo == null) {
            detailedErrors.put(
                    CoreItems.grammar, "//supplementalData/grammaticalData/grammaticalFeatures");
        }

        // finalize
        return ImmutableSet.copyOf(Sets.difference(CoreItems.ALL, detailedErrors.keySet()));
    }

    private static void rbnfHelper(
            String stringLocale,
            String rbnfType,
            Multimap<CoreItems, String> detailedErrors,
            CoreItems coreItems) {
        CLDRLocale cldrLocale = CLDRLocale.getInstance(stringLocale);
        while (cldrLocale != null // if either null or root, we fail
                && !cldrLocale.equals(CLDRLocale.ROOT)) {
            Multimap<String, String> typeInfo =
                    RbnfData.INSTANCE.getLocaleToTypesToSubtypes().get(cldrLocale.toString());
            if (typeInfo != null // if we succeed, just return
                    && typeInfo.containsKey(rbnfType)) {
                return;
            }
            // otherwise try the parent
            cldrLocale = cldrLocale.getParent();
        }
        detailedErrors.put(coreItems, RbnfData.INSTANCE.getPath(rbnfType));
    }

    private static final String[][] ROMANIZATION_PATHS = {
        {"", "-Latin"},
        {"", "-Latin-BGN"},
        {"Latin-", ""},
    };

    private static final Relation SCRIPT_NAMES = Relation.of(new HashMap(), HashSet.class);

    static {
        SCRIPT_NAMES.putAll("Arab", Arrays.asList("Arabic", "Arab"));
        SCRIPT_NAMES.putAll("Jpan", Arrays.asList("Jpan", "Han"));
        SCRIPT_NAMES.putAll("Hant", Arrays.asList("Hant", "Han"));
        SCRIPT_NAMES.putAll("Hans", Arrays.asList("Hans", "Han"));
        SCRIPT_NAMES.putAll("Kore", Arrays.asList("Hang", "Hangul"));
        SCRIPT_NAMES.freeze();
    }

    private static Set<String> getScriptNames(String script) {
        Set<String> result = SCRIPT_NAMES.get(script);
        if (result != null) {
            return result;
        }
        result = new HashSet<>();
        try {
            String name = UScript.getName(UScript.getCodeFromName(script));
            result.add(name);
            result.add(script);
        } catch (Exception e) {
        }
        return result;
    }

    private enum SpecialDir {
        transforms,
        collation,
        casing
    }

    private static final Relation<SpecialDir, String> SPECIAL_FILES =
            Relation.of(new EnumMap(SpecialDir.class), HashSet.class);

    static {
        for (SpecialDir dir : SpecialDir.values()) {
            File realDir = new File(CLDR_BASE_DIRECTORY + "/common/" + dir);
            for (String s : realDir.list()) {
                if (s.endsWith(".xml")) {
                    s = s.substring(0, s.length() - 4);
                }
                SPECIAL_FILES.put(dir, s);
            }
        }
    }

    private static boolean hasFile(SpecialDir type, String filename) {
        return SPECIAL_FILES.get(type).contains(filename);
    }
}
