package org.unicode.cldr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import org.unicode.cldr.util.CLDRFile.DtdType;


public class ElementAttributeInfo {

  private DtdType dtdType;
  private Map<R2<String, String>,R3<Set<String>, String, String>> elementAttribute2Data = new TreeMap<R2<String, String>,R3<Set<String>, String, String>>();
  private Relation<String,String> element2children = new Relation(new LinkedHashMap(), LinkedHashSet.class);
  private Relation<String,String> element2parents = new Relation(new LinkedHashMap(), LinkedHashSet.class);
  private Relation<String,String> element2attributes = new Relation(new LinkedHashMap(), LinkedHashSet.class);

  static Map<String,Map<DtdType, ElementAttributeInfo>> cache = new HashMap<String,Map<DtdType, ElementAttributeInfo>>(); // new HashMap<DtdType, Data>();

  public static final ElementAttributeInfo getInstance(DtdType dtdType) {
    return getInstance(CldrUtility.COMMON_DIRECTORY, dtdType);
  }

  public static final ElementAttributeInfo getInstance(String commonDirectory, DtdType dtdType) {
    Map<DtdType, ElementAttributeInfo> result = cache.get(commonDirectory);
    if (result == null) {
      try {
        File file = new File(commonDirectory);
        String canonicalCommonDirectory;
        canonicalCommonDirectory = file.getCanonicalFile().toString();
        if (!commonDirectory.equals(canonicalCommonDirectory)) {
          result = cache.get(commonDirectory);
          if (result != null) {
            cache.put(commonDirectory, result);
          }
        }
        if (result == null) {
          result = new HashMap<DtdType, ElementAttributeInfo>();
          // pick short files that are in repository
          result.put(DtdType.ldml, new ElementAttributeInfo(canonicalCommonDirectory + "/main/root.xml", DtdType.ldml));
          result.put(DtdType.supplementalData, new ElementAttributeInfo(canonicalCommonDirectory + "/supplemental/plurals.xml", DtdType.supplementalData));
          result.put(DtdType.ldmlBCP47, new ElementAttributeInfo(canonicalCommonDirectory + "/bcp47/calendar.xml", DtdType.ldmlBCP47));
          cache.put(commonDirectory, result);
          cache.put(canonicalCommonDirectory, result);
        }
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return result.get(dtdType);
  }

  //  static {
  //    try {
  //      addFromDTD(CldrUtility.COMMON_DIRECTORY + "main/en.xml", DtdType.ldml);
  //      addFromDTD(CldrUtility.COMMON_DIRECTORY + "supplemental/characters.xml", DtdType.supplementalData);
  //      addFromDTD(CldrUtility.COMMON_DIRECTORY + "bcp47/calendar.xml", DtdType.ldmlBCP47);
  //    } catch (IOException e) {
  //      throw new IllegalArgumentException(e);
  //    }
  //  }

  private ElementAttributeInfo(String filename, CLDRFile.DtdType type) throws IOException {
    //StringBufferInputStream fis = new StringBufferInputStream(
    //"<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/1.2/ldml.dtd\"><ldml></ldml>");
    FileInputStream fis = new FileInputStream(filename);
    try {
      XMLReader xmlReader = CLDRFile.createXMLReader(true);
      this.dtdType = type;
      MyDeclHandler me = new MyDeclHandler(this);
      xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", me);
      InputSource is = new InputSource(fis);
      is.setSystemId(filename);
      //xmlReader.setContentHandler(me);
      //xmlReader.setErrorHandler(me);
      xmlReader.parse(is);
      this.elementAttribute2Data = Collections.unmodifiableMap(getElementAttribute2Data()); // TODO, protect rows
      getElement2Children().freeze(); 
      getElement2Parents().freeze();
      getElement2Attributes().freeze();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      fis.close();
    }
  }


  private DtdType getDtdType() {
    return dtdType;
  }

  public Map<R2<String, String>,R3<Set<String>, String, String>> getElementAttribute2Data() {
    return elementAttribute2Data;
  }

  public Relation<String,String> getElement2Children() {
    return element2children;
  }

  public Relation<String,String> getElement2Parents() {
    return element2parents;
  }

  public Relation<String,String> getElement2Attributes() {
    return element2attributes;
  }

  static class MyDeclHandler implements DeclHandler {
    private static final boolean SHOW = false;
    private ElementAttributeInfo myData;

    Matcher idmatcher = Pattern.compile("[a-zA-Z0-9][-_a-zA-Z0-9]*").matcher("");

    public MyDeclHandler(ElementAttributeInfo indata) {
      myData = indata;
    }

    public void attributeDecl(String eName, String aName, String type, String mode, String value) throws SAXException {
      if (SHOW) System.out.println(myData.getDtdType() + "\tAttributeDecl\t" + eName + "\t" + aName + "\t" + type + "\t" + mode + "\t" + value);
      R2<String, String> key = Row.of(eName, aName);
      Set<String> typeSet = getIdentifiers(type);
      R3<Set<String>, String, String> value2 = Row.of(typeSet, mode, value);
      R3<Set<String>, String, String> oldValue = myData.getElementAttribute2Data().get(key);
      if (oldValue != null && !oldValue.equals(value2)) {
        throw new IllegalArgumentException("Conflict in data: " + key + "\told: " + oldValue + "\tnew: " + value2);
      }
      myData.getElementAttribute2Data().put(key, value2);
      myData.getElement2Attributes().put(eName, aName);
    }

    private Set<String> getIdentifiers(String type) {
      Set<String> result = new LinkedHashSet<String>();
      idmatcher.reset(type);
      while (idmatcher.find()) {
        result.add(idmatcher.group());
      }
      if (result.size() == 0) {
        throw new IllegalArgumentException("No identifiers found in: " + type);
      }
      return result;
    }

    public void elementDecl(String name, String model) throws SAXException {
      if (SHOW) System.out.println(myData.getDtdType() + "\tElement\t" + name + "\t" + model);
      Set<String> identifiers = getIdentifiers(model);
      //identifiers.remove("special");
      //identifiers.remove("alias");
      if (identifiers.size() == 0) {
        identifiers.add("EMPTY");
      }
      myData.getElement2Children().putAll(name, identifiers);
      for (String identifier : identifiers) {
        myData.getElement2Parents().put(identifier, name);
      }
    }

    public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
      // TODO Auto-generated method stub

    }

    public void internalEntityDecl(String name, String value) throws SAXException {
      // TODO Auto-generated method stub

    }
  }
}
