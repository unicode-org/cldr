package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.w3c.dom.Node;

public class WindowsZonesConverter extends BaseTimeZoneDataConverter {

    public WindowsZonesConverter(ICULog log, String fileName,
            String supplementalDir) {
        super(log, fileName, supplementalDir, LDMLConstants.WINDOWS_ZONES);
    }

    @Override
    protected Resource parseInfo(Node root, StringBuilder xpath) {
        Resource res = null;
        for (Node node = root.getFirstChild(); node != null; node = node
                .getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String name = node.getNodeName();
            if (name.equals(LDMLConstants.MAP_TIMEZONES)) {
                res = parseMapTimezones(node);
                break;
            }
        }
        return res;
    }
}
