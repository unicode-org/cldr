package org.unicode.cldr.json;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileProcessor;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import com.ibm.icu.dev.util.CollectionUtilities;

/**
 * Utility methods to extract data from CLDR repository and export it in JSON
 * format.
 *
 * @author shanjian / emmons
 */
@CLDRTool(alias = "ldml2json", description = "Convert CLDR data to JSON")
public class Ldml2JsonConverter {
    private static boolean DEBUG = false;

    private enum RunType {
        main, supplemental, segments, rbnf
    };

    private static final StandardCodes sc = StandardCodes.make();
    private Set<String> defaultContentLocales = SupplementalDataInfo.getInstance().getDefaultContentLocales();
    private Set<String> skippedDefaultContentLocales = new TreeSet<String>();

    private class availableLocales {
        Set<String> modern = new TreeSet<String>();
        Set<String> full = new TreeSet<String>();
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
                .add("type", 't', "(main|supplemental|segments|rbnf)", "main",
                    "Type of CLDR data being generated, main, supplemental, or segments.")
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
                .add("konfig", 'k', ".*", null, "LDML to JSON configuration file");

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
            options.get("konfig").getValue());

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
    private RunType type;

    private class JSONSection implements Comparable<JSONSection> {
        public String section;
        public Matcher matcher;
        public String packageName;

        public int compareTo(JSONSection other) {
            return section.compareTo(other.section);
        }

    }

    private Map<JSONSection, List<CldrItem>> sectionItems = new TreeMap<JSONSection, List<CldrItem>>();

    private Map<String, String> dependencies;
    private List<JSONSection> sections;
    private Set<String> packages;

