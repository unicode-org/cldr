package org.unicode.cldr.draft;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Minimize;

public class XLikelySubtags {
    private static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();
    private static final Map<String, Map<String, R2<List<String>, String>>> aliasInfo = SDI.getLocaleAliasInfo();
    private static final Map<String, R2<List<String>, String>> REGION_ALIASES = aliasInfo.get("territory");
    private static final Map<String, R2<List<String>, String>> LANGUAGE_ALIASES = aliasInfo.get("language");
    private static final XLikelySubtags DEFAULT = new XLikelySubtags(SDI.getLikelySubtags(), true);

    public static final XLikelySubtags getDefault() {
        return DEFAULT;
    }

    private static final boolean SHORT = false;

    static abstract class Maker {
        abstract <V> V make();

        @SuppressWarnings("unchecked")
        public <K, V> V getSubtable(Map<K, V> langTable, final K language) {
            V scriptTable = langTable.get(language);
            if (scriptTable == null) {
                langTable.put(language, scriptTable = (V) make());
            }
            return scriptTable;
        }

        static final Maker HASHMAP = new Maker() {
            @SuppressWarnings("unchecked")
            public Map<Object, Object> make() {
                return new HashMap<>();
            }
        };

        static final Maker TREEMAP = new Maker() {
            @SuppressWarnings("unchecked")
            public Map<Object, Object> make() {
                return new TreeMap<>();
            }
        };
    }

    public static class LSR {
        public final String language;
        public final String script;
        public final String region;

        public static LSR from(String language, String script, String region) {
            return new LSR(language, script, region);
        }

        public static LSR from(ULocale locale) {
            return new LSR(locale.getLanguage(), locale.getScript(), locale.getCountry());
        }

        public static LSR fromMaximalized(ULocale locale) {
            return fromMaximalized(locale.getLanguage(), locale.getScript(), locale.getCountry());
        }

        public static LSR fromMaximalized(String language, String script, String region) {
            String canonicalLanguage = getCanonical(LANGUAGE_ALIASES.get(language));
            // hack
            if (language.equals("mo")) {
                canonicalLanguage = "ro";
            }
            String canonicalRegion = getCanonical(REGION_ALIASES.get(region));

            return DEFAULT.maximize(
                canonicalLanguage == null ? language : canonicalLanguage,
                script,
                canonicalRegion == null ? region : canonicalRegion);
        }

        public LSR(String language, String script, String region) {
            this.language = language;
            this.script = script;
            this.region = region;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(language);
            if (!script.isEmpty()) {
                result.append('-').append(script);
            }
            if (!region.isEmpty()) {
                result.append('-').append(region);
            }
            return result.toString();
        }

        public LSR replace(String language2, String script2, String region2) {
            if (language2 == null && script2 == null && region2 == null) return this;
            return new LSR(
                language2 == null ? language : language2,
                script2 == null ? script : script2,
                region2 == null ? region : region2);
        }

        @Override
        public boolean equals(Object obj) {
            LSR other = (LSR) obj;
            return language.equals(other.language)
                && script.equals(other.script)
                && region.equals(other.region);
        }

        @Override
        public int hashCode() {
            return Objects.hash(language, script, region);
        }
    }

    final Map<String, Map<String, Map<String, LSR>>> langTable;

    public XLikelySubtags(Map<String, String> rawData, boolean skipNoncanonical) {
        this.langTable = init(rawData, skipNoncanonical);
    }

