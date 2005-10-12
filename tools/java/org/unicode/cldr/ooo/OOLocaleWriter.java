/************************************************************************
 * Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
 *************************************************************************/

package org.unicode.cldr.ooo;

import java.io.*;
import java.util.*;

/**
 *
 * class has ONLY generic methods for writing to LDML
 * these methods are platform independent
 */

public class OOLocaleWriter extends XMLWriter
{
    private Hashtable m_Aliases;
    
    /** Creates a new instance of OOLocaleWriter */
    public OOLocaleWriter(PrintStream out)
    {
        super(out);
    }
    
    public OOLocaleWriter(PrintStream out, PrintStream err)
    {
        super(out, err);
    }
    
    protected void indent()
    {
        indent(1);
    }
    protected void outdent()
    {
        outdent(1);
    }
    
    public void open(Hashtable localeInfo)
    {
        println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        String dtd = "<!DOCTYPE Locale SYSTEM \'locale.dtd\'>";
        println(dtd);
        
        StringBuffer localeStrBuff = new StringBuffer("<" + OOConstants.LOCALE);
        
        String str = (String) localeInfo.get(OOConstants.VERSION_DTD);
        if (str != null)
            localeStrBuff.append(" " + OOConstants.VERSION_DTD + "=\"" + str + "\"");
        
        str = (String) localeInfo.get(OOConstants.ALLOW_UPDATE_FROM_CLDR);
        if (str != null)
            localeStrBuff.append(" " + OOConstants.ALLOW_UPDATE_FROM_CLDR + "=\"" + str + "\"");
        
        str = (String) localeInfo.get(OOConstants.VERSION);
        if (str != null)
            localeStrBuff.append(" " + OOConstants.VERSION + "=\"" + str + "\"");
        
        localeStrBuff.append(">");
        println(localeStrBuff.toString());
        indent();
    }
    
    
    public void close()
    {
        outdent();
        println("</" + OOConstants.LOCALE + ">");
    }
    
    protected void print(String val)
    {
        if (needsIndent)
        {
            out.print(indentString);
            needsIndent = false;
        }
        out.print(val);
    }
    
    protected void println(String val)
    {
        print(val);
        out.println();
        lineLength = 0;
        needsIndent = true;
    }
    
    public void setAliases(Hashtable aliases)
    {
        if (aliases != null)
            m_Aliases = aliases;
    }
    
    public void writeLC_INFO(Hashtable data)
    {
        println("<" + OOConstants.LC_INFO + ">");
        indent();
        
        // LANGUAGE
        println("<" + OOConstants.LANGUAGE + ">");
        indent();
        // LangID
        String langID = (String) data.get(OOConstants.LANG_ID);
        println("<" + OOConstants.LANG_ID + ">" + langID + "</" + OOConstants.LANG_ID + ">");
        
        String language = (String) data.get(OOConstants.LANGUAGE);
        if ((language != null) && (language.length()>0))
        {
            println("<" + OOConstants.DEFAULT_NAME + ">" + language + "</" + OOConstants.DEFAULT_NAME + ">");
        }
        outdent();
        println("</" + OOConstants.LANGUAGE + ">");
        
        
        // COUNTRY
        println("<" + OOConstants.COUNTRY + ">");
        indent();
        //CountryID
        String countryID = (String) data.get(OOConstants.COUNTRY_ID);
        if (countryID != null)
            println("<" + OOConstants.COUNTRY_ID + ">" + countryID + "</" + OOConstants.COUNTRY_ID + ">");
        else
            println("<" + OOConstants.COUNTRY_ID + "/>");  //mandatory element in OO locale.dtd
        
        String country = (String) data.get(OOConstants.COUNTRY);
        if ((country != null) && (country.length()>0))
            println("<" + OOConstants.DEFAULT_NAME + ">" + country + "</" + OOConstants.DEFAULT_NAME + ">");
        else
            println("<" + OOConstants.DEFAULT_NAME + "/>");   //mandatory element in OO locale.dtd
        
        outdent();
        println("</" + OOConstants.COUNTRY + ">");
        
        // Platform
        String platformID = (String) data.get(OOConstants.PLATFORM_ID);
        if (platformID != null)
        {
            println("<" + OOConstants.PLATFORM + ">");
            indent();
            
            println("<" + OOConstants.PLATFORM_ID + ">" + platformID + "</" + OOConstants.PLATFORM_ID + ">");
            
            outdent();
            println("</" + OOConstants.PLATFORM + ">");
        }
        
        // Variant
        String variant = (String) data.get(OOConstants.VARIANT);
        if (variant != null)
            println("<" + OOConstants.VARIANT + ">" + variant + "</" + OOConstants.VARIANT + ">");
        
        outdent();
        println("</" + OOConstants.LC_INFO + ">");
    }
    
