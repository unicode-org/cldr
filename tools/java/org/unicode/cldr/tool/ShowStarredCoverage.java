package org.unicode.cldr.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.LengthFirstComparator;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row.R2;

public class ShowStarredCoverage {
    static final CLDRConfig config = CLDRConfig.getInstance();

    public static void main(String[] args) {
        M4<Level, String, String, Boolean> levelToData = ChainedMap.of(
            new TreeMap<Level,Object>(), 
            new TreeMap<String,Object>(), 
            new TreeMap<String,Object>(), 
            Boolean.class);

        if (true) {
            new LanguageTagCollector().getLanguageTags();
            return;
        }
        final String fileLocale = "ar";
        PathStarrer pathStarrer = new PathStarrer().setSubstitutionPattern("*");
        Status status = new Status();
        Counter<Level> counter = new Counter();
        Factory phf = PathHeader.getFactory(config.getEnglish());
        TreeSet<PathHeader> pathHeaders = new TreeSet();
        SupplementalDataInfo sdi = config.getSupplementalDataInfo();

        CLDRFile file = config.getCldrFactory().make(fileLocale, true);
        for (String path : file) {
            if (path.endsWith("/alias") || path.startsWith("//ldml/identity")) {
                continue;
            }
            String locale = file.getSourceLocaleID(path, status);
            if (!path.equals(status.pathWhereFound)) {
                // path is aliased, skip
                continue;
            }
            if (config.getSupplementalDataInfo().isDeprecated(DtdType.ldml, path)) {
                continue;
            }
            PathHeader ph = phf.fromPath(path);
            CLDRLocale loc = CLDRLocale.getInstance(fileLocale);
            int requiredVotes = sdi.getRequiredVotes(loc, ph);

            Level level = config.getSupplementalDataInfo().getCoverageLevel(path, fileLocale);
            if (level.compareTo(Level.MODERN) <= 0) {
                pathHeaders.add(ph);
            }
            SurveyToolStatus stStatus = ph.getSurveyToolStatus();
            String starred = pathStarrer.set(path);
            String attributes = CollectionUtilities.join(pathStarrer.getAttributes(),"|");
            levelToData.put(level, starred + "|" + stStatus + "|" + requiredVotes, attributes, Boolean.TRUE);
            counter.add(level, 1);
        }
        for (Level level : Level.values()) {
            System.out.println(counter.get(level) + "\t" + level);
        }
        for (Entry<Level, Map<String, Map<String, Boolean>>> entry : levelToData) {
            Level level = entry.getKey();
            for (Entry<String, Map<String, Boolean>> entry2 : entry.getValue().entrySet()) {
                String[] starredStatus = entry2.getKey().split("\\|");
                Map<String, Boolean> attributes = entry2.getValue();
                int count = attributes.size();
                if (count < 1) {
                    count = 1;
                }
                System.out.println(count 
                    + "\t" + level 
                    + "\t" + starredStatus[0] 
                        + "\t" + starredStatus[1]
                            + "\t" + starredStatus[2]
                                + "\t" + CollectionUtilities.join(attributes.keySet(), ", "));
            }
        }
        Counter<String> pageCount = new Counter<>();
        Counter<String> codeCount = new Counter<>();
        for (PathHeader ph : pathHeaders) {
            pageCount.add(ph.getSection() + "\t" + ph.getPage(), 1);
            codeCount.add(ph.getSection() + "\t" + ph.getPage() + "\t" + ph.getHeader(), 1);
        }
        System.out.println("\n*Page Count");
        for (String line : pageCount) {
            System.out.println(pageCount.getCount(line) + "\t" + line);
        }
        System.out.println("\n*Code Count");
        for (String line : codeCount) {
            System.out.println(codeCount.getCount(line) + "\t" + line);
        }
    }

    static class LanguageTagCollector {
        private static final CLDRConfig CldrConfig = CLDRConfig.getInstance();

        enum Source {main, canon, supp, seed, exemplars, keyboards, alias}
        LanguageTagParser ltp = new LanguageTagParser();
        LanguageTagCanonicalizer ltc = new LanguageTagCanonicalizer();
        Relation<String, Source> languageTags = Relation.of(new TreeMap(new LengthFirstComparator()), TreeSet.class);
        final SupplementalDataInfo supp = CldrConfig.getSupplementalDataInfo();
        final Map<String, R2<List<String>, String>> languageFix = supp.getLocaleAliasInfo().get("language");

        private void getLanguageTags() {

            Map<String, String> likely = supp.getLikelySubtags();
            for (Entry<String, String> entry : likely.entrySet()) {
                addLanguage(entry.getKey(), Source.canon);
            }
            for (String entry : supp.getLanguagesForTerritoriesPopulationData()) {
                addLanguage(entry, Source.supp);
            }
            for (String entry : supp.getLanguages()) {
                addLanguage(entry, Source.supp);
            }
            for (String entry : supp.getBasicLanguageDataLanguages()) {
                addLanguage(entry, Source.supp);
            }

            for (Entry<String, R2<List<String>, String>> entry : languageFix.entrySet()) {
                final String lang = entry.getKey();
                if (!lang.contains("_")) {
                    addLanguage(lang, Source.alias);
                }
            }
            // just use filenames
            File base = CldrConfig.getCldrBaseDirectory();
            // System.out.println(base);
            // just do main, exemplars/main, seed/main, keyboards/.*
            addFiles(base, "common/main", Source.main);
            addFiles(base, "exemplars/main", Source.exemplars);
            addFiles(base, "seed/main", Source.seed);
            addFiles(base, "keyboards", Source.keyboards);

            Set<String> badLines = new LinkedHashSet();
            
            for (Entry<String, Set<Source>> entry : languageTags.keyValuesSet()) {
                final String written = entry.getKey();
                final String name = getName(written);
                Set<Source> source = entry.getValue();
                if (source.contains(Source.alias) && source.size() > 1) {
                    badLines.add(written 
                        + "\t" + name 
                        + "\t" + languageFix.get(written).get0()
                        + "\t" + CollectionUtilities.join(source, " "));
                    source = Collections.singleton(Source.alias);
                }
                System.out.println(written 
                    + "\t" + name 
                    + "\t" + CollectionUtilities.join(source, " "));
            }
            for (String s : badLines) {
                System.out.println("BAD:\t" + s);
            }
        }

        public String getName(final String written) {
            String result = CldrConfig.getEnglish().getName(written);
            if (result.equals(written)) {
                R2<List<String>, String> alias = languageFix.get(written);
                if (alias != null) {
                    result = CldrConfig.getEnglish().getName(alias.get0().get(0));
                }
            }
            return result;
        }

        private void addFiles(File base, String name, Source source) {
            addFiles(new File(base, name), source);
        }
        private void addFiles(File base, Source source) {
            if (!base.isDirectory()) {
                return;
            }
            for (File file : base.listFiles()) {
                if (file.isDirectory()) {
                    addFiles(file, source);
                    continue;
                }
                String fileName = file.getName();
                if (!fileName.endsWith(".xml") || fileName.startsWith("_")) {
                    continue;
                }
                addLanguage(fileName.substring(0, fileName.length()-4), source);
            }

        }

        private void addLanguage(String key, Source source) {
            if (key.startsWith("und") || key.startsWith("root")) {
                languageTags.put("und", source);
                return;
            }
            ltp.set(key);
            languageTags.put(ltp.getLanguage(), source);
        }
    }
}
