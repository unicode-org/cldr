package org.unicode.cldr.unittest;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.ElementAttributeInfo;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.DtdType;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;

public class TestAttributes extends TestFmwk {

  /**
   * Simple test that loads each file in the cldr directory, thus verifying that
   * the DTD works, and also checks that the PrettyPaths work.
   * 
   * @author markdavis
   */

  public static void main(String[] args) {
    new TestAttributes().run(args);
  }

  public void TestDistinguishing() {
    for (DtdType type : DtdType.values()) {
      showDistinguishing(type);
    }
  }

  private void showDistinguishing(DtdType dtdType) {
    Relation<String, String> elementToAttributes = ElementAttributeInfo.getElementToAttributes(dtdType);
    Relation<String,String> distinguishingAttributeToElements = new Relation(new TreeMap(), TreeSet.class);
    Relation<String,String> nondistinguishingAttributeToElements = new Relation(new TreeMap(), TreeSet.class);
    Relation<String,String> orderedAttributeToElements = new Relation(new TreeMap(), TreeSet.class);

    for (String element : elementToAttributes.keySet()) {
      boolean isOrdered = CLDRFile.isOrdered(element);
      Set<String> attributes = elementToAttributes.getAll(element);
      for (String attribute : attributes) {
        if (isOrdered) {
          orderedAttributeToElements.put(attribute, element);
        } if (CLDRFile.isDistinguishing(element, attribute)) {
          distinguishingAttributeToElements.put(attribute, element);
        } else {
          nondistinguishingAttributeToElements.put(attribute, element);
        }
      }
    }

    Set<String> nondistinguishing = new TreeSet(nondistinguishingAttributeToElements.keySet());
    nondistinguishing.removeAll(distinguishingAttributeToElements.keySet());
    System.out.println("// " + dtdType + "\tnondistinguishing: " + nondistinguishing);
    
    Set<String> distinguishing = new TreeSet(distinguishingAttributeToElements.keySet());
    nondistinguishing.removeAll(nondistinguishingAttributeToElements.keySet());
    System.out.println("// " + dtdType + "\tdistinguishing: " + distinguishing);
    
    Set<String> both = new TreeSet(distinguishingAttributeToElements.keySet());
    both.retainAll(nondistinguishingAttributeToElements.keySet());
    System.out.println("// " + dtdType + "\tboth: " + both);

    for (String attribute : distinguishing) {
      System.out.println("{\"" + "dist" + "\", \"" + dtdType + "\", \"" + "*" + "\", \"" + attribute + "\"},");
    }
    for (String attribute : both) {
        for (String element : nondistinguishingAttributeToElements.getAll(attribute)) {
          System.out.println("{\"" + "nondist" + "\", \"" + dtdType + "\", \"" + element + "\", \"" + attribute + "\"},");
        }
    }
  }
  public void TestAttributes() {
    Map<R2<String, String>, R3<Set<String>, String, String>> mainData = ElementAttributeInfo.getAttributeData(DtdType.ldml);
    Map<R2<String, String>, R3<Set<String>, String, String>> suppData = ElementAttributeInfo.getAttributeData(DtdType.supplementalData);
    Set<R2<String, String>> commonKeys = getCommon(mainData.keySet(), suppData.keySet(), new TreeSet<R2<String, String>>());
    Set<R2<String, String>> same = new TreeSet<R2<String, String>>();
    for (R2<String, String> key : commonKeys) {
      R3<Set<String>, String, String> mainValue = mainData.get(key);
      R3<Set<String>, String, String> suppValue = suppData.get(key);
      if (mainValue.equals(suppValue)) {
        same.add(key);
        continue;
      }
      errln(key + "\t" + DtdType.ldml + ":\t" + mainValue + "\t" + DtdType.supplementalData + ":\t" + suppValue);
    }
    for (R2<String, String> key : same) {
      logln("both" + ":\t" + key + "\t" + mainData.get(key));
    }
    for (R2<String, String> key : mainData.keySet()) {
      if (commonKeys.contains(key)) continue;
      logln(DtdType.ldml + ":\t" + key + "\t" + mainData.get(key));
    }
    for (R2<String, String> key : suppData.keySet()) {
      if (commonKeys.contains(key)) continue;
      logln(DtdType.supplementalData + ":\t" + key + "\t" + suppData.get(key));
    }
  }

  private <T extends Collection> T getCommon(T a, T b, T result) {
    result.addAll(a);
    result.retainAll(b);
    return result;
  }