    public void writeLC_CTYPE(Hashtable data)
    {
        // first check if any LC_CTYPE exists, since all fields are optional.
        // If data exists, print out the tag name.
        
        // for Separators, Markers, TimeAM, TimePM, MeasurementSystem,
        // must check each for any data contained within them.
        
        if ((data == null) || (data.size()<1))
            return;
        
        println("<" + OOConstants.LC_CTYPE + ">");
        indent();
        
        // Separators
        Hashtable Separators = (Hashtable) data.get(OOConstants.SEPARATORS);
        if ((Separators != null) && (Separators.size()>0))
        {
            println("<" + OOConstants.SEPARATORS + ">");
            indent();
            
            //if data not present then print <element/> as these are mandatory in Oo locale.dtd
            String dateSeparator = (String) Separators.get(OOConstants.DATE_SEPARATOR);
            if ((dateSeparator != null) && (dateSeparator.length()>0))
                println("<" + OOConstants.DATE_SEPARATOR + ">" + dateSeparator + "</" + OOConstants.DATE_SEPARATOR + ">");
            else
                println("<" + OOConstants.DATE_SEPARATOR + "/>");
            
            String thousandSeparator = (String) Separators.get(OOConstants.THOUSAND_SEPARATOR);
            if ((thousandSeparator != null) && (thousandSeparator.length()>0))
                println("<" + OOConstants.THOUSAND_SEPARATOR + ">" + thousandSeparator + "</" + OOConstants.THOUSAND_SEPARATOR + ">");
            else
                println("<" + OOConstants.THOUSAND_SEPARATOR + "/>");
            
            String decimalSeparator = (String) Separators.get(OOConstants.DECIMAL_SEPARATOR);
            if ((decimalSeparator != null) && (decimalSeparator.length()>0))
                println("<" + OOConstants.DECIMAL_SEPARATOR + ">" + decimalSeparator + "</" + OOConstants.DECIMAL_SEPARATOR + ">");
            else
                println("<" + OOConstants.DECIMAL_SEPARATOR + "/>");
            
            String timeSeparator = (String) Separators.get(OOConstants.TIME_SEPARATOR);
            if ((timeSeparator != null) && (timeSeparator.length()>0))
                println("<" + OOConstants.TIME_SEPARATOR + ">" + timeSeparator + "</" + OOConstants.TIME_SEPARATOR + ">");
            else
                println("<" + OOConstants.TIME_SEPARATOR + "/>");
            
            String time100SecSeparator = (String) Separators.get(OOConstants.TIME_100SEC_SEPARATOR);
            if ((time100SecSeparator != null) && (time100SecSeparator.length()>0))
                println("<" + OOConstants.TIME_100SEC_SEPARATOR + ">" + time100SecSeparator + "</" + OOConstants.TIME_100SEC_SEPARATOR + ">");
            else
                println("<" + OOConstants.TIME_100SEC_SEPARATOR + "/>" );
            
            String listSeparator = (String) Separators.get(OOConstants.LIST_SEPARATOR);
            if ((listSeparator != null) && (listSeparator.length()>0))
                println("<" + OOConstants.LIST_SEPARATOR + ">" + listSeparator + "</" + OOConstants.LIST_SEPARATOR + ">");
            else
                println("<" + OOConstants.LIST_SEPARATOR + "/>");
            
            String longDateDayOfWeekSeparator = (String) Separators.get(OOConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR);
            if ((longDateDayOfWeekSeparator != null) && (longDateDayOfWeekSeparator.length()>0))
                println("<" + OOConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR + ">" + longDateDayOfWeekSeparator + "</" + OOConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR + ">");
            else
                println("<" + OOConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR + "/>");
            
            String longDateDaySeparator = (String) Separators.get(OOConstants.LONG_DATE_DAY_SEPARATOR);
            if ((longDateDaySeparator != null) && (longDateDaySeparator.length()>0))
                println("<" + OOConstants.LONG_DATE_DAY_SEPARATOR + ">" + longDateDaySeparator + "</" + OOConstants.LONG_DATE_DAY_SEPARATOR + ">");
            else
                println("<" + OOConstants.LONG_DATE_DAY_SEPARATOR + "/>");
            
            String longDateMonthSeparator = (String) Separators.get(OOConstants.LONG_DATE_MONTH_SEPARATOR);
            if ((longDateMonthSeparator != null) && (longDateMonthSeparator.length()>0))
                println("<" + OOConstants.LONG_DATE_MONTH_SEPARATOR + ">" + longDateMonthSeparator + "</" + OOConstants.LONG_DATE_MONTH_SEPARATOR + ">");
            else
                println("<" + OOConstants.LONG_DATE_MONTH_SEPARATOR + "/>");
            
            String longDateYearSeparator = (String) Separators.get(OOConstants.LONG_DATE_YEAR_SEPARATOR);
            if ((longDateYearSeparator != null) && (longDateYearSeparator.length()>0))
                println("<" + OOConstants.LONG_DATE_YEAR_SEPARATOR + ">" + longDateYearSeparator + "</" + OOConstants.LONG_DATE_YEAR_SEPARATOR + ">");
            else
                println("<" + OOConstants.LONG_DATE_YEAR_SEPARATOR + "/>");
            
            outdent();
            println("</" + OOConstants.SEPARATORS + ">");
        }
        
        // Markers
        Hashtable Markers = (Hashtable) data.get(OOConstants.MARKERS);
        if ((Markers != null) && (Markers.size()>0))
        {
            println("<" + OOConstants.MARKERS + ">");
            indent();
            
            String quotationStart = (String) Markers.get(OOConstants.QUOTATION_START);
            if ((quotationStart != null) && (quotationStart.length()>0))
                println("<" + OOConstants.QUOTATION_START + ">" + quotationStart + "</" + OOConstants.QUOTATION_START + ">");
            String quotationEnd = (String) Markers.get(OOConstants.QUOTATION_END);
            if ((quotationEnd != null) && (quotationEnd.length()>0))
                println("<" + OOConstants.QUOTATION_END + ">" + quotationEnd + "</" + OOConstants.QUOTATION_END + ">");
            String doubleQuotationStart = (String) Markers.get(OOConstants.DOUBLE_QUOTATION_START);
            if ((doubleQuotationStart != null) && (doubleQuotationStart.length()>0))
                println("<" + OOConstants.DOUBLE_QUOTATION_START + ">" + doubleQuotationStart + "</" + OOConstants.DOUBLE_QUOTATION_START + ">");
            String doubleQuotationEnd = (String) Markers.get(OOConstants.DOUBLE_QUOTATION_END);
            if ((doubleQuotationEnd != null) && (doubleQuotationEnd.length()>0))
                println("<" + OOConstants.DOUBLE_QUOTATION_END + ">" + doubleQuotationEnd + "</" + OOConstants.DOUBLE_QUOTATION_END + ">");
            
            outdent();
            println("</" + OOConstants.MARKERS + ">");
        }
        
        // TimeAM, TimePM
        String timeAM = (String) data.get(OOConstants.TIME_AM);
        if ((timeAM != null) && (timeAM.length()>0))
            println("<" + OOConstants.TIME_AM + ">" + timeAM + "</" + OOConstants.TIME_AM + ">");
        String timePM = (String) data.get(OOConstants.TIME_PM);
        if ((timePM != null) && (timePM.length()>0))
            println("<" + OOConstants.TIME_PM + ">" + timePM + "</" + OOConstants.TIME_PM + ">");
        
        // MeasurementSystem
        String measurementSystem = (String) data.get(OOConstants.MEASUREMENT_SYSTEM);
        if ((measurementSystem != null) && (measurementSystem.length()>0))
            println("<" + OOConstants.MEASUREMENT_SYSTEM + ">" + measurementSystem + "</" + OOConstants.MEASUREMENT_SYSTEM + ">");
        
        
        outdent();
        println("</" + OOConstants.LC_CTYPE + ">");
        
    }
    
    //use this when a ref was written
    public boolean writeLC_FORMATRef(String replaceFrom, String replaceTo, String ref)
    {
        StringBuffer lcFormat = new StringBuffer("<" + OOConstants.LC_FORMAT);
        
        if (ref != null)
            lcFormat.append(" " + OOConstants.REF + "=\"" + ref + "\"");
        if (replaceFrom != null)
            lcFormat.append(" " + OOConstants.REPLACE_FROM_SMALL + "=\"" + replaceFrom + "\"");
        if (replaceTo != null)
            lcFormat.append(" " + OOConstants.REPLACE_TO_SMALL + "=\"" + replaceTo + "\"");
        
        lcFormat.append("/>");
        println(lcFormat.toString());
        return true;
    }
    
