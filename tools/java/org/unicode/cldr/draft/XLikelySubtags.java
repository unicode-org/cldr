package org.unicode.cldr.draft;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Splitter;
import com.ibm.icu.util.ULocale;

public class XLikelySubtags {

    static abstract class Maker {
        abstract <V> V make();

        @SuppressWarnings("unchecked")
        public <K,V> V getSubtable(Map<K, V> langTable, final K language) {
            V scriptTable = langTable.get(language);
            if (scriptTable == null) {
                langTable.put(language, scriptTable = (V) make());
            }
            return scriptTable;
        }

        static final Maker HASHMAP = new Maker() {
            @SuppressWarnings("unchecked")
            public Map<Object,Object> make() {
                return new HashMap<>();
            }
        };

        static final Maker TREEMAP = new Maker() {
            @SuppressWarnings("unchecked")
            public Map<Object,Object> make() {
                return new TreeMap<>();
            }
        };
    }

    static class LSR {
        final String language;
        final String script;
        final String region;

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
                language2 == null ? language: language2, 
                    script2 == null ? script : script2, 
                        region2 == null ? region : region2);
        }
    }
    final Map<String, Map<String, Map<String, LSR>>> langTable;

    public XLikelySubtags(Map<String, String> rawData) {
        this.langTable = init(rawData);
    }

    public XLikelySubtags() {
        this(CLDRConfig.getInstance().getSupplementalDataInfo().getLikelySubtags());
    }

    private Map<String, Map<String, Map<String, LSR>>> init(final Map<String, String> rawData) {
        Maker maker = Maker.TREEMAP;
        Map<String, Map<String, Map<String, LSR>>> result = maker.make();
        LanguageTagParser ltp = new LanguageTagParser();
        Splitter bar = Splitter.on('_');
        int last = -1;
        // set the base data
        Map<LSR,LSR> internCache = new HashMap<>();
        for (Entry<String, String> sourceTarget : rawData.entrySet()) {
            ltp.set(sourceTarget.getKey());
            final String language = ltp.getLanguage();
            final String script = ltp.getScript();
            final String region = ltp.getRegion();

            ltp.set(sourceTarget.getValue());
            final String languageTarget = ltp.getLanguage();
            final String scriptTarget = ltp.getScript();
            final String regionTarget = ltp.getRegion();

            set(result, language, script, region, languageTarget, scriptTarget, regionTarget, internCache);
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
     * Raw access to addLikelySubtags. Input must be in canonical format, eg "en", not "eng" or "EN".
     */
    public LSR addLikelySubtags(String language, String script, String region) {
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
        case 0: return result;
        case 1: return result.replace(null, null, region);
        case 2: return result.replace(null, script, null);
        case 3: return result.replace(null, script, region);
        case 4: return result.replace(language, null, null);
        case 5: return result.replace(language, null, region);
        case 6: return result.replace(language, script, null);
        case 7: return result.replace(language, script, region);
        }
    }

    private static <V> StringBuilder show(Map<String,V> map, String indent, StringBuilder output) {
        String first = indent.isEmpty() ? "" : "\t";
        for (Entry<String,V> e : map.entrySet()) {
            String key = e.getKey();
            V value = e.getValue();
            output.append(first + (key.isEmpty() ? "∅" : key) + " →");
            if (value instanceof Map) {
                show((Map)value, indent+"\t", output);
            } else {
                output.append(" " + CldrUtility.toString(value)).append("\n");
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
        SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        final Map<String, String> rawData = sdi.getLikelySubtags();
        XLikelySubtags ls = new XLikelySubtags();
        System.out.println(ls);

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
        
        for (Entry<String, String> sourceTarget : rawData.entrySet()) {
            final String source = sourceTarget.getKey();
            ltp.set(source);
            languages.add(ltp.getLanguage());
            scripts.add(ltp.getScript());
            regions.add(ltp.getRegion());
            ltp.set(sourceTarget.getValue());
            languages.add(ltp.getLanguage());
            scripts.add(ltp.getScript());
            regions.add(ltp.getRegion());
        }
        ltp.set("und-Zzzz-ZZ");
        languages.add(ltp.getLanguage());
        scripts.add(ltp.getScript());
        regions.add(ltp.getRegion());
        
        System.out.println("languages: " + languages.size() + "\t" + languages);
        System.out.println("scripts: " + scripts.size() + "\t" + scripts);
        System.out.println("regions: " + regions.size() + "\t" + regions);
        
        int maxCount = 1000000;
        int counter = maxCount;
        long tempTime = System.nanoTime();
        newMain:
        for (String language : languages) {
            for (String script : scripts) {
                for (String region : regions) {
                    LSR result = ls.addLikelySubtags(language, script, region);
                    if (--counter < 0) break newMain;
                }
            }
        }
        long newTime = System.nanoTime() - tempTime;
        System.out.println("newTime: " + newTime);

        tempTime = System.nanoTime();
        counter = maxCount;
        oldMain:
        for (String language : languages) {
            for (String script : scripts) {
                for (String region : regions) {
                    ULocale tempLocale = new ULocale(language, script, region);
                    ULocale max = ULocale.addLikelySubtags(tempLocale);
                    if (--counter < 0) break oldMain;
                }
            }
        }
        long oldTime = System.nanoTime() - tempTime;
        System.out.println("oldTime: " + oldTime + "\t" + oldTime/newTime + "x");
       
        counter = maxCount;
        testMain:
        for (String language : languages) {
            int tests = 0;
            for (String script : scripts) {
                for (String region : regions) {
                    ++tests;
                    if (--counter < 0) break testMain;
                    LSR result = ls.addLikelySubtags(language, script, region);
                    ULocale tempLocale = new ULocale(language, script, region);
                    ULocale max = ULocale.addLikelySubtags(tempLocale);
                    final String resultString = String.valueOf(result);
                    final String maxString = max.toLanguageTag();
                    boolean same = maxString.equals(resultString);
                    if (same) continue;
                    System.out.println(language + "\t" + script + "\t" + region + "\t" + result + (same ? "" : "\t≠" + maxString));        
                    result = ls.addLikelySubtags(language, script, region);
                }
            }
            System.out.println(language + ": " + tests);
        }
    }
}
