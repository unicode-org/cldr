package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Node;


public abstract class BaseTimeZoneDataConverter extends SimpleLDMLConverter {

    public BaseTimeZoneDataConverter(ICULog log, String fileName,
            String supplementalDir, String tableName) {
        super(log, fileName, supplementalDir, tableName);
    }

    public Resource parseMapTimezones(Node root) {
        Resource current = null;
        Resource res = null;

        ResourceTable table = new ResourceTable();
        table.name = LDMLConstants.MAP_TIMEZONES;

        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType()!= Node.ELEMENT_NODE) {
                continue;
            }
            String name = node.getNodeName();

            if (name.equals(LDMLConstants.MAP_ZONE)) {
                String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
                String other = LDMLUtilities.getAttributeValue(node, LDMLConstants.OTHER);
                String territory = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORY);

                String resname = other;
                if (territory != null && territory.length() > 0 && !territory.equals("001")) {
                    resname = resname + ":" + territory;
                }
                resname = "\"" + resname + "\"";
                res = new ResourceString(resname, type);
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
        }

        if (table.first != null) {
            return table;
        }

        return null;
      }
}