    // Called multiple times for each of dateFormat, timeFormat etc.
    // Return true if the <LC_FORMAT> tag has been written.  Need to know this
    // to close when finished.
    //inFormatElements contains all the attribute data of the FormatElement element
    //inFormatElements is Hashtable of Hashtables, outer key = msgid, inner key = attr name, inner value = attr value
    public boolean writeLC_FORMAT(Hashtable inFormatElements, Hashtable inFormatCodes, Hashtable inFormatDefaultNames, boolean inHasOpened, String replaceFrom, String replaceTo)
    {
        boolean m_hasOpened = inHasOpened;
        
        if ((inFormatElements == null) || (inFormatCodes == null))
            return m_hasOpened;
        
        if (!m_hasOpened)
        {
            StringBuffer lcFormat = new StringBuffer("<" + OOConstants.LC_FORMAT);
            if ((replaceFrom != null) && (replaceTo != null))
                lcFormat.append(" " + OOConstants.REPLACE_FROM_SMALL + "=\"" + replaceFrom + "\" " + OOConstants.REPLACE_TO_SMALL + "=\"" + replaceTo + "\"");
            lcFormat.append(">");
            
            println(lcFormat.toString());
            indent();
            m_hasOpened = true;
        }
        
        String defaultKey = null;
        String type = null;
        String usage = null;
        String formatIndex = null;
        String formatCode = null;
        String defaultName = null;
        
        // want to print out in order of increasing formatindex
        
        // get the formatindex values
        TreeSet fi_values = new TreeSet();
        Enumeration data = inFormatElements.elements();
        while (data.hasMoreElements() == true)
        {
            Hashtable elements = (Hashtable) data.nextElement();
            formatIndex = (String) elements.get(OOConstants.FORMAT_INDEX);
            fi_values.add(formatIndex);
        }
        
        //data in fi[] is ordered so the for loop will rpint out ordered by formatindex
        Object [] fi =  fi_values.toArray();
        for (int i=0; i < fi.length ; i++)
        {
            //         System.out.println("formatindex = " + (String) fi[i]);
            // hashtables in argument basically in OO XML <FormatElement> structure.
            Enumeration keys = inFormatElements.keys();
            while (keys.hasMoreElements() == true)
            {
                // type/msgid
                String key = (String) keys.nextElement();
                if ((key != null) && (key.length()>0))
                {
                    Hashtable elementAttribs = (Hashtable) inFormatElements.get(key);
                    if ((elementAttribs != null) && (elementAttribs.size()>0))
                    {
                        formatIndex = (String) elementAttribs.get(OOConstants.FORMAT_INDEX);
                        //print them in order, they are already ordered in final []
                        if (formatIndex.compareTo((String)fi[i]) !=0)
                            continue;
                        
                        // Piece together the first line of the FormatElement
                        // with all its attributes.
                        StringBuffer formatLine = new StringBuffer("<" + OOConstants.FORMAT_ELEMENT + " " + OOConstants.MSGID + "=\"" + key + "\"");
                        
                        defaultKey = (String) elementAttribs.get(OOConstants.DEFAULT);
                        if ((defaultKey != null) && (defaultKey.length()>0))
                            formatLine.append(" " + OOConstants.DEFAULT + "=\"" + defaultKey + "\"");
                        
                        type = (String) elementAttribs.get(OOConstants.TYPE);
                        if ((type != null) && (type.length()>0))
                            formatLine.append(" " + OOConstants.TYPE + "=\"" + type + "\"");
                        
                        usage = (String) elementAttribs.get(OOConstants.USAGE);
                        if ((usage != null) && (usage.length()>0))
                            formatLine.append(" " + OOConstants.USAGE + "=\"" + usage + "\"");
                        
                        if ((formatIndex != null) && (formatIndex.length()>0))
                            formatLine.append(" " + OOConstants.FORMAT_INDEX + "=\"" + formatIndex + "\"");
                        
                        // Write as a sub element below.
                        defaultName = (String) inFormatDefaultNames.get(key);
                        
                        formatLine.append(">");
                        println(formatLine.toString());
                        indent();
                        
                        // Sub element: FormatCode
                        formatCode = (String) inFormatCodes.get(key);
                        if  ((formatCode != null) && (formatCode.length()>0))
                            println("<" + OOConstants.FORMAT_CODE + ">" + formatCode + "</" + OOConstants.FORMAT_CODE + ">");
                        
                        // Sub element: DefaultName
                        if ((defaultName != null) && (defaultName.length()>0))
                            println("<" + OOConstants.DEFAULT_NAME + ">" + defaultName + "</" + OOConstants.DEFAULT_NAME + ">");
                        // DefaultName is an optional element.
                        
                        outdent();
                        println("</" + OOConstants.FORMAT_ELEMENT + ">");
                        
                    }
                }
            }
        }
        return m_hasOpened;
    }
    
    public void writeCloseLC_FORMAT(boolean hasOpened)
    {
        if (hasOpened)
        {
            outdent();
            println("</" + OOConstants.LC_FORMAT + ">");
        }
    }
    
    public void writeLC_COLLATION(Hashtable data, String ref)
    {
        boolean noSubElems = false;
        
        StringBuffer lcCollation = new StringBuffer("<" + OOConstants.LC_COLLATION);
        //    String refLocale = (String) data.get(OOConstants.REF);
        //   if ((refLocale != null) && (refLocale.length()>0))
        if ((ref != null) && (ref.length()>0))
        {
            lcCollation.append(" " + OOConstants.REF + "=\"" + ref + "\"");
        }
        
        Vector collators = (Vector) data.get(OOConstants.COLLATOR);
        Vector collationOptions = (Vector) data.get(OOConstants.COLLATION_OPTIONS);
        if ((collators == null) && (collationOptions == null))
        {
            lcCollation.append(" />");
            noSubElems = true;
            
        }
        else
            lcCollation.append(">");
        
        println(lcCollation.toString());
        if (noSubElems)
            return;
        
        indent();
        
        Enumeration collatorKeys = collators.elements();
        while (collatorKeys.hasMoreElements() == true)
        {
            Hashtable collator = (Hashtable) collatorKeys.nextElement();
            if ((collator != null) && (collator.size()>0))
            {
                StringBuffer collStr = new StringBuffer("<" + OOConstants.COLLATOR);
                Enumeration collKeys = collator.keys();
                while (collKeys.hasMoreElements())
                {
                    // Print out each attribute of the Collator
                    String key = (String) collKeys.nextElement();
                    String value = (String) collator.get(key);
                    if ((key != null) && (key.length()>0) && (value != null))
                    {
                        collStr.append(" " + key + "=\"" + value + "\"");
                    }
                }//end while
                collStr.append(" />");
                println(collStr.toString());
            }
        }//end while
        
        // CollationOptions
        Enumeration collationOptsKeys = collationOptions.elements();
        while (collationOptsKeys.hasMoreElements() == true)
        {
            String transliterationModule = (String) collationOptsKeys.nextElement();
            if ((transliterationModule != null) && (transliterationModule.length()>0))
            {
                println("<" + OOConstants.COLLATION_OPTIONS + ">");
                indent();
                println("<" + OOConstants.TRANSLITERATION_MODULES + ">" + transliterationModule + "</" + OOConstants.TRANSLITERATION_MODULES + ">");
                outdent();
                println("</" + OOConstants.COLLATION_OPTIONS + ">");
            }
        }//end while
        
        outdent();
        println("</" + OOConstants.LC_COLLATION + ">");
    }
    
