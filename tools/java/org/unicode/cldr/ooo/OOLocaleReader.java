/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

import org.unicode.cldr.util.LDMLUtilities;
import java.io.*;
import java.util.*;
import org.w3c.dom.*;

/**
 *
 * class reads all OO XML data into memory
 */
public class OOLocaleReader
{
    
    private String m_filename = null;
    private Document m_doc = null;
    private DOMWrapper m_domWrapper = null;
    
    //### class members to hold the data ####
    
    // attributes of locale element
    public Hashtable m_LocaleInfo = new Hashtable();  //key = atrib name, value = attrib value
    
    //refs   Hashtable of Strings/or Hashtables,   outer key = OOConstant, data = fully resolved reference
    private Hashtable m_Refs = new Hashtable();
    
    public Hashtable m_Refs2 = new Hashtable();  //key = OOConstant, date is contentts of ref attrib (i.e. not fully resolved)
    
    // LC_INFO
    public static String m_LangId = null;
    public String m_Language_DefaultName = null;
    public static String m_Country_CountryID = null;
    public String m_Country_DefaultName = null;
    public String m_PlatformID = null;
    public String m_Varient = null;
    public String m_version = null;
    public String m_allowUpdateFromCLDR = null;
    public String m_versionDTD = null;
    
    //LC_CTYPE
    public Hashtable m_Separators = new Hashtable();  //key=element name, value= element value (String)
    public Hashtable m_Markers = new Hashtable();   //key=element name, value= element value (String)
    public String m_TimeAM = null;
    public String m_TimePM = null;
    public String m_MeasurementSystem = null;
    
    //LC_FORMAT
    public Vector m_LCFormat = null;   //Vector of Hashtables (key=attrib, value=attrib value), Vector will only have 1 entry
    public Hashtable m_FormatElements = null;   //Hashtable of Hashtables (outerKey=msgId, innerKey=aatrib, value=attrib value) 
    public Hashtable m_FormatCodes = null;  //key=parent's msgId, value = element value
    public Hashtable m_FormatDefaultNames = null;  //key=parent's msgId, value = element value
    public String m_ReplaceFrom = null;
    public String m_ReplaceTo = null;
    
    //LC_COLATION
    public Vector m_Collators = null;   //Vector of Hashtables (key=attr name, value=attr value
    public Vector m_CollationOptions = null;  //Vector of Strings
    
    //LC_SEARCH
    public Vector m_SearchOptions = null;   //simple Vector of Strings
    
    //LC_INDEX
    public Vector m_IndexKeys = null;  //Vector of Hashtables (key=attr name, value=attr value)
    public Hashtable m_IndexKeysData = null;  //key = element unoid, value=element data
    public Vector m_UnicodeScript = null;  //simple Vector of Strings, making assumption that vector maintains order
    public Vector m_FollowPageWord = null;  //simple Vector of Strings, making assumption that vector maintains order
    
    //LC_CALENDAR
    //Vector of Hashtables (key=attr name , value = attr value)
    public Vector m_Calendars = null;
    
    //Hashtable of Hashtables <key1 <key2, value>> where key1=calendar type , key2=month type (MonthID)
    public Hashtable m_AbbrMonths = null;
    public Hashtable m_WideMonths = null;
    
    //Hashtable of Hashtables <key1 <key2, value>> where key1=calendar type , key2=day type (DayID)
    public Hashtable m_AbbrDays = null;
    public Hashtable m_WideDays = null;
    
    //Hashtable of Hashtables <key1 <key2, value>> where key1=calendar type , key2=era type (EraID)
    public Hashtable m_AbbrEras = null;
    public Hashtable m_WideEras = null;
    
    //Hashtable key = calendar type, value = element value
    public Hashtable m_StartDayOfWeek = null;
    public Hashtable m_MinDaysInFirstweek = null;
    
    //LC_CURRENCY -  in OO.o can no longer use code as unique identifier
    // inner holds data in following order : CurrencyID,CurrencySymbol,BankSymbol,CurrencyName,DecimalPlaces,default, usedInCompatibleFormatCode, legacyOnly (if defined)
    public Vector m_CurrencyData = new Vector ();   //Vector of vectors
      
    //LC_TRANSLITERATION
    public Vector m_Transliterations = null;  //Vector of Hashtables (key=attr name, value=attr value)
    
    //LC_MISC
    public Hashtable m_ForbiddenChars = new Hashtable(); //key = element name, value = element value holds m_ForbiddenBeginChar + m_ForbiddenEndChar
    public String m_ForbiddenBeginChar = null;  //used bu OOComparator
    public String m_ForbiddenEndChar = null;   //used bu OOComparator
    public Hashtable m_ReservedWords = new Hashtable();
    
    //LC_NumberingLevel  - order is important !!!!!
    public Vector m_NumberingLevels = null;  // Vector of hashtables (key=attr name, value=attr value
    
    //LC_OutlineNumberingLevel-  order is important !!!!!
    public Vector m_OutlineNumberingLevels = null;   // Vector of hashtables (key=attr name, value=attr value
    // need to be grouped separately in groups of 5
    
    
    public OOLocaleReader(String filename)
    {
        m_filename = filename;
    }
    
