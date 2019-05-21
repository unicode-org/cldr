package org.unicode.cldr.json;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.impl.Utility;

/**
 * CldrNode represent a Element in XML as it appears in a CldrItem's path.
 */
public class CldrNode {

    public static CldrNode createNode(String parent, String pathSegment,
        String fullPathSegment) throws ParseException {
        CldrNode node = new CldrNode();

        node.parent = parent;
        node.name = extractAttrs(pathSegment, node.distinguishingAttributes);
        String fullTrunk = extractAttrs(fullPathSegment,
            node.nondistinguishingAttributes);
        if (!node.name.equals(fullTrunk)) {
            throw new ParseException("Error in parsing \"" + pathSegment + " \":\"" +
                fullPathSegment, 0);
        }

        for (String key : node.distinguishingAttributes.keySet()) {
            node.nondistinguishingAttributes.remove(key);
        }

        String[] suppressList = LdmlConvertRules.ATTR_SUPPRESS_LIST;

        // let's check if there is anything that can be suppressed
        for (int i = 0; i < suppressList.length; i += 3) {
            if (node.name.equals(suppressList[i])) {
                String key = suppressList[i + 2];
                String value = node.distinguishingAttributes.get(key);
                if (value != null && value.equals(suppressList[i + 1])) {
                    node.distinguishingAttributes.remove(key);

                }

            }
        }
        return node;
    }

    /**
     * Extract all the attributes and their value in the path.
     *
     * @param pathSegment
     *            A complete or partial path.
     * @param attributes
     *            String map to receive attribute mapping.
     *
     * @return Part of the string before the first attribute.
     * @throws ParseException
     */
    private static String extractAttrs(String pathSegment,
        Map<String, String> attributes) throws ParseException {
        int start = 0;

        String trunk = new String();
        while (true) {
            int ind1 = pathSegment.indexOf("[@", start);
            if (ind1 < 0) {
                if (trunk.isEmpty()) {
                    trunk = pathSegment;
                }
                break;
            }
            if (trunk.isEmpty()) {
                trunk = pathSegment.substring(0, ind1);
            }
            ind1 += 2;
            int ind2 = pathSegment.indexOf("=", ind1);
            if (ind2 < 0) {
                throw new ParseException("Missing '=' in attribute specification.",
                    ind1);
            }
            String attr = pathSegment.substring(ind1, ind2);

            ind1 = ind2 + 1;
            if (pathSegment.charAt(ind1) == '"') {
                ind1 += 1;
                ind2 = pathSegment.indexOf("\"]", ind1);
            } else {
                ind2 = pathSegment.indexOf("]", ind1);
            }

            if (ind2 < 0) {
                throw new ParseException(
                    "Unexpected end in attribute specification.", ind1);
            }

            String value = pathSegment.substring(ind1, ind2);

            start = ind2;

            attributes.put(attr, value);
        }

        return trunk;
    }

    /**
     * distinguishing attributes as identified by CLDR tools.
     */
    private Map<String, String> distinguishingAttributes;

    /**
     * non-distinguishing attributes as identified by CLDR tools.
     */
    private Map<String, String> nondistinguishingAttributes;

    /**
     * name of the element.
     */
    private String name;

    /**
     * parent element for this element.
     */
    private String parent;

    /**
     * This name is derived from element name and attributes. Once it is
     * calculated, it is cached in this variable.
     */
    private String uniqueNodeName;

    private CldrNode() {
        distinguishingAttributes = new HashMap<String, String>();
        nondistinguishingAttributes = new HashMap<String, String>();
    }

    /**
     * Get the string map for attributes that should be treated as values.
     *
     * @return String map.
     */
    public Map<String, String> getAttrAsValueMap() {
        Map<String, String> attributesAsValues = new HashMap<String, String>();
        for (String key : distinguishingAttributes.keySet()) {
            String keyStr = parent + ":" + name + ":" + key;
            if (LdmlConvertRules.ATTR_AS_VALUE_SET.contains(keyStr)) {
                if (LdmlConvertRules.COMPACTABLE_ATTR_AS_VALUE_SET.contains(keyStr)) {
                    attributesAsValues.put(LdmlConvertRules.ANONYMOUS_KEY,
                        distinguishingAttributes.get(key));
                } else {
                    attributesAsValues.put(key, distinguishingAttributes.get(key));
                }
            }
        }

        for (String key : nondistinguishingAttributes.keySet()) {
            if (LdmlConvertRules.IGNORABLE_NONDISTINGUISHING_ATTR_SET.contains(key)) {
                continue;
            }
            String keyStr = parent + ":" + name + ":" + key;
            if (LdmlConvertRules.COMPACTABLE_ATTR_AS_VALUE_SET.contains(keyStr)) {
                attributesAsValues.put(LdmlConvertRules.ANONYMOUS_KEY,
                    nondistinguishingAttributes.get(key));
            } else {
                attributesAsValues.put(key, nondistinguishingAttributes.get(key));
            }
        }
        return attributesAsValues;
    }