    public void writeLC_SEARCH(Vector searchOptions, String ref)
    {
        boolean noSubElems = false;
        
        //if these a ref then these are no sub elements
        if ((ref != null) && (ref.length()>0))
        {
            println("<" + OOConstants.LC_SEARCH + " " + OOConstants.REF + "=\"" + ref + "\"/>");
        }
        else if ((searchOptions != null) && (searchOptions.size()>0))
        {
            println("<" + OOConstants.LC_SEARCH + ">");
            indent();
            println("<" + OOConstants.SEARCH_OPTIONS + ">");
            indent();
            
            // SearchOptions
            Enumeration searchKeys = searchOptions.elements();
            while (searchKeys.hasMoreElements() == true)
            {
                String transliterationModule = (String) searchKeys.nextElement();
                if ((transliterationModule != null) && (transliterationModule.length()>0))
                {
                    println("<" + OOConstants.TRANSLITERATION_MODULES + ">" + transliterationModule + "</" + OOConstants.TRANSLITERATION_MODULES + ">");
                }
            }//end while
            
            outdent();
            println("</" + OOConstants.SEARCH_OPTIONS + ">");
            outdent();
            println("</" + OOConstants.LC_SEARCH + ">");
        }
    }
    
    public void writeLC_INDEX(Hashtable indexData, String ref)
    {
        //if these a ref then these are no sub elements
        if ((ref != null) && (ref.length()>0))
        {
            println("<" + OOConstants.LC_INDEX + " " + OOConstants.REF + "=\"" + ref + "\"/>");
        }
        else if ((indexData != null) && (indexData.size()>0))
        {
            println("<" + OOConstants.LC_INDEX + ">");
            indent();
            
            // indexKeys
            Vector indexKeys = (Vector) indexData.get(OOConstants.INDEX_KEY);
            if ((indexKeys != null) && (indexKeys.size()>0))
            {
                Enumeration indexkeysEnum = indexKeys.elements();
                while (indexkeysEnum.hasMoreElements() == true)
                {
                    Hashtable indexKey = (Hashtable) indexkeysEnum.nextElement();
                    if ((indexKey != null) && (indexKey.size()>0))
                    {
                        String indexkeyText = (String) indexKey.get("innerText");
                        Hashtable attributes = (Hashtable) indexKey.get("attributes");
                        
                        StringBuffer indexkeyStr = new StringBuffer("<" + OOConstants.INDEX_KEY);
                        // place the attributes
                        Enumeration attributeKeys = attributes.keys();
                        while (attributeKeys.hasMoreElements())
                        {
                            String key = (String) attributeKeys.nextElement();
                            String value = (String) attributes.get(key);
                            
                            if ((key != null) && (key.length()>0) && (value != null))
                            {
                                indexkeyStr.append(" " + key + "=\"" + value + "\"");
                            }
                            
                        }
                        if ((indexkeyText != null) && (indexkeyText.length()>0))
                            indexkeyStr.append(">" + indexkeyText + "</" + OOConstants.INDEX_KEY + ">");
                        else
                            indexkeyStr.append(" />");
                        println(indexkeyStr.toString());
                    }
                    
                }//end while
            }
            
            //unicode scripts
            Vector unicodeScripts = (Vector) indexData.get(OOConstants.UNICODE_SCRIPT);
            if ((unicodeScripts != null) && (unicodeScripts.size()>0))
            {
                Enumeration usEnum = unicodeScripts.elements();
                while (usEnum.hasMoreElements())
                {
                    String us = (String) usEnum.nextElement();
                    if ((us != null) && (us.length()>0))
                    {
                        println("<" + OOConstants.UNICODE_SCRIPT + ">" + us + "</" + OOConstants.UNICODE_SCRIPT + ">");
                    }
                }
            }
            
            //followPageWord
            Vector followWordPages = (Vector) indexData.get(OOConstants.FOLLOW_PAGE_WORD);
            if ((followWordPages != null) && (followWordPages.size()>0))
            {
                Enumeration fwpEnum = followWordPages.elements();
                while (fwpEnum.hasMoreElements())
                {
                    String fwp = (String) fwpEnum.nextElement();
                    if ((fwp != null) && (fwp.length()>0))
                    {
                        println("<" + OOConstants.FOLLOW_PAGE_WORD + ">" + fwp + "</" + OOConstants.FOLLOW_PAGE_WORD + ">");
                    }
                }
            }
            
            outdent();
            println("</" + OOConstants.LC_INDEX + ">");
        }
    }
    
    public void writeLC_CALENDAR(Hashtable data)
    {
        if ((data == null) || (data.size()==0))
            return;
        
        println("<" + OOConstants.LC_CALENDAR + ">");
        indent();
        
        // Loop around for each type of calenda
        Vector calendarTypes = (Vector) data.get(OOConstants.UNOID);
        if ((calendarTypes != null) && (calendarTypes.size()>0))
        {
            String defaultCal = (String) data.get(OOConstants.DEFAULT);
            
            Enumeration calEnum = calendarTypes.elements();
            while (calEnum.hasMoreElements())
            {
                Hashtable calendar = (Hashtable) calEnum.nextElement();
                if (calendar != null)
                {
                    String calendarType = (String) calendar.get(OOConstants.UNOID);
                    if ((calendarType != null) && (calendarType.length()>0))
                    {
                        // Print out data relevant to this calendar. e.g. gregorian.
                        StringBuffer calTag = new StringBuffer("<" + OOConstants.CALENDAR + " " + OOConstants.UNOID + "=\"" + calendarType + "\"");
                        if ((defaultCal != null) && (defaultCal.compareTo(calendarType)==0))
                            calTag.append(" " + OOConstants.DEFAULT + "=\"" + OOConstants.TRUE + "\"");
                        else
                            calTag.append(" " + OOConstants.DEFAULT + "=\"" + OOConstants.FALSE + "\"");
                        calTag.append(">");
                        println(calTag.toString());
                        indent();
                        
                        writeDays(data, calendarType);
                        writeMonths(data, calendarType);
                        writeEras(data, calendarType);
                        writeStartOfWeek(data, calendarType);
                        writeMinDays(data, calendarType);
                        
                        outdent();
                        println("</" + OOConstants.CALENDAR + ">");
                    }
                }
            }
            // CONTINUE HERE LOOPING THROUGH CALENDAR TYPES.
        }
        
        outdent();
        println("</" + OOConstants.LC_CALENDAR + ">");
    }
    
