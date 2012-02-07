package org.unicode.cldr.icu;

import java.text.SimpleDateFormat;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceArray;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Node;

public class MetaZonesConverter extends BaseTimeZoneDataConverter {

    private SimpleDateFormat gmtfmt;
    private static final int MAX_RES_INT = 134217727;
    private static final int MIN_RES_INT = -134217728;

    public MetaZonesConverter(ICULog log, String fileName,
            String supplementalDir) {
        super(log, fileName, supplementalDir, LDMLConstants.META_ZONES);
    }

    @Override
    protected Resource parseInfo(Node root, StringBuilder xpath) {
        Resource first = null;
        Resource current = null;
        for (Node node = root.getFirstChild(); node != null; node = node
                .getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Resource res = null;
            String name = node.getNodeName();
            if (name.equals(LDMLConstants.METAZONE_INFO)) {
                res = parseMetazoneInfo(node);
            } else if (name.equals(LDMLConstants.MAP_TIMEZONES)) {
                res = parseMapTimezones(node);
            }

            if (current == null) {
                first = current = res;
            } else {
                current.next = res;
            }
        }
        return first;
    }

    private Resource parseMetazoneInfo(Node root) {
        ResourceTable table = new ResourceTable();
        table.name = LDMLConstants.METAZONE_INFO;
        Resource current = null;

        for (Node node = root.getFirstChild(); node != null; node = node
                .getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String name = node.getNodeName();

            if (name.equals(LDMLConstants.TIMEZONE)) {
                String zone = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
                ResourceArray zoneData = new ResourceArray();
                zoneData.name = "\"" + zone.replaceAll("/", ":") + "\"";

                ResourceArray lastMzmap = null;

                for (Node node2 = node.getFirstChild(); node2 != null; node2 = node2.getNextSibling()) {
                    if (node2.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    String name2 = node2.getNodeName();
                    if (name2.equals(LDMLConstants.USES_METAZONE)) {
                        String mzone = LDMLUtilities.getAttributeValue(node2, LDMLConstants.MZONE);
                        String from = LDMLUtilities.getAttributeValue(node2, LDMLConstants.FROM);
                        String to = LDMLUtilities.getAttributeValue(node2, LDMLConstants.TO);

                        ResourceArray mzmap = new ResourceArray();
                        ResourceString mzoneRes = new ResourceString();
                        mzoneRes.val = mzone;
                        mzmap.first = mzoneRes;

                        if (from != null || to != null) {
                            ResourceString fromRes = new ResourceString();
                            fromRes.val = (from == null) ? "1970-01-01 00:00" : from;
                            mzoneRes.next = fromRes;

                            ResourceString toRes = new ResourceString();
                            toRes.val = (to == null) ? "9999-12-31 23:59" : to;
                            fromRes.next = toRes;
                        }

                        if (lastMzmap == null) {
                            zoneData.first = mzmap;
                        } else {
                            lastMzmap.next = mzmap;
                        }
                        lastMzmap = mzmap;
                    }
                }

                if (current == null) {
                    table.first = zoneData;
                } else {
                    current.next = zoneData;
                }
                current = zoneData;
            }
        }
        return table;
    }
}
