/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

import org.w3c.dom.*;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.icu.LDMLConstants;
import java.io.*;
import java.util.*;

/**
 *
 * class reads all OO required LDML XML data into memory
 */
public class LDMLReaderForOO
{
    private double m_cldr_ver = 0.0;  //read it from docType info in xml file
    
    private String m_filename = null;
    private Document m_doc = null;
    private DOMWrapper m_domWrapper = null;
    
    //### class members to hold the data ####
    public String m_Version = null;   //the CLDR version
    
    // attributes of locale element
    public Hashtable m_LocaleInfo = new Hashtable();  //key = atrib name, value = attrib value
    
    public String m_GenerationDate = null;
    public Vector m_Language_Vect = null;
    public Vector m_Script_Vect = null;
    public Vector m_Territory_Vect = null;
    
    public String m_LangID = null;
    public String m_TerritoryID = null;
    public String m_LangDefaultName = null;
    public String m_TerritoryDefaultName = null;
    public String m_PlatformID = null;
    public String m_Variant = null;
    
    public String m_DateSeparator = null;
    public String m_ThousandSeparator = null;
    public String m_DecimalSeparator = null;
    public String m_TimeSeparator = null;
    public String m_Time100SecSeparator = null;
    public String m_TimeAM = null;
    public String m_TimePM = null;
    public String m_ListSeparator = null;
    public String m_QuotationStart = null;
    public String m_QuotationEnd = null;
    public String m_DoubleQuotationStart = null;
    public String m_DoubleQuotationEnd = null;
    public String m_MeasurementSystem = null;
    public String m_LongDateDayOfWeekSeparator = null;
    public String m_LongDateDaySeparator = null;
    public String m_LongDateMonthSeparator = null;
    public String m_LongDateYearSeparator = null;
    
    public String m_FormatRefLocale = null;
    public String m_FormatReplaceFrom = null;
    public String m_FormatReplaceTo = null;
    
    // FormatElement variables
    public Hashtable m_FixedNFormats = null; // hashtable of hashtables
    public Hashtable m_FixedNPatterns = null;
    public Hashtable m_FixedNOO = null; // hashtable of hashtables
    public Hashtable m_FixedDefaultName = null;
    
    //vurrently not in xml
    public Hashtable m_FractNFormats = null; // hashtable of hashtables
    public Hashtable m_FractNPatterns = null;
    public Hashtable m_FractNOO = null; // hashtable of hashtables
    public Hashtable m_FractDefaultName = null;
    
    public Hashtable m_PercNFormats = null; // hashtable of hashtables
    public Hashtable m_PercNPatterns = null;
    public Hashtable m_PercNOO = null; // hashtable of hashtables
    public Hashtable m_PercDefaultName = null;
    public Hashtable m_SciNFormats = null; // hashtable of hashtables
    public Hashtable m_SciNPatterns = null;
    public Hashtable m_SciNOO = null; // hashtable of hashtables
    public Hashtable m_SciDefaultName = null;
    public Hashtable m_CurrNFormats = null; // hashtable of hashtables
    public Hashtable m_CurrNPatterns = null;
    public Hashtable m_CurrNOO = null; // hashtable of hashtables
    public Hashtable m_CurrNDefaultName = null;
    public Hashtable m_DateFormats = null; // hashtable of hashtables
    public Hashtable m_DatePatterns = null;
    public Hashtable m_DateOO = null; // hashtable of hashtables
    public Hashtable m_DateDefaultName = null;
    public Hashtable m_TimeFormats = null; // hashtable of hashtables
    public Hashtable m_TimePatterns = null;
    public Hashtable m_TimeOO = null; // hashtable of hashtables
    public Hashtable m_TimeDefaultName = null;
    public Hashtable m_DateTimeFormats = null; // hashtable of hashtables
    public Hashtable m_DateTimePatterns = null;
    public Hashtable m_DateTimeOO = null; // hashtable of hashtables
    public Hashtable m_DateTimeDefaultName = null;
    
    public String m_CollationRefLocale = null;
    public Vector m_Collators = null;  // vector of hashtables
    public Vector m_CollationOptions = null; // vector of string
//    public Hashtable m_CollationDefaultName = null;
    
    public String m_SearchRefLocale = null;
    public Vector m_SearchOptions = null; // vector of string
    
    public String m_IndexRefLocale = null;
    public Vector m_IndexKeys = null; // vector of hashtables of String and Hashtable
    public Vector m_IndexUnicodeScript = null; // vector of strings
    public Vector m_IndexFollowPageWord = null; // vector of strings
    
