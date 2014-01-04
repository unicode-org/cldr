package org.unicode.cldr.json;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.impl.Utility;

/**
 * Utility methods to extract data from CLDR repository and export it in JSON
 * format.
 * 
 * @author shanjian / emmons
 */
public class Ldml2JsonConverter {
    private static boolean DEBUG = false;
    private static final String MAIN = "main";
    private static final String SEGMENTS = "segments";

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
        .add("type", 't', "(main|supplemental|segments)", "main",
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
        .add("identity", 'i', "(true|false)", "true",
            "Whether to copy the identity info into all sections containing data")
        .add("konfig", 'k', ".*", null, "LDML to JSON configuration file");

    public static void main(String[] args) throws Exception {
        options.parse(args, true);

        Ldml2JsonConverter extractor = new Ldml2JsonConverter(
            options.get("commondir").getValue(),
            options.get("destdir").getValue(),
            Boolean.parseBoolean(options.get("fullnumbers").getValue()),
            Boolean.parseBoolean(options.get("resolved").getValue()),
            options.get("coverage").getValue(),
            options.get("match").getValue(),
            options.get("konfig").getValue());

        long start = System.currentTimeMillis();
        DraftStatus status = DraftStatus.valueOf(options.get("draftstatus").getValue());
        extractor.processDirectory(options.get("type").getValue(), status);
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

    private class JSONSection implements Comparable<JSONSection> {
        public String section;
        public Matcher matcher;

        public int compareTo(JSONSection other) {
            return section.compareTo(other.section);
        }

    }

    private Map<JSONSection, List<CldrItem>> sectionItems = new TreeMap<JSONSection, List<CldrItem>>();

    private List<JSONSection> sections;

    public Ldml2JsonConverter(String cldrDir, String outputDir, boolean fullNumbers, boolean resolve, String coverage, String match,
        String configFile) {
        this.cldrCommonDir = cldrDir;
        this.outputDir = outputDir;
        this.fullNumbers = fullNumbers;
        this.resolve = resolve;
        this.match = match;
        this.coverageValue = Level.get(coverage).getLevel();

        sections = new ArrayList<JSONSection>();
        FileUtilities.FileProcessor myReader = new FileUtilities.FileProcessor() {
            @Override
            protected boolean handleLine(int lineCount, String line) {
                String[] lineParts = line.trim().split("\\s*;\\s*");
                String key, value, section = null, path = null;
                boolean hasSection = false;
                boolean hasPath = false;
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
                    }
                    if (hasSection && hasPath) {
                        JSONSection j = new JSONSection();
                        j.section = section;
                        j.matcher = Pattern.compile(path).matcher("");
                        sections.add(j);
                    }
                }

                return true;
            }
        };

        if (configFile != null) {
            myReader.process(configFile);
        } else {
            myReader.process(Ldml2JsonConverter.class, "JSON_config.txt");
        }

        // Add a section at the end of the list that will match anything not already matched.
        JSONSection j = new JSONSection();
        j.section = "other";
        j.matcher = Pattern.compile(".*").matcher("");
        sections.add(j);

    }

    /**
     * Indent to specified level.
     * 
     * @param level
     *            Level of indent.
     * @return indentation string.
     * @throws IOException
     */
    private String indent(int level) throws IOException {
        return Utility.repeat("  ", level);
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
        return result;
    }

