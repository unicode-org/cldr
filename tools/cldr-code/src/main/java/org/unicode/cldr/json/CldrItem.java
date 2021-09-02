package org.unicode.cldr.json;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.json.LdmlConvertRules.SplittableAttributeSpec;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.ZoneParser;

/**
 * A object to present a CLDR XML item.
 */
public class CldrItem implements Comparable<CldrItem> {

    private static boolean DEBUG = false;

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
        ArrayList<String> segments = new ArrayList<>();
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
     * The full path of a CLDR item.
     *
     * Comparing to path, this full contains non-distinguishable attributes.
     */
    private String untransformedFullPath;

    /**
     * The resolution path of a CLDR item.
     *
     * This path only contains distinguishable attributes that are necessary to
     * identify a CLDR XML item in the CLDR tree.
     */
    private String untransformedPath;

    protected String getUntransformedPath() {
        return untransformedPath;
    }

    @Override
    public String toString() {
        return "[CldrItem " + getUntransformedPath()+"]";
    }

    /**
     * The value of this CLDR item.
     */
    private String value;

    CldrItem(final String path, String fullPath, String untransformedPath, String untransformedFullPath, String value) {

        if (DEBUG) {
            System.out.println("---");
            System.out.println("    PATH => " + path);
            System.out.println("FULLPATH => " + fullPath);
            System.out.println("   VALUE => " + value);
            System.out.println("---");
        }

        if(path.isEmpty()) {
            // Should not happen
            throw new IllegalArgumentException("empty path with " + fullPath+"|"+untransformedPath+"|"+untransformedFullPath+ " = " + value );
        }

        this.path = path;
        this.fullPath = fullPath;
        this.untransformedPath = untransformedPath;
        this.untransformedFullPath = untransformedFullPath;

        if (value == null) {
            this.value = "";
        } else {
            this.value = value;
        }
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getPath() {
        return path;
    }

    /**
     * Obtain the sortKey string, construct it if not yet.
     *
     * @return sort key string.
     */

    public String getValue() {
        return value;
    }

    // Zone and time zone element has '/' in attribute value, like
    // .../zone[@type="America/Adak"]/...
    // Such element can not be converted to "zone-type-America/Adak" as it is
    // not url safe. To deal with such issue, two segment are generated. It is
    // like the original path is written as:
    // .../zone/America/Adak/...

    public void setValue(String value) {
        this.value = value;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

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
        ArrayList<CldrNode> nodesInPath = new ArrayList<>();

        String parent = "";
        for (int i = 0; i < pathSegments.length; i++) {
            CldrNode node = CldrNode.createNode(parent, pathSegments[i],
                fullPathSegments[i], this);

            // Zone and time zone element has '/' in attribute value, like
            // .../zone[@type="America/Adak"]/...
            // Such element can not be converted to "zone-type-America/Adak" as it is
            // not url safe. To deal with such issue, two segment are generated. It is
            // like the original path is written as:
            // .../zone/America/Adak/...
            String nodeName = node.getName();
            if (node.isTimezoneType()) {
                nodesInPath.add(CldrNode.createNode(parent, node.getName(),
                    node.getName(), this));
                String typeValue = node.getDistinguishingAttributes().get("type");
                typeValue = typeValue.replaceAll("Asia:Taipei", "Asia/Taipei");
                String[] segments = typeValue.split("/");
                for (int j = 0; j < segments.length; j++) {
                    CldrNode newNode = CldrNode.createNode(parent, node.getName(),
                        node.getName(), this);
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

    public void setPath(String path) {
        if(path.isEmpty()) {
            throw new IllegalArgumentException("empty path");
        }
        this.path = path;
    }

    /**
     * Some CLDR items have attributes that should be split before
     * transformation. For examples, item like:
     * <calendarPreference territories="CN CX" ordering="gregorian chinese"/>
     * should really be treated as 2 separate items:
     * <calendarPreference territories="CN" ordering="gregorian chinese"/>
     * <calendarPreference territories="CX" ordering="gregorian chinese"/>
     *
     * @return Array of CldrItem if it can be split, otherwise null if nothing to split.
     */
    public CldrItem[] split() {
        XPathParts xpp = XPathParts.getFrozenInstance(path);
        XPathParts fullxpp = XPathParts.getFrozenInstance(fullPath);
        XPathParts untransformedxpp = XPathParts.getFrozenInstance(untransformedPath);
        XPathParts untransformedfullxpp = XPathParts.getFrozenInstance(untransformedFullPath);

        for (SplittableAttributeSpec s : LdmlConvertRules.getSplittableAttrs()) {
            if (fullxpp.containsElement(s.element) && fullxpp.containsAttribute(s.attribute)) {
                ArrayList<CldrItem> list = new ArrayList<>();
                String wordString = fullxpp.findAttributeValue(s.element, s.attribute);
                String[] words = wordString.trim().split("\\s+");
                Set<String> hadWords = new TreeSet<>();
                for (String word : words) {
                    if(hadWords.add(word) == false) {
                        System.err.println("Warning: Duplicate attribute " + word + " in " + fullPath);
                        continue;
                    }
                    // TODO: Ideally, there would be a separate post-split path transform.

                    XPathParts newxpp = xpp.cloneAsThawed();
                    XPathParts newfullxpp = fullxpp.cloneAsThawed();
                    XPathParts untransformednewxpp = untransformedxpp.cloneAsThawed();
                    XPathParts untransformednewfullxpp = untransformedfullxpp.cloneAsThawed();

                    newxpp.setAttribute(s.element, s.attribute, word);
                    newfullxpp.setAttribute(s.element, s.attribute, word);
                    untransformednewxpp.setAttribute(s.element, s.attribute, word);
                    untransformednewfullxpp.setAttribute(s.element, s.attribute, word);

                    if (s.attrAsValueAfterSplit != null) {
                        String newValue = fullxpp.findAttributeValue(s.element, s.attrAsValueAfterSplit);
                        newxpp.removeAttribute(s.element, s.attrAsValueAfterSplit);
                        newxpp.removeAttribute(s.element, s.attribute);
                        newxpp.addElement(word);
                        newfullxpp.removeAttribute(s.element, s.attrAsValueAfterSplit);
                        newfullxpp.removeAttribute(s.element, s.attribute);
                        newfullxpp.addElement(word);
                        list.add(new CldrItem(newxpp.toString(), newfullxpp.toString(), untransformednewxpp.toString(), untransformednewfullxpp.toString(),
                            newValue));
                    } else {
                        list.add(new CldrItem(newxpp.toString(), newfullxpp.toString(), untransformednewxpp.toString(), untransformednewfullxpp.toString(),
                            value));
                    }
                }
                return list.toArray(new CldrItem[list.size()]);
            }
        }
        return null; // nothing to split
    }

    /**
     * Check if the element path contains any item that need to be sorted first.
     *
     * @return True if the element need to be sorted before further process.
     */
    public boolean needsSort() {
        for (String item : LdmlConvertRules.ELEMENT_NEED_SORT) {
            XPathParts xpp = XPathParts.getFrozenInstance(path);
            if (xpp.containsElement(item)) {
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
        XPathParts thisxpp = XPathParts.getFrozenInstance(untransformedPath);
        XPathParts otherxpp = XPathParts.getFrozenInstance(otherItem.untransformedFullPath);
        if (thisxpp.containsElement("zone") && otherxpp.containsElement("zone")) {
            String[] thisZonePieces = thisxpp.findAttributeValue("zone", "type").split("/");
            String[] otherZonePieces = otherxpp.findAttributeValue("zone", "type").split("/");
            int result = ZoneParser.regionalCompare.compare(thisZonePieces[0], otherZonePieces[0]);
            if (result != 0) {
                return result;
            }
            result = thisZonePieces[1].compareTo(otherZonePieces[1]);
            if (result != 0) {
                return result;
            }
        }

        DtdType fileDtdType;
        if (thisxpp.getElement(0).equals("supplementalData")) {
            fileDtdType = DtdType.supplementalData;
        } else {
            fileDtdType = DtdType.ldml;
        }
        int result = 0;
        if (thisxpp.getElement(1).equals("weekData") && thisxpp.getElement(2).equals(otherxpp.getElement(2))) {
            String thisTerritory = thisxpp.findFirstAttributeValue("territories");
            String otherTerritory = otherxpp.findFirstAttributeValue("territories");
            if (thisTerritory != null && otherTerritory != null) {
                result = thisTerritory.compareTo(otherTerritory);
            }
            if (result != 0) {
                return result;
            }
        }
        if (thisxpp.getElement(1).equals("measurementData") && thisxpp.getElement(2).equals(otherxpp.getElement(2))) {
            String thisCategory = thisxpp.findAttributeValue("measurementSystem", "category");
            if (thisCategory == null) {
                thisCategory = "";
            }
            String otherCategory = otherxpp.findAttributeValue("measurementSystem", "category");
            if (otherCategory == null) {
                otherCategory = "";
            }
            if (!thisCategory.equals(otherCategory)) {
                result = thisCategory.compareTo(otherCategory);
                return result;
            }
            String thisTerritory = thisxpp.findFirstAttributeValue("territories");
            String otherTerritory = otherxpp.findFirstAttributeValue("territories");
            if (thisTerritory != null && otherTerritory != null) {
                result = thisTerritory.compareTo(otherTerritory);
            }
            if (result != 0) {
                return result;
            }
        }
        result = DtdData.getInstance(fileDtdType).getDtdComparator(null).compare(untransformedPath, otherItem.untransformedPath);
        return result;
    }

    void adjustRbnfPath() {
        XPathParts xpp = XPathParts.getFrozenInstance(getFullPath());
        final String sub = xpp.findAttributeValue("rbnfrule", "value");
        if(sub != null){
            xpp = xpp.cloneAsThawed();
            final String value = getValue();
            xpp.removeAttribute(-1, "value");
            xpp.addAttribute(sub, value);
            setFullPath(xpp.toString());
            setValue("");
        }
        // ADJUST ACCESS=PRIVATE/PUBLIC BASED ON ICU RULE
        String fullpath = getFullPath();
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
                    setPath(getPath().replace(type, newType));
                }
                fullpath = fullpath.replace(oldRulePath, newRulePath);
                setFullPath(fullpath);
            }
        }
    }
}
