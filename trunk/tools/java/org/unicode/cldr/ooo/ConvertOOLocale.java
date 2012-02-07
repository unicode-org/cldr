/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

import java.io.File;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Vector;

import org.unicode.cldr.icu.LDMLConstants;

/*
 * works as follows :
 *      - it invokes OOLoccaleReader to read OO XML data,
 *      - calls OOToLDMLMapper to map this data to LDML data
 *      - calls LDMLLocaleWriterForOO which writes this data to file in LDML
 *
 * depending on the args will convert single files or full directories
 *
 * areas of overlap bwtween OO.o special elements and CLDR .14 standardf elements :
 * - quarters
 * - flexible date and time
 */

public class ConvertOOLocale
{
    private static final int OPT_SINGLE = 0x0001;
    private static final int OPT_BULK = 0x0002;
    private static final int OPT_DATE_TIME = 0x0004;
    private static final int OPT_RES_REFS = 0x0008;
    private static final int OPT_DEST_DIR = 0x0100;  //specify the dest dir where output is written
    private static final int OPT_DTD_DIR = 0x0200;   //specify the folder where ALL the dtds are located
    private static final int OPT_CLDR_VER = 0x0400;
    private static final int OPT_CLDR_ONLY = 0x0800;  //only geenrate standard CLDR LDML data from OO.o, no specials
    private static final int OPT_INVALID = 0x4000;
    
    private static final String SINGLE = "-single";
    private static final String BULK = "-bulk";
    private static final String DATE_TIME = "-date_time";  //will convert to CLDR compliant date/time syntax
    private static final String RES_REFS = "-res_refs";  //resolve all refs fully, otherwise only resolve those for non OO.0 specific element categories
    private static final String DEST_DIR = "-dest_dir";
    private static final String DTD_DIR  = "-dtd_dir";
    private static final String CLDR_VER  = "-cldr_ver";
    private static final String CLDR_ONLY  = "-cldr_only";
    
    private static final String USER_OPTIONS[] =
    {
        SINGLE,
        BULK,
        DATE_TIME,
        RES_REFS,
        DEST_DIR,
        DTD_DIR,
        CLDR_VER,
        CLDR_ONLY
    };
    
    private String m_inFile = null;
    private String m_inDir = null;
    private LDMLLocaleWriterForOO m_LDMLLocaleWriterForOO = null;
    private String m_localeStr = null;
    
    private int m_iOptions = 0;
    
    private String m_dest_dir = "main";
    private String m_dtd_dir = null;
    private String m_cldr_ver = "1.3";    //default if not specified on CLI
    private boolean m_bConvertDateTime = false;  //if true => write date, time in LDML else write in OO format
    private boolean m_bWriteCLDROnly = false;   //if true don't write any speecials to LDML
    
    /** Creates a new instance of convertOOLocale */
    public ConvertOOLocale(String [] options)
    {
        m_iOptions = identifyOptions(options);
        
        if  ((m_iOptions & OPT_DATE_TIME) != 0)
        {
            m_bConvertDateTime = true;
        }
        
        if ((m_iOptions & OPT_INVALID) != 0)
        {
            printUsage();
        }
        else if ((m_iOptions & OPT_SINGLE) != 0)
        {
            if (m_inFile == null)
                printUsage();
            else
                processSingle();
        }
        else if ((m_iOptions & OPT_BULK) != 0)
        {
            if (m_inDir == null)
                printUsage();
            else
                processBulk();
        }
        else
        {
            printUsage();
        }
        
        
    }
    
    public static void main(String[] args)
    {
        try
        {
            //log levels 1 to 4, 1 is highest
            Logging.setLevel(true /*level 1*/,  /*level 2 */ true, true /*level 3 */, true /*level 4 */);
            new ConvertOOLocale(args);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            System.err.println("Unknown error: "+t);
        }
    }
    
