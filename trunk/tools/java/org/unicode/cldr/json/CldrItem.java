package org.unicode.cldr.json;

import java.text.ParseException;
import java.util.ArrayList;

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

    /**
     * The value of this CLDR item.
     */
    private String value;

    CldrItem(String path, String fullPath, String untransformedPath, String untransformedFullPath, String value) {

        if (DEBUG) {
            System.out.println("---");
            System.out.println("    PATH => " + path);
            System.out.println("FULLPATH => " + fullPath);
            System.out.println("   VALUE => " + value);
            System.out.println("---");
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

    public void setPath(String path) {
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
     * @return Array of CldrItem if it can be split, otherwise null.
     */
    public CldrItem[] split() {
        XPathParts xpp = new XPathParts();
        XPathParts fullxpp = new XPathParts();
        XPathParts newxpp = new XPathParts();
        XPathParts newfullxpp = new XPathParts();
        XPathParts untransformedxpp = new XPathParts();
        XPathParts untransformedfullxpp = new XPathParts();
        XPathParts untransformednewxpp = new XPathParts();
        XPathParts untransformednewfullxpp = new XPathParts();
        xpp.set(path);
        fullxpp.set(fullPath);
        untransformedxpp.set(untransformedPath);
        untransformedfullxpp.set(untransformedFullPath);
        for (SplittableAttributeSpec s : LdmlConvertRules.SPLITTABLE_ATTRS) {
            if (fullxpp.containsElement(s.element) && fullxpp.containsAttribute(s.attribute)) {
                ArrayList<CldrItem> list = new ArrayList<CldrItem>();
                String wordString = fullxpp.findAttributeValue(s.element, s.attribute);
                String[] words = null;
                words = wordString.trim().split("\\s+");
                XPathParts[] newparts = { newxpp, newfullxpp, untransformednewxpp, untransformednewfullxpp };
                XPathParts[] trparts = { newxpp, newfullxpp };
                for (String word : words) {
                    newxpp.set(xpp);
                    newfullxpp.set(fullxpp);
                    untransformednewxpp.set(untransformedxpp);
                    untransformednewfullxpp.set(untransformedfullxpp);
                    for (XPathParts np : newparts) {
                        np.setAttribute(s.element, s.attribute, word);
                    }
                    if (s.attrAsValueAfterSplit != null) {
                        String newValue = fullxpp.findAttributeValue(s.element, s.attrAsValueAfterSplit);
                        for (XPathParts np : trparts) {
                            np.removeAttribute(s.element, s.attrAsValueAfterSplit);
                            np.removeAttribute(s.element, s.attribute);
                            np.addElement(word);
                        }
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
        return null;
    }

    /**
     * Check if the element path contains any item that need to be sorted first.
     *
     * @return True if the element need to be sorted before further process.
     */
    public boolean needsSort() {
        for (String item : LdmlConvertRules.ELEMENT_NEED_SORT) {
            XPathParts xpp = new XPathParts();
            xpp.set(path);
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
        XPathParts thisxpp = new XPathParts();
        XPathParts otherxpp = new XPathParts();
        thisxpp.set(untransformedPath);
        otherxpp.set(otherItem.untransformedFullPath);
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
        //return CLDRFile.getLdmlComparator().compare(path, otherItem.path);
        //return path.compareTo(otherItem.path);
    }
}