    public Vector m_CalendarTypes = null;
    public Vector m_CalendarDefaultAttribs = null;
    public Hashtable m_AbbrDays = null;
    public Hashtable m_FullDays = null;
    public Hashtable m_AbbrMonths = null;
    public Hashtable m_FullMonths = null;
    public Hashtable m_EraNames = null;
    public Hashtable m_AbbrEras = null;
    public Hashtable m_StartOfWeeks = null;
    public Hashtable m_MinDays = null;
    public Hashtable m_AbbrQuarters = null;   //for cldr 1.4+ read from cldr else read from OO.o special
    public Hashtable m_WideQuarters = null;
       
    // inner holds data in following order : CurrencyID,symbol,code,name, blank ,default, usedInCompatibleFormatCode, legacyOnly (if defined)
    public Vector m_CurrencyData_ooo = new Vector ();   //Vector of vectors
   // inner holds data in following order : blank ,symbol,code,name, 
    public Vector m_CurrencyData_cldr = new Vector ();   //Vector of vectors, will hold data for all currencies not jsut those used in the locale
    
    public String m_TransliterationRefLocale = null;
    public Vector m_TransliterationAtts = null;
    
    public String m_ReservedRefLocale = null;
    public Hashtable m_ReservedWords = null;   //without quarters 
     
    public String m_ForbiddenRefLocale = null;
    public String m_ForbiddenLineBeg = null;
    public String m_ForbiddenLineEnd = null;
    
    public String m_NumberingRefLocale = null;
    public Vector m_NumberingLevelAtts = null;  // vector of hashtables
    
    public String m_OutlineNumberingRefLocale = null;
    public Vector m_OutlineNumberingLevels = null; // vector of vectors of hashtables.
    
    public Vector m_DateFormatItems = null;   //simple Vecotr of flexible date/time pattern strings
    
    
    /** Creates a new instance of LDMLReaderForOO */
    public LDMLReaderForOO(String filename)
    {
        m_filename = filename;
    }
    
    public void readDocument(String locale, boolean bIsCLDR)
    {
        if (m_filename == null)
            return;
        
        
        if (bIsCLDR == true)  //apply inheritance from language and root locales as well as resolving aliases
        {
            String sourceDir = m_filename.substring(0, m_filename.lastIndexOf('/'));
            // if params 3,4,5 = false then any errors will cause exception to be thrown
            m_doc = LDMLUtilities.getFullyResolvedLDML(sourceDir, locale, false, false, false, false /*ignoreDraft*/);
        }
        else
            m_doc = LDMLUtilities.parse(m_filename, false /*if false throw exception*/);
        
        //get the version
        String sysId = m_doc.getDoctype().getSystemId();
        String ver = (sysId.substring(sysId.indexOf("/dtd/") + 5, sysId.indexOf("/ldml.dtd")));
        Double d = new Double(ver);
        m_cldr_ver = d.doubleValue();
    }
    
    public double getCLDRVersion ()
    {
        return m_cldr_ver;
    }
    
   /* parse the LDML XML document to store its data in memory.
      Returns true is document read in completely, false if not.*/
    public boolean readInXML(boolean bIsCLDR)
    {
        if (m_filename == null)
            return false;
        boolean bRc = false;
             
        m_domWrapper = new DOMWrapper(m_doc);
        if (m_doc == null)
            return false;
        
        if (m_domWrapper.elementExists(LDMLConstants.LDML) == false)
            return false;
        
        readXML ();
        if (bIsCLDR == false) readSpecialXML ();
        
        bRc = true;
        return bRc;
    }
    
