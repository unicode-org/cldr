// Copyright 2011-2012 International Business Machines, Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceArray;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
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
  
  protected Resource parseAlias(Node root, StringBuilder xpath) {
      Resource current = null;
      Resource first = null;
      Resource res = null;
      boolean languageAliasParsed = false;
      boolean territoryAliasParsed = false;
      boolean scriptAliasParsed = false;
      boolean variantAliasParsed = false;
      
      for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {

          if (node.getNodeType() != Node.ELEMENT_NODE) {
              continue;
          }

          String name = node.getNodeName();
          if (name.equals(LDMLConstants.LANGUAGE_ALIAS) && !languageAliasParsed) {
              res = parseAliasResources(node,xpath,LDMLConstants.LANGUAGE_ALIAS);
              languageAliasParsed = true;
          } else if (name.equals(LDMLConstants.TERRITORY_ALIAS) && !territoryAliasParsed) {
              res = parseAliasResources(node,xpath,LDMLConstants.TERRITORY_ALIAS);
              territoryAliasParsed = true;
          } else if (name.equals(LDMLConstants.SCRIPT_ALIAS) && !scriptAliasParsed) {
              res = parseAliasResources(node,xpath,LDMLConstants.SCRIPT_ALIAS);
              scriptAliasParsed = true;
          } else if (name.equals(LDMLConstants.VARIANT_ALIAS) && !variantAliasParsed) {
              res = parseAliasResources(node,xpath,LDMLConstants.VARIANT_ALIAS);
              variantAliasParsed = true;
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

  private Resource parseAliasResources(Node root, StringBuilder xpath, String aliasType) {
      Resource res = null;
      Resource current = null;
      
      Resource table = new ResourceTable();
      table.name = aliasType;
      for (Node node = root; node != null; node = node.getNextSibling()) {
        if (node.getNodeType()!= Node.ELEMENT_NODE) {
          continue;
        }
        String name = node.getNodeName();

        if ( name.equals(aliasType)){
            String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
            String replacement = LDMLUtilities.getAttributeValue(node,LDMLConstants.REPLACEMENT);
       
            if ( type != null && replacement != null) {
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

     }
     return table.first == null ? null : table;
  }
}