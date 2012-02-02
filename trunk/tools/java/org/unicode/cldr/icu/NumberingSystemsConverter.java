// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceInt;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Node;


public class NumberingSystemsConverter extends SimpleLDMLConverter {
  public NumberingSystemsConverter(ICULog log, String fileName, String supplementalDir) {
    super(log, fileName, supplementalDir, LDMLConstants.NUMBERING_SYSTEMS);
  }

  @Override
  protected Resource parseInfo(Node root, StringBuilder xpath) {
    Resource current = null;
    ResourceTable ns = new ResourceTable();
    ns.name = LDMLConstants.NUMBERING_SYSTEMS;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (name.equals(LDMLConstants.NUMBERING_SYSTEM)) {
        ResourceTable nsTable = new ResourceTable();
        nsTable.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.ID);
        String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);

        ResourceInt radix = new ResourceInt();
        ResourceInt algorithmic = new ResourceInt();
        ResourceString desc = new ResourceString();

        radix.name = LDMLConstants.RADIX;
        desc.name = LDMLConstants.DESC;
        algorithmic.name = LDMLConstants.ALGORITHMIC;

        String radixString = LDMLUtilities.getAttributeValue(node, LDMLConstants.RADIX);
        if (radixString != null) {
          radix.val = radixString;
        } else {
          radix.val = "10";
        }

        if (type.equals(LDMLConstants.ALGORITHMIC)) {
          String numSysRules = LDMLUtilities.getAttributeValue(node, LDMLConstants.RULES);
          int marker = numSysRules.lastIndexOf("/");
          if (marker > 0) {
            String prefix = numSysRules.substring(0,marker + 1);
            String suffix = numSysRules.substring(marker + 1);
            desc.val = prefix + "%" + suffix;
          } else {
            desc.val = "%" + numSysRules;
          }
          algorithmic.val = "1";
        } else {
          desc.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.DIGITS);
          algorithmic.val = "0";
        }

        nsTable.first = radix;
        radix.next = desc;
        desc.next = algorithmic;

        if (current == null) {
          ns.first = nsTable;
          current = nsTable.end();
        } else{
          current.next = nsTable;
          current = nsTable.end();
        }
      }
    }

    return ns.first == null ? null : ns;
  }
}