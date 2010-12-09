/***********************************************m_localeStr*************************
 * Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
 *************************************************************************/

/* converts LDML to OpenOffice.org XML format
 * there are 3 ways to use this app :
 * 1. Conversion back to OpenOffice.org XML of OO data which was previously converted to LDML,
 *    this is a simple round trip conversion which is useful for testing the tools
 * 2. Conversion to OO XML of OpenOffice.org specific data from 1 above merged with CLDR standard data,
 *    this is the step to perform when it is desired to generate OO data from CLDR
 * 3. Conversion of CLDR data only to OpenOffice.org format.
 *    this is the step to perform when generating a brand new OpenOffice.org locale
 *
 *
 * Design Decisions :
 *    - LC_INFO (country and lang IDs should be same anyway, DefaultNames are only info so leave the Oo.o ones
 *    - if locale not found in CLDR, then jsut write back the OO.o locale
 *    - wide era data is NOT sourced from CLDR yet as there are too many gaps
      - delimiters (quotations) are NOT source yet from cldr 1.4 although most data looks reasonable, await 1 more release
      - quarters are are NOT source yet from cldr 1.4 although most data looks reasonable, await 1 more release
      - minDays, firstDay and measurementSystem are read from supplemental.xml if it's 1.4 or later. 
             As this data is country based, for language only locales like ia, the Rest Of World value will be used.
      - Currency :
	- conflicts in default currency between OO.o and CLDR are written to currency.txt. 
	- If CLDR and OO.o disagree on the default curency code, use the Oo.o one anyway, it's too complicated to handle all the cases here. The default currency should be resolved manually by referring to currency.txt
	- non default currencies are taken from Oo.o
	- symbols and displayName of the default currency (only) come from CLDR
	- decimal places of all currencies come from CLDR
	- all other data is from OO.o
	- added handling of legacyOnly attribute
 *   - if no CLDR lcoale found , write back the OO.o data
 *   - if round trip then don't read measurementSystem, StartDay or FirstDay from supplemental, just write back the Oo.o data
 *        (NB : OO.o "Metri" is mapped to CLDR "metric" in OO->LDML mapper
 *   - handles currency symbol choice like INR
 * 
 * - workarounds (look for "workaround" in processSingle () ) :
 *      - zh_TW ROC eras    (not in CLDR 1.3, probably in 1.4)
 *      - eo,eu,ia  Currency   don't have a currency but OO.o wants it so just write back the Oo.o currency)
 *      - ja_JP             use OO.o data for calendars as :
 *                              OO.o needs English days,months for gregorian and English months for gengou calendars
 *                              OO.o gengou calendar has a DUMMY era, no such thing in CLDR
 *      - ko_KR             use OO.o data for calendars as :
 *                               OO.o gregorian calendar needs English months
 *                               OO.o has hanja calendar, no such thing in CLDR so use OO.o data
 *     - ia, eo ,eu           for lang only locales, use minDays, startDay of OO.o 
 *                              as there is no country so CLDR can't rpovide this data
 *      sr_CS                has dual circulation currency, CSD & EUR, prefer CSD as it more widely used
 */

package org.unicode.cldr.ooo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;

import com.ibm.icu.dev.tool.UOption;

public class LDMLToOOConverter
{
    // the version is read from cldr locale dtd specifier and determines 
    // whether certain data is read from OO.o or cldr i.e. there was no stable delimiter data in cldr prior to 1.4
    private double m_cldr_ver = 0.0;  
    
    private supplementalData m_suppData = null;
   
    private boolean m_bRoundTrip = false;  //false means reading some data from CLDR DB and
    //and OO stuff from OO files, true means it's all coming from the OO file
     
    private boolean m_bTemplate = false;   //geenrate new OO.o template file with cldr data filled in
            
    private static final int
            HELP1 = 0,
            HELP2 = 1,
            FORCE_OVERWRITE = 2,
            LOCALE = 3,
            OOO_LDML_DIR = 4,
            CLDR_DIR = 5,
            SUPP_DIR = 6,
            OUT_DIR = 7,
            SKIP_IF_NO_CLDR = 8,   //skip this locale if it doesn't exsit in CLDR, makes it easier to look at comparison charts
            TEMPLATE = 9;   //generate an OO.o template file with CLDR data where available and empty OO.o specific elements
    
    private static final UOption[] m_options = {
        UOption.HELP_H(),
                UOption.HELP_QUESTION_MARK(),
                UOption.create("force_overwrite", 'f', UOption.NO_ARG),
                UOption.create("locale", 'l', UOption.REQUIRES_ARG).setDefault(null),
                UOption.create("ooo_ldml_dir", 'o', UOption.REQUIRES_ARG).setDefault(null),
                UOption.create("cldr_dir", 'c', UOption.REQUIRES_ARG).setDefault(null),
                UOption.create("supp_dir", 's', UOption.REQUIRES_ARG).setDefault(null),
                UOption.create("out_dir", 't', UOption.REQUIRES_ARG).setDefault(null),
                UOption.create("skip_if_no_cldr", 'k', UOption.NO_ARG),
                UOption.create("template", 'e', UOption.NO_ARG),
    };
    
    public static void main(String[] args)
    {
        LDMLToOOConverter converter = new LDMLToOOConverter();
        converter.processArgs(args);
    }
    
    LDMLToOOConverter()
    {
    }
    
