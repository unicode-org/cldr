// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Node;


public class PluralsConverter extends SimpleLDMLConverter {
  public PluralsConverter(ICULog log, String fileName, String supplementalDir) {
    super(log, fileName, supplementalDir, LDMLConstants.PLURALS);
  }
  
  @Override
  protected Resource parseInfo(Node root, StringBuilder xpath) {
    int currentSetNumber = 1;
    ResourceTable localesTable = new ResourceTable();
    localesTable.name = LDMLConstants.LOCALES;

    ResourceTable ruleSetsTable = new ResourceTable();
    ruleSetsTable.name = LDMLConstants.RULES;

    // The ruleSetsTable is a sibling of the locales table.
    localesTable.next = ruleSetsTable;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (!name.equals(LDMLConstants.PLURAL_RULES)) {
        log.error("Encountered element " + name + " processing plurals.");
        System.exit(-1);
      }

      ResourceTable currentSetTable = null;
      ResourceString currentRuleString = null;
      Node child = node.getFirstChild();

      String locales = LDMLUtilities.getAttributeValue(node, LDMLConstants.LOCALES);
      String [] localesArray = locales.split("\\s");

      if (child == null) {
        // Create empty resource strings with the locale as the ID.
        for (int i = 0; i < localesArray.length; ++i) {
          ResourceString localeString = new ResourceString(localesArray[i], "");
          localesTable.appendContents(localeString);
        }
      } else {
        do {
          if (child.getNodeType() == Node.ELEMENT_NODE) {
            String childName = child.getNodeName();
            if (!childName.equals(LDMLConstants.PLURAL_RULE)) {
              log.error("Encountered element " + childName + " processing plurals.");
              System.exit(-1);
            }

            // This creates a rule string for the current rule set
            ResourceString ruleString = new ResourceString();
            ruleString.name = LDMLUtilities.getAttributeValue(child, LDMLConstants.COUNT);
            ruleString.val = LDMLUtilities.getNodeValue(child);

            // Defer the creation of the table until the first
            // rule for the locale, since there are some locales
            // with no rules, and we don't want those in the
            // ICU resource file.
            if (currentSetTable != null) {
              currentRuleString.next = ruleString;
            } else {
              currentSetTable = new ResourceTable();
              String currentSetName = new String("set") + currentSetNumber;
              ++currentSetNumber;
              currentSetTable.name = currentSetName;
              currentSetTable.first = ruleString;
              ruleSetsTable.appendContents(currentSetTable);

              // Now that we've created a rule set table, we can put all of the
              // locales for this rule set into the locales table.
              for (int i = 0; i < localesArray.length; ++i) {
                ResourceString localeString =
                  new ResourceString(localesArray[i], currentSetName);
                localesTable.appendContents(localeString);
              }
            }
            currentRuleString = ruleString;
          }
          child = child.getNextSibling();
        } while (child != null);
      }
    }

    return localesTable.first == null ? null : localesTable;
  }
}