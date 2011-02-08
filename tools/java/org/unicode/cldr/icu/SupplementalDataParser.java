// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.transform.TransformerException;

import org.unicode.cldr.icu.ICULog.Level;
import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceAlias;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceArray;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceInt;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceIntVector;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.icu.LDML2ICUConverter.LDMLServices;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.w3c.dom.Node;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;

public class SupplementalDataParser {
  private final ICULog log;
  private final LDMLServices service;

  private static final String commentForCurrencyMeta =
    "Currency metadata.  Unlike the \"Currencies\" element, this is\n" +
    "NOT true locale data.  It exists only in root.  The two\n" +
    "integers are the fraction digits for each currency, and the\n" +
    "rounding increment.  The fraction digits must be an integer\n" +
    "from 0..9.  If there is no rounding, the rounding incrementis \n" +
    "zero.  Otherwise the rounding increment is given in units of\n" +
    "10^(-fraction_digits).  The special tag \"DEFAULT\" gives the\n" +
    "meta data for all currencies not otherwise listed.";

  private static final String commentForCurrencyMap =
    "Map from ISO 3166 country codes to ISO 4217 currency codes\n" +
    "NOTE: This is not true locale data; it exists only in ROOT";

  private static final String commentForTelephoneCodeData =
    "Map from territory codes to ITU telephone codes.\n" +
    "NOTE: This is not true locale data; it exists only in ROOT";

  public SupplementalDataParser(ICULog log, LDMLServices service) {
    this.log = log;
    this.service = service;
  }