    private void writeDays(Hashtable data, String unoid)
    {
        
        Hashtable daysAbbr = (Hashtable) data.get(OOConstants.DAYS_OF_WEEK + "." + OOConstants.DEFAULT_ABBRV_NAME);
        Hashtable daysFull = (Hashtable) data.get(OOConstants.DAYS_OF_WEEK + "." + OOConstants.DEFAULT_FULL_NAME);
        
        if (( (daysAbbr != null) && (daysAbbr.size()>0) ) || ( (daysFull != null) && (daysFull.size()>0) ))
        {
            // find data specific to this unoid, e.g. gregorian
            
            Hashtable daysAbbrvUnoid = null;
            Hashtable daysFullUnoid = null;
            if (daysAbbr != null) daysAbbrvUnoid = (Hashtable) daysAbbr.get(unoid);
            if (daysFull != null) daysFullUnoid = (Hashtable) daysFull.get(unoid);
            
            //add workaround as getFullyResolvedLDML () doesn't implement other calendar inheritance from gregorian
            if (daysAbbrvUnoid == null) daysAbbrvUnoid = (Hashtable) daysAbbr.get(OOConstants.GREGORIAN);
            if (daysFullUnoid == null) daysFullUnoid = (Hashtable) daysFull.get(OOConstants.GREGORIAN);
            
            if (((daysAbbrvUnoid != null) && (daysAbbrvUnoid.size()>0)) || ((daysFullUnoid != null) && (daysFullUnoid.size()>0)))
            {
                
                println("<" + OOConstants.DAYS_OF_WEEK + ">");
                indent();
                
                //good - order is important as DayID is totally ignored in OO
                writeDay(OOConstants.SUN, daysAbbrvUnoid, daysFullUnoid);
                writeDay(OOConstants.MON, daysAbbrvUnoid, daysFullUnoid);
                writeDay(OOConstants.TUE, daysAbbrvUnoid, daysFullUnoid);
                writeDay(OOConstants.WED, daysAbbrvUnoid, daysFullUnoid);
                writeDay(OOConstants.THU, daysAbbrvUnoid, daysFullUnoid);
                writeDay(OOConstants.FRI, daysAbbrvUnoid, daysFullUnoid);
                writeDay(OOConstants.SAT, daysAbbrvUnoid, daysFullUnoid);
                
                outdent();
                println("</" + OOConstants.DAYS_OF_WEEK + ">");
            }
        }
    }
    
    private void writeDay(String ooDay, Hashtable daysAbbr, Hashtable daysFull)
    {
        String dayID = ooDay;
        String abbrvDay = null;
        String fullDay = null;
        if (daysAbbr != null)
            abbrvDay = (String) daysAbbr.get(dayID);
        if (daysFull != null)
            fullDay = (String) daysFull.get(dayID);
        if ((abbrvDay != null) || (fullDay != null))
        {
            println("<" + OOConstants.DAY + ">");
            indent();
            
            println("<" + OOConstants.DAY_ID + ">" + dayID + "</" + OOConstants.DAY_ID + ">");
            if (abbrvDay != null)
                println("<" + OOConstants.DEFAULT_ABBRV_NAME + ">" + abbrvDay + "</" + OOConstants.DEFAULT_ABBRV_NAME + ">");
            if (fullDay != null)
                println("<" + OOConstants.DEFAULT_FULL_NAME + ">" + fullDay + "</" + OOConstants.DEFAULT_FULL_NAME + ">");
            
            outdent();
            println("</" + OOConstants.DAY + ">");
        }
    }
    
    
    private void writeMonths(Hashtable data, String unoid)
    {
        
        Hashtable monthsAbbr = (Hashtable) data.get(OOConstants.MONTHS_OF_YEAR + "." + OOConstants.DEFAULT_ABBRV_NAME);
        Hashtable monthsFull = (Hashtable) data.get(OOConstants.MONTHS_OF_YEAR + "." + OOConstants.DEFAULT_FULL_NAME);
        
        if (( (monthsAbbr != null) && (monthsAbbr.size()>0) ) || ( (monthsFull != null) && (monthsFull.size()>0) ))
        {
            // find data specific to this unoid, e.g. gregorian
            
            Hashtable monthsAbbrvUnoid = null;
            Hashtable monthsFullUnoid = null;
            if (monthsAbbr != null) monthsAbbrvUnoid = (Hashtable) monthsAbbr.get(unoid);
            if (monthsFull != null) monthsFullUnoid = (Hashtable) monthsFull.get(unoid);
            
            //add workaround as getFullyResolvedLDML () doesn't implement other calendar inheritance from gregorian
            if (monthsAbbrvUnoid == null) monthsAbbrvUnoid = (Hashtable) monthsAbbr.get(OOConstants.GREGORIAN);
            if (monthsFullUnoid == null) monthsFullUnoid = (Hashtable) monthsFull.get(OOConstants.GREGORIAN);
            
            if (((monthsAbbrvUnoid != null) && (monthsAbbrvUnoid.size()>0)) || ((monthsFullUnoid != null) && (monthsFullUnoid.size()>0)))
            {
                
                println("<" + OOConstants.MONTHS_OF_YEAR + ">");
                indent();
                
                
                String m1 = OOConstants.MONTH_1;
                String m2 = OOConstants.MONTH_2;
                String m3 = OOConstants.MONTH_3;
                String m4 = OOConstants.MONTH_4;
                String m5 = OOConstants.MONTH_5;
                String m6 = OOConstants.MONTH_6;
                String m7 = OOConstants.MONTH_7;
                String m8 = OOConstants.MONTH_8;
                String m9 = OOConstants.MONTH_9;
                String m10 = OOConstants.MONTH_10;
                String m11 = OOConstants.MONTH_11;
                String m12 = OOConstants.MONTH_12;
                String m13 = "";   //month 13 only present in jewish calendar in OO
                if (unoid.compareTo(OOConstants.JEWISH)==0)
                {
                    m1 = OOConstants.MONTH_1_ALT;
                    m2 = OOConstants.MONTH_2_ALT;
                    m3 = OOConstants.MONTH_3_ALT;
                    m4 = OOConstants.MONTH_4_ALT;
                    m5 = OOConstants.MONTH_5_ALT;
                    m6 = OOConstants.MONTH_6_ALT;
                    m7 = OOConstants.MONTH_7_ALT;
                    m8 = OOConstants.MONTH_8_ALT;
                    m9 = OOConstants.MONTH_9_ALT;
                    m10 = OOConstants.MONTH_10_ALT;
                    m11 = OOConstants.MONTH_11_ALT;
                    m12 = OOConstants.MONTH_12_ALT;
                    m13 = OOConstants.MONTH_13_ALT;
                }
                
                //good - order is important as MonthID is totally ignored in OO
                writeMonth(m1, monthsAbbrvUnoid, monthsFullUnoid);
                writeMonth(m2, monthsAbbrvUnoid, monthsFullUnoid);
                writeMonth(m3, monthsAbbrvUnoid, monthsFullUnoid);
                writeMonth(m4, monthsAbbrvUnoid, monthsFullUnoid);
                writeMonth(m5, monthsAbbrvUnoid, monthsFullUnoid);
                writeMonth(m6, monthsAbbrvUnoid, monthsFullUnoid);
                writeMonth(m7, monthsAbbrvUnoid, monthsFullUnoid);
                writeMonth(m8, monthsAbbrvUnoid, monthsFullUnoid);
                writeMonth(m9, monthsAbbrvUnoid, monthsFullUnoid);
                writeMonth(m10, monthsAbbrvUnoid, monthsFullUnoid);
                writeMonth(m11, monthsAbbrvUnoid, monthsFullUnoid);
                writeMonth(m12, monthsAbbrvUnoid, monthsFullUnoid);
                if (unoid.compareTo(OOConstants.JEWISH)==0)
                {
                    writeMonth(m13, monthsAbbrvUnoid, monthsFullUnoid);
                }
                
                outdent();
                println("</" + OOConstants.MONTHS_OF_YEAR + ">");
            }
        }
    }
    
    
    private void writeMonth(String ooMonth, Hashtable monthsAbbr, Hashtable monthsFull)
    {
        String monthID = ooMonth;
        String abbrvMonth = null;
        String fullMonth = null;
        if (monthsAbbr != null)
            abbrvMonth = (String) monthsAbbr.get(monthID);
        if (monthsFull != null)
            fullMonth = (String) monthsFull.get(monthID);
        if ((abbrvMonth != null) || (fullMonth != null))
        {
            println("<" + OOConstants.MONTH + ">");
            indent();
            
            println("<" + OOConstants.MONTH_ID + ">" + monthID + "</" + OOConstants.MONTH_ID + ">");
            if (abbrvMonth != null)
                println("<" + OOConstants.DEFAULT_ABBRV_NAME + ">" + abbrvMonth + "</" + OOConstants.DEFAULT_ABBRV_NAME + ">");
            if (fullMonth != null)
                println("<" + OOConstants.DEFAULT_FULL_NAME + ">" + fullMonth + "</" + OOConstants.DEFAULT_FULL_NAME + ">");
            
            outdent();
            println("</" + OOConstants.MONTH + ">");
        }
    }
    
