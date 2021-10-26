package org.unicode.cldr.json;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import com.ibm.icu.number.IntegerWidth;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.NoUnit;

/**
 * Utility methods to extract data from CLDR repository and export it in JSON
 * format.
 *
 * @author shanjian / emmons
 */
@CLDRTool(alias = "ldml2json", description = "Convert CLDR data to JSON")
public class Ldml2JsonConverter {
    private static final String CLDR_PKG_PREFIX = "cldr-";
    private static final String FULL_TIER_SUFFIX = "-full";
    private static final String MODERN_TIER_SUFFIX = "-modern";
    private static Logger logger = Logger.getLogger(Ldml2JsonConverter.class.getName());

    enum RunType {
        all,
        main,
        supplemental(false, false), // aka 'core'
        segments, rbnf(false, true), annotations, annotationsDerived, bcp47(false, false);

        private final boolean isTiered;
        private final boolean hasLocales;
        RunType() {
            this.isTiered = true;
            this.hasLocales = true;
        }
        RunType(boolean isTiered, boolean hasLocales) {
            this.isTiered = isTiered;
            this.hasLocales = hasLocales;
        }
        /**
         * Is it split into modern/full?
         * @return
         */
        public boolean tiered() {
            return isTiered;
        }
        /**
         * Does it have locale IDs?
         * @return
         */
        public boolean locales() {
            return hasLocales;
        }
        /**
         * return the options as a pipe-delimited list
         * @return
         */
        public static String valueList() {
            return String.join("|", Lists.newArrayList(
                RunType.values())
                .stream()
                .map(t -> t.name())
                .toArray(String[]::new));
        }
    }

    private static final StandardCodes sc = StandardCodes.make();
    private Set<String> defaultContentLocales = SupplementalDataInfo.getInstance().getDefaultContentLocales();
    private Set<String> skippedDefaultContentLocales = new TreeSet<>();

    private class AvailableLocales {
        Set<String> modern = new TreeSet<>();
        Set<String> full = new TreeSet<>();
    }

    private AvailableLocales avl = new AvailableLocales();
    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Options options = new Options(
        "Usage: LDML2JsonConverter [OPTIONS] [FILES]\n" +
            "This program converts CLDR data to the JSON format.\n" +
            "Please refer to the following options. \n" +
            "\texample: org.unicode.cldr.json.Ldml2JsonConverter -c xxx -d yyy")
                .add("bcp47", 'B', "(true|false)", "true",
                    "Whether to strictly use BCP47 tags in filenames and data. Defaults to true.")
                .add("bcp47-no-subtags", 'T', "(true|false)", "true",
                    "In BCP47 mode, ignore locales with subtags such as en-US-u-va-posix. Defaults to true.")
                .add("commondir", 'c', ".*", CLDRPaths.COMMON_DIRECTORY,
                    "Common directory for CLDR files, defaults to CldrUtility.COMMON_DIRECTORY")
                .add("destdir", 'd', ".*", CLDRPaths.GEN_DIRECTORY,
                    "Destination directory for output files, defaults to CldrUtility.GEN_DIRECTORY")
                .add("match", 'm', ".*", ".*",
                    "Regular expression to define only specific locales or files to be generated")
                .add("type", 't', "("+RunType.valueList()+")", "all",
                    "Type of CLDR data being generated, such as main, supplemental, or segments. All gets all.")
                .add("resolved", 'r', "(true|false)", "false",
                    "Whether the output JSON for the main directory should be based on resolved or unresolved data")
                .add("draftstatus", 's', "(approved|contributed|provisional|unconfirmed)", "unconfirmed",
                    "The minimum draft status of the output data")
                .add("coverage", 'l', "(minimal|basic|moderate|modern|comprehensive|optional)", "optional",
                    "The maximum coverage level of the output data")
                .add("packagelist", 'P', "(true|false)", "true",
                    "Whether to output PACKAGES.md and cldr-core/cldr-packages.json (during supplemental/cldr-core)")
                .add("fullnumbers", 'n', "(true|false)", "false",
                    "Whether the output JSON should output data for all numbering systems, even those not used in the locale")
                .add("other", 'o', "(true|false)", "false",
                    "Whether to write out the 'other' section, which contains any unmatched paths")
                .add("packages", 'p', "(true|false)", "false",
                    "Whether to group data files into installable packages")
                .add("identity", 'i', "(true|false)", "true",
                    "Whether to copy the identity info into all sections containing data")
                .add("konfig", 'k', ".*", null, "LDML to JSON configuration file")
                .add("pkgversion",  'V', ".*", getDefaultVersion(), "Version to be used in writing package files");

    public static void main(String[] args) throws Exception {
        options.parse(args, true);

        Timer overallTimer = new Timer();
        overallTimer.start();
        final String rawType = options.get("type").getValue();

        if (RunType.all.name().equals(rawType)) {
            for(final RunType t : RunType.values()) {
                if (t == RunType.all) continue;
                System.out.println();
                System.out.println("#######################  " + t + " #######################");
                Timer subTimer = new Timer();
                subTimer.start();
                processType(t.name());
                System.out.println(t + "\tFinished in " + subTimer.toMeasureString());
                System.out.println();
            }
        } else {
            processType(rawType);
        }

        System.out.println("\n\n###\n\nFinished everything in " + overallTimer.toMeasureString());
    }

    static void processType(final String runType) throws Exception {
        Ldml2JsonConverter l2jc = new Ldml2JsonConverter(
            options.get("commondir").getValue(),
            options.get("destdir").getValue(),
            runType,
            Boolean.parseBoolean(options.get("fullnumbers").getValue()),
            Boolean.parseBoolean(options.get("resolved").getValue()),
            options.get("coverage").getValue(),
            options.get("match").getValue(),
            Boolean.parseBoolean(options.get("packages").getValue()),
            options.get("konfig").getValue(),
            options.get("pkgversion").getValue(),
            Boolean.parseBoolean(options.get("bcp47").getValue()),
            Boolean.parseBoolean(options.get("bcp47-no-subtags").getValue())
        );

        DraftStatus status = DraftStatus.valueOf(options.get("draftstatus").getValue());
        l2jc.processDirectory(runType, status);
    }

    // The CLDR file directory where those official XML files will be found.
    private String cldrCommonDir;
    // Where the generated JSON files will be stored.
    private String outputDir;
    // Whether data in main should output all numbering systems, even those not in use in the locale.
    private boolean fullNumbers;
    // Whether data in main should be resolved for output.
    private boolean resolve;
    // Used to match specific locales for output
    private String match;
    // Used to filter based on coverage
    private int coverageValue;
    // Whether we should write output files into installable packages
    private boolean writePackages;
    // Type of run for this converter: main, supplemental, or segments
    final private RunType type;

    static class JSONSection implements Comparable<JSONSection> {
        public String section;
        public Pattern pattern;
        public String packageName;

        @Override
        public int compareTo(JSONSection other) {
            return section.compareTo(other.section);
        }
    }

    private Map<String, String> dependencies;
    private List<JSONSection> sections;
    private Set<String> packages;
    final private String pkgVersion;
    final private boolean strictBcp47;
    final private boolean skipBcp47LocalesWithSubtags;
    private LdmlConfigFileReader configFileReader;

