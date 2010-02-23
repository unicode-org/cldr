package org.unicode.cldr.draft;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Utility;

public class JsonConverter {

  private static final String FILES = ".*";
  private static final String MAIN_DIRECTORY = CldrUtility.SUPPLEMENTAL_DIRECTORY; //CldrUtility.MAIN_DIRECTORY;
  private static final String OUT_DIRECTORY = CldrUtility.GEN_DIRECTORY + "/jason/"; //CldrUtility.MAIN_DIRECTORY;
  private static final boolean COMPACT = false;
  static final Set<String> REPLACING_BASE = !COMPACT ? Collections.EMPTY_SET : new HashSet<String>(Arrays.asList("type id key count".split("\\s")));
  static final Set<String> EXTRA_DISTINGUISHING = new HashSet<String>(Arrays.asList("locales territory desired supported".split("\\s")));
  static final Relation<String, String> mainInfo = ElementAttributeInfo.getElementToAttributes(DtdType.ldml);
  static final Relation<String, String> suppInfo = ElementAttributeInfo.getElementToAttributes(DtdType.supplementalData);

  public static void main(String[] args) throws IOException {
    final String subdirectory = new File(MAIN_DIRECTORY).getName();
    final Factory cldrFactory = Factory.make(MAIN_DIRECTORY, FILES);
    final Set<String> locales = new TreeSet<String>(cldrFactory.getAvailable());
    final XPathParts parts = new XPathParts();
    //ElementName elementName = new ElementName();
    LinkedHashMap<String,String> nonDistinguishing = new LinkedHashMap<String,String>();
    BitSet ordered = new BitSet();
    for (String locale : locales) {
      System.out.println("Converting:\t" + locale);
      final CLDRFile file = (CLDRFile) cldrFactory.make(locale, false);
      Relation<String, String> element2Attributes = file.isNonInheriting() ? suppInfo : mainInfo;
      final Item main = new TableItem(null);
      for (Iterator<String> it = file.iterator("",CLDRFile.ldmlComparator); it.hasNext();) {
        final String xpath = it.next();
        final String fullXpath = file.getFullXPath(xpath);
        final String value = file.getStringValue(xpath);
        parts.set(fullXpath);
        rewrite(parts);
        Item current = main;
        int size = parts.size();

        // we have to prescan for _q
        ordered.clear();
        for (int i = 0; i < size-1; ++i) {
          if (parts.getAttributeValue(i, "_q") != null) {
            ordered.set(i);
          }
        }

        // build up the table with the intermediate elements
        for (int i = 1; i < size; ++i) {
          final String element = parts.getElement(i);
          Item.Type nextLevelOrdered = ordered.get(i+1) ? Item.Type.orderedItem : Item.Type.unorderedItem;
          current = current.makeSubItem(element, nextLevelOrdered);

          Collection<String> actualAttributeKeys = parts.getAttributeKeys(i);
          Set<String> possibleAttributeKeys = element2Attributes.getAll(element);
          if (possibleAttributeKeys != null) {
            for (final String attribute : actualAttributeKeys) {
              String attributeValue = parts.getAttributeValue(i, attribute);
              if (!isDistinguishing(element, attribute)) {
                try {
                  current.put("~" + attribute, attributeValue);
                  //Item temp = current.makeSubItem(elementName.toString(), nextLevelOrdered);
                  //nextLevelOrdered = Item.Type.unorderedItem;
                  //current.put(elementName.toString() + "~" + attribute, attributeValue);
                } catch (Exception e) {
                  System.out.println("*3 " + e.getMessage() + "\t" + xpath);
                }
              }
            }
            for (final String attribute : possibleAttributeKeys) {
              if (isDistinguishing(element, attribute)) {
                //elementName.add(attribute, attributeValue);
                if (attribute.equals("alt")) continue; // TODO fix
                current = current.makeSubItem("_" + attribute, Item.Type.unorderedItem);
                String attributeValue = parts.getAttributeValue(i, attribute);
                if (attributeValue == null) attributeValue = "?";
                current = current.makeSubItem(attributeValue, Item.Type.unorderedItem);
              }
            }
          }
//          if (i == size-1) {
//            current.put(element, value);
//          } else {
//            current = current.makeSubItem(elementName.toString(),nextLevelOrdered);
//          }
        }
        if (value.length() != 0) {
          current.put("~value", value);
        } else {
          if (current.size() == 0) {
            Item parent = current.parent;
            if (parent.size() == 1) {
              Item grandparent = parent.parent;
            }
          }
        }

//
//        // do the final element
//        final String element = parts.getElement(size-1);
//        elementName.reset(element);
//        nonDistinguishing.clear();
//        for (final String attribute : parts.getAttributeKeys(-1)) {
//          if (attribute.equals("_q")) continue;
//          final String attributeValue = parts.getAttributeValue(-1, attribute);
//          if (isDistinguishing(element, attribute)) {
//            elementName.add(attribute, attributeValue);
//          } else {
//            nonDistinguishing.put(attribute, attributeValue);
//          }
//        }
//        String name = elementName.toString();
//        try {
//          current.put(name,value);
//        } catch (Exception e) {
//          System.out.println("*4 " + e.getMessage());
//        }
//        for (String attribute : nonDistinguishing.keySet()) {
//          String attributeValue = nonDistinguishing.get(attribute);
//          try {
//            current.put(name + "~" + attribute, attributeValue);
//          } catch (Exception e) {
//            System.out.println("*5 " + e.getMessage() + "\t" + xpath + ",\t" + value);
//          }
//        }
      }
      PrintWriter out = BagFormatter.openUTF8Writer(OUT_DIRECTORY + subdirectory, locale +".json");
      main.print(out, 0);
      out.close();
    }

  }

