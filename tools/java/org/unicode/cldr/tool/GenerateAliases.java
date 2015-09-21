package org.unicode.cldr.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;

public class GenerateAliases {
    public static void main(String[] args) {
        new Builder().getAliases();
    }

    static class Builder {
        Map<String, String> aliasMap = new LinkedHashMap<String, String>();
        Factory factory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");

        SupplementalDataInfo dataInfo = SupplementalDataInfo.getInstance();
        Set<String> defaultContents = dataInfo.getDefaultContentLocales();
        LikelySubtags likelySubtags = new LikelySubtags();
        Map<String, Map<String, R2<List<String>, String>>> aliasInfo = dataInfo.getLocaleAliasInfo();

        Relation<String, String> goodToBadLanguages = getGoodToBad(aliasInfo, "language");
        Relation<String, String> goodToBadTerritories = getGoodToBad(aliasInfo, "territory");
        Relation<String, String> goodToBadScripts = getGoodToBad(aliasInfo, "script");

        // sh //ldml/alias[@source="sr_Latn"][@path="//ldml"]
        LanguageTagParser ltp = new LanguageTagParser();
        final Set<String> available = factory.getAvailable();

        Builder() {

            for (String localeID : available) {
                String targetID = getDefaultContents(localeID);
                if (targetID == null) {
                    targetID = localeID;
                }
                addAlias("deprecated", localeID, targetID);
                // special hack for sh
                if (localeID.startsWith("sr_Latn")) {
                    addAlias("deprecated", "sh" + localeID.substring(7), localeID);
                }
            }

            Map<String, String> likely = new TreeMap<String, String>();

            // get all the combinations
            for (String max : likelySubtags.getToMaximized().values()) {
                likely.put(max, getDefaultContents(max));
                ltp.set(max);
                ltp.setScript("");
                addToLikely(likely);
                ltp.set(max);
                ltp.setRegion("");
                addToLikely(likely);
                ltp.setScript("");
                addToLikely(likely);
            }

            for (Entry<String, String> small2large : likely.entrySet()) {
                String localeID = small2large.getKey();
                String targetID = small2large.getValue();
                if (localeID.equals(targetID)) {
                    continue;
                }
                String base = ltp.set(localeID).getLanguage();
                if (!available.contains(base)) { // skip seed locales
                    continue;
                }
                // if (!localeID.contains("_")) {
                // continue; // skip languages not represented
                // }
                if (available.contains(localeID) && !isWholeAlias(factory, localeID)) {
                    continue;
                }
                targetID = getDefaultContents(targetID);
                addAlias("default", localeID, targetID);
            }

            for (String localeID : available) {
                if (aliasMap.get(localeID) != null) {
                    continue;
                }
                if (isWholeAlias(factory, localeID)) {
                    System.out.println("missing" + "\t" + localeID);
                }
            }

            // System.out.println(CollectionUtilities.join(aliasMap.entrySet(), "\n"));
        }

        private void addToLikely(Map<String, String> likely) {
            String partial = ltp.toString();
            final String target = getDefaultContents(partial);
            String parent = LocaleIDParser.getSimpleParent(partial);
            if (target.equals(parent)) {
                return;
            }
            likely.put(partial, target);
        }

        static final Set<String> HAS_MULTIPLE_SCRIPTS = org.unicode.cldr.util.Builder.with(new HashSet<String>())
            .addAll("ha", "ku", "zh", "sr", "uz", "sh").freeze();

        private boolean hasMultipleScripts(String localeID) {
            LanguageTagParser ltp = new LanguageTagParser().set(localeID);
            return HAS_MULTIPLE_SCRIPTS.contains(ltp.getLanguage());
        }

        private String getDefaultContents(String localeID) {
            String targetID = hasMultipleScripts(localeID) ? likelySubtags.maximize(localeID) : likelySubtags
                .minimize(localeID);

            if (targetID == null) {
                System.out.println("missingLikely" + "\t" + localeID);
                return localeID;
            }
            while (defaultContents.contains(targetID)) {
                String parent = LocaleIDParser.getSimpleParent(targetID);
                if (parent == null || parent.equals("root)")) {
                    break;
                }
                targetID = parent;
            }
            return targetID;
        }