    private short identifyOptions(String[] options)
    {
        if (options == null)
            return OPT_INVALID;
        
        short result = 0;
        
        for (int j = 0; j < options.length; j++)
        {
            String option = options[j];
            if (option.startsWith("-"))
            {
                boolean optionRecognized = false;
                for (short i = 0; i < USER_OPTIONS.length; i++)
                {
                    if (USER_OPTIONS[i].equals(option))
                    {
                        result |= (short)(1 << i);
                        optionRecognized = true;
                        break;
                    }
                }
                if (!optionRecognized)
                {
                    result |= OPT_INVALID;
                }
                
                if ((option.compareTo(SINGLE)==0)
                && (j < (options.length-1)))
                {
                    m_inFile = options [++j];
                    File fin = new File(m_inFile);
                    if (fin.isFile()==false)
                        result |= OPT_INVALID; //input file and output dir must exist
                    else
                    {
                        m_localeStr = Utilities.extractLocaleFromFilename(m_inFile);
                    }
                }
                else if ((option.compareTo(BULK)==0)
                && (j < (options.length-1)))
                {
                    m_inDir = options [++j];
                    File din = new File(m_inDir);
                    if (din.isDirectory()==false)
                        result |= OPT_INVALID;
                }
                
                if ((option.compareTo(DEST_DIR)==0)
                && (j < (options.length-1)))
                {
                    m_dest_dir = options [++j];
                //    System.out.println (m_dest_dir);
                }
                
                if ((option.compareTo(DTD_DIR)==0)
                && (j < (options.length-1)))
                {
                    m_dtd_dir = options [++j];
                    File din = new File(m_dtd_dir);
                    if (din.isDirectory()==false)
                        result |= OPT_INVALID;
                }
                
                if ((option.compareTo(CLDR_VER)==0)
                && (j < (options.length-1)))
                {
                    m_cldr_ver = options [++j];
                }            
                
                if (option.compareTo(CLDR_ONLY)==0)
                    m_bWriteCLDROnly = true;
            }
        }
        return result;
    }
    
    private void printUsage()
    {
        System.err.println("");
        System.err.println("DESCRIPTION:");
        System.err.println("  Converts OpenOffice.org locale data to CLDR's LDML format");
        System.err.println("");
        System.err.println("USAGE:");
        System.err.println("  ConvertOOLocalw [-single inFile] | [-bulk inDir]  [-date_time] [-res_refs] [-dest_dir] [-dtd_dir] [-cldr_ver]");
        System.err.println("");
        System.err.println("OPTIONS:");
        System.err.println("  " + SINGLE + "    : converty the specified file");
        System.err.println("  " + BULK + "      : convert all xml files in the specified directory");
        System.err.println("  " + DATE_TIME + " : convert date/time format strings to be LDML compliant");
        System.err.println("  " + RES_REFS + "  : resolve fully all OpenOffice.org XML refs");
        System.err.println("  " + DEST_DIR + "  : the dir where output is written (it will be created if non existant) (dafault=./main) ");
        System.err.println("  " + DTD_DIR + "   : the location of the dtds (ldml.dtd and ldmlOpenOffice.dtd)");
        System.err.println("  " + CLDR_VER + "  : the CLDR version (default =1.3) this is used to determine the URL of the DTD if -dtd_dir not specified");
        System.err.println("  " + CLDR_ONLY + " : only write data that can be mapped to CLDR. Used for importing new locales into CLDR.");
        System.err.println("");
        System.err.println("EXAMPLES:");
        System.err.println("  ConvertOOLocale -single OOFileName.xml");
        System.err.println("    will create the LDML file in the \"main\" subfolder (\"main\" will be created if it doesn't exist)");
        System.err.println("  ConvertOOLocale -bulk OODirName  ");
        System.err.println("    will create the LDML files in the \"main\" subfolder (\"main\" will be created if it doesn't exist)");
        System.err.println("");
    }
    
