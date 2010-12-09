package org.unicode.cldr.unittest;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.ElementAttributeInfo;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Utility;
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

  public void TestOrdering() {
    for (DtdType type : DtdType.values()) {
      checkOrdering(type);
    }
  }

  private void checkOrdering(DtdType type) {
    Relation<String, String> toChildren = ElementAttributeInfo.getInstance(type).getElement2Children();
    //Relation<String, String> toParents = ElementAttributeInfo.getElement2Parents(type);
    Set<String> containsOrdered = new LinkedHashSet();
    for (String element : toChildren.keySet()) {
      int ordered = 0;
      Set<String> containedElements = toChildren.getAll(element);
      for (String contained : containedElements) {
        if (contained.equals("cp")) continue;
        int currentOrdered = CLDRFile.isOrdered(contained, null) ? 1 : -1;
        if (currentOrdered > 0) {
          containsOrdered.add(element);
        }
        if (ordered == 0) {
          ordered = currentOrdered;
        } else {
          if (ordered != currentOrdered) {
            String error = type + "\tMixed ordering inside\t" + element + ":\tordered:\t";
            for (String contained2 : containedElements) {
              if (contained.equals("cp") || !CLDRFile.isOrdered(contained2, null)) continue;
              error += " " + contained2;
            }
            error += ":\tunordered:\t";
            for (String contained2 : containedElements) {
              if (contained.equals("cp") || CLDRFile.isOrdered(contained2, null)) continue;
              error += " " + contained2;
            }
            errln(error);
            break;
          }
        }
      }
    }
    if (containsOrdered.size() != 0) {
      System.out.println();
      System.out.println("                  " + "// DTD: " + type);
      for (String element : containsOrdered) {
        System.out.println("                  " + "// <" + element + "> children");
        StringBuilder line = new StringBuilder("                  ");
        for (String contained : toChildren.getAll(element)) {
          line.append("\"" +
                  contained +
          "\", ");
        }
        System.out.println(line);
        System.out.println();
      }
    }
  }

  public void TestDistinguishing() {
    for (DtdType type : DtdType.values()) {
      showDistinguishing(type);
    }
  }

  private void showDistinguishing(DtdType dtdType) {
    Relation<String, String> elementToAttributes = ElementAttributeInfo.getInstance(dtdType).getElement2Attributes();
    Relation<String,String> distinguishingAttributeToElements = new Relation(new TreeMap(), TreeSet.class);
    Relation<String,String> nondistinguishingAttributeToElements = new Relation(new TreeMap(), TreeSet.class);
    Relation<String,String> orderedAttributeToElements = new Relation(new TreeMap(), TreeSet.class);
    Map<R2<String, String>, R3<Set<String>, String, String>> attributeData = ElementAttributeInfo.getInstance(dtdType).getElementAttribute2Data();

    for (String element : elementToAttributes.keySet()) {
      boolean isOrdered = CLDRFile.isOrdered(element, null);
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

    // distinguishing elements should be required
    // make exception for alt
    for (String attribute :distinguishingAttributeToElements.keySet()) {
      if (attribute.equals("alt")) continue;
      for (String element :distinguishingAttributeToElements.getAll(attribute)) {
        R3<Set<String>, String, String> attributeInfo = attributeData.get(Row.of(element,attribute));
        if (!"REQUIRED".equals(attributeInfo.get1())) {
          errln(dtdType + "\t" + element + "\t" + attribute + "\tDistinguishing attribute, but not REQUIRED in DTD");
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
    checkAttributes(DtdType.ldml, DtdType.supplementalData);
    checkAttributes(DtdType.ldml, DtdType.ldmlBCP47);
    checkAttributes(DtdType.ldmlBCP47, DtdType.supplementalData);
  }

  private void checkAttributes(DtdType dtdType1, DtdType dtdType2) {
    Map<R2<String, String>, R3<Set<String>, String, String>> mainData = ElementAttributeInfo.getInstance(dtdType1).getElementAttribute2Data();
    Map<R2<String, String>, R3<Set<String>, String, String>> suppData = ElementAttributeInfo.getInstance(dtdType2).getElementAttribute2Data();
    Set<R2<String, String>> commonKeys = getCommon(mainData.keySet(), suppData.keySet(), new TreeSet<R2<String, String>>());
    Set<R2<String, String>> same = new TreeSet<R2<String, String>>();
    for (R2<String, String> key : commonKeys) {
      R3<Set<String>, String, String> mainValue = mainData.get(key);
      R3<Set<String>, String, String> suppValue = suppData.get(key);
      if (mainValue.equals(suppValue)) {
        same.add(key);
        continue;
      }
      errln("DTDs have different attributes\t" + key + "\t" + dtdType1 + ":\t" + mainValue + "\t" + dtdType2 + ":\t" + suppValue);
    }
    for (R2<String, String> key : same) {
      logln(dtdType1 + " and " + dtdType2 + ":\t" + key + "\t" + mainData.get(key));
    }
    for (R2<String, String> key : mainData.keySet()) {
      if (commonKeys.contains(key)) continue;
      logln(dtdType1 + ":\t" + key + "\t" + mainData.get(key));
    }
    for (R2<String, String> key : suppData.keySet()) {
      if (commonKeys.contains(key)) continue;
      logln(dtdType2 + ":\t" + key + "\t" + suppData.get(key));
    }
  }

  private <T extends Collection> T getCommon(T a, T b, T result) {
    result.addAll(a);
    result.retainAll(b);
    return result;
  }

  public void TestElements() {
    checkElements(DtdType.ldml, DtdType.supplementalData);
    checkElements(DtdType.ldml, DtdType.ldmlBCP47);
    checkElements(DtdType.ldmlBCP47, DtdType.supplementalData);
  }

  private void checkElements(DtdType dtdType1, DtdType dtdType2) {
    Relation<String, String> mainData = ElementAttributeInfo.getInstance(dtdType1).getElement2Children();
    Relation<String, String> suppData = ElementAttributeInfo.getInstance(dtdType2).getElement2Children();
    Set<String> commonKeys = getCommon(mainData.keySet(), suppData.keySet(), new TreeSet<String>());
    Set<String> same = new TreeSet<String>();
    for (String key : commonKeys) {
      Set<String> mainValues = mainData.getAll(key);
      Set<String> suppValues = suppData.getAll(key);
      if (mainValues.equals(suppValues)) {
        same.add(key);
        continue;
      }
      errln("DTD elements have different children\t" + key + "\t" + dtdType1 + ":\t" + mainValues + "\t" + dtdType2 + ":\t" + suppValues);
    }
    for (String key : same) {
      logln(dtdType1 + " and " + dtdType2 + ":\t" + key + "\t" + mainData.getAll(key));
    }
    for (String key : mainData.keySet()) {
      if (commonKeys.contains(key)) continue;
      logln(dtdType1 + ":\t" + key + "\t" + mainData.getAll(key));
    }
    for (String key : suppData.keySet()) {
      if (commonKeys.contains(key)) continue;
      logln(dtdType2 + ":\t" + key + "\t" + suppData.getAll(key));
    }
  }

  public void TestEmpty() {
    for (DtdType type : DtdType.values()) {
      checkEmpty(type);
    }
  }

  static final Set<String> COLLATION_SINGLETONS = new HashSet<String>(Arrays.asList(new String[] {
          "first_non_ignorable", "first_primary_ignorable", "first_secondary_ignorable", "first_tertiary_ignorable", "first_trailing", "first_variable",
          "last_non_ignorable", "last_primary_ignorable", "last_secondary_ignorable", "last_tertiary_ignorable", "last_trailing last_variable",
          "last_trailing", "last_variable"
  }));

  private void checkEmpty(DtdType type) {
    Relation<String, String> mainData = ElementAttributeInfo.getInstance(type).getElement2Parents();
    Relation<String, String> elementToAttributes = ElementAttributeInfo.getInstance(type).getElement2Attributes();
    Map<R2<String, String>, R3<Set<String>, String, String>> eaData = ElementAttributeInfo.getInstance(type).getElementAttribute2Data();

    Set<String> empties = mainData.getAll("EMPTY");
    for (String empty : empties) {
      if (COLLATION_SINGLETONS.contains(empty)) continue;
      Set<String> attributes = elementToAttributes.getAll(empty);
      if (attributes == null) {
        errln("Is EMPTY but no attributes:\t" + type + ", " + empty);
        continue;
      }
      for (String attribute : attributes) {
        if (attribute.equals("draft") || attribute.equals("references")) continue;
        logln(type + ", " + empty + ", " + attribute + ", " + eaData.get(Row.of(empty, attribute)));
      }
    }
  }

  public void TestLeaf() {
    for (DtdType type : DtdType.values()) {
      checkLeaf(type, "PCDATA");
    }
  }

  private void checkLeaf(DtdType type, String contained) {
    Relation<String, String> mainData = ElementAttributeInfo.getInstance(type).getElement2Parents();
    Relation<String, String> elementToAttributes = ElementAttributeInfo.getInstance(type).getElement2Attributes();
    Map<R2<String, String>, R3<Set<String>, String, String>> eaData = ElementAttributeInfo.getInstance(type).getElementAttribute2Data();

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

  public void TestZNodeData() {
    for (DtdType type : DtdType.values()) {
      checkNodeData(type);
    }
  }

  public void checkNodeData(DtdType type) {
    CurrentData.NodeData data = CurrentData.fullNodeData.get(type);
    Relation<String, String> element2Parents = ElementAttributeInfo.getInstance(type).getElement2Parents();
    Relation<String, String> element2Children = ElementAttributeInfo.getInstance(type).getElement2Children();
    Relation<String, String> elementToAttributes = data.elementToAttributes;

    Relation<String, String> dtdElementToAttributes = ElementAttributeInfo.getInstance(type).getElement2Attributes();
    Map<R2<String, String>, R3<Set<String>, String, String>> attributeData = ElementAttributeInfo.getInstance(type).getElementAttribute2Data();


    Set<String> possibleElements = dtdElementToAttributes.keySet();
    Set<String> foundElements = elementToAttributes.keySet();
    if (!foundElements.equals(possibleElements)) {
      Set<String> missing = new TreeSet(possibleElements);
      missing.removeAll(foundElements);
      errln(type + "\t" + "Elements defined but not in data:\t" + missing);
    }

    // attributes not found
    for (String element : dtdElementToAttributes.keySet()) {
      Set<String> dtdAttributes = dtdElementToAttributes.getAll(element);
      Set<String> actualAttributes = remove_q(elementToAttributes.getAll(element));
      Set<String> attributesAlwaysFound = remove_q(data.elementToAttributesAlwaysFound.getAll(element));

      if (!dtdAttributes.containsAll(actualAttributes) || !dtdAttributes.containsAll(attributesAlwaysFound)) {
        errln(type + "\t" + "Actual attributes exceed DTD attributes:\t" + type + ", " + element + ", " + dtdAttributes + ", " + actualAttributes + ", " + attributesAlwaysFound);
      }
      
      Set<String> notFound = new TreeSet(dtdAttributes);
      notFound.removeAll(actualAttributes);
      notFound.remove("draft");
      notFound.remove("references");
      notFound.remove("standard");
      notFound.remove("alt");
      notFound.remove("validSubLocales");
      if (notFound.size() != 0) {
        warnln(type + "\tAttributes not found for:\t" + element + "\tnotFound:\t" + notFound + "\tfound:\t" + actualAttributes);
      }

      for (String attributeAlwaysFound : attributesAlwaysFound) {
        // make sure REQUIRED; not really an error, but for now...
        R3<Set<String>, String, String> attributeDatum = attributeData.get(Row.of(element,attributeAlwaysFound));
        if (attributeDatum == null) {
          errln(type + "\tData not found for " + type + ", " + element + ", " + attributeAlwaysFound);
          continue;
        }
        if (!"#REQUIRED".equals(attributeDatum.get1())) {
          if (attributeDatum.get1() == null) {
            warnln(type + "\tAttribute not REQUIRED but ALWAYS found, element:\t" + element + "\tattribute:\t" + attributeAlwaysFound);
          }
        }
      }
    }


    Set<String> empties = element2Parents.getAll("EMPTY");

    Set<String> overlap = getCommon(data.valueNodes.keySet(), data.branchNodes.keySet(), new TreeSet<String>());
    for (String s : overlap) {
      warnln(type + "\tOverlap in value and branch!!\t" + s + "\tvalue:\t" + data.valueNodes.get(s) + "\tbranch:\t" + data.branchNodes.get(s));
    }
    for (String s : data.valueNodes.keySet()) {
      if (overlap.contains(s)) continue;
      logln(type + "\tLeaf:\t" + s + "\t\t" + data.valueNodes.get(s));
    }
    for (String s : data.branchNodes.keySet()) {
      if (overlap.contains(s)) continue;
      logln(type + "\tBranch:\t" + s + "\t\t" + data.branchNodes.get(s));
    }

    overlap = getCommon(data.valueNodes.keySet(), data.valuelessNodes.keySet(), new TreeSet<String>());
    for (String s : overlap) {
      warnln(type + "\tOverlap in value and valueless!!\t" + s + "\tvalue:\t" + data.valueNodes.get(s) + "\tvalueless:\t" + data.valuelessNodes.get(s));
    }

    for (String s : data.valueNodes.keySet()) {
      if (overlap.contains(s)) continue;
      logln(type + "\tValue:\t" + s + "\t\t" + data.valueNodes.get(s));
    }
    for (String s : data.valuelessNodes.keySet()) {
      if (overlap.contains(s)) continue;
      logln(type + "\tValueless:\t" + s + "\t\t" + data.valuelessNodes.get(s));
      Set<String> containing = element2Children.getAll(s);
      if (!empties.contains(s) && containing.contains("PCDATA")) {
        errln(type + "\t***Should be empty in DTD but isn't:\t" + s + "\t" + containing);
      }
    }
  }

  private Set<String> remove_q(Set<String> actualAttributes) {
    if (actualAttributes == null) {
      actualAttributes = Collections.EMPTY_SET;
    } else if (actualAttributes.contains("_q")){
      actualAttributes = new LinkedHashSet<String>(actualAttributes);
      actualAttributes.remove("_q");
    }
    return actualAttributes;
  }


  static class CurrentData {
    static class NodeData {
      final DtdType myType;
      Map<String,R2<String,String>> branchNodes = new TreeMap<String,R2<String,String>>();
      Map<String,R3<String,String,String>> valueNodes = new TreeMap<String,R3<String,String,String>>();
      Map<String,R2<String,String>> valuelessNodes = new TreeMap<String,R2<String,String>>();
      Relation<String,String> elementToAttributes = new Relation(new TreeMap(), TreeSet.class);
      Relation<String,String> elementToAttributesAlwaysFound = new Relation(new TreeMap(), TreeSet.class);
      NodeData(DtdType type) {
        myType = type;
      }
    }
    static Map<DtdType,NodeData> fullNodeData = new HashMap<DtdType,NodeData>();
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
              Set<String> oldAlways = nodeData.elementToAttributesAlwaysFound.getAll(element);
              if (oldAlways == null) {
                nodeData.elementToAttributesAlwaysFound.putAll(element,attributes);
              } else {
                // need retainAll, removeAll
                for (String old : new TreeSet<String>(oldAlways)) {
                  if (!attributes.contains(old)) {
                    nodeData.elementToAttributesAlwaysFound.remove(element, old);
                  }
                }
              }
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
        value = value.replace("\n", "\\n");
        map.put(key, Row.of(value, locale, xpath));
      }
    }
  }

  public void TestStructure() {
    for (DtdType type : DtdType.values()) {
      System.out.println();
      System.out.println("*= distinguished, \u2021=ordered, \u2020=default");
      Relation<String, String> toChildren = ElementAttributeInfo.getInstance(type).getElement2Children();
      Relation<String, String> toAttributes = ElementAttributeInfo.getInstance(type).getElement2Attributes();
      Map<R2<String, String>, R3<Set<String>, String, String>> toAttributeData = ElementAttributeInfo.getInstance(type).getElementAttribute2Data();
      checkStructure(type, type.toString(), 0, toChildren, toAttributes, toAttributeData);
    }
  }

  private void checkStructure(DtdType dtdType, String element, int indent, Relation<String, String> toChildren,
          Relation<String, String> toAttributes, Map<R2<String, String>, R3<Set<String>, String, String>> toAttributeData) {
    Set<String> myChildren = toChildren.getAll(element);
    String values = "";
    boolean skipChildren = false;
    if (myChildren != null) {
      if (myChildren.contains("PCDATA")) {
        values = "\tVALUE=" + myChildren.toString();
        skipChildren = true;
      } else if (myChildren.contains("EMPTY")) {
        // values = ";\t\tVALUE=" + myChildren.toString();
        skipChildren = true;
      }
    }
    String elementName = element;
    if (CLDRFile.isOrdered(element, dtdType)) {
      elementName += "\u2021";
    }
    System.out.println(Utility.repeat("\t", indent) + elementName 
            + checkAttributeStructure(element, toAttributes.getAll(element), toAttributeData)
            + values);
    if (myChildren == null || skipChildren) return;
    for (String child : myChildren) {
      checkStructure(dtdType, child, indent+1, toChildren, toAttributes, toAttributeData);
    }
  }

  private String checkAttributeStructure(String element, Set<String> attributes, Map<R2<String, String>, 
          R3<Set<String>, String, String>> toAttributeData) {
    if (attributes == null) return "";
    String result = "";
    for (String attribute : attributes) {
      if (attribute.equals("alt") || attribute.equals("draft") || attribute.equals("standard") || attribute.equals("references")) continue;
      if (result.length() != 0) result += "\t";
      R3<Set<String>, String, String> data = toAttributeData.get(Row.of(element,attribute));
      if (CLDRFile.isDistinguishing(element, attribute)) {
        attribute += "*";
      }
      attribute += "=" + formatForAttribute(data.get0(), data.get1(), data.get2());
      result += attribute;
    }
    if (result.length() == 0) return result;
    return "\t" + result;
    // draft, standard, references, alt
  }

  private String formatForAttribute(Set<String> type, String mode, String value) {
    String first = type.iterator().next();
    String typeName = attributeTypes.contains(first) ? first : type.toString();
    if (mode == null) {
      if (value == null)      {
        throw new IllegalArgumentException(type + ", " + mode + ", " + value);
      }
      return typeName.replace(value, value + "\u2020");
    }
    if (mode.equals("#FIXED")) return value;
    if (value != null) {
      throw new IllegalArgumentException(type + ", " + mode + ", " + value);
    }
    if (mode.equals("#REQUIRED")) {
      return typeName;
    }
    if (mode.equals("#IMPLIED")) {
      return typeName + "?";
    }
    throw new IllegalArgumentException(type + ", " + mode + ", " + value);
  }

  static Set<String> attributeTypes = new HashSet<String>(Arrays.asList(new String[] {
          "CDATA", "ID", "IDREF", "IDREFS", "ENTITY", "ENTITIES", "NMTOKEN", "NMTOKENS"
  }));

  static Set<String> attributeMode = new HashSet<String>(Arrays.asList(new String[] {
          "#REQUIRED", "#IMPLIED", "#FIXED"
  }));
}
