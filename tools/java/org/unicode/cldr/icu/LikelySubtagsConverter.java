// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Node;


public class LikelySubtagsConverter extends SimpleLDMLConverter {
  public LikelySubtagsConverter(ICULog log, String fileName, String supplementalDir) {
    super(log, fileName, supplementalDir, LDMLConstants.LIKELY_SUBTAGS);
  }

  @Override
  protected Resource parseInfo(Node root, StringBuilder xpath) {
    Resource first = null;
    Resource current = null;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (name.equals(LDMLConstants.LIKELY_SUBTAG)) {
        ResourceString subtagString = new ResourceString();
        subtagString.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.FROM);
        subtagString.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.TO);

        if (current == null) {
          first = current = subtagString;
        } else {
          current.next = subtagString;
        }
        current = subtagString;
      }
    }

    return first;
  }
}