    //reads std LDML
    private void readXML ()
    {
        //#######  <LC_INDEX> sub-elements   #########
        //get the CLDR Version
        m_Version = m_domWrapper.getAttributeFromElement(LDMLConstants.IDENTITY, LDMLConstants.VERSION);

        // Generation date
        String generationStr = m_domWrapper.getAttributeValue(LDMLConstants.GENERATION, LDMLConstants.DATE);
        if ((generationStr != null) && (generationStr.length()>0))
        {
            m_GenerationDate = generationStr;
        }
        
        m_Language_Vect = m_domWrapper.getAttributesFromElement(LDMLConstants.IDENTITY, LDMLConstants.LANGUAGE);
        m_Script_Vect = m_domWrapper.getAttributesFromElement(LDMLConstants.IDENTITY, LDMLConstants.SCRIPT);
        m_Territory_Vect = m_domWrapper.getAttributesFromElement(LDMLConstants.IDENTITY, LDMLConstants.TERRITORY);
        
        // Extract language type and territory type
        if ((m_Language_Vect != null) && (m_Language_Vect.size()>0))
        {
            Hashtable languageTable = (Hashtable) m_Language_Vect.elementAt(0);
            if (languageTable != null)
                m_LangID = (String) languageTable.get(LDMLConstants.TYPE);
            else
                m_LangID = ""; 
        }
        if ((m_Territory_Vect != null) && (m_Territory_Vect.size()>0))
        {
            Hashtable territoryTable = (Hashtable) m_Territory_Vect.elementAt(0);
            if (territoryTable != null)
                m_TerritoryID = (String) territoryTable.get(LDMLConstants.TYPE);
            else
                m_TerritoryID = ""; 
        }
        if ((m_TerritoryID != null) && (m_TerritoryID.compareTo("CS")==0))
            m_TerritoryID="YU";  //workaround : CLDR v.complicated for sr, sh
        
        m_ThousandSeparator = m_domWrapper.getTextFromElement(LDMLConstants.GROUP);
        m_DecimalSeparator = m_domWrapper.getTextFromElement(LDMLConstants.DECIMAL);
        m_ListSeparator = m_domWrapper.getTextFromElement(LDMLConstants.LIST);
        
        m_QuotationStart = m_domWrapper.getTextFromElement(LDMLConstants.QS);
        m_QuotationEnd = m_domWrapper.getTextFromElement(LDMLConstants.QE);
        m_DoubleQuotationStart = m_domWrapper.getTextFromElement(LDMLConstants.AQS);
        m_DoubleQuotationEnd = m_domWrapper.getTextFromElement(LDMLConstants.AQE);
        
        m_TimeAM = m_domWrapper.getTextFromElement(LDMLConstants.AM);
        m_TimePM = m_domWrapper.getTextFromElement(LDMLConstants.PM);
        
        m_MeasurementSystem = m_domWrapper.getAttributeFromElement(LDMLConstants.MEASUREMENT, LDMLConstants.MS, LDMLConstants.TYPE);
 
        //calendar
        m_CalendarTypes = m_domWrapper.getAttributesFromElement(LDMLConstants.CALENDARS, LDMLConstants.CALENDAR);
        m_CalendarDefaultAttribs = m_domWrapper.getAttributesFromElement(LDMLConstants.CALENDARS, LDMLConstants.DEFAULT);
        m_AbbrDays = m_domWrapper.getTextFromAllElementsWithGGGParent(LDMLConstants.DAY, LDMLConstants.TYPE, LDMLConstants.DAY_WIDTH, LDMLConstants.TYPE, 
                LDMLConstants.ABBREVIATED, LDMLConstants.DAY_CONTEXT, LDMLConstants.DAYS, 
                LDMLConstants.CALENDAR, LDMLConstants.TYPE, LDMLConstants.TYPE, LDMLConstants.FORMAT);
        
        m_FullDays = m_domWrapper.getTextFromAllElementsWithGGGParent(LDMLConstants.DAY, LDMLConstants.TYPE, LDMLConstants.DAY_WIDTH, LDMLConstants.TYPE, 
                LDMLConstants.WIDE, LDMLConstants.DAY_CONTEXT, LDMLConstants.DAYS, 
                LDMLConstants.CALENDAR, LDMLConstants.TYPE, LDMLConstants.TYPE, LDMLConstants.FORMAT);
        
        m_AbbrMonths = m_domWrapper.getTextFromAllElementsWithGGGParent(LDMLConstants.MONTH, LDMLConstants.TYPE, LDMLConstants.MONTH_WIDTH, LDMLConstants.TYPE, 
                LDMLConstants.ABBREVIATED, LDMLConstants.MONTH_CONTEXT, LDMLConstants.MONTHS, 
                LDMLConstants.CALENDAR, LDMLConstants.TYPE, LDMLConstants.TYPE, LDMLConstants.FORMAT);
        
        m_FullMonths = m_domWrapper.getTextFromAllElementsWithGGGParent(LDMLConstants.MONTH, LDMLConstants.TYPE, LDMLConstants.MONTH_WIDTH, LDMLConstants.TYPE, 
                LDMLConstants.WIDE, LDMLConstants.MONTH_CONTEXT, LDMLConstants.MONTHS, 
                LDMLConstants.CALENDAR, LDMLConstants.TYPE, LDMLConstants.TYPE, LDMLConstants.FORMAT
                );
        // Eras
        m_EraNames = m_domWrapper.getTextFromAllElementsWithGGParent(LDMLConstants.ERA, LDMLConstants.TYPE, LDMLConstants.ERANAMES, LDMLConstants.ERAS, LDMLConstants.CALENDAR, LDMLConstants.TYPE);
        m_AbbrEras = m_domWrapper.getTextFromAllElementsWithGGParent(LDMLConstants.ERA, LDMLConstants.TYPE, LDMLConstants.ERAABBR, LDMLConstants.ERAS, LDMLConstants.CALENDAR, LDMLConstants.TYPE);
        
        //Quarters is in cldr 1.4
        if (m_cldr_ver > 1.399)
        {    
            m_AbbrQuarters = m_domWrapper.getTextFromAllElementsWithGGGParent(LDMLConstants.QUARTER, LDMLConstants.TYPE, LDMLConstants.QUARTER_WIDTH, LDMLConstants.TYPE, 
                LDMLConstants.ABBREVIATED, LDMLConstants.QUARTER_CONTEXT, LDMLConstants.QUARTERS, 
                LDMLConstants.CALENDAR, LDMLConstants.TYPE, LDMLConstants.TYPE, LDMLConstants.FORMAT);

            m_WideQuarters = m_domWrapper.getTextFromAllElementsWithGGGParent(LDMLConstants.QUARTER, LDMLConstants.TYPE, LDMLConstants.QUARTER_WIDTH, LDMLConstants.TYPE,
                    LDMLConstants.WIDE, LDMLConstants.QUARTER_CONTEXT, LDMLConstants.QUARTERS,
                    LDMLConstants.CALENDAR, LDMLConstants.TYPE, LDMLConstants.TYPE, LDMLConstants.FORMAT);
        }
        
        m_StartOfWeeks = m_domWrapper.getAttributeFromElementsAndGPAttrib(LDMLConstants.WEEK, LDMLConstants.FIRSTDAY, LDMLConstants.DAY, LDMLConstants.TYPE);
        m_MinDays = m_domWrapper.getAttributeFromElementsAndGPAttrib(LDMLConstants.WEEK, LDMLConstants.MINDAYS, LDMLConstants.COUNT, LDMLConstants.TYPE);
        
        //#######  <LC_CURRENCY> sub-elements   #########                
        String SearchLocation = "//ldml/numbers/currencies/currency"; 
        NodeList nl_code = LDMLUtilities.getNodeList (m_doc, SearchLocation);
        for (int i=0; i < nl_code.getLength(); i++)
        {
            Vector inner = new Vector ();
            String code = LDMLUtilities.getAttributeValue (nl_code.item(i), LDMLConstants.TYPE);
            String symbol = null;
            String name = null;
            
         //   String str = SearchLocation + "[@type=" + code + "\"]/symbol";
        //    System.err.println (str);
            Node n = LDMLUtilities.getNode (m_doc, SearchLocation + "[@type=\"" + code + "\"]/symbol");
            if (n !=null) symbol = LDMLUtilities.getNodeValue (n);
            n = LDMLUtilities.getNode (m_doc, SearchLocation + "[@type=\"" + code + "\"]/displayName");
            if (n !=null) name = LDMLUtilities.getNodeValue (n);
            
            if (symbol !=null && name != null)
            {
                inner.add(0, "blank");   //dummy data so as to keep currency data is asame location : in OO.o vector pos 0 = currencyId
                inner.add(1, symbol);
                inner.add(2, code);
                inner.add(3, name);
       //       System.err.println("CLDR : symbol=" + symbol + " code=" + code + " name=" + name );
                m_CurrencyData_cldr.add(inner);
            }
        }        
        
        // new dateFormatItems
        m_DateFormatItems = m_domWrapper.getTextFromAllElements (LDMLConstants.AVAIL_FMTS, LDMLConstants.DATE_FMT_ITEM);
    }
    