  public void TestElements() {
    Relation<String, String> mainData = ElementAttributeInfo.getContainingElementData(DtdType.ldml);
    Relation<String, String> suppData = ElementAttributeInfo.getContainingElementData(DtdType.supplementalData);
    Set<String> commonKeys = getCommon(mainData.keySet(), suppData.keySet(), new TreeSet<String>());
    Set<String> same = new TreeSet<String>();
    for (String key : commonKeys) {
      Set<String> mainValues = mainData.getAll(key);
      Set<String> suppValues = suppData.getAll(key);
      if (mainValues.equals(suppValues)) {
        same.add(key);
        continue;
      }
      errln(key + "\t" + DtdType.ldml + ":\t" + mainValues + "\t" + DtdType.supplementalData + ":\t" + suppValues);
    }
    for (String key : same) {
      logln("both" + ":\t" + key + "\t" + mainData.getAll(key));
    }
    for (String key : mainData.keySet()) {
      if (commonKeys.contains(key)) continue;
      logln(DtdType.ldml + ":\t" + key + "\t" + mainData.getAll(key));
    }
    for (String key : suppData.keySet()) {
      if (commonKeys.contains(key)) continue;
      logln(DtdType.supplementalData + ":\t" + key + "\t" + suppData.getAll(key));
    }
  }

  public void TestEmpty() {
    checkEmpty(DtdType.ldml);
    checkEmpty(DtdType.supplementalData);
  }

  static final Set<String> COLLATION_SINGLETONS = new HashSet<String>(Arrays.asList(new String[] {
          "first_non_ignorable", "first_primary_ignorable", "first_secondary_ignorable", "first_tertiary_ignorable", "first_trailing", "first_variable",
          "last_non_ignorable", "last_primary_ignorable", "last_secondary_ignorable", "last_tertiary_ignorable", "last_trailing last_variable",
          "last_trailing", "last_variable"
  }));

  private void checkEmpty(DtdType type) {
    Relation<String, String> mainData = ElementAttributeInfo.getContainedElementData(type);
    Relation<String, String> elementToAttributes = ElementAttributeInfo.getElementToAttributes(type);
    Map<R2<String, String>, R3<Set<String>, String, String>> eaData = ElementAttributeInfo.getAttributeData(type);

    Set<String> empties = mainData.getAll("EMPTY");
    for (String empty : empties) {
      if (COLLATION_SINGLETONS.contains(empty)) continue;
      Set<String> attributes = elementToAttributes.getAll(empty);
      if (attributes == null) {
        errln(type + ", " + empty + ", No attributes!");
        continue;
      }
      for (String attribute : attributes) {
        if (attribute.equals("draft") || attribute.equals("references")) continue;
        logln(type + ", " + empty + ", " + attribute + ", " + eaData.get(Row.of(empty, attribute)));
      }
    }
  }

  public void TestLeaf() {
    checkLeaf(DtdType.ldml, "PCDATA");
    checkLeaf(DtdType.supplementalData, "PCDATA");
  }

  private void checkLeaf(DtdType type, String contained) {
    Relation<String, String> mainData = ElementAttributeInfo.getContainedElementData(type);
    Relation<String, String> elementToAttributes = ElementAttributeInfo.getElementToAttributes(type);
    Map<R2<String, String>, R3<Set<String>, String, String>> eaData = ElementAttributeInfo.getAttributeData(type);

    Set<String> data = mainData.getAll(contained);
    if (data == null) return;
    for (String element : data) {
      Set<String> attributes = elementToAttributes.getAll(element);
      if (attributes == null) {
        logln(type + ", " + contained + ", " + element + ", No attributes!");
        continue;
      }
      for (String attribute : attributes) {
        if (attribute.equals("draft") || attribute.equals("references")) continue;
        logln(type + ", " +  contained + ", " + element + ", " + attribute + ", " + eaData.get(Row.of(element, attribute)));
      }
    }
  }

  public void TestNodeData() {
    for (DtdType type : DtdType.values()) {
      checkNodeData(type);
    }
  }