    private void processArgs(String[] args)
    {
        UOption.parseArgs(args, m_options);
        //check input
        if ((m_options[OOO_LDML_DIR].value == null ) && (m_options[CLDR_DIR].value == null )
        || (m_options[SUPP_DIR].value == null ) )
            printUsage();
        
        try
        {
            // tool can generate OOO XML either from CLDR, OOO in LDML format or a combination
            //check for single file convertion
            String locale = m_options[LOCALE].value;
            m_suppData = new supplementalData(m_options[SUPP_DIR].value);
            if (locale != null)
            {
                processSingle(locale, "./"+locale+".xml");
            }
            else
            {
                processBulk();
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }      
    }
    
    // Find each XML file in the input directory, convert to LDML in output directory
    private void processBulk()
    {
        String ooo_ldml_dir = m_options[OOO_LDML_DIR].value;
        String cldr_dir = m_options[CLDR_DIR].value;
        String out_dir = m_options[OUT_DIR].value;
        if ( ((ooo_ldml_dir==null) && (cldr_dir==null)) || (out_dir==null))
        {
            printError("Either input or output diorectories have not been specified");
            return;
        }
        
        //create the output dir if it doesn't exist
        File out = new File(out_dir);
        if (!out.isDirectory())
        {
            out.mkdir ();
        }
        
        XMLFileFilter filter = new XMLFileFilter();
        
        // start loop : OOO LDML files (if they exist) take precedence over CLDR ones
        File inDir = null;
        String files[] = null;
        String locale = null;
        String out_file = null;
        if (ooo_ldml_dir != null)
        {
            inDir = new File(ooo_ldml_dir);
            if (! inDir.exists())
            {
                printError("the specified OpenOffice.org input dir does not exist");
                return;
            }
        }
        else if (cldr_dir != null)
        {
            inDir = new File(cldr_dir);
            if (! inDir.exists())
            {
                printError("the specified CLDR input dir does not exist");
                return;
            }
        }
        
        int converted = 0;
        int skippedExisting = 0;
        int filesOverwritten = 0;
        
        files = inDir.list(filter);
        for (int i = 0; i<files.length; i++)
        {
            locale = Utilities.extractLocaleFromFilename(files[i]);
            out_file = out_dir + "/" + locale + ".xml";
            
            //if the file exists and we haven't chosen to force overwrite then notify user and skip generation
            File f = new File(out_file);
            if (f.exists())
            {
                if (m_options[FORCE_OVERWRITE].doesOccur)
                {
                    processSingle(locale, out_file);
                    filesOverwritten++;
                }
                else
                {
                    printWarning("The file " + out_file + " exists and will not be overwritten.");
                    skippedExisting++;
                }
            }
            else
            {
                processSingle(locale, out_file);
                converted++;
            }
        }
        
        System.err.println("Conversion complete.  Total of " + converted + " file(s) converted.");
        if (skippedExisting > 0)
        {
            System.err.println(skippedExisting + " file(s) were not converted because the destination files already existed.");
            System.err.println("Use the -help option to learn about forcing overwrite.");
        }
        if (filesOverwritten > 0)
        {
            System.err.println(filesOverwritten + " file(s) were overwritten in the destination folder.");
        }
    }
    
    // First, read the XML file into memory, then write onto destination.
    // Accesses vars: m_infilename, m_outfilename.
    // Return true if file has been properly processed, false if there was
    // a problem and the file was not written.
    private boolean processSingle(String locale, String out_file)
    {
        System.err.println ("Processing : " + locale);
        String cldr_file = null;
        String ooo_ldml_file = null;
        String cldrLocale = null;
        if (m_options[CLDR_DIR].value != null)
        {
            cldrLocale = handleNonCLDRLocales(locale);
            cldr_file = m_options[CLDR_DIR].value + "/" + cldrLocale + ".xml";
        }
        if (m_options[OOO_LDML_DIR].value != null)
            ooo_ldml_file = m_options[OOO_LDML_DIR].value + "/" + locale + ".xml";
        
                
        if ((cldr_file==null) && (ooo_ldml_file==null))
        {
            printWarning("Skipping this locale " + locale + " as no input files found");
            return false;
        }
        
        File cldr_f = null;
        if (cldr_file != null) cldr_f = new File (cldr_file); 
        File ooo_ldml_f = null;
        if (ooo_ldml_file != null) ooo_ldml_f = new File (ooo_ldml_file); 

        if( ((cldr_f==null) || (! cldr_f.exists())) && ((ooo_ldml_f==null) || (! ooo_ldml_f.exists())))
        {
            printWarning("Skipping this locale " + locale + " as no source OpenOffice.org LDML or CLDR file were not found");
            return false;
        }
        
        LDMLReaderForOO reader_ooo_ldml = null;
        LDMLReaderForOO reader_cldr = null;
        LDMLReaderForOO reader_cldr_temp = null;  //used for OO.o specific workarounds
        
        if ((cldr_file != null) && (cldr_f.exists()))
        {
           
            reader_cldr = new LDMLReaderForOO(cldr_file); 
            reader_cldr.readDocument(cldrLocale, true);
            m_cldr_ver = reader_cldr.getCLDRVersion ();

            if (reader_cldr.readInXML(true) == false)
            {
                printWarning("Failed to read CLDR file: " + cldr_file);
            }
        }
        else
        {
            if (m_options[SKIP_IF_NO_CLDR].doesOccur)
            {
                printWarning ("No such CLDR locale, no output written");
                return false;
            }
            printWarning ("Not using CLDR data, writing back the OO.o data");
            m_bRoundTrip = true;   //so read everything from the OO LDML
        }
        
        if ((ooo_ldml_file != null) && (ooo_ldml_f.exists()))
        {
            //reading from OO data in LDML format
            reader_ooo_ldml = new LDMLReaderForOO(ooo_ldml_file);
            reader_ooo_ldml.readDocument(locale, false);
            if (reader_ooo_ldml.readInXML(false) == false)
            {
                printWarning("Failed to read OpenOffice.org LDML file: " + ooo_ldml_file);
                return false;
            }          
        }
        else
        {   //read it form CLDR only i.e.    do we need this ??
            reader_ooo_ldml = reader_cldr;
        }

        if ((reader_ooo_ldml==null) && (reader_cldr==null))
        {
            printWarning("Skipping this locale " + locale + " as failed to read OpenOffice.org LDML and CLDR files");
            return false;
        }
          
        if (m_bRoundTrip == true)
            reader_cldr = reader_ooo_ldml;
        
        // Begin write process by creating a printstream which will output
        // text to the specified file location.
        PrintStream ps = setLocaleWriter(locale, null, out_file);
        
        m_bTemplate = m_options[TEMPLATE].doesOccur ? true:false;
        OOLocaleWriter writer = new OOLocaleWriter(ps, m_bTemplate);
        
        // set up the mapping between LDML and OpenOffice XML formats
        Hashtable aliases = new Hashtable();
        // Place the hashtable in the writer object.
        writer.setAliases(aliases);

        // Start m_infilenamewriting the OpenOffice XML
        writer.open(reader_ooo_ldml.m_LocaleInfo);   //write the OO version back to OO not the CLDR version

        // Data hashtable that only contains some of the data at any one time;
        // it's reset for each section of the XML file being created.
        Hashtable data = new Hashtable();

        //###### write LC_INFO ######
        //data.put(OOContants.Version, reader.m_Version);
        //use the OO.o data , CLDR should be same for lang + territory anyway, but defaultNames are all english in OO.o
        if (reader_ooo_ldml.m_LangDefaultName != null) data.put(OOConstants.LANGUAGE, reader_ooo_ldml.m_LangDefaultName);
        if (reader_ooo_ldml.m_LangID != null) data.put(OOConstants.LANG_ID, reader_ooo_ldml.m_LangID);
        if (reader_ooo_ldml.m_TerritoryDefaultName != null) data.put(OOConstants.COUNTRY, reader_ooo_ldml.m_TerritoryDefaultName);
        if (reader_ooo_ldml.m_TerritoryID != null) data.put(OOConstants.COUNTRY_ID, reader_ooo_ldml.m_TerritoryID);
        if ((reader_ooo_ldml.m_PlatformID != null) && (reader_ooo_ldml.m_PlatformID.length()>0))
            data.put(OOConstants.PLATFORM_ID, reader_ooo_ldml.m_PlatformID);
        if ((reader_ooo_ldml.m_Variant != null) && (reader_ooo_ldml.m_Variant.length()>0))
            data.put(OOConstants.VARIANT, reader_ooo_ldml.m_Variant);
        writer.writeLC_INFO(data);

        //###### write LC_CTYPE ######
        data.clear();
        
        // Separators
        Hashtable separators = new Hashtable();
        if (reader_ooo_ldml.m_DateSeparator != null)
            separators.put(OOConstants.DATE_SEPARATOR, reader_ooo_ldml.m_DateSeparator);
        if (reader_cldr.m_ThousandSeparator != null)
            separators.put(OOConstants.THOUSAND_SEPARATOR, reader_cldr.m_ThousandSeparator);
        if (reader_cldr.m_DecimalSeparator != null)
            separators.put(OOConstants.DECIMAL_SEPARATOR, reader_cldr.m_DecimalSeparator);
        if (reader_ooo_ldml.m_TimeSeparator != null)
            separators.put(OOConstants.TIME_SEPARATOR, reader_ooo_ldml.m_TimeSeparator);
        if (reader_ooo_ldml.m_Time100SecSeparator != null)
            separators.put(OOConstants.TIME_100SEC_SEPARATOR, reader_ooo_ldml.m_Time100SecSeparator);
        if (reader_cldr.m_ListSeparator != null)
            separators.put(OOConstants.LIST_SEPARATOR, reader_cldr.m_ListSeparator);
        if (reader_ooo_ldml.m_LongDateDayOfWeekSeparator != null)
            separators.put(OOConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR, reader_ooo_ldml.m_LongDateDayOfWeekSeparator);
        if (reader_ooo_ldml.m_LongDateDaySeparator != null)
            separators.put(OOConstants.LONG_DATE_DAY_SEPARATOR, reader_ooo_ldml.m_LongDateDaySeparator);
        if (reader_ooo_ldml.m_LongDateMonthSeparator != null)
            separators.put(OOConstants.LONG_DATE_MONTH_SEPARATOR, reader_ooo_ldml.m_LongDateMonthSeparator);
        if (reader_ooo_ldml.m_LongDateYearSeparator != null)
            separators.put(OOConstants.LONG_DATE_YEAR_SEPARATOR, reader_ooo_ldml.m_LongDateYearSeparator);
        if ((separators != null) && (separators.size()>0))
            data.put(OOConstants.SEPARATORS, separators);
        
        // Markers
        Hashtable markers = new Hashtable();
        //markers are now stable in cldr 1.4+
        LDMLReaderForOO markerReader = null;
  //await cldr 1.5 for better data      
  //      if (m_cldr_ver > 1.399) 
  //          markerReader = reader_cldr;
 //       else
        if (m_bTemplate == true)
            markerReader = reader_cldr;
        else
            markerReader = reader_ooo_ldml;
        
        if (markerReader.m_QuotationStart != null)
            markers.put(OOConstants.QUOTATION_START, markerReader.m_QuotationStart);
        if (markerReader.m_QuotationEnd != null)
            markers.put(OOConstants.QUOTATION_END, markerReader.m_QuotationEnd);
        if (markerReader.m_DoubleQuotationStart != null)
            markers.put(OOConstants.DOUBLE_QUOTATION_START, markerReader.m_DoubleQuotationStart);
        if (markerReader.m_DoubleQuotationEnd != null)
            markers.put(OOConstants.DOUBLE_QUOTATION_END, markerReader.m_DoubleQuotationEnd);
        if (markers.size()>0)
            data.put(OOConstants.MARKERS, markers);
        
        // TimeAM, TimePM
        if ((reader_cldr.m_TimeAM != null) && (reader_cldr.m_TimeAM.length()>0))
            data.put(OOConstants.TIME_AM, reader_cldr.m_TimeAM);
        if ((reader_cldr.m_TimePM != null) && (reader_cldr.m_TimePM.length()>0))
            data.put(OOConstants.TIME_PM, reader_cldr.m_TimePM);
        
        // MeasurementSystem
        String ms = null;
        if (m_cldr_ver > 1.399 && m_bRoundTrip == false)
            ms = m_suppData.getMessSys(reader_ooo_ldml.m_TerritoryID);
        else if (reader_cldr.m_MeasurementSystem != null)   //reader_cldr = reader_ooo_ldml if it's round trip'
            ms = LDMLToOOMapper.MapMeasurementSystem(reader_cldr.m_MeasurementSystem);
        if ((ms != null) && (ms.length()>0))
            data.put(OOConstants.MEASUREMENT_SYSTEM, ms);

        if ((data != null) && (data.size()>0))
            writer.writeLC_CTYPE(data);
        
        //###### write LC_FORMAT ######
        /*
         *Assumption taken with reading LDML format elements (e.g. decimalFormat):
         *The format element's type attribute will contain the exact same string
         *as its subelement "sp/org/unicode/cldr/util/ecial"'s attribute openOffice:msgtype.
         */
        data.clear();
        
        if (reader_ooo_ldml.m_FormatRefLocale != null)
        {
            writer.writeLC_FORMATRef(reader_ooo_ldml.m_FormatReplaceFrom, reader_ooo_ldml.m_FormatReplaceTo, reader_ooo_ldml.m_FormatRefLocale);
        }
        else if (m_bTemplate == true)
        {
            writer.writeLC_FORMAT_template ();
        }
        else
        {
            boolean lcformatOpened = false;
            
            Hashtable m_outFormatElements = new Hashtable();
            Hashtable m_outFormatCodes = new Hashtable();
            Hashtable m_outFormatDefaultNames = new Hashtable();
            
            //Eike want's then written in this order :
            // FIXED_NUMBER, SCIENTIFIC_NUMBER, PERCENT_NUMBER, FRACTION_NUMBER, CURRENCY, DATE, TIME, DATE_TIME
            // fixed number
            LDMLToOOMapper.MapFormatElements(reader_ooo_ldml.m_FixedNFormats, reader_ooo_ldml.m_FixedNPatterns, reader_ooo_ldml.m_FixedNOO, reader_ooo_ldml.m_FixedDefaultName, m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames);
            lcformatOpened = writer.writeLC_FORMAT(m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames, lcformatOpened, reader_ooo_ldml.m_FormatReplaceFrom, reader_ooo_ldml.m_FormatReplaceTo);
            
            // scientific number
            m_outFormatElements.clear(); m_outFormatCodes.clear(); m_outFormatDefaultNames.clear();
            LDMLToOOMapper.MapFormatElements(reader_ooo_ldml.m_SciNFormats, reader_ooo_ldml.m_SciNPatterns, reader_ooo_ldml.m_SciNOO, reader_ooo_ldml.m_SciDefaultName, m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames);
            lcformatOpened = writer.writeLC_FORMAT(m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames, lcformatOpened, reader_ooo_ldml.m_FormatReplaceFrom, reader_ooo_ldml.m_FormatReplaceTo);
            
            // percent number
            m_outFormatElements.clear(); m_outFormatCodes.clear(); m_outFormatDefaultNames.clear();
            LDMLToOOMapper.MapFormatElements(reader_ooo_ldml.m_PercNFormats, reader_ooo_ldml.m_PercNPatterns, reader_ooo_ldml.m_PercNOO, reader_ooo_ldml.m_PercDefaultName, m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames);
            lcformatOpened = writer.writeLC_FORMAT(m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames, lcformatOpened, reader_ooo_ldml.m_FormatReplaceFrom, reader_ooo_ldml.m_FormatReplaceTo);
            
            //fraction number  FRACTION_NUMBER
            // TODO need to impleemnt
            //   m_outFormatElements.clear(); m_outFormatCodes.clear(); m_outFormatDefaultNames.clear();
            //   LDMLToOOMapper.MapFormatElements(reader_ooo_ldml.m_FractNFormats, reader_ooo_ldml.m_FractNPatterns, reader_ooo_ldml.m_FractNOO, reader_ooo_ldml.m_FractNDefaultName, m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames);
            //   lcformatOpened = writer.writeLC_FORMAT(m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames, lcformatOpened, reader_ooo_ldml.m_FormatReplaceFrom, reader_ooo_ldml.m_FormatReplaceTo);
            
            // currency number
            m_outFormatElements.clear(); m_outFormatCodes.clear(); m_outFormatDefaultNames.clear();
            LDMLToOOMapper.MapFormatElements(reader_ooo_ldml.m_CurrNFormats, reader_ooo_ldml.m_CurrNPatterns, reader_ooo_ldml.m_CurrNOO, reader_ooo_ldml.m_CurrNDefaultName, m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames);
            lcformatOpened = writer.writeLC_FORMAT(m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames, lcformatOpened, reader_ooo_ldml.m_FormatReplaceFrom, reader_ooo_ldml.m_FormatReplaceTo);
            
            // date
            m_outFormatElements.clear(); m_outFormatCodes.clear(); m_outFormatDefaultNames.clear();
            LDMLToOOMapper.MapFormatElements(reader_ooo_ldml.m_DateFormats, reader_ooo_ldml.m_DatePatterns, reader_ooo_ldml.m_DateOO, reader_ooo_ldml.m_DateDefaultName, m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames);
            lcformatOpened = writer.writeLC_FORMAT(m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames, lcformatOpened, reader_ooo_ldml.m_FormatReplaceFrom, reader_ooo_ldml.m_FormatReplaceTo);
            
            // time
            m_outFormatElements.clear(); m_outFormatCodes.clear(); m_outFormatDefaultNames.clear();
            LDMLToOOMapper.MapFormatElements(reader_ooo_ldml.m_TimeFormats, reader_ooo_ldml.m_TimePatterns, reader_ooo_ldml.m_TimeOO, reader_ooo_ldml.m_TimeDefaultName, m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames);
            lcformatOpened = writer.writeLC_FORMAT(m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames, lcformatOpened, reader_ooo_ldml.m_FormatReplaceFrom, reader_ooo_ldml.m_FormatReplaceTo);
            
            // date time
            m_outFormatElements.clear(); m_outFormatCodes.clear(); m_outFormatDefaultNames.clear();
            LDMLToOOMapper.MapFormatElements(reader_ooo_ldml.m_DateTimeFormats, reader_ooo_ldml.m_DateTimePatterns, reader_ooo_ldml.m_DateTimeOO, reader_ooo_ldml.m_DateTimeDefaultName, m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames);
            lcformatOpened = writer.writeLC_FORMAT(m_outFormatElements, m_outFormatCodes, m_outFormatDefaultNames, lcformatOpened, reader_ooo_ldml.m_FormatReplaceFrom, reader_ooo_ldml.m_FormatReplaceTo);
            
            // close the LC_FORMAT tag
            writer.writeCloseLC_FORMAT(lcformatOpened);
        }
        
        //###### write LC_COLLATION ######
        if (m_bTemplate == true)
            writer.writeLC_COLLATION_template ();
        else
        {
            data.clear();
            if ((reader_ooo_ldml.m_Collators != null) && (reader_ooo_ldml.m_Collators.size()>0))
            {
                LDMLToOOMapper.MapCollators(reader_ooo_ldml.m_Collators);
                data.put(OOConstants.COLLATOR, reader_ooo_ldml.m_Collators);
            }
            if ((reader_ooo_ldml.m_CollationOptions != null) && (reader_ooo_ldml.m_CollationOptions.size()>0))
                data.put(OOConstants.COLLATION_OPTIONS, reader_ooo_ldml.m_CollationOptions);
            
            writer.writeLC_COLLATION(data, reader_ooo_ldml.m_CollationRefLocale);
        }
        
        //###### write LC_SEARCH ######
        if (m_bTemplate == true)
            writer.writeLC_SEARCH_template ();
        else
            writer.writeLC_SEARCH(reader_ooo_ldml.m_SearchOptions, reader_ooo_ldml.m_SearchRefLocale);
        
        //###### write LC_INDEX ######
        if (m_bTemplate == true)
            writer.writeLC_INDEX_template ();
        else
        {
            data.clear();
            if ((reader_ooo_ldml.m_IndexKeys != null) && (reader_ooo_ldml.m_IndexKeys.size() > 0))
            {
                LDMLToOOMapper.MapIndexKeys(reader_ooo_ldml.m_IndexKeys);
                data.put(OOConstants.INDEX_KEY, reader_ooo_ldml.m_IndexKeys);
            }
            if ((reader_ooo_ldml.m_IndexUnicodeScript != null) && (reader_ooo_ldml.m_IndexUnicodeScript.size() > 0))
                data.put(OOConstants.UNICODE_SCRIPT, reader_ooo_ldml.m_IndexUnicodeScript);
            if ((reader_ooo_ldml.m_IndexFollowPageWord != null) && (reader_ooo_ldml.m_IndexFollowPageWord.size() > 0))
                data.put(OOConstants.FOLLOW_PAGE_WORD, reader_ooo_ldml.m_IndexFollowPageWord);
            
            writer.writeLC_INDEX(data, reader_ooo_ldml.m_IndexRefLocale);
        }
        
        //###### write LC_CALENDAR ######
        data.clear();
        
        //workaround : lots of calendar issues, see comments at top of this file
        if ((locale.compareTo("ja_JP")==0) || (locale.compareTo("ko_KR")==0))
        {
            reader_cldr_temp = reader_cldr;
            reader_cldr = reader_ooo_ldml;
        }
        
        Vector outCalendarTypes = new Vector();
        LDMLToOOMapper.MapCalendar(reader_cldr.m_CalendarTypes, outCalendarTypes, locale);
        if (outCalendarTypes.size()>0)
            data.put(OOConstants.UNOID, outCalendarTypes);
        
        //use the OO.o for default calendar , it's always gregorian anyway
        //String defaultCalendar = LDMLToOOMapper.MapDefaultCalendar(reader_ooo_ldml.m_CalendarDefaultAttribs);
        //if ((defaultCalendar != null) && (defaultCalendar.length()>0))
       //     data.put(OOConstants.DEFAULT, defaultCalendar);
       data.put(OOConstants.DEFAULT, OOConstants.GREGORIAN);   //default calendar is semi-deprecated from CLDR
        
        if ((reader_cldr.m_AbbrDays != null) && (reader_cldr.m_AbbrDays.size()>0))
        {
            Hashtable abbrDays = LDMLToOOMapper.MapDays(reader_cldr.m_AbbrDays, locale);
            data.put(OOConstants.DAYS_OF_WEEK + "." + OOConstants.DEFAULT_ABBRV_NAME, abbrDays);
        }
        if ((reader_cldr.m_FullDays != null) && (reader_cldr.m_FullDays.size()>0))
        {
            Hashtable fullDays = LDMLToOOMapper.MapDays(reader_cldr.m_FullDays, locale);
            data.put(OOConstants.DAYS_OF_WEEK + "." + OOConstants.DEFAULT_FULL_NAME, fullDays);
        }
        if ((reader_cldr.m_AbbrMonths != null) && (reader_cldr.m_AbbrMonths.size()>0))
        {
            Hashtable abbrMonths = LDMLToOOMapper.MapMonths(reader_cldr.m_AbbrMonths, locale);
            data.put(OOConstants.MONTHS_OF_YEAR + "." + OOConstants.DEFAULT_ABBRV_NAME, abbrMonths);
        }
        if ((reader_cldr.m_FullMonths != null) && (reader_cldr.m_FullMonths.size()>0))
        {
            Hashtable fullMonths = LDMLToOOMapper.MapMonths(reader_cldr.m_FullMonths, locale);
            data.put(OOConstants.MONTHS_OF_YEAR + "." + OOConstants.DEFAULT_FULL_NAME, fullMonths);
        }
        
        // Eras   wide era names are in CLDR 1.4+
        LDMLReaderForOO wideEraReader = null;
     // still too many gaps in cldr 1.4 to take eras from it.   
     //   if (m_cldr_ver > 1.399) 
     //       wideEraReader = reader_cldr;
     //   else
        
        if (m_bTemplate == true)
            wideEraReader = reader_cldr;
        else
            wideEraReader = reader_ooo_ldml;
        if  (((wideEraReader.m_EraNames != null) && (reader_ooo_ldml.m_EraNames.size()>0)) || ((reader_cldr.m_AbbrEras != null) && (reader_cldr.m_AbbrEras.size()>0)) )
        {
            Hashtable eras = null;
            if (locale.compareTo("zh_TW")==0)   //workaround : CLDR deosn't have ROC calendar yet
                eras = LDMLToOOMapper.MapEras(reader_ooo_ldml.m_EraNames, reader_ooo_ldml.m_AbbrEras, locale);
            else
                eras = LDMLToOOMapper.MapEras(wideEraReader.m_EraNames, reader_cldr.m_AbbrEras, locale);
            
            if ((eras != null) && (eras.size()>0)) data.put(OOConstants.ERAS, eras);
        }
        
        Hashtable startOfWeeks = null;
        if (locale.substring(0,2).equals("eo") || locale.substring(0,2).equals("ia") || locale.substring(0,2).equals("eu") )
        {   //there is no startOfWeek or moinDays for lang only locales, so use OO.org
            startOfWeeks = LDMLToOOMapper.MapStartOfWeeks(reader_ooo_ldml.m_StartOfWeeks, locale); 
        }  
        else if (m_cldr_ver > 1.399 && m_bRoundTrip == false)
        {
            String firstDay = m_suppData.getFirstDay(reader_ooo_ldml.m_TerritoryID);
            startOfWeeks = new Hashtable();
            startOfWeeks.put(OOConstants.GREGORIAN, firstDay);
        }
        else if ((reader_cldr.m_StartOfWeeks != null) && (reader_cldr.m_StartOfWeeks.size() >0))
            startOfWeeks = LDMLToOOMapper.MapStartOfWeeks(reader_cldr.m_StartOfWeeks, locale); 
        if (startOfWeeks != null) 
            data.put(OOConstants.START_DAY_OF_WEEK, startOfWeeks);
        
        Hashtable minDays = null;
        if (locale.substring(0,2).equals("eo") || locale.substring(0,2).equals("ia") || locale.substring(0,2).equals("eu"))
        {   //there is no startOfWeek or moinDays for lang only locales, so use OO.org
            minDays = LDMLToOOMapper.MapMinDays(reader_ooo_ldml.m_MinDays, locale);
        }
        else if (m_cldr_ver > 1.399 && m_bRoundTrip == false)
        {
            String min = m_suppData.getMinDays(reader_ooo_ldml.m_TerritoryID);
            minDays = new Hashtable ();
            minDays.put (OOConstants.GREGORIAN, min);
        }
        else if ((reader_cldr.m_MinDays != null) && (reader_cldr.m_MinDays.size() >0))
            minDays = LDMLToOOMapper.MapMinDays(reader_cldr.m_MinDays, locale);
        if (minDays != null) 
            data.put(OOConstants.MINIMAL_DAYS_IN_FIRST_WEEK, minDays);
        
        if (data.size()>0)
            writer.writeLC_CALENDAR(data);
        
        //undo the above workaround
        if ((locale.compareTo("ja_JP")==0) || (locale.compareTo("ko_KR")==0))
            reader_cldr = reader_cldr_temp;
        
        
        //###### write LC_CURRENCY ######
        data.clear();
        
        // START_WORKAROUND: language only locales (ia,eo) don't have currency in CLDR  but do in OO.o
        if (locale.substring(0,2).equals("eo") || locale.substring(0,2).equals("ia") || locale.substring(0,2).equals("eu"))
        {
            reader_cldr_temp = reader_cldr;
            reader_cldr = reader_ooo_ldml;
            m_bRoundTrip = true;  //it's a round trip on the currency data
        }
        
        writer.WriteLC_CURRENCY(reader_cldr.m_CurrencyData_cldr, reader_ooo_ldml.m_CurrencyData_ooo, m_suppData, m_bRoundTrip, reader_ooo_ldml.m_TerritoryID);
        diagnostics(reader_ooo_ldml,  reader_cldr,  locale);
        //END_WORKAROUND
        if (locale.substring(0,2).equals("eo") || locale.substring(0,2).equals("ia") || locale.substring(0,2).equals("eu") )
        {
            reader_cldr = reader_cldr_temp;
            m_bRoundTrip = false;
        }
        
        
        //###### write LC_TRANSLITERATIONS ######
        if (m_bTemplate == true)
            writer.writeLC_TRANSLITERATIONS_template ();
        else
        {
            Vector transliterations = null;
            if ((reader_ooo_ldml.m_TransliterationAtts != null) && (reader_ooo_ldml.m_TransliterationAtts.size()>0))
            {
                transliterations = LDMLToOOMapper.MapTransliterations(reader_ooo_ldml.m_TransliterationAtts);
            }
            writer.WriteLC_TRANSLITERATIONS(transliterations, reader_ooo_ldml.m_TransliterationRefLocale);
        }
        
        //###### write LC_MISC ######
        data.clear();
        String ref = null;
        
        if ((reader_ooo_ldml.m_ReservedRefLocale != null) && (reader_ooo_ldml.m_ReservedRefLocale.length() > 0))
            ref = reader_ooo_ldml.m_ReservedRefLocale;
        else if ((reader_ooo_ldml.m_ForbiddenRefLocale != null) && (reader_ooo_ldml.m_ForbiddenRefLocale.length() > 0))
            ref = reader_ooo_ldml.m_ForbiddenRefLocale;
        if ((reader_ooo_ldml.m_ReservedWords != null && reader_ooo_ldml.m_ReservedWords.size()>0) || m_bTemplate==true)
        {
            Hashtable reservedWords = LDMLToOOMapper.MapReservedWords(reader_ooo_ldml.m_ReservedWords);
            
            //quarters are kept separately noe that they are in 1.4
            //theyt are stored in LDMLReaderForOO.m_abbrQuarters and m_abbrQuarters regardless of where they are read from
            //still sue OO.o data for quarters as there are some gaps in CLDR
            //     if (m_bRoundTrip == false)
            //         LDMLToOOMapper.MapQuarters(reservedWords, reader_cldr.m_AbbrQuarters, reader_cldr.m_WideQuarters);
            //     else
            if (m_bTemplate == true)
            {
                reservedWords = new Hashtable();
                LDMLToOOMapper.MapQuarters(reservedWords, reader_cldr.m_AbbrQuarters, reader_cldr.m_WideQuarters);
            }
            else
                LDMLToOOMapper.MapQuarters(reservedWords, reader_ooo_ldml.m_AbbrQuarters, reader_ooo_ldml.m_WideQuarters);
            
            if ((reservedWords != null) && (reservedWords.size()>0))
                data.put(OOConstants.RESERVED_WORDS, reservedWords);
            
            String forbiddenLineBeg = reader_ooo_ldml.m_ForbiddenLineBeg;
            if ((forbiddenLineBeg != null) && (forbiddenLineBeg.length()>0))
                data.put(OOConstants.FORBIDDEN_LINE_BEGIN_CHARACTERS, forbiddenLineBeg);
            String forbiddenLineEnd = reader_ooo_ldml.m_ForbiddenLineEnd;
            if ((forbiddenLineEnd != null) && (forbiddenLineEnd.length()>0))
                data.put(OOConstants.FORBIDDEN_LINE_END_CHARACTERS, forbiddenLineEnd);
        }
        writer.WriteLC_MISC(data, ref);
        
        //###### write LC_NumberingLevel ######
        if (m_bTemplate == true)
            writer.writeLC_NumberingLevel_template ();
        else
        {
            Vector numberingLevelsAttsOO = null;
            if ((reader_ooo_ldml.m_NumberingLevelAtts != null) && (reader_ooo_ldml.m_NumberingLevelAtts.size()>0))
            {
                numberingLevelsAttsOO = LDMLToOOMapper.MapNumberingLevels(reader_ooo_ldml.m_NumberingLevelAtts);
            }
            writer.WriteLC_NumberingLevel(numberingLevelsAttsOO, reader_ooo_ldml.m_NumberingRefLocale);
        }
        
        //###### write LC_OutlineNumberingLevel ######
        if (m_bTemplate == true)
            writer.writeLC_OutlineNumberingLevel_template ();
        else
        {
            Vector outlineNumberingLevelsOO = null;
            if ((reader_ooo_ldml.m_OutlineNumberingLevels != null) && (reader_ooo_ldml.m_OutlineNumberingLevels.size()>0))
            {
                outlineNumberingLevelsOO = LDMLToOOMapper.MapOutlineNumberingLevels(reader_ooo_ldml.m_OutlineNumberingLevels);
            }
            writer.WriteLC_OutlineNumberingLevel(outlineNumberingLevelsOO, reader_ooo_ldml.m_OutlineNumberingRefLocale);
        }
        
        writer.close();
        System.err.println("Writing :  " + out_file);
        
        return true;
    }
   
    
    void printUsage()
    {
        System.err.println("");
        System.err.println("DESCRIPTION:");
        System.err.println("  LDMLToOOConverter converts LDML XML files to OpenOffice.org XML files.");
        System.err.println("  There are 3 ways to use the tool :");
        System.err.println("  1. Conversion back to OpenOffice.org XML of data which was previously converted to LDML.");
        System.err.println("     this is a simple round trip conversion which is useful for testing the tools");
        System.err.println("     Example : LDMLToOOConverter -s /supplemental_dir -o ooo_dir -t OOO/");
        System.err.println("  2. Conversion to OO XML of OpenOffice.org specific data from 1 above merged with CLDR standard data.");
        System.err.println("     this is the step to perform when it is desired to migrate OpenOffice.org data to CLDR");
        System.err.println("     Example : LDMLToOOConverter -s supplemental_dir -c cldr_dir -o ooo_dir -t OOO/");
        System.err.println("  3. Conversion of CLDR data only to OpenOffice.org format.");
        System.err.println("     this is the step to perform when generating date for a brand new OpenOffice.org locale");
        System.err.println("     Example : LDMLToOOConverter -s supplemental_dir -l ga_IE -c cldr_dir");
        System.err.println("");
        System.err.println("USAGE:");
        System.err.println("  For single file conversion:");
        System.err.println("    LDMLToOOConverter [-f] -l LOCALE_NAME -c CLDR_DIR");
        System.err.println("  For bulk file conversion:");
        System.err.println("    LDMLToOOConverter [-f] -c CLDR_DIR -o OOO_LDML_DIRECTORY -t DESTINATION_DIRECTORY");
        System.err.println("      if the -l option is not specified then a bulk conversion is performed");
        System.err.println("  DESTINATION_DIRECTORY will be created if it does not exist.");
        System.err.println("");
        System.err.println("OPTIONS:");
        System.err.println("  -f         force overwrite of destination file if it already exists");
        System.err.println("  -c         specifies dir containing CLDR data");
        System.err.println("  -l         specifies single locale to be converted to OpenOffice.org format");
        System.err.println("  -o         the folder containing OpenOffice.org data in LDML format");
        System.err.println("  -s         the folder containing CLDR's supplementalData.xml (this is needed)");
        System.err.println("  -t         the folder where output is written for bulk conversion (mandatory)");
        System.err.println("  -k         write no output if this locale does not occur in CLDR");
        System.err.println("  -e         generate OO.o template file for new locales (OO.o specific elements are empty)");
        System.err.println("  -help      (this document) usage of LDMLToOOConverter");
        System.err.println("");
        
    }
    
    void printError(String err)
    {
        System.err.println("Processing was unsuccessful : " + err);
        System.err.println("Use \"LDMLToOOConverter -h\" for usage");
    }
    
    void printWarning(String warning)
    {
        System.err.println("(Warning) " + warning);
    }
    
    
    //creates the printStream for output
    // Modified from Utilities class
    public static PrintStream setLocaleWriter(String localeStr, String enc, String in_filename)
    {
        //enc if specified will be used to compose output filename, so it should normally be null (except for Solaris maybe)
        if ((localeStr == null) || (in_filename == null))
            return null;
        
        //create the subdir if it doesn't already exist
        //File dir = new File("./" + filename);
        
        //if (dir.exists() == false)
        //    dir.mkdir();
        
        PrintStream ps = null;
        try
        {
            String fileName = in_filename; //getOutputFilename(localeStr, enc, subDir);
            if ((fileName != null) && (fileName.compareTo("")!=0))
            {
                FileOutputStream fos = new FileOutputStream(new File(fileName));
                ps = new PrintStream(fos, true, "UTF8");
                Logging.Log1("Writing to file : " + fileName);
            }
        }
        catch (FileNotFoundException e )
        {
            System.err.println("Unable to create output file");
        }
        catch (UnsupportedEncodingException e)
        {
            System.err.println("Unsupported encoding");
        }
        return ps;
    }
    
    protected class XMLFileFilter implements FilenameFilter
    {
        // Accept a file if it ends in .xml.
        public boolean accept(File dir, String filename)
        {
            boolean accepted = false;
            if ((filename != null) && (filename.length()>4))
                accepted = (filename.substring(filename.length()-4).toLowerCase().compareTo(".xml") == 0);
            return accepted;
        }
    }
    
    private String handleNonCLDRLocales(String localeStr)
    {
        //no_NO is an alias of nb_NO
        //for en_CB, ia, bs_BA do nothing , if they return not in CLDR then all contents of the OO LDML
        // will just be written back out
        
        String cldrLocale = localeStr;
        if (localeStr.compareTo("no_NO")==0)
        {
            cldrLocale = "nb_NO";
        }
        return cldrLocale;
        
    }
    
    private void diagnostics (LDMLReaderForOO reader_ooo_ldml, LDMLReaderForOO reader_cldr, String locale )
    {    //print OO.o vs CLDR currency conflicts to separate file, if no conflict print nothing
        if (locale.indexOf("_") < 0)
            return;   //lang only locale has no currency
        
        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter("currency.txt",true));
            out.write(locale + " :\n");
            
            //if CLDR and OO.o disagree on default curr code print the details
            String cldr_def_code = (String)m_suppData.getCurrencies(reader_ooo_ldml.m_TerritoryID).elementAt(0);
            String ooo_def_code = "";
            for (int i=0; i < reader_ooo_ldml.m_CurrencyData_ooo.size(); i++)
            {
                Vector v = (Vector) reader_ooo_ldml.m_CurrencyData_ooo.elementAt(i);
                if (((String)v.elementAt(5)).equals("true"))
                {
                    ooo_def_code = (String) v.elementAt(2);
                    break;
                }
            }
            if (!cldr_def_code.equals(ooo_def_code))
            {
                out.write("\tDefault=true  :  CLDR = " +  cldr_def_code  + "    OO.org = " + ooo_def_code + "\n");
                for (int i=0; i < reader_cldr.m_CurrencyData_cldr.size(); i++)
                {
                    Vector v = (Vector) reader_cldr.m_CurrencyData_cldr.elementAt(i);
                    if (((String)v.elementAt(2)).equals(cldr_def_code))
                    {
                        out.write("\t\t CLDR iso4217 code = " +  cldr_def_code  + "  Symbol = " + (String)v.elementAt(1)
                        + "  Display Name = " + (String) v.elementAt(3) + "\n");
                        break;
                    }
                }
            }
            out.close();
        }
        catch (IOException e)
        {
        }
    }
}