    //reads OO specials only
    private void readSpecialXML ()
    {       
        //get the locale info
        String version = m_domWrapper.getAttributeFromElement(XMLNamespace.OPEN_OFFICE+":"+OpenOfficeLDMLConstants.LOCALE, XMLNamespace.OPEN_OFFICE+":"+OpenOfficeLDMLConstants.VERSION);
        if (version != null) m_LocaleInfo.put(OpenOfficeLDMLConstants.VERSION, version);
        String allowUpdateFromCLDR = m_domWrapper.getAttributeFromElement(XMLNamespace.OPEN_OFFICE+":"+OpenOfficeLDMLConstants.LOCALE, XMLNamespace.OPEN_OFFICE+":"+OpenOfficeLDMLConstants.ALLOW_UPDATE_FROM_CLDR);
        if (allowUpdateFromCLDR != null) m_LocaleInfo.put( OpenOfficeLDMLConstants.ALLOW_UPDATE_FROM_CLDR, allowUpdateFromCLDR);
        String versionDTD = m_domWrapper.getAttributeFromElement(XMLNamespace.OPEN_OFFICE+":"+OpenOfficeLDMLConstants.LOCALE, XMLNamespace.OPEN_OFFICE+":"+OpenOfficeLDMLConstants.VERSION_DTD);
        if (versionDTD != null) m_LocaleInfo.put(OpenOfficeLDMLConstants.VERSION_DTD, versionDTD);
             
        // Get default language & territory names, specific to OpenOffice.
        if (m_LangID !=null) m_LangDefaultName = m_domWrapper.getTextFromElementWithAttrib(XMLNamespace.OPEN_OFFICE+":"+OpenOfficeLDMLConstants.DEFAULT_NAME, XMLNamespace.OPEN_OFFICE+":"+OpenOfficeLDMLConstants.TYPE, m_LangID, LDMLConstants.SPECIAL, LDMLConstants.LANGUAGES);
        if (m_TerritoryID !=null) m_TerritoryDefaultName = m_domWrapper.getTextFromElementWithAttrib(XMLNamespace.OPEN_OFFICE+":"+OpenOfficeLDMLConstants.DEFAULT_NAME, XMLNamespace.OPEN_OFFICE+":"+OpenOfficeLDMLConstants.TYPE, m_TerritoryID, LDMLConstants.SPECIAL, LDMLConstants.TERRITORIES);
        m_PlatformID = m_domWrapper.getTextFromElement(LDMLConstants.SPECIAL, XMLNamespace.OPEN_OFFICE+":"+OpenOfficeLDMLConstants.PLATFORM_ID);
        m_Variant = m_domWrapper.getAttributeFromElement(LDMLConstants.IDENTITY, LDMLConstants.VARIANT, LDMLConstants.TYPE);
        
        //#######  <LC_CTYPE> sub-elements   #########
        m_DateSeparator = m_domWrapper.getTextFromElement(/*LDMLConstants.SPECIAL,*/ XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DATE_SEPARATOR); 
        m_TimeSeparator = m_domWrapper.getTextFromElement(/*LDMLConstants.SPECIAL,*/ XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TIME_SEPARATOR);
        m_Time100SecSeparator = m_domWrapper.getTextFromElement(/*LDMLConstants.SPECIAL,*/ XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TIME_100SEC_SEPARATOR);
        m_LongDateDayOfWeekSeparator = m_domWrapper.getTextFromElement(/*LDMLConstants.SPECIAL,*/ XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR);
        m_LongDateDaySeparator = m_domWrapper.getTextFromElement(/*LDMLConstants.SPECIAL,*/ XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_DAY_SEPARATOR);
        m_LongDateMonthSeparator = m_domWrapper.getTextFromElement(/*LDMLConstants.SPECIAL,*/ XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_MONTH_SEPARATOR);
        m_LongDateYearSeparator = m_domWrapper.getTextFromElement(/*LDMLConstants.SPECIAL,*/ XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_YEAR_SEPARATOR);
               
        //#######  <LC_FORMAT> sub-elements   #########
        // replaceFrom, replaceTo attributes of LC_FORMAT.
        m_FormatRefLocale = m_domWrapper.getAttributeValue(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FORMAT, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.REF);
        m_FormatReplaceFrom = m_domWrapper.getAttributeFromElement(LDMLConstants.SPECIAL, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FORMAT, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.REPLACE_FROM_SMALL);
        m_FormatReplaceTo = m_domWrapper.getAttributeFromElement(LDMLConstants.SPECIAL, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FORMAT, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.REPLACE_TO_SMALL);
        if (m_FormatRefLocale == null)
        {
            // Fixed Number
            // Hashtable of hashtables.  Outer Hashtable key=Type attribute value.  Inner hashtables=(attribute, attribute value).
            m_FixedNFormats = m_domWrapper.getAttributesFromElement(LDMLConstants.DECFL, LDMLConstants.DECIMAL_FORMAT, LDMLConstants.TYPE);
            m_FixedNPatterns = m_domWrapper.getTextFromElementsAndParentAttrib(LDMLConstants.DECIMAL_FORMAT, LDMLConstants.PATTERN, LDMLConstants.TYPE);
            m_FixedNOO = m_domWrapper.getAttributesFromElement(LDMLConstants.DECIMAL_FORMAT, LDMLConstants.SPECIAL, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.MSGTYPE);
            m_FixedDefaultName = m_domWrapper.getTextFromElementsAndGPAttrib(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT_NAME, LDMLConstants.SPECIAL, LDMLConstants.DECIMAL_FORMAT, LDMLConstants.TYPE);
            
            // Scientific Number
            m_SciNFormats = m_domWrapper.getAttributesFromElement(LDMLConstants.SCIFL, LDMLConstants.SCIENTIFIC_FORMAT, LDMLConstants.TYPE);
            m_SciNPatterns = m_domWrapper.getTextFromElementsAndParentAttrib(LDMLConstants.SCIENTIFIC_FORMAT, LDMLConstants.PATTERN, LDMLConstants.TYPE);
            m_SciNOO = m_domWrapper.getAttributesFromElement(LDMLConstants.SCIENTIFIC_FORMAT, LDMLConstants.SPECIAL, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.MSGTYPE);
            m_SciDefaultName = m_domWrapper.getTextFromElementsAndGPAttrib(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT_NAME, LDMLConstants.SPECIAL, LDMLConstants.SCIENTIFIC_FORMAT, LDMLConstants.TYPE);
            
            // Percent Number
            m_PercNFormats = m_domWrapper.getAttributesFromElement(LDMLConstants.PERFL, LDMLConstants.PERCENT_FORMAT, LDMLConstants.TYPE);
            m_PercNPatterns = m_domWrapper.getTextFromElementsAndParentAttrib(LDMLConstants.PERCENT_FORMAT, LDMLConstants.PATTERN, LDMLConstants.TYPE);
            m_PercNOO = m_domWrapper.getAttributesFromElement(LDMLConstants.PERCENT_FORMAT, LDMLConstants.SPECIAL, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.MSGTYPE);
            m_PercDefaultName = m_domWrapper.getTextFromElementsAndGPAttrib(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT_NAME, LDMLConstants.SPECIAL, LDMLConstants.PERCENT_FORMAT, LDMLConstants.TYPE);
            
            // Fraction Number
            //TODO ....
            
            // Currency Number
            m_CurrNFormats = m_domWrapper.getAttributesFromElement(LDMLConstants.CURFL, LDMLConstants.CURRENCY_FORMAT, LDMLConstants.TYPE);
            m_CurrNPatterns = m_domWrapper.getTextFromElementsAndParentAttrib(LDMLConstants.CURRENCY_FORMAT, LDMLConstants.PATTERN, LDMLConstants.TYPE);
            m_CurrNOO = m_domWrapper.getAttributesFromElement(LDMLConstants.CURRENCY_FORMAT, LDMLConstants.SPECIAL, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.MSGTYPE);
            m_CurrNDefaultName = m_domWrapper.getTextFromElementsAndGPAttrib(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT_NAME, LDMLConstants.SPECIAL, LDMLConstants.CURRENCY_FORMAT, LDMLConstants.TYPE);
            
            // Date
            m_DateFormats = m_domWrapper.getAttributesFromElement(LDMLConstants.DFL, LDMLConstants.DATE_FORMAT, LDMLConstants.TYPE);
            m_DatePatterns = m_domWrapper.getTextFromElementsAndParentAttrib(LDMLConstants.DATE_FORMAT, LDMLConstants.PATTERN, LDMLConstants.TYPE);
            m_DateOO = m_domWrapper.getAttributesFromElement(LDMLConstants.DATE_FORMAT, LDMLConstants.SPECIAL, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.MSGTYPE);
            m_DateDefaultName = m_domWrapper.getTextFromElementsAndGPAttrib(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT_NAME, LDMLConstants.SPECIAL, LDMLConstants.DATE_FORMAT, LDMLConstants.TYPE);
            
            // Time
            m_TimeFormats = m_domWrapper.getAttributesFromElement(LDMLConstants.TFL, LDMLConstants.TIME_FORMAT, LDMLConstants.TYPE);
            m_TimePatterns = m_domWrapper.getTextFromElementsAndParentAttrib(LDMLConstants.TIME_FORMAT, LDMLConstants.PATTERN, LDMLConstants.TYPE);
            m_TimeOO = m_domWrapper.getAttributesFromElement(LDMLConstants.TIME_FORMAT, LDMLConstants.SPECIAL, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.MSGTYPE);
            m_TimeDefaultName = m_domWrapper.getTextFromElementsAndGPAttrib(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT_NAME, LDMLConstants.SPECIAL, LDMLConstants.TIME_FORMAT, LDMLConstants.TYPE);
            
            // Date time
            m_DateTimeFormats = m_domWrapper.getAttributesFromElement(LDMLConstants.DTFL, LDMLConstants.DATE_TIME_FORMAT, LDMLConstants.TYPE);
            m_DateTimePatterns = m_domWrapper.getTextFromElementsAndParentAttrib(LDMLConstants.DATE_TIME_FORMAT, LDMLConstants.PATTERN, LDMLConstants.TYPE);
            m_DateTimeOO = m_domWrapper.getAttributesFromElement(LDMLConstants.DATE_TIME_FORMAT, LDMLConstants.SPECIAL, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.MSGTYPE);
            m_DateTimeDefaultName = m_domWrapper.getTextFromElementsAndGPAttrib(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT_NAME, LDMLConstants.SPECIAL, LDMLConstants.DATE_TIME_FORMAT, LDMLConstants.TYPE);
        }
        
        //#######  <LC_COLLATION> sub-elements   #########
        m_CollationRefLocale = m_domWrapper.getAttributeValue(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.COLLATIONS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.REF);
        if (m_CollationRefLocale == null)
        {
            m_Collators = m_domWrapper.getAttributesFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.COLLATIONS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.COLLATOR);
            m_CollationOptions = m_domWrapper.getTextFromAllElements(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.COLLATION_OPTIONS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TRANSLITERATION_MODULES);
        }
        