  static Relation<String,String> extraDistinguishing = new Relation(new TreeMap(), LinkedHashSet.class);
  static {
    putAll(extraDistinguishing, "dayPeriodRule", "earlyMorning", "before", "from");
  }
  static <K, V> void putAll(Relation r, K key, V... values) {
    r.putAll(key, Arrays.asList(values));
  }
  private static boolean isDistinguishing(final String element, final String attribute) {
    //      <mapZone other="Afghanistan" territory="001" type="Asia/Kabul"/> result is the type!
    //             <deprecatedItems elements="variant" attributes="type" values="BOKMAL NYNORSK AALAND POLYTONI"/>
    // ugly: if there are values, then everything else is distinguishing, ow if there are attibutes, elements are
    if (element.equals("deprecatedItems")) {

    }
    Set<String> extras = extraDistinguishing.getAll(element);
    if (extras != null && extras.contains(attribute)) return true;
    if (EXTRA_DISTINGUISHING.contains(attribute)) return true;
    return CLDRFile.isDistinguishing(element, attribute);
  }

  private static void rewrite(XPathParts parts) {
    if (!COMPACT) {
      return;
    }
    if (parts.getElement(-1).equals("type")) {
      String key = parts.getAttributeValue(-1, "key");
      if (key != null) {
        parts.setElement(-2, key + "Key");
        parts.putAttributeValue(-1, "key", null);
      }
      // fall thru
    }
    if (parts.getElement(1).equals("localeDisplayNames")) {
      String element2 = parts.getElement(2);
      if (!element2.endsWith("Pattern")) {
        if (element2.endsWith("s")) {
          element2 = element2.substring(0,element2.length()-1);
        }
        parts.setElement(2, element2+"Names");
      }
      parts.removeElement(1);
    }
    if (parts.getElement(1).equals("dates")) {
      parts.removeElement(1);
      String element1 = parts.getElement(1);
      if (element1.equals("timeZoneNames")) {
        String main = parts.getElement(2);
        if (main.equals("zone") || main.equals("metazone")) {
          parts.setElement(1, main + "Names");
        }
        return;
      }
    }
    if (parts.getElement(1).equals("numbers") && parts.getElement(2).equals("currencies")) {
      parts.removeElement(1);
      return;
    }
  }

  static class ElementName {
    String oldBase;
    String base;
    boolean replacedBase;
    StringBuilder suffix = new StringBuilder();
    public void reset(String element) {
      suffix.setLength(0);
      base = oldBase = element;
      replacedBase = false;
    }
    public void add(String attribute, String attributeValue) {
      if (REPLACING_BASE.contains(attribute)) {
        if (replacedBase) {
          System.out.println("ERROR: Two replacement types on same element!!\t" + oldBase + "," + base + "," + attribute + "," + attributeValue);
        } else {
          replacedBase = true;
          base = attributeValue;
          return;
        }
      }
      suffix.append('$').append(attribute).append('=').append(attributeValue);
    }
    public String toString() {
      if (suffix == null) {
        return base;
      }
      return base + suffix;
    }
  }

  static abstract class Item {
    protected Item parent;
    
    public Item(Item parent) {
      this.parent = parent;
    }
    
    public abstract int size();

    enum Type {unorderedItem, orderedItem}

    public abstract Appendable print(Appendable result, int i);

