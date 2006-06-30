/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

import org.unicode.cldr.icu.LDMLConstants;
import java.io.*;
import java.util.*;

/**
 *
 * class writes in memory LDML data to file , data is OO specific
 */
public class LDMLLocaleWriterForOO extends LDMLLocaleWriter
{
    private Hashtable m_Aliases = null;   //key = eleement name, data = value of "ref" attribute
    
    private Vector m_flexDateTimePatterns = null;  //holds all date/time patterns for writing to availableFormats
    private boolean m_bWriteCLDROnly = false;
    
    public LDMLLocaleWriterForOO(PrintStream out, boolean bWriteCLDROnly)
    {
        super(out);
        m_bWriteCLDROnly = bWriteCLDROnly;
    }
    
    public LDMLLocaleWriterForOO(PrintStream out, PrintStream err)
    {
        super(out, err);
    }
    
    public void setAliases(Hashtable aliases)
    {
        if (aliases != null)
            m_Aliases = aliases;
        
    }
    
    public void writeLocaleDisplaynames(Hashtable data)
    {
        if (m_bWriteCLDROnly == true) return;   //this method writes OO.o specials only
        
        if ((data == null) || (data.size()==0))
        {
            Logging.Log1("No LDML localeDisplayNames data to write ");
            return;
        }
        
        String language_type = (String) data.get(LDMLConstants.LANGUAGE + " " + LDMLConstants.TYPE);
        String defaultName_lang = null;
        if (language_type != null) defaultName_lang = (String) data.get(OpenOfficeLDMLConstants.DEFAULT_NAME + " " + language_type);
        String territory_type = (String) data.get(LDMLConstants.TERRITORY + " " + LDMLConstants.TYPE);
        String defaultName_terr = null;
        if (territory_type != null) defaultName_terr = (String) data.get(OpenOfficeLDMLConstants.DEFAULT_NAME + " " + territory_type);
        
        println(LDMLConstants.LDN_O);
        indent();
        
        if ( (language_type != null) && (defaultName_lang != null))
        {
            println(LDMLConstants.LANGUAGES_O);
            indent();
            println("<" + LDMLConstants.SPECIAL + " " + LDMLConstants.XMLNS + ":" + XMLNamespace.OPEN_OFFICE + "=\"" + XMLNamespace.OPEN_OFFICE_WWW + "\">" );
            indent();
            
            Hashtable attribs = new Hashtable();
            attribs.put(LDMLConstants.TYPE, language_type);
            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.DEFAULT_NAME, attribs, true, true, false);
            print(defaultName_lang);
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.DEFAULT_NAME, false);
            