        //#######  <LC_SEARCH> sub-elements   #########
        m_SearchRefLocale = m_domWrapper.getAttributeValue(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.SEARCH, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.REF);
        if (m_SearchRefLocale == null)
        {
            m_SearchOptions = m_domWrapper.getTextFromAllElements(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.SEARCH_OPTIONS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TRANSLITERATION_MODULES);
        }
        
        //#######  <LC_INDEX> sub-elements   #########
        m_IndexRefLocale = m_domWrapper.getAttributeValue(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.INDEX, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.REF);
        if (m_IndexRefLocale == null)
        {
            m_IndexKeys = m_domWrapper.getAttributesAndTextFromAllElements(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.INDEX, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.INDEX_KEY);
            m_IndexUnicodeScript = m_domWrapper.getTextFromAllElements(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.INDEX, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.UNICODE_SCRIPT);
            m_IndexFollowPageWord = m_domWrapper.getTextFromAllElements(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.INDEX, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FOLLOW_PAGE_WORD);
        }     
        
        //currency
        String SearchLocation = "//ldml/numbers/currencies/currency"; 
        NodeList ns = LDMLUtilities.getNodeList (m_doc, SearchLocation + "/special");
        NodeList nl_id = LDMLUtilities.getNodeList (m_doc, SearchLocation + "/special/openOffice:currency/openOffice:currencyId", ns.item(0));
        NodeList nl_symbol = LDMLUtilities.getNodeList (m_doc, SearchLocation + "/symbol");
        NodeList nl_code = LDMLUtilities.getNodeList (m_doc, SearchLocation);
        NodeList nl_name = LDMLUtilities.getNodeList (m_doc, SearchLocation + "/displayName");
        NodeList nl_attrs = LDMLUtilities.getNodeList (m_doc, SearchLocation + "/special/openOffice:currency", ns.item(0));  //default,usedInCompatibelFormatCodes,legacyOnly
        for (int i=0; i < nl_code.getLength(); i++)
        {
            Vector inner = new Vector ();
            String id = LDMLUtilities.getNodeValue (nl_id.item(i));
            inner.add (0, id);
            String symbol = LDMLUtilities.getNodeValue (nl_symbol.item(i));
            inner.add (1, symbol);
            String code = LDMLUtilities.getAttributeValue (nl_code.item(i), LDMLConstants.TYPE);
            inner.add (2, code);
            String name = LDMLUtilities.getNodeValue (nl_name.item(i));
            inner.add (3, name); 
            //4 = decimal 
            inner.add (4, "blank");   //dummy data so as to keep currency data is asame location
            String def = LDMLUtilities.getAttributeValue (nl_attrs.item(i), "openOffice:"+OOConstants.DEFAULT);
            inner.add (5, def);
            String uicfc = LDMLUtilities.getAttributeValue (nl_attrs.item(i), "openOffice:"+OOConstants.USED_IN_COMPARTIBLE_FORMATCODES_SMALL);
            inner.add (6, uicfc);
            String legacyOnly = LDMLUtilities.getAttributeValue (nl_attrs.item(i), "openOffice:"+OOConstants.LEGACY_ONLY);
            if (legacyOnly != null) inner.add (7, legacyOnly);   //only optional one
            
      //      System.err.println ("OOo  : id=" + id + " symbol=" + symbol + " code=" + code + " name=" + name + " default=" + def + " usedInComp...=" + uicfc);
            m_CurrencyData_ooo.add (inner);
        }
        
        
        