    public Ldml2JsonConverter(String cldrDir, String outputDir, String runType, boolean fullNumbers, boolean resolve, String coverage, String match,
        boolean writePackages, String configFile) {
        this.cldrCommonDir = cldrDir;
        this.outputDir = outputDir;
        this.type = RunType.valueOf(runType);
        this.fullNumbers = fullNumbers;
        this.resolve = resolve;
        this.match = match;
        this.writePackages = writePackages;
        this.coverageValue = Level.get(coverage).getLevel();

        sections = new ArrayList<JSONSection>();
        packages = new TreeSet<String>();
        dependencies = new HashMap<String, String>();

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
                    j.matcher = PatternCache.get(path).matcher("");
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

            }
        }

        // Add a section at the end of the list that will match anything not already matched.
        JSONSection j = new JSONSection();
        j.section = "other";
        j.matcher = PatternCache.get(".*").matcher("");
        sections.add(j);

    }

    /**
     * Transform the path by applying PATH_TRANSFORMATIONS rules.
     *
     * @param pathStr
     *            The path string being transformed.
     * @return The transformed path.
     */
    private String transformPath(String pathStr, String pathPrefix) {
        String result = pathStr;

        if (DEBUG) {
            System.out.println(" IN pathStr : " + result);
        }
        Matcher m;
        for (int i = 0; i < LdmlConvertRules.PATH_TRANSFORMATIONS.length; i++) {
            m = LdmlConvertRules.PATH_TRANSFORMATIONS[i].pattern.matcher(pathStr);
            if (m.matches()) {
                if (DEBUG) {
                    System.out.println(LdmlConvertRules.PATH_TRANSFORMATIONS[i].pattern);
                }
                result = m.replaceFirst(LdmlConvertRules.PATH_TRANSFORMATIONS[i].replacement);
                break;
            }
        }
        result = result.replaceFirst("/ldml/", pathPrefix);
        result = result.replaceFirst("/supplementalData/", pathPrefix);

        if (result.contains("languages") ||
            result.contains("languageAlias") ||
            result.contains("languageMatches") ||
            result.contains("likelySubtags") ||
            result.contains("parentLocale") ||
            result.contains("locales=")) {
            result = result.replaceAll("_", "-");
        }
        if (DEBUG) {
            System.out.println("OUT pathStr : " + result);
        }

        if (DEBUG) {
            System.out.println("result: " + result);
        }
        return result;
    }

    private void mapPathsToSections(CLDRFile file, String pathPrefix, SupplementalDataInfo sdi)
        throws IOException, ParseException {

        String locID = file.getLocaleID();
        Matcher noNumberingSystemMatcher = LdmlConvertRules.NO_NUMBERING_SYSTEM_PATTERN.matcher("");
        Matcher numberingSystemMatcher = LdmlConvertRules.NUMBERING_SYSTEM_PATTERN.matcher("");
        Matcher rootIdentityMatcher = LdmlConvertRules.ROOT_IDENTITY_PATTERN.matcher("");
        Set<String> activeNumberingSystems = new TreeSet<String>();
        activeNumberingSystems.add("latn"); // Always include latin script numbers
        for (String np : LdmlConvertRules.ACTIVE_NUMBERING_SYSTEM_XPATHS) {
            String ns = file.getWinningValue(np);
            if (ns != null && ns.length() > 0) {
                activeNumberingSystems.add(ns);
            }
        }
        DtdType fileDtdType;
        if (CLDRFile.isSupplementalName(locID)) {
            fileDtdType = DtdType.supplementalData;
        } else {
            fileDtdType = DtdType.ldml;
        }
        CoverageInfo covInfo = CLDRConfig.getInstance().getCoverageInfo();
        for (Iterator<String> it = file.iterator("", DtdData.getInstance(fileDtdType).getDtdComparator(null)); it.hasNext();) {
            int cv = Level.UNDETERMINED.getLevel();
            String path = it.next();
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
                XPathParts xpp = new XPathParts();
                xpp.set(fullPath);
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

            for (JSONSection js : sections) {
                js.matcher.reset(transformedPath);
                if (js.matcher.matches()) {
                    CldrItem item = new CldrItem(transformedPath, transformedFullPath, path, fullPath, value);

                    List<CldrItem> cldrItems = sectionItems.get(js);
                    if (cldrItems == null) {
                        cldrItems = new ArrayList<CldrItem>();
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
            return;
        }
        List<CldrItem> otherSectionItems = new ArrayList<CldrItem>(others);
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
     */
    private void convertCldrItems(String dirName, String filename, String pathPrefix)
        throws IOException, ParseException {
        // zone and timezone items are queued for sorting first before they are
        // processed.

        for (JSONSection js : sections) {
            String outFilename;
            if (type == RunType.rbnf) {
                outFilename = filename.replaceAll("_", "-") + ".json";
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
                    if (type != RunType.supplemental && type != RunType.rbnf) {
                        LocaleIDParser lp = new LocaleIDParser();
                        lp.set(filename);
                        if (defaultContentLocales.contains(filename) &&
                            lp.getRegion().length() > 0) {
                            if (type == RunType.main) {
                                skippedDefaultContentLocales.add(filename.replaceAll("_", "-"));
                            }
                            continue;
                        }
                        Level localeCoverageLevel = sc.getLocaleCoverageLevel("Cldr", filename);
                        if (localeCoverageLevel == Level.MODERN || filename.equals("root")) {
                            tier = "-modern";
                            if (type == RunType.main) {
                                avl.modern.add(filename.replaceAll("_", "-"));
                            }
                        } else {
                            tier = "-full";
                        }
                        if (type == RunType.main) {
                            avl.full.add(filename.replaceAll("_", "-"));
                        }
                    } else if (type == RunType.rbnf) {
                        js.packageName = "rbnf";
                        tier = "";
                    }
                    if (js.packageName != null) {
                        String packageName = "cldr-" + js.packageName + tier;
                        outputDirname.append("/" + packageName);
                        packages.add(packageName);
                    }
                    outputDirname.append("/" + dirName + "/");
                    if (type != RunType.supplemental && type != RunType.rbnf) {
                        outputDirname.append(filename.replaceAll("_", "-"));
                    }
                    if (DEBUG) {
                        System.out.println("outDir: " + outputDirname);
                        System.out.println("pack: " + js.packageName);
                        System.out.println("dir: " + dirName);
                    }
                }

                File dir = new File(outputDirname.toString());
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                List<String> outputDirs = new ArrayList<String>();
                outputDirs.add(outputDirname.toString());
                if (writePackages && type == RunType.main && tier.equals("-modern")) {
                    outputDirs.add(outputDirname.toString().replaceFirst("-modern", "-full"));
                }

                for (String outputDir : outputDirs) {
                    List<CldrItem> theItems = sectionItems.get(js);
                    if (theItems == null || theItems.size() == 0) {
                        continue;
                    }
                    PrintWriter outf = FileUtilities.openUTF8Writer(outputDir, outFilename);
                    JsonWriter out = new JsonWriter(outf);
                    out.setIndent("  ");

                    ArrayList<CldrItem> sortingItems = new ArrayList<CldrItem>();
                    ArrayList<CldrItem> arrayItems = new ArrayList<CldrItem>();

                    ArrayList<CldrNode> nodesForLastItem = new ArrayList<CldrNode>();
                    String lastLeadingArrayItemPath = null;
                    String leadingArrayItemPath = "";
                    int valueCount = 0;
                    String previousIdentityPath = null;
                    for (CldrItem item : theItems) {

                        if (type == RunType.rbnf) {
                            item.setValue(item.getValue().replace('→', '>'));
                            item.setValue(item.getValue().replace('←', '<'));
                            if (item.getFullPath().contains("@value")) {
                                int indexStart = item.getFullPath().indexOf("@value") + 8;
                                int indexEnd = item.getFullPath().indexOf("]", indexStart) - 1;
                                if (indexStart >= 0 && indexEnd >= 0 && indexEnd > indexStart) {
                                    String sub = item.getFullPath().substring(indexStart, indexEnd);
                                    /* System.out.println("sub: " + sub);
                                    System.out.println("full: " + item.getFullPath());
                                    System.out.println("val: " + item.getValue());*/
                                    item.setFullPath(item.getFullPath().replace(sub, item.getValue()));
                                    item.setFullPath(item.getFullPath().replaceAll("@value", "@" + sub));
                                    //System.out.println("modifyfull: " + item.getFullPath());
                                    item.setValue("");
                                }
                            }

                        }
                        // ADJUST ACCESS=PRIVATE/PUBLIC BASED ON ICU RULE -- START
                        if (type == RunType.rbnf) {
                            String fullpath = item.getFullPath();
                            if (fullpath.contains("/ruleset")) {
                                int ruleStartIndex = fullpath.indexOf("/ruleset[");
                                String checkString = fullpath.substring(ruleStartIndex);

                                int ruleEndIndex = 0;
                                if (checkString.contains("/")) {
                                    ruleEndIndex = fullpath.indexOf("/", ruleStartIndex + 1);
                                }
                                if (ruleEndIndex > ruleStartIndex) {
                                    String oldRulePath = fullpath.substring(ruleStartIndex, ruleEndIndex);

                                    String newRulePath = oldRulePath;
                                    if (newRulePath.contains("@type")) {
                                        int typeIndexStart = newRulePath.indexOf("\"", newRulePath.indexOf("@type"));
                                        int typeIndexEnd = newRulePath.indexOf("\"", typeIndexStart + 1);
                                        String type = newRulePath.substring(typeIndexStart + 1, typeIndexEnd);

                                        String newType = "";
                                        if (newRulePath.contains("@access")) {
                                            newType = "%%" + type;
                                        } else {
                                            newType = "%" + type;
                                        }
                                        newRulePath = newRulePath.replace(type, newType);
                                        item.setPath(item.getPath().replace(type, newType));
                                    }
                                    fullpath = fullpath.replace(oldRulePath, newRulePath);
                                    item.setFullPath(fullpath);

                                }
                            }
                        }
                        // ADJUST ACCESS=PRIVATE/PUBLIC BASED ON ICU RULE -- END

                        // items in the identity section of a file should only ever contain the lowest level, even if using
                        // resolving source, so if we have duplicates ( caused by attributes used as a value ) then suppress
                        // them here.
                        if (item.getPath().contains("/identity/")) {
                            XPathParts xpp = new XPathParts();
                            String[] parts = item.getPath().split("\\[");
                            if (parts[0].equals(previousIdentityPath)) {
                                continue;
                            } else {
                                xpp.set(item.getPath());
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
                        CldrItem[] items = item.split();
                        if (items == null) {
                            items = new CldrItem[1];
                            items[0] = item;
                        }
                        valueCount += items.length;

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
                    System.out.println(String.format("  %s = %d values", outFilename, valueCount));
                    closeNodes(out, nodesForLastItem.size() - 2, 0);
                    outf.println();
                    out.close();
                }
            }
        }
    }

    /**
     * Creates the packaging files ( i.e. package.json ) for a particular package
     *
     * @param packageName
     *            The name of the installable package
     */
    public void writePackagingFiles(String outputDir, String packageName) throws IOException {
        writePackageJson(outputDir, packageName);
        writeBowerJson(outputDir, packageName);
    }

    public void writeBasicInfo(JsonObject obj, String packageName, boolean isNPM) {

        obj.addProperty("name", packageName);
        String versionString = CLDRFile.GEN_VERSION;
        while (versionString.split("\\.").length < 3) {
            versionString = versionString + ".0";
        }
        obj.addProperty("version", versionString);

        String[] packageNameParts = packageName.split("-");
        String dependency = dependencies.get(packageNameParts[1]);
        if (dependency != null) {
            String[] dependentPackageNames = new String[1];
            String tier = packageNameParts[packageNameParts.length - 1];
            if (dependency.equals("core")) {
                dependentPackageNames[0] = "cldr-core";
            } else {
                dependentPackageNames[0] = "cldr-" + dependency + "-" + tier;
            }

            JsonObject dependencies = new JsonObject();
            for (String dependentPackageName : dependentPackageNames) {
                if (dependentPackageName != null) {
                    dependencies.addProperty(dependentPackageName, versionString);
                }
            }
            obj.add(isNPM ? "peerDependencies" : "dependencies", dependencies);
        }
    }

    public void writePackageJson(String outputDir, String packageName) throws IOException {
        PrintWriter outf = FileUtilities.openUTF8Writer(outputDir + "/" + packageName, "package.json");
        System.out.println("Creating packaging file => " + outputDir + File.separator + packageName + File.separator + "package.json");
        JsonObject obj = new JsonObject();
        writeBasicInfo(obj, packageName, true);

        JsonArray licenses = new JsonArray();
        JsonArray maintainers = new JsonArray();
        JsonObject UnicodeLicense = new JsonObject();
        JsonObject primaryMaintainer = new JsonObject();

        obj.addProperty("homepage", "http://cldr.unicode.org");
        obj.addProperty("author", "The Unicode Consortium");

        primaryMaintainer.addProperty("name", "John Emmons");
        primaryMaintainer.addProperty("email", "emmo@us.ibm.com");
        primaryMaintainer.addProperty("url", "https://github.com/JCEmmons");
        maintainers.add(primaryMaintainer);
        obj.add("maintainers", maintainers);

        JsonObject repository = new JsonObject();
        repository.addProperty("type", "git");
        repository.addProperty("url", "git://github.com/unicode-cldr/" + packageName + ".git");
        obj.add("repository", repository);

        UnicodeLicense.addProperty("type", "Unicode-TOU");
        UnicodeLicense.addProperty("url", "http://www.unicode.org/copyright.html");
        licenses.add(UnicodeLicense);
        obj.add("licenses", licenses);

        obj.addProperty("bugs", "http://unicode.org/cldr/trac/newticket");

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
            mainPaths.add(new JsonPrimitive("defaultContent.json"));
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
        Map<String, Info> scriptInfo = new TreeMap<String, Info>();
        for (String script : ScriptMetadata.getScripts()) {
            Info i = ScriptMetadata.getInfo(script);
            scriptInfo.put(script, i);
        }
        if (ScriptMetadata.errors.size() > 0) {
            System.err.println(CollectionUtilities.join(ScriptMetadata.errors, "\n\t"));
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
        ArrayList<CldrItem> arrayItems = new ArrayList<CldrItem>();
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
        boolean rbnfFlag = false;
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
            if (arrayItems.get(0).getFullPath().contains("rbnfrule")) {
                rbnfFlag = true;
                out.beginObject();
            }
            for (CldrItem insideItem : arrayItems) {

                outputArrayItem(out, insideItem, nodesForLastItem, arrayLevel);

            }
            if (rbnfFlag) {
                out.endObject();
            }

            arrayItems.clear();

            int lastLevel = nodesForLastItem.size() - 1;
            closeNodes(out, lastLevel, arrayLevel);
            if (!rbnfFlag) {
                out.endArray();
            }
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
        if (!item.getFullPath().contains("rbnfrule")) {
            out.beginArray();
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

        out.name(objName);
        out.beginObject();
        for (String key : attrAsValueMap.keySet()) {
            String value = escapeValue(attrAsValueMap.get(key));
            // attribute is prefixed with "_" when being used as key.
            out.name("_" + key).value(value);
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
        if (diff > arrayLevel) {
            // close previous nodes
            closeNodes(out, nodesForLastItem.size() - 1, diff + 1);

            for (int i = diff; i < nodesNum - 1; i++) {
                startNonleafNode(out, nodesInPath.get(i), i + 1);
            }
            writeLeafNode(out, nodesInPath.get(nodesNum - 1), value, nodesNum);
            return;
        }

        if (arrayLevel == nodesNum - 1) {
            // case 2
            // close previous nodes
            if (nodesForLastItem.size() - 1 - arrayLevel > 0) {
                closeNodes(out, nodesForLastItem.size() - 1, arrayLevel);
            }

            String objName = nodesInPath.get(nodesNum - 1).getNodeKeyName();
            int pos = objName.indexOf('-');
            if (pos > 0) {
                objName = objName.substring(0, pos);
            }

            Map<String, String> attrAsValueMap = nodesInPath.get(nodesNum - 1).getAttrAsValueMap();

            // ADJUST RADIX BASED ON ICU RULE -- BEGIN
            if (attrAsValueMap.containsKey("radix")) {
                String radixValue = attrAsValueMap.get("radix");
                attrAsValueMap.remove("radix");
                for (Map.Entry<String, String> attributes : attrAsValueMap.entrySet()) {
                    String oldKey = attributes.getKey();
                    String newValue = attributes.getValue();
                    String newKey = oldKey + "/" + radixValue;
                    attrAsValueMap.remove(oldKey);
                    attrAsValueMap.put(newKey, newValue);

                }
            }
            // ADJUST RADIX BASED ON ICU RULE -- END

            if (attrAsValueMap.isEmpty()) {
                out.beginObject();
                out.name(objName).value(value);
                out.endObject();
            } else {
                if (!objName.equals("rbnfrule")) {
                    out.beginObject();
                }
                writeLeafNode(out, objName, attrAsValueMap, value, nodesNum);
                if (!objName.equals("rbnfrule")) {
                    out.endObject();
                }

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
            writeLeafNode(out, nodesInPath.get(nodesNum - 1), value, nodesNum);
        }

        nodesForLastItem.clear();
        nodesForLastItem.addAll(nodesInPath);
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
        Set<String> files = cldrFactory.getAvailable();
        for (String filename : files) {
            if (LdmlConvertRules.IGNORE_FILE_SET.contains(filename)) {
                continue;
            }
            if (!filename.matches(match)) {
                continue;
            }

            System.out.println("Processing file " + dirName + "/" + filename);
            String pathPrefix;
            CLDRFile file = cldrFactory.make(filename, resolve && type == RunType.main, minimalDraftStatus);

            sectionItems.clear();
            if (type == RunType.main) {
                pathPrefix = "/cldr/" + dirName + "/" + filename.replaceAll("_", "-") + "/";
            } else {
                pathPrefix = "/cldr/" + dirName + "/";
            }
            mapPathsToSections(file, pathPrefix, sdi);

            convertCldrItems(dirName, filename, pathPrefix);

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
        writeLeafNode(out, objName, attrAsValueMaps, value, level);
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
     * @throws IOException
     */
    private void writeLeafNode(JsonWriter out, String objName,
        Map<String, String> attrAsValueMap, String value, int level)
        throws IOException {
        if (objName == null) {
            return;
        }
        value = escapeValue(value);

        if (attrAsValueMap.isEmpty()) {
            if (value.isEmpty()) {
                out.name(objName);
                out.beginObject();
                out.endObject();
            } else {
                out.name(objName).value(value);
            }
            return;
        }

        // If there is no value, but a attribute being treated as value,
        // simplify the output.
        if (value.isEmpty() &&
            attrAsValueMap.containsKey(LdmlConvertRules.ANONYMOUS_KEY)) {
            out.name(objName).value(attrAsValueMap.get(LdmlConvertRules.ANONYMOUS_KEY));
            return;
        }
        if (!objName.equals("rbnfrule")) {
            out.name(objName);
            out.beginObject();
        }

        if (!value.isEmpty()) {
            out.name("_value").value(value);
        }

        for (String key : attrAsValueMap.keySet()) {
            String attrValue = escapeValue(attrAsValueMap.get(key));
            // attribute is prefixed with "_" when being used as key.
            if (LdmlConvertRules.ATTRVALUE_AS_ARRAY_SET.contains(key)) {
                String[] strings = attrValue.trim().split("\\s+");
                if (type != RunType.rbnf) {
                    out.name("_" + key);
                } else {
                    out.name(key);
                }
                out.beginArray();
                for (String s : strings) {
                    out.value(s);
                }
                out.endArray();
            } else {
                if (type != RunType.rbnf) {
                    out.name("_" + key).value(attrValue);
                } else {
                    out.name(key).value(attrValue);
                }

            }
        }
        if (!objName.equals("rbnfrule")) {
            out.endObject();
        }
    }
}
