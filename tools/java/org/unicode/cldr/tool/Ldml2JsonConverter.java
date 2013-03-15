package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
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
    private static final String MAIN = "main";

    private static final Options options = new Options(
        "Usage: LDML2JsonConverter [OPTIONS] [FILES]\n" +
            "This program converts CLDR data to the JSON format.\n" +
            "Please refer to the following options. \n" +
            "\texample: org.unicode.cldr.tool.Ldml2JsonConverter -c xxx -d yyy")
        .add("commondir", ".*", CldrUtility.COMMON_DIRECTORY,
            "Common directory for CLDR files, defaults to CldrUtility.COMMON_DIRECTORY")
        .add("destdir", ".*", CldrUtility.GEN_DIRECTORY,
            "Destination directory for output files, defaults to CldrUtility.GEN_DIRECTORY")
        .add("match", 'm', ".*", ".*",
            "Regular expression to define only specific locales or files to be generated")
        .add("fullnumbers", 'n', "(true|false)", "false",
            "Whether the output JSON should output data for all numbering systems, even those not used in the locale")
        .add("resolved", 'r', "(true|false)", "false",
            "Whether the output JSON for the main directory should be based on resolved or unresolved data")
        .add("draftstatus", 's', "(approved|contributed|provisional|unconfirmed)", "unconfirmed",
            "The minimum draft status of the output data");

    public static void main(String[] args) throws Exception {
        options.parse(args, true);

        Ldml2JsonConverter extractor = new Ldml2JsonConverter(
            options.get("commondir").getValue(),
            options.get("destdir").getValue(),
            Boolean.parseBoolean(options.get("fullnumbers").getValue()),
            Boolean.parseBoolean(options.get("resolved").getValue()),
            options.get("match").getValue());

        long start = System.currentTimeMillis();
        DraftStatus status = DraftStatus.valueOf(options.get("draftstatus").getValue());
        for (String dir : LdmlConvertRules.CLDR_SUBDIRS) {
            System.out.println("Processing directory " + dir);
            extractor.processDirectory(dir, status);
        }
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

    public Ldml2JsonConverter(String cldrDir, String outputDir, boolean fullNumbers, boolean resolve, String match) {
        this.cldrCommonDir = cldrDir;
        this.outputDir = outputDir;
        this.fullNumbers = fullNumbers;
        this.resolve = resolve;
        this.match = match;
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
    private String transformPath(String pathStr) {
        Matcher m;
        for (int i = 0; i < LdmlConvertRules.PATH_TRANSFORMATIONS.length; i++) {
            m = LdmlConvertRules.PATH_TRANSFORMATIONS[i].pattern.matcher(pathStr);
            if (m.matches()) {
                return m.replaceFirst(
                    LdmlConvertRules.PATH_TRANSFORMATIONS[i].replacement);
            }
        }
        return pathStr;
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
    private void convertCldrFile(CLDRFile file, String outDirname, String outFilename)
        throws IOException, ParseException {
        // zone and timezone items are queued for sorting first before they are
        // processed.
        ArrayList<CldrItem> sortingItems = new ArrayList<CldrItem>();
        ArrayList<CldrItem> arrayItems = new ArrayList<CldrItem>();

        ArrayList<String> out = new ArrayList<String>();
        ArrayList<CldrNode> nodesForLastItem = new ArrayList<CldrNode>();
        String lastLeadingArrayItemPath = null;
        String leadingArrayItemPath = "";
        int valueCount = 0;
        String previousIdentityPath = null;
        Matcher noNumberingSystemMatcher = LdmlConvertRules.NO_NUMBERING_SYSTEM_PATTERN.matcher("");
        Matcher numberingSystemMatcher = LdmlConvertRules.NUMBERING_SYSTEM_PATTERN.matcher("");
        Set<String> activeNumberingSystems = new TreeSet<String>();
        for (Iterator<String> it = file.iterator("", CLDRFile.ldmlComparator); it.hasNext();) {
            String path = it.next();
            String fullPath = file.getFullXPath(path);
            if (fullPath == null) {
                fullPath = path;
            }

            String transformedPath = transformPath(path);
            String transformedFullPath = transformPath(fullPath);
            CldrItem item = new CldrItem(transformedPath, transformedFullPath,
                file.getStringValue(path));

            // automatically filter out number symbols and formats without a numbering system
            noNumberingSystemMatcher.reset(fullPath);
            if (noNumberingSystemMatcher.matches()) {
                continue;
            }

            // Filter out non-active numbering systems data unless fullNumbers is specified.
            numberingSystemMatcher.reset(fullPath);
            if (numberingSystemMatcher.matches() && !fullNumbers) {
                if (activeNumberingSystems.isEmpty()) {
                    activeNumberingSystems.add("latn"); // Always include latin script numbers
                    for (String np : LdmlConvertRules.ACTIVE_NUMBERING_SYSTEM_XPATHS) {
                        String ns = file.getWinningValue(np);
                        if (ns != null && ns.length() > 0) {
                            activeNumberingSystems.add(ns);
                        }
                    }
                }
                XPathParts xpp = new XPathParts();
                xpp.set(fullPath);
                String currentNS = xpp.getAttributeValue(2, "numberSystem");
                if (currentNS != null && !activeNumberingSystems.contains(currentNS)) {
                    continue;
                }

            }
            // items in the identity section of a file should only ever contain the lowest level, even if using
            // resolving source, so if we have duplicates ( caused by attributes used as a value ) then suppress them
            // here.
            if (path.startsWith("//ldml/identity/")) {
                String[] parts = path.split("\\[");
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

        writeToFile(outDirname, outFilename, out);

        System.out.println(String.format("%s = %d values", outFilename, valueCount));
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
        CldrItem fakeItem = new CldrItem(leadingPath, leadingPath, "");
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

        // first level need no key, it is the container.
        if (level == 0) {
            out.add("{");
            return;
        }

        Map<String, String> attrAsValueMap = node.getAttrAsValueMap();
        out.add(indent(level) + "\"" + objName + "\": {");
        for (String key : attrAsValueMap.keySet()) {
            String value = escapeValue(attrAsValueMap.get(key));
            // attribute is prefixed with "@" when being used as key.
            out.add(indent(level + 1) + "\"@" + key + "\": \"" + value + "\"");
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
                // attribute is prefixed with "@" when being used as key.
                out.add(indent(arrayLevel + 2) + "\"@" + key + "\": \"" +
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
        File dir = new File(outputDir + dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }

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
            CLDRFile file = cldrFactory.make(filename, resolve && dirName.equals(MAIN),
                minimalDraftStatus);
            String outputFile = dirName.equals(MAIN) ?
                filename.replaceAll("_", "-") + ".json" :
                filename + ".json";
            String outputDirname = outputDir + dirName;
            convertCldrFile(file, outputDirname, outputFile);
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
            // attribute is prefixed with "@" when being used as key.
            out.add(indent(level + 1) + "\"@" + key + "\": \"" + attrValue + "\"");
        }
        out.add(indent(level) + "}");
    }
}