        //#######  <LC_TRANSLITERATION> sub-elements   #########
        m_TransliterationRefLocale = m_domWrapper.getAttributeValue(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TRANSLITERATIONS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.REF);
        if (m_TransliterationRefLocale == null)
            m_TransliterationAtts = m_domWrapper.getAttributesFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TRANSLITERATIONS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TRANSLITERATION);
        
        //#######  <LC_MISC> sub-elements   #########
        //reserved words
        m_ReservedRefLocale = m_domWrapper.getAttributeValue(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.RESERVED_WORDS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.REF);
        if (m_ReservedRefLocale == null)
        {
            m_ReservedWords = new Hashtable();
            
            String text;
            text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TRUE_WORD);
            if (text != null) m_ReservedWords.put(OpenOfficeLDMLConstants.TRUE_WORD, text);
            text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FALSE_WORD);
            if (text != null) m_ReservedWords.put(OpenOfficeLDMLConstants.FALSE_WORD, text);
            
            text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.ABOVE_WORD); 
            if (text != null) m_ReservedWords.put(OpenOfficeLDMLConstants.ABOVE_WORD, text);
           text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.BELOW_WORD);
            if (text != null) m_ReservedWords.put(OpenOfficeLDMLConstants.BELOW_WORD, text);
            
            //Quarters is in cldr 1.4
            if (m_cldr_ver < 1.399)
            {
                text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.QUARTER_1_WORD);
                if (text != null) m_WideQuarters.put(OpenOfficeLDMLConstants.QUARTER_1_WORD, text);
                text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.QUARTER_2_WORD);
                if (text != null) m_WideQuarters.put(OpenOfficeLDMLConstants.QUARTER_2_WORD, text);
                text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.QUARTER_3_WORD);
                if (text != null) m_WideQuarters.put(OpenOfficeLDMLConstants.QUARTER_3_WORD, text);
                text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.QUARTER_4_WORD);
                if (text != null) m_WideQuarters.put(OpenOfficeLDMLConstants.QUARTER_4_WORD, text);
 
                text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.QUARTER_1_ABBREVIATION);
                if (text != null) m_AbbrQuarters.put(OpenOfficeLDMLConstants.QUARTER_1_ABBREVIATION, text);
                text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.QUARTER_2_ABBREVIATION);
                if (text != null) m_AbbrQuarters.put(OpenOfficeLDMLConstants.QUARTER_2_ABBREVIATION, text);
                text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.QUARTER_3_ABBREVIATION);
                if (text != null) m_AbbrQuarters.put(OpenOfficeLDMLConstants.QUARTER_3_ABBREVIATION, text);
                text = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.QUARTER_4_ABBREVIATION);
                if (text != null) m_AbbrQuarters.put(OpenOfficeLDMLConstants.QUARTER_4_ABBREVIATION, text);
            }
        }
        
        m_ForbiddenRefLocale = m_domWrapper.getAttributeValue(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS, OpenOfficeLDMLConstants.REF);
        if (m_ForbiddenRefLocale == null)
        {
            m_ForbiddenLineBeg = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FORBIDDEN_LINE_BEGIN_CHARACTERS);
            m_ForbiddenLineEnd = m_domWrapper.getTextFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FORBIDDEN_LINE_END_CHARACTERS);
        }
        
        //#######  <LC_NumberingLevel> sub-elements   #########
        m_NumberingRefLocale = m_domWrapper.getAttributeValue(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.NUMBERING_LEVELS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.REF);
        if (m_NumberingRefLocale == null)
            m_NumberingLevelAtts = m_domWrapper.getAttributesFromElement(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.NUMBERING_LEVELS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.NUMBERING_LEVEL);
        
        //#######  <LC_OutlineNumberingLevel> sub-elements   #########
        m_OutlineNumberingRefLocale = m_domWrapper.getAttributeValue(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVELS, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.REF);
        if (m_OutlineNumberingRefLocale == null)
            m_OutlineNumberingLevels = m_domWrapper.getAttributesFromElementGroups(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.OUTLINE_STYLE, XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVEL);
                
        
    }
}
