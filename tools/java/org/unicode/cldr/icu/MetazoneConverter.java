// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceArray;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Node;


public class MetazoneConverter extends SimpleLDMLConverter {
  public MetazoneConverter(ICULog log, String fileName, String supplementalDir) {
    super(log, fileName, supplementalDir, LDMLConstants.METAZONE_INFO);
  }
  
  @Override
  protected Resource parseInfo(Node root, StringBuilder xpath) {
    Resource current = null;
    ResourceTable mzInfo = new ResourceTable();
    mzInfo.name = LDMLConstants.METAZONE_MAPPINGS;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (name.equals(LDMLConstants.TIMEZONE)) {
        Resource current_mz = null;
        ResourceTable mzTable = new ResourceTable();
        mzTable.name = "\"" + LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE).
            replaceAll("/", ":") + "\"";
        int mz_count = 0;

        for (Node node2 = node.getFirstChild(); node2 != null; node2 = node2.getNextSibling()) {
          if (node2.getNodeType() != Node.ELEMENT_NODE) {
            continue;
          }

          String name2 = node2.getNodeName();
          if (name2.equals(LDMLConstants.USES_METAZONE)) {
            ResourceArray this_mz = new ResourceArray();
            ResourceString mzone = new ResourceString();
            ResourceString from = new ResourceString();
            ResourceString to = new ResourceString();

            this_mz.name = "mz" + String.valueOf(mz_count);
            this_mz.first = mzone;
            mzone.next = from;
            from.next = to;
            mz_count++;

            mzone.val = LDMLUtilities.getAttributeValue(node2, LDMLConstants.MZONE);
            String str = LDMLUtilities.getAttributeValue(node2, LDMLConstants.FROM);
            if (str != null) {
              from.val = str;
            } else {
              from.val = "1970-01-01 00:00";
            }

            str = LDMLUtilities.getAttributeValue(node2, LDMLConstants.TO);
            if (str != null) {
              to.val = str;
            } else {
              to.val = "9999-12-31 23:59";
            }

            if (current_mz == null) {
              mzTable.first = this_mz;
              current_mz = this_mz.end();
            } else {
              current_mz.next = this_mz;
              current_mz = this_mz.end();
            }
          }
        }

        if (current == null) {
          mzInfo.first = mzTable;
          current = mzTable.end();
        } else {
          current.next = mzTable;
          current = mzTable.end();
        }
      }
    }

    return mzInfo.first == null ? null : mzInfo;
  }
}