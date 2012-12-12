package org.unicode.cldr.tool;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A object to present a CLDR XML item.
 */
public class CldrItem implements Comparable<CldrItem> {

    /**
     * Split the path to an array of string, each string represent a segment.
     * 
     * @param path
     *            The path of XML element.
     * @return array of segments.
     */
    private static String[] splitPathToSegments(String path) {
        // remove leading //
        if (path.startsWith("//")) {
            path = path.substring(2);
        }

        int start = 0;
        ArrayList<String> segments = new ArrayList<String>();
        boolean inBracket = false;
        boolean inBracketQuote = false;
        for (int pos = start; pos < path.length(); ++pos) {
            char ch = path.charAt(pos);
            if (inBracketQuote) {
                if (ch == '"') {
                    inBracketQuote = false;
                }
            } else if (inBracket) {
                if (ch == ']') {
                    inBracket = false;
                } else if (ch == '"') {
                    inBracketQuote = true;
                }
            } else {
                if (ch == '[') {
                    inBracket = true;
                } else if (ch == '/') {
                    segments.add(path.substring(start, pos));
                    start = pos + 1;
                }
            }
        }
        segments.add(path.substring(start, path.length()));

        return segments.toArray(new String[segments.size()]);
    }

    /**
     * The full path of a CLDR item.
     * 
     * Comparing to path, this full contains non-distinguishable attributes.
     */
    private String fullPath;

    /**
     * The resolution path of a CLDR item.
     * 
     * This path only contains distinguishable attributes that are necessary to
     * identify a CLDR XML item in the CLDR tree.
     */
    private String path;

    /**
     * The value of this CLDR item.
     */
    private String value;

    /**
     * The key used for sorting.
     */
    private String sortKey;

    CldrItem(String path, String fullPath, String value) {
        // for pluralRules, attribute "locales" should be treated as distinguishing
        // attribute
        if (path.startsWith("//supplementalData/plurals/pluralRules")) {
            int start = fullPath.indexOf("[@locales=");
            int end = fullPath.indexOf("]", start);
            path = path.substring(0, 38) + fullPath.substring(start, end + 1) +
                path.substring(38);
        }

        this.path = path;
        this.fullPath = fullPath;

        if (value == null) {
            this.value = "";
        } else {
            this.value = value;
        }

        sortKey = null;
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getPath() {
        return path;
    }

    /**
     * The pre-compiled regex pattern for removing attributes in path.
     */
    private static final Pattern reAttr = Pattern.compile("\\[@_q=\"\\d*\"\\]");

    /**
     * Obtain the sortKey string, construct it if not yet.
     * 
     * @return sort key string.
     */
    public String getSortKey() {
        if (sortKey == null) {
            Matcher m = reAttr.matcher(path);
            sortKey = m.replaceAll("");
        }
        return sortKey;
    }

    public String getValue() {
        return value;
    }

    // Zone and time zone element has '/' in attribute value, like
    // .../zone[@type="America/Adak"]/...
    // Such element can not be converted to "zone-type-America/Adak" as it is
    // not url safe. To deal with such issue, two segment are generated. It is
    // like the original path is written as:
    // .../zone/America/Adak/...

    /**
     * This function create a node list from a CLDR path.
     * 
     * Mostly, the node has one-to-one correspondence with path segment. But there
     * are special cases where one segment can be split to multiple nodes. If
     * necessary, several segments can also be combined to one node.
     * 
     * @return A list of node in strict parent-to-child order.
     * @throws ParseException
     */
    public ArrayList<CldrNode> getNodesInPath() throws ParseException {
        String[] pathSegments = splitPathToSegments(path);
        String[] fullPathSegments = splitPathToSegments(fullPath);
        assert (pathSegments.length == fullPathSegments.length);
        ArrayList<CldrNode> nodesInPath = new ArrayList<CldrNode>();

        String parent = "";
        for (int i = 0; i < pathSegments.length; i++) {
            CldrNode node = CldrNode.createNode(parent, pathSegments[i],
                fullPathSegments[i]);

            // Zone and time zone element has '/' in attribute value, like
            // .../zone[@type="America/Adak"]/...
            // Such element can not be converted to "zone-type-America/Adak" as it is
            // not url safe. To deal with such issue, two segment are generated. It is
            // like the original path is written as:
            // .../zone/America/Adak/...
            String nodeName = node.getName();
            if (node.isTimezoneType()) {
                nodesInPath.add(CldrNode.createNode(parent, node.getName(),
                    node.getName()));
                String typeValue = node.getDistinguishingAttributes().get("type");
                typeValue = typeValue.replaceAll("Asia:Taipei", "Asia/Taipei");
                String[] segments = typeValue.split("/");
                for (int j = 0; j < segments.length; j++) {
                    CldrNode newNode = CldrNode.createNode(parent, node.getName(),
                        node.getName());
                    if (j == segments.length - 1) {
                        newNode.getDistinguishingAttributes().putAll(
                            node.getDistinguishingAttributes());
                        newNode.getDistinguishingAttributes().remove("type");
                    }
                    newNode.getDistinguishingAttributes().put("type", segments[j]);
                    nodesInPath.add(newNode);
                }
            } else {
                nodesInPath.add(node);
            }
            parent = nodeName;
        }
        return nodesInPath;
    }

    /**
     * Some CLDR items have attributes that should be split before
     * transformation. For examples, item like:
     * <calendarPreference territories="CN CX" ordering="gregorian chinese"/>
     * should really be treated as 2 separate items:
     * <calendarPreference territories="CN" ordering="gregorian chinese"/>
     * <calendarPreference territories="CX" ordering="gregorian chinese"/>
     * 
     * @return Array of CldrItem if it can be split, otherwise null.
     */
    public CldrItem[] split() {
        for (int i = 0; i < LdmlConvertRules.SPLITTABLE_ATTRS.length; i++) {
            int pos = path.indexOf(LdmlConvertRules.SPLITTABLE_ATTRS[i].element);
            if (pos < 0) {
                continue;
            }

            Pattern pattern = LdmlConvertRules.SPLITTABLE_ATTRS[i].pattern;
            Matcher m = pattern.matcher(path);
            Matcher fullPathMatch = pattern.matcher(fullPath);
            if (m.matches()) {
                if (!fullPathMatch.matches()) {
                    System.out.println("FullPath does not match while path matches.");
                    continue;
                }
                ArrayList<CldrItem> list = new ArrayList<CldrItem>();
                String[] words = m.group(2).split(" ");
                for (String word : words) {
                    String newPath = m.group(1) + word + m.group(3);
                    String newFullPath = fullPathMatch.group(1) + word
                        + fullPathMatch.group(3);
                    list.add(new CldrItem(newPath, newFullPath, value));
                }
                return list.toArray(new CldrItem[list.size()]);
            }
        }
        return null;
    }

    /**
     * Check if the element path contains any item that need to be sorted first.
     * 
     * @return True if the element need to be sorted before further process.
     */
    public boolean needsSort() {
        for (String item : LdmlConvertRules.ELEMENT_NEED_SORT) {
            if (path.indexOf("/" + item + "[@") > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isAliasItem() {
        return path.endsWith("/alias");
    }

    @Override
    public int compareTo(CldrItem otherItem) {
        return getSortKey().compareTo(otherItem.getSortKey());
    }
}