        public Map<String, String> getAliases() {
            return aliasMap;
        }

        static final UnicodeSet NUMBERS = new UnicodeSet("[0-9]");

        private Relation<String, String> getGoodToBad(Map<String, Map<String, R2<List<String>, String>>> aliasInfo,
            String tag) {
            Relation<String, String> result = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
            Map<String, R2<List<String>, String>> map = aliasInfo.get(tag);
            for (Entry<String, R2<List<String>, String>> entity : map.entrySet()) {
                final String key = entity.getKey();
                final R2<List<String>, String> listAndReason = entity.getValue();
                final List<String> list = listAndReason.get0();
                final String reason = listAndReason.get1();
                if (reason.equals("overlong")) {
                    continue;
                }
                if (list == null) {
                    continue;
                }
                if (NUMBERS.containsAll(key)) { // special check for items like 172
                    continue;
                }
                result.put(list.iterator().next(), key);
            }
            return result;
        }

        private void addAlias(String title, String localeID, String targetID) {
            ltp.set(localeID);
            Set<String> languages = addExtras(ltp.getLanguage(), goodToBadLanguages);
            Set<String> scripts = addExtras(ltp.getScript(), goodToBadScripts);
            Set<String> territories = addExtras(ltp.getRegion(), goodToBadTerritories);
            for (String language : languages) {
                try {
                    ltp.set(language); // whole language tag
                } catch (Exception e) {
                    continue;
                }
                if (!ltp.getVariants().isEmpty()) { // skip variants
                    continue;
                }
                for (String script : scripts) {
                    ltp.setScript(script);
                    for (String territory : territories) {
                        ltp.setRegion(territory);
                        String newTag = ltp.toString().replace('-', '_');
                        main: {
                            if (newTag.equals(targetID)) {
                                break main;
                            }
                            String old = aliasMap.get(newTag);
                            if (old != null) {
                                if (!old.equals(targetID)) {
                                    System.out.println(newTag + "\t→\t" + targetID + "\tconflict with\t" + old);
                                }
                                break main;
                            }
                            final boolean wholeAlias = isWholeAlias(factory, newTag);
                            if (!available.contains(newTag) || wholeAlias) {
                                System.out.println(title + "\t" + newTag + "\t→\t" + targetID
                                    + (wholeAlias ? "\talias-already" : ""));
                                aliasMap.put(newTag, targetID);
                            }
                        }
                    }
                }
            }
        }

        /*
         * Problems
         * missingLikely tl
         * missingLikely tl_PH
         * sr_YU -> conflict with sr
         * sr_CS -> conflict with sr_Cyrl_CS
         * sr_CS -> conflict with sr_Cyrl_CS
         * sh_CS -> conflict with sr_Latn_CS
         * sh_YU -> conflict with sr_Latn_RS
         */

        private Set<String> addExtras(String language, Relation<String, String> goodToBadLanguages) {
            Set<String> languages = new TreeSet<String>();
            languages.add(language);
            Set<String> badLanguages = goodToBadLanguages.get(language);
            if (badLanguages != null) {
                languages.addAll(badLanguages);
            }
            return languages;
        }

        Map<String, Boolean> wholeAliasCache = new HashMap<String, Boolean>();

        private boolean isWholeAlias(Factory factory, String localeID) {
            Boolean result = wholeAliasCache.get(localeID);
            if (result != null) {
                return result;
            }
            CLDRFile cldrFile;
            try {
                cldrFile = factory.make(localeID, false);
            } catch (Exception e) {
                wholeAliasCache.put(localeID, false);
                return false;
            }
            for (String path : cldrFile) {
                if (path.startsWith("//ldml/identity")) {
                    continue;
                } else if (path.startsWith("//ldml/alias")) {
                    wholeAliasCache.put(localeID, true);
                    return true;
                }
                wholeAliasCache.put(localeID, false);
                return false;
            }
            wholeAliasCache.put(localeID, false);
            return false;
        }
    }
}
