package org.unicode.cldr.util;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.draft.ScriptMetadata.Trinary;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterDirection;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class CoreCoverageInfo {

    private static final CLDRConfig config = CLDRConfig.getInstance();
    private static final String CLDR_BASE_DIRECTORY = config.getCldrBaseDirectory().toString();
    private static final SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
    private static final LikelySubtags ls = new LikelySubtags(sdi);

    public enum CoreItems {
        main_exemplar, auxiliary_exemplar, index_exemplar, orientation, plurals, ordinals, default_content, likely_subtags, country_data, romanization, casing, collation;
        public static Set<CoreItems> ONLY_RECOMMENDED = Collections.unmodifiableSet(
            EnumSet.of(romanization, ordinals));
        public static final int COUNT = CoreItems.values().length;
    }

    public static Set<CoreItems> getCoreCoverageInfo(CLDRFile file, Set<String> detailedErrors) {
        if (file.isResolved()) {
            file = file.getUnresolved();
        }
        String locale = file.getLocaleID();
        LanguageTagParser ltp = new LanguageTagParser();
        locale = ltp.set(locale).getLanguageScript();
        String baseLanguage = ltp.getLanguage();
        String script = ltp.getScript();

        Set<CoreItems> result = EnumSet.noneOf(CoreItems.class);

        //      (04) Exemplar sets: main, auxiliary, index, punctuation. [main/xxx.xml]
        //      These must reflect the Unicode model. For more information, see tr35-general.html#Character_Elements.
        boolean isRtl = false;
        for (CoreItems exemplar : EXEMPLARS) {
            String type = exemplar.toString();
            type = type.substring(0, type.indexOf('_'));

            String path = "//ldml/characters/exemplarCharacters";
            boolean isMain = type.equals("main");
            if (!isMain) {
                path += "[@type=\"" + type + "\"]";
            }
            String value = file.getStringValue(path);
            if (value != null) {
                String sourceLocale = file.getSourceLocaleID(path, null);
                if (locale.equals(sourceLocale)) {
                    result.add(exemplar);
                }
            }
            if (isMain && result.contains(exemplar)) {
                UnicodeSet main = new UnicodeSet(value);
                isRtl = isRtl(main);
            }
        }
        //      (02) Orientation (bidi writing systems only) [main/xxx.xml]
        String path = "//ldml/layout/orientation/characterOrder";
        String value = file.getStringValue(path);
        if ("right-to-left".equals(value) == isRtl) {
            result.add(CoreItems.orientation);
        }

        //      (01) Plural rules [supplemental/plurals.xml and ordinals.xml]
        //      For more information, see cldr-spec/plural-rules.
        if (sdi.getPluralLocales(PluralType.cardinal).contains(baseLanguage)) {
            result.add(CoreItems.plurals);
        }
        if (sdi.getPluralLocales(PluralType.ordinal).contains(baseLanguage)) {
            result.add(CoreItems.ordinals);
        }

        //      (01) Default content script and region (normally: normally country with largest population using that language, and normal script for that).  [supplemental/supplementalMetadata.xml]

        String defaultContent = sdi.getDefaultContentLocale(locale);
        if (defaultContent != null) {
            result.add(CoreItems.default_content);
        }
        // likely subtags
        String max = ls.maximize(locale);
        String maxLangScript = null;
        if (max != null) {
            ltp.set(max);
            maxLangScript = ltp.getLanguageScript();
            script = ltp.getScript();
            if (!script.isEmpty() && !ltp.getRegion().isEmpty()) {
                result.add(CoreItems.likely_subtags);
            }
        }
        //      (N) Verify the country data ( i.e. which territories in which the language is spoken enough to create a locale ) [supplemental/supplementalData.xml]
        // we verify that there is at least one region
        // we try 3 cases: language, locale, maxLangScript
        Set<String> territories = sdi.getTerritoriesForPopulationData(locale);
        if (territories == null) {
            territories = sdi.getTerritoriesForPopulationData(baseLanguage);
        }
        if (territories == null && maxLangScript != null) {
            territories = sdi.getTerritoriesForPopulationData(maxLangScript);
        }
        if (territories != null && territories.size() != 0) {
            result.add(CoreItems.country_data);
        } else {
            sdi.getTerritoriesForPopulationData(locale); // for debugging
        }
        //      *(N) Romanization table (non-Latin writing systems only) [spreadsheet, we'll translate into transforms/xxx-en.xml]
        //      If a spreadsheet, for each letter (or sequence) in the exemplars, what is the corresponding Latin letter (or sequence).
        //      More sophisticated users can do a better job, supplying a file of rules like transforms/Arabic-Latin-BGN.xml.

        if (script.equals("Latn")) {
            result.add(CoreItems.romanization);
        } else {
            boolean found = false;
            Set<String> scriptNames = getScriptNames(script);
            Set<String> tempErrors = new HashSet<>();
            for (String scriptName : scriptNames) {
                for (String[] pair : ROMANIZATION_PATHS) {
                    String filename = pair[0] + scriptName + pair[1];
                    if (hasFile(SpecialDir.transforms, filename)) {
                        result.add(CoreItems.romanization);
                        found = true;
                        break;
                    } else {
                        tempErrors.add("No romanization for " + script + " => " + scriptName);
                    }
                }
            }
            if (!found) {
                detailedErrors.addAll(tempErrors);
            }
        }

        //      (N) Casing information (cased scripts only, according to ScriptMetadata.txt)
        //      This will be in common/casing
        Info scriptData = ScriptMetadata.getInfo(script);
        if (scriptData.hasCase == Trinary.YES) {
            if (hasFile(SpecialDir.casing, baseLanguage)) {
                result.add(CoreItems.casing);
            }
        } else {
            result.add(CoreItems.casing);
        }
        //      (N) Collation rules [non-Survey Tool]
        //      For details, see cldr-spec/collation-guidelines.
        //      The result will be a file like: common/collation/ar.xml or common/collation/da.xml.
        //      Note that the "search" collators (which tend to be large) are not needed initially.

        // check for file cldr/collation/<language>.xml
        if (hasFile(SpecialDir.collation, baseLanguage)) {
            result.add(CoreItems.collation);
        }
        return Collections.unmodifiableSet(result);
    }

    private static final Set<CoreItems> EXEMPLARS = EnumSet.of(
        CoreItems.main_exemplar,
        CoreItems.auxiliary_exemplar,
        CoreItems.index_exemplar);

    private static final String[][] ROMANIZATION_PATHS = {
        { "", "-Latin" },
        { "", "-Latin-BGN" },
        { "Latin-", "" },
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
        result = new HashSet();
        String name = UScript.getName(UScript.getCodeFromName(script));
        result.add(name);
        result.add(script);
        return result;
    }

    private enum SpecialDir {
        transforms, collation, casing
    }

    private static final Relation<SpecialDir, String> SPECIAL_FILES = Relation.of(new EnumMap(SpecialDir.class), HashSet.class);
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

    public static boolean isRtl(UnicodeSet main) {
        for (UnicodeSetIterator it = new UnicodeSetIterator(main); it.nextRange();) {
            for (int i = it.codepoint; i <= it.codepointEnd; ++i) {
                int bidiClass = UCharacter.getDirection(i);
                switch (bidiClass) {
                case UCharacterDirection.RIGHT_TO_LEFT:
                case UCharacterDirection.RIGHT_TO_LEFT_ARABIC:
                    return true;
                case UCharacterDirection.LEFT_TO_RIGHT:
                    return false;
                }
            }
        }
        return false;
    }

}