    // Write Eras tag for a specific calendar.
    private void writeEras(Hashtable data, String unoid)
    {
        if ((data == null) || (data.size()==0))
            return;
        
        Hashtable erasCollection = (Hashtable) data.get(OOConstants.ERAS);
        if (erasCollection == null)
            return;
        
        Hashtable calendarEra = (Hashtable) erasCollection.get(unoid);
        if ((calendarEra == null) || (calendarEra.size()==0))
            return;
        
        println("<" + OOConstants.ERAS + ">");
        indent();
        
        //order is important in OO XML as EraID is ignored
        if (unoid.compareTo(OOConstants.GREGORIAN)==0)
        {
            writeSingleEra(calendarEra, OOConstants.BC);
            writeSingleEra(calendarEra, OOConstants.AD);
        }
        else if (unoid.compareTo(OOConstants.ROC)==0)
        {
            writeSingleEra(calendarEra, OOConstants.BEFORE_ROC);
            writeSingleEra(calendarEra, OOConstants.MINGUO);
        }
        else if (unoid.compareTo(OOConstants.HIJRI)==0)
        {
            writeSingleEra(calendarEra, OOConstants.BEFORE_HIJRA);
            writeSingleEra(calendarEra, OOConstants.AFTER_HIJRA);
        }
        else if (unoid.compareTo(OOConstants.JEWISH)==0)
        {
            writeSingleEra(calendarEra, OOConstants.BEFORE);
            writeSingleEra(calendarEra, OOConstants.AFTER);
        }
        else if (unoid.compareTo(OOConstants.BUDDHIST)==0)
        {
            writeSingleEra(calendarEra, OOConstants.BEFORE);
            writeSingleEra(calendarEra, OOConstants.AFTER);
        }
        else if (unoid.compareTo(OOConstants.HANJA)==0)
        {
            writeSingleEra(calendarEra, OOConstants.BC);
            writeSingleEra(calendarEra, OOConstants.AD);
        }
        else if (unoid.compareTo(OOConstants.GENGOU)==0)
        {
            writeSingleEra(calendarEra, OOConstants.DUMMY);
            writeSingleEra(calendarEra, OOConstants.MEIJI);
            writeSingleEra(calendarEra, OOConstants.TAISHO);
            writeSingleEra(calendarEra, OOConstants.SHOWA);
            writeSingleEra(calendarEra, OOConstants.HEISEI);
        }
        
        outdent();
        println("</" + OOConstants.ERAS + ">");
    }
    
    private void writeSingleEra(Hashtable calendarEra, String eraID)
    {
        Hashtable eraNames = (Hashtable) calendarEra.get(eraID);
        if (eraNames == null)
            return;
        String abbr = (String) eraNames.get(OOConstants.DEFAULT_ABBRV_NAME);
        String name = (String) eraNames.get(OOConstants.DEFAULT_FULL_NAME);
        
        println("<" + OOConstants.ERA + ">");
        indent();
        println("<" + OOConstants.ERA_ID + ">" + eraID + "</" + OOConstants.ERA_ID + ">");
        
        //sometimes this data is not available but elements are mandatory in OO locale.dtd so write <element/> if absent
        if (abbr != null)
            println("<" + OOConstants.DEFAULT_ABBRV_NAME + ">" + abbr + "</" + OOConstants.DEFAULT_ABBRV_NAME + ">");
        else
            println("<" + OOConstants.DEFAULT_ABBRV_NAME + "/>");
        
        if (name != null)
            println("<" + OOConstants.DEFAULT_FULL_NAME + ">" + name + "</" + OOConstants.DEFAULT_FULL_NAME + ">");
        else
            println("<" + OOConstants.DEFAULT_FULL_NAME + "/>");
        
        outdent();
        println("</" + OOConstants.ERA + ">");
    }
    
    
    private void writeStartOfWeek(Hashtable data, String calendarType)
    {
        if (data == null)
            return;
        
        Hashtable startOfWeeks = (Hashtable) data.get(OOConstants.START_DAY_OF_WEEK);
        if (startOfWeeks == null)
            return;
        
        String startOfWeek = (String) startOfWeeks.get(calendarType);
        
        //add workaround as getFullyResolvedLDML () doesn't implement other calendar inheritance from gregorian
        if (startOfWeek == null) startOfWeek = (String) startOfWeeks.get(OOConstants.GREGORIAN);
        
        if (startOfWeek != null)
        {
            println("<" + OOConstants.START_DAY_OF_WEEK + ">");
            indent();
            
            println("<" + OOConstants.DAY_ID + ">" + startOfWeek + "</" + OOConstants.DAY_ID + ">");
            
            outdent();
            println("</" + OOConstants.START_DAY_OF_WEEK + ">");
        }
        
    }
    
    public void writeMinDays(Hashtable data, String calendarType)
    {
        if (data == null)
            return;
        
        Hashtable minDaysTable = (Hashtable) data.get(OOConstants.MINIMAL_DAYS_IN_FIRST_WEEK);
        if (minDaysTable == null)
        {
            println("<" + OOConstants.MINIMAL_DAYS_IN_FIRST_WEEK + "/>");
            return;
        }
        
        String minDays = (String) minDaysTable.get(calendarType);
        
        //add workaround as getFullyResolvedLDML () doesn't implement other calendar inheritance from gregorian
        if (minDays == null) minDays = (String) minDaysTable.get(OOConstants.GREGORIAN);
        
        //this is mandatory in OO locale.dtd so write <element/> if data not found
        if (minDays != null)
            println("<" + OOConstants.MINIMAL_DAYS_IN_FIRST_WEEK + ">" + minDays + "</" + OOConstants.MINIMAL_DAYS_IN_FIRST_WEEK + ">");
        else
            println("<" + OOConstants.MINIMAL_DAYS_IN_FIRST_WEEK + "/>");
        
    }
    