    private void processSingle()
    {
        //read OpenOffice doc into memory
        boolean bResolveRefs = false;
        if ((m_iOptions & OPT_RES_REFS) != 0)
            bResolveRefs = true;
        
        OOLocaleReader reader = new OOLocaleReader(m_inFile);
        reader.readDocument(bResolveRefs);
        
        //create instance of LDMLLocaleWriterForOO which does the writing to file
        PrintStream ps = Utilities.setLocaleWriter2(m_localeStr, null, m_dest_dir);
        m_LDMLLocaleWriterForOO = new LDMLLocaleWriterForOO(ps, m_bWriteCLDROnly);
        
        Hashtable aliases = OOToLDMLMapper.mapRefs(reader.m_Refs2);
        m_LDMLLocaleWriterForOO.setAliases(aliases);
        
        //start writing the LDML
        String ns = null;
        if (m_bWriteCLDROnly == false) ns = XMLNamespace.OPEN_OFFICE;  // don't write OO.o dtd line if importing new locale to CLDR
        m_LDMLLocaleWriterForOO.open(ns, m_dtd_dir, "ldmlOpenOffice.dtd", m_cldr_ver);
        
        Hashtable data = new Hashtable();
        
        //###### write identity ######
        data.put(LDMLConstants.VERSION + " " + LDMLConstants.NUMBER, "$Revision$");  //this is the CVS version not the CLDR version
    //    data.put(LDMLConstants.GENERATION, "Generated from ConvertOOLocale");  //comment is superflous and would need to be removed for importing new locales inot CLDR
        if (reader.m_LangId!=null)
            data.put(LDMLConstants.LANGUAGE + " " + LDMLConstants.TYPE, reader.m_LangId);
        if (reader.m_Country_CountryID!=null)
            data.put(LDMLConstants.TERRITORY + " " + LDMLConstants.TYPE, reader.m_Country_CountryID);
        
        //write PlatformID as special under <identity>
        if (reader.m_PlatformID != null) data.put(OpenOfficeLDMLConstants.PLATFORM_ID, reader.m_PlatformID);
        
        m_LDMLLocaleWriterForOO.writeIdentity(data);
        data.clear();
        
        //###### write localeDisplayNames ######
        //defaultNames will be written as special under localeDisplayNames
        if (reader.m_LangId!=null) data.put(LDMLConstants.LANGUAGE + " " + LDMLConstants.TYPE, reader.m_LangId );
        if (reader.m_Language_DefaultName!=null) data.put(OpenOfficeLDMLConstants.DEFAULT_NAME + " " + reader.m_LangId, reader.m_Language_DefaultName);
        if (reader.m_Country_CountryID!=null) data.put(LDMLConstants.TERRITORY + " " + LDMLConstants.TYPE, reader.m_Country_CountryID);
        if (reader.m_Country_DefaultName!=null) data.put(OpenOfficeLDMLConstants.DEFAULT_NAME + " " + reader.m_Country_CountryID , reader.m_Country_DefaultName);
        m_LDMLLocaleWriterForOO.writeLocaleDisplaynames(data);
        data.clear();
        
        //###### write delimiters ######
        Hashtable markers = OOToLDMLMapper.MapDelimiters(reader.m_Markers);
        if (markers != null) data.putAll(markers);
        m_LDMLLocaleWriterForOO.writeDelimiterss(data);
        data.clear();
        
        //###### write measurement ######
        String measSys = OOToLDMLMapper.MapMeasurementSystem(reader.m_MeasurementSystem);
        if (measSys!=null) data.put(LDMLConstants.MEASUREMENT, measSys);
        m_LDMLLocaleWriterForOO.writeMeasurement(data);
        data.clear();
        
        //###### write calendars ######
        Vector calendars = OOToLDMLMapper.MapCalendar(reader.m_Calendars);
        if (calendars!=null) data.put(LDMLConstants.CALENDAR + " " + LDMLConstants.TYPE, calendars);
        
        Hashtable abbrDays = OOToLDMLMapper.MapDays(reader.m_AbbrDays);
        if (abbrDays!=null) data.put(LDMLConstants.DAY_WIDTH + " " + LDMLConstants.ABBREVIATED, abbrDays);
        
        Hashtable wideDays = OOToLDMLMapper.MapDays(reader.m_WideDays);
        if (wideDays!=null) data.put(LDMLConstants.DAY_WIDTH + " " + LDMLConstants.WIDE, wideDays);
        
        Hashtable abbrMonths = OOToLDMLMapper.MapMonths(reader.m_AbbrMonths);
        if (abbrMonths!=null) data.put(LDMLConstants.MONTH_WIDTH + " " + LDMLConstants.ABBREVIATED, abbrMonths);
        
        Hashtable wideMonths = OOToLDMLMapper.MapMonths(reader.m_WideMonths);
        if (wideMonths!=null) data.put(LDMLConstants.MONTH_WIDTH + " " + LDMLConstants.WIDE, wideMonths);
        
        //reservedWords contains quarters, true/false, above/below
        //Quarters now part of CLDR 1.4 standard
        if (Float.parseFloat(m_cldr_ver) > 1.3999)
        {
            Hashtable wideQuarters = OOToLDMLMapper.MapWideQuarters(reader.m_ReservedWords);
            if (wideQuarters != null) data.put(LDMLConstants.QUARTER_WIDTH+ " " + LDMLConstants.WIDE, wideQuarters);
            Hashtable abbrQuarters = OOToLDMLMapper.MapAbbrQuarters(reader.m_ReservedWords);
            if (abbrQuarters != null) data.put(LDMLConstants.QUARTER_WIDTH+ " " + LDMLConstants.ABBREVIATED, abbrQuarters);
        
            //don't write to OO.org specials
            reader.m_ReservedWords.remove (OOConstants.QUARTER_1_WORD);
            reader.m_ReservedWords.remove (OOConstants.QUARTER_2_WORD);
            reader.m_ReservedWords.remove (OOConstants.QUARTER_3_WORD);
            reader.m_ReservedWords.remove (OOConstants.QUARTER_4_WORD);
            reader.m_ReservedWords.remove (OOConstants.QUARTER_1_ABBREVIATION);
            reader.m_ReservedWords.remove (OOConstants.QUARTER_2_ABBREVIATION);
            reader.m_ReservedWords.remove (OOConstants.QUARTER_3_ABBREVIATION);
            reader.m_ReservedWords.remove (OOConstants.QUARTER_4_ABBREVIATION);
        }
        
        Hashtable abbrEras = OOToLDMLMapper.MapEras(reader.m_AbbrEras);
        if (abbrEras!=null) data.put(LDMLConstants.ERAABBR, abbrEras);
        
        Hashtable wideEras = OOToLDMLMapper.MapEras(reader.m_WideEras);
        if (wideEras!=null) data.put(LDMLConstants.ERANAMES, wideEras);
        
        Hashtable startDayOfWeek = OOToLDMLMapper.MapStartDayOfWeek(reader.m_StartDayOfWeek);
        if (startDayOfWeek!=null) data.put(LDMLConstants.FIRSTDAY, startDayOfWeek);
        
        Hashtable minDaysInFirstWeek = OOToLDMLMapper.MapMinDaysInFirstWeek(reader.m_MinDaysInFirstweek);
        if (minDaysInFirstWeek!=null) data.put(LDMLConstants.MINDAYS, minDaysInFirstWeek);
        
        if (reader.m_TimeAM!=null) data.put(LDMLConstants.AM, reader.m_TimeAM);
        if (reader.m_TimePM!=null) data.put(LDMLConstants.PM, reader.m_TimePM);
        
        
   /*     try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter("date_time",true));
            out.write(m_localeStr + "\n");
            out.close();
        }
        catch (IOException e)
        {}*/
        
        //get the DATE FormatElements
        Hashtable formatElements_date = new Hashtable();
        Hashtable formatCodes_date = new Hashtable();
        Hashtable formatDefaultNames_date = new Hashtable();
        Hashtable flexDate = new Hashtable ();
        
        OOToLDMLMapper.MapFormatElements(reader.m_FormatElements,
        reader.m_FormatCodes,
        reader.m_FormatDefaultNames,
        formatElements_date,
        formatCodes_date,    //the patterns
        formatDefaultNames_date,
        OOConstants.FEU_DATE, m_localeStr, m_bConvertDateTime, flexDate);
        
        data.put(LDMLConstants.DATE_FORMAT, formatElements_date);
        data.put(LDMLConstants.DATE_FORMAT + " " + LDMLConstants.PATTERN, formatCodes_date);
        data.put(LDMLConstants.DATE_FORMAT + OpenOfficeLDMLConstants.DEFAULT_NAME, formatDefaultNames_date);
        
        //get the TIME FormatElements
        Hashtable formatElements_time = new Hashtable();
        Hashtable formatCodes_time = new Hashtable();
        Hashtable formatDefaultNames_time = new Hashtable();
        Hashtable flexTime = new Hashtable ();
        OOToLDMLMapper.MapFormatElements(reader.m_FormatElements,
        reader.m_FormatCodes,
        reader.m_FormatDefaultNames,
        formatElements_time,
        formatCodes_time,    //the patterns
        formatDefaultNames_time,
        OOConstants.FEU_TIME, m_localeStr, m_bConvertDateTime, flexTime);
        
        data.put(LDMLConstants.TIME_FORMAT, formatElements_time);
        data.put(LDMLConstants.TIME_FORMAT + " " + LDMLConstants.PATTERN, formatCodes_time);
        data.put(LDMLConstants.TIME_FORMAT + OpenOfficeLDMLConstants.DEFAULT_NAME, formatDefaultNames_time);
        
        //get the DATE_TIME FormatElements
        Hashtable formatElements_date_time = new Hashtable();
        Hashtable formatCodes_date_time = new Hashtable();
        Hashtable formatDefaultNames_date_time = new Hashtable();
        Hashtable flexDateTime = new Hashtable ();
        OOToLDMLMapper.MapFormatElements(reader.m_FormatElements,
        reader.m_FormatCodes,
        reader.m_FormatDefaultNames,
        formatElements_date_time,
        formatCodes_date_time,    //the patterns
        formatDefaultNames_date_time,
        OOConstants.FEU_DATE_TIME, m_localeStr, m_bConvertDateTime, flexDateTime);
        
        data.put(LDMLConstants.DATE_TIME_FORMAT, formatElements_date_time);
        data.put(LDMLConstants.DATE_TIME_FORMAT + " " + LDMLConstants.PATTERN, formatCodes_date_time);
        data.put(LDMLConstants.DATE_TIME_FORMAT + OpenOfficeLDMLConstants.DEFAULT_NAME, formatDefaultNames_date_time);
        
        //fill a vector of flexible date time patterns
    // flex data time new in 1.4 standard
        //TODO : fix the ids to be pattenr skeleton not sequential number
/*        if (Float.parseFloat(m_cldr_ver) > 1.3999)
        {
            Vector flexDateTimePatterms = new Vector();
            Collection coll = flexDate.values();
            flexDateTimePatterms.addAll(coll);
            coll = flexTime.values();
            flexDateTimePatterms.addAll(coll);
            coll = flexDateTime.values();
            flexDateTimePatterms.addAll(coll);
            data.put(LDMLConstants.AVAIL_FMTS, flexDateTimePatterms);
        }*/
        
        m_LDMLLocaleWriterForOO.writeDates(data);
        data.clear();
        
        //###### numbers ######
        Hashtable symbols = new Hashtable();
        symbols = OOToLDMLMapper.MapSymbols(reader.m_Separators);
        if (symbols != null) data.put(LDMLConstants.SYMBOLS, symbols);
             
        data.put(LDMLConstants.CURRENCIES, reader.m_CurrencyData);
        
        //get the FIXED_NUMBER FormatElements
        //OO dtd also mentions FRACTION_NUMBER but its not used in any XML files
        Hashtable formatElements_fn = new Hashtable();
        Hashtable formatCodes_fn = new Hashtable();
        Hashtable formatDefaultNames_fn = new Hashtable();
        OOToLDMLMapper.MapFormatElements(reader.m_FormatElements,
        reader.m_FormatCodes,
        reader.m_FormatDefaultNames,
        formatElements_fn,
        formatCodes_fn,
        formatDefaultNames_fn,
        OOConstants.FEU_FIXED_NUMBER, m_localeStr, m_bConvertDateTime, null);
        
        data.put(LDMLConstants.DECIMAL_FORMATS, formatElements_fn);
        data.put(LDMLConstants.DECIMAL_FORMATS + " " + LDMLConstants.PATTERN, formatCodes_fn);
        data.put(LDMLConstants.DECIMAL_FORMATS + OpenOfficeLDMLConstants.DEFAULT_NAME, formatDefaultNames_fn);
        
        //get the SCIENTIFIC_NUMBER FormatElements
        Hashtable formatElements_sn = new Hashtable();
        Hashtable formatCodes_sn = new Hashtable();
        Hashtable formatDefaultNames_sn = new Hashtable();
        OOToLDMLMapper.MapFormatElements(reader.m_FormatElements,
        reader.m_FormatCodes,
        reader.m_FormatDefaultNames,
        formatElements_sn,
        formatCodes_sn,
        formatDefaultNames_sn,
        OOConstants.FEU_SCIENTIFIC_NUMBER, m_localeStr, m_bConvertDateTime, null);
        
        data.put(LDMLConstants.SCIENTIFIC_FORMATS, formatElements_sn);
        data.put(LDMLConstants.SCIENTIFIC_FORMATS + " " + LDMLConstants.PATTERN, formatCodes_sn);
        data.put(LDMLConstants.SCIENTIFIC_FORMATS + OpenOfficeLDMLConstants.DEFAULT_NAME, formatDefaultNames_sn);
        
        //get the PERCENT_NUMBER FormatElements
        Hashtable formatElements_pn = new Hashtable();
        Hashtable formatCodes_pn = new Hashtable();
        Hashtable formatDefaultNames_pn = new Hashtable();
        OOToLDMLMapper.MapFormatElements(reader.m_FormatElements,
        reader.m_FormatCodes,
        reader.m_FormatDefaultNames,
        formatElements_pn,
        formatCodes_pn,
        formatDefaultNames_pn,
        OOConstants.FEU_PERCENT_NUMBER, m_localeStr, m_bConvertDateTime, null);
        
        data.put(LDMLConstants.PERCENT_FORMATS, formatElements_pn);
        data.put(LDMLConstants.PERCENT_FORMATS + " " + LDMLConstants.PATTERN, formatCodes_pn);
        data.put(LDMLConstants.PERCENT_FORMATS + OpenOfficeLDMLConstants.DEFAULT_NAME, formatDefaultNames_pn);
        
        //get the CURRENCY FormatElements
        Hashtable formatElements_c = new Hashtable();
        Hashtable formatCodes_c = new Hashtable();
        Hashtable formatDefaultNames_c = new Hashtable();
        OOToLDMLMapper.MapFormatElements(reader.m_FormatElements,
        reader.m_FormatCodes,
        reader.m_FormatDefaultNames,
        formatElements_c,
        formatCodes_c,
        formatDefaultNames_c,
        OOConstants.FEU_CURRENCY, m_localeStr, m_bConvertDateTime, null);
            
        data.put(LDMLConstants.CURRENCY_FORMATS, formatElements_c);
        data.put(LDMLConstants.CURRENCY_FORMATS + " " + LDMLConstants.PATTERN, formatCodes_c);
        data.put(LDMLConstants.CURRENCY_FORMATS + OpenOfficeLDMLConstants.DEFAULT_NAME, formatDefaultNames_c);
        
        m_LDMLLocaleWriterForOO.writeNumbers(data);
        data.clear();
        
        
        //###### write remaining OpenOffice.org specials under top level <special> element  ######
        
        //any containers with attribute names should undergo MapFirstCharToLowerCase
        // as in OpenOffice DTD most attribute names start with capital, whereas all attribs and eleemnts
        // in ldml for OO start with lower case
        //MapFirstCharToLowerCase not needed for containers with no attrib names in them
        
        Hashtable OOsymbols = new Hashtable();
        OOsymbols = OOToLDMLMapper.MapOOSymbols(reader.m_Separators);
        if (OOsymbols != null) data.put(LDMLConstants.SYMBOLS, OOsymbols);

        Hashtable forbiddenChars = OOToLDMLMapper.MapFirstCharToLowerCase(reader.m_ForbiddenChars);
        if (forbiddenChars != null) data.put(OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS, forbiddenChars);
        
        //reservedWords contains quarters, true/false, above/below
        Hashtable reservedWords = OOToLDMLMapper.MapFirstCharToLowerCase(reader.m_ReservedWords);
        if (reservedWords != null) data.put(OpenOfficeLDMLConstants.RESERVED_WORDS, reservedWords);
        
        Vector numLevels = OOToLDMLMapper.MapFirstCharToLowerCase(reader.m_NumberingLevels);
        if (numLevels != null) data.put(OpenOfficeLDMLConstants.NUMBERING_LEVELS, numLevels);
        
        Vector  OutlineNumberingLevels = OOToLDMLMapper.MapFirstCharToLowerCase(reader.m_OutlineNumberingLevels);
        if (OutlineNumberingLevels != null) data.put(OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVELS, OutlineNumberingLevels);
        
        Vector translits = OOToLDMLMapper.MapFirstCharToLowerCase(reader.m_Transliterations);
        if (translits != null) data.put(OpenOfficeLDMLConstants.TRANSLITERATIONS, translits);
        
        if (reader.m_SearchOptions != null) data.put(OpenOfficeLDMLConstants.SEARCH_OPTIONS, reader.m_SearchOptions);
        
        if (reader.m_IndexKeys != null) data.put(OpenOfficeLDMLConstants.INDEX_KEY, reader.m_IndexKeys);
        if (reader.m_IndexKeysData != null) data.put(OpenOfficeLDMLConstants.INDEX, reader.m_IndexKeysData);
        if (reader.m_UnicodeScript != null) data.put(OpenOfficeLDMLConstants.UNICODE_SCRIPT, reader.m_UnicodeScript);
        if (reader.m_FollowPageWord != null) data.put(OpenOfficeLDMLConstants.FOLLOW_PAGE_WORD, reader.m_FollowPageWord);
        
        Vector collators = OOToLDMLMapper.MapFirstCharToLowerCase(reader.m_Collators);
        if (collators != null) data.put(OpenOfficeLDMLConstants.COLLATOR, collators);
        if (reader.m_CollationOptions != null) data.put(OpenOfficeLDMLConstants.COLLATION_OPTIONS, reader.m_CollationOptions);
        
        Vector format = OOToLDMLMapper.MapFirstCharToLowerCase(reader.m_LCFormat);
        if (format != null) data.put(OpenOfficeLDMLConstants.FORMAT, format);
        
        Hashtable localeInfo = OOToLDMLMapper.mapLocaleInfo(reader.m_LocaleInfo);
        if (localeInfo != null) data.put(OpenOfficeLDMLConstants.LOCALE, localeInfo);
        
        m_LDMLLocaleWriterForOO.writeSpecial(data);
        data.clear();
             
        m_LDMLLocaleWriterForOO.close();
        
    }
    
    private void processBulk()
    {
        //loop thru' dir list and call processSingle () for each file
        File inDir = new File(m_inDir);
        String [] fileList = inDir.list();
        
        for (int j=0; j<fileList.length; j++)
        {
            String file = fileList[j];
            if (file.endsWith(".xml"))
            {
                m_inFile = m_inDir + "/" + file;
                m_localeStr = Utilities.extractLocaleFromFilename(file);
                processSingle();
            }
        }
    }
    
}