    private Map<String, Map<String, Map<String, LSR>>> init(final Map<String, String> rawData, boolean skipNoncanonical) {
        // prepare alias info. We want a mapping from the canonical form to all aliases

        Multimap<String, String> canonicalToAliasLanguage = HashMultimap.create();
        getAliasInfo(LANGUAGE_ALIASES, canonicalToAliasLanguage);

        // Don't bother with script; there are none

        Multimap<String, String> canonicalToAliasRegion = HashMultimap.create();
        getAliasInfo(REGION_ALIASES, canonicalToAliasRegion);

        Maker maker = Maker.TREEMAP;
        Map<String, Map<String, Map<String, LSR>>> result = maker.make();
        LanguageTagParser ltp = new LanguageTagParser();
        Splitter bar = Splitter.on('_');
        int last = -1;
        // set the base data
        Map<LSR, LSR> internCache = new HashMap<>();
        for (Entry<String, String> sourceTarget : rawData.entrySet()) {
            ltp.set(sourceTarget.getKey());
            final String language = ltp.getLanguage();
            final String script = ltp.getScript();
            final String region = ltp.getRegion();

            ltp.set(sourceTarget.getValue());
            String languageTarget = ltp.getLanguage();
            final String scriptTarget = ltp.getScript();
            final String regionTarget = ltp.getRegion();

            set(result, language, script, region, languageTarget, scriptTarget, regionTarget, internCache);
            // now add aliases
            Collection<String> languageAliases = canonicalToAliasLanguage.get(language);
            if (languageAliases.isEmpty()) {
                languageAliases = Collections.singleton(language);
            }
            Collection<String> regionAliases = canonicalToAliasRegion.get(region);
            if (regionAliases.isEmpty()) {
                regionAliases = Collections.singleton(region);
            }
            for (String languageAlias : languageAliases) {
                for (String regionAlias : regionAliases) {
                    if (languageAlias.equals(language) && regionAlias.equals(region)) {
                        continue;
                    }
                    set(result, languageAlias, script, regionAlias, languageTarget, scriptTarget, regionTarget, internCache);
                }
            }
        }
        // hack
        set(result, "und", "Latn", "", "en", "Latn", "US", internCache);

        // hack, ensure that if und-YY => und-Xxxx-YY, then we add Xxxx=>YY to the table
        // <likelySubtag from="und_GH" to="ak_Latn_GH"/>

        // so und-Latn-GH   =>  ak-Latn-GH
        Map<String, Map<String, LSR>> undScriptMap = result.get("und");
        Map<String, LSR> undEmptyRegionMap = undScriptMap.get("");
        for (Entry<String, LSR> regionEntry : undEmptyRegionMap.entrySet()) {
            final LSR value = regionEntry.getValue();
            set(result, "und", value.script, value.region, value);
        }
        // 
        // check that every level has "" (or "und")
        if (!result.containsKey("und")) {
            throw new IllegalArgumentException("failure: base");
        }
        for (Entry<String, Map<String, Map<String, LSR>>> langEntry : result.entrySet()) {
            String lang = langEntry.getKey();
            final Map<String, Map<String, LSR>> scriptMap = langEntry.getValue();
            if (!scriptMap.containsKey("")) {
                throw new IllegalArgumentException("failure: " + lang);
            }
            for (Entry<String, Map<String, LSR>> scriptEntry : scriptMap.entrySet()) {
                String script = scriptEntry.getKey();
                final Map<String, LSR> regionMap = scriptEntry.getValue();
                if (!regionMap.containsKey("")) {
                    throw new IllegalArgumentException("failure: " + lang + "-" + script);
                }
//                for (Entry<String, LSR> regionEntry : regionMap.entrySet()) {
//                    String region = regionEntry.getKey();
//                    LSR value = regionEntry.getValue();
//                }
            }
        }
        return result;
    }

    private void getAliasInfo(Map<String, R2<List<String>, String>> aliasInfo, Multimap<String, String> canonicalToAlias) {
        for (Entry<String, R2<List<String>, String>> e : aliasInfo.entrySet()) {
            final String alias = e.getKey();
            if (alias.contains("_")) {
                continue; // only do simple aliasing
            }
            String canonical = getCanonical(e.getValue());
            canonicalToAlias.put(canonical, alias);
        }
    }

    private static String getCanonical(R2<List<String>, String> aliasAndReason) {
        if (aliasAndReason == null) {
            return null;
        }
        if (aliasAndReason.get1().equals("overlong")) {
            return null;
        }
        List<String> value = aliasAndReason.get0();
        if (value.size() != 1) {
            return null;
        }
        final String canonical = value.iterator().next();
        if (canonical.contains("_")) {
            return null; // only do simple aliasing
        }
        return canonical;
    }

    private void set(Map<String, Map<String, Map<String, LSR>>> langTable, final String language, final String script, final String region,
        final String languageTarget, final String scriptTarget, final String regionTarget, Map<LSR, LSR> internCache) {
        LSR newValue = new LSR(languageTarget, scriptTarget, regionTarget);
        LSR oldValue = internCache.get(newValue);
        if (oldValue == null) {
            internCache.put(newValue, newValue);
            oldValue = newValue;
        }
        set(langTable, language, script, region, oldValue);
    }

