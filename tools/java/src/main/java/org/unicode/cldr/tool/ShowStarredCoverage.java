package org.unicode.cldr.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.LengthFirstComparator;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;

public class ShowStarredCoverage {
    static final CLDRConfig config = CLDRConfig.getInstance();

    enum MyOptions {
        language(".*", "it", "language to gather coverage data for"), tag(".*", null, "gather data on language tags"), dtdTypes(".*", "ldml",
            "dtdTypes, comma separated."),
        //filter(".*", "en_001", "locale ancestor"),
        ;

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = new Option(this, argumentPattern, defaultArgument, helpText);
        }

        static Options myOptions = new Options();
        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args, boolean showArguments) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    static final PathStarrer pathStarrer = new PathStarrer().setSubstitutionPattern("*");
    static final Factory phf = PathHeader.getFactory(config.getEnglish());
    static final SupplementalDataInfo sdi = config.getSupplementalDataInfo();

    public static void main(String[] args) {
        MyOptions.parse(args, true);

        if (MyOptions.tag.option.doesOccur()) {
            new LanguageTagCollector().getLanguageTags();
            return;
        }

        Set<DtdType> dtdTypes = EnumSet.noneOf(DtdType.class);
        String[] values = MyOptions.dtdTypes.option.getValue().split("[, ]+");
        for (String value : values) {
            dtdTypes.add(DtdType.valueOf(value));
        }

        final String fileLocale = MyOptions.language.option.getValue();

        M3<Level, PathHeader, Boolean> levelToPathHeaders = ChainedMap.of(
            new TreeMap<Level, Object>(),
            new TreeMap<PathHeader, Object>(),
            Boolean.class);

        for (DtdType dtdType : DtdType.values()) {
            if (dtdTypes != null && !dtdTypes.contains(dtdType)) {
                continue;
            }
            for (String dir : dtdType.directories) {
                if (dtdType == DtdType.ldml) {
                    doLdml(dir, fileLocale, levelToPathHeaders);
                } else {
                    doNonLdml(dtdType, dir, fileLocale, levelToPathHeaders);
                }
            }
        }
        System.out.println("№\tLevel\tSection|Page\tStarredPath");
        for (Entry<Level, Map<PathHeader, Boolean>> levelAndPathHeader : levelToPathHeaders) {
            Level level = levelAndPathHeader.getKey();
            Map<PathHeader, Boolean> pathHeaders2 = levelAndPathHeader.getValue();
            Counter<String> codeCount = new Counter<>();
            for (PathHeader ph : pathHeaders2.keySet()) {
                codeCount.add(condense(ph), 1);
            }
            showResults("code count", level, codeCount);
        }
    }

    static final Set<PageId> MainDateTimePages = EnumSet.of(PageId.Fields, PageId.Gregorian, PageId.Generic);

    private static String condense(PathHeader ph) {
        // TODO Auto-generated method stub
        String starredPath = pathStarrer.set(ph.getOriginalPath()).toString();
        starredPath = starredPath.replace("[@alt=\"*\"]", "");// collapse alts
        SectionId sectionId = ph.getSectionId();
        PageId pageId = ph.getPageId();
        String category = sectionId + "|" + pageId;
        switch (sectionId) {
        case Core_Data:
            category = sectionId.toString();
            break;
        case Currencies:
            category = sectionId + " — " + (starredPath.contains("@count") ? "long name" : starredPath.contains("/symbol") ? "symbol" : "name");
            break;
        case DateTime:
            category = sectionId + " — " + (starredPath.contains("/displayName") ? "field labels"
                : starredPath.contains("/interval") ? "intervals"
                    : pageId == PageId.Fields ? "relative"
                        : "basic");
            category += MainDateTimePages.contains(pageId) ? "" : " (non-greg)";
            break;
        case Locale_Display_Names:
            category = "Names — " + (starredPath.contains("/subdivision") ? "Country subdivisions"
                : pageId == PageId.Territories ? "Continents & Sub~"
                    : pageId.toString().startsWith("Territor") ? "Countries"
                        : pageId.toString());
            break;
        case Numbers:
            category = pageId == PageId.Compact_Decimal_Formatting ? (starredPath.contains("currency") ? "Currency" : "Number") + " Formats — compact"
                : sectionId.toString();
            break;
        case Misc:
            category = starredPath.contains("/annotation") ? "Emoji " + (starredPath.contains("@type") ? "names" : "keywords")
                : starredPath.contains("/characterLabel") ? "Character Labels"
                    : PageId.LinguisticElements.toString();
            break;
        case Timezones:
            category = sectionId.toString();
            break;
        case Units:
            category = sectionId.toString();
            break;
        }
        return stripParens(category + "\t" + starredPath);
    }

    static final Pattern PARENS = Pattern.compile("\\s*\\(.*\\)");
    private static final boolean SHOW = false;

    private static String stripParens(String label) {
        if (label.contains("(")) {
            String newLabel = PARENS.matcher(label).replaceAll("");
            if (label.equals(newLabel)) {
                System.out.println(RegexUtilities.showMismatch(Pattern.compile(".*" + PARENS.toString() + ".*"), label));
            }
            return newLabel;
        } else {
            return label;
        }
    }

    private static void showResults(String title, Level level, Counter<String> counts) {
        for (String key : counts.keySet()) {
            long results = counts.get(key);
            System.out.println(results
                + "\t" + level
                + "\t" + key);
        }
    }

    private static void doNonLdml(DtdType dtdType, String dir, String fileLocale, M3<Level, PathHeader, Boolean> levelToPathHeaders) {
        Matcher localeMatch = Pattern.compile("\\b" + fileLocale + "\\b").matcher("");
        // Not keyed by locale, need to dig into data for that.
        for (String file : new File(CLDRPaths.COMMON_DIRECTORY + dir).list()) {
            if (!file.endsWith(".xml")) {
                continue;
            }

            if (file.startsWith("plural")) {
                int debug = 0;
            }

            List<Pair<String, String>> contents1;
            try {
                contents1 = XMLFileReader.loadPathValues(CLDRPaths.COMMON_DIRECTORY + dir + "/" + file, new ArrayList<Pair<String, String>>(), true);
            } catch (Exception e) {
                return;
            }
            DtdData dtdData = DtdData.getInstance(dtdType);
            Multimap<String, String> extras = TreeMultimap.create();

            for (Pair<String, String> s : contents1) {
                String path = s.getFirst();
                if (path.contains("it")) {
                    int debug = 0;
                }

                String value = s.getSecond();
                XPathParts pathPlain = XPathParts.getFrozenInstance(path);
                if (dtdData.isMetadata(pathPlain)) {
                    continue;
                }
                Set<String> pathForValues = dtdData.getRegularizedPaths(pathPlain, extras);
                if (pathForValues != null) {
                    for (String pathForValue : pathForValues) {
                        if (!localeMatch.reset(pathForValue).find() && !localeMatch.reset(value).find()) {
                            continue;
                        }
                        PathHeader pathHeader = phf.fromPath(pathForValue);
                        levelToPathHeaders.put(Level.UNDETERMINED, pathHeader, true);
                        Splitter splitter = DtdData.getValueSplitter(pathPlain);
                        for (String line : splitter.split(value)) {
                            // special case # in transforms
                            if (isComment(pathPlain, line)) {
                                continue;
                            }
                        }
                    }
                }
                for (Entry<String, Collection<String>> entry : extras.asMap().entrySet()) {
                    final String extraPath = entry.getKey();
                    for (String value2 : entry.getValue()) {
                        if (!localeMatch.reset(extraPath).find() && !localeMatch.reset(value2).find()) {
                            continue;
                        }
                        final PathHeader pathHeaderExtra = phf.fromPath(extraPath);
                        levelToPathHeaders.put(Level.UNDETERMINED, pathHeaderExtra, true);
//                    final Collection<String> extraValue = entry.getValue();
//                    if (isExtraSplit(extraPath)) {
//                        for (String items : extraValue) {
//                            results.putAll(pathHeaderExtra, DtdData.SPACE_SPLITTER.splitToList(items));
//                        }
//                    } else {
//                        results.putAll(pathHeaderExtra, extraValue);
//                    }
                    }
                }
            }
        }
    }

    static boolean isExtraSplit(String extraPath) {
        if (extraPath.endsWith("/_type") && extraPath.startsWith("//supplementalData/metaZones/mapTimezones")) {
            return true;
        }
        return false;
    }

    public static boolean isComment(XPathParts pathPlain, String line) {
        if (pathPlain.contains("transform")) {
            if (line.startsWith("#")) {
                return true;
            }
        }
        return false;
    }

    private static void doLdml(String dir, String fileLocale, M3<Level, PathHeader, Boolean> levelToPathHeaders) {
        Status status = new Status();
        boolean isMain = "main".equals(dir);
        System.out.println("directory:\t" + dir);
        final org.unicode.cldr.util.Factory cldrFactory = org.unicode.cldr.util.Factory.make(
            CLDRPaths.COMMON_DIRECTORY + dir, fileLocale, DraftStatus.unconfirmed);
        CLDRFile file;
        try {
            file = cldrFactory.make(fileLocale, isMain); // bug, resolving source doesn't work without directory
        } catch (Exception e) {
            System.out.println(Level.UNDETERMINED + "\tNo file " + dir + "/" + fileLocale + ".xml");
            return;
        }
        M4<Level, String, String, Boolean> levelToData = ChainedMap.of(
            new TreeMap<Level, Object>(),
            new TreeMap<String, Object>(),
            new TreeMap<String, Object>(),
            Boolean.class);

        Counter<Level> counter = new Counter<>();
        TreeSet<PathHeader> pathHeaders = new TreeSet<>();
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

            Level level = config.getSupplementalDataInfo().getCoverageLevel(path, fileLocale); // isMain ? ... : Level.UNDETERMINED;
            if (level.compareTo(Level.MODERN) > 0) {
                continue;
            }
            levelToPathHeaders.put(level, ph, true);
            pathHeaders.add(ph);
            SurveyToolStatus stStatus = ph.getSurveyToolStatus();
            String starred = pathStarrer.set(path);
            String attributes = CollectionUtilities.join(pathStarrer.getAttributes(), "|");
            levelToData.put(level, starred + "|" + stStatus + "|" + requiredVotes, attributes, Boolean.TRUE);
            counter.add(level, 1);
        }
        if (SHOW) {
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
                    if (true) {
                        String samples = CollectionUtilities.join(attributes.keySet(), ", ");
                        if (samples.length() > 50) samples = samples.substring(0, 50) + "…";
                        System.out.println(count
                            + "\t" + level
                            + "\t" + starredStatus[0]
                            + "\t" + starredStatus[1]
                            + "\t" + starredStatus[2]
                            + "\t" + samples);
                    }
                }
            }
        }
//        for (Entry<Level, Map<PathHeader, Boolean>> levelAndPathHeader : levelToPathHeaders) {
//            Level level = levelAndPathHeader.getKey();
//            Map<PathHeader, Boolean> pathHeaders2 = levelAndPathHeader.getValue();
//            Builder<String, String> pageCount = ImmutableMultimap.builder();
//            for (PathHeader ph : pathHeaders2.keySet()) {
//                pageCount.put(ph.getSectionId() + "\t" + ph.getPageId(), ph.getHeader() + " : " + ph.getCode());
//            }
//            showResults("header+code count", level, pageCount.build());
//        }
    }

    static class LanguageTagCollector {
        private static final CLDRConfig CldrConfig = CLDRConfig.getInstance();

        enum Source {
            main, canon, supp, seed, exemplars, keyboards, alias
        }

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
                addLanguage(fileName.substring(0, fileName.length() - 4), source);
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