    public Ldml2JsonConverter(String cldrDir, String outputDir, String runType, boolean fullNumbers, boolean resolve, String coverage, String match,
        boolean writePackages, String configFile, String pkgVersion,
        boolean strictBcp47, boolean skipBcp47LocalesWithSubtags) {
        this.strictBcp47 = strictBcp47;
        this.skipBcp47LocalesWithSubtags = strictBcp47 && skipBcp47LocalesWithSubtags;
        this.cldrCommonDir = cldrDir;
        this.outputDir = outputDir;
        try {
            this.type = RunType.valueOf(runType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RuntimeException("runType (-t) invalid: " + runType + " must be one of " + RunType.valueList(),
                e);
        }
        this.fullNumbers = fullNumbers;
        this.resolve = resolve;
        this.match = match;
        this.writePackages = writePackages;
        this.coverageValue = Level.get(coverage).getLevel();
        this.pkgVersion = pkgVersion;

        LdmlConvertRules.addVersionHandler(pkgVersion.split("\\.")[0]);

        configFileReader = new LdmlConfigFileReader();
        configFileReader.read(configFile, type);
        this.dependencies = configFileReader.getDependencies();
        this.sections = configFileReader.getSections();
        this.packages = new TreeSet<>();
    }

    /**
     * @see XPathParts#addInternal
     */
    static final Pattern ANNOTATION_CP_REMAP = PatternCache.get("^(.*)\\[@cp=\"(\\[|\\]|'|\"|@|/|=)\"\\](.*)$");

    /**
     * Transform the path by applying PATH_TRANSFORMATIONS rules.
     *
     * @param pathStr
     *            The path string being transformed.
     * @return The transformed path.
     */
    private String transformPath(final String pathStr, final String pathPrefix) {
        String result = pathStr;

        // handle annotation cp value
        Matcher cpm = ANNOTATION_CP_REMAP.matcher(result);
        if (cpm.matches()) {
            // We need to avoid breaking the syntax not just of JSON, but of XPATH.
            final String badCodepointRange = cpm.group(2);
            StringBuilder sb = new StringBuilder(cpm.group(1))
                .append("[@cp=\"");
            // JSON would handle a wide range of things if escaped, but XPATH will not.
            if (badCodepointRange.codePointCount(0, badCodepointRange.length()) != 1) {
                // forbid more than one U+ (because we will have to unescape it.)
                throw new IllegalArgumentException("Need exactly one codepoint in the @cp string, but got " + badCodepointRange + " in xpath " + pathStr);
            }
            badCodepointRange.codePoints().forEach(cp -> sb.append("U+").append(Integer.toHexString(cp).toUpperCase()));
            sb.append("\"]").append(cpm.group(3));
            result = sb.toString();
        }

        logger.finest(" IN pathStr : " + result);
        result = LdmlConvertRules.PathTransformSpec.applyAll(result);
        result = result.replaceFirst("/ldml/", pathPrefix);
        result = result.replaceFirst("/supplementalData/", pathPrefix);

        if (result.startsWith("//cldr/supplemental/references/reference")) {
            // no change
        } else if (strictBcp47) {
            // Look for something like <!--@MATCH:set/validity/locale--> in DTD
            if (result.contains("localeDisplayNames/languages/language")) {
                if (result.contains("type=\"root\"")) {
                    // This is strictBcp47
                    // Drop translation for 'root' as it conflicts with 'und'
                    return ""; // 'drop this path'
                }
                result = fixXpathBcp47(result, "language", "type");
            } else if (result.contains("likelySubtags/likelySubtag")) {
                if (!result.contains("\"iw\"") &&
                    !result.contains("\"in\"") &&
                    !result.contains("\"ji\"")) {
                    // Special case: preserve 'iw' and 'in' likely subtags
                    result = fixXpathBcp47(result, "likelySubtag", "from", "to");
                } else {
                    result = underscoreToHypen(result);
                    logger.warning("Including aliased likelySubtags: " + result);
                }
            } else if (result.startsWith("//cldr/supplemental/weekData/weekOfPreference")) {
                result = fixXpathBcp47(result, "weekOfPreference", "locales");
            } else if (result.startsWith("//cldr/supplemental/metadata/defaultContent")) {
                result = fixXpathBcp47(result, "defaultContent", "locales");
            } else if (result.startsWith("//cldr/supplemental/grammatical") && result.contains("Data/grammaticalFeatures")) {
                result = fixXpathBcp47(result, "grammaticalFeatures", "locales");
            } else if (result.startsWith("//cldr/supplemental/grammatical") && result.contains("Data/grammaticalDerivations")) {
                result = fixXpathBcp47(result, "grammaticalDerivations", "locales");
            } else if (result.startsWith("//cldr/supplemental/dayPeriodRuleSet")) {
                result = fixXpathBcp47(result, "dayPeriodRules", "locales");
            } else if (result.startsWith("//cldr/supplemental/plurals")) {
                result = fixXpathBcp47(result, "pluralRules", "locales");
            } else if (result.startsWith("//cldr/supplemental/timeData/hours")) {
                result = fixXpathBcp47MishMash(result, "hours", "regions");
            } else if (result.startsWith("//cldr/supplemental/parentLocales/parentLocale")) {
                result = fixXpathBcp47(result, "parentLocale", "parent", "locales");
            } else if (result.startsWith("//cldr/supplemental/territoryInfo/territory/languagePopulation")) {
                result = fixXpathBcp47(result, "languagePopulation", "type");
            } else if (result.contains("languages") ||
            result.contains("languageAlias") ||
            result.contains("languageMatches") ||
            result.contains("likelySubtags") ||
            result.contains("parentLocale") ||
            result.contains("locales=")) {
                final String oldResult = result;
                result = underscoreToHypen(result);
                if (!oldResult.equals(result)) {
                    logger.fine(oldResult + " => " + result);
                }
            }
        } else if (result.contains("languages") ||
            result.contains("languageAlias") ||
            result.contains("languageMatches") ||
            result.contains("likelySubtags") ||
            result.contains("parentLocale") ||
            result.contains("locales=")) {
            // old behavior: just munge paths..
            result = underscoreToHypen(result);
        }
        logger.finest("OUT pathStr : " + result);
        logger.finest("result: " + result);
        return result;
    }

    private Map<JSONSection, List<CldrItem>> mapPathsToSections(AtomicInteger readCount, int totalCount,
        CLDRFile file, String pathPrefix, SupplementalDataInfo sdi)
        throws IOException, ParseException {
        final Map<JSONSection, List<CldrItem>> sectionItems = new TreeMap<>();

        String locID = file.getLocaleID();
        Matcher noNumberingSystemMatcher = LdmlConvertRules.NO_NUMBERING_SYSTEM_PATTERN.matcher("");
        Matcher numberingSystemMatcher = LdmlConvertRules.NUMBERING_SYSTEM_PATTERN.matcher("");
        Matcher rootIdentityMatcher = LdmlConvertRules.ROOT_IDENTITY_PATTERN.matcher("");
        Set<String> activeNumberingSystems = new TreeSet<>();
        activeNumberingSystems.add("latn"); // Always include latin script numbers
        for (String np : LdmlConvertRules.ACTIVE_NUMBERING_SYSTEM_XPATHS) {
            String ns = file.getWinningValue(np);
            if (ns != null && ns.length() > 0) {
                activeNumberingSystems.add(ns);
            }
        }
        final DtdType fileDtdType = file.getDtdType();
        CoverageInfo covInfo = CLDRConfig.getInstance().getCoverageInfo();
        for (Iterator<String> it = file.iterator("", DtdData.getInstance(fileDtdType).getDtdComparator(null)); it.hasNext();) {
            int cv = Level.UNDETERMINED.getLevel();
            final String path = it.next();
            String fullPath = file.getFullXPath(path);
            String value = file.getWinningValue(path);
            if (path.startsWith("//ldml/localeDisplayNames/languages") &&
                file.getSourceLocaleID(path, null).equals("code-fallback")) {
                value = file.getConstructedBaileyValue(path, null, null);
            }

            if (fullPath == null) {
                fullPath = path;
            }

            if (!CLDRFile.isSupplementalName(locID) && path.startsWith("//ldml/") && !path.contains("/identity")) {
                cv = covInfo.getCoverageValue(path, locID);
            }
            if (cv > coverageValue) {
                continue;
            }
            // Discard root identity element unless the locale is root
            rootIdentityMatcher.reset(fullPath);
            if (rootIdentityMatcher.matches() && !"root".equals(locID)) {
                continue;
            }

            // automatically filter out number symbols and formats without a numbering system
            noNumberingSystemMatcher.reset(fullPath);
            if (noNumberingSystemMatcher.matches()) {
                continue;
            }

            // Filter out non-active numbering systems data unless fullNumbers is specified.
            numberingSystemMatcher.reset(fullPath);
            if (numberingSystemMatcher.matches() && !fullNumbers) {
                XPathParts xpp = XPathParts.getFrozenInstance(fullPath);
                String currentNS = xpp.getAttributeValue(2, "numberSystem");
                if (currentNS != null && !activeNumberingSystems.contains(currentNS)) {
                    continue;
                }
            }

            // Handle the no inheritance marker.
            if (resolve && CldrUtility.NO_INHERITANCE_MARKER.equals(value)) {
                continue;
            }

            String transformedPath = transformPath(path, pathPrefix);
            String transformedFullPath = transformPath(fullPath, pathPrefix);

            if (transformedPath.isEmpty()) {
                continue; // skip this path
            }

            for (JSONSection js : sections) {
                if (js.pattern.matcher(transformedPath).matches()) {
                    CldrItem item = new CldrItem(transformedPath, transformedFullPath, path, fullPath, value);

                    List<CldrItem> cldrItems = sectionItems.get(js);
                    if (cldrItems == null) {
                        cldrItems = new ArrayList<>();
                    }
                    cldrItems.add(item);
                    sectionItems.put(js, cldrItems);
                    break;
                }
            }
        }

        Matcher versionInfoMatcher = PatternCache.get(".*/(identity|version).*").matcher("");
        // Automatically copy the version info to any sections that had real data in them.
        JSONSection otherSection = sections.get(sections.size() - 1);
        List<CldrItem> others = sectionItems.get(otherSection);
        if (others == null) {
            return sectionItems;
        }
        List<CldrItem> otherSectionItems = new ArrayList<>(others);
        int addedItemCount = 0;
        boolean copyIdentityInfo = Boolean.parseBoolean(options.get("identity").getValue());

        for (CldrItem item : otherSectionItems) {
            String thisPath = item.getPath();
            versionInfoMatcher.reset(thisPath);
            if (versionInfoMatcher.matches()) {
                for (JSONSection js : sections) {
                    if (sectionItems.get(js) != null && !js.section.equals("other") && copyIdentityInfo) {
                        List<CldrItem> hit = sectionItems.get(js);
                        hit.add(addedItemCount, item);
                        sectionItems.put(js, hit);
                    }
                    if (js.section.equals("other")) {
                        List<CldrItem> hit = sectionItems.get(js);
                        hit.remove(item);
                        sectionItems.put(js, hit);
                    }
                }
                addedItemCount++;
            }
        }
        return sectionItems;
    }

    final static Pattern HAS_SUBTAG = PatternCache.get(".*-[a-z]-.*");

    /**
     * Convert CLDR's XML data to JSON format.
     *
     * @param file
     *            CLDRFile object.
     * @param outFilename
     *            The file name used to save JSON data.
     * @throws IOException
     * @throws ParseException
     * @return total items written in all files. (if 0, file had no effect)
     */
    private int convertCldrItems(AtomicInteger readCount, int totalCount,
        String dirName, String filename, String pathPrefix,
        final Map<JSONSection, List<CldrItem>> sectionItems)
        throws IOException, ParseException {
        // zone and timezone items are queued for sorting first before they are
        // processed.

        final String filenameAsLangTag = unicodeLocaleToString(filename);

        if (skipBcp47LocalesWithSubtags &&
            type.locales() &&
            HAS_SUBTAG.matcher(filenameAsLangTag).matches()) {
            // Has a subtag, so skip it.
            // It will show up in the "no output" list.
            return 0;
        }

        int totalItemsInFile = 0;

        List<Pair<String,Integer>> outputProgress = new LinkedList<>();

        for (JSONSection js : sections) {
            if (js.section.equals("IGNORE")) {
                continue;
            }
            String outFilename;
            if (type == RunType.rbnf) {
                outFilename = filenameAsLangTag + ".json";
            } else if (type == RunType.bcp47) {
                outFilename = filename + ".json";
            } else if(js.section.equals("other")) {
                // If you see other-___.json, it means items that were missing from JSON_config_*.txt
                outFilename = js.section + "-" + filename + ".json"; // Use original filename
            } else {
                outFilename = js.section + ".json";
            }
            String tier = "";
            boolean writeOther = Boolean.parseBoolean(options.get("other").getValue());
            if (js.section.equals("other") && !writeOther) {
                continue;
            } else {
                StringBuilder outputDirname = new StringBuilder(outputDir);
                if (writePackages) {
                    if (type.tiered()) {
                        LocaleIDParser lp = new LocaleIDParser();
                        lp.set(filename);
                        if (defaultContentLocales.contains(filename) &&
                            lp.getRegion().length() > 0) {
                            if (type == RunType.main) {
                                skippedDefaultContentLocales.add(filenameAsLangTag);
                            }
                            continue;
                        }
                        final boolean isModernTier = localeIsModernTier(filename);
                        if (isModernTier) {
                            tier = MODERN_TIER_SUFFIX;
                            if (type == RunType.main) {
                                avl.modern.add(filenameAsLangTag);
                            }
                        } else {
                            tier = FULL_TIER_SUFFIX;
                        }
                        if (type == RunType.main) {
                            avl.full.add(filenameAsLangTag);
                        }
                    } else if (type == RunType.rbnf) {
                        js.packageName = "rbnf";
                        tier = "";
                    } else if (type == RunType.bcp47) {
                        js.packageName = "bcp47";
                        tier = "";
                    }
                    if (js.packageName != null) {
                        String packageName = CLDR_PKG_PREFIX + js.packageName + tier;
                        outputDirname.append("/" + packageName);
                        packages.add(packageName);
                    }
                    outputDirname.append("/" + dirName + "/");
                    if (type.tiered()) {
                        outputDirname.append(filenameAsLangTag);
                    }
                    logger.fine("outDir: " + outputDirname);
                    logger.fine("pack: " + js.packageName);
                    logger.fine("dir: " + dirName);
                } else {
                    outputDirname.append("/" + filename);
                }

                assert(tier.isEmpty() == !type.tiered());

                List<String> outputDirs = new ArrayList<>();
                outputDirs.add(outputDirname.toString());
                if (writePackages && tier.equals(MODERN_TIER_SUFFIX) && js.packageName != null) {
                    // if it is in 'modern', add it to 'full' also.
                    outputDirs.add(outputDirname.toString().replaceFirst(MODERN_TIER_SUFFIX, FULL_TIER_SUFFIX));
                    // Also need to make sure that the full package is added
                    packages.add(CLDR_PKG_PREFIX + js.packageName + FULL_TIER_SUFFIX);
                }

                for (String outputDir : outputDirs) {
                    List<CldrItem> theItems = sectionItems.get(js);
                    if (theItems == null || theItems.size() == 0) {
                        logger.fine(() -> ">" + progressPrefix(readCount, totalCount) +
                            outputDir + " - no items to write in " + js.section); // mostly noise
                        continue;
                    }
                    logger.fine(() -> ("?" + progressPrefix(readCount, totalCount, filename, js.section) +
                         " - " + theItems.size() + " item(s)" + "\r"));
                    // Create the output dir if it doesn't exist
                    File dir = new File(outputDir.toString());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    JsonObject out = new JsonObject(); // root object for writing

                    ArrayList<CldrItem> sortingItems = new ArrayList<>();
                    ArrayList<CldrItem> arrayItems = new ArrayList<>();

                    ArrayList<CldrNode> nodesForLastItem = new ArrayList<>();
                    String lastLeadingArrayItemPath = null;
                    String leadingArrayItemPath = "";
                    int valueCount = 0;
                    String previousIdentityPath = null;
                    for (CldrItem item : theItems) {
                        if (item.getPath().isEmpty()) {
                            throw new IllegalArgumentException("empty xpath in " + filename + " section " + js.packageName + "/" + js.section);
                        }
                        if (type == RunType.rbnf) {
                            item.adjustRbnfPath();
                        }

                        // items in the identity section of a file should only ever contain the lowest level, even if using
                        // resolving source, so if we have duplicates ( caused by attributes used as a value ) then suppress
                        // them here.
                        if (item.getPath().contains("/identity/")) {
                            String[] parts = item.getPath().split("\\[");
                            if (parts[0].equals(previousIdentityPath)) {
                                continue;
                            } else {
                                XPathParts xpp = XPathParts.getFrozenInstance(item.getPath());
                                String territory = xpp.findAttributeValue("territory", "type");
                                LocaleIDParser lp = new LocaleIDParser().set(filename);
                                if (territory != null && territory.length() > 0 && !territory.equals(lp.getRegion())) {
                                    continue;
                                }
                                previousIdentityPath = parts[0];
                            }
                        }

                        // some items need to be split to multiple item before processing. None
                        // of those items need to be sorted.
                        // Applies to SPLITTABLE_ATTRS attributes.
                        CldrItem[] items = item.split();
                        if (items == null) {
                            // Nothing to split. Make it a 1-element array.
                            items = new CldrItem[1];
                            items[0] = item;
                        }
                        valueCount += items.length;

                        // Hard code this part.
                        if (item.getUntransformedPath().contains("unitPreference")) {
                            // Need to do more transforms on this one, so just output version/etc here.
                            continue;
                        }

                        for (CldrItem newItem : items) {
                            // alias will be dropped in conversion, don't count it.
                            if (newItem.isAliasItem()) {
                                valueCount--;
                            }

                            // Items like zone items need to be sorted first before write them out.
                            if (newItem.needsSort()) {
                                resolveArrayItems(out, nodesForLastItem, arrayItems);
                                sortingItems.add(newItem);
                            } else {
                                Matcher matcher = LdmlConvertRules.ARRAY_ITEM_PATTERN.matcher(
                                    newItem.getPath());
                                if (matcher.matches()) {
                                    resolveSortingItems(out, nodesForLastItem, sortingItems);
                                    leadingArrayItemPath = matcher.group(1);
                                    if (lastLeadingArrayItemPath != null &&
                                        !lastLeadingArrayItemPath.equals(leadingArrayItemPath)) {
                                        resolveArrayItems(out, nodesForLastItem, arrayItems);
                                    }
                                    lastLeadingArrayItemPath = leadingArrayItemPath;
                                    arrayItems.add(newItem);
                                } else {
                                    // output a single item
                                    resolveSortingItems(out, nodesForLastItem, sortingItems);
                                    resolveArrayItems(out, nodesForLastItem, arrayItems);
                                    outputCldrItem(out, nodesForLastItem, newItem);
                                    lastLeadingArrayItemPath = "";
                                }
                            }
                        }
                    }

                    resolveSortingItems(out, nodesForLastItem, sortingItems);
                    resolveArrayItems(out, nodesForLastItem, arrayItems);
                    if (js.section.contains("unitPreferenceData")) {
                        outputUnitPreferenceData(js, theItems, out, nodesForLastItem);
                    }

                    // closeNodes(out, nodesForLastItem.size() - 2, 0);

                    // write JSON
                    try (PrintWriter outf = FileUtilities.openUTF8Writer(outputDir, outFilename)) {
                        outf.println(gson.toJson(out));
                    }

                    String outPath = new File(outputDir.substring(this.outputDir.length()), outFilename).getPath();
                    outputProgress.add(Pair.of(js.section+' '+outPath, valueCount));
                    logger.fine(">" + progressPrefix(readCount, totalCount, filename, js.section) + String.format("…%s (%d values)",
                        outPath, valueCount));

                    totalItemsInFile += valueCount;
                }
            }

        }        // this is the only normal output with debug off
        StringBuilder outStr = new StringBuilder();
        if(!outputProgress.isEmpty()) {
            // Put these first, so the percent is at the end.
            for(final Pair<String, Integer> outputItem : outputProgress) {
                outStr.append(String.format("\t- %s (%d)\n", outputItem.getFirst(), outputItem.getSecond()));
            }
            outStr.append(String.format("%s%s (%d values in %d sections)\n",
                progressPrefix(readCount, totalCount), filename,
                totalItemsInFile, outputProgress.size()));
        } else {
            outStr.append(String.format("%s%s (no items output)\n", progressPrefix(readCount, totalCount), filename));
        }
        synchronized(readCount) { // to prevent interleaved output
            System.out.print(outStr);
        }
        return totalItemsInFile;
    }

    private boolean localeIsModernTier(String filename) {
        boolean isModernTier;
        {
            final Level localeCoverageLevel = sc.getLocaleCoverageLevel("Cldr", filename);
            isModernTier = localeCoverageLevel == Level.MODERN || filename.equals("root") || filename.equals("und");
        }
        return isModernTier;
    }

    /**
     * Entire xpaths and random short strings are passed through this function.
     * Not really Locale ID to Language Tag.
     * @param filename
     * @return
     */
    private String underscoreToHypen(String filename) {
        return filename.replaceAll("_", "-");
    }

    /**
     * Bottleneck for converting Unicode Locale ID (root, ca_ES_VALENCIA)
     * to String for filename or data item.
     * If strictBcp47 is true (default) then it will convert to (und, ca-ES-valencia)
     * @param locale
     * @return
     */
    private final String unicodeLocaleToString(String locale) {
        if(strictBcp47) {
            return CLDRLocale.toLanguageTag(locale);
        } else {
            return underscoreToHypen(locale);
        }
    }

    Pattern IS_REGION_CODE = PatternCache.get("([A-Z][A-Z])|([0-9][0-9][0-9])");
    /**
     * Bottleneck for converting Unicode Locale ID (root, ca_ES_VALENCIA)
     * to String for filename or data item.
     * If strictBcp47 is true (default) then it will convert to (und, ca-ES-valencia)
     * Differs from unicodeLocaleToString in that it will preserve all uppercase
     * region ids
     * @param locale
     * @return
     */
    private final String unicodeLocaleMishMashToString(String locale) {
        if(strictBcp47) {
            if (IS_REGION_CODE.matcher(locale).matches()) {
                return locale;
            } else {
                return CLDRLocale.toLanguageTag(locale);
            }
        } else {
            return underscoreToHypen(locale);
        }
    }

    /**
     * Fixup a path to be BCP47 compliant
     * @param path XPath (usually ends in elementName, but not necessarily)
     * @param elementName element to fixup
     * @param attributeNames list of attributes to fix
     * @return new path
     */
    final String fixXpathBcp47(final String path, String elementName, String... attributeNames) {
        final XPathParts xpp = XPathParts.getFrozenInstance(path).cloneAsThawed();
        for(final String attributeName : attributeNames) {
            final String oldValue = xpp.findAttributeValue(elementName, attributeName);
            if (oldValue == null) continue;
            final String oldValues[] = oldValue.split(" ");
            String newValue = Arrays.stream(oldValues)
                .map((String s) -> unicodeLocaleToString(s))
                .collect(Collectors.joining(" "));
            if (!oldValue.equals(newValue)) {
                xpp.setAttribute(elementName, attributeName, newValue);
                logger.finest(attributeName + " = " + oldValue + " -> " + newValue);
            }
        }
        return xpp.toString();
    }

    /**
     * Fixup a path to be BCP47 compliant
     * …but support a mishmash of regions and locale ids CLDR-15069
     * @param path XPath (usually ends in elementName, but not necessarily)
     * @param elementName element to fixup
     * @param attributeNames list of attributes to fix
     * @return new path
     */
    final String fixXpathBcp47MishMash(final String path, String elementName, String... attributeNames) {
        final XPathParts xpp = XPathParts.getFrozenInstance(path).cloneAsThawed();
        for(final String attributeName : attributeNames) {
            final String oldValue = xpp.findAttributeValue(elementName, attributeName);
            if (oldValue == null) continue;
            final String oldValues[] = oldValue.split(" ");
            String newValue = Arrays.stream(oldValues)
                .map((String s) -> unicodeLocaleMishMashToString(s))
                .collect(Collectors.joining(" "));
            if (!oldValue.equals(newValue)) {
                xpp.setAttribute(elementName, attributeName, newValue);
                logger.finest(attributeName + " = " + oldValue + " -> " + newValue);
            }
        }
        return xpp.toString();
    }

    private void outputUnitPreferenceData(JSONSection js, List<CldrItem> theItems, JsonObject out, ArrayList<CldrNode> nodesForLastItem)
        throws ParseException, IOException {
        // handle these specially.
        // redo earlier loop somewhat.
        CldrNode supplementalNode = CldrNode.createNode("cldr", "supplemental", "supplemental");
        JsonElement supplementalObject = startNonleafNode(out, supplementalNode);
        CldrNode unitPrefNode = CldrNode.createNode("supplemental", js.section, js.section);
        final JsonElement o = startNonleafNode(supplementalObject, unitPrefNode);

        // We'll directly write to 'out'

        // Unit preference sorting is a bit more complicated, so we're going to use the CldrItems,
        // but collect the results more directly.

        Map<Pair<String, String>, Map<String, List<CldrItem>>> catUsagetoRegionItems = new TreeMap<>();

        for (CldrItem item : theItems) {
            if (!item.getUntransformedPath().contains("unitPref")) {
                continue;
            }
            CldrItem[] items = item.split();
            if (items == null) {
                throw new IllegalArgumentException("expected unit pref to split: " + item);
            }
            for (final CldrItem subItem : items) {
                // step 1: make sure the category/usage is there
                final XPathParts xpp = XPathParts.getFrozenInstance(subItem.getPath());
                final String category = xpp.findFirstAttributeValue("category");
                final String usage = xpp.findFirstAttributeValue("usage");
                final String region = xpp.findFirstAttributeValue("regions"); // actually one region (split)
                Pair<String, String> key = Pair.of(category, usage);
                Map<String, List<CldrItem>> regionMap = catUsagetoRegionItems.computeIfAbsent(key, ignored -> new TreeMap<>());
                List<CldrItem> perRegion = regionMap.computeIfAbsent(region, ignored -> new ArrayList<>());
                perRegion.add(subItem);
            }
        }

        // OK, now start outputting
        // Traverse categories/usage/regions
        // unitPreferenceData is already open {
        catUsagetoRegionItems.keySet().stream().map(p -> p.getFirst()).distinct() // for each category
            .forEach(category -> {
                JsonObject oo = new JsonObject();
                o.getAsJsonObject().add(category, oo);

                catUsagetoRegionItems.entrySet().stream().filter(p -> p.getKey().getFirst().equals(category))
                    .forEach(ent -> {
                        final String usage = ent.getKey().getSecond();
                        JsonObject ooo = new JsonObject();
                        oo.getAsJsonObject().add(usage, ooo);

                        ent.getValue().forEach((region, list) -> {
                            JsonArray array = new JsonArray();
                            ooo.getAsJsonObject().add(region, array);
                            list.forEach(item -> {
                                final XPathParts xpp = XPathParts.getFrozenInstance(item.getPath());
                                JsonObject u = new JsonObject();
                                array.add(u);
                                u.addProperty("unit", item.getValue());
                                if (xpp.containsAttribute("geq")) {
                                    u.addProperty("geq", Double.parseDouble(xpp.findFirstAttributeValue("geq")));
                                }
                            });
                        });
                    });
            });

        // Computer, switch to 'automatic' navigation
        // We'll let closeNodes take over.
        nodesForLastItem.add(unitPrefNode); // unitPreferenceData }
    }

    /**
     * Creates the packaging files ( i.e. package.json ) for a particular package
     *
     * @param packageName
     *            The name of the installable package
     */
    public void writePackagingFiles(String outputDir, String packageName) throws IOException {
        File dir = new File(outputDir.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        writePackageJson(outputDir, packageName);
        writeBowerJson(outputDir, packageName);
        writeReadme(outputDir, packageName);
    }

    public void writeReadme(String outputDir, String packageName) throws IOException {
        final String basePackageName = getBasePackageName(packageName);
        try (PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/" + packageName, "README.md");) {
            outf.println("# " + packageName);
            outf.println();
            outf.println(configFileReader.getPackageDescriptions().get(basePackageName));
            outf.println();
            if (packageName.endsWith(FULL_TIER_SUFFIX)) {
                outf.println("This package contains the complete set of locales, including what is in the `" +
                CLDR_PKG_PREFIX + basePackageName + MODERN_TIER_SUFFIX + "` package.");
                outf.println();
            } else if (packageName.endsWith(MODERN_TIER_SUFFIX)) {
                outf.println("This package contains the set of locales listed as modern coverage. See also the `" +
                CLDR_PKG_PREFIX + basePackageName + FULL_TIER_SUFFIX + "` package.");
                outf.println();
            }
            outf.println();
            outf.println(getNpmBadge(packageName));
            outf.println();
            FileCopier.copy(CldrUtility.getUTF8Data("cldr-json-readme.md"), outf);
        }
        try (PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/" + packageName, "LICENSE");) {
            FileCopier.copy(CldrUtility.getUTF8Data("unicode-license.txt"), outf);
        }
    }

    String getBasePackageName(final String packageName) {
        String basePackageName = packageName;
        if (basePackageName.startsWith(CLDR_PKG_PREFIX)) {
            basePackageName = basePackageName.substring(CLDR_PKG_PREFIX.length());
        }
        if (basePackageName.endsWith(FULL_TIER_SUFFIX)) {
            basePackageName = basePackageName.substring(0, basePackageName.length() - FULL_TIER_SUFFIX.length());
        } else if (basePackageName.endsWith(MODERN_TIER_SUFFIX)) {
            basePackageName = basePackageName.substring(0, basePackageName.length() - MODERN_TIER_SUFFIX.length());
        }
        return basePackageName;
    }

    public void writeBasicInfo(JsonObject obj, String packageName, boolean isNPM) {
        obj.addProperty("name", packageName);
        obj.addProperty("version", pkgVersion);

        String[] packageNameParts = packageName.split("-");
        String dependency = dependencies.get(packageNameParts[1]);
        if (dependency != null) {
            String[] dependentPackageNames = new String[1];
            String tier = packageNameParts[packageNameParts.length - 1];
            if (dependency.equals("core") || dependency.equals("bcp47")) {
                dependentPackageNames[0] = CLDR_PKG_PREFIX + dependency;
            } else {
                dependentPackageNames[0] = CLDR_PKG_PREFIX + dependency + "-" + tier;
            }

            JsonObject dependencies = new JsonObject();
            for (String dependentPackageName : dependentPackageNames) {
                if (dependentPackageName != null) {
                    dependencies.addProperty(dependentPackageName, pkgVersion);
                }
            }
            obj.add(isNPM ? "peerDependencies" : "dependencies", dependencies);
        }
    }

    /**
     * Default for version string
     * @return
     */
    private static String getDefaultVersion() {
        String versionString = CLDRFile.GEN_VERSION;
        while (versionString.split("\\.").length < 3) {
            versionString = versionString + ".0";
        }
        return versionString;
    }

    public void writePackageJson(String outputDir, String packageName) throws IOException {
        PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/" + packageName, "package.json");
        logger.fine("Creating packaging file => " + outputDir + File.separator + packageName + File.separator + "package.json");
        JsonObject obj = new JsonObject();
        writeBasicInfo(obj, packageName, true);

        JsonArray maintainers = new JsonArray();
        JsonObject primaryMaintainer = new JsonObject();
        JsonObject secondaryMaintainer = new JsonObject();

        final String basePackageName = getBasePackageName(packageName);
        String description = configFileReader.getPackageDescriptions().get(basePackageName);
        if (packageName.endsWith(FULL_TIER_SUFFIX)) {
            description = description + " (complete)";
        } else if (packageName.endsWith(MODERN_TIER_SUFFIX)) {
            description = description + " (modern coverage locales)";
        }
        obj.addProperty("description", description);

        obj.addProperty("homepage", CLDRURLS.CLDR_HOMEPAGE);
        obj.addProperty("author", CLDRURLS.UNICODE_CONSORTIUM);

        primaryMaintainer.addProperty("name", "Steven R. Loomis");
        primaryMaintainer.addProperty("email", "srloomis@unicode.org");

        maintainers.add(primaryMaintainer);

        secondaryMaintainer.addProperty("name", "John Emmons");
        secondaryMaintainer.addProperty("email", "emmo@us.ibm.com");
        secondaryMaintainer.addProperty("url", "https://github.com/JCEmmons");

        maintainers.add(secondaryMaintainer);
        obj.add("maintainers", maintainers);

        JsonObject repository = new JsonObject();
        repository.addProperty("type", "git");
        repository.addProperty("url", "git://github.com/unicode-cldr/cldr-json.git");
        obj.add("repository", repository);

        obj.addProperty("license", CLDRURLS.UNICODE_SPDX);
        obj.addProperty("bugs", CLDRURLS.CLDR_NEWTICKET_URL);

        outf.println(gson.toJson(obj));
        outf.close();
    }

    public void writeBowerJson(String outputDir, String packageName) throws IOException {
        PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/" + packageName, "bower.json");
        logger.fine("Creating packaging file => " + outputDir + File.separator + packageName + File.separator + "bower.json");
        JsonObject obj = new JsonObject();
        writeBasicInfo(obj, packageName, false);
        if (type == RunType.supplemental) {
            JsonArray mainPaths = new JsonArray();
            mainPaths.add(new JsonPrimitive("availableLocales.json"));
            mainPaths.add(new JsonPrimitive("defaultContent.json")); // Handled specially
            mainPaths.add(new JsonPrimitive("scriptMetadata.json"));
            mainPaths.add(new JsonPrimitive(type.toString() + "/*.json"));
            obj.add("main", mainPaths);
        } else if (type == RunType.rbnf) {
            obj.addProperty("main", type.toString() + "/*.json");
        } else {
            obj.addProperty("main", type.toString() + "/**/*.json");
        }

        JsonArray ignorePaths = new JsonArray();
        ignorePaths.add(new JsonPrimitive(".gitattributes"));
        ignorePaths.add(new JsonPrimitive("README.md"));
        obj.add("ignore", ignorePaths);
        obj.addProperty("license", CLDRURLS.UNICODE_SPDX);

        outf.println(gson.toJson(obj));
        outf.close();
    }

    public void writeDefaultContent(String outputDir) throws IOException {
        PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/cldr-core", "defaultContent.json");
        System.out.println("Creating packaging file => " + outputDir + "/cldr-core" + File.separator + "defaultContent.json");
        JsonObject obj = new JsonObject();
        obj.add("defaultContent", gson.toJsonTree(skippedDefaultContentLocales));
        outf.println(gson.toJson(obj));
        outf.close();
    }

    public void writeAvailableLocales(String outputDir) throws IOException {
        PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/cldr-core", "availableLocales.json");
        System.out.println("Creating packaging file => " + outputDir + "/cldr-core" + File.separator + "availableLocales.json");
        JsonObject obj = new JsonObject();
        obj.add("availableLocales", gson.toJsonTree(avl));
        outf.println(gson.toJson(obj));
        outf.close();
    }

    public void writeScriptMetadata(String outputDir) throws IOException {
        PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/cldr-core", "scriptMetadata.json");
        System.out.println("Creating script metadata file => " + outputDir + File.separator + "cldr-core" + File.separator + "scriptMetadata.json");
        Map<String, Info> scriptInfo = new TreeMap<>();
        for (String script : ScriptMetadata.getScripts()) {
            Info i = ScriptMetadata.getInfo(script);
            scriptInfo.put(script, i);
        }
        if (ScriptMetadata.errors.size() > 0) {
            System.err.println(Joiner.on("\n\t").join(ScriptMetadata.errors));
            //throw new IllegalArgumentException();
        }

        JsonObject obj = new JsonObject();
        obj.add("scriptMetadata", gson.toJsonTree(scriptInfo));
        outf.println(gson.toJson(obj));
        outf.close();
    }

    public void writePackageList(String outputDir) throws IOException {
        PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/cldr-core", "cldr-packages.json");
        System.out.println("Creating packaging metadata file => " + outputDir + File.separator + "cldr-core" + File.separator + "cldr-packages.json and PACKAGES.md");
        PrintWriter pkgs = FileUtilities.openUTF8Writer(outputDir + "/..", "PACKAGES.md");

        pkgs.println("# CLDR JSON Packages");
        pkgs.println();

        LdmlConfigFileReader uberReader = new LdmlConfigFileReader();

        for (RunType r : RunType.values()) {
            if (r == RunType.all) continue;
            uberReader.read(null, r);
        }

        TreeMap<String, String> pkgsToDesc = new TreeMap<>();

        JsonObject obj = new JsonObject();
        obj.addProperty("license", CLDRURLS.UNICODE_SPDX);
        obj.addProperty("bugs", CLDRURLS.CLDR_NEWTICKET_URL);
        obj.addProperty("homepage", CLDRURLS.CLDR_HOMEPAGE);
        obj.addProperty("version", pkgVersion);

        JsonArray packages = new JsonArray();
        for(Map.Entry<String, String> e : uberReader.getPackageDescriptions().entrySet()) {
            final String baseName = e.getKey();

            if (baseName.equals("IGNORE") || baseName.equals("cal")) continue;
            if (baseName.equals("core") || baseName.equals("rbnf") || baseName.equals("bcp47")) {
                JsonObject packageEntry = new JsonObject();
                packageEntry.addProperty("description", e.getValue());
                packageEntry.addProperty("name", CLDR_PKG_PREFIX + baseName);
                packages.add(packageEntry);
                pkgsToDesc.put(packageEntry.get("name").getAsString(), packageEntry.get("description").getAsString());
            } else {
                {
                    JsonObject packageEntry = new JsonObject();
                    packageEntry.addProperty("description", e.getValue() + " (full)");
                    packageEntry.addProperty("tier", "full");
                    packageEntry.addProperty("name", CLDR_PKG_PREFIX + baseName + FULL_TIER_SUFFIX);
                    packages.add(packageEntry);
                    pkgsToDesc.put(packageEntry.get("name").getAsString(), packageEntry.get("description").getAsString());
                }
                {
                    JsonObject packageEntry = new JsonObject();
                    packageEntry.addProperty("description", e.getValue() + " (modern only)");
                    packageEntry.addProperty("tier", "modern");
                    packageEntry.addProperty("name", CLDR_PKG_PREFIX + baseName + MODERN_TIER_SUFFIX);
                    packages.add(packageEntry);
                    pkgsToDesc.put(packageEntry.get("name").getAsString(), packageEntry.get("description").getAsString());
                }
            }
        }
        pkgs.println();
        for (Map.Entry<String, String> e : pkgsToDesc.entrySet()) {
            pkgs.println("### [" +
                e.getKey() + "](./cldr-json/" + e.getKey() + "/)");
            pkgs.println();
            pkgs.println(" - " + e.getValue());
            pkgs.println(" - " + getNpmBadge(e.getKey()));
            pkgs.println();
        }
        obj.add("packages", packages);
        outf.println(gson.toJson(obj));
        outf.close();
        pkgs.println("## JSON Metadata");
        pkgs.println();
        pkgs.println("Package metadata is available at [`cldr-core`/cldr-packages.json](./cldr-json/cldr-core/cldr-packages.json)");
        pkgs.println();

        FileCopier.copy(CldrUtility.getUTF8Data("cldr-json-readme.md"), pkgs);
        pkgs.close();
    }

    private String getNpmBadge(final String packageName) {
        return String.format("[![NPM version](https://img.shields.io/npm/v/%s.svg?style=flat)](https://www.npmjs.org/package/%s)",
        packageName, packageName);
    }

    /**
     * Process the pending sorting items.
     *
     * @param out
     *            The ArrayList to hold all output lines.
     * @param nodesForLastItem
     *            All the nodes from last item.
     * @param sortingItems
     *            The item list that should be sorted before output.
     * @throws IOException
     * @throws ParseException
     */
    private void resolveSortingItems(JsonObject out,
        ArrayList<CldrNode> nodesForLastItem,
        ArrayList<CldrItem> sortingItems)
        throws IOException, ParseException {
        ArrayList<CldrItem> arrayItems = new ArrayList<>();
        String lastLeadingArrayItemPath = null;

        if (!sortingItems.isEmpty()) {
            Collections.sort(sortingItems);
            for (CldrItem item : sortingItems) {
                Matcher matcher = LdmlConvertRules.ARRAY_ITEM_PATTERN.matcher(
                    item.getPath());
                if (matcher.matches()) {
                    String leadingArrayItemPath = matcher.group(1);
                    if (lastLeadingArrayItemPath != null &&
                        !lastLeadingArrayItemPath.equals(leadingArrayItemPath)) {
                        resolveArrayItems(out, nodesForLastItem, arrayItems);
                    }
                    lastLeadingArrayItemPath = leadingArrayItemPath;
                    arrayItems.add(item);
                } else {
                    outputCldrItem(out, nodesForLastItem, item);
                }
            }
            sortingItems.clear();
            resolveArrayItems(out, nodesForLastItem, arrayItems);
        }
    }

    /**
     * Process the pending array items.
     *
     * @param out
     *            The ArrayList to hold all output lines.
     * @param nodesForLastItem
     *            All the nodes from last item.
     * @param arrayItems
     *            The item list that should be output as array.
     * @throws IOException
     * @throws ParseException
     */
    private void resolveArrayItems(JsonObject out,
        ArrayList<CldrNode> nodesForLastItem,
        ArrayList<CldrItem> arrayItems)
        throws IOException, ParseException {
        if (!arrayItems.isEmpty()) {
            CldrItem firstItem = arrayItems.get(0);
            if (firstItem.needsSort()) {
                Collections.sort(arrayItems);
                firstItem = arrayItems.get(0);
            }

            int arrayLevel = getArrayIndentLevel(firstItem); // only used for trim

            JsonArray array = outputStartArray(out, nodesForLastItem, firstItem, arrayLevel);

            // Previous statement closed for first element, trim nodesForLastItem
            // so that it will not happen again inside.
            int len = nodesForLastItem.size();
            while (len > arrayLevel) {
                nodesForLastItem.remove(len - 1);
                len--;
            }
            for (CldrItem insideItem : arrayItems) {
                outputArrayItem(array, insideItem, nodesForLastItem, arrayLevel);
            }
            arrayItems.clear();

            int lastLevel = nodesForLastItem.size() - 1;
            // closeNodes(out, lastLevel, arrayLevel);
            // out.endArray();
            for (int i = arrayLevel - 1; i < lastLevel; i++) {
                nodesForLastItem.remove(i);
            }
        }
    }

    /**
     * Find the indent level on which array should be inserted.
     *
     * @param item
     *            The CldrItem being examined.
     * @return The array indent level.
     * @throws ParseException
     */
    private int getArrayIndentLevel(CldrItem item) throws ParseException {
        Matcher matcher = LdmlConvertRules.ARRAY_ITEM_PATTERN.matcher(
            item.getPath());
        if (!matcher.matches()) {
            System.out.println("No match found for " + item.getPath() + ", this shouldn't happen.");
            return 0;
        }

        String leadingPath = matcher.group(1);
        CldrItem fakeItem = new CldrItem(leadingPath, leadingPath, leadingPath, leadingPath, "");
        return fakeItem.getNodesInPath().size() - 1;
    }

    /**
     * Write the start of an array.
     *
     * @param out
     *            The root object
     * @param nodesForLastItem
     *            Nodes in path for last CldrItem.
     * @param item
     *            The CldrItem to be processed.
     * @param arrayLevel
     *            The level on which array is laid out.
     * @throws IOException
     * @throws ParseException
     */
    private JsonArray outputStartArray(JsonObject out,
        ArrayList<CldrNode> nodesForLastItem, CldrItem item, int arrayLevel)
        throws IOException, ParseException {

        ArrayList<CldrNode> nodesInPath = item.getNodesInPath();

        JsonElement o = out;

        // final CldrNode last = nodesInPath.get(nodesInPath.size()-1);

        // Output nodes up to parent of 'arrayLevel'
        for (int i=1; i < arrayLevel-1; i++) {
            final CldrNode node = nodesInPath.get(i);
            o = startNonleafNode(o, node);
        }

        // at arrayLevel, we have a named Array.
        // Get the name of the parent of the array
        String objName = nodesInPath.get(arrayLevel-1).getNodeKeyName();
        JsonArray array = new JsonArray();
        o.getAsJsonObject().add(objName, array);

        return array;
    }

    /**
     * Write a CLDR item to file.
     *
     * "usesMetazone" will be checked to see if it is current. Those non-current
     * item will be dropped.
     *
     * @param out
     *            The ArrayList to hold all output lines.
     * @param nodesForLastItem
     * @param item
     *            The CldrItem to be processed.
     * @throws IOException
     * @throws ParseException
     */
    private void outputCldrItem(JsonObject out,
        ArrayList<CldrNode> nodesForLastItem, CldrItem item)
        throws IOException, ParseException {
        // alias has been resolved, no need to keep it.
        if (item.isAliasItem()) {
            return;
        }

        ArrayList<CldrNode> nodesInPath = item.getNodesInPath();
        int arraySize = nodesInPath.size();

        int i = 0;
        if (i == nodesInPath.size() && type != RunType.rbnf) {
            System.err.println("This nodes and last nodes has identical path. ("
                + item.getPath() + ") Some distinguishing attributes wrongly removed?");
            return;
        }

        // close previous nodes
        // closeNodes(out, nodesForLastItem.size() - 2, i);
        JsonElement o = out;
        for (; i < nodesInPath.size() - 1; ++i) {
            o = startNonleafNode(o, nodesInPath.get(i));
        }

        writeLeafNode(o, nodesInPath.get(i), item.getValue());
        nodesForLastItem.clear();
        nodesForLastItem.addAll(nodesInPath);
    }

    /**
     * Start a non-leaf node, adding it if not there.
     *
     * @param out
     *            The input JsonObject
     * @param node
     *            The node being written.
     * @throws IOException
     */
    private JsonElement startNonleafNode(JsonElement out, final CldrNode node)
        throws IOException {
        String objName = node.getNodeKeyName();
        // Some node should be skipped as indicated by objName being null.
        logger.finest(() -> "objName= " + objName + " for path " + node.getUntransformedPath());
        if (objName == null ||
            objName.equals("cldr") || objName.equals("ldmlBCP47")) { // Skip root 'cldr' node
            return out;
        }

        Map<String, String> attrAsValueMap = node.getAttrAsValueMap();

        String name;

        if (type == RunType.annotations || type == RunType.annotationsDerived) {
            if (objName.startsWith("U+")) {
                // parse U+22 -> "   etc
                name = (com.ibm.icu.text.UTF16.valueOf(Integer.parseInt(objName.substring(2), 16)));
            } else {
                name = (objName);
            }
        } else {
            name =(objName);
        }

        JsonElement o = out.getAsJsonObject().get(name);

        if (o == null) {
            o = new JsonObject();
            out.getAsJsonObject().add(name, o);
        }

        for (final String key : attrAsValueMap.keySet()) {
            logger.finest(() -> "Non-Leaf Node: " + node.getUntransformedPath() + " ." + key);
            String rawAttrValue = attrAsValueMap.get(key);
            String value = escapeValue(rawAttrValue);
            // attribute is prefixed with "_" when being used as key.
            String attrAsKey = "_" + key;
            if (LdmlConvertRules.attrIsBooleanOmitFalse(node.getUntransformedPath(), node.getName(), node.getParent(), key)) {
                final Boolean v = Boolean.parseBoolean(rawAttrValue);
                if (v) {
                    o.getAsJsonObject().addProperty(attrAsKey, v);
                } // else, omit
            } else {
                o.getAsJsonObject().addProperty(attrAsKey, value);
            }
        }
        return o;
    }

    /**
     * Write a CLDR item to file.
     *
     * "usesMetazone" will be checked to see if it is current. Those non-current
     * item will be dropped.
     *
     * @param out
     *            The ArrayList to hold all output lines.
     * @param item
     *            The CldrItem to be processed.
     * @param nodesForLastItem
     *            Nodes in path for last item.
     * @param arrayLevel
     *            The indentation level in which array exists.
     * @throws IOException
     * @throws ParseException
     */
    private void outputArrayItem(JsonArray out, CldrItem item,
        ArrayList<CldrNode> nodesForLastItem, int arrayLevel)
        throws IOException, ParseException {

        // This method is more complicated that outputCldrItem because it needs to
        // handle 3 different cases.
        // 1. When difference is found below array item, this item will be of the
        // same array item. Inside the array item, it is about the same as
        // outputCldrItem, just with one more level of indentation because of
        // the array.
        // 2. The array item is the leaf item with no attribute, simplify it as
        // an object with one name/value pair.
        // 3. The array item is the leaf item with attribute, an embedded object
        // will be created inside the array item object.

        ArrayList<CldrNode> nodesInPath = item.getNodesInPath();
        String value = escapeValue(item.getValue());
        int nodesNum = nodesInPath.size();

        // case 1
        // int diff = findFirstDiffNodeIndex(nodesForLastItem, nodesInPath);
        CldrNode cldrNode = nodesInPath.get(nodesNum - 1);

        // if (diff > arrayLevel) {
        //     // close previous nodes
        //     closeNodes(out, nodesForLastItem.size() - 1, diff + 1);

        //     for (int i = diff; i < nodesNum - 1; i++) {
        //         startNonleafNode(out, nodesInPath.get(i), i + 1);
        //     }
        //     writeLeafNode(out, cldrNode, value, nodesNum);
        //     return;
        // }

        if (arrayLevel == nodesNum - 1) {
            // case 2
            // close previous nodes
            // if (nodesForLastItem.size() - 1 - arrayLevel > 0) {
            //     closeNodes(out, nodesForLastItem.size() - 1, arrayLevel);
            // }

            String objName = cldrNode.getNodeKeyName();
            int pos = objName.indexOf('-');
            if (pos > 0) {
                objName = objName.substring(0, pos);
            }

            Map<String, String> attrAsValueMap = cldrNode.getAttrAsValueMap();

            if (attrAsValueMap.isEmpty()) {
                JsonObject o = new JsonObject();
                out.add(o);
                o.addProperty(objName, value);
            } else if (objName.equals("rbnfrule")) {
                writeRbnfLeafNode(out, item, attrAsValueMap);
            } else {
                JsonObject o = new JsonObject();
                writeLeafNode(o, objName, attrAsValueMap, value, cldrNode.getName(), cldrNode.getParent(), cldrNode);
                out.add(o);
            }
            // the last node is closed, remove it.
            nodesInPath.remove(nodesNum - 1);
        } else {
            // case 3
            // close previous nodes
            // if (nodesForLastItem.size() - 1 - (arrayLevel) > 0) {
            //     closeNodes(out, nodesForLastItem.size() - 1, arrayLevel);
            // }

            JsonObject o = new JsonObject();
            out.add(o);

            CldrNode node = nodesInPath.get(arrayLevel);
            String objName = node.getNodeKeyName();
            int pos = objName.indexOf('-');
            if (pos > 0) {
                objName = objName.substring(0, pos);
            }
            Map<String, String> attrAsValueMap = node.getAttrAsValueMap();
            JsonObject oo = new JsonObject();
            o.add(objName, oo);
            for (String key : attrAsValueMap.keySet()) {
                // attribute is prefixed with "_" when being used as key.
                oo.addProperty("_" + key, escapeValue(attrAsValueMap.get(key)));
            }

            JsonElement o2 = out;
            System.err.println("PROBLEM at " + cldrNode.getUntransformedPath());
            // TODO ?!!
            for (int i = arrayLevel + 1; i < nodesInPath.size() - 1; i++) {
                o2 = startNonleafNode(o2, nodesInPath.get(i));
            }
            writeLeafNode(o2, cldrNode, value);
        }

        nodesForLastItem.clear();
        nodesForLastItem.addAll(nodesInPath);
    }

    private void writeRbnfLeafNode(JsonElement out, CldrItem item, Map<String, String> attrAsValueMap) throws IOException {
        if(attrAsValueMap.size() != 1) {
            throw new IllegalArgumentException("Error, attributes seem wrong for RBNF " + item.getUntransformedPath());
        }
        Entry<String, String> entry = attrAsValueMap.entrySet().iterator().next();
        JsonArray arr = new JsonArray();
        arr.add(entry.getKey());
        arr.add(entry.getValue());
        out.getAsJsonArray().add(arr);
    }

    private String progressPrefix(AtomicInteger readCount, int totalCount, String filename, String section) {
        return progressPrefix(readCount.get(), totalCount, filename, section);
   }

    private String progressPrefix(int readCount, int totalCount, String filename, String section) {
         return progressPrefix(readCount, totalCount) + filename + "\t" + section + "\t";
    }

    private final String progressPrefix(AtomicInteger readCount, int totalCount) {
        return progressPrefix(readCount.get(), totalCount);
    }

    LocalizedNumberFormatter percentFormatter = NumberFormatter
            .withLocale(Locale.ENGLISH)
            .unit(NoUnit.PERCENT)
            .integerWidth(IntegerWidth.zeroFillTo(3))
            .precision(Precision.integer());

    private final String progressPrefix(int readCount, int totalCount) {
        double asPercent = ((double)readCount/(double)totalCount) * 100.0;
        return String.format("%s\t[%s]:\t", type, percentFormatter.format(asPercent));
    }

    /**
     * Process files in a directory of CLDR file tree.
     *
     * @param dirName
     *            The directory in which xml file will be transformed.
     * @param minimalDraftStatus
     *            The minimumDraftStatus that will be accepted.
     * @throws IOException
     * @throws ParseException
     */
    public void processDirectory(String dirName, DraftStatus minimalDraftStatus)
        throws IOException, ParseException {
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance(cldrCommonDir + "supplemental");
        Factory cldrFactory = Factory.make(
            cldrCommonDir + dirName + "/", ".*");
        Set<String> files = cldrFactory.getAvailable()
                // filter these out early so our work count is correct
                .stream().filter(filename ->
                    filename.matches(match) &&
                    !LdmlConvertRules.IGNORE_FILE_SET.contains(filename))
                .collect(Collectors.toSet());
        final int total = files.size();
        AtomicInteger readCount = new AtomicInteger(0);
        Map<String, Throwable> errs = new TreeMap<>();

        // This takes a long time (minutes, in 2020), so run it in parallel forkJoinPool threads.
        // The result of this pipeline is an array of toString()-able filenames of XML files which
        // produced no JSON output, just as a warning.
        System.out.println(progressPrefix(0, total) + " Beginning parallel process of " + total + " file(s)");
        Object noOutputFiles[] = files
            .parallelStream()
            .unordered()
            .map(filename -> {
                String pathPrefix;
                CLDRFile file = cldrFactory.make(filename, resolve && type == RunType.main, minimalDraftStatus);
                // Print 'reading' after the make, to stagger the output a little bit.
                // Otherwise, the printout happens before any work happens, and is easily out of order.
                readCount.incrementAndGet();
                logger.fine(() -> "<" + progressPrefix(readCount, total, dirName, filename) + "\r");

                if (type == RunType.main) {
                    pathPrefix = "/cldr/" + dirName + "/" + unicodeLocaleToString(filename) + "/";
                } else {
                    pathPrefix = "/cldr/" + dirName + "/";
                }
                int totalForThisFile = 0;
                try {
                    totalForThisFile = convertCldrItems(readCount, total, dirName, filename, pathPrefix,
                        mapPathsToSections(readCount, total, file, pathPrefix, sdi));
                } catch (IOException | ParseException t) {
                    t.printStackTrace();
                    System.err.println("!" + progressPrefix(readCount, total) + filename + " - err - " + t);
                    errs.put(filename, t);
                } finally {
                    logger.fine(() -> "." + progressPrefix(readCount, total) +
                        "Completing " + dirName + "/" + filename);
                }
                return new Pair<>(dirName + "/" + filename, totalForThisFile);
            })
            .filter(p -> p.getSecond() == 0)
            .map(p -> p.getFirst())
            .toArray();
        System.out.println(progressPrefix(total, total) + " Completed parallel process of " + total + " file(s)");
        if (noOutputFiles.length > 0) {
            System.err.println("WARNING: These " + noOutputFiles.length + " file(s) did not produce any output (check JSON config):");
            for (final Object f : noOutputFiles) {
                final String loc = f.toString();
                final String uloc = unicodeLocaleToString(f.toString());
                if (skipBcp47LocalesWithSubtags && type.locales() && HAS_SUBTAG.matcher(uloc).matches()) {
                    System.err.println("\t- " + loc + " (Skipped due to '-T true': " + uloc + ")");
                } else {
                    System.err.println("\t- " + loc);
                }
            }
        }

        if (!errs.isEmpty()) {
            System.err.println("Errors in these files:");
            for (Map.Entry<String, Throwable> e : errs.entrySet()) {
                System.err.println(e.getKey() + " - " + e.getValue());
            }
            // rethrow
            for (Map.Entry<String, Throwable> e : errs.entrySet()) {
                if (e.getValue() instanceof IOException) {
                    throw (IOException) e.getValue(); // throw the first one
                } else if (e.getValue() instanceof ParseException) {
                    throw (ParseException) e.getValue(); // throw the first one
                } else {
                    throw new RuntimeException("Other exception thrown: " + e.getValue());
                }
                /* NOTREACHED */
            }
        }

        if (writePackages) {
            for (String currentPackage : packages) {
                writePackagingFiles(outputDir, currentPackage);
            }
            if (type == RunType.main) {
                writeDefaultContent(outputDir);
                writeAvailableLocales(outputDir);
            } else if (type == RunType.supplemental) {
                writeScriptMetadata(outputDir);
                if (Boolean.parseBoolean(options.get("packagelist").getValue())) {
                    writePackageList(outputDir);
                }
            }

        }
    }

    /**
     * Replacement pattern for escaping.
     */
    private static final Pattern escapePattern = PatternCache.get("\\\\(?!u)");

    /**
     * Escape \ and " in value string.
     * \ should be replaced by \\, except in case of \u1234
     * " should be replaced by \"
     * In following code, \\\\ represent one \, because java compiler and
     * regular expression compiler each do one round of escape.
     *
     * @param value
     *            Input string.
     * @return escaped string.
     */
    private String escapeValue(String value) {
        Matcher match = escapePattern.matcher(value);
        String ret = match.replaceAll("\\\\\\\\");
        return ret.replace("\"", "\\\"").replace("\n", " ").replace("\t", " ");
    }

    /**
     * Write the value to output.
     *
     * @param out
     *            The ArrayList to hold all output lines.
     * @param node
     *            The CldrNode being written.
     * @param value
     *            The value part for this element.
     * @param level
     *            Indent level.
     * @throws IOException
     */
    private void writeLeafNode(JsonElement out, CldrNode node, String value) throws IOException {

        String objName = node.getNodeKeyName();
        Map<String, String> attrAsValueMaps = node.getAttrAsValueMap();
        writeLeafNode(out, objName, attrAsValueMaps, value, node.getName(), node.getParent(), node);
    }

    /**
     * Write the value to output.
     *
     * @param out
     *            The ArrayList to hold all output lines.
     * @param objName
     *            The node's node.
     * @param attrAsValueMap
     *            Those attributes that will be treated as values.
     * @param value
     *            The value part for this element.
     * @param level
     *            Indent level.
     * @param nodeName the original nodeName (not distinguished)
     * @throws IOException
     */
    private void writeLeafNode(JsonElement out, String objName,
        Map<String, String> attrAsValueMap, String value, final String nodeName,
            String parent, CldrNode node)
        throws IOException {
        if (objName == null) {
            return;
        }
        value = escapeValue(value);

        final boolean valueIsSpacesepArray = LdmlConvertRules.valueIsSpacesepArray(nodeName, parent);
        if (attrAsValueMap.isEmpty()) {
            // out.name(objName);
            if (value.isEmpty()) {
                if (valueIsSpacesepArray) {
                    out.getAsJsonObject().add(objName, new JsonArray());
                } else {
                    out.getAsJsonObject().add(objName, new JsonObject());
                }
            } else if (type == RunType.annotations ||
                type == RunType.annotationsDerived) {
                JsonArray a = new JsonArray();
                // split this, so "a | b | c" becomes ["a","b","c"]
                for (final String s : Annotations.splitter.split(value.trim())) {
                    a.add(s);
                }
                out.getAsJsonObject().add(objName, a);
            } else if(valueIsSpacesepArray) {
                outputSpaceSepArray(out, objName, value);
            } else {
                // normal value
                out.getAsJsonObject().addProperty(objName, value);
            }
            return;
        }

        // If there is no value, but a attribute being treated as value,
        // simplify the output.
        if (value.isEmpty() &&
            attrAsValueMap.containsKey(LdmlConvertRules.ANONYMOUS_KEY)) {
            String v = attrAsValueMap.get(LdmlConvertRules.ANONYMOUS_KEY);
            // out.name(objName);
            if (valueIsSpacesepArray) {
                outputSpaceSepArray(out, objName, v);
            } else {
                out.getAsJsonObject().addProperty(objName, v);
            }
            return;
        }

        JsonObject o = new JsonObject();
        out.getAsJsonObject().add(objName, o);

        if (!value.isEmpty()) {
            o.addProperty("_value", value);
        }

        for (final String key : attrAsValueMap.keySet()) {
            String rawAttrValue = attrAsValueMap.get(key);
            String attrValue = escapeValue(rawAttrValue);
            // attribute is prefixed with "_" when being used as key.
            String attrAsKey = "_" + key;
            logger.finest(() -> "Leaf Node: " + node.getUntransformedPath() + " ." + key);
            if (LdmlConvertRules.ATTRVALUE_AS_ARRAY_SET.contains(key)) {
                String[] strings = attrValue.trim().split("\\s+");
                JsonArray a = new JsonArray();
                o.add(attrAsKey, a);
                for (String s : strings) {
                    a.add(s);
                }
            } else if (node != null &&
                LdmlConvertRules.attrIsBooleanOmitFalse(node.getUntransformedPath(), nodeName, parent, key)) {
                final Boolean v = Boolean.parseBoolean(rawAttrValue);
                if (v) {
                    o.addProperty(attrAsKey, v);
                } // else: omit falsy value
            } else {
                o.addProperty(attrAsKey, attrValue);
            }
        }
    }

    private void outputSpaceSepArray(JsonElement out, String objName, String v) throws IOException {
        JsonArray a = new JsonArray();
        out.getAsJsonObject().add(objName, a);
        // split this, so "a b c" becomes ["a","b","c"]
        for (final String s : v.trim().split(" ")) {
            if(!s.isEmpty()) {
                a.add(s);
            }
        }
    }
}