    private void set(Map<String, Map<String, Map<String, LSR>>> langTable, final String language, final String script, final String region, LSR newValue) {
        Map<String, Map<String, LSR>> scriptTable = Maker.TREEMAP.getSubtable(langTable, language);
        Map<String, LSR> regionTable = Maker.TREEMAP.getSubtable(scriptTable, script);
        LSR oldValue = regionTable.get(region);
        if (oldValue != null) {
            int debug = 0;
        }
        regionTable.put(region, newValue);
    }

    /**
     * Convenience methods
     * @param source
     * @return
     */
    public LSR maximize(String source) {
        return maximize(ULocale.forLanguageTag(source));
    }

    public LSR maximize(ULocale source) {
        return maximize(source.getLanguage(), source.getScript(), source.getCountry());
    }

    public LSR maximize(LSR source) {
        return maximize(source.language, source.script, source.region);
    }

//    public static ULocale addLikelySubtags(ULocale loc) {
//        
//    }

    /**
     * Raw access to addLikelySubtags. Input must be in canonical format, eg "en", not "eng" or "EN".
     */
    public LSR maximize(String language, String script, String region) {
        int retainOldMask = 0;
        Map<String, Map<String, LSR>> scriptTable = langTable.get(language);
        if (scriptTable == null) { // cannot happen if language == "und"
            retainOldMask |= 4;
            scriptTable = langTable.get("und");
        } else if (!language.equals("und")) {
            retainOldMask |= 4;
        }

        if (script.equals("Zzzz")) {
            script = "";
        }
        Map<String, LSR> regionTable = scriptTable.get(script);
        if (regionTable == null) { // cannot happen if script == ""
            retainOldMask |= 2;
            regionTable = scriptTable.get("");
        } else if (!script.isEmpty()) {
            retainOldMask |= 2;
        }

        if (region.equals("ZZ")) {
            region = "";
        }
        LSR result = regionTable.get(region);
        if (result == null) { // cannot happen if region == ""
            retainOldMask |= 1;
            result = regionTable.get("");
            if (result == null) {
                return null;
            }
        } else if (!region.isEmpty()) {
            retainOldMask |= 1;
        }

        switch (retainOldMask) {
        default:
        case 0:
            return result;
        case 1:
            return result.replace(null, null, region);
        case 2:
            return result.replace(null, script, null);
        case 3:
            return result.replace(null, script, region);
        case 4:
            return result.replace(language, null, null);
        case 5:
            return result.replace(language, null, region);
        case 6:
            return result.replace(language, script, null);
        case 7:
            return result.replace(language, script, region);
        }
    }

    private LSR minimizeSubtags(String languageIn, String scriptIn, String regionIn, Minimize fieldToFavor) {
        LSR result = maximize(languageIn, scriptIn, regionIn);

        // We could try just a series of checks, like:
        // LSR result2 = addLikelySubtags(languageIn, "", "");
        // if result.equals(result2) return result2;
        // However, we can optimize 2 of the cases:
        //   (languageIn, "", "")
        //   (languageIn, "", regionIn)

        Map<String, Map<String, LSR>> scriptTable = langTable.get(result.language);

        Map<String, LSR> regionTable0 = scriptTable.get("");
        LSR value00 = regionTable0.get("");
        boolean favorRegionOk = false;
        if (result.script.equals(value00.script)) { //script is default
            if (result.region.equals(value00.region)) {
                return result.replace(null, "", "");
            } else if (fieldToFavor == fieldToFavor.FAVOR_REGION) {
                return result.replace(null, "", null);
            } else {
                favorRegionOk = true;
            }
        }

        // The last case is not as easy to optimize.
        // Maybe do later, but for now use the straightforward code.
        LSR result2 = maximize(languageIn, scriptIn, "");
        if (result2.equals(result)) {
            return result.replace(null, null, "");
        } else if (favorRegionOk) {
            return result.replace(null, "", null);
        }
        return result;
    }

