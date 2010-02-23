package org.unicode.cldr.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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

  static class Data {
    DtdType dtdType;
    Map<R2<String, String>,R3<Set<String>, String, String>> mainAttributeData = new TreeMap<R2<String, String>,R3<Set<String>, String, String>>();
    Relation<String,String> mainElementData = new Relation(new TreeMap(), TreeSet.class);
    Relation<String,String> mainElementDataRev = new Relation(new TreeMap(), TreeSet.class);
    Relation<String,String> mainElementToAttributes = new Relation(new TreeMap(), TreeSet.class);
    Data(DtdType type) {
      dtdType = type;
    }
  }
  
  static Map<DtdType, Data> data = new HashMap<DtdType, Data>();

  static {
    try {
      addFromDTD(CldrUtility.COMMON_DIRECTORY + "main/en.xml", DtdType.ldml);
      addFromDTD(CldrUtility.COMMON_DIRECTORY + "supplemental/characters.xml", DtdType.supplementalData);
      addFromDTD(CldrUtility.COMMON_DIRECTORY + "bcp47/calendar.xml", DtdType.ldmlBCP47);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static void addFromDTD(String filename, CLDRFile.DtdType type) throws IOException {
    //StringBufferInputStream fis = new StringBufferInputStream(
    //"<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/1.2/ldml.dtd\"><ldml></ldml>");
    FileInputStream fis = new FileInputStream(filename);
    try {
      XMLReader xmlReader = CLDRFile.createXMLReader(true);
      Data myData = new Data(type);
      data.put(type, myData);

      MyDeclHandler me = new MyDeclHandler(myData);
      xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", me);
      InputSource is = new InputSource(fis);
      is.setSystemId(filename);
      //xmlReader.setContentHandler(me);
      //xmlReader.setErrorHandler(me);
      xmlReader.parse(is);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      fis.close();
    }
  }


  public static Map<R2<String, String>, R3<Set<String>, String, String>> getAttributeData(CLDRFile.DtdType type) {
    return data.get(type).mainAttributeData;
  }

  public static Relation<String, String> getContainingElementData(CLDRFile.DtdType type) {
    return data.get(type).mainElementData; 
  }
  
  public static Relation<String, String> getContainedElementData(CLDRFile.DtdType type) {
    return data.get(type).mainElementDataRev;
  }

  public static Relation<String,String> getElementToAttributes(CLDRFile.DtdType type) {
    return data.get(type).mainElementToAttributes;
  }

  static class MyDeclHandler implements DeclHandler {
    private static final boolean SHOW = false;
    private Data myData;

    Matcher idmatcher = Pattern.compile("[a-zA-Z0-9][-_a-zA-Z0-9]*").matcher("");

    public MyDeclHandler(Data indata) {
      myData = indata;
    }

    public void attributeDecl(String eName, String aName, String type, String mode, String value) throws SAXException {
      if (SHOW) System.out.println(myData.dtdType + "\tAttributeDecl\t" + eName + "\t" + aName + "\t" + type + "\t" + mode + "\t" + value);
      R2<String, String> key = Row.of(eName, aName);
      Set<String> typeSet = getIdentifiers(type);
      R3<Set<String>, String, String> value2 = Row.of(typeSet, mode, value);
      R3<Set<String>, String, String> oldValue = myData.mainAttributeData.get(key);
      if (oldValue != null && !oldValue.equals(value2)) {
        throw new IllegalArgumentException("Conflict in data: " + key + "\told: " + oldValue + "\tnew: " + value2);
      }
      myData.mainAttributeData.put(key, value2);
      myData.mainElementToAttributes.put(eName, aName);
    }

    private Set<String> getIdentifiers(String type) {
      Set<String> result = new TreeSet<String>();
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
      if (SHOW) System.out.println(myData.dtdType + "\tElement\t" + name + "\t" + model);
      Set<String> identifiers = getIdentifiers(model);
      identifiers.remove("special");
      identifiers.remove("alias");
      if (identifiers.size() == 0) {
        identifiers.add("EMPTY");
      }
      myData.mainElementData.putAll(name, identifiers);
      for (String identifier : identifiers) {
        myData.mainElementDataRev.put(identifier, name);
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