  public void checkNodeData(DtdType type) {
    NodeData data = CurrentData.fullNodeData.get(type);
    Relation<String, String> dtdData = ElementAttributeInfo.getContainedElementData(type);
    Relation<String, String> containingData = ElementAttributeInfo.getContainingElementData(type);
    Relation<String, String> elementToAttributes = data.elementToAttributes;

    Relation<String, String> dtdElementToAttributes = ElementAttributeInfo.getElementToAttributes(type);
    
    Set<String> possibleElements = dtdElementToAttributes.keySet();
    Set<String> foundElements = elementToAttributes.keySet();
    if (!foundElements.equals(possibleElements)) {
      Set<String> missing = new TreeSet(possibleElements);
      missing.removeAll(foundElements);
      errln(type + "\t" + "Elements defined but not in data: " + missing);
    }
    
    // attributes not found
    for (String element : dtdElementToAttributes.keySet()) {
      Set<String> dtdAttributes = dtdElementToAttributes.getAll(element);
      Set<String> actualAttributes = elementToAttributes.getAll(element);
      if (actualAttributes == null) actualAttributes = Collections.EMPTY_SET;
      Set<String> notFound = new TreeSet(dtdAttributes);
      notFound.removeAll(actualAttributes);
      notFound.remove("draft");
      notFound.remove("references");
      notFound.remove("standard");
      notFound.remove("alt");
      notFound.remove("validSubLocales");
      if (notFound.size() != 0) {
        errln(type + "\t" + element + "\tAttributes not found: " + notFound + "\tfound: " + actualAttributes);
      }
    }

    
    
    Set<String> empties = dtdData.getAll("EMPTY");

    Set<String> overlap = getCommon(data.valueNodes.keySet(), data.branchNodes.keySet(), new TreeSet<String>());
    for (String s : overlap) {
      errln(type + "\tOverlap in value and branch!!\t" + s + "\t\t" + data.valueNodes.get(s) + "\t\t" + data.branchNodes.get(s));
    }
    for (String s : data.valueNodes.keySet()) {
      if (overlap.contains(s)) continue;
      logln(type + "\tLeaf: " + s + "\t\t" + data.valueNodes.get(s));
    }
    for (String s : data.branchNodes.keySet()) {
      if (overlap.contains(s)) continue;
      logln(type + "\tBranch: " + s + "\t\t" + data.branchNodes.get(s));
    }

    overlap = getCommon(data.valueNodes.keySet(), data.valuelessNodes.keySet(), new TreeSet<String>());
    for (String s : overlap) {
      errln(type + "\tOverlap in value and valueless!!\t" + s + "\t\t" + data.valueNodes.get(s) + "\t\t" + data.valuelessNodes.get(s));
    }

    for (String s : data.valueNodes.keySet()) {
      if (overlap.contains(s)) continue;
      logln(type + "\tValue: " + s + "\t\t" + data.valueNodes.get(s));
    }
    for (String s : data.valuelessNodes.keySet()) {
      if (overlap.contains(s)) continue;
      logln(type + "\tValueless: " + s + "\t\t" + data.valuelessNodes.get(s));
      if (!empties.contains(s)) {
        errln(type + "\t***Should be empty in DTD but isn't:\t" + s + ", " + containingData.getAll(s));
      }
    }
  }

  static class NodeData {
    final DtdType myType;
    Map<String,R2<String,String>> branchNodes = new TreeMap<String,R2<String,String>>();
    Map<String,R3<String,String,String>> valueNodes = new TreeMap<String,R3<String,String,String>>();
    Map<String,R2<String,String>> valuelessNodes = new TreeMap<String,R2<String,String>>();
    Relation<String,String> elementToAttributes = new Relation(new TreeMap(), TreeSet.class);
    NodeData(DtdType type) {
      myType = type;
    }
  }

  static class CurrentData {
    static Map<DtdType,NodeData> fullNodeData = new HashMap();
    static {
      int counter = 0;
      File common = new File(CldrUtility.COMMON_DIRECTORY);
      XPathParts parts = new XPathParts();
      for (String dir : common.list()) {
        Factory cldrFactory = Factory.make(CldrUtility.COMMON_DIRECTORY + "/" + dir, ".*");
        Set<String> locales = new TreeSet(cldrFactory.getAvailable());
        for (String locale : locales) {
          if ((counter++ % 10) == 0) {
            System.out.println(counter + ") Checking: " + dir + "," + locale);
          }
          CLDRFile file = (CLDRFile) cldrFactory.make(locale, false);
          NodeData nodeData = null;
          for (String xpath : file) {
            String value = file.getStringValue(xpath);
            String fullXpath = file.getFullXPath(xpath);
            parts.set(fullXpath);
            if (nodeData == null) {
              String root = parts.getElement(0);
              DtdType type = DtdType.valueOf(root);
              nodeData = fullNodeData.get(type);
              if (nodeData == null) {
                fullNodeData.put(type, nodeData = new NodeData(type));
              }
            }
            int last = parts.size()-1;
            String element = null;
            for (int i = 0; i <= last; ++i) {
              element = parts.getElement(i);
              Collection<String> attributes = parts.getAttributeKeys(i);
              nodeData.elementToAttributes.putAll(element,attributes);
              if (i != last) {
                putIfNew(nodeData.branchNodes, element, locale, xpath);
              } else {
                if (value.length() > 0) {
                  putIfNew(nodeData.valueNodes, element, value, locale, xpath);
                } else {
                  putIfNew(nodeData.valuelessNodes, element, locale, xpath);
                }
              }
            }
          }
        }
      }
    }
    static void putIfNew(Map<String,R2<String,String>> map, String key, String locale, String xpath) {
      if (!map.containsKey(key)) {
        map.put(key, Row.of(locale, xpath));
      }
    }
    static void putIfNew(Map<String,R3<String,String,String>> map, String key, String value, String locale, String xpath) {
      if (!map.containsKey(key)) {
        if (value.length() > 30) value = value.substring(0,30) + "...";
        map.put(key, Row.of(value, locale, xpath));
      }
    }
  }
}
