// Copyright 2011 International Business Machines, Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceArray;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Node;


public class SupplementalMetadataConverter extends SimpleLDMLConverter {
  public SupplementalMetadataConverter(ICULog log, String fileName, String supplementalDir) {
    super(log, fileName, supplementalDir, LDMLConstants.META_DATA);
  }

  @Override
  protected Resource parseInfo(Node root, StringBuilder xpath) {
    Resource current = null;
    Resource first = null;
    Resource res = null;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {

        if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
        }

        String name = node.getNodeName();
        if (name.equals(LDMLConstants.VALIDITY)) {
            res = parseValidity(node,xpath);
        } else if (name.equals(LDMLConstants.ALIAS)) {
            res = parseAlias(node,xpath);
        }
        if (res != null) {
            if (current == null) {
                current = first = res;
            } else {
                current.next = res;
                current = current.next;
            }
            res = null;
        }
     
    }
    return first;
  }
  
  private Resource parseValidity(Node root, StringBuilder xpath) {
      Resource res = null;

      for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
        if (node.getNodeType()!= Node.ELEMENT_NODE) {
          continue;
        }
        String name = node.getNodeName();

        if ( name.equals(LDMLConstants.VARIABLE) &&
             LDMLUtilities.getAttributeValue(node, LDMLConstants.ID).equals(LDMLConstants.TERRITORY_VARIABLE)) {
      
            ResourceArray arr = new ResourceArray();
            Resource c = null;
            arr.name = LDMLConstants.REGION_CODES;
            String validRegionString = LDMLUtilities.getNodeValue(node).trim();
            String [] validRegions = validRegionString.split("\\s+");
            for (int i = 0; i < validRegions.length; i++) {
                ResourceString str = new ResourceString();
                str.val = validRegions[i];
                if (c == null) {
                    arr.first = c = str;
                } else {
                    c.next = str;
                    c = c.next;
                }
            }
            if (arr.first != null) {
                res = arr;
                break;
            }
        }
     }
     return res;
  }
  private Resource parseAlias(Node root, StringBuilder xpath) {
      Resource res = null;
      Resource current = null;
      
      Resource table = new ResourceTable();
      table.name = LDMLConstants.TERRITORY_ALIAS;
      for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
        if (node.getNodeType()!= Node.ELEMENT_NODE) {
          continue;
        }
        String name = node.getNodeName();

        if ( name.equals(LDMLConstants.TERRITORY_ALIAS)){
            String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
            String replacement = LDMLUtilities.getAttributeValue(node,LDMLConstants.REPLACEMENT);
            
            ResourceString str = new ResourceString();
            str.name = type;
            str.val = replacement;
            res = str;

            if (current == null) {
                current = table.first = res;
            } else {
                current.next = res;
                current = current.next;
            }
            res = null;
        }

     }
     return table.first == null ? null : table;
  }
}