  public Resource parse(Node root, String file) {
    log.setStatus(file);
    ResourceTable table = new ResourceTable();
    Resource current = null;
    StringBuilder xpath = new StringBuilder();
    xpath.append("//");
    xpath.append(LDMLConstants.SUPPLEMENTAL_DATA);
    table.name = LDMLConstants.SUPPLEMENTAL_DATA;
    table.annotation = ResourceTable.NO_FALLBACK;
    int savedLength = xpath.length();
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      Resource res = null;
      if (name.equals(LDMLConstants.SUPPLEMENTAL_DATA)) {
        node = node.getFirstChild();
        continue;
      }

      if (name.equals(LDMLConstants.SPECIAL)) {
        // IGNORE SPECIALS FOR NOW
        // node = node.getFirstChild();
        // continue;
      } else if (name.equals(LDMLConstants.CURRENCY_DATA)) {
        res = parseCurrencyData(node, xpath);
      } else if (name.equals(LDMLConstants.TERRITORY_CONTAINMENT)) {
        //if (DEBUG)printXPathWarning(node, xpath);
        res = parseTerritoryContainment(node, xpath);
      } else if (name.equals(LDMLConstants.LANGUAGE_DATA)) {
        //if (DEBUG)printXPathWarning(node, xpath);
        res = parseLanguageData(node, xpath);
      } else if (name.equals(LDMLConstants.TERRITORY_DATA)) {
        //if (DEBUG)printXPathWarning(node, xpath);
        res = parseTerritoryData(node, xpath);
      } else if (name.equals(LDMLConstants.META_DATA)) {
        //Ignore this
        //if (DEBUG)printXPathWarning(node, xpath);
      } else if (name.equals(LDMLConstants.TERRITORY_INFO)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.CODE_MAPPINGS)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.REFERENCES)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.VERSION)) {
        res = parseCLDRVersion(node, xpath);
        //Ignore this
        //if (DEBUG)printXPathWarning(node, xpath);
      } else if (name.equals(LDMLConstants.GENERATION)) {
        //Ignore this
        //if (DEBUG)printXPathWarning(node, xpath);
      } else if (name.equals(LDMLConstants.CALENDAR_DATA)) {
        //Ignore this
        //res = parseCalendarData(node, xpath);
      } else if (name.equals(LDMLConstants.CALENDAR_PREFERENCE_DATA)) {
        res = parseCalendarPreferenceData(node, xpath);
      } else if (name.equals(LDMLConstants.TIMEZONE_DATA)) {
        res = parseTimeZoneData(node, xpath);
      } else if (name.equals(LDMLConstants.WEEK_DATA)) {
        res = parseWeekData(node, xpath);
      } else if (name.equals(LDMLConstants.CHARACTERS)) {
        //continue .. these are required for posix
      } else if (name.equals(LDMLConstants.MEASUREMENT_DATA)) {
        //res = parseMeasurementData(node, xpath);
        // if (DEBUG)printXPathWarning(node, getXPath(node, xpath));
      } else if (name.equals(LDMLConstants.LIKELY_SUBTAGS)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.PLURALS)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.NUMBERING_SYSTEMS)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.POSTAL_CODE_DATA)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.TELEPHONE_CODE_DATA)) {
        res = addTelephoneCodeData(); // uses SupplementalDataInfo, doesn't need node, xpath
      } else if (name.equals(LDMLConstants.BCP47_KEYWORD_MAPPINGS)) {
        res = parseBCP47MappingData(node, xpath);
      } else if (name.equals(LDMLConstants.LANGUAGE_MATCHING)) {
        res = parseLanguageMatch(node, xpath);
      } else if (name.equals(LDMLConstants.COVERAGE_LEVELS)) {
          //Ignore this for now
      } else if (name.equals(LDMLConstants.PARENT_LOCALES)) {
          //Ignore this for now
      } else if (name.equals(LDMLConstants.DAY_PERIOD_RULE_SET)) {
        //Ignore this for now
      } else {
        log.warning("Encountered unknown element " + LDML2ICUConverter.getXPath(node, xpath).toString());
        // System.exit(-1);
      }
      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
      xpath.delete(savedLength,xpath.length());
    }

    return table;
  }

  // Utilities


  private boolean isNodeNotConvertible(Node node, StringBuilder xpath) {
    // only deal with leaf nodes!
    // Here we assume that the CLDR files are normalized
    // and that the draft attributes are only on leaf nodes
    if (LDMLUtilities.areChildrenElementNodes(node)) {
      return false;
    }

    return !service.xpathListContains(xpath.toString());
  }

  private Resource parseAliasResource(Node node, StringBuilder xpath) {
    int saveLength = xpath.length();
    LDML2ICUConverter.getXPath(node, xpath);
    try {
      if (node != null && !isNodeNotConvertible(node, xpath)) {
        ResourceAlias alias = new ResourceAlias();
        xpath.setLength(saveLength);
        String val = LDMLUtilities.convertXPath2ICU(node, null, xpath);
        alias.val = val;
        alias.name = node.getParentNode().getNodeName();
        xpath.setLength(saveLength);
        return alias;
      }
    } catch(TransformerException ex) {
      log.error(
              "Could not compile XPATH for" +
              " source:  " + LDMLUtilities.getAttributeValue(node, LDMLConstants.SOURCE) +
              " path: " + LDMLUtilities.getAttributeValue(node, LDMLConstants.PATH) +
              " Node: " + node.getParentNode().getNodeName(), ex);
      System.exit(-1);
    }

    xpath.setLength(saveLength);
    // TODO update when XPATH is integrated into LDML
    return null;
  }

  /**
   * @deprecated
   */
  @Deprecated
  private void printXPathWarning(Node node, StringBuilder xpath) {
    int len = xpath.length();
    LDML2ICUConverter.getXPath(node, xpath);
    log.warning("Not producing resource for " + xpath.toString());
    xpath.setLength(len);
  }

  //
  // Currency
  //
  private Resource parseCurrencyData(Node root, StringBuilder xpath) {
    Resource currencyMeta = null;
    ResourceTable currencyMap = new ResourceTable();
    currencyMap.name = "CurrencyMap";
    currencyMap.comment = commentForCurrencyMap;
    currencyMap.noSort = true;
    Resource currentMap = null;

    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole collation node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      Resource res = null;
      //getXPath(node, xpath);
      if (name.equals(LDMLConstants.REGION)) {
        res = parseCurrencyRegion(node, xpath);
        if (res != null) {
          if (currentMap == null) {
            currencyMap.first = res;
            currentMap = res.end();
          } else {
            currentMap.next = res;
            currentMap = res.end();
          }
          res = null;
        }
      } else if (name.equals(LDMLConstants.FRACTIONS)) {
        currencyMeta = parseCurrencyFraction(node, xpath);
        currencyMeta.comment = commentForCurrencyMeta;
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    currencyMeta.next = currencyMap;

    return currencyMeta;
  }

  private Resource parseCurrencyRegion(Node root, StringBuilder xpath) {
    ResourceTable table = new ResourceTable();
    Resource current = null;

    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    table.name =  LDMLUtilities.getAttributeValue(root, LDMLConstants.ISO_3166);
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      Resource res = null;
      LDML2ICUConverter.getXPath(node, xpath);
      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath);
        res.name = name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = LDML2ICUConverter.getDefaultResource(node, xpath, name);
      } else if (name.equals(LDMLConstants.CURRENCY)) {
        //getXPath(node, xpath);
        if (isNodeNotConvertible(node, xpath)) {
          xpath.setLength(oldLength);
          continue;
        }
        ResourceTable curr = new ResourceTable();
        curr.name ="";
        ResourceString id = new ResourceString();
        id.name ="id";
        id.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.ISO_4217);

        String tender = LDMLUtilities.getAttributeValue(node, LDMLConstants.TENDER);

        ResourceIntVector fromRes = getSeconds(
                LDMLUtilities.getAttributeValue(node, LDMLConstants.FROM));
        ResourceIntVector toRes =  getSeconds(
                LDMLUtilities.getAttributeValue(node, LDMLConstants.TO));

        if (fromRes != null) {
          fromRes.name = LDMLConstants.FROM;
          curr.first = id;
          id.next = fromRes;
        }
        if (toRes != null) {
          toRes.name = LDMLConstants.TO;
          fromRes.next = toRes;
        }
        if (tender != null && tender.equals("false")) {
          res = null;
        } else {
          res = curr;
        }
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private Resource parseCurrencyFraction(Node root, StringBuilder xpath) {
    ResourceTable table = new ResourceTable();
    Resource current = null;

    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    table.name = "CurrencyMeta";
    table.noSort = true;
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      Resource res = null;
      LDML2ICUConverter.getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath);
        res.name = name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = LDML2ICUConverter.getDefaultResource(node, xpath, name);
      } else if (name.equals(LDMLConstants.INFO)) {
        ResourceIntVector vector = new ResourceIntVector();
        vector.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.ISO_4217);
        ResourceInt zero = new ResourceInt();
        ResourceInt one = new ResourceInt();
        zero.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.DIGITS);
        one.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.ROUNDING);
        vector.first = zero;
        zero.next = one;
        res = vector;
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        }else{
          current.next = res;
          current = res.end();
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private static int countHyphens(String str) {
    int ret = 0;
    for (int i = 0; i <str.length(); i++) {
      if (str.charAt(i) == '-') {
        ret++;
      }
    }

    return ret;
  }

  private long getMilliSeconds(String dateStr) {
    try {
      if (dateStr != null) {
        int count = countHyphens(dateStr);
        SimpleDateFormat format = new SimpleDateFormat();
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = null;
        if (count == 2) {
          format.applyPattern("yyyy-mm-dd");
          date = format.parse(dateStr);
        } else if (count == 1) {
          format.applyPattern("yyyy-mm");
          date = format.parse(dateStr);
        } else {
          format.applyPattern("yyyy");
          date = format.parse(dateStr);
        }
        return date.getTime();
      }
    } catch(ParseException ex) {
      log.error("Could not parse date: " + dateStr, ex);
      System.exit(-1);
    }
    return -1;
  }

  private ResourceIntVector getSeconds(String dateStr) {
    long millis = getMilliSeconds(dateStr);
    if (millis == -1) {
      return null;
    }

    int top =(int)((millis & 0xFFFFFFFF00000000L)>>>32);
    int bottom = (int)((millis & 0x00000000FFFFFFFFL));
    ResourceIntVector vector = new ResourceIntVector();
    ResourceInt int1 = new ResourceInt();
    ResourceInt int2 = new ResourceInt();
    int1.val = Integer.toString(top);
    int2.val = Integer.toString(bottom);
    vector.first = int1;
    int1.next = int2;
    vector.smallComment = dateStr; // + " " + millis + "L";

    if (log.willOutput(Level.DEBUG)) {
      top = Integer.parseInt(int1.val);
      bottom = Integer.parseInt(int2.val);
      long bot = 0xffffffffL & bottom;
      long full = ((long)(top) << 32);
      full += bot;
      if (full != millis) {
        log.debug("Did not get the value back.");
      }
    }

    return vector;
  }

  //
  // TerritoryContainment
  //
  private Resource parseTerritoryContainment(Node root, StringBuilder xpath) {
    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();
    Resource current = null;
    Resource res = null;
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ResourceTable table = new ResourceTable();
    table.name = LDMLConstants.TERRITORY_CONTAINMENT;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      LDML2ICUConverter.getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }
      if (name.equals(LDMLConstants.GROUP)) {
        String cnt = LDMLUtilities.getAttributeValue(node, LDMLConstants.CONTAINS);
        String value = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        res = LDML2ICUConverter.getResourceArray(cnt, value);
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }
      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }


  private Resource parseLanguageMatch(Node root, StringBuilder xpath) {
    //  <languageMatching>
    //    <languageMatches type="written">

    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();

    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }
    Hashtable<String, Resource> hash = new Hashtable<String, Resource>();

    ResourceTable table = new ResourceTable();
    table.name = LDMLConstants.LANGUAGE_MATCHING;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      LDML2ICUConverter.getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }
      int oldLength2 = xpath.length();

      if (name.equals(LDMLConstants.LANGUAGE_MATCHES)) {
        String key = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        if (key == null) {
          log.error("<language > element does not have type attribute ! " + xpath.toString());
          return null;
        }

        ResourceArray subtable = new ResourceArray();
        subtable.name = key;
        LDML2ICUConverter.addToTable(table, subtable);

        //      <languageMatch desired="no" supported="nb" percent="100"/>
        //      <languageMatch desired="nn" supported="nb" percent="96"/>

        for (Node node2 = node.getFirstChild(); node2 != null; node2 = node2.getNextSibling()) {
          if (node2.getNodeType()!= Node.ELEMENT_NODE) {
            continue;
          }
          String name2 = node2.getNodeName();
          LDML2ICUConverter.getXPath(node2, xpath);
          if (isNodeNotConvertible(node2, xpath)) {
            xpath.setLength(oldLength2);
            continue;
          }

          if (name2.equals(LDMLConstants.LANGUAGE_MATCH)) {
            String desired = LDMLUtilities.getAttributeValue(node2, "desired");
            String supported = LDMLUtilities.getAttributeValue(node2, "supported");
            String percent = LDMLUtilities.getAttributeValue(node2, "percent");
            if (desired == null || supported == null || percent == null ) {
              log.error(LDMLConstants.LANGUAGE_MATCH + " does not have type attributes ! " + xpath.toString());
              return null;
            }

            ResourceArray subsubsubtable = new ResourceArray();
            //subsubsubtable.name = LDMLConstants.LANGUAGE_MATCH;
            LDML2ICUConverter.addToTable(subtable, subsubsubtable);

            ResourceString res = new ResourceString();
            //res.name = "desired";
            res.val = desired;
            LDML2ICUConverter.addToTable(subsubsubtable, res);
            res = new ResourceString();
            //res.name = "supported";
            res.val = supported;
            LDML2ICUConverter.addToTable(subsubsubtable, res);
            res = new ResourceString();
            //res.name = "percent";
            res.val = percent;
            LDML2ICUConverter.addToTable(subsubsubtable, res);
          }
        }
      }
    }
    if (table.first != null) {
      return table;
    }

    return null;
  }

  //
  // LanguageData
  //
  private Resource parseLanguageData(Node root, StringBuilder xpath) {
    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();

    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }
    Hashtable<String, Resource> hash = new Hashtable<String, Resource>();

    ResourceTable table = new ResourceTable();
    table.name = LDMLConstants.LANGUAGE_DATA;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      LDML2ICUConverter.getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }
      if (name.equals(LDMLConstants.LANGUAGE)) {
        String key = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        if (key == null) {
          log.error("<language > element does not have type attribute ! " + xpath.toString());
          return null;
        }

        String scs = LDMLUtilities.getAttributeValue(node, LDMLConstants.SCRIPTS);
        String trs = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORIES);
        String mpt = LDMLUtilities.getAttributeValue(node, LDMLConstants.MPT);

        String alt = LDMLUtilities.getAttributeValue(node, LDMLConstants.ALT);
        if (alt == null) {
          alt = LDMLConstants.PRIMARY;
        }
        ResourceTable tbl = new ResourceTable();
        tbl.name = alt;
        ResourceArray scripts = LDML2ICUConverter.getResourceArray(scs, LDMLConstants.SCRIPTS);
        ResourceArray terrs = LDML2ICUConverter.getResourceArray(trs, LDMLConstants.TERRITORIES);
        ResourceArray mpts = LDML2ICUConverter.getResourceArray(mpt, LDMLConstants.MPT);
        if (scripts != null) {
          tbl.first = scripts;
        }
        if (terrs != null) {
          if (tbl.first != null) {
            tbl.first.end().next = terrs;
          } else {
            tbl.first = terrs;
          }
        }
        if (mpts != null) {
          if (tbl.first != null) {
            tbl.first.end().next = mpts;
          } else {
            tbl.first = terrs;
          }
        }
        // now find in the Hashtable
        ResourceTable main = (ResourceTable) hash.get(key);
        if (main == null) {
          main = new ResourceTable();
          main.name = key;
          hash.put(key, main);
        }
        if (main.first != null) {
          main.first.end().next = tbl;
        } else {
          main.first = tbl;
        }
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }
      xpath.setLength(oldLength);
    }

    Enumeration<String> iter = hash.keys();
    Resource current = null, res = null;
    while (iter.hasMoreElements()) {
      String key = iter.nextElement();
      res = hash.get(key);
      if (current == null) {
        current = table.first = res;
      } else {
        current.next = res;
        current = current.next;
      }
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  //
  // TerritoryData
  //
  private Resource parseTerritoryData(Node root, StringBuilder xpath) {
    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();
    Resource current = null;
    Resource res = null;

    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ResourceTable table = new ResourceTable();
    table.name = LDMLConstants.TERRITORY_DATA;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      LDML2ICUConverter.getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }

      if (name.equals(LDMLConstants.TERRITORY)) {
        ResourceString str = new ResourceString();
        String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        if (type == null) {
          log.error("Could not get type attribute for xpath: " + xpath.toString());
        }
        str.name = type;
        str.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.MPTZ);
        res = str;
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }
      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  //
  // CalendarPreferenceData
  //
  private Resource parseCalendarPreferenceData(Node root, StringBuilder xpath) {
    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();
    Resource current = null;
    Resource res = null;

    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ResourceTable table = new ResourceTable();
    table.name = LDMLConstants.CALENDAR_PREFERENCE_DATA;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      LDML2ICUConverter.getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }
      if (!name.equals(LDMLConstants.CALENDAR_PREFERENCE)) {
        log.error("Encountered unknown " + xpath.toString());
        System.exit(-1);
      }
      String tmp = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORIES);
      String order = LDMLUtilities.getAttributeValue(node, LDMLConstants.ORDERING);

      // expand territories and create separated ordering array for each
      String[] territories = tmp.split("\\s+");
      for (int i = 0; i < territories.length; i++) {
        res = LDML2ICUConverter.getResourceArray(order, territories[i]);
        if (current == null) {
          table.first = res;
        } else {
          current.next = res;
        }
        current = res.end();
      }
      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }
  
  private Resource parseWeekData(Node root, StringBuilder xpath) {
      Resource current = null;
      int savedLength = xpath.length();
      LDML2ICUConverter.getXPath(root, xpath);

      // if the whole node is marked draft then
      // don't write anything
      if (isNodeNotConvertible(root, xpath)) {
        xpath.setLength(savedLength);
        return null;
      }
      
      ResourceTable table = new ResourceTable();
      table.name = LDMLConstants.WEEK_DATA;
      Set<String> useTerritories = new TreeSet<String>();
      Map<String,Integer> minDaysMap = new HashMap<String,Integer>();
      Map<String,Integer> firstDayOfWeekMap = new HashMap<String,Integer>();
      Map<String,Integer[]> weekendStartMap = new HashMap<String,Integer[]>();
      Map<String,Integer[]> weekendEndMap = new HashMap<String,Integer[]>();
      for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
          if (node.getNodeType() != Node.ELEMENT_NODE ) {
            continue;
          }
          if (LDMLUtilities.getAttributeValue(node,LDMLConstants.ALT)!= null ) {
              continue; // Only use non-alt values from supplemental
          }
          String name = node.getNodeName();
          if ( name.equals(LDMLConstants.MINDAYS) || name.equals(LDMLConstants.FIRSTDAY)||
               name.equals(LDMLConstants.WENDSTART) || name.equals(LDMLConstants.WENDEND)) {
              String territoryString = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORIES);
              String[] territories = territoryString.split(" ");
              for ( int i = 0 ; i < territories.length ; i++ ) {
                  if (name.equals(LDMLConstants.MINDAYS)) {
                      String countString = LDMLUtilities.getAttributeValue(node, LDMLConstants.COUNT);
                      Integer countVal = new Integer(countString);
                      minDaysMap.put(territories[i], countVal);
                  }
                  if (name.equals(LDMLConstants.FIRSTDAY)) {
                      String dayString = LDML2ICUConverter.getDayNumberAsString(LDMLUtilities.getAttributeValue(node, LDMLConstants.DAY));
                      Integer dayVal = new Integer(dayString);
                      firstDayOfWeekMap.put(territories[i], dayVal);
                  }
                  if (name.equals(LDMLConstants.WENDSTART)) {
                      Integer[] weekendStart = new Integer[2];
                      String dayString = LDML2ICUConverter.getDayNumberAsString(LDMLUtilities.getAttributeValue(node, LDMLConstants.DAY));
                      String timeString = LDMLUtilities.getAttributeValue(node, LDMLConstants.TIME);
                      weekendStart[0] = Integer.valueOf(dayString);
                      weekendStart[1] = Integer.valueOf(LDML2ICUConverter.getMillis(timeString == null ? "00:00" : timeString));
                      weekendStartMap.put(territories[i], weekendStart);
                  }
                  if (name.equals(LDMLConstants.WENDEND)) {
                      Integer[] weekendEnd = new Integer[2];
                      String dayString = LDML2ICUConverter.getDayNumberAsString(LDMLUtilities.getAttributeValue(node, LDMLConstants.DAY));
                      String timeString = LDMLUtilities.getAttributeValue(node, LDMLConstants.TIME);
                      weekendEnd[0] = Integer.valueOf(dayString);
                      weekendEnd[1] = Integer.valueOf(LDML2ICUConverter.getMillis(timeString == null ? "24:00" : timeString));
                      weekendEndMap.put(territories[i], weekendEnd);
                  }
                  useTerritories.add(territories[i]);
              }
          }
      }
      
      Iterator<String> it = useTerritories.iterator();
      while (it.hasNext()) {
          ResourceIntVector weekData = new ResourceIntVector();
          String country = it.next();
          weekData.name = country;
          ResourceInt[] weekDataInfo = new ResourceInt[6];
          Integer firstDayOfWeek = firstDayOfWeekMap.get(country);
          if ( firstDayOfWeek == null ) {
              firstDayOfWeek = firstDayOfWeekMap.get("001");
          }
          if ( firstDayOfWeek == null ) {
              log.error("Unable to find firstDayOfWeek element for weekData");
              System.exit(-1);
          }
          
          Integer minDays = minDaysMap.get(country);
          if ( minDays == null ) {
              minDays = minDaysMap.get("001");
          }
          if ( minDays == null ) {
              log.error("Unable to find minDays element for weekData");
              System.exit(-1);
          }
          
          Integer [] weekendStart = weekendStartMap.get(country);
          if ( weekendStart == null ) {
              weekendStart = weekendStartMap.get("001");
          }
          if ( weekendStart == null ) {
              log.error("Unable to find weekendStart element for weekData");
              System.exit(-1);
          }
         
          Integer [] weekendEnd = weekendEndMap.get(country);
          if ( weekendEnd == null ) {
              weekendEnd = weekendEndMap.get("001");
          }
          if ( weekendEnd == null ) {
              log.error("Unable to find weekendEnd element for weekData");
              System.exit(-1);
          }
          
          ResourceInt int1 = new ResourceInt();
          int1.val = firstDayOfWeek.toString();
          weekData.appendContents(int1);
          
          ResourceInt int2 = new ResourceInt();
          int2.val = minDays.toString();
          weekData.appendContents(int2);
          
          ResourceInt int3 = new ResourceInt();
          int3.val = weekendStart[0].toString();
          weekData.appendContents(int3);
          
          ResourceInt int4 = new ResourceInt();
          int4.val = weekendStart[1].toString();
          weekData.appendContents(int4);

          ResourceInt int5 = new ResourceInt();
          int5.val = weekendEnd[0].toString();
          weekData.appendContents(int5);

          ResourceInt int6 = new ResourceInt();
          int6.val = weekendEnd[1].toString();
          weekData.appendContents(int6);
         
//          weekDataInfo[0].val = firstDayOfWeek.toString();
//          weekDataInfo[1].val = minDays.toString();
//          weekDataInfo[2].val = weekendStart[0].toString();
//          weekDataInfo[3].val = weekendStart[1].toString();
//          weekDataInfo[4].val = weekendEnd[0].toString();
//          weekDataInfo[5].val = weekendEnd[1].toString();

//          weekData.first = weekDataInfo[0];
//          for ( int i = 0 ; i < weekDataInfo.length - 1 ; i++ ) {
//              weekDataInfo[i].next = weekDataInfo[i+1];
//          }
          
          if (table.first == null) {
              table.first = weekData;
          } else {
              current.next = weekData;
          }
          current = weekData;
      }
      
      if (table.first != null) {
          return table;
      }
      return null;
  }
  
  //
  // TimeZoneData
  //
  private Resource parseTimeZoneData(Node root, StringBuilder xpath) {
    Resource current = null;
    Resource first = null;
    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // dont write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ResourceTable mapZones = new ResourceTable();
    mapZones.name = LDMLConstants.MAP_TIMEZONES;
    for(Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      Resource res = null;

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath);
        res.name = name;
        return res;
      } else if (name.equals(LDMLConstants.DEFAULT)) {
        res = LDML2ICUConverter.getDefaultResource(node, xpath, name);
      } else if (name.equals(LDMLConstants.MAP_TIMEZONES)) {

        //if (DEBUG)printXPathWarning(node, xpath);
        res = parseMapTimezones(node, xpath);
        if (res != null) {
          if (mapZones.first == null) {
            mapZones.first = res;
          } else {
            mapZones.first.end().next = res;
          }
        }
        res = null;
      } else if (name.equals(LDMLConstants.ZONE_FORMATTING)) {
        res = parseZoneFormatting(node, xpath);
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    if (mapZones.first != null) {
      if (current == null) {
        first = current = mapZones;
      }else{
        current.next = mapZones;
        current = mapZones.end();
      }
    }

    xpath.delete(savedLength, xpath.length());
    if (first != null) {
      return first;
    }

    return null;
  }

  private Resource parseMapTimezones(Node root, StringBuilder xpath) {
    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();
    Resource current = null;
    Resource res = null;

    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ResourceTable table = new ResourceTable();
    table.name = LDMLUtilities.getAttributeValue(root, LDMLConstants.TYPE);

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();

      LDML2ICUConverter.getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }

      if (name.equals(LDMLConstants.MAP_ZONE)) {
        String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        String other = LDMLUtilities.getAttributeValue(node, LDMLConstants.OTHER);
        String territory = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORY);
        String result;
        ResourceString str = new ResourceString();
        if (territory != null && territory.length() > 0) {
          result = "meta:" + other + "_" + territory;
          str.name = "\"" + result + "\"";
          str.val = type;
        } else {
          result = type;
          str.name = "\"" + other + "\"";
          str.val = result;
        }
        res = str;
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
      xpath.setLength(oldLength);
    }

    xpath.setLength(savedLength);
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private Resource parseZoneFormatting(Node root, StringBuilder xpath) {
    ResourceTable table = new ResourceTable();
    Resource current = null;

    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();

    //if the whole node is marked draft then
    //dont write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    table.name = "zoneFormatting";
    table.noSort = true;
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      Resource res = null;
      LDML2ICUConverter.getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath);
        res.name = name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = LDML2ICUConverter.getDefaultResource(node, xpath, name);
      } else if (name.equals(LDMLConstants.ZONE_ITEM)) {
        ResourceTable zi = new ResourceTable();
        zi.name = "\"" + LDMLUtilities.getAttributeValue(
                node, LDMLConstants.TYPE).replaceAll("/", ":") + "\"";

        String canonical = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        ResourceString canon = new ResourceString();
        canon.name = LDMLConstants.CANONICAL;
        canon.val = canonical;
        zi.first = canon;

        String territory = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORY);
        ResourceString ter = new ResourceString();
        ter.name = LDMLConstants.TERRITORY;
        ter.val = territory;
        canon.next = ter;

        String aliases = LDMLUtilities.getAttributeValue(node, LDMLConstants.ALIASES);
        String icu_aliases = LDML2ICUConverter.getICUAlias(LDMLUtilities.getAttributeValue(node,LDMLConstants.TYPE));
        String all_aliases = aliases;
        if (icu_aliases != null) {
          if (aliases == null) {
            all_aliases = icu_aliases;
          } else {
            all_aliases = aliases + " " + icu_aliases;
          }
        }

        if (all_aliases != null) {
          String[] arr = all_aliases.split("\\s+");
          ResourceArray als = new ResourceArray();
          als.name = LDMLConstants.ALIASES;
          Resource cur = null;
          for(int i = 0; i <arr.length; i++) {
            ResourceString str = new ResourceString();
            str.val = arr[i];
            if (cur == null) {
              als.first = cur = str;
            }else{
              cur.next = str;
              cur = cur.next;
            }
          }
          ter.next = als;
        }
        res = zi;
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }
    xpath.delete(savedLength, xpath.length());

    // Now add the multi-zone list to the table
    String multizone = LDMLUtilities.getAttributeValue(root,LDMLConstants.MULTIZONE);
    ResourceArray mz;
    mz = LDML2ICUConverter.getResourceArray(multizone, LDMLConstants.MULTIZONE);
    if (current == null) {
      table.first = mz;
      current = mz.end();
    } else {
      current.next = mz;
      current = mz.end();
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  //
  // CLDRVersion
  //
  private Resource parseCLDRVersion(Node root, StringBuilder xpath) {
    ResourceString str = new ResourceString();
    str.name = LDMLConstants.CLDR_VERSION;
    str.val = LDMLUtilities.getAttributeValue(root, LDMLConstants.CLDR_VERSION);
    return str;
  }

  //
  // BCP47MappingData
  //
  private Resource parseBCP47MappingData(Node root, StringBuilder xpath) {
    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ResourceTable bcp47KeywordMappings = new ResourceTable();
    bcp47KeywordMappings.name = LDMLConstants.BCP47_KEYWORD_MAPPINGS;
    Resource current = null;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      Resource res = null;

      if (name.equals(LDMLConstants.MAP_KEYS)) {
        res = parseMapKeys(node, xpath);
      } else if (name.equals(LDMLConstants.MAP_TYPES)) {
        res = parseMapTypes(node, xpath);
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          bcp47KeywordMappings.first = res;
        }else{
          current.next = res;
        }
        current = res;
      }

      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (bcp47KeywordMappings.first != null) {
      return bcp47KeywordMappings;
    }

    return null;
  }

  private Resource parseMapKeys(Node root, StringBuilder xpath) {
    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ResourceTable mapKeys = new ResourceTable();
    mapKeys.name = "key";
    Resource current = null;

    for(Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      Resource res = null;
      if (name.equals(LDMLConstants.KEY_MAP)) {
        ResourceString keyMap = new ResourceString();
        keyMap.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE)
        .toLowerCase(Locale.ENGLISH);
        keyMap.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.BCP47)
        .toLowerCase(Locale.ENGLISH);
        res = keyMap;
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          mapKeys.first = res;
        }else{
          current.next = res;
        }
        current = res;
      }

      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (mapKeys.first != null) {
      return mapKeys;
    }

    return null;
  }

  private Resource parseMapTypes(Node root, StringBuilder xpath) {
    int savedLength = xpath.length();
    LDML2ICUConverter.getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ResourceTable mapTypes = new ResourceTable();
    String ldmlKey = LDMLUtilities.getAttributeValue(root, LDMLConstants.TYPE)
    .toLowerCase(Locale.ENGLISH);
    mapTypes.name = ldmlKey;
    boolean isTimeZone = ldmlKey.equals("timezone");
    Resource current = null;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      Resource res = null;

      if (name.equals(LDMLConstants.TYPE_MAP)) {
        ResourceString typeMap = new ResourceString();
        if (isTimeZone) {
          typeMap.name = "\""
            + LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE).replaceAll("/", ":")
            .toLowerCase(Locale.ENGLISH)
            + "\"";
        } else {
          typeMap.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE)
          .toLowerCase(Locale.ENGLISH);
        }
        typeMap.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.BCP47)
        .toLowerCase(Locale.ENGLISH);
        res = typeMap;
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          mapTypes.first = res;
        }else{
          current.next = res;
        }
        current = res;
      }

      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());

    if (mapTypes.first != null) {
      return mapTypes;
    }

    return null;
  }

  // Quick & dirty bare-bones version of this for now just to start writing something out.
  // Doesn't write from or to data (don't have any yet), and just writes alt as a comment.
  // Add more once we figure out what ICU needs. -pedberg
  private Resource addTelephoneCodeData() {
    // uses SupplementalDataInfo, doesn't need node, xpath
    ResourceTable table = new ResourceTable();
    table.name = LDMLConstants.TELEPHONE_CODE_DATA;
    table.comment = commentForTelephoneCodeData;
    Resource currTerr = null;
    ResourceTable terrTable = null;

    SupplementalDataInfo suppInfo = service.getSupplementalDataInfo();
    for (String terr: suppInfo.getTerritoriesForTelephoneCodeInfo()) {
      terrTable = new ResourceTable();
      terrTable.name = terr;
      Resource currCode = null;
      for (SupplementalDataInfo.TelephoneCodeInfo telephoneCodeInfo :
        suppInfo.getTelephoneCodeInfoForTerritory(terr)) {
        ResourceTable codeData = new ResourceTable();
        codeData.name = "";
        ResourceString codeEntry = new ResourceString();
        codeEntry.name = "code";
        codeEntry.val = telephoneCodeInfo.getCode();
        codeData.first = codeEntry;
        String alt = telephoneCodeInfo.getAlt();
        if (alt.length() > 0) {
          ResourceString altEntry = new ResourceString();
          altEntry.name = "alt";
          altEntry.val = alt;
          codeEntry.next = altEntry;
        }
        if (currCode == null) {
          terrTable.first = currCode = codeData;
        } else {
          currCode.next = codeData;
          currCode = currCode.next;
        }
      }

      if (currTerr == null) {
        table.first = terrTable;
        currTerr = terrTable.end();
      } else {
        currTerr.next = terrTable;
        currTerr = terrTable.end();
      }
    }

    return table;
  }
}