    public void setDistinguishingAttributes(Map<String, String> distinguishingAttributes) {
        this.distinguishingAttributes = distinguishingAttributes;
    }

    public void setNondistinguishingAttributes(Map<String, String> nondistinguishingAttributes) {
        this.nondistinguishingAttributes = nondistinguishingAttributes;
    }

    public Map<String, String> getDistinguishingAttributes() {
        return distinguishingAttributes;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getNondistinguishingAttributes() {
        return nondistinguishingAttributes;
    }

    /**
     * Construct a name that can be used as key in its container (by
     * incorporating distinguishing attributes).
     *
     * Each segment in CLDR path corresponding to a XML element. Element name
     * itself can not be used as JSON key because it might not be unique in its
     * container. A set of rules is used here to construct this key name. Some
     * of the attributes will be used in constructing the key name, the remaining
     * attributes are returned and should be used to fill the mapping.
     *
     * The basic mapping is from
     * <element_name>[@<attr_name>=<attr_value>]+
     * to
     * <element_name>-<attr_name>-<attr_value>
     *
     * @return A unique name that can be used as key in its container.
     */
    public String getNodeKeyName() {
        if (uniqueNodeName != null) {
            return uniqueNodeName;
        }

        // decide the main name
        StringBuffer strbuf = new StringBuffer();
        for (String key : distinguishingAttributes.keySet()) {
            String attrIdStr = parent + ":" + name + ":" + key;
            if (LdmlConvertRules.IsSuppresedAttr(attrIdStr)) {
                continue;
            }
            if (LdmlConvertRules.ATTR_AS_VALUE_SET.contains(attrIdStr)) {
                continue;
            }

            if (!key.equals("alt") && !key.equals("count") &&
                !LdmlConvertRules.NAME_PART_DISTINGUISHING_ATTR_SET.contains(attrIdStr)) {
                if (strbuf.length() != 0) {
                    throw new IllegalArgumentException(
                        "Can not have more than 1 key values in name: " +
                            "both '" + strbuf + "' and '" + distinguishingAttributes.get(key) +
                            "'. attrIdStr=" + attrIdStr + " - check LdmlConvertRules.java");
                }
                strbuf.append(distinguishingAttributes.get(key));
            }
        }
        if (strbuf.length() == 0) {
            strbuf.append(name);
        }

        // append distinguishing attributes
        for (String key : distinguishingAttributes.keySet()) {
            String attrIdStr = parent + ":" + name + ":" + key;
            if (LdmlConvertRules.IsSuppresedAttr(attrIdStr)) {
                continue;
            }
            if (LdmlConvertRules.ATTR_AS_VALUE_SET.contains(attrIdStr)) {
                continue;
            }

            if (!key.equals("alt") &&
                !LdmlConvertRules.NAME_PART_DISTINGUISHING_ATTR_SET.contains(attrIdStr)) {
                continue;
            }
            strbuf.append("-");
            strbuf.append(key);
            strbuf.append("-");
            strbuf.append(distinguishingAttributes.get(key));
        }
        uniqueNodeName = strbuf.toString();

        if (uniqueNodeName.length() == 1 && name.equals("character")) {
            // character attribute has value that can be any unicode character. Those
            // might not be url safe and can be difficult for user to specify. It is
            // converted to hex string here.
            uniqueNodeName = "U+" + Utility.hex(uniqueNodeName.charAt(0), 4);
        } else if (isTimezoneType()) {
            // time zone name has GMT+9 type of thing. "+" need to be removed to make
            // it URL safe.
            uniqueNodeName = uniqueNodeName.replaceFirst("\\+", "");
        }

        return uniqueNodeName;
    }

    /**
     * Construct a name that has all distinguishing attributes that should not be
     * ignored.
     *
     * Different from getNodeKeyName, this name has include those distinguishing
     * attributes that will be treated as values.
     *
     * @return A distinguishing name for differentiating element.
     */
    public String getNodeDistinguishingName() {
        // decide the main name
        StringBuffer strbuf = new StringBuffer();
        strbuf.append(name);

        // append distinguishing attributes
        for (String key : distinguishingAttributes.keySet()) {
            strbuf.append("-");
            strbuf.append(key);
            strbuf.append("-");
            strbuf.append(distinguishingAttributes.get(key));
        }
        return strbuf.toString();
    }

    public boolean isTimezoneType() {
        return LdmlConvertRules.TIMEZONE_ELEMENT_NAME_SET.contains(name);
    }
}
