package org.unicode.cldr.json;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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
import org.unicode.cldr.util.FileProcessor;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
    private static boolean DEBUG = false;

    private enum RunType {
        main,
        supplemental(false), // aka 'core'
        segments, rbnf(false), annotations, annotationsDerived, bcp47(false);

        private final boolean isTiered;
        RunType() {
            this.isTiered = true;
        }
        RunType(boolean isTiered) {
            this.isTiered = isTiered;
        }
        /**
         * Is it split into modern/full?
         * @return
         */
        public boolean tiered() {
            return isTiered;
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

    private class availableLocales {
        Set<String> modern = new TreeSet<>();
        Set<String> full = new TreeSet<>();
    }

    private availableLocales avl = new availableLocales();
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Options options = new Options(
        "Usage: LDML2JsonConverter [OPTIONS] [FILES]\n" +
            "This program converts CLDR data to the JSON format.\n" +
            "Please refer to the following options. \n" +
            "\texample: org.unicode.cldr.json.Ldml2JsonConverter -c xxx -d yyy")
                .add("commondir", 'c', ".*", CLDRPaths.COMMON_DIRECTORY,
                    "Common directory for CLDR files, defaults to CldrUtility.COMMON_DIRECTORY")
                .add("destdir", 'd', ".*", CLDRPaths.GEN_DIRECTORY,
                    "Destination directory for output files, defaults to CldrUtility.GEN_DIRECTORY")
                .add("match", 'm', ".*", ".*",
                    "Regular expression to define only specific locales or files to be generated")
                .add("type", 't', "("+RunType.valueList()+")", "main",
                    "Type of CLDR data being generated, such as main, supplemental, or segments.")
                .add("resolved", 'r', "(true|false)", "false",
                    "Whether the output JSON for the main directory should be based on resolved or unresolved data")
                .add("draftstatus", 's', "(approved|contributed|provisional|unconfirmed)", "unconfirmed",
                    "The minimum draft status of the output data")
                .add("coverage", 'l', "(minimal|basic|moderate|modern|comprehensive|optional)", "optional",
                    "The maximum coverage level of the output data")
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

        Ldml2JsonConverter l2jc = new Ldml2JsonConverter(
            options.get("commondir").getValue(),
            options.get("destdir").getValue(),
            options.get("type").getValue(),
            Boolean.parseBoolean(options.get("fullnumbers").getValue()),
            Boolean.parseBoolean(options.get("resolved").getValue()),
            options.get("coverage").getValue(),
            options.get("match").getValue(),
            Boolean.parseBoolean(options.get("packages").getValue()),
            options.get("konfig").getValue(),
            options.get("pkgversion").getValue());

        long start = System.currentTimeMillis();
        DraftStatus status = DraftStatus.valueOf(options.get("draftstatus").getValue());
        l2jc.processDirectory(options.get("type").getValue(), status);
        long end = System.currentTimeMillis();
        System.out.println("Finished in " + (end - start) + " ms");
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

    private class JSONSection implements Comparable<JSONSection> {
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

    public Ldml2JsonConverter(String cldrDir, String outputDir, String runType, boolean fullNumbers, boolean resolve, String coverage, String match,
        boolean writePackages, String configFile, String pkgVersion) {
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

        sections = new ArrayList<>();
        packages = new TreeSet<>();
        dependencies = new HashMap<>();

        FileProcessor myReader = new FileProcessor() {
            @Override
            protected boolean handleLine(int lineCount, String line) {
                String[] lineParts = line.trim().split("\\s*;\\s*");
                String key, value, section = null, path = null, packageName = null, dependency = null;
                boolean hasSection = false;
                boolean hasPath = false;
                boolean hasPackage = false;
                boolean hasDependency = false;
                for (String linePart : lineParts) {
                    int pos = linePart.indexOf('=');
                    if (pos < 0) {
                        throw new IllegalArgumentException();
                    }
                    key = linePart.substring(0, pos);
                    value = linePart.substring(pos + 1);
                    if (key.equals("section")) {
                        hasSection = true;
                        section = value;
                    } else if (key.equals("path")) {
                        hasPath = true;
                        path = value;
                    } else if (key.equals("package")) {
                        hasPackage = true;
                        packageName = value;
                    } else if (key.equals("dependency")) {
                        hasDependency = true;
                        dependency = value;
                    }
                }
                if (hasSection && hasPath) {
                    JSONSection j = new JSONSection();
                    j.section = section;
                    j.pattern = PatternCache.get(path);
                    if (hasPackage) {
                        j.packageName = packageName;
                    }
                    sections.add(j);
                }
                if (hasDependency && hasPackage) {
                    dependencies.put(packageName, dependency);
                }
                return true;
            }
        };

        if (configFile != null) {
            myReader.process(configFile);
        } else {
            switch (type) {
            case main:
                myReader.process(Ldml2JsonConverter.class, "JSON_config.txt");
                break;
            case supplemental:
                myReader.process(Ldml2JsonConverter.class, "JSON_config_supplemental.txt");
                break;
            case segments:
                myReader.process(Ldml2JsonConverter.class, "JSON_config_segments.txt");
                break;
            case rbnf:
                myReader.process(Ldml2JsonConverter.class, "JSON_config_rbnf.txt");
                break;
            default:
                myReader.process(Ldml2JsonConverter.class, "JSON_config_" + type.name() + ".txt");
            }
        }

        // Add a section at the end of the list that will match anything not already matched.
        JSONSection j = new JSONSection();
        j.section = "other";
        j.pattern = PatternCache.get(".*");
        sections.add(j);

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

        if (DEBUG) {
            System.out.println(" IN pathStr : " + result);
        }
        result = LdmlConvertRules.PathTransformSpec.applyAll(result);
        result = result.replaceFirst("/ldml/", pathPrefix);
        result = result.replaceFirst("/supplementalData/", pathPrefix);

        if (result.contains("languages") ||
            result.contains("languageAlias") ||
            result.contains("languageMatches") ||
            result.contains("likelySubtags") ||
            result.contains("parentLocale") ||
            result.contains("locales=")) {
            result = localeIdToLangTag(result);
        }
        if (DEBUG) {
            System.out.println("OUT pathStr : " + result);
        }

        if (DEBUG) {
            System.out.println("result: " + result);
        }
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

        int totalItemsInFile = 0;

        List<Pair<String,Integer>> outputProgress = new LinkedList<>();

        for (JSONSection js : sections) {
            if (js.section.equals("IGNORE")) {
                continue;
            }
            String outFilename;
            final String filenameAsLangTag = localeIdToLangTag(filename);
            if (type == RunType.rbnf || type == RunType.bcp47) {
                outFilename = filenameAsLangTag + ".json";
            } else if(js.section.equals("other")) {
                // If you see other-___.json, it means items that were missing from JSON_config_*.txt
                outFilename = js.section + "-" + filenameAsLangTag + ".json";
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
                    if (DEBUG) {
                        System.out.println("outDir: " + outputDirname);
                        System.out.println("pack: " + js.packageName);
                        System.out.println("dir: " + dirName);
                    }
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
                        if (DEBUG) System.out.println(">" + progressPrefix(readCount, totalCount) +
                            outputDir + " - no items to write in " + js.section); // mostly noise
                        continue;
                    }
                    if(DEBUG) System.out
                        .print("?" + progressPrefix(readCount, totalCount, filename, js.section) + " - " + theItems.size() + " item(s)" + "\r");
                    // Create the output dir if it doesn't exist
                    File dir = new File(outputDir.toString());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    PrintWriter outf = FileUtilities.openUTF8Writer(outputDir, outFilename);
                    JsonWriter out = new JsonWriter(outf);
                    out.setIndent("  ");

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

                    closeNodes(out, nodesForLastItem.size() - 2, 0);

                    outf.println();
                    out.close();

                    String outPath = new File(outputDir.substring(this.outputDir.length()), outFilename).getPath();
                    outputProgress.add(Pair.of(js.section+' '+outPath, valueCount));
                    if(DEBUG) {
                        String outStr = ">" + progressPrefix(readCount, totalCount, filename, js.section) + String.format("…%s (%d values)",
                            outPath, valueCount);
                        synchronized(readCount) { // to prevent interleaved output
                            System.out.println(outStr);
                        }
                    }

                    totalItemsInFile += valueCount;
                }
            }

        }        // this is the only normal output with debug off
        StringBuilder outStr = new StringBuilder();
        if(!outputProgress.isEmpty()) {
            // Put these first, so the percent is at the end.
            for(final Pair<String, Integer> outputItem : outputProgress) {
                outStr.append(String.format("\t%s (%d)\n", outputItem.getFirst(), outputItem.getSecond()));
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
            isModernTier = localeCoverageLevel == Level.MODERN || filename.equals("root");
        }
        return isModernTier;
    }

    private String localeIdToLangTag(String filename) {
        return filename.replaceAll("_", "-");
    }

    private void outputUnitPreferenceData(JSONSection js, List<CldrItem> theItems, JsonWriter out, ArrayList<CldrNode> nodesForLastItem)
        throws ParseException, IOException {
        // handle these specially.
        // redo earlier loop somewhat.
        CldrNode unitPrefNode = CldrNode.createNode("supplemental", js.section, js.section);
        startNonleafNode(out, unitPrefNode, 1);
        // Computer, switch to 'manual' navigation
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
                try {
                    out.name(category).beginObject();
                    catUsagetoRegionItems.entrySet().stream().filter(p -> p.getKey().getFirst().equals(category))
                        .forEach(ent -> {
                            final String usage = ent.getKey().getSecond();
                            try {
                                out.name(usage);
                                out.beginObject();

                                ent.getValue().forEach((region, list) -> {
                                    try {
                                        out.name(region);
                                        out.beginArray();
                                        list.forEach(item -> {
                                            try {
                                                final XPathParts xpp = XPathParts.getFrozenInstance(item.getPath());
                                                out.beginObject();
                                                out.name("unit");
                                                out.value(item.getValue());
                                                if (xpp.containsAttribute("geq")) {
                                                    out.name("geq");
                                                    out.value(Double.parseDouble(xpp.findFirstAttributeValue("geq")));
                                                }
                                                out.endObject();
                                            } catch (IOException e) {
                                                throw (new ICUUncheckedIOException(e));
                                            }
                                        });
                                        out.endArray();
                                    } catch (IOException e) {
                                        throw (new ICUUncheckedIOException(e));
                                    }
                                });
                                out.endObject();
                            } catch (IOException e) {
                                throw (new ICUUncheckedIOException(e));
                            }
                        });
                    out.endObject();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw (new ICUUncheckedIOException(e));
                }
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
        try (PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/" + packageName, "README.md");) {
            FileCopier.copy(CldrUtility.getUTF8Data("cldr-json-readme.md"), outf);
        }
        try (PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/" + packageName, "LICENSE");) {
            FileCopier.copy(CldrUtility.getUTF8Data("unicode-license.txt"), outf);
        }
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
        System.out.println("Creating packaging file => " + outputDir + File.separator + packageName + File.separator + "package.json");
        JsonObject obj = new JsonObject();
        writeBasicInfo(obj, packageName, true);

        JsonArray maintainers = new JsonArray();
        JsonObject primaryMaintainer = new JsonObject();

        obj.addProperty("homepage", CLDRURLS.CLDR_HOMEPAGE);
        obj.addProperty("author", CLDRURLS.UNICODE_CONSORTIUM);

        primaryMaintainer.addProperty("name", "John Emmons");
        primaryMaintainer.addProperty("email", "emmo@us.ibm.com");
        primaryMaintainer.addProperty("url", "https://github.com/JCEmmons");

        maintainers.add(primaryMaintainer);
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
        System.out.println("Creating packaging file => " + outputDir + File.separator + packageName + File.separator + "bower.json");
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
        System.out.println("Creating packaging file => " + outputDir + "cldr-core" + File.separator + "defaultContent.json");
        JsonObject obj = new JsonObject();
        obj.add("defaultContent", gson.toJsonTree(skippedDefaultContentLocales));
        outf.println(gson.toJson(obj));
        outf.close();
    }

    public void writeAvailableLocales(String outputDir) throws IOException {
        PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/cldr-core", "availableLocales.json");
        System.out.println("Creating packaging file => " + outputDir + "cldr-core" + File.separator + "availableLocales.json");
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
    private void resolveSortingItems(JsonWriter out,
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
    private void resolveArrayItems(JsonWriter out,
        ArrayList<CldrNode> nodesForLastItem,
        ArrayList<CldrItem> arrayItems)
        throws IOException, ParseException {
        if (!arrayItems.isEmpty()) {
            CldrItem firstItem = arrayItems.get(0);
            if (firstItem.needsSort()) {
                Collections.sort(arrayItems);
                firstItem = arrayItems.get(0);
            }

            int arrayLevel = getArrayIndentLevel(firstItem);

            outputStartArray(out, nodesForLastItem, firstItem, arrayLevel);

            // Previous statement closed for first element, trim nodesForLastItem
            // so that it will not happen again inside.
            int len = nodesForLastItem.size();
            while (len > arrayLevel) {
                nodesForLastItem.remove(len - 1);
                len--;
            }
            for (CldrItem insideItem : arrayItems) {

                outputArrayItem(out, insideItem, nodesForLastItem, arrayLevel);

            }
            arrayItems.clear();

            int lastLevel = nodesForLastItem.size() - 1;
            closeNodes(out, lastLevel, arrayLevel);
            out.endArray();
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
     *            The ArrayList to hold all output lines.
     * @param nodesForLastItem
     *            Nodes in path for last CldrItem.
     * @param item
     *            The CldrItem to be processed.
     * @param arrayLevel
     *            The level on which array is laid out.
     * @throws IOException
     * @throws ParseException
     */
    private void outputStartArray(JsonWriter out,
        ArrayList<CldrNode> nodesForLastItem, CldrItem item, int arrayLevel)
        throws IOException, ParseException {

        ArrayList<CldrNode> nodesInPath = item.getNodesInPath();

        int i = findFirstDiffNodeIndex(nodesForLastItem, nodesInPath);

        // close previous nodes
        closeNodes(out, nodesForLastItem.size() - 2, i);

        for (; i < arrayLevel - 1; i++) {
            startNonleafNode(out, nodesInPath.get(i), i);
        }

        String objName = nodesInPath.get(i).getNodeKeyName();
        out.name(objName);
        out.beginArray();
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
    private void outputCldrItem(JsonWriter out,
        ArrayList<CldrNode> nodesForLastItem, CldrItem item)
        throws IOException, ParseException {
        // alias has been resolved, no need to keep it.
        if (item.isAliasItem()) {
            return;
        }

        ArrayList<CldrNode> nodesInPath = item.getNodesInPath();
        int arraySize = nodesInPath.size();

        int i = findFirstDiffNodeIndex(nodesForLastItem, nodesInPath);
        if (i == nodesInPath.size() && type != RunType.rbnf) {
            System.err.println("This nodes and last nodes has identical path. ("
                + item.getPath() + ") Some distinguishing attributes wrongly removed?");
            return;
        }

        // close previous nodes
        closeNodes(out, nodesForLastItem.size() - 2, i);

        for (; i < nodesInPath.size() - 1; ++i) {
            startNonleafNode(out, nodesInPath.get(i), i);
        }

        writeLeafNode(out, nodesInPath.get(i), item.getValue(), i);
        nodesForLastItem.clear();
        nodesForLastItem.addAll(nodesInPath);
    }

    /**
     * Close nodes that no longer appears in path.
     *
     * @param out
     *            The JsonWriter to hold all output lines.
     * @param last
     *            The last node index in previous item.
     * @param firstDiff
     *            The first different node in next item.
     * @throws IOException
     */
    private void closeNodes(JsonWriter out, int last, int firstDiff)
        throws IOException {
        for (int i = last; i >= firstDiff; --i) {
            if (i == 0) {
                out.endObject();
                break;
            }
            out.endObject();
        }
    }

    /**
     * Start a non-leaf node, write out its attributes.
     *
     * @param out
     *            The ArrayList to hold all output lines.
     * @param node
     *            The node being written.
     * @param level
     *            indentation level.
     * @throws IOException
     */
    private void startNonleafNode(JsonWriter out, CldrNode node, int level)
        throws IOException {
        String objName = node.getNodeKeyName();
        // Some node should be skipped as indicated by objName being null.
        if (objName == null) {
            return;
        }

        // first level needs no key, it is the container.
        if (level == 0) {
            out.beginObject();
            return;
        }

        Map<String, String> attrAsValueMap = node.getAttrAsValueMap();

        if (type == RunType.annotations || type == RunType.annotationsDerived) {
            if (objName.startsWith("U+")) {
                // parse U+22 -> "   etc
                out.name(com.ibm.icu.text.UTF16.valueOf(Integer.parseInt(objName.substring(2), 16)));
            } else {
                out.name(objName);
            }
        } else {
            out.name(objName);
        }

        out.beginObject();
        for (String key : attrAsValueMap.keySet()) {
            String rawAttrValue = attrAsValueMap.get(key);
            String value = escapeValue(rawAttrValue);
            // attribute is prefixed with "_" when being used as key.
            String attrAsKey = "_" + key;
            if (LdmlConvertRules.attrIsBooleanOmitFalse(node.getUntransformedPath(), node.getName(), node.getParent(), key)) {
                final Boolean v = Boolean.parseBoolean(rawAttrValue);
                if (v) {
                    out.name(attrAsKey).value(v);
                } // else, omit
            } else {
                out.name(attrAsKey).value(value);
            }
        }
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
    private void outputArrayItem(JsonWriter out, CldrItem item,
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
        int diff = findFirstDiffNodeIndex(nodesForLastItem, nodesInPath);
        CldrNode cldrNode = nodesInPath.get(nodesNum - 1);

        if (diff > arrayLevel) {
            // close previous nodes
            closeNodes(out, nodesForLastItem.size() - 1, diff + 1);

            for (int i = diff; i < nodesNum - 1; i++) {
                startNonleafNode(out, nodesInPath.get(i), i + 1);
            }
            writeLeafNode(out, cldrNode, value, nodesNum);
            return;
        }

        if (arrayLevel == nodesNum - 1) {
            // case 2
            // close previous nodes
            if (nodesForLastItem.size() - 1 - arrayLevel > 0) {
                closeNodes(out, nodesForLastItem.size() - 1, arrayLevel);
            }

            String objName = cldrNode.getNodeKeyName();
            int pos = objName.indexOf('-');
            if (pos > 0) {
                objName = objName.substring(0, pos);
            }

            Map<String, String> attrAsValueMap = cldrNode.getAttrAsValueMap();

            if (attrAsValueMap.isEmpty()) {
                out.beginObject();
                out.name(objName).value(value);
                out.endObject();
            } else if (objName.equals("rbnfrule")) {
                writeRbnfLeafNode(out, item, attrAsValueMap);
            } else {
                out.beginObject();
                writeLeafNode(out, objName, attrAsValueMap, value, nodesNum, cldrNode.getName(), cldrNode.getParent(), cldrNode);
                out.endObject();
            }
            // the last node is closed, remove it.
            nodesInPath.remove(nodesNum - 1);
        } else {
            // case 3
            // close previous nodes
            if (nodesForLastItem.size() - 1 - (arrayLevel) > 0) {
                closeNodes(out, nodesForLastItem.size() - 1, arrayLevel);
            }

            out.beginObject();

            CldrNode node = nodesInPath.get(arrayLevel);
            String objName = node.getNodeKeyName();
            int pos = objName.indexOf('-');
            if (pos > 0) {
                objName = objName.substring(0, pos);
            }
            Map<String, String> attrAsValueMap = node.getAttrAsValueMap();
            out.name(objName);
            out.beginObject();
            for (String key : attrAsValueMap.keySet()) {
                // attribute is prefixed with "_" when being used as key.
                out.name("_" + key).value(escapeValue(attrAsValueMap.get(key)));
            }

            for (int i = arrayLevel + 1; i < nodesInPath.size() - 1; i++) {
                startNonleafNode(out, nodesInPath.get(i), i + 1);
            }
            writeLeafNode(out, cldrNode, value, nodesNum);
        }

        nodesForLastItem.clear();
        nodesForLastItem.addAll(nodesInPath);
    }

    private void writeRbnfLeafNode(JsonWriter out, CldrItem item, Map<String, String> attrAsValueMap) throws IOException {
        if(attrAsValueMap.size() != 1) {
            throw new IllegalArgumentException("Error, attributes seem wrong for RBNF " + item.getUntransformedPath());
        }
        Entry<String, String> entry = attrAsValueMap.entrySet().iterator().next();
        out.beginArray()
            .value(entry.getKey())
            .value(entry.getValue())
            .endArray();
    }

    /**
     * Compare two nodes list, find first index that the two list have different
     * nodes and return it.
     *
     * @param nodesForLastItem
     *            Nodes from last item.
     * @param nodesInPath
     *            Nodes for current item.
     * @return The index of first different node.
     */
    private int findFirstDiffNodeIndex(ArrayList<CldrNode> nodesForLastItem,
        ArrayList<CldrNode> nodesInPath) {
        int i;
        for (i = 0; i < nodesInPath.size(); ++i) {
            if (i >= nodesForLastItem.size() ||
                !nodesInPath.get(i).getNodeDistinguishingName().equals(
                    nodesForLastItem.get(i).getNodeDistinguishingName())) {
                break;
            }
        }
        return i;
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
        return String.format("[%s]:\t", percentFormatter.format(asPercent));
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
                if(DEBUG) System.out.print("<" + progressPrefix(readCount, total, dirName, filename) + "\r");

                if (type == RunType.main) {
                    pathPrefix = "/cldr/" + dirName + "/" + localeIdToLangTag(filename) + "/";
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
                    if(DEBUG) System.out.println("." + progressPrefix(readCount, total) +
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
                System.err.println("\t- " + f.toString());
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
    private void writeLeafNode(JsonWriter out, CldrNode node, String value,
        int level) throws IOException {

        String objName = node.getNodeKeyName();
        Map<String, String> attrAsValueMaps = node.getAttrAsValueMap();
        writeLeafNode(out, objName, attrAsValueMaps, value, level, node.getName(), node.getParent(), node);
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
    private void writeLeafNode(JsonWriter out, String objName,
        Map<String, String> attrAsValueMap, String value, int level, final String nodeName,
            String parent, CldrNode node)
        throws IOException {
        if (objName == null) {
            return;
        }
        value = escapeValue(value);

        final boolean valueIsSpacesepArray = LdmlConvertRules.valueIsSpacesepArray(nodeName, parent);
        if (attrAsValueMap.isEmpty()) {
            out.name(objName);
            if (value.isEmpty()) {
                if (valueIsSpacesepArray) {
                    out.beginArray();
                    out.endArray();
                } else {
                    out.beginObject();
                    out.endObject();
                }
            } else if (type == RunType.annotations ||
                type == RunType.annotationsDerived) {
                out.beginArray();
                // split this, so "a | b | c" becomes ["a","b","c"]
                for (final String s : Annotations.splitter.split(value.trim())) {
                    out.value(s);
                }
                out.endArray();
            } else if(valueIsSpacesepArray) {
                outputSpaceSepArray(out, value);
            } else {
                // normal value
                out.value(value);
            }
            return;
        }

        // If there is no value, but a attribute being treated as value,
        // simplify the output.
        if (value.isEmpty() &&
            attrAsValueMap.containsKey(LdmlConvertRules.ANONYMOUS_KEY)) {
            String v = attrAsValueMap.get(LdmlConvertRules.ANONYMOUS_KEY);
            out.name(objName);
            if (valueIsSpacesepArray) {
                outputSpaceSepArray(out, v);
            } else {
                out.value(v);
            }
            return;
        }
        out.name(objName);
        out.beginObject();

        if (!value.isEmpty()) {
            out.name("_value").value(value);
        }

        for (String key : attrAsValueMap.keySet()) {
            String rawAttrValue = attrAsValueMap.get(key);
            String attrValue = escapeValue(rawAttrValue);
            // attribute is prefixed with "_" when being used as key.
            String attrAsKey = "_" + key;
            if (LdmlConvertRules.ATTRVALUE_AS_ARRAY_SET.contains(key)) {
                String[] strings = attrValue.trim().split("\\s+");
                out.name(attrAsKey);
                out.beginArray();
                for (String s : strings) {
                    out.value(s);
                }
                out.endArray();
            } else if (node != null &&
                LdmlConvertRules.attrIsBooleanOmitFalse(node.getUntransformedPath(), nodeName, parent, key)) {
                final Boolean v = Boolean.parseBoolean(rawAttrValue);
                if (v) {
                    out.name(attrAsKey).value(v);
                } // else: omit falsy value
            } else {
                out.name(attrAsKey).value(attrValue);
            }
        }
        out.endObject();
    }

    private void outputSpaceSepArray(JsonWriter out, String v) throws IOException {
        out.beginArray();
        // split this, so "a b c" becomes ["a","b","c"]
        for (final String s : v.trim().split(" ")) {
            if(!s.isEmpty()) {
                out.value(s);
            }
        }
        out.endArray();
    }
}