    protected Appendable indent(Appendable result, int i) throws IOException {
      return result.append(getIndent(i));
    }
    protected String getIndent(int i) {
      return Utility.repeat("    ", i);
    }
    public Appendable appendString(Appendable result, String string, int indent) throws IOException {
      result.append('"');
      for (int i = 0; i < string.length(); ++i) {
        // http://www.json.org/
        // any-Unicode-character-except-"-or-\-or-control-character
        // uses UTF16
        char ch = string.charAt(i);
        switch(ch) {
        case '\"': result.append("\\\""); break;
        case '\\': result.append("\\\\"); break;
        case '/': result.append("\\/"); break;
        case '\b': result.append("\\b"); break;
        case '\f': result.append("\\f"); break;
        case '\n': 
          if (indent < 0) {
            result.append("\\n");
          } else {
            result.append('\n').append(getIndent(indent));
          }
          break;
        case '\r': result.append("\\r"); break;
        case '\t': result.append("\\t"); break;
        default:
          if (ch <= 0x1F || 0x7F <= ch && ch <= 0x9F) {
            result.append("\\u").append(Utility.hex(ch,4));
          } else {
            result.append(ch);
          }
          break;
        }
      }
      return result.append('"');
    }
    public String toString() {
      return print(new StringBuilder(),0).toString();
    }
    protected Item create(Type ordered) {
      switch (ordered) {
      case unorderedItem:
        return new TableItem(this);
      case orderedItem:
        return new ArrayItem(this);
      default:
        throw new UnsupportedOperationException();
      }
    }
    public abstract Item makeSubItem(String element, Type ordered);
    public abstract void put(String element, String value);
  }

  static class TableItem extends Item {
    public TableItem(Item parent) {
      super(parent);
    }

    private Map<String,Item> map = new LinkedHashMap<String,Item>();

    public Item get(String element) {
      return map.get(element);
    }

    public void put(String element, String value) {
      Item old = map.get(element);
      if (old != null) {
        if (old instanceof StringItem) {
          if (value.equals(((StringItem)old).value)) {
            return;
          }
        }
        throw new IllegalArgumentException("ERROR: Table already has object: " + element + ", " + old + ", " + value);
      }
      map.put(element, new StringItem(value));
    }

    public Item makeSubItem(String element, Type ordered) {
      Item result = map.get(element);
      if (result != null) {
        return result;
      }
      result = create(ordered);
      result.parent = this;
      
      map.put(element, result);
      return result;
    }

    public Appendable print(Appendable result, int i) {
      try {
        if (map.size() == 0) {
          result.append("{}");
          return result;
        }
        result.append("{\n");
        boolean first = true;
        for (String key : map.keySet()) {
          Item value = map.get(key);
          if (first) {
            first = false;
          } else {
            result.append(",\n");
          }
          indent(result, i+1);
          appendString(result, key, -1).append(" : ");
          value.print(result,i+1);
        }
        result.append("\n");
        indent(result, i).append("}");
        return result;
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }

    @Override
    public int size() {
      return map.size();
    }
  }

  static class ArrayItem extends Item {
    public ArrayItem(Item parent) {
      super(parent);
    }

    private List<Item> list = new ArrayList<Item>();

    @Override
    public Appendable print(Appendable result, int i) {
      try {
        if (list.size() == 0) {
          result.append("[]");
          return result;
        }

        result.append("[\n");
        for (int j = 0; j < list.size(); ++j) {
          if (j != 0) {
            result.append(",\n");
          }
          indent(result, i+1);
          list.get(j).print(result,i+1);
        }
        result.append("\n");
        indent(result, i).append("]");
        return result;
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }

    public Item makeSubItem(String element, Type ordered) {
      Item result = create(ordered);
      TableItem pair = new TableItem(this);
      pair.map.put(element, result);
      list.add(pair);
      return result;
    }

    public void put(String element, String value) {
      TableItem pair = new TableItem(this);
      pair.put(element, value);
      list.add(pair);
    }

    @Override
    public int size() {
      return list.size();
    }
  }

  static class StringItem extends Item {
    private String value;
    public StringItem(String value2) {
      super(null);
      value = value2;
    }
    @Override
    public Appendable print(Appendable result, int i) {
      try {
        return appendString(result, value, i+1);
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }
    @Override
    public Item makeSubItem(String element, Type ordered) {
      throw new UnsupportedOperationException();
    }
    @Override
    public void put(String element, String value) {
      throw new UnsupportedOperationException();
    }
    @Override
    public int size() {
      throw new UnsupportedOperationException();
    }
  }
}