    private static <V> StringBuilder show(Map<String, V> map, String indent, StringBuilder output) {
        String first = indent.isEmpty() ? "" : "\t";
        for (Entry<String, V> e : map.entrySet()) {
            String key = e.getKey();
            V value = e.getValue();
            output.append(first + (key.isEmpty() ? "∅" : key));
            if (value instanceof Map) {
                show((Map) value, indent + "\t", output);
            } else {
                output.append("\t" + CldrUtility.toString(value)).append("\n");
            }
            first = indent;
        }
        return output;
    }

    @Override
    public String toString() {
        return show(langTable, "", new StringBuilder()).toString();
    }

    public static void main(String[] args) {
        System.out.println(LSR.fromMaximalized(ULocale.ENGLISH));

        SupplementalDataInfo sdi = SDI;
        final Map<String, String> rawData = sdi.getLikelySubtags();
        XLikelySubtags ls = XLikelySubtags.getDefault();
        System.out.println(ls);
        ls.maximize(new ULocale("iw"));
        if (true) return;

        LanguageTagParser ltp = new LanguageTagParser();
//        String[][] tests = {
//            {"und", "en-Latn-US"},
//            {"und-TW", "en-Latn-US"},
//            {"und-CN", "en-Latn-US"},
//            {"und-Hans", "en-Latn-US"},
//            {"und-Hans-CN", "en-Latn-US"},
//            {"und-Hans-TW", "en-Latn-US"},
//            {"und-Hant", "en-Latn-US"},
//            {"und-Hant-TW", "en-Latn-US"},
//            {"und-Hant-CN", "en-Latn-US"},
//            {"zh-TW", "en-Latn-US"},
//            {"zh-CN", "en-Latn-US"},
//            {"zh-Hans", "en-Latn-US"},
//            {"zh-Hans-CN", "en-Latn-US"},
//            {"zh-Hans-TW", "en-Latn-US"},
//            {"zh-Hant", "en-Latn-US"},
//            {"zh-Hant-TW", "en-Latn-US"},
//            {"zh-Hant-CN", "en-Latn-US"},
//        };
//        for (String[] sourceTarget : tests) {
//            ltp.set(sourceTarget[0]);
//            LSR result = ls.addLikelySubtags(ltp.getLanguage(), ltp.getScript(), ltp.getRegion());
//            ltp.set(sourceTarget[1]);
//            ULocale sourceLocale = ULocale.forLanguageTag(sourceTarget[0]);
//            ULocale max = ULocale.addLikelySubtags(sourceLocale);
//            boolean same = max.toLanguageTag().equals(result.toString());
//            System.out.println(sourceTarget[0] + "\t" + sourceTarget[1] + "\t" + result + (same ? "" : "\t≠" + max.toLanguageTag()));
//        }

        // get all the languages, scripts, and regions
        Set<String> languages = new TreeSet<String>();
        Set<String> scripts = new TreeSet<String>();
        Set<String> regions = new TreeSet<String>();
        Counter<String> languageCounter = new Counter<>();
        Counter<String> scriptCounter = new Counter<>();
        Counter<String> regionCounter = new Counter<>();

        for (Entry<String, String> sourceTarget : rawData.entrySet()) {
            final String source = sourceTarget.getKey();
            ltp.set(source);
            languages.add(ltp.getLanguage());
            scripts.add(ltp.getScript());
            regions.add(ltp.getRegion());
            final String target = sourceTarget.getValue();
            ltp.set(target);
            add(target, languageCounter, ltp.getLanguage(), 1);
            add(target, scriptCounter, ltp.getScript(), 1);
            add(target, regionCounter, ltp.getRegion(), 1);
        }
        ltp.set("und-Zzzz-ZZ");
        languageCounter.add(ltp.getLanguage(), 1);
        scriptCounter.add(ltp.getScript(), 1);
        regionCounter.add(ltp.getRegion(), 1);

        if (SHORT) {
            removeSingletons(languages, languageCounter);
            removeSingletons(scripts, scriptCounter);
            removeSingletons(regions, regionCounter);
        }

        System.out.println("languages: " + languages.size() + "\n\t" + languages + "\n\t" + languageCounter);
        System.out.println("scripts: " + scripts.size() + "\n\t" + scripts + "\n\t" + scriptCounter);
        System.out.println("regions: " + regions.size() + "\n\t" + regions + "\n\t" + regionCounter);

        int maxCount = Integer.MAX_VALUE;

        int counter = maxCount;
        long tempTime = System.nanoTime();
        newMax: for (String language : languages) {
            for (String script : scripts) {
                for (String region : regions) {
                    if (--counter < 0) break newMax;
                    LSR result = ls.maximize(language, script, region);
                }
            }
        }
        long newMaxTime = System.nanoTime() - tempTime;
        System.out.println("newMaxTime: " + newMaxTime);

        counter = maxCount;
        tempTime = System.nanoTime();
        newMin: for (String language : languages) {
            for (String script : scripts) {
                for (String region : regions) {
                    if (--counter < 0) break newMin;
                    LSR minNewS = ls.minimizeSubtags(language, script, region, Minimize.FAVOR_SCRIPT);
                }
            }
        }
        long newMinTime = System.nanoTime() - tempTime;
        System.out.println("newMinTime: " + newMinTime);

        // *****

        tempTime = System.nanoTime();
        counter = maxCount;
        oldMax: for (String language : languages) {
            for (String script : scripts) {
                for (String region : regions) {
                    if (--counter < 0) break oldMax;
                    ULocale tempLocale = new ULocale(language, script, region);
                    ULocale max = ULocale.addLikelySubtags(tempLocale);
                }
            }
        }
        long oldMaxTime = System.nanoTime() - tempTime;
        System.out.println("oldMaxTime: " + oldMaxTime + "\t" + oldMaxTime / newMaxTime + "x");

        counter = maxCount;
        tempTime = System.nanoTime();
        oldMin: for (String language : languages) {
            for (String script : scripts) {
                for (String region : regions) {
                    if (--counter < 0) break oldMin;
                    ULocale tempLocale = new ULocale(language, script, region);
                    ULocale minOldS = ULocale.minimizeSubtags(tempLocale, Minimize.FAVOR_SCRIPT);
                }
            }
        }
        long oldMinTime = System.nanoTime() - tempTime;
        System.out.println("oldMinTime: " + oldMinTime + "\t" + oldMinTime / newMinTime + "x");

        counter = maxCount;
        testMain: for (String language : languages) {
            System.out.println(language);
            int tests = 0;
            for (String script : scripts) {
                for (String region : regions) {
                    ++tests;
                    if (--counter < 0) break testMain;
                    LSR maxNew = ls.maximize(language, script, region);
                    LSR minNewS = ls.minimizeSubtags(language, script, region, Minimize.FAVOR_SCRIPT);
                    LSR minNewR = ls.minimizeSubtags(language, script, region, Minimize.FAVOR_REGION);

                    ULocale tempLocale = new ULocale(language, script, region);
                    ULocale maxOld = ULocale.addLikelySubtags(tempLocale);
                    ULocale minOldS = ULocale.minimizeSubtags(tempLocale, Minimize.FAVOR_SCRIPT);
                    ULocale minOldR = ULocale.minimizeSubtags(tempLocale, Minimize.FAVOR_REGION);

                    // check values
                    final String maxNewS = String.valueOf(maxNew);
                    final String maxOldS = maxOld.toLanguageTag();
                    boolean sameMax = maxOldS.equals(maxNewS);

                    final String minNewSS = String.valueOf(minNewS);
                    final String minOldSS = minOldS.toLanguageTag();
                    boolean sameMinS = minNewSS.equals(minOldSS);

                    final String minNewRS = String.valueOf(minNewR);
                    final String minOldRS = minOldS.toLanguageTag();
                    boolean sameMinR = minNewRS.equals(minOldRS);

                    if (sameMax && sameMinS && sameMinR) continue;
                    System.out.println(new LSR(language, script, region)
                        + "\tmax: " + maxNew
                        + (sameMax ? "" : "≠" + maxOldS)
                        + "\tminS: " + minNewS
                        + (sameMinS ? "" : "≠" + minOldS)
                        + "\tminR: " + minNewR
                        + (sameMinR ? "" : "≠" + minOldR));
                }
            }
            System.out.println(language + ": " + tests);
        }
    }

    private static void add(String target, Counter<String> languageCounter, String language, int count) {
        if (language.equals("aa")) {
            int debug = 0;
        }
        languageCounter.add(language, count);
    }

    private static void removeSingletons(Set<String> languages, Counter<String> languageCounter) {
        for (String s : languageCounter) {
            final long count = languageCounter.get(s);
            if (count <= 1) {
                languages.remove(s);
            }
        }
    }
}