    private void mapPathsToSections(CLDRFile file, String pathPrefix, SupplementalDataInfo sdi)
        throws IOException, ParseException {

        String locID = file.getLocaleID();

        Matcher noNumberingSystemMatcher = LdmlConvertRules.NO_NUMBERING_SYSTEM_PATTERN.matcher("");
        Matcher numberingSystemMatcher = LdmlConvertRules.NUMBERING_SYSTEM_PATTERN.matcher("");
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
        for (Iterator<String> it = file.iterator("", DtdData.getInstance(fileDtdType).getDtdComparator(null)); it.hasNext();) {
            int cv = Level.UNDETERMINED.getLevel();
            String path = it.next();
            String fullPath = file.getFullXPath(path);
            String value = file.getWinningValue(path);

            if (fullPath == null) {
                fullPath = path;
            }

            if (!CLDRFile.isSupplementalName(locID) && path.startsWith("//ldml/") && !path.contains("/identity")) {
                cv = sdi.getCoverageValue(path, locID);
            }
            if (cv > coverageValue) {
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
                    CldrItem item = new CldrItem(transformedPath, transformedFullPath, path, fullPath, file.getWinningValue(path));
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

        Matcher versionInfoMatcher = Pattern.compile(".*/(identity|version|generation).*").matcher("");
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
    private void convertCldrItems(String outDirname, String pathPrefix)
        throws IOException, ParseException {
        // zone and timezone items are queued for sorting first before they are
        // processed.

        for (JSONSection js : sections) {
            ArrayList<CldrItem> sortingItems = new ArrayList<CldrItem>();
            ArrayList<CldrItem> arrayItems = new ArrayList<CldrItem>();

            ArrayList<String> out = new ArrayList<String>();
            ArrayList<CldrNode> nodesForLastItem = new ArrayList<CldrNode>();
            String lastLeadingArrayItemPath = null;
            String leadingArrayItemPath = "";
            int valueCount = 0;
            String previousIdentityPath = null;
            List<CldrItem> theItems = sectionItems.get(js);
            if (theItems == null || theItems.size() == 0) {
                continue;
            }
            for (CldrItem item : theItems) {

                // items in the identity section of a file should only ever contain the lowest level, even if using
                // resolving source, so if we have duplicates ( caused by attributes used as a value ) then suppress
                // them here.
                if (item.getPath().contains("/identity/")) {
                    String[] parts = item.getPath().split("\\[");
                    if (parts[0].equals(previousIdentityPath)) {
                        continue;
                    } else {
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

            closeNodes(out, nodesForLastItem.size() - 2, 0);
            String outFilename;
            outFilename = js.section + ".json";
            boolean writeOther = Boolean.parseBoolean(options.get("other").getValue());
            if (js.section.equals("other") && !writeOther) {
                continue;
            } else {
                writeToFile(outDirname, outFilename, out);
            }

            System.out.println(String.format("  %s = %d values", outFilename, valueCount));
        }
    }

    /**
     * Write all the line of the generated JSON output to files. Add comma if
     * necessary, and add newline.
     * 
     * @param outFilename
     *            The filename for output file.
     * @param out
     *            Lines of JSON representation.
     * @throws IOException
     */
    private void writeToFile(String outputDir, String outFilename, ArrayList<String> out) throws IOException {
        PrintWriter outf = BagFormatter.openUTF8Writer(outputDir, outFilename);
        Iterator<String> it = out.iterator();
        int lastSpaceCount = -1;
        String line;
        int spaceCount;
        while (it.hasNext()) {
            line = it.next();
            spaceCount = 0;
            while (line.charAt(spaceCount) == ' ') {
                spaceCount++;
            }
            if (spaceCount == lastSpaceCount) {
                outf.println(",");
            } else if (lastSpaceCount >= 0) {
                outf.println();
            }
            outf.print(line);
            lastSpaceCount = spaceCount;
        }
        outf.println();
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
    private void resolveSortingItems(ArrayList<String> out,
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
    private void resolveArrayItems(ArrayList<String> out,
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

            out.add(indent(arrayLevel - 1) + "]");
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
    private void outputStartArray(ArrayList<String> out,
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
        out.add(indent(i) + "\"" + objName + "\": [");
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
    private void outputCldrItem(ArrayList<String> out,
        ArrayList<CldrNode> nodesForLastItem, CldrItem item)
        throws IOException, ParseException {
        // alias has been resolved, no need to keep it.
        if (item.isAliasItem()) {
            return;
        }

        ArrayList<CldrNode> nodesInPath = item.getNodesInPath();

        int i = findFirstDiffNodeIndex(nodesForLastItem, nodesInPath);
        if (i == nodesInPath.size()) {
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
     *            The ArrayList to hold all output lines.
     * @param last
     *            The last node index in previous item.
     * @param firstDiff
     *            The first different node in next item.
     * @throws IOException
     */
    private void closeNodes(ArrayList<String> out, int last, int firstDiff)
        throws IOException {
        for (int i = last; i >= firstDiff; --i) {
            if (i == 0) {
                out.add("}");
                break;
            }
            out.add(indent(i) + "}");
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
    private void startNonleafNode(ArrayList<String> out, CldrNode node, int level)
        throws IOException {
        String objName = node.getNodeKeyName();
        // Some node should be skipped as indicated by objName being null.
        if (objName == null) {
            return;
        }

        // first level needs no key, it is the container.
        if (level == 0) {
            out.add("{");
            return;
        }

        Map<String, String> attrAsValueMap = node.getAttrAsValueMap();
        out.add(indent(level) + "\"" + objName + "\": {");
        for (String key : attrAsValueMap.keySet()) {
            String value = escapeValue(attrAsValueMap.get(key));
            // attribute is prefixed with "_" when being used as key.
            out.add(indent(level + 1) + "\"_" + key + "\": \"" + value + "\"");
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
    private void outputArrayItem(ArrayList<String> out, CldrItem item,
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

            Map<String, String> attrAsValueMap =
                nodesInPath.get(nodesNum - 1).getAttrAsValueMap();

            if (attrAsValueMap.isEmpty()) {
                out.add(indent(nodesNum - 1) + "{ \"" + objName + "\": \"" + value + "\"}");
            } else {
                out.add(indent(nodesNum - 1) + "{");
                writeLeafNode(out, objName, attrAsValueMap, value, nodesNum);
                out.add(indent(nodesNum - 1) + "}");
            }
            // the last node is closed, remove it.
            nodesInPath.remove(nodesNum - 1);
        } else {
            // case 3
            // close previous nodes
            if (nodesForLastItem.size() - 1 - (arrayLevel) > 0) {
                closeNodes(out, nodesForLastItem.size() - 1, arrayLevel);
            }

            out.add(indent(arrayLevel) + "{");

            CldrNode node = nodesInPath.get(arrayLevel);
            String objName = node.getNodeKeyName();
            int pos = objName.indexOf('-');
            if (pos > 0) {
                objName = objName.substring(0, pos);
            }
            Map<String, String> attrAsValueMap = node.getAttrAsValueMap();
            out.add(indent(arrayLevel + 1) + "\"" + objName + "\": {");
            for (String key : attrAsValueMap.keySet()) {
                // attribute is prefixed with "_" when being used as key.
                out.add(indent(arrayLevel + 2) + "\"_" + key + "\": \"" +
                    escapeValue(attrAsValueMap.get(key)) + "\"");
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
            CLDRFile file = cldrFactory.make(filename, resolve && dirName.equals(MAIN), minimalDraftStatus);

            sectionItems.clear();
            if (dirName.equals(MAIN)) {
                pathPrefix = "/cldr/" + dirName + "/" + filename.replaceAll("_", "-") + "/";
            } else {
                pathPrefix = "/cldr/" + dirName + "/";
            }
            mapPathsToSections(file, pathPrefix, sdi);

            String outputDirname;
            if (dirName.equals(MAIN) || dirName.equals(SEGMENTS)) {
                outputDirname = outputDir + File.separator + filename.replaceAll("_", "-");
            } else {
                outputDirname = outputDir + File.separator + dirName;
            }
            File dir = new File(outputDirname);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            convertCldrItems(outputDirname, pathPrefix);
        }
    }

    /**
     * Replacement pattern for escaping.
     */
    private static final Pattern escapePattern = Pattern.compile("\\\\(?!u)");

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
    private void writeLeafNode(ArrayList<String> out, CldrNode node, String value,
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
    private void writeLeafNode(ArrayList<String> out, String objName,
        Map<String, String> attrAsValueMap, String value, int level)
        throws IOException {
        if (objName == null) {
            return;
        }
        value = escapeValue(value);

        if (attrAsValueMap.isEmpty()) {
            if (value.isEmpty()) {
                out.add(indent(level) + "\"" + objName + "\": {}");
            } else {
                out.add(indent(level) + "\"" + objName + "\": \"" + value + "\"");
            }
            return;
        }

        // If there is no value, but a attribute being treated as value,
        // simplify the output.
        if (value.isEmpty() &&
            attrAsValueMap.containsKey(LdmlConvertRules.ANONYMOUS_KEY)) {
            out.add(indent(level) + "\"" + objName + "\": \"" +
                attrAsValueMap.get(LdmlConvertRules.ANONYMOUS_KEY) + "\"");
            return;
        }

        out.add(indent(level) + "\"" + objName + "\": {");

        if (!value.isEmpty()) {
            out.add(indent(level + 1) + "\"_value\": \"" + value + "\"");
        }

        for (String key : attrAsValueMap.keySet()) {
            String attrValue = escapeValue(attrAsValueMap.get(key));
            // attribute is prefixed with "_" when being used as key.
            if (LdmlConvertRules.ATTRVALUE_AS_ARRAY_SET.contains(key)) {
                String[] strings = attrValue.trim().split("\\s+");
                out.add(indent(level + 1) + "\"_" + key + "\": [");
                for (String s : strings) {
                    out.add(indent(level + 2) + "\"" + s + "\"");
                }
                out.add(indent(level + 1) + "]");
            } else {
                out.add(indent(level + 1) + "\"_" + key + "\": \"" + attrValue + "\"");
            }
        }
        out.add(indent(level) + "}");
    }
}
