/*
 **********************************************************************
 * Copyright (C) 2010 International Business Machines Corporation and *
 * others. All Rights Reserved.                                       *
 **********************************************************************
 */

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Node;

class TimeZoneDataParser {
    static Resource parseTimeZoneData(Node root, StringBuilder xpath, ICULog log) {
        Resource current = null;
        Resource first = null;
        int savedLength = xpath.length();
        LDML2ICUConverter.getXPath(root, xpath);
        int oldLength = xpath.length();

        ResourceTable mapZones = new ResourceTable();
        mapZones.name = LDMLConstants.MAP_TIMEZONES;
        for(Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String name = node.getNodeName();
            Resource res = null;

            if (name.equals(LDMLConstants.MAP_TIMEZONES)) {

                //if (DEBUG)printXPathWarning(node, xpath);
                res = parseMapTimezones(node, xpath, log);
                if (res != null) {
                    if (mapZones.first == null) {
                        mapZones.first = res;
                    } else {
                        mapZones.first.end().next = res;
                    }
                }
                res = null;
            } else {
                log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
                System.exit(-1);
            }

            if (res != null) {
                if (current == null) {
                    first = res;
                    current = res.end();
                } else {
                    current.next = res;
                    current = res.end();
                }
                res = null;
            }
            xpath.delete(oldLength, xpath.length());
        }

        if (mapZones.first != null) {
            if (current == null) {
                first = current = mapZones;
            }else{
                current.next = mapZones;
                current = mapZones.end();
            }
        }

        xpath.delete(savedLength, xpath.length());
        if (first != null) {
            return first;
        }

        return null;
    }
    private static Resource parseMapTimezones(Node root, StringBuilder xpath, ICULog log) {
        int savedLength = xpath.length();
        LDML2ICUConverter.getXPath(root, xpath);
        int oldLength = xpath.length();
        Resource current = null;
        Resource res = null;

        ResourceTable table = new ResourceTable();
        table.name = LDMLUtilities.getAttributeValue(root, LDMLConstants.TYPE);

        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType()!= Node.ELEMENT_NODE) {
                continue;
            }
            String name = node.getNodeName();

            if (name.equals(LDMLConstants.MAP_ZONE)) {
                String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
                String other = LDMLUtilities.getAttributeValue(node, LDMLConstants.OTHER);
                String territory = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORY);
                String result;
                ResourceString str = new ResourceString();
                if (territory != null && territory.length() > 0) {
                    result = "meta:" + other + "_" + territory;
                    str.name = "\"" + result + "\"";
                    str.val = type;
                } else {
                    result = type;
                    str.name = "\"" + other + "\"";
                    str.val = result;
                }
                res = str;
            } else {
                log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
                System.exit(-1);
            }

            if (res != null) {
                if (current == null) {
                    table.first = res;
                    current = res.end();
                } else {
                    current.next = res;
                    current = res.end();
                }
                res = null;
            }
            xpath.setLength(oldLength);
        }

        xpath.setLength(savedLength);
        if (table.first != null) {
            return table;
        }

        return null;
    }
};