    public void WriteLC_CURRENCY(Vector ldmlCurrTypes, String ldmlDefaultCurr, Hashtable ldmlCurrDisplayNames,
            Hashtable ldmlCurrSymbols, Hashtable ldmlCurrIDs, Hashtable ldmlCurrCompatibleFormatCodes,
            supplementalData suppData, boolean bRoundTrip)
    {
        if ((ldmlCurrTypes == null) || (ldmlCurrTypes.size()==0))
            return;
        
        println("<" + OOConstants.LC_CURRENCY + ">");
        indent();
        
        // loop through the different currencies types
        
        Enumeration currsEnum = ldmlCurrTypes.elements();
        while (currsEnum.hasMoreElements())
        {
            String currencyType = (String) currsEnum.nextElement();
            
            StringBuffer currStr = new StringBuffer("<" + OOConstants.CURRENCY);
            
            // is default?
            if ((ldmlDefaultCurr != null) && (ldmlDefaultCurr.length()>0))
            {
                if (ldmlDefaultCurr.compareTo(currencyType)==0)
                    currStr.append(" " + OOConstants.DEFAULT + "=\"" + OOConstants.TRUE + "\"");
                else
                    currStr.append(" " + OOConstants.DEFAULT + "=\"" + OOConstants.FALSE + "\"");
            }
            
            // is used in compatible format codes?
            if ((ldmlCurrCompatibleFormatCodes != null) && (ldmlCurrCompatibleFormatCodes.size()>0))
            {
                String isUsedComp = (String) ldmlCurrCompatibleFormatCodes.get(currencyType);
                String isUsedCompVal = "";
                if ((isUsedComp != null) && (isUsedComp.compareTo(OOConstants.TRUE)==0)) // no true word in LDMLConstants.
                    isUsedCompVal = OOConstants.TRUE;
                else
                    isUsedCompVal = OOConstants.FALSE;
                currStr.append(" " + OOConstants.USED_IN_COMPARTIBLE_FORMATCODES_SMALL + "=\"" + isUsedCompVal + "\"");
            }
            
            // deal with attributes
            currStr.append(">");
            println(currStr.toString());
            indent();
            
            String currID = "";
            if (bRoundTrip == true)  //write back OO data
            {
                currID = currencyType;  //don't use the OO currencyID as data can be in conflict
                if ((ldmlCurrIDs != null) && (ldmlCurrIDs.size()>0))
                    currID = (String) ldmlCurrIDs.get(currencyType);
                if (currID == null) currID = "";
            }
            else
                currID = currencyType;  //don't use the OO currencyID as data can be in conflict
            println("<" + OOConstants.CURRENCY_ID + ">" + currID + "</" + OOConstants.CURRENCY_ID + ">");
            
            String currSymbol =  "";
            if ((ldmlCurrSymbols != null) && (ldmlCurrSymbols.size()>0))
            {
                currSymbol = (String) ldmlCurrSymbols.get(currencyType);
                currSymbol = escapeXML(currSymbol);  //some symbols have specials
            }
            if (currSymbol == null) currSymbol = currencyType; //if it's N/A then put = code
            println("<" + OOConstants.CURRENCY_SYMBOL + ">" + currSymbol + "</" + OOConstants.CURRENCY_SYMBOL + ">");
            
            println("<" + OOConstants.BANK_SYMBOL + ">" + currencyType + "</" + OOConstants.BANK_SYMBOL + ">");
            
            String currName =  "";
            if ((ldmlCurrDisplayNames != null) && (ldmlCurrDisplayNames.size()>0))
                currName = (String) ldmlCurrDisplayNames.get(currencyType);
            if (currName == null)
                currName = currencyType;
            println("<" + OOConstants.CURRENCY_NAME + ">" + currName + "</" + OOConstants.CURRENCY_NAME + ">");
            
            // Get DecimalPlaces from suppData.
            if (suppData != null)
            {
                int decimalPlaces = suppData.getDigits(currencyType);
                println("<" + OOConstants.DECIMAL_PLACES + ">" + decimalPlaces + "</" + OOConstants.DECIMAL_PLACES + ">");
            }
            
            outdent();
            println("</" + OOConstants.CURRENCY + ">");
        }
        
        outdent();
        println("</" + OOConstants.LC_CURRENCY + ">");
    }
    
    public void WriteLC_TRANSLITERATIONS(Vector transliterations, String ref)
    {
        //if these a ref then these are no sub elements
        if ((ref != null) && (ref.length()>0))   
        {
            println("<" + OOConstants.LC_TRANSLITERATION + " " + OOConstants.REF + "=\"" + ref + "\"/>");
        }
        else if ((transliterations != null) && (transliterations.size()!=0))
        { 
            println("<" + OOConstants.LC_TRANSLITERATION + ">");
            indent();
            
            Enumeration transEnum = transliterations.elements();
            while (transEnum.hasMoreElements())
            {
                String transliteration = (String) transEnum.nextElement();
                if ((transliteration != null) && (transliteration.length()>0))
                    println("<" + OOConstants.TRANSLITERATION + " " + OOConstants.UNOID + "=\"" + transliteration + "\"" + "/>");
            }
            
            outdent();
            println("</" + OOConstants.LC_TRANSLITERATION + ">");
        }
    }
    