            outdent();
            println(LDMLConstants.SPECIAL_C);
            outdent();
            println(LDMLConstants.LANGUAGES_C);
        }
        else
            Logging.Log3("Element localeDisplayNames/languages/special/openOffice:defaultName not written ");
        
        
        if ((territory_type != null) && (territory_type.compareTo("")!=0))
        {
            println(LDMLConstants.TERRITORIES_O);
            indent();
            println("<" + LDMLConstants.SPECIAL + " " + LDMLConstants.XMLNS + ":" + XMLNamespace.OPEN_OFFICE + "=\"" + XMLNamespace.OPEN_OFFICE_WWW + "\">" );
            indent();
            
            Hashtable attribs = new Hashtable();
            attribs.put(LDMLConstants.TYPE, territory_type);
            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.DEFAULT_NAME, attribs, true, true, false);
            print(defaultName_terr);
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.DEFAULT_NAME, false);
            
            outdent();
            println(LDMLConstants.SPECIAL_C);
            outdent();
            println(LDMLConstants.TERRITORIES_C);
        }
        else
            Logging.Log3("Element localeDisplayNames/territories/special/openOffice:defaultName not written ");
        
        outdent();
        println(LDMLConstants.LDN_C);
        
    }
    
    public void writeDates(Hashtable data)
    {
        if ((data == null) || (data.size()==0))
        {
            Logging.Log1("No LDML calendar data to write ");
            return;
        }
        
        //*** read in all data to nbe printed to LDML
        //a Vector of calendar types plus last entry = pos of default calendar
        Vector calendars = (Vector) data.get(LDMLConstants.CALENDAR + " " + LDMLConstants.TYPE);
        Hashtable abbrDays = (Hashtable) data.get(LDMLConstants.DAY_WIDTH + " " + LDMLConstants.ABBREVIATED);
        Hashtable wideDays = (Hashtable) data.get(LDMLConstants.DAY_WIDTH + " " + LDMLConstants.WIDE);
        Hashtable abbrMonths = (Hashtable) data.get(LDMLConstants.MONTH_WIDTH + " " + LDMLConstants.ABBREVIATED);
        Hashtable wideMonths = (Hashtable) data.get(LDMLConstants.MONTH_WIDTH + " " + LDMLConstants.WIDE);
        Hashtable abbrQuarters = (Hashtable) data.get(LDMLConstants.QUARTER_WIDTH + " " + LDMLConstants.ABBREVIATED);
        Hashtable wideQuarters = (Hashtable) data.get(LDMLConstants.QUARTER_WIDTH + " " + LDMLConstants.WIDE);        
        Hashtable abbrEras = (Hashtable) data.get(LDMLConstants.ERAABBR);
        Hashtable wideEras = (Hashtable) data.get(LDMLConstants.ERANAMES);
        Hashtable startDaysOfWeek = (Hashtable) data.get(LDMLConstants.FIRSTDAY);
        Hashtable minDayssInFirstWeek = (Hashtable) data.get(LDMLConstants.MINDAYS);
        String am = (String) data.get(LDMLConstants.AM);
        String pm = (String) data.get(LDMLConstants.PM);
        
        Hashtable formatElements_date = (Hashtable) data.get(LDMLConstants.DATE_FORMAT);
        Hashtable formatCodes_date = (Hashtable) data.get(LDMLConstants.DATE_FORMAT + " " + LDMLConstants.PATTERN);
        Hashtable formatDefaultNames_date = (Hashtable) data.get(LDMLConstants.DATE_FORMAT + OpenOfficeLDMLConstants.DEFAULT_NAME);
        
        Hashtable formatElements_time = (Hashtable) data.get(LDMLConstants.TIME_FORMAT);
        Hashtable formatCodes_time = (Hashtable) data.get(LDMLConstants.TIME_FORMAT + " " + LDMLConstants.PATTERN);
        Hashtable formatDefaultNames_time = (Hashtable) data.get(LDMLConstants.TIME_FORMAT + OpenOfficeLDMLConstants.DEFAULT_NAME);
        
        Hashtable formatElements_date_time = (Hashtable) data.get(LDMLConstants.DATE_TIME_FORMAT);
        Hashtable formatCodes_date_time = (Hashtable) data.get(LDMLConstants.DATE_TIME_FORMAT + " " + LDMLConstants.PATTERN);
        Hashtable formatDefaultNames_date_time = (Hashtable) data.get(LDMLConstants.DATE_TIME_FORMAT + OpenOfficeLDMLConstants.DEFAULT_NAME);
        
        m_flexDateTimePatterns = (Vector) data.get(LDMLConstants.AVAIL_FMTS);
       
        //if there's nothing to write then return
        if ((calendars == null) && (abbrDays == null) && (wideDays==null) && (abbrMonths==null)
        && (wideMonths==null) && (abbrEras==null) && (wideEras==null) && (startDaysOfWeek==null)
        && (minDayssInFirstWeek==null) && (am==null) && (pm==null)
        && (formatElements_date==null) && (formatElements_time==null) && (formatElements_date_time==null)
        && (m_flexDateTimePatterns==null))
        {
            Logging.Log1("No LDML calendar data to write ");
            return;
        }
        
        println(LDMLConstants.DATES_O);
        indent();
        
        println(LDMLConstants.CALENDARS_O);
        indent();
        
        String posDefault = (String)calendars.elementAt(calendars.size()-1);
        int iPosDefault = Integer.parseInt(posDefault);
        if (iPosDefault != -1)
        {   //we have a default calendar type
            String deflt_calendar = (String) calendars.elementAt(iPosDefault);
            if (deflt_calendar != null)
                println("<" + LDMLConstants.DEFAULT + " " + LDMLConstants.TYPE + "=\"" + deflt_calendar + "\"/>");
        }
        else
            Logging.Log3("No default calendar type to write ");
        
        for (int i=0; i < calendars.size()-1; i++)   //-1 as last entry is a special entry=pos of defauult calendar
        {
            String calendar = (String) calendars.elementAt(i);
            println("<" + LDMLConstants.CALENDAR + " " + LDMLConstants.TYPE + "=\"" + calendar + "\">");
            indent();
            
            //months
            Hashtable abbrMonthsI = null;
            if (abbrMonths != null)
            {
                abbrMonthsI = (Hashtable) abbrMonths.get((Object)calendar);
            }
            Hashtable wideMonthsI = null;
            if (wideMonths != null)
            {
                wideMonthsI = (Hashtable) wideMonths.get((Object)calendar);
            }
            if ((abbrMonthsI != null) || (wideMonthsI != null))
            {
                println(LDMLConstants.MONTHS_O);
                indent();
                println("<" + LDMLConstants.MONTH_CONTEXT + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.FORMAT + "\">");
                indent();
                if (abbrMonthsI != null)
                    writeMonthWidth(abbrMonthsI, LDMLConstants.ABBREVIATED);
                else
                    Logging.Log3("No abbreviated months to write for calendar : " + calendar);
                
                if (wideMonthsI != null)
                    writeMonthWidth(wideMonthsI, LDMLConstants.WIDE);
                else
                    Logging.Log3("No wide to write for calendar : " + calendar);
                
                outdent();
                println(LDMLConstants.MONTH_CONTEXT_C);
                outdent();
                println(LDMLConstants.MONTHS_C);
            }
            else
                Logging.Log3("No abbreviated or wide months to write for calendar : " + calendar);
            
            //days
            Hashtable abbrDaysI = null;
            if (abbrDays != null)
            {
                abbrDaysI = (Hashtable) abbrDays.get((Object)calendar);
            }
            Hashtable wideDaysI = null;
            if (wideDays != null)
            {
                wideDaysI = (Hashtable) wideDays.get((Object)calendar);
            }
            if ((abbrDaysI != null) || (wideDaysI != null))
            {
                println(LDMLConstants.DAYS_O);
                indent();
                println("<" + LDMLConstants.DAY_CONTEXT + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.FORMAT + "\">");
                indent();
                if (abbrDaysI != null)
                    writeDayWidth(abbrDaysI, LDMLConstants.ABBREVIATED);
                else
                    Logging.Log3("No abbreviated Days to write for calendar : " + calendar);
                
                if (wideDaysI != null)
                    writeDayWidth(wideDaysI, LDMLConstants.WIDE);
                else
                    Logging.Log3("No wide Days to write for calendar : " + calendar);
                
                outdent();
                println(LDMLConstants.DAY_CONTEXT_C);
                outdent();
                println(LDMLConstants.DAYS_C);
            }
            else
                Logging.Log3("No abbreviated or wide Days to write for calendar : " + calendar);
            
            //quarters
            Hashtable abbrQuartersI = null;
            if (abbrQuarters != null)
            {
                abbrQuartersI = (Hashtable) abbrQuarters.get((Object)calendar);
            }
            Hashtable wideQuartersI = null;
            if (wideQuarters != null)
            {
                wideQuartersI = (Hashtable) wideQuarters.get((Object)calendar);
            }
            if ((abbrQuartersI != null) || (wideQuartersI != null))
            {
                println(LDMLConstants.QUARTERS_O);
                indent();
                println("<" + LDMLConstants.QUARTER_CONTEXT + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.FORMAT + "\">");
                indent();
                if (abbrQuartersI != null)
                    writeQuarterWidth(abbrQuartersI, LDMLConstants.ABBREVIATED);
                else
                    Logging.Log3("No abbreviated Quarters to write for calendar : " + calendar);
                
                if (wideQuartersI != null)
                    writeQuarterWidth(wideQuartersI, LDMLConstants.WIDE);
                else
                    Logging.Log3("No wide Quarters to write for calendar : " + calendar);
                
                outdent();
                println(LDMLConstants.QUARTER_CONTEXT_C);
                outdent();
                println(LDMLConstants.QUARTERS_C);
            }
            else if (calendar.equals (LDMLConstants.GREGORIAN))
                Logging.Log3("No abbreviated or wide Quarters to write for calendar : " + calendar);
            
            
            //StartDayOfWeek and MinDays in week
            String startDayOfWeek = null;
            if (startDaysOfWeek != null)
                startDayOfWeek = (String) startDaysOfWeek.get((Object)calendar);
            String minDaysInFirstWeek = null;
            if (minDayssInFirstWeek != null)
                minDaysInFirstWeek = (String) minDayssInFirstWeek.get((Object) calendar);
            
            writeWeek(minDaysInFirstWeek, startDayOfWeek, null, null, calendar);
            
            //am  - as am and pm are generic and not specific to calendar type in OO , write them to all calendar types in LDML
            //TODO : this is a bug, should only write to gregorian, all cals inherit from gregorian
            writeAmPm(am, pm, calendar);
            
            //Eras
            Hashtable wideErasI = null;
            if (wideEras != null)
                wideErasI = (Hashtable) wideEras.get((Object)calendar);
            Hashtable abbrErasI = null;
            if (abbrEras != null)
                abbrErasI = (Hashtable) abbrEras.get((Object)calendar);
            if ((wideErasI != null) || (abbrErasI != null))
            {
                println(LDMLConstants.ERAS_O);
                indent();
                if (wideErasI != null)
                {
                    println(LDMLConstants.ERANAMES_O);
                    indent();
                    writeEras(wideErasI);
                    outdent();
                    println(LDMLConstants.ERANAMES_C);
                }
                else
                    Logging.Log3("No wide eras to write for calendar : " + calendar);
                
                if (abbrErasI != null)
                {
                    println(LDMLConstants.ERAABBR_O);
                    indent();
                    writeEras(abbrErasI);
                    outdent();
                    println(LDMLConstants.ERAABBR_C);
                }
                else
                    Logging.Log3("No abbreviated eras to write for calendar : " + calendar);
                
                outdent();
                println(LDMLConstants.ERAS_C);
            }
            else
                Logging.Log3("No eras to write for calendar : " + calendar);
            
            
            
            if (m_bWriteCLDROnly == false)   //below writes OO.o specials only
            {
                if (i==0)  //only write special Format elements once
                {
                    writeFormats(formatElements_date, formatCodes_date, formatDefaultNames_date, LDMLConstants.DATE_FORMATS, LDMLConstants.DFL, LDMLConstants.DATE_FORMAT, calendar);
                    writeFormats(formatElements_time, formatCodes_time, formatDefaultNames_time, LDMLConstants.TIME_FORMATS, LDMLConstants.TFL, LDMLConstants.TIME_FORMAT, calendar);
                    writeFormats(formatElements_date_time, formatCodes_date_time, formatDefaultNames_date_time, LDMLConstants.DATE_TIME_FORMATS, LDMLConstants.DTFL, LDMLConstants.DATE_TIME_FORMAT, calendar);
                }
            }
            
            outdent();
            println(LDMLConstants.CALENDAR_C);
        }  //end for loop
        
        outdent();
        println(LDMLConstants.CALENDARS_C);
        
        outdent();
        println(LDMLConstants.DATES_C);
    }
    
    
    public void writeNumbers(Hashtable data)
    {
        if ((data == null) || (data.size()==0))
        {
            Logging.Log1("No LDML numbers data to write ");
            return;
        }
        
        Hashtable symbols = (Hashtable) data.get(LDMLConstants.SYMBOLS);
        Vector currencies = (Vector) data.get(LDMLConstants.CURRENCIES);
        
        Hashtable formatElements_fn = (Hashtable) data.get(LDMLConstants.DECIMAL_FORMATS);
        Hashtable formatCodes_fn = (Hashtable) data.get(LDMLConstants.DECIMAL_FORMATS + " " + LDMLConstants.PATTERN);
        Hashtable formatDefaultNames_fn = (Hashtable) data.get(LDMLConstants.DECIMAL_FORMATS + OpenOfficeLDMLConstants.DEFAULT_NAME);
        
        Hashtable formatElements_sn = (Hashtable) data.get(LDMLConstants.SCIENTIFIC_FORMATS);
        Hashtable formatCodes_sn = (Hashtable) data.get(LDMLConstants.SCIENTIFIC_FORMATS + " " + LDMLConstants.PATTERN);
        Hashtable formatDefaultNames_sn = (Hashtable) data.get(LDMLConstants.SCIENTIFIC_FORMATS + OpenOfficeLDMLConstants.DEFAULT_NAME);
        
        Hashtable formatElements_pn = (Hashtable) data.get(LDMLConstants.PERCENT_FORMATS);
        Hashtable formatCodes_pn = (Hashtable) data.get(LDMLConstants.PERCENT_FORMATS + " " + LDMLConstants.PATTERN);
        Hashtable formatDefaultNames_pn = (Hashtable) data.get(LDMLConstants.PERCENT_FORMATS + OpenOfficeLDMLConstants.DEFAULT_NAME);
        
        Hashtable formatElements_c = (Hashtable) data.get(LDMLConstants.CURRENCY_FORMATS);
        Hashtable formatCodes_c = (Hashtable) data.get(LDMLConstants.CURRENCY_FORMATS + " " + LDMLConstants.PATTERN);
        Hashtable formatDefaultNames_c = (Hashtable) data.get(LDMLConstants.CURRENCY_FORMATS + OpenOfficeLDMLConstants.DEFAULT_NAME);
        
        if ((symbols == null) && (currencies == null)
        && (formatElements_fn == null) && (formatElements_sn == null) && (formatElements_pn == null) && (formatElements_c==null))
        {
            Logging.Log1("No LDML numbers data to write ");
            return;
        }
        
        println(LDMLConstants.NUMBERS_O);
        indent();
        
        writeSymbols(symbols);
        
        if (m_bWriteCLDROnly == false)   //below writes OO.o specials only
        {
            writeFormats(formatElements_fn, formatCodes_fn, formatDefaultNames_fn, LDMLConstants.DECIMAL_FORMATS, LDMLConstants.DECFL, LDMLConstants.DECIMAL_FORMAT, null);
            writeFormats(formatElements_sn, formatCodes_sn, formatDefaultNames_sn, LDMLConstants.SCIENTIFIC_FORMATS, LDMLConstants.SCIFL, LDMLConstants.SCIENTIFIC_FORMAT, null);
            writeFormats(formatElements_pn, formatCodes_pn, formatDefaultNames_pn, LDMLConstants.PERCENT_FORMATS, LDMLConstants.PERFL, LDMLConstants.PERCENT_FORMAT, null);
            writeFormats(formatElements_c, formatCodes_c, formatDefaultNames_c, LDMLConstants.CURRENCY_FORMATS, LDMLConstants.CURFL, LDMLConstants.CURRENCY_FORMAT, null);
        }
        
        writeCurrencies(currencies, symbols);
        outdent();
        println(LDMLConstants.NUMBERS_C);
    }
    
    
    //special
    public void writeSpecial(Hashtable data)
    {
        if (m_bWriteCLDROnly == true) return;   //this method writes OO.o specials only
        
        if ((data == null) || (data.size()==0))
        {
            Logging.Log1("No LDML Special data to write ");
            return;
        }
        
        Hashtable localeInfo = (Hashtable) data.get(OpenOfficeLDMLConstants.LOCALE);
        Hashtable OOsymbols = (Hashtable) data.get(LDMLConstants.SYMBOLS);
        Hashtable forbiddenChars = (Hashtable) data.get(OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS);
        Hashtable reservedWords = (Hashtable) data.get(OpenOfficeLDMLConstants.RESERVED_WORDS);
        Vector numLevels = (Vector) data.get(OpenOfficeLDMLConstants.NUMBERING_LEVELS);
        Vector outlineNumberingLevels = (Vector) data.get(OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVELS);
        Vector translits = (Vector) data.get(OpenOfficeLDMLConstants.TRANSLITERATIONS);
        Vector searchOptions = (Vector) data.get(OpenOfficeLDMLConstants.SEARCH_OPTIONS);
        Vector indexKeys = (Vector) data.get(OpenOfficeLDMLConstants.INDEX_KEY);
        Hashtable indexKeysData = (Hashtable) data.get(OpenOfficeLDMLConstants.INDEX);
        Vector unicodeScript = (Vector) data.get(OpenOfficeLDMLConstants.UNICODE_SCRIPT);
        Vector followPageWord = (Vector) data.get(OpenOfficeLDMLConstants.FOLLOW_PAGE_WORD);
        
        Vector collators = (Vector) data.get(OpenOfficeLDMLConstants.COLLATOR);
        Vector collationOptions = (Vector) data.get(OpenOfficeLDMLConstants.COLLATION_OPTIONS);
        
        Vector format = (Vector) data.get(OpenOfficeLDMLConstants.FORMAT);
        
        //look for aliases in specials
        boolean bFoundAlias = false;
        Vector specials = new Vector();
        specials.add(OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS);
        specials.add(OpenOfficeLDMLConstants.RESERVED_WORDS);
        specials.add(OpenOfficeLDMLConstants.NUMBERING_LEVELS);
        specials.add(OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVELS);
        specials.add(OpenOfficeLDMLConstants.TRANSLITERATIONS);
        specials.add(OpenOfficeLDMLConstants.SEARCH);
        specials.add(OpenOfficeLDMLConstants.INDEX);
        specials.add(OpenOfficeLDMLConstants.COLLATIONS);
        specials.add(OpenOfficeLDMLConstants.FORMAT);
        specials.add(LDMLConstants.SYMBOLS);
        for (int i=0; i < specials.size(); i++)
        {
            if (m_Aliases.containsKey(specials.elementAt(i))==true)
            {
                bFoundAlias = true;
                break;
            }
        }
        
        //see is there anything to write
        if (((localeInfo==null) || (localeInfo.size()==0))
        && ((OOsymbols==null) || (OOsymbols.size()==0))
        && ((forbiddenChars==null) || (forbiddenChars.size()==0))
        && ((reservedWords==null) ||  (reservedWords.size()==0))
        && ((numLevels==null) || (numLevels.size()==0))
        && ((translits==null) || (translits.size()==0))
        && ((searchOptions==null) || (searchOptions.size()==0))
        && ((indexKeys==null) || (indexKeys.size()==0))
        && ((indexKeysData==null) || (indexKeysData.size()==0))
        && ((unicodeScript==null) || (unicodeScript.size()==0))
        && ((followPageWord==null) || (followPageWord.size()==0))
        && ((outlineNumberingLevels==null) || (outlineNumberingLevels.size()==0))
        && ((collators==null) || (collators.size()==0))
        && ((collationOptions==null) || (collationOptions.size()==0))
        && ((format==null) || (format.size()==0))
        && (bFoundAlias==false))
        {
            Logging.Log1("No LDML Special data to write ");
            return;
        }
        
        println("<" + LDMLConstants.SPECIAL + " " + LDMLConstants.XMLNS + ":" + XMLNamespace.OPEN_OFFICE +
        "=\"" + XMLNamespace.OPEN_OFFICE_WWW +"\"" + ">");
        indent();
        
        writeLocale(localeInfo);
        writeOOSymbols (OOsymbols);
        writeForbiddenChars(forbiddenChars);
        writeReservedWords(reservedWords);
        writeNumberingLevels(numLevels);
        writeOutLineNnumLevels(outlineNumberingLevels);
        writeTransliterations(translits);
        writeSearchOptions(searchOptions);
        writeIndex(indexKeys, indexKeysData, unicodeScript, followPageWord);
        writeCollations(collators, collationOptions);
        writeSpecialFormat(format);
        
        outdent();
        println(LDMLConstants.SPECIAL_C);
    }
    
    
    public void writeForbiddenChars(Hashtable data)
    {
        String alias = (String) m_Aliases.get(OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS);
        
        if (((data == null) || (data.size()==0)) && (alias == null))
        {
            Logging.Log1("No LDML forbiddenCharacters data to write ");
            return;
        }
        
        if (alias != null)
        {
            Hashtable attribs = new Hashtable();
            attribs.put(OpenOfficeLDMLConstants.REF, alias);
            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS, attribs, true, false, true);
            //writeAlias(alias);
        }
        else
        {
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS, true);
            indent();
            
            String forLineBeginChars = (String) data.get(OpenOfficeLDMLConstants.FORBIDDEN_LINE_BEGIN_CHARACTERS);
            String forLineEndChars = (String) data.get(OpenOfficeLDMLConstants.FORBIDDEN_LINE_END_CHARACTERS);
            if (forLineBeginChars != null)
            {
                forLineBeginChars = escapeXML(forLineBeginChars);
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FORBIDDEN_LINE_BEGIN_CHARACTERS, true);
                print(forLineBeginChars);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FORBIDDEN_LINE_BEGIN_CHARACTERS, false);
            }
            else
                Logging.Log3("No LDML Special forbiddenLineBeginCharacters to write ");
            
            if (forLineEndChars != null)
            {
                forLineEndChars = escapeXML(forLineEndChars);
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FORBIDDEN_LINE_END_CHARACTERS, true);
                print(forLineEndChars);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FORBIDDEN_LINE_END_CHARACTERS, false);
            }
            else
                Logging.Log3("No LDML Special forbiddenLineEndCharacters to write ");
            
            outdent();
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS, false);
        }
    }
    
    public void writeReservedWords(Hashtable data)
    {
        String alias = (String) m_Aliases.get(OpenOfficeLDMLConstants.RESERVED_WORDS);
        
        if (((data == null) || (data.size()==0)) && (alias == null))
        {
            Logging.Log1("No LDML reservedWords data to write ");
            return;
        }
        
        if (alias != null)
        {
            Hashtable attribs = new Hashtable();
            attribs.put(OpenOfficeLDMLConstants.REF, alias);
            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.RESERVED_WORDS, attribs, true, false, true);
            //writeAlias(alias);
        }
        else
        {
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.RESERVED_WORDS, true);
            indent();
            
            String trueWord = (String) data.get(OpenOfficeLDMLConstants.TRUE_WORD);
            String falseWord = (String) data.get(OpenOfficeLDMLConstants.FALSE_WORD);
            String q1Word = (String) data.get(OpenOfficeLDMLConstants.QUARTER_1_WORD);
            String q2Word = (String) data.get(OpenOfficeLDMLConstants.QUARTER_2_WORD);
            String q3Word = (String) data.get(OpenOfficeLDMLConstants.QUARTER_3_WORD);
            String q4Word = (String) data.get(OpenOfficeLDMLConstants.QUARTER_4_WORD);
            String aboveWord = (String) data.get(OpenOfficeLDMLConstants.ABOVE_WORD);
            String belowWord = (String) data.get(OpenOfficeLDMLConstants.BELOW_WORD);
            String q1Abbr = (String) data.get(OpenOfficeLDMLConstants.QUARTER_1_ABBREVIATION);
            String q2Abbr = (String) data.get(OpenOfficeLDMLConstants.QUARTER_2_ABBREVIATION);
            String q3Abbr = (String) data.get(OpenOfficeLDMLConstants.QUARTER_3_ABBREVIATION);
            String q4Abbr = (String) data.get(OpenOfficeLDMLConstants.QUARTER_4_ABBREVIATION);
            
            if (trueWord != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.TRUE_WORD, true);
                print(trueWord);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.TRUE_WORD, false);
            }
            else
                Logging.Log3("No LDML Special reservedWords:trueWord to write ");
            
            if (falseWord != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FALSE_WORD, true);
                print(falseWord);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FALSE_WORD, false);
            }
            else
                Logging.Log3("No LDML Special reservedWords:falseWord to write ");
            
            if (q1Word != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_1_WORD, true);
                print(q1Word);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_1_WORD, false);
            }
        //    else
        //        Logging.Log3("No LDML Special reservedWords:q1Word to write ");
            
            if (q2Word != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_2_WORD, true);
                print(q2Word);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_2_WORD, false);
            }
        //    else
       //         Logging.Log3("No LDML Special reservedWords:q2Word to write ");
            
            if (q3Word != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_3_WORD, true);
                print(q3Word);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_3_WORD, false);
            }
       //     else
      //          Logging.Log3("No LDML Special reservedWords:q3Word to write ");
            
            if (q4Word != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_4_WORD, true);
                print(q4Word);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_4_WORD, false);
            }
    //        else
    //            Logging.Log3("No LDML Special reservedWords:q4Word to write ");
            
            if (aboveWord != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.ABOVE_WORD, true);
                print(aboveWord);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.ABOVE_WORD, false);
            }
            else
                Logging.Log3("No LDML Special reservedWords:aboveWord to write ");
            
            if (belowWord != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.BELOW_WORD, true);
                print(belowWord);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.BELOW_WORD, false);
            }
            else
                Logging.Log3("No LDML Special reservedWords:belowWord to write ");
            
            if (q1Abbr != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_1_ABBREVIATION, true);
                print(q1Abbr);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_1_ABBREVIATION, false);
            }
      //      else
      //          Logging.Log3("No LDML Special reservedWords:q1Abbr to write ");
            
            if (q2Abbr != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_2_ABBREVIATION, true);
                print(q2Abbr);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_2_ABBREVIATION, false);
            }
       //     else
       //         Logging.Log3("No LDML Special reservedWords:q2Abbr to write ");
            
            if (q3Abbr != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_3_ABBREVIATION, true);
                print(q3Abbr);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_3_ABBREVIATION, false);
            }
       //     else
       //         Logging.Log3("No LDML Special reservedWords:q3Abbr to write ");
            
            if (q4Abbr != null)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_4_ABBREVIATION, true);
                print(q4Abbr);
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.QUARTER_4_ABBREVIATION, false);
            }
     //       else
     //           Logging.Log3("No LDML Special reservedWords:q4Abbr to write ");
            
            outdent();
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.RESERVED_WORDS, false);
        }
    }
    
    //data is Vector of Hashtables
    public void writeNumberingLevels(Vector data)
    {
        String alias = (String) m_Aliases.get(OpenOfficeLDMLConstants.NUMBERING_LEVELS);
        
        if (((data == null) || (data.size()==0)) && (alias == null))
        {
            Logging.Log1("No LDML numberingLevels data to write ");
            return;
        }
        
        if (alias != null)
        {
            Hashtable attribs = new Hashtable();
            attribs.put(OpenOfficeLDMLConstants.REF, alias);
            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.NUMBERING_LEVELS, attribs, true, false, true);
            //writeAlias(alias);
        }
        else
        {
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.NUMBERING_LEVELS, true);
            indent();
            for (int i=0; i < data.size(); i++)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.NUMBERING_LEVEL, (Hashtable)data.elementAt(i), true, false, true);
            }
            outdent();
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.NUMBERING_LEVELS, false);
        }
    }
    
    public void writeOutLineNnumLevels(Vector data)
    {
        String alias = (String) m_Aliases.get(OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVELS);
        
        if (((data == null) || (data.size()==0)) && (alias == null))
        {
            Logging.Log1("No LDML outlineNumberingLevels data to write ");
            return;
        }
        
        if (alias != null)
        {
            Hashtable attribs = new Hashtable();
            attribs.put(OpenOfficeLDMLConstants.REF, alias);
            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVELS, attribs, true, false, true);
            //writeAlias(alias);
        }
        else
        {
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVELS, true);
            indent();
            
            for (int i=0; i < data.size(); i++)
            {
                //outLineNumberingLevels are grouped in 5s
                int rem = i % 5;
                if (rem ==0)
                {
                    printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.OUTLINE_STYLE, true);
                    indent();
                }
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVEL, (Hashtable)data.elementAt(i), true, false, true);
                
                rem = (i+1) % 5;
                if (rem ==0)
                {
                    outdent();
                    printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.OUTLINE_STYLE, false);
                }
            }
            outdent();
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVELS, false);
        }
        
    }
    
    
    //data is Vector of Hashtables
    public void writeTransliterations(Vector data)
    {
        String alias = (String) m_Aliases.get(OpenOfficeLDMLConstants.TRANSLITERATIONS);
        
        if (((data == null) || (data.size()==0)) && (alias == null))
        {
            Logging.Log1("No LDML transliterations data to write ");
            return;
        }
        
        if (alias != null)
        {
            Hashtable attribs = new Hashtable();
            attribs.put(OpenOfficeLDMLConstants.REF, alias);
            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.TRANSLITERATIONS, attribs, true, false, true);
            //writeAlias(alias);
        }
        else
        {
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.TRANSLITERATIONS, true);
            indent();
            
            for (int i=0; i < data.size(); i++)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.TRANSLITERATION, (Hashtable)data.elementAt(i), true, false, true);
            }
            
            outdent();
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.TRANSLITERATIONS, false);
        }
    }
    
    public void writeSearchOptions(Vector data)
    {
        String alias = (String) m_Aliases.get(OpenOfficeLDMLConstants.SEARCH);
        
        if (((data == null) || (data.size()==0)) && (alias == null))
        {
            Logging.Log1("No LDML searchOptions data to write ");
            return;
        }
        
        if (alias != null)
        {
            Hashtable attribs = new Hashtable();
            attribs.put(OpenOfficeLDMLConstants.REF, alias);
            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.SEARCH, attribs, true, false, true);
            //writeAlias(alias);
        }
        else
        {
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.SEARCH, true);
            indent();
            
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.SEARCH_OPTIONS, true);
            indent();
            for (int i=0; i < data.size(); i++)
            {
                printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.TRANSLITERATION_MODULES, true);
                print((String)data.elementAt(i));
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.TRANSLITERATION_MODULES, false);
            }
            outdent();
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.SEARCH_OPTIONS, false);
            
            outdent();
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.SEARCH, false);
        }
    }
    
    public void writeIndex(Vector indexKeys, Hashtable indexKeysData, Vector unicodeScript, Vector followPageWord)
    {
        String alias = (String) m_Aliases.get(OpenOfficeLDMLConstants.INDEX);
        
        if ((((indexKeys==null) || (indexKeys.size()==0))
        && ((indexKeysData==null) || (indexKeysData.size()==0))
        && ((unicodeScript==null) || (unicodeScript.size()==0))
        && ((followPageWord==null) || (followPageWord.size()==0))) && (alias==null))
        {
            Logging.Log1("No LDML index data to write ");
            return;
        }
        
        if (alias != null)
        {
            Hashtable attribs = new Hashtable();
            attribs.put(OpenOfficeLDMLConstants.REF, alias);
            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.INDEX, attribs, true, false, true);
            //writeAlias(alias);
        }
        else
        {
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.INDEX, true);
            indent();
            
            if (indexKeys != null)
            {
                for (int i=0; i < indexKeys.size(); i++)
                {
                    //retrieve indexKey's attributes and values
                    Hashtable inner = (Hashtable) indexKeys.elementAt(i);
                    
                    //now see does this indexKey have any data
                    String unoid = (String) inner.get(OpenOfficeLDMLConstants.UNOID);
                    String indexKeyValue = null;
                    if (indexKeysData != null)
                        indexKeyValue = (String) indexKeysData.get(unoid /*OpenOfficeLDMLConstants.INDEX*/);
                    
                    boolean bHasSubElementsOrData = false;
                    boolean bNewLine = true;
                    if (indexKeyValue != null)
                    {
                        bHasSubElementsOrData = true;
                        bNewLine = false;
                    }
                    printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.INDEX_KEY, inner, true, bHasSubElementsOrData, bNewLine);
                    
                    if (indexKeyValue != null)
                    {
                        print(indexKeyValue);
                        printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.INDEX_KEY, false);
                    }
                }
            }
            else
                Logging.Log3("No LDML Special indexKeys to write ");
            
            
            if (unicodeScript != null)
            {
                for (int i=0; i < unicodeScript.size(); i++)
                {
                    printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.UNICODE_SCRIPT, true);
                    print( (String)unicodeScript.elementAt(i));
                    printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.UNICODE_SCRIPT, false);
                }
            }
            else
                Logging.Log3("No LDML Special unicodeScript to write ");
            
            if (followPageWord != null)
            {
                for (int i=0; i < followPageWord.size(); i++)
                {
                    printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FOLLOW_PAGE_WORD, true);
                    print( (String)followPageWord.elementAt(i));
                    printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FOLLOW_PAGE_WORD, false);
                }
            }
            else
                Logging.Log3("No LDML Special followPageWord to write ");
            
            outdent();
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.INDEX, false);
        }
    }
    
    
    public void writeCollations(Vector collators, Vector collationOptions)
    {
        String alias = (String) m_Aliases.get(OpenOfficeLDMLConstants.COLLATIONS);
        
        if (((collators==null) || (collators.size()==0)
        && (collationOptions==null) || (collationOptions.size()==0)) && (alias==null))
        {
            Logging.Log1("No LDML collations data to write ");
            return;
        }
        
        if (alias != null)
        {
            Hashtable attribs = new Hashtable();
            attribs.put(OpenOfficeLDMLConstants.REF, alias);
            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.COLLATIONS, attribs, true, false, true);
            //writeAlias(alias);
        }
        else
        {
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.COLLATIONS, true);
            indent();
            if (collators != null)
            {
                for (int i=0; i < collators.size(); i++)
                {
                    printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.COLLATOR, (Hashtable)collators.elementAt(i), true, false, true);
                }
            }
            else
                Logging.Log3("No LDML Special collators to write ");
            
            if (collationOptions != null)
            {
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.COLLATION_OPTIONS, true);
                indent();
                for (int i=0; i < collationOptions.size(); i++)
                {
                    printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.TRANSLITERATION_MODULES, true);
                    print( (String)collationOptions.elementAt(i));
                    printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.TRANSLITERATION_MODULES, false);
                }
                outdent();
                printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.COLLATION_OPTIONS, false);
            }
            else
                Logging.Log3("No LDML Special collationOptions to write ");
            
            outdent();
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.COLLATIONS, false);
        }
    }
    
   
    public void writeCurrencies(Vector data /*Vector of vectors*/, Hashtable separators)
    {
        if ((data==null) || (data.size()==0))
        {
            Logging.Log1("No LDML currency data to write ");
            return;
        }
        
        println(LDMLConstants.CURRENCIES_O);
        indent();
        
        for (int i=0; i < data.size(); i++)
        {
            Vector inner = (Vector) data.elementAt(i);
           // inner holds data in following order : CurrencyID,CurrencySymbol,BankSymbol,CurrencyName,DecimalPlaces,default, usedInCompatibleFormatCode, legacyOnly (if defined)
            println("<" + LDMLConstants.CURRENCY + " " + LDMLConstants.TYPE + "=\"" + (String) inner.elementAt(2) + "\">");
            indent();
            println(LDMLConstants.DISPLAY_NAME_O + (String) inner.elementAt(3) + LDMLConstants.DISPLAY_NAME_C);
            println(LDMLConstants.SYMBOL_O + (String) inner.elementAt(1) + LDMLConstants.SYMBOL_C);
            if (m_bWriteCLDROnly == false)//below  writes OO.o specials only
            {
                String id = (String) inner.elementAt(0);
                String def = " " + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT + "=\"" + (String) inner.elementAt(5) + "\"";
                String uicfc = " " + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.USED_IN_COMP_FORMAT_CODES + "=\"" + (String) inner.elementAt(6) + "\"";
                String legacy = "";  //it's optional #IMPLIED=false
                if (inner.size() > 7) legacy = " " + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LEGACY_ONLY + "=\"" + (String) inner.elementAt(7) + "\"";
                
               println("<" + LDMLConstants.SPECIAL + " " + LDMLConstants.XMLNS + ":" + XMLNamespace.OPEN_OFFICE + "=\"" + XMLNamespace.OPEN_OFFICE_WWW + "\">" );
               indent();
               println("<" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.CURRENCY + def + uicfc + legacy + ">");
               indent ();
               printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.CURRENCY_ID, true);
               print(id);
               printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.CURRENCY_ID, false);
               outdent();
               printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.CURRENCY, false);
               outdent();
               println(LDMLConstants.SPECIAL_C);
            }
            outdent();
            println(LDMLConstants.CURRENCY_C);
        }
        
        outdent();
        println(LDMLConstants.CURRENCIES_C);
        
        }

        
    protected void writeSymbols(Hashtable data)
    {
        if ((data == null) || (data.size()==0))
        {
            Logging.Log1("No LDML symbols data to write ");
            return;
        }
        
        String decimal = (String) data.get(LDMLConstants.DECIMAL);
        String group = (String) data.get(LDMLConstants.GROUP);
        String list = (String) data.get(LDMLConstants.LIST);
          
        println(LDMLConstants.SYMBOLS_O);
        indent();
        
        if (decimal != null)
            println(LDMLConstants.DECIMAL_O + decimal + LDMLConstants.DECIMAL_C);
        else
            Logging.Log3("No LDML decimal symbol to write ");
        
        if (group != null)
            println(LDMLConstants.GROUP_O + group + LDMLConstants.GROUP_C);
        else
            Logging.Log3("No LDML group symbol to write ");
        
        if (list != null)
            println(LDMLConstants.LIST_O + list + LDMLConstants.LIST_C);
        else
            Logging.Log3("No LDML list symbol to write ");
        
        outdent();
        println(LDMLConstants.SYMBOLS_C);
        
    }
    
    protected void writeOOSymbols(Hashtable data)
    {
        if ((data == null) || (data.size()==0))
        {
            Logging.Log1("No OO.o symbols data to write ");
            return;
        }
        
        printlnNS (XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.SEPARATORS, true);
        indent();
        
        String dateSeparator = (String) data.get(OpenOfficeLDMLConstants.DATE_SEPARATOR);
        String timeSeparator = (String) data.get( OpenOfficeLDMLConstants.TIME_SEPARATOR);
        String longDateDayOfWeekSeparator = (String) data.get( OpenOfficeLDMLConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR);
        String longDateDaySeparator = (String) data.get( OpenOfficeLDMLConstants.LONG_DATE_DAY_SEPARATOR);
        String longDateMnothSeparator = (String) data.get( OpenOfficeLDMLConstants.LONG_DATE_MONTH_SEPARATOR);
        String longDateYearSeparator = (String) data.get( OpenOfficeLDMLConstants.LONG_DATE_YEAR_SEPARATOR);
        String time100SecSeparator = (String) data.get( OpenOfficeLDMLConstants.TIME_100SEC_SEPARATOR);
        
        if (dateSeparator !=null)
        {
            print("<" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DATE_SEPARATOR + ">");
            print(dateSeparator);
            println("</" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DATE_SEPARATOR + ">");
        }
        else
            Logging.Log3("No LDML Special dateSeparator symbol to write ");
        
        if (timeSeparator !=null)
        {
            print("<" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TIME_SEPARATOR + ">");
            print(timeSeparator);
            println("</" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TIME_SEPARATOR + ">");
        }
        else
            Logging.Log3("No LDML Special timeSeparator symbol to write ");
        
        if (longDateDayOfWeekSeparator !=null)
        {
            print("<" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR + ">");
            print(longDateDayOfWeekSeparator);
            println("</" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR + ">");
        }
        else
            Logging.Log3("No LDML Special longDateDayOfWeekSeparator symbol to write ");
        
        if (longDateDaySeparator !=null)
        {
            print("<" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_DAY_SEPARATOR + ">");
            print(longDateDaySeparator);
            println("</" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_DAY_SEPARATOR + ">");
        }
        else
            Logging.Log3("No LDML Special longDateDaySeparator symbol to write ");
        
        if (longDateMnothSeparator !=null)
        {
            print("<" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_MONTH_SEPARATOR + ">");
            print(longDateMnothSeparator);
            println("</" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_MONTH_SEPARATOR + ">");
        }
        else
            Logging.Log3("No LDML Special longDateMnothSeparator symbol to write ");
        
        if (longDateYearSeparator !=null)
        {
            print("<" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_YEAR_SEPARATOR + ">");
            print(longDateYearSeparator);
            println("</" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LONG_DATE_YEAR_SEPARATOR + ">");
        }
        else
            Logging.Log3("No LDML Special longDateYearSeparator symbol to write ");
        
        if (time100SecSeparator !=null)
        {
            print("<" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TIME_100SEC_SEPARATOR + ">");
            print(time100SecSeparator);
            println("</" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TIME_100SEC_SEPARATOR + ">");
        }
        else
            Logging.Log3("No LDML Special time100SecSeparator symbol to write ");
        
         
        outdent();
        printlnNS (XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.SEPARATORS, false);        
    }
    
    /* method will handle dateFormat, timeFormat, dateTimeFormat, currencyformat, decimalFormat, scientificFormat,
     *  percentFormat
     */
    protected void writeFormats(Hashtable formatElements, Hashtable patterns, Hashtable defaultNames, String fmts, String fmtLength, String fmt, String calendar)
    {  //calendar param only used in writeFlexibleDateTime
        
         if ((String) m_Aliases.get(OpenOfficeLDMLConstants.FORMAT) != null)
             return;  //there's a ref="xx_YY" for LC_FORMAT so there's nothing to write here

         
        if ((formatElements == null) || (formatElements.size()==0)
        || (patterns == null) || (patterns.size() ==0)
        || (defaultNames == null) //defaultName? in DTD
        || (fmts == null) || (fmtLength == null) || (fmt == null) )
        {
            Logging.Log1("No LDML " + fmts + " data to write ");
            return;
        }
        
         //uncoment for generating flex date time only
       //  if (fmts.compareTo (LDMLConstants.DATE_TIME_FORMATS) == 0)
      //   {
             println("<" + fmts + ">");
            indent();
      //   }
        
        //get the "long", "medium" and "short"
        writeFormatLengths(formatElements, patterns, defaultNames, OpenOfficeLDMLConstants.LONG, fmtLength, fmt);
        writeFormatLengths(formatElements, patterns, defaultNames, OpenOfficeLDMLConstants.MEDIUM, fmtLength, fmt);
        writeFormatLengths(formatElements, patterns, defaultNames, OpenOfficeLDMLConstants.SHORT, fmtLength, fmt);

        if (fmts.compareTo (LDMLConstants.DATE_TIME_FORMATS) == 0)
        {
            writeFlexibleDateTime (m_flexDateTimePatterns, calendar);
        }
        
    //    if (fmts.compareTo(LDMLConstants.DATE_TIME_FORMATS) == 0)
    //    {
            outdent();
            println("</" + fmts + ">");
    //    }
    }
    
    protected void writeFormatLengths(Hashtable formatElements, Hashtable patterns, Hashtable defaultNames, String type, String fmtLength, String fmt)
    {
        println("<" + fmtLength + " " + LDMLConstants.TYPE + "=\"" + type + "\">");
        indent();
        
        //write the default if any
        String def = (String) formatElements.get(LDMLConstants.DEFAULT + " " + type);
        if (def != null)
            println("<" + LDMLConstants.DEFAULT + " " + LDMLConstants.TYPE + "=\"" + def + "\"/>");
        else
            Logging.Log4("No LDML Special default to write for " + fmtLength + " " + type);
        
        Enumeration keys = formatElements.keys();
        Enumeration data = formatElements.elements();
        while (keys.hasMoreElements() == true)
        {
            String key = (String) keys.nextElement();
            //Strings and Hashtables are stored, we're only interested in Hashtables
            Object obj = data.nextElement();
            if (obj instanceof String)
                continue;
            
            Hashtable inner = (Hashtable) obj;
            Enumeration keys_inner = inner.keys();
            Enumeration data_inner = inner.elements();
            while (keys_inner.hasMoreElements() == true)
            {
                String key_inner = (String) keys_inner.nextElement();
                String datum_inner = (String) data_inner.nextElement();
                
                if (key_inner.compareTo(LDMLConstants.TYPE)==0)
                {
                    if (type.compareTo(datum_inner) ==0)
                    {
                        //it's the right type (long, medium or short) so print it
                        println("<" + fmt + " " + LDMLConstants.TYPE + "=\"" + key + "\">");
                        indent();
                        
                        //pattern
                        String pattern = (String) patterns.get(key);
                        if (pattern != null)
                        {
                            print(LDMLConstants.PATTERN_O);
                            print(pattern);
                            println(LDMLConstants.PATTERN_C);
                        }
                        else
                            Logging.Log3("No LDML Special pattern to write for " + fmt + " " + key);
                                     
                        String defaultName = (String) defaultNames.get(key);
                        if (defaultName == null)
                        {
                            printSpecial(XMLNamespace.OPEN_OFFICE, XMLNamespace.OPEN_OFFICE_WWW, inner, false, true);
                        }
                        else
                        {
                            printSpecial(XMLNamespace.OPEN_OFFICE, XMLNamespace.OPEN_OFFICE_WWW, inner, true, true);
                            indent();
                            Hashtable attribs = new Hashtable();
                            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.DEFAULT_NAME, attribs, true, true, false);
                            print(defaultName);
                            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.DEFAULT_NAME, false);
                            outdent();
                            println(LDMLConstants.SPECIAL_C);
                        }
                        
                        outdent();
                        println("</" + fmt + ">");
                    }
                }
            } //end inner while loop
        } //end outer while loop
        outdent();
        println("</" + fmtLength + ">");
    }
    
    //wirte <availableFormats> and <appendItems>
    protected void writeFlexibleDateTime (Vector patterns, String calendar)
    {
        if (patterns == null) 
        {
            Logging.Log3("Not writing flexible date time formsts (N.B. this is a CLDR 1.4 feature)");
            return;
        }
        
        //temp for merge
   /*     String file = OOLocaleReader.m_LangId;
        if (OOLocaleReader.m_Country_CountryID != null) file = file + "_" + OOLocaleReader.m_Country_CountryID;
        file += ".xml";
        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter("./ldml2/"+file,true));
            out.write ("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            out.write ("<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/1.4/ldml.dtd\">\n");
            out.write ("<ldml>\n");
            out.write ("    <identity>\n");
            out.write ("        <version number=\"1.4\"/>\n");
            out.write ("        <generation date=\"23-11-05\"/>\n");
            out.write ("        <language type=\"" + OOLocaleReader.m_LangId + "\"/>\n");
            if (OOLocaleReader.m_Country_CountryID != null)
                out.write ("         <territory type=\"" + OOLocaleReader.m_Country_CountryID + "\"/>\n");
            out.write ("    </identity>\n");
            out.write ("    <dates>\n");
            out.write ("        <calendars>\n");
            out.write ("            <calendar type=\"gregorian\">\n");
            out.write ("                <dateTimeFormats>\n");
            out.write  ("                    <availableFormats draft=\"true\">\n");                   
            out.close();
        }
        catch (IOException e)
        {}  */
        //end temp
            
        //OO.o formats belong to different calendar types so make sure to write to appropriate places
        // look for [~hijri]  ][~jewish]  >[~hanja]  [~buddhist]
        //skip ones with [HH] or [NatNum1] [natnum1] etc
        
        println(LDMLConstants.AVAIL_FMTS_O);
        indent();
        
        //remove duplicates
        Vector patterns_no_dups = new Vector ();
        for (int i=0; i < patterns.size(); i++)
        {
            String pattern = (String) patterns.elementAt(i);
            if (patterns_no_dups.contains(pattern) == false)
            {
                patterns_no_dups.add (pattern);
         //       System.err.println ("INFO: Adding pattern : " + pattern );
            }
            else
                System.err.println ("INFO: Skipping duplicate pattern : " + pattern + " for availableFormats");
        }
        
        int index=0;
        for (int i=0; i < patterns_no_dups.size(); i++)
        {
            String pattern = (String) patterns_no_dups.elementAt(i);
            if ((pattern != null) && (pattern.indexOf('[') == -1))  // no [] => gregorian
            {
                index++;
          //      print(LDMLConstants.DATE_FMT_ITEM_O);
                //id is 1 indexed (just like months)
                print("<" + LDMLConstants.DATE_FMT_ITEM + " " + LDMLConstants.ID + "=\"" + Integer.toString(index) + "\">");
                print(pattern);
                println(LDMLConstants.DATE_FMT_ITEM_C);
                
                //temp for merge
         /*       try
                {
                    BufferedWriter out = new BufferedWriter(new FileWriter("./ldml2/"+file,true));
                    out.write("                        <" + LDMLConstants.DATE_FMT_ITEM + " " + LDMLConstants.ID + "=\"" + Integer.toString(index) + "\">" + pattern + LDMLConstants.DATE_FMT_ITEM_C + "\n");
                    out.close ();
                }
                catch (IOException e)
                {}*/
                //end temp
            }
        }
        
        outdent();
        println(LDMLConstants.AVAIL_FMTS_C);
        
        //temp for merge to cLDR 1.4
   /*     try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter("./ldml2/"+file,true));
            out.write ("                    </availableFormats>\n");
            out.write ("                </dateTimeFormats>\n");
            out.write ("            </calendar>\n");
            out.write ("        </calendars>\n");
            out.write ("    </dates>\n");
            out.write ("</ldml>\n");
            out.close ();
        }
        catch (IOException e)
        {}  */
        //end temp
    }
    
    
    //writes the format replaceTo and replaceFrom (if present) to LDML
    // input Vector has 1 entry qwhich is a hashtable
    protected void writeSpecialFormat(Vector format)
    {
        if ((format==null) || (format.size()==0))
        {
            Logging.Log1("No LDML Special format replaceTo or replaceFrom data to write ");
            return;
        }
        
        Hashtable attribs = (Hashtable) format.elementAt(0);
        if ((String) m_Aliases.get(OpenOfficeLDMLConstants.FORMAT) == null)
            attribs.remove(OpenOfficeLDMLConstants.REF);   //an y reffed data has already been written
        
        if (attribs.size()==0)
            Logging.Log3("No LDML Special format replaceTo or replaceFrom data to write" );
        
        printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.FORMAT, attribs, true, false, true);
    }
    
    private  void writeLocale(Hashtable data)
    {
        if ((data == null) || (data.size()==0))
        {
            Logging.Log1("No LDML openOddice:locale data to write ");
            return;
        }
        
        print("<" + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LOCALE);
        
        String str = (String) data.get(OpenOfficeLDMLConstants.VERSION_DTD);
        if (str != null)
            print(" " + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.VERSION_DTD + "=\"" + str + "\"");
        else
            Logging.Log1("No " + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.VERSION_DTD + " to write" );
        
        str = (String) data.get(OpenOfficeLDMLConstants.ALLOW_UPDATE_FROM_CLDR);
        if (str != null)
            print(" " + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.ALLOW_UPDATE_FROM_CLDR + "=\"" + str + "\"");
        else
            Logging.Log1("No " + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.ALLOW_UPDATE_FROM_CLDR + " to write" );
            
        str = (String) data.get(OpenOfficeLDMLConstants.VERSION);
        if (str != null)
            print(" " + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.VERSION + "=\"" + str + "\"");
        else
            Logging.Log1("No " + XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.VERSION + " to write" );
      
        println ("/>");
    }
}