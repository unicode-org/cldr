package org.unicode.cldr.icu;

import java.util.TreeMap;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Node;

public abstract class BaseTimeZoneDataConverter extends SimpleLDMLConverter {

    public BaseTimeZoneDataConverter(ICULog log, String fileName, String supplementalDir, String tableName) {
        super(log, fileName, supplementalDir, tableName);
    }

    public Resource parseMapTimezones(Node root) {

        TreeMap<String, TreeMap<String, String>> mapZones = new TreeMap<String, TreeMap<String, String>>();

        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String name = node.getNodeName();

            if (name.equals(LDMLConstants.MAP_ZONE)) {
                String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
                String other = LDMLUtilities.getAttributeValue(node, LDMLConstants.OTHER);
                String territory = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORY);
                if (territory == null) {
                    territory = "001";
                }

                TreeMap<String, String> territoryMap = mapZones.get(other);
                if (territoryMap == null) {
                    territoryMap = new TreeMap<String, String>();
                    mapZones.put(other, territoryMap);
                }
                territoryMap.put(territory, type);
            } else {
                log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
                System.exit(-1);
            }
        }

        if (mapZones.isEmpty()) {
            return null;
        }

        ResourceTable table = new ResourceTable();
        table.name = LDMLConstants.MAP_TIMEZONES;
        Resource currentSubTable = null;
        Resource subTable = null;

        for (String other : mapZones.keySet()) {
            subTable = new ResourceTable();
            subTable.name = other;

            Resource current = null;
            Resource res;

            TreeMap<String, String> territoryMap = mapZones.get(other);

            for (String territory : territoryMap.keySet()) {
                String zone = territoryMap.get(territory);
                res = new ResourceString(territory, zone);

                if (current == null) {
                    subTable.first = res;
                } else {
                    current.next = res;
                }
                current = res;
            }

            if (currentSubTable == null) {
                table.first = subTable;
            } else {
                currentSubTable.next = subTable;
            }
            currentSubTable = subTable;
        }

        return table;
    }
}