    public boolean readDocument(boolean bResolveRefs)
    {
        if (m_filename == null)
            return false;
        boolean bRc = false;
        
  /*      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);
        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(filename));
   
   
            Element el = doc.getDocumentElement();  //should get base element
        }
        catch (ParserConfigurationException pce)
        {
            pce.printStackTrace();
        }
        catch (SAXException se)
        {
            se.printStackTrace();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
   */
        
        m_doc = LDMLUtilities.parse(m_filename, true);
        m_domWrapper = new DOMWrapper(m_doc);
        
        //get the locale attributes
        m_version = m_domWrapper.getAttributeFromElement(OOConstants.LOCALE, OOConstants.VERSION);
        if (m_version != null) m_LocaleInfo.put(OOConstants.VERSION, m_version);
        m_allowUpdateFromCLDR = m_domWrapper.getAttributeFromElement(OOConstants.LOCALE, OOConstants.ALLOW_UPDATE_FROM_CLDR);
        if (m_allowUpdateFromCLDR != null) m_LocaleInfo.put( OOConstants.ALLOW_UPDATE_FROM_CLDR, m_allowUpdateFromCLDR);
        m_versionDTD = m_domWrapper.getAttributeFromElement(OOConstants.LOCALE, OOConstants.VERSION_DTD);
        if (m_versionDTD != null) m_LocaleInfo.put(OOConstants.VERSION_DTD, m_versionDTD);
        
        //####### LC_INFO sub-elements ########
        m_LangId = m_domWrapper.getTextFromElement(OOConstants.LANGUAGE, OOConstants.LANG_ID);
        m_Language_DefaultName = m_domWrapper.getTextFromElement(OOConstants.LANGUAGE, OOConstants.DEFAULT_NAME);
        
        m_Country_CountryID = m_domWrapper.getTextFromElement(OOConstants.COUNTRY, OOConstants.COUNTRY_ID);
        m_Country_DefaultName = m_domWrapper.getTextFromElement(OOConstants.COUNTRY, OOConstants.DEFAULT_NAME);
        
        m_PlatformID = m_domWrapper.getTextFromElement(OOConstants.PLATFORM, OOConstants.PLATFORM_ID);
        
        //Varient always empty in OO XML so we ignore it
        m_Varient = m_domWrapper.getTextFromElement(OOConstants.LC_INFO, OOConstants.VARIENT);
        
        String locale = "unknown";
        if (m_LangId != null) locale =  m_LangId;
        if (m_Country_CountryID != null) locale = locale + "_" + m_Country_CountryID;
        Logging.Log1("\nLocale : " + locale);
        
        //read refs here so logging looks right
        readRefs(bResolveRefs);
        
        //#######  LC_CTYPE sub-elements   #########
        checkForRef(OOConstants.LC_CTYPE);
        String dateSeparator = m_domWrapper.getTextFromElement(OOConstants.SEPARATORS, OOConstants.DATE_SEPARATOR);
        if (dateSeparator != null) m_Separators.put(OOConstants.DATE_SEPARATOR, dateSeparator);
        
        String thousandSeparator = m_domWrapper.getTextFromElement(OOConstants.SEPARATORS, OOConstants.THOUSAND_SEPARATOR);
        if (thousandSeparator != null) m_Separators.put(OOConstants.THOUSAND_SEPARATOR, thousandSeparator);
        
        String decimalSeparator = m_domWrapper.getTextFromElement(OOConstants.SEPARATORS, OOConstants.DECIMAL_SEPARATOR);
        if (decimalSeparator != null) m_Separators.put(OOConstants.DECIMAL_SEPARATOR, decimalSeparator);
        
        String timeSeparator = m_domWrapper.getTextFromElement(OOConstants.SEPARATORS, OOConstants.TIME_SEPARATOR);
        if (timeSeparator != null) m_Separators.put(OOConstants.TIME_SEPARATOR, timeSeparator);
        
        String time100SecSeparator = m_domWrapper.getTextFromElement(OOConstants.SEPARATORS, OOConstants.TIME_100SEC_SEPARATOR);
        if (time100SecSeparator != null) m_Separators.put(OOConstants.TIME_100SEC_SEPARATOR, time100SecSeparator);
        
        String listSeparator = m_domWrapper.getTextFromElement(OOConstants.SEPARATORS, OOConstants.LIST_SEPARATOR);
        if (listSeparator != null) m_Separators.put(OOConstants.LIST_SEPARATOR, listSeparator);
        
        String longDateDayOfWeekSeparator = m_domWrapper.getTextFromElement(OOConstants.SEPARATORS, OOConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR);
        if (longDateDayOfWeekSeparator != null) m_Separators.put(OOConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR, longDateDayOfWeekSeparator);
        
        String longDateDaySeparator = m_domWrapper.getTextFromElement(OOConstants.SEPARATORS, OOConstants.LONG_DATE_DAY_SEPARATOR);
        if (longDateDaySeparator != null) m_Separators.put(OOConstants.LONG_DATE_DAY_SEPARATOR, longDateDaySeparator);
        
        String longDateMnothSeparator = m_domWrapper.getTextFromElement(OOConstants.SEPARATORS, OOConstants.LONG_DATE_MONTH_SEPARATOR);
        if (longDateMnothSeparator != null) m_Separators.put(OOConstants.LONG_DATE_MONTH_SEPARATOR, longDateMnothSeparator);
        
        String longDateYearSeparator = m_domWrapper.getTextFromElement(OOConstants.SEPARATORS, OOConstants.LONG_DATE_YEAR_SEPARATOR);
        if (longDateYearSeparator != null) m_Separators.put(OOConstants.LONG_DATE_YEAR_SEPARATOR, longDateYearSeparator);
        
        String quotationStart = m_domWrapper.getTextFromElement(OOConstants.MARKERS, OOConstants.QUOTATION_START);
        if (quotationStart!=null) m_Markers.put(OOConstants.QUOTATION_START, quotationStart);
        String quotationEnd = m_domWrapper.getTextFromElement(OOConstants.MARKERS, OOConstants.QUOTATION_END);
        if (quotationEnd!=null) m_Markers.put(OOConstants.QUOTATION_END, quotationEnd);
        String doubleQuotationStart = m_domWrapper.getTextFromElement(OOConstants.MARKERS, OOConstants.DOUBLE_QUOTATION_START);
        if (doubleQuotationStart!=null) m_Markers.put(OOConstants.DOUBLE_QUOTATION_START, doubleQuotationStart);
        String doubleQuotationEnd = m_domWrapper.getTextFromElement(OOConstants.MARKERS, OOConstants.DOUBLE_QUOTATION_END);
        if (doubleQuotationEnd!=null) m_Markers.put(OOConstants.DOUBLE_QUOTATION_END, doubleQuotationEnd);
        
        m_TimeAM = m_domWrapper.getTextFromElement(OOConstants.LC_CTYPE, OOConstants.TIME_AM);
        m_TimePM = m_domWrapper.getTextFromElement(OOConstants.LC_CTYPE, OOConstants.TIME_PM);
        
        m_MeasurementSystem = m_domWrapper.getTextFromElement(OOConstants.LC_CTYPE, OOConstants.MEASUREMENT_SYSTEM);
        m_domWrapper.resetDoc(m_doc);
        
        //#######  LC_FORMAT sub-elements   #########
        //before referencing another file we need to get the "replaceTo" value from current doc if there
        //sometimes we have entries of type :
        // <LC_FORMAT ref="en_US" replaceTo="blah...">
        m_ReplaceTo = m_domWrapper.getAttributeFromElement(OOConstants.LOCALE, OOConstants.LC_FORMAT, OOConstants.REPLACE_TO_SMALL);
        m_ReplaceFrom = m_domWrapper.getAttributeFromElement(OOConstants.LOCALE, OOConstants.LC_FORMAT, OOConstants.REPLACE_FROM_SMALL);
        
        if (bResolveRefs == true) checkForRef(OOConstants.LC_FORMAT);
        
        //m_LCFormat has the replaceFrom and replaceTo data
        m_LCFormat = m_domWrapper.getAttributesFromElement(OOConstants.LOCALE, OOConstants.LC_FORMAT);
        checkReplacements();  //we have 2 copies of replaceFrom,replaceTo , 1 used by ConvertOOLocale, 1 by OOComparator
        
        m_FormatElements = m_domWrapper.getAttributesFromElement(OOConstants.LC_FORMAT, OOConstants.FORMAT_ELEMENT, OOConstants.MSGID);
        m_FormatCodes = m_domWrapper.getTextFromElementsAndParentAttrib(OOConstants.FORMAT_ELEMENT, OOConstants.FORMAT_CODE, OOConstants.MSGID);
        m_FormatDefaultNames = m_domWrapper.getTextFromElementsAndParentAttrib(OOConstants.FORMAT_ELEMENT, OOConstants.DEFAULT_NAME, OOConstants.MSGID);
        m_domWrapper.resetDoc(m_doc);  //set doc back to input one as checkForRef() may have set it to aliased doc
        
        //#######  LC_COLLATION sub-elements   #########
        if (bResolveRefs == true) checkForRef(OOConstants.LC_COLLATION);
        m_Collators = m_domWrapper.getAttributesFromElement( OOConstants.LC_COLLATION, OOConstants.COLLATOR);
        m_CollationOptions = m_domWrapper.getTextFromAllElements(OOConstants.COLLATION_OPTIONS, OOConstants.TRANSLITERATION_MODULES);;
        m_domWrapper.resetDoc(m_doc);
        
        //#######  LC_SEARCH sub-elements   #########
        if (bResolveRefs == true) checkForRef(OOConstants.LC_SEARCH);
        m_SearchOptions = m_domWrapper.getTextFromAllElements(OOConstants.SEARCH_OPTIONS, OOConstants.TRANSLITERATION_MODULES);
        m_domWrapper.resetDoc(m_doc);
        
        //#######  LC_INDEX sub-elements   #########
        if (bResolveRefs == true) checkForRef(OOConstants.LC_INDEX);
        m_IndexKeys = m_domWrapper.getAttributesFromElement(OOConstants.LC_INDEX, OOConstants.INDEX_KEY);
        m_IndexKeysData = m_domWrapper.getTextFromElementsAndElementAttrib(OOConstants.LC_INDEX, OOConstants.INDEX_KEY, OOConstants.UNOID);
        m_UnicodeScript = m_domWrapper.getTextFromAllElements(OOConstants.LC_INDEX, OOConstants.UNICODE_SCRIPT);
        m_FollowPageWord = m_domWrapper.getTextFromAllElements(OOConstants.LC_INDEX, OOConstants.FOLLOW_PAGE_WORD);
        m_domWrapper.resetDoc(m_doc);
        
        
        // #######  LC_CALENDAR sub-elements   #########
        //LC_CALENDAR can have ref but so can DaysOfWeek, MonthsOfYear and Eras !!!!!
        checkForRef(OOConstants.LC_CALENDAR);
        //here getting data from locale or LC_CALENDAR's referenced locale
        m_Calendars = m_domWrapper.getAttributesFromElement(OOConstants.LC_CALENDAR, OOConstants.CALENDAR);
        m_StartDayOfWeek = m_domWrapper.getTextFromElementsAndGPAttrib(OOConstants.START_DAY_OF_WEEK, OOConstants.DAY_ID, OOConstants.UNOID);
        m_MinDaysInFirstweek = m_domWrapper.getTextFromElementsAndParentAttrib(OOConstants.CALENDAR, OOConstants.MINIMAL_DAYS_IN_FIRST_WEEK, OOConstants.UNOID);
        
        m_AbbrDays = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.DAY, OOConstants.DAY_ID, OOConstants.DEFAULT_ABBRV_NAME, OOConstants.UNOID);
        m_WideDays = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.DAY, OOConstants.DAY_ID, OOConstants.DEFAULT_FULL_NAME, OOConstants.UNOID);
        
        m_AbbrMonths = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.MONTH, OOConstants.MONTH_ID, OOConstants.DEFAULT_ABBRV_NAME, OOConstants.UNOID);
        m_WideMonths = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.MONTH, OOConstants.MONTH_ID, OOConstants.DEFAULT_FULL_NAME, OOConstants.UNOID);
        
        m_AbbrEras = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.ERA, OOConstants.ERA_ID, OOConstants.DEFAULT_ABBRV_NAME, OOConstants.UNOID);
        m_WideEras = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.ERA, OOConstants.ERA_ID, OOConstants.DEFAULT_FULL_NAME, OOConstants.UNOID);
        
        //now add referenced data (if any) from aysOfWeek, MonthsOfYear and Eras to the above
        getRefs(OOConstants.DAYS_OF_WEEK);
        getRefs(OOConstants.MONTHS_OF_YEAR);
        getRefs(OOConstants.ERAS);
        m_domWrapper.resetDoc(m_doc);
        
        
        // #######  LC_CURRENCY sub-elements   #########
        Document doc = m_doc;
        String refFilename = getRef(OOConstants.LC_CURRENCY);
        if (refFilename != null)
            doc = LDMLUtilities.parse(refFilename, true);
            
        String SearchLocation = "//Locale/LC_CURRENCY/Currency"; 
        NodeList nl = LDMLUtilities.getNodeList (doc, SearchLocation);
        NodeList nl_id = LDMLUtilities.getNodeList (doc, "//Locale/LC_CURRENCY/Currency/CurrencyID");
        NodeList nl_symbol = LDMLUtilities.getNodeList (doc, "//Locale/LC_CURRENCY/Currency/CurrencySymbol");
        NodeList nl_code = LDMLUtilities.getNodeList (doc, "//Locale/LC_CURRENCY/Currency/BankSymbol");
        NodeList nl_name = LDMLUtilities.getNodeList (doc, "//Locale/LC_CURRENCY/Currency/CurrencyName");
        NodeList nl_decimal = LDMLUtilities.getNodeList (doc, "//Locale/LC_CURRENCY/Currency/DecimalPlaces");
        for (int i=0; i < nl.getLength(); i++)
        {
            Vector inner = new Vector ();
            String id = LDMLUtilities.getNodeValue (nl_id.item(i));
            inner.add (0, id);
            String symbol = LDMLUtilities.getNodeValue (nl_symbol.item(i));
            inner.add (1, symbol);
            String code = LDMLUtilities.getNodeValue (nl_code.item(i));
            inner.add (2, code);
            String name = LDMLUtilities.getNodeValue (nl_name.item(i));
            inner.add (3, name);
            String decimal = LDMLUtilities.getNodeValue (nl_decimal.item(i));
            inner.add (4, decimal);
            
            String def = LDMLUtilities.getAttributeValue (nl.item(i), OOConstants.DEFAULT);
            inner.add (5, def);
            String uicfc = LDMLUtilities.getAttributeValue (nl.item(i), OOConstants.USED_IN_COMPARTIBLE_FORMATCODES_SMALL);
            inner.add (6, uicfc);
            String legacyOnly = LDMLUtilities.getAttributeValue (nl.item(i), OOConstants.LEGACY_ONLY);
            if (legacyOnly != null) inner.add (7, legacyOnly);   //only optional one
            
    //        System.err.println (id + " " + symbol + " " + code + " " + name + " " + decimal + " " + def + " " + uicfc);
            m_CurrencyData.add (inner);
        }  
        
        //####### LC_TRANSLITERATION sub-elements   #########
        if (bResolveRefs == true) checkForRef(OOConstants.LC_TRANSLITERATION);
        m_Transliterations = m_domWrapper.getAttributesFromElement(OOConstants.LC_TRANSLITERATION, OOConstants.TRANSLITERATION);
        m_domWrapper.resetDoc(m_doc);
        
        //#######  LC_MISC sub-elements   #########
        if (bResolveRefs == true) checkForRef(OOConstants.LC_MISC);
        
        String miscData = m_domWrapper.getTextFromElement(OOConstants.FORBIDDEN_CHARACTERS, OOConstants.FORBIDDEN_LINE_BEGIN_CHARACTERS);
        m_ForbiddenBeginChar = miscData;
        if (miscData!=null) m_ForbiddenChars.put(OOConstants.FORBIDDEN_LINE_BEGIN_CHARACTERS, miscData);
        miscData = m_domWrapper.getTextFromElement(OOConstants.FORBIDDEN_CHARACTERS, OOConstants.FORBIDDEN_LINE_END_CHARACTERS);
        m_ForbiddenEndChar = miscData;
        if (miscData!=null) m_ForbiddenChars.put(OOConstants.FORBIDDEN_LINE_END_CHARACTERS, miscData);
        
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.TRUE_WORD);
        if (miscData!=null) m_ReservedWords.put(OOConstants.TRUE_WORD, miscData);
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.FALSE_WORD);
        if (miscData!=null) m_ReservedWords.put(OOConstants.FALSE_WORD, miscData);
        
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.ABOVE_WORD);
        if (miscData!=null) m_ReservedWords.put(OOConstants.ABOVE_WORD, miscData);
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.BELOW_WORD);
        if (miscData!=null) m_ReservedWords.put(OOConstants.BELOW_WORD, miscData);
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.QUARTER_1_WORD);
        if (miscData!=null) m_ReservedWords.put(OOConstants.QUARTER_1_WORD, miscData);
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.QUARTER_2_WORD);
        if (miscData!=null) m_ReservedWords.put(OOConstants.QUARTER_2_WORD, miscData);
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.QUARTER_3_WORD);
        if (miscData!=null)  m_ReservedWords.put(OOConstants.QUARTER_3_WORD, miscData);
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.QUARTER_4_WORD);
        if (miscData!=null) m_ReservedWords.put(OOConstants.QUARTER_4_WORD, miscData);
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.QUARTER_1_ABBREVIATION);
        if (miscData!=null) m_ReservedWords.put(OOConstants.QUARTER_1_ABBREVIATION, miscData);
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.QUARTER_2_ABBREVIATION);
        if (miscData!=null) m_ReservedWords.put(OOConstants.QUARTER_2_ABBREVIATION, miscData);
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.QUARTER_3_ABBREVIATION);
        if (miscData!=null) m_ReservedWords.put(OOConstants.QUARTER_3_ABBREVIATION, miscData);
        miscData = m_domWrapper.getTextFromElement(OOConstants.RESERVED_WORDS, OOConstants.QUARTER_4_ABBREVIATION);
        if (miscData!=null) m_ReservedWords.put(OOConstants.QUARTER_4_ABBREVIATION, miscData);
        m_domWrapper.resetDoc(m_doc);
        
        
        //#######  LC_NumberingLevel sub-elements   #########
        if (bResolveRefs == true) checkForRef(OOConstants.LC_NUMBERING_LEVEL);
        m_NumberingLevels = m_domWrapper.getAttributesFromElement(OOConstants.LC_NUMBERING_LEVEL, OOConstants.NUMBERING_LEVEL);
        m_domWrapper.resetDoc(m_doc);
        
        //#######  LC_OutlineNumberingLevel sub-elements   #########
        //m_OutlineNumberingLevels are all bunched together as parent element is * and has no attr to distinguish it
        //however, we know they should go in groups of 5, so we can break this out later
        if (bResolveRefs == true) checkForRef(OOConstants.LC_OUTLINE_NUMBERING_LEVEL);
        m_OutlineNumberingLevels = m_domWrapper.getAttributesFromElement(OOConstants.OUTLINE_STYLE, OOConstants.OUTLUNE_NUMBERING_LEVEL);
        m_domWrapper.resetDoc(m_doc);
        
        bRc = true;
        return bRc;
    }
    
    //extracts all refs from the doc
    // if bResolveRefs = false then we only ned to resolves those refs where data overlaps CLDR std data
    private void readRefs(boolean bResolveRefs)
    {
    /*any of the following can have the RefLocale attribute
     *      LC_FORMAT                       use "ref=" in LDML
     *      LC_CALENDAR
     *      LC_CALENDAR > DaysOfWeek
     *      LC_CALENDAR > MonthsOfYear
     *      LC_CALENDAR > Eras
     *      LC_CURRENCY                     use "ref=" in LDML
     *      LC_CTYPE
     *      LC_COLLATION                    use "ref=" in LDML
     *      LC_SEARCH                       use "ref=" in LDML
     *      LC_INDEX                        use "ref=" in LDML
     *      LC_TRANSLITERATION              use "ref=" in LDML
     *      LC_MISC                         use "ref=" in LDML
     *      LC_NumberingLevel               use "ref=" in LDML
     *      LC_OutlineNumberingLevel        use "ref=" in LDML
     **/
        
        // moved     String ref = getFullyResulvedRef(OOConstants.LC_FORMAT);
        //      if (ref!=null) m_Refs.put(OOConstants.LC_FORMAT, ref);
        
        boolean bLC_CALENDAR_Has_Ref = false;
        String refFilename = null;
        String ref = getFullyResulvedRef(OOConstants.LC_CALENDAR);
        if (ref!=null)
        {
            m_Refs.put(OOConstants.LC_CALENDAR, ref);
            bLC_CALENDAR_Has_Ref = true;
            //handle situation where the referenced LC_CALENDAR has sub DAYS_OF_WEEK or MONTHS_OF_YEAR or ERAS
            //which have references
            refFilename = m_filename.substring(0, m_filename.lastIndexOf('/')) + "/";
            refFilename += ref;
            refFilename += ".xml";
        }
        
        //if LC_CALENDAR has a ref then DAYS_OF_WEEK or MONTHS_OF_YEAR or ERAS will not be in current doc
        // but in the doc refernced by LC_CALENDAR these sub-elements may have refs
        if (bLC_CALENDAR_Has_Ref == true)
            resetDoc(refFilename);
        Hashtable table = getFullyResulvedRef(OOConstants.DAYS_OF_WEEK, OOConstants.CALENDAR, OOConstants.UNOID);
        if (table.size() >0)
            m_Refs.put(OOConstants.DAYS_OF_WEEK, table);
        
        if (bLC_CALENDAR_Has_Ref == true)
            resetDoc(refFilename);
        table = getFullyResulvedRef(OOConstants.MONTHS_OF_YEAR, OOConstants.CALENDAR, OOConstants.UNOID);
        if (table.size() >0)
            m_Refs.put(OOConstants.MONTHS_OF_YEAR, table);
        
        if (bLC_CALENDAR_Has_Ref == true)
            resetDoc(refFilename);
        table = getFullyResulvedRef(OOConstants.ERAS, OOConstants.CALENDAR, OOConstants.UNOID);
        if (table.size() >0)
            m_Refs.put(OOConstants.ERAS, table);
        
        ref = getFullyResulvedRef(OOConstants.LC_CURRENCY);
        if (ref!=null) m_Refs.put(OOConstants.LC_CURRENCY, ref);
        
        ref = getFullyResulvedRef(OOConstants.LC_CTYPE);
        if (ref!=null) m_Refs.put(OOConstants.LC_CTYPE, ref);
        
        if (bResolveRefs == true)
        {
            ref = getFullyResulvedRef(OOConstants.LC_FORMAT);
            if (ref!=null) m_Refs.put(OOConstants.LC_FORMAT, ref);
            
            ref = getFullyResulvedRef(OOConstants.LC_COLLATION);
            if (ref!=null) m_Refs.put(OOConstants.LC_COLLATION, ref);
            
            ref = getFullyResulvedRef(OOConstants.LC_SEARCH);
            if (ref!=null) m_Refs.put(OOConstants.LC_SEARCH, ref);
            
            ref = getFullyResulvedRef(OOConstants.LC_INDEX);
            if (ref!=null) m_Refs.put(OOConstants.LC_INDEX, ref);
            
            ref = getFullyResulvedRef(OOConstants.LC_TRANSLITERATION);
            if (ref!=null) m_Refs.put(OOConstants.LC_TRANSLITERATION, ref);
            
            ref = getFullyResulvedRef(OOConstants.LC_MISC);
            if (ref!=null) m_Refs.put(OOConstants.LC_MISC, ref);
            
            ref = getFullyResulvedRef(OOConstants.LC_NUMBERING_LEVEL);
            if (ref!=null) m_Refs.put(OOConstants.LC_NUMBERING_LEVEL, ref);
            
            ref = getFullyResulvedRef(OOConstants.LC_OUTLINE_NUMBERING_LEVEL);
            if (ref!=null) m_Refs.put(OOConstants.LC_OUTLINE_NUMBERING_LEVEL, ref);
        }
        else
        {  //we just read the ref attrib value, this is the "data"
            ref = m_domWrapper.getAttributeValue(OOConstants.LC_FORMAT, OOConstants.REF);
            if (ref!=null) m_Refs2.put(OOConstants.LC_FORMAT, ref);
            
            ref = m_domWrapper.getAttributeValue(OOConstants.LC_COLLATION, OOConstants.REF);
            if (ref!=null) m_Refs2.put(OOConstants.LC_COLLATION, ref);
            
            ref = m_domWrapper.getAttributeValue(OOConstants.LC_SEARCH, OOConstants.REF);
            if (ref!=null) m_Refs2.put(OOConstants.LC_SEARCH, ref);
            
            ref = m_domWrapper.getAttributeValue(OOConstants.LC_INDEX, OOConstants.REF);
            if (ref!=null) m_Refs2.put(OOConstants.LC_INDEX, ref);
            
            ref = m_domWrapper.getAttributeValue(OOConstants.LC_TRANSLITERATION, OOConstants.REF);
            if (ref!=null) m_Refs2.put(OOConstants.LC_TRANSLITERATION, ref);
            
            ref = m_domWrapper.getAttributeValue(OOConstants.LC_MISC, OOConstants.REF);
            if (ref!=null) m_Refs2.put(OOConstants.LC_MISC, ref);
            
            ref = m_domWrapper.getAttributeValue(OOConstants.LC_NUMBERING_LEVEL, OOConstants.REF);
            if (ref!=null) m_Refs2.put(OOConstants.LC_NUMBERING_LEVEL, ref);
            
            ref = m_domWrapper.getAttributeValue(OOConstants.LC_OUTLINE_NUMBERING_LEVEL, OOConstants.REF);
            if (ref!=null) m_Refs2.put(OOConstants.LC_OUTLINE_NUMBERING_LEVEL, ref);
            
        }
    }
    
    
    // can have multiple reference chains i.e. XML1 refs XML2 which in turn refs XML3 and so on
    private String getFullyResulvedRef(String element)
    {
        if (element == null)
            return null;
        
        String ref = m_domWrapper.getAttributeValue(element, OOConstants.REF);
        String fullyResulvedRef = null;
        String logStr = "";
        
        while (ref != null)
        {
            logStr = logStr + " -> " + ref;
            fullyResulvedRef = ref;
            String refFilename = m_filename.substring(0, m_filename.lastIndexOf('/')) + "/";
            refFilename += ref;
            refFilename += ".xml";
            
            resetDoc(refFilename);  //set doc = referenced one
            ref = m_domWrapper.getAttributeValue(element, OOConstants.REF);
        }
        
        if (fullyResulvedRef != null)  //logging
            Logging.Log2("Fully resolved Ref for : " + element + " = " +  logStr);
        
        resetDoc(m_filename);  //important !! set back to original doc
        return fullyResulvedRef;
    }
    
    // can have multiple reference chains i.e. XML1 refs XML2 which in turn refs XML3 and so on
    private Hashtable getFullyResulvedRef(String element, String parentElement, String elementAttrib)
    {
        if ((element == null) || (parentElement == null) || (elementAttrib == null))
            return null;
        
        Hashtable fullyResolvedTable = new Hashtable();
        
        Hashtable table = m_domWrapper.getAttributeValues(element, OOConstants.REF, parentElement, elementAttrib);
        
        String fullyResulvedRef = null;
        String logStr = "";
        
        Enumeration data = table.elements();
        Enumeration keys = table.keys();
        String refFilename = m_filename;
        while (data.hasMoreElements()==true)
        {
            String ref = (String) data.nextElement();
            String key = (String) keys.nextElement();
            Vector localeAndType = null;
            while (ref != null)
            {
                //ref can be of type "en_US_grtegorian", need to extract the locale
                localeAndType = OOToLDMLMapper.getLocaleAndType(refFilename, ref);
                if ((localeAndType==null) || (localeAndType.size()==0))
                    break;
                
                String type = null;
                if (localeAndType.size()>1)
                    type = (String) localeAndType.elementAt(1);
                else
                    type = key;
                
                logStr = logStr + " -> " + ref;
                fullyResulvedRef = ref;
                refFilename = m_filename.substring(0, m_filename.lastIndexOf('/')) + "/";
                refFilename += localeAndType.elementAt(0);
                refFilename += ".xml";
                
                resetDoc(refFilename);  //set doc = referenced one
                ref = m_domWrapper.getAttributesFromElement(parentElement, element, OOConstants.REF, elementAttrib, type);
            }
            
            //handle case where fully resolved locale is of type "gregorian"
            //we need to indicate that this in not the gregorian in the current locale file
            if (fullyResulvedRef.indexOf('_')== -1)
            {
                fullyResulvedRef = (String) (localeAndType.elementAt(0)) + "_" + fullyResulvedRef;
                logStr = logStr + " -> " + fullyResulvedRef;
            }
            
            if (fullyResulvedRef != null)  //logging
                Logging.Log2("Fully resolved Ref for : " + key + " " + element + " = " +  logStr);
            logStr = "";
            
            fullyResolvedTable.put(key, fullyResulvedRef);
        }
        
        resetDoc(m_filename);  //important !! set back to original doc
        return fullyResolvedTable;
    }
    
    
    /* method causes Document to be reset to referenced one if necessary
     */
    private void checkForRef(String element)
    {
        if (element == null)
            return;
        
        String refFilename = getRef(element);
        
        if (refFilename != null)
            Logging.Log2("Reading referenced data from : " + refFilename + " for " + element);
        
        resetDoc(refFilename);
    }
    
    /* method checks in memory Hashtable to see if the element has a ref and if so returns it's filename
     */
    private String getRef(String element)
    {
        if (element == null)
            return null;
        
        String refFilename = null;
        Object obj = m_Refs.get(element);
        
        if (obj != null)
        {
            if (obj instanceof String)
            {
                String ref = (String) obj;
                //pos 0 of Vector=locale name, pos 1 = type
                Vector localeAndType = OOToLDMLMapper.getLocaleAndType(m_filename, ref);
                if ((localeAndType != null) && (localeAndType.size() >0))
                {
                    refFilename = m_filename.substring(0, m_filename.lastIndexOf('/')) + "/";
                    refFilename +=  (String) localeAndType.elementAt(0);
                    refFilename += ".xml";
                }
            }
        }
        return refFilename;
    }
    
        /* method checks in memory Hashtable to see if the element has a ref and if so retrieves the data
         * from the reference locale file and puts it in the appropriate container
         * method called for DaysOfWeek, MonthsOfYear and Eras
         */
    private void getRefs(String element)
    {
        if (element == null)
            return;
        
        Object obj = m_Refs.get(element);
        if ((obj != null) && (obj instanceof Hashtable))
        {
            Hashtable refs = (Hashtable) obj;
            Enumeration keys = refs.keys();
            Enumeration data = refs.elements();
            while (keys.hasMoreElements()==true)
            {
                String ref = (String) data.nextElement();   //something like "en_US_gregorian"
                String key = (String) keys.nextElement();   //something like "gregorian"
                
                //pos 0 of Vector=locale name, pos 1 = type
                String refFilename = null;
                Vector localeAndType = OOToLDMLMapper.getLocaleAndType(m_filename, ref);
                if ((localeAndType != null) && (localeAndType.size() >0))
                {
                    refFilename = m_filename.substring(0, m_filename.lastIndexOf('/')) + "/";
                    refFilename +=  (String) localeAndType.elementAt(0);
                    refFilename += ".xml";
                }
                
                if (refFilename != null)
                {
                    //need to decide which part of ref file to retrieve
                    //if we are here then the type should also be specified, (this is currently the case in the OO data)
                    //but to future proof it,  we use the key if it's not specified
                    String type = null;
                    if (localeAndType.size() >1)
                        type = (String) localeAndType.elementAt(1);
                    else
                        type = key;
                    
                    resetDoc(refFilename);
                    if (element.compareTo(OOConstants.DAYS_OF_WEEK)==0)
                    {
                        Hashtable Abbrs = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.DAY, OOConstants.DAY_ID, OOConstants.DEFAULT_ABBRV_NAME, OOConstants.UNOID);
                        Hashtable Wides = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.DAY, OOConstants.DAY_ID, OOConstants.DEFAULT_FULL_NAME, OOConstants.UNOID);
                        
                        Hashtable AbbrOfInterest = (Hashtable) Abbrs.get(type);
                        if (AbbrOfInterest != null)
                            m_AbbrDays.put(key, AbbrOfInterest);
                        
                        Hashtable WideOfInterest = (Hashtable) Wides.get(type);
                        if (WideOfInterest != null)
                            m_WideDays.put(key, WideOfInterest);
                    }
                    else if (element.compareTo(OOConstants.MONTHS_OF_YEAR)==0)
                    {
                        Hashtable Abbrs = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.MONTH, OOConstants.MONTH_ID, OOConstants.DEFAULT_ABBRV_NAME, OOConstants.UNOID);
                        Hashtable Wides = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.MONTH, OOConstants.MONTH_ID, OOConstants.DEFAULT_FULL_NAME, OOConstants.UNOID);
                        
                        Hashtable AbbrOfInterest = (Hashtable) Abbrs.get(type);
                        if (AbbrOfInterest != null)
                            m_AbbrMonths.put(key, AbbrOfInterest);
                        
                        Hashtable WideOfInterest = (Hashtable) Wides.get(type);
                        if (WideOfInterest != null)
                            m_WideMonths.put(key, WideOfInterest);
                    }
                    else if (element.compareTo(OOConstants.ERAS)==0)
                    {
                        Hashtable Abbrs = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.ERA, OOConstants.ERA_ID, OOConstants.DEFAULT_ABBRV_NAME, OOConstants.UNOID);
                        Hashtable Wides = m_domWrapper.getTextFromElementsAndGGPAttrib(OOConstants.ERA, OOConstants.ERA_ID, OOConstants.DEFAULT_FULL_NAME, OOConstants.UNOID);
                        
                        Hashtable AbbrOfInterest = (Hashtable) Abbrs.get(type);
                        if (AbbrOfInterest != null)
                            m_AbbrEras.put(key, AbbrOfInterest);
                        
                        Hashtable WideOfInterest = (Hashtable) Wides.get(type);
                        if (WideOfInterest != null)
                            m_WideEras.put(key, WideOfInterest);
                    }
                }
            }
        }
    }
    
    //methods resets Document if it needs to be
    private void resetDoc(String refFilename)
    {
        if (refFilename != null)
        {
            Document doc = LDMLUtilities.parse(refFilename, true);
            m_domWrapper.resetDoc(doc);
        }
    }
    
    private void checkReplacements()
    {
        Hashtable t = (Hashtable) m_LCFormat.elementAt(0);
        if (t != null)
        {
            //if replaceTo is not null then we want to be sure it doesn't get overwritten by referenced locale's replaceTo
            if (m_ReplaceTo != null)
            {
                if (t.containsKey(OOConstants.REPLACE_TO_SMALL)==true)
                {
                    t.remove(OOConstants.REPLACE_TO_SMALL);
                    t.put(OOConstants.REPLACE_TO_SMALL, m_ReplaceTo);
                }
            }
            
            if (m_ReplaceFrom == null)
            {
                //if LC_FORMAT has a "ref" atrtrib then it shouldn't have a "replaceFrom" atrrib, this will be in referenced locale
                if (t.containsKey(OOConstants.REPLACE_FROM_SMALL)==true)
                {
                    m_ReplaceFrom = (String) t.get(OOConstants.REPLACE_FROM_SMALL);
                }
            }
        }
    }
    
    
    
}