    public void WriteLC_MISC(Hashtable data, String ref)
    {
        //if these a ref then these are no sub elements
        if ((ref != null) && (ref.length()>0))
        {
            println("<" + OOConstants.LC_MISC + " " + OOConstants.REF + "=\"" + ref + "\"/>");
        }
        else if ((data != null) && (data.size()!=0))
        {
            println("<" + OOConstants.LC_MISC + ">");
            indent();
            
            String forbiddenLineBeg = (String) data.get(OOConstants.FORBIDDEN_LINE_BEGIN_CHARACTERS);
            String forbiddenLineEnd = (String) data.get(OOConstants.FORBIDDEN_LINE_END_CHARACTERS);
            
            if (((forbiddenLineBeg != null) && (forbiddenLineBeg.length()>0)) || ((forbiddenLineEnd != null) && (forbiddenLineEnd.length()>0)))
            {
                println("<" + OOConstants.FORBIDDEN_CHARACTERS + ">");
                indent();
                
                if ((forbiddenLineBeg != null) && (forbiddenLineBeg.length()>0))
                {
                    forbiddenLineBeg = escapeXML(forbiddenLineBeg);
                    println("<" + OOConstants.FORBIDDEN_LINE_BEGIN_CHARACTERS + ">" + forbiddenLineBeg + "</" + OOConstants.FORBIDDEN_LINE_BEGIN_CHARACTERS + ">");
                }
                if ((forbiddenLineEnd != null) && (forbiddenLineEnd.length()>0))
                {
                    forbiddenLineEnd = escapeXML(forbiddenLineEnd);
                    println("<" + OOConstants.FORBIDDEN_LINE_END_CHARACTERS + ">" + forbiddenLineEnd + "</" + OOConstants.FORBIDDEN_LINE_END_CHARACTERS + ">");
                }
                
                outdent();
                println("</" + OOConstants.FORBIDDEN_CHARACTERS + ">");
                
            }
            
            // reserved words
            Hashtable resWords = (Hashtable) data.get(OOConstants.RESERVED_WORDS);
            if ((resWords != null) && (resWords.size()>0))
            {
                println("<" + OOConstants.RESERVED_WORDS + ">");
                indent();
                writeReservedWord(resWords, OOConstants.TRUE_WORD);
                writeReservedWord(resWords, OOConstants.FALSE_WORD);
                writeReservedWord(resWords, OOConstants.QUARTER_1_WORD);
                writeReservedWord(resWords, OOConstants.QUARTER_2_WORD);
                writeReservedWord(resWords, OOConstants.QUARTER_3_WORD);
                writeReservedWord(resWords, OOConstants.QUARTER_4_WORD);
                writeReservedWord(resWords, OOConstants.ABOVE_WORD);
                writeReservedWord(resWords, OOConstants.BELOW_WORD);
                writeReservedWord(resWords, OOConstants.QUARTER_1_ABBREVIATION);
                writeReservedWord(resWords, OOConstants.QUARTER_2_ABBREVIATION);
                writeReservedWord(resWords, OOConstants.QUARTER_3_ABBREVIATION);
                writeReservedWord(resWords, OOConstants.QUARTER_4_ABBREVIATION);
                outdent();
                println("</" + OOConstants.RESERVED_WORDS + ">");
            }
            
            outdent();
            println("</" + OOConstants.LC_MISC + ">");
        }
    }
    
    private void writeReservedWord(Hashtable reservedWords, String ooKey)
    {
        String reservedWord = (String) reservedWords.get(ooKey);
        if ((reservedWord != null) && (reservedWord.length()>0))
            println("<" + ooKey + ">" + reservedWord + "</" + ooKey + ">");
    }
    
    public void WriteLC_NumberingLevel(Vector numberingLevelAtts, String ref)
    {
        //if these a ref then these are no sub elements
        if ((ref != null) && (ref.length()>0))
        {
            println("<" + OOConstants.LC_NUMBERING_LEVEL + " " + OOConstants.REF + "=\"" + ref + "\"/>");
        }
        else if ((numberingLevelAtts != null) && (numberingLevelAtts.size()!=0))
        {
            
            println("<" + OOConstants.LC_NUMBERING_LEVEL + ">");
            indent();
            
            Enumeration numEnum = numberingLevelAtts.elements();
            
            while (numEnum.hasMoreElements())
            {
                StringBuffer elem = new StringBuffer("<" + OOConstants.NUMBERING_LEVEL);
                Hashtable atts = (Hashtable) numEnum.nextElement();
                
                if (atts != null)
                {
                    Enumeration keys = atts.keys();
                    while (keys.hasMoreElements())
                    {
                        String key = (String) keys.nextElement();
                        String value = (String) atts.get(key);
                        if ((value != null) && (value.length()>0))
                            elem.append(" "	+ key + "=\"" + value + "\"");
                    }
                }
                elem.append("/>");
                println(elem.toString());
            }
            
            outdent();
            println("</" + OOConstants.LC_NUMBERING_LEVEL + ">");
        }
    }
    
    public void WriteLC_OutlineNumberingLevel(Vector outlineNumberingLevels, String ref)
    {
        //if these a ref then these are no sub elements
        if ((ref != null) && (ref.length()>0))
        {
            println("<" + OOConstants.LC_OUTLINE_NUMBERING_LEVEL + " " + OOConstants.REF + "=\"" + ref + "\"/>");
        }
        else if ((outlineNumberingLevels != null) && (outlineNumberingLevels.size()!=0))
        {
            println("<" + OOConstants.LC_OUTLINE_NUMBERING_LEVEL + ">");
            indent();
            
            Enumeration parentsEnum = outlineNumberingLevels.elements();
            while (parentsEnum.hasMoreElements())
            {
                Vector styleGroup = (Vector) parentsEnum.nextElement();  //vector of hashtables
                if ((styleGroup != null) && (styleGroup.size()>0))
                {
                    println("<" + OOConstants.OUTLINE_STYLE + ">");
                    indent();
                    
                    // Print out OutlineNumberingLevel elements with their attribs.
                    Enumeration elemsEnum = styleGroup.elements();
                    while (elemsEnum.hasMoreElements())
                        write_outlineNumberingLevel((Hashtable) elemsEnum.nextElement());
                    
                    outdent();
                    println("</" + OOConstants.OUTLINE_STYLE + ">");
                }
            }
            
            outdent();
            println("</" + OOConstants.LC_OUTLINE_NUMBERING_LEVEL + ">");
        }
    }
    
    private void write_outlineNumberingLevel(Hashtable attributes)
    {
        if ((attributes == null) || (attributes.size()==0))
            return;
        
        StringBuffer elem = new StringBuffer("<" + OOConstants.OUTLUNE_NUMBERING_LEVEL);
        appentOutlineNumberingLevelAtt(attributes,  elem, OOConstants.PREFIX);
        appentOutlineNumberingLevelAtt(attributes,  elem, OOConstants.NUM_TYPE);
        appentOutlineNumberingLevelAtt(attributes,  elem, OOConstants.SUFFIX);
        appentOutlineNumberingLevelAtt(attributes,  elem, OOConstants.BULLET_CHAR);
        appentOutlineNumberingLevelAtt(attributes,  elem, OOConstants.BULLET_FONT_NAME);
        appentOutlineNumberingLevelAtt(attributes,  elem, OOConstants.PARENT_NUMBERING);
        appentOutlineNumberingLevelAtt(attributes,  elem, OOConstants.LEFT_MARGIN);
        appentOutlineNumberingLevelAtt(attributes,  elem, OOConstants.SYMBOL_TEXT_DISTANCE);
        appentOutlineNumberingLevelAtt(attributes,  elem, OOConstants.FIRST_LINE_OFFSET);
        appentOutlineNumberingLevelAtt(attributes,  elem, OOConstants.TRANSLITERATION);
        appentOutlineNumberingLevelAtt(attributes,  elem, OOConstants.NAT_NUM);
        
        elem.append("/>");
        println(elem.toString());
    }
    
    private void appentOutlineNumberingLevelAtt(Hashtable attributes, StringBuffer str, String ooConstant)
    {
        String val = (String) attributes.get(ooConstant);
        if ((val != null))
            str.append(" " + ooConstant + "=\"" + val + "\"");
    }
    
    // & < > ' " need to be "escaped" in XML
    protected String escapeXML(String inData)
    {
        if (inData == null) return null;
        
        String outData = inData.replaceAll("&", "&amp;");
        outData = outData.replaceAll("<", "&lt;");
        outData = outData.replaceAll(">", "&gt;");
        outData = outData.replaceAll("'", "&apos;");
        outData = outData.replaceAll("\"", "&quot;");
        return outData;
    }
}
