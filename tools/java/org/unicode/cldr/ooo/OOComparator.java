/************************************************************************
 * Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
 *************************************************************************/

package org.unicode.cldr.ooo;

import java.io.*;
import java.util.*;
import java.lang.reflect.Field;


/**
 *
 * program compares the contents of 2 or more OO XML files or set of files
 * and prints the differences to a HTML file
 * i.e compares it_IT.xml in two different folders and writes results to it_IT.html in the current folder
 * or compares contents of 2 folders and prints all results to a single Bulk.html file in the current folder
 */

public class OOComparator
{
    private class LocaleFiles
    {
        public Locale m_locale;
        public Vector m_files = new Vector();  //the files belonging to the locale m_locale;
    };
    
    private class TableInfo   //holds optional details to go in talble headers
    {
        public int index ;  //index in m_XMLfiles or m_XMLdirs
        public String name;   //title to use in comparison table like "OO.org" or "CLDR""
        public String url;    //url to link to from talbe header like http://l10n.openoffice.org/nonav/source/browse/*checkout*/l10n/i18npool/source/localedata/data/zu_ZA.xml?rev=SRC680_m151
    
        TableInfo (int index, String name, String url )
        {
            this.index = index;
            this.name = name;
            this.url = url;
        }
    
    }
    
    private static final String STRING_SEPARATOR = ",";
    
    private static final int OPT_SINGLE = 0x001;
    private static final int OPT_BULK = 0x002;
    private static final int OPT_UNRESOLVED_REFS = 0x004;  //means refs are not resolved in conversion to LDML so don't compare "ref" attributes
    private static final int OPT_SOLUTIONS = 0x008;    //add a solutions column to talbe
    private static final int OPT_INVALID = 0x4000;
    private static final int OPT_UNKNOWN = 0x8000;
    
    private static final String OPT_SINGLE_STR = "-single";
    private static final String OPT_BULK_STR = "-bulk";
    private static final String OPT_SHOW_REFS_STR = "-show_refs";
    private static final String OPT_SOLUTIONS_STR = "-solutions";
    
    private static final String MISSING_ATT = "(missing_att)";
    private static final String MISSING_ELEM = "(missing_element)";
    
    private int m_iOptions = 0;
    
    private Calendar cal = Calendar.getInstance();
    
    private Vector m_XMLfiles = new Vector();  //holds names incl. paths of files to compare
    private Vector m_XMLdirs = new Vector();   //holds names incl. paths of dirs whose contents are to be compared
    private Vector m_TableInfo = new Vector ();   //holds optional info to be written at top of tables
    private int m_ArgsCounter = 0;  //counts the number of input files or folder specified
    private int m_CurrNumFiles = 0;
    
    private int m_DiffCounter = 0;   // count the conflicts
    private int m_CommonCounter = 0;  //count elements wihch agree
    private int m_TotalNumLocales = 0;  //count all locales for which there is at least one version in any input folder
    
    private int cntDiff = 0;  //counter for conflicts
    
    private Hashtable m_DiffsPerLocale = new Hashtable ();   //key = # conflicts, value = # of locales with this number of conflicts
    
    /** Creates a new instance of OOComparator */
    public OOComparator()
    {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        OOComparator comparator = new OOComparator();
        comparator.processArgs(args);
        System.err.println("INFO: execution complete.");
    }
    
    private void printUsage()
    {
        System.err.println("");
        System.err.println("DESCRIPTION:");
        System.err.println("  Compares 2 or more locale files or directories of locale files and writes the differences ");
        System.out.println("  to a HTML file");
        System.err.println("");
        System.err.println("USAGE:");
        System.err.println("  OOComparator [-single / -bulk] file or directory list");
        System.err.println("     compares the contents of 2 or more OO XML: files and writes the ");
        System.err.println("     differences to a HTML file");
        System.err.println("  Each argument can take the folllowing format : \"file or directory,name,url\" ");
        System.err.println("      where the 2nd and 3rd parts are optional  ");
        System.err.println("");
        System.err.println("OPTIONS:");
        System.err.println("  "+OPT_SINGLE_STR + "    : specifies 2 or more locales in OpenOffice.org format to be compared");
        System.err.println("  "+OPT_BULK_STR + "      : specifies 2 or more dirs containing locales in OpenOffice.org format to be compared");
        System.err.println("  "+OPT_SHOW_REFS_STR + " :");
        System.err.println("  "+OPT_SOLUTIONS_STR + " : adds a solutions column to table");
        System.err.println("");
        System.err.println("EXAMPLES:");
        System.err.println("  OOComparator -solutions -single dir1/it_IT.xml dir2/it_IT.xml dir3/it_IT.xml ....");
        System.err.println("    write the differences to it_IT.html");
        System.err.println("  OOComparator -bulk  dir1 dir2 dir3....");
        System.err.println("    write the differences to Bulk.html");
        System.err.println("  OOComparator -single dir1/it_IT.xml;OOo;http://l10n.openoffice.org/location/it_IT.xml?rev=SRC680_m151 dir2/it_IT.xml dir3/it_IT.xml ....");
        System.err.println("    write the differences to it_IT.html adding the header \"OOo\" and link http://l10n.openoffice.org/location/he_IL.xml?rev=SRC680_m151 to the table");
        System.err.println("  OOComparator -bulk dir1;OOo;http://l10n.openoffice.org/location/LOCALE?rev=SRC680_m151 dir2;CLDR ....");
        System.err.println("    the string \"LOCALE\" will be replaced by the tool with the appropriate locale name");
    }
    
    private void processArgs(String[] args)
    {
        m_iOptions = identifyOptions(args);
        
        if ((args.length < 3) || ((m_iOptions & OPT_UNKNOWN) != 0))
        {
            printUsage();
            return;
        }
        
        try
        {
            if  ((m_iOptions & OPT_SINGLE) != 0)
            {
                Locale locale = extractLocale((String)m_XMLfiles.elementAt(0));
                String fileName = localeToString(locale);
                OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(fileName + ".html" ),"UTF-8");
                System.err.println("INFO: Writing to : " + fileName + ".html" );
                PrintWriter writer = new PrintWriter(os);
                printHTMLStart(writer);
                doSingleComparison(writer, m_XMLfiles, locale);
                printHTMLEnd(writer);
            }
            else if ((m_iOptions & OPT_BULK) != 0)
            {
                String fileName = "./Bulk.html";
                OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(fileName),"UTF-8");
                System.err.println("INFO: Writing to : " + fileName );
                PrintWriter writer = new PrintWriter(os);
                printHTMLStart(writer);
                doBulkComparison(writer);
                printHTMLEnd(writer);
            }
            
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    private int identifyOptions(String[] options)
    {
        int opts = 0;
        
        if (options.length ==0)
        {
            opts = OPT_UNKNOWN;
        }
        else
        {
            for (int k=0; k < options.length; k++)
            {
                if (options[k].compareTo(OPT_SINGLE_STR) == 0)
                {
                    opts += OPT_SINGLE;
                    for (int i = k+1; i < options.length; i++)
                    {
                        String data = options[i];
                        String filename = "";
                        //parse the optional ; separated string of format filename;name;url
                        String parts[] = data.split(STRING_SEPARATOR);
                        filename = parts[0];   //can be relative or absolute path incl. filename
   
                        File file = new File(filename);
                        if ( (!file.exists()) || (!file.isFile()) )
                        {
                            // Just skip over this file.  Continue processing.
                            printWarning("The file " + filename + " could not be found.");
                        }
                        else
                        {
                            String absfile = file.getAbsolutePath();
                            m_XMLfiles.add ( absfile );
                            m_ArgsCounter++;
                            
                            //deal with table headers
                            int index = m_XMLfiles.size()-1;
                            String name = "File"+index;
                            String url = absfile;
                            TableInfo ti = null;
                            if (parts.length == 3)  
                                ti = new TableInfo(index, parts[1], parts[2]);
                            else if (parts.length == 2)
                               ti = new TableInfo(index, parts[1], url);
                            else   //it's 1
                               ti = new TableInfo(index, name, url);
                            m_TableInfo.add(ti);
                        //        System.err.println(Integer.toString(m_XMLfiles.size()-1) + "  " +  parts[1] + "  " +  parts[2]);
                        }
                    }
                }
                else if (options[k].compareTo(OPT_BULK_STR) == 0)
                {
                    opts += OPT_BULK;
                    for (int i = 1; i < options.length; i++)
                    {
                        String data = options[i];
                        String filename = "";
                        //parse the optional ; separated string of format filename;name;url
                        String parts[] = data.split(STRING_SEPARATOR);
                        filename = parts[0];   //can be relative or absolute path incl. filename

                        File file = new File (filename);
                        if ( (!file.exists()) || (!file.isDirectory()) )
                        {
                            // Just skip over this file.  Continue processing.
                            printWarning("The directory " + options[i] + " could not be found.");
                        }
                        else
                        {
                            String absdir = file.getAbsolutePath();
                            m_XMLdirs.add (absdir);
                            m_ArgsCounter++;
                            
                            //deal with table headers
                            int index = m_XMLdirs.size()-1;
                            String name = "File"+index;
                            String url = absdir;
                            TableInfo ti = null;
                            if (parts.length == 3)  
                                ti = new TableInfo(index, parts[1], parts[2]);
                            else if (parts.length == 2)
                               ti = new TableInfo(index, parts[1], url);
                            else   //it's 1
                               ti = new TableInfo(index, name, url);
                            m_TableInfo.add(ti);
                        }
                    }
                }
                else  if (options[k].compareTo(OPT_SHOW_REFS_STR) == 0)
                    opts += OPT_UNRESOLVED_REFS;
                else  if (options[k].compareTo(OPT_SOLUTIONS_STR) == 0)
                    opts += OPT_SOLUTIONS;
            }
        }
        return opts;
    }
    
    private void printHTMLStart(PrintWriter writer)
    {
        System.err.println("INFO: Creating the comparison chart ");
        
        writer.print("<html>\n"+
                "    <head>\n"+
                "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n"+
                "        <style type=\"text/css\">\n"+
                "            <!--\n" +
                "            table        { border-spacing: 0; border-collapse: collapse; width:100%; \n" +
                "                           border: 1px solid black }\n" +
                "            td, th       { border-spacing: 0; border-collapse: collapse; color: black; \n" +
                "                           vertical-align: top; border: 1px solid black }\n" +
                "            .small       { font-size:75%; } \n" +
                "            th.file0     { background: #ffcc00 } \n" +  
                "            th.file1     { background: #ffff99 } \n" +
                "            th.solutions { background: #ffcc99 } \n" +                
                "            -->\n" +
                "        </style>"+
                "    </head>\n"+
                "    <body bgcolor=\"#FFFFFF\"> \n" +
                "        <p><b>LOCALE DATA AUDIT</b></p>");
        
        writer.print("        <p>Created on: " + cal.getTime() +"</p>\n");
    }
    
    private void printHTMLEnd(PrintWriter writer)
    {
        writer.print("<p>&nbsp;</p>");
        writer.print("<p>&nbsp;</p>");
        writer.print("          <p><b>SUMMARY : </b></p>");
        
        float percent = 0;
        if (m_CommonCounter > 0)
            percent =  (m_DiffCounter*100)/m_CommonCounter;
        
        writer.print("          <p><b>Total Number of locales audited : " +  m_TotalNumLocales + "</b></p>" +
                "           <p><b>Total Number of shared locale data across all locales : " + m_CommonCounter + "</b></p>" +
                "           <p><b>Total Number of conflicting locale data across all locales : " + m_DiffCounter + " (" +  percent + " %)" + "</b></p>" +
                "    </body>\n"+
                "</html>\n");
        writer.flush();
        writer.flush();
    }
    
    private void printTableStart(PrintWriter writer, Vector XMLfiles, Locale locale)
    {
        String localeStr = localeToString(locale);
        String displayLang = locale.getDisplayLanguage();
        String dispCountry = locale.getDisplayCountry();
        //sometimes locales are language only like interlingua
        String displayName = "<a name=\"" + localeStr + "\" href=\"#" + localeStr + "\">" + localeStr + "</a>" + " (" + displayLang + (dispCountry.equals("")==false ? ("_"+dispCountry) : "") + ")";    

        writer.println( "<p><b>" + m_TotalNumLocales + "&nbsp;&nbsp;&nbsp;" + displayName+ "</b></p>");
        writer.println( "<p class=\"small\">");
  // not really needed
  //      for (int i = 0; i < XMLfiles.size(); i++)
  //      {
  //          writer.println( "File"+i+": <a href=\""+XMLfiles.get(i)+"\">"+XMLfiles.get(i)+"</a><br />");
  //      }
        writer.println( "</p>");
        
         if (XMLfiles.size() < 2)
        {
            printWarning(XMLfiles.size() + " file was specified for comparison for " + locale.toString() + ".  Minimum of two required.");
            writer.println("Skipped as only " + Integer.toString (XMLfiles.size()) + " file available for comparison");
            return;
        }
        
        writer.println("<table>");
        writer.print(  "            <tr>\n" +
                "                <th width=10%>N.</th>\n"+
                "                <th width=10%>ParentNode</th>\n"+
                "                <th width=10%>Name</th>\n"+
                "                <th width=10%>ID</th>\n");
                
        for (int i=0; i < m_CurrNumFiles; i++)
        {
            //set the cell colour
            writer.print( "                <th class=\"");   
            if (i % 2 == 0)  
                writer.print("file0\"");
            else  
                writer.print("file1\"");   
            
            String column_name = ""; 
            String href = "";
            for (int j=0; i < m_TableInfo.size(); j++)
            {
                if ( ((TableInfo)m_TableInfo.elementAt(j)).index == i)
                {
                    column_name = ((TableInfo)m_TableInfo.elementAt(j)).name;
                    href = ((TableInfo)m_TableInfo.elementAt(j)).url;
                    href = href.replaceAll("LOCALE", localeStr+".xml");
                    
                    //deal with bulk comparison case
                    if (href.indexOf(".xml")==-1) href = (String) XMLfiles.get(i);
                    break;
                }
            }
            writer.println( "\"><a href=\"" + href + "\">" + column_name + "</a></th>");
        }
        
        if  ((m_iOptions & OPT_SOLUTIONS) != 0)
            writer.println( "                <th class=\"solutions\">Solutions</th>");     
        writer.println( "            </tr>");
    }
    
    
    private void printTableEnd(PrintWriter writer, Locale locale)
    {
        
        writer.println("</table>");
    }
    
    
    private void doSingleComparison(PrintWriter writer, Vector XMLfiles, Locale locale)
    {
        m_CurrNumFiles = XMLfiles.size();       
        m_TotalNumLocales++;
        
        String loc = localeToString(locale);
        System.err.println("INFO: Processing locale : " + loc);
        
        //start the table and writer column headings
        printTableStart(writer, XMLfiles, locale);
        
        //do the comparison and print the differences to HTML table
        compareData(writer, XMLfiles, locale);
        
        //close the table
        printTableEnd(writer, locale);
    }
    
    private void doBulkComparison(PrintWriter writer)
    {
        if (m_XMLdirs.size() < 2)
        {
            printError(m_XMLdirs.size() + " directories were specified for comparison.  Minimum of two required.");
            return;
        }

        TreeMap locales = new TreeMap();  //TreeMap isorte in ascending order key=locale String , value = LocaleFiles inst
        XMLFileFilter filter = new XMLFileFilter();
        Enumeration enDirs = m_XMLdirs.elements();
        while (enDirs.hasMoreElements())
        {
            String dirName = (String) enDirs.nextElement();  //=dir1, dir2, dir3 etc
            File dir = new File(dirName);
            File[] files = dir.listFiles(filter);
            for (int i = 0; i < files.length; i++)  
            { //create a LocaleFiles inst for each locale and add to inst all fully qualified filenames belonging to that locale
                Locale locale = extractLocale(files[i].getAbsolutePath() );
                String localeStr = localeToString (locale);
                
                boolean bFound = false;
                for (int j=0; j < locales.size(); j++)
                {
                    LocaleFiles lf = (LocaleFiles) locales.get(localeStr);
                    if ((lf != null) && (lf.m_locale.equals(locale)))
                    {
                        lf.m_files.add(files[i].getAbsolutePath());
                        bFound = true;
                        break;
                    }
                }
                
                if (bFound == false)
                {   //create a new entry for this locale and add the file location
                    LocaleFiles lf = new LocaleFiles();
                    lf.m_locale = locale;
                    lf.m_files.add(files[i].getAbsolutePath());
                    locales.put(localeStr, lf);
                }
            }
        }
        
        Collection col = locales.values ();
        Iterator it = col.iterator ();
        while (it.hasNext ())
        {
            int diffs_before = cntDiff;
            LocaleFiles lf = (LocaleFiles) it.next ();
            doSingleComparison(writer, (Vector) lf.m_files, (Locale) lf.m_locale);
            printSummaryPart1 (cntDiff-diffs_before, m_TotalNumLocales, lf.m_locale);
        }
        printSummaryPart2();
        
    }
    
    private void compareData(PrintWriter writer, Vector XMLfiles, Locale locale)
    {
        //loop thru' XMLfiles , reading in data from each one using OOLocaleReader
        //(sic PPLoclaeReader)
        
        //iterate through data printing differences any time they occur
        // i.e if 3 files are compared and 2 differ then print data from all 3 to HTML
        
        // Assumption: at least two files with absolute paths are contained in XMLfiles.
        
        OOLocaleReader readers[] = new OOLocaleReader[m_CurrNumFiles];
        int cntData = 0;
        
        // Get the XML data into data structures.
        for (int i = 0; i < m_CurrNumFiles; i ++)
        {
            readers[i] = new OOLocaleReader((String) XMLfiles.get(i));
            readers[i].readDocument(true);  //true => do resolve refs
        }
        
        // LC_INFO
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.LOCALE, OOConstants.LOCALE, readers, "m_LocaleInfo");
        cntDiff = compareString(writer, cntDiff, OOConstants.LOCALE, OOConstants.VERSION_DTD, "", readers, "m_versionDTD");
        cntDiff = compareString(writer, cntDiff, OOConstants.LOCALE, OOConstants.ALLOW_UPDATE_FROM_CLDR, "", readers, "m_allowUpdateFromCLDR");
        cntDiff = compareString(writer, cntDiff, OOConstants.LOCALE, OOConstants.VERSION, "", readers, "m_version");
        cntDiff = compareString(writer, cntDiff, OOConstants.LANGUAGE, OOConstants.LANG_ID, "", readers, "m_LangId");
        cntDiff = compareString(writer, cntDiff, OOConstants.LANGUAGE, OOConstants.DEFAULT_NAME, "", readers, "m_Language_DefaultName");
        cntDiff = compareString(writer, cntDiff, OOConstants.COUNTRY, OOConstants.COUNTRY_ID, "", readers, "m_Country_CountryID");
        cntDiff = compareString(writer, cntDiff, OOConstants.COUNTRY, OOConstants.DEFAULT_NAME, "", readers, "m_Country_DefaultName");
        cntDiff = compareString(writer, cntDiff, OOConstants.PLATFORM, OOConstants.PLATFORM_ID, "", readers, "m_PlatformID");
        cntDiff = compareString(writer, cntDiff, OOConstants.LC_INFO, OOConstants.VARIANT, "", readers, "m_Varient");
        
        // LC_TYPE
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.LC_CTYPE, OOConstants.SEPARATORS, readers, "m_Separators");
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.LC_CTYPE, OOConstants.MARKERS, readers, "m_Markers");
        cntDiff = compareString(writer, cntDiff, OOConstants.LC_CTYPE, OOConstants.TIME_AM, "", readers, "m_TimeAM");
        cntDiff = compareString(writer, cntDiff, OOConstants.LC_CTYPE, OOConstants.TIME_PM, "", readers, "m_TimePM");
        cntDiff = compareString(writer, cntDiff, OOConstants.LC_CTYPE, OOConstants.MEASUREMENT_SYSTEM, "", readers, "m_MeasurementSystem");
        
        // LC_FORMAT
        cntDiff = compareString(writer, cntDiff, OOConstants.LC_FORMAT, OOConstants.REPLACE_FROM_SMALL, "", readers, "m_ReplaceFrom");
        cntDiff = compareString(writer, cntDiff, OOConstants.LC_FORMAT, OOConstants.REPLACE_TO_SMALL, "", readers, "m_ReplaceTo");
        cntDiff = compareUniqueElemAtts(writer, cntDiff, OOConstants.FORMAT_ELEMENT, readers, "m_FormatElements");
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.LC_FORMAT, OOConstants.FORMAT_CODE, readers, "m_FormatCodes");
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.LC_FORMAT, OOConstants.FORMAT_CODE, readers, "m_FormatDefaultNames");
        
        // LC_COLLATION
        cntDiff = compareUniqueElemAtts(writer, cntDiff, OOConstants.COLLATOR, readers, "m_Collators", OOConstants.UNOID);
        cntDiff = compareVectorofStrings(writer, cntDiff, OOConstants.COLLATION_OPTIONS, OOConstants.TRANSLITERATION_MODULES, readers, "m_CollationOptions");
        
        // LC_SEARCH
        cntDiff = compareVectorofStrings(writer, cntDiff, OOConstants.SEARCH_OPTIONS, OOConstants.TRANSLITERATION_MODULES, readers, "m_SearchOptions");
        
        // LC_INDEX
        cntDiff = compareUniqueElemAtts(writer, cntDiff, OOConstants.INDEX_KEY, readers, "m_IndexKeys", OOConstants.UNOID);
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.LC_INDEX, OOConstants.INDEX_KEY, readers, "m_IndexKeysData");
        cntDiff = compareVectorofStrings(writer, cntDiff, OOConstants.LC_INDEX, OOConstants.UNICODE_SCRIPT, readers, "m_UnicodeScript");
        cntDiff = compareVectorofStrings(writer, cntDiff, OOConstants.LC_INDEX, OOConstants.FOLLOW_PAGE_WORD, readers, "m_FollowPageWord");
        
        // LC_CALENDAR
        cntDiff = compareUniqueElemAtts(writer, cntDiff, OOConstants.CALENDAR, readers, "m_Calendars", OOConstants.UNOID);
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.START_DAY_OF_WEEK, OOConstants.DAY_ID, readers, "m_StartDayOfWeek");
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.CALENDAR, OOConstants.MINIMAL_DAYS_IN_FIRST_WEEK, readers, "m_MinDaysInFirstweek");
        cntDiff = compareHashtableOfHashtablesOfStrings(writer, cntDiff, OOConstants.DAYS_OF_WEEK+"."+OOConstants.DAY+"."+OOConstants.DAY_ID, OOConstants.DEFAULT_ABBRV_NAME, readers, "m_AbbrDays");
        cntDiff = compareHashtableOfHashtablesOfStrings(writer, cntDiff, OOConstants.DAYS_OF_WEEK+"."+OOConstants.DAY+"."+OOConstants.DAY_ID, OOConstants.DEFAULT_FULL_NAME, readers, "m_WideDays");
        cntDiff = compareHashtableOfHashtablesOfStrings(writer, cntDiff, OOConstants.MONTHS_OF_YEAR+"."+OOConstants.MONTH+"."+OOConstants.MONTH_ID, OOConstants.DEFAULT_ABBRV_NAME, readers, "m_AbbrMonths");
        cntDiff = compareHashtableOfHashtablesOfStrings(writer, cntDiff, OOConstants.MONTHS_OF_YEAR+"."+OOConstants.MONTH+"."+OOConstants.MONTH_ID, OOConstants.DEFAULT_FULL_NAME, readers, "m_WideMonths");
        cntDiff = compareHashtableOfHashtablesOfStrings(writer, cntDiff, OOConstants.ERAS+"."+OOConstants.ERA+"."+OOConstants.ERA_ID, OOConstants.DEFAULT_ABBRV_NAME, readers, "m_AbbrEras");
        cntDiff = compareHashtableOfHashtablesOfStrings(writer, cntDiff, OOConstants.ERAS+"."+OOConstants.ERA+"."+OOConstants.ERA_ID, OOConstants.DEFAULT_FULL_NAME, readers, "m_WideEras");
        
        
        // LC_CURRENCY
        cntDiff = compareCurrencies (writer, cntDiff, OOConstants.CURRENCY, readers);
       
        cntDiff = compareUniqueElemAtts(writer, cntDiff, OOConstants.TRANSLITERATION, readers, "m_Transliterations", OOConstants.UNOID);
        
        // LC_MISC
        cntDiff = compareString(writer, cntDiff, OOConstants.FORBIDDEN_CHARACTERS, OOConstants.FORBIDDEN_LINE_BEGIN_CHARACTERS, "", readers, "m_ForbiddenBeginChar");
        cntDiff = compareString(writer, cntDiff, OOConstants.FORBIDDEN_CHARACTERS, OOConstants.FORBIDDEN_LINE_END_CHARACTERS, "", readers, "m_ForbiddenEndChar");
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.LC_MISC, OOConstants.RESERVED_WORDS, readers, "m_ReservedWords");
        
        // LC_NumberingLevel
        cntDiff = compareVectorofHashtables(writer, cntDiff, OOConstants.LC_NUMBERING_LEVEL, OOConstants.NUMBERING_LEVEL, readers, "m_NumberingLevels");
        cntDiff = compareVectorofHashtables(writer, cntDiff, OOConstants.OUTLINE_STYLE, OOConstants.OUTLUNE_NUMBERING_LEVEL, readers, "m_OutlineNumberingLevels");
        
        // RefCodes
        if ((m_iOptions & OPT_UNRESOLVED_REFS) != 0)
            cntDiff =  compareRefs(writer, cntDiff, readers, "m_Refs");
        
    }
    
    
    
    // Given 2+ OOLocaleReader ojects, compare a specified string variable.
    private int compareString(PrintWriter writer, int cntDiff, String parent, String attribute, String id, OOLocaleReader[] readers, String OOLocaleReaderVarName)
    {
        // First, put all 4 values in an array.
        String[] values = new String[m_CurrNumFiles];
        boolean diff = false;
        
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            // Using this method so that variable in each reader can be
            // accessed by name.  Otherwise, a datastructure would have to be
            // passed to this method already containing the different values.
            Class readerClass = readers[i].getClass();
            
            try
            {
                Field field = readerClass.getDeclaredField(OOLocaleReaderVarName);
                values[i] = (String) field.get(readers[i]);
                if ((i > 0) && (!diff))
                {
                    diff = strDiff(values[0], values[i]);
                    //System.err.println("INFO: strDiff("+values[0]+", "+values[i]+")="+diff);
                }
            }
            catch (NoSuchFieldException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        
        if (diff)
        {
            // Print out warning that there was a difference in values
            // between the files (reader objects).
            printHTMLRow(writer, cntDiff, parent, attribute, id, values);
            cntDiff++;
        }
        
        return cntDiff;
    }
    
    // Each file can contain a Vector of strings.  Each string represents the value
    // of an element from that file.
    private int compareVectorofStrings(PrintWriter writer, int cntDiff, String parentName, String elementName, OOLocaleReader[] readers, String OOLocaleReaderVarName)
    {
        // First, build a list of all String values.  Later, we will check if
        // each string value exists in each file.  If not, print out a warning
        // for that string.
        Vector strValues = new Vector();
        Vector[] vectors = new Vector[m_CurrNumFiles];
        
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            // Get vector from the file.
            Class readerClass = readers[i].getClass();
            
            try
            {
                Field field = readerClass.getDeclaredField(OOLocaleReaderVarName);
                vectors[i] = (Vector) field.get(readers[i]);
                if ( (vectors[i] != null) && (vectors[i].size() > 0) )
                {
                    Enumeration en = vectors[i].elements();
                    while (en.hasMoreElements())
                    {
                        // Extract the string values.
                        String value = (String) en.nextElement();
                        if (!strValues.contains(value))
                            strValues.add(value);
                    }
                }
            }
            catch (NoSuchFieldException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            
        }
        
        // Print a warning for any files missing string values that were
        // contained in other files.
        Enumeration en = strValues.elements();
        while (en.hasMoreElements())
        {
            String str = (String) en.nextElement();
            String output[] = new String[m_CurrNumFiles];
            boolean diff = false;
            
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                if (vectors[i].contains(str))
                    output[i] = "";
                else
                {
                    diff = true;
                    output[i] = MISSING_ELEM;
                }
            }
            if (diff)
            {
                printHTMLRow(writer, cntDiff, parentName, elementName, str, output);
                cntDiff++;
            }
        }
        
        return cntDiff;
    }
    
    // Refcodes are stored in a Hashtable of a mixture of strings and
    // hashtables.  Extract the string-only values and compare them.
    private int compareRefs(PrintWriter writer, int cntDiff, OOLocaleReader[] readers, String OOLocaleReaderVarName)
    {
        Hashtable[] hashtables = new Hashtable[m_CurrNumFiles];
        Hashtable[] hashtablesOfStrings = new Hashtable[m_CurrNumFiles];
        Hashtable[] daysOfWeek = new Hashtable[m_CurrNumFiles];
        Hashtable[] monthsOfYear = new Hashtable[m_CurrNumFiles];
        Hashtable[] eras = new Hashtable[m_CurrNumFiles];
        
        // Get refcodes from files
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            // Get hashtable of strings+hashtables from the file.
            Class readerClass = readers[i].getClass();
            
            try
            {
                Field field = readerClass.getDeclaredField(OOLocaleReaderVarName);
                hashtables[i] = (Hashtable) field.get(readers[i]);
                if (hashtables[i] != null)
                {
                    daysOfWeek[i] = (Hashtable) hashtables[i].get(OOConstants.DAYS_OF_WEEK);
                    monthsOfYear[i] = (Hashtable) hashtables[i].get(OOConstants.MONTHS_OF_YEAR);
                    eras[i] = (Hashtable) hashtables[i].get(OOConstants.ERAS);
                }
            }
            catch (NoSuchFieldException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            Enumeration en = hashtables[i].keys();
            hashtablesOfStrings[i] = new Hashtable();
            while (en.hasMoreElements())
            {
                String key = (String) en.nextElement();
                Object obj = hashtables[i].get(key);
                
                Class objClass = obj.getClass();
                if (objClass.getName().compareTo("java.lang.String") == 0)
                {
                    String value = (String) obj;
                    hashtablesOfStrings[i].put(key, value);
                }
            }
        }
        
        // Compare the string refcodes.
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.REF, OOConstants.REF, hashtablesOfStrings);
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.CALENDAR, OOConstants.DAYS_OF_WEEK+" _"+OOConstants.REF, daysOfWeek);
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.CALENDAR, OOConstants.MONTHS_OF_YEAR+" _"+OOConstants.REF, monthsOfYear);
        cntDiff = compareHashtableofStrings(writer, cntDiff, OOConstants.CALENDAR, OOConstants.ERAS+" _"+OOConstants.REF, eras);
        
        return cntDiff;
    }
    
    private int compareHashtableOfHashtablesOfStrings(PrintWriter writer, int cntDiff, String parent, String element, OOLocaleReader[] readers, String OOLocaleReaderVarName)
    {
        // First, get the hashtables from each of the files.
        Hashtable[] hashtables = new Hashtable[m_CurrNumFiles];
        Vector keys = new Vector();
        
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            // Get vector from the file.
            Class readerClass = readers[i].getClass();
            
            try
            {
                Field field = readerClass.getDeclaredField(OOLocaleReaderVarName);
                hashtables[i] = (Hashtable) field.get(readers[i]);
                if ( (hashtables[i] != null) && (hashtables[i].size() > 0) )
                {
                    Enumeration en = hashtables[i].keys();
                    while (en.hasMoreElements())
                    {
                        // Extract the keys.
                        String key = (String) en.nextElement();
                        if (!keys.contains(key))
                            keys.add(key);
                    }
                }
            }
            catch (NoSuchFieldException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        
        // 1. For each unique key, create a hashtable that
        // compareHashtableofStrings() will accept.
        Enumeration en = keys.elements();
        while (en.hasMoreElements())
        {
            Hashtable[] elements = new Hashtable[m_CurrNumFiles];
            String key = (String) en.nextElement();
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                if (hashtables[i] != null)
                    elements[i] = (Hashtable) hashtables[i].get(key);
            }
            cntDiff = compareHashtableofStrings(writer, cntDiff, key+":"+parent, element, elements);
        }
        
        return cntDiff;
    }
    
    
    // Each file (reader) has a hashtable of strings that need to be compared.
    // Hashtable keys identify the strings.
    private int compareHashtableofStrings(PrintWriter writer, int cntDiff, String parent, String element, OOLocaleReader[] readers, String OOLocaleReaderVarName)
    {
        // First, build a list of all the keys.
        Vector keys = new Vector();  // Vector of Strings.
        Hashtable[] hashtables =  new Hashtable[m_CurrNumFiles];
        
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            // Get hashtable from the file.
            Class readerClass = readers[i].getClass();
            
            try
            {
                Field field = readerClass.getDeclaredField(OOLocaleReaderVarName);
                hashtables[i] = (Hashtable) field.get(readers[i]);
                if ( (hashtables[i] != null) && (hashtables[i].size() > 0) )
                {
                    Enumeration en = hashtables[i].keys();
                    while (en.hasMoreElements())
                    {
                        // Extract the keys.
                        String key = (String) en.nextElement();
                        if (!keys.contains(key))
                            keys.add(key);
                    }
                }
            }
            catch (NoSuchFieldException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        
        cntDiff = compareHashtables(writer, cntDiff, keys, hashtables, parent, element);
        
        // Second, check for missing keys in files.
        // Third, check for discrepencies.
        
        return cntDiff;
    }
    
    private int compareHashtableofStrings(PrintWriter writer, int cntDiff, String parent, String element, Hashtable[] elements)
    {
        // First, build a list of all the keys.
        Vector keys = new Vector();  // Vector of Strings.
        
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            if (elements[i] != null)
            {
                Enumeration en = elements[i].keys();
                while (en.hasMoreElements())
                {
                    // Extract the keys.
                    String key = (String) en.nextElement();
                    if (!keys.contains(key))
                        keys.add(key);
                }
            }
        }
        
        cntDiff = compareHashtables(writer, cntDiff, keys, elements, parent, element);
        
        return cntDiff;
    }
    
    private int compareHashtables(PrintWriter writer, int cntDiff, Vector keys, Hashtable[] hashtables, String parent, String element)
    {
        // For each unique key found, print out errors for any
        // discrepencies in data.
        
        Enumeration en = keys.elements();
        while (en.hasMoreElements())
        {
            String key = (String) en.nextElement();
            String[] values = new String[m_CurrNumFiles];
            boolean diff = false;
            
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                if (hashtables[i] != null)
                {
                    values[i] = (String) hashtables[i].get(key);
                    if (values[i] == null)
                        values[i] = MISSING_ELEM;
                }
                else
                {
                    values[i] = MISSING_ELEM;
                }
                
                if ((i > 0) && (!diff))
                    diff = strDiff(values[0], values[i]);
                
            }
            
            if (diff)
            {
                printHTMLRow(writer, cntDiff, parent, element, key, values);
                cntDiff++;
            }
            else
                m_CommonCounter++;
        }
        
        return cntDiff;
    }
    
    // Used by compareUniqueElemAtts.  Builds list of attribute names and of
    // unique identifiers.
    private void parseElementHashtables(Hashtable hashtable, Vector keys, Vector attributes)
    {
        if ( (hashtable != null) && (hashtable.size() > 0) )
        {
            Enumeration en = hashtable.keys();
            while (en.hasMoreElements())
            {
                // Extract the keys.
                String key = (String) en.nextElement();
                if (!keys.contains(key))
                    keys.add(key);
                
                if ((key != null) && (key.length() > 0))
                {
                    // Build the list of all existing attributes.
                    Hashtable elementTab = (Hashtable) hashtable.get(key);
                    if ((elementTab != null) && (elementTab.size() > 0))
                    {
                        Enumeration subEn = elementTab.keys();
                        while (subEn.hasMoreElements())
                        {
                            // Each key is an attribute's name.
                            String subKey = (String) subEn.nextElement();
                            if ((subKey != null) && (subKey.length() > 0))
                            {
                                if (!attributes.contains(subKey))
                                    attributes.add(subKey);
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Input: Vector of Hashtables (key=attribute name, value=att value).
    // Each Hashtable represents one element.
    // Return: Hashtable (key=unique attribute value) of Hashtables
    // (key=attribute name, value=att value).
    private Hashtable convertElemVectorToHashtable(Vector data, String uniqueAttName)
    {
        Hashtable dataOut = new Hashtable();
        Enumeration en = data.elements();
        while (en.hasMoreElements())
        {
            Hashtable element = (Hashtable) en.nextElement();
            String attVal = (String) element.get(uniqueAttName);
            if ((attVal != null) && (attVal.length() > 0))
                dataOut.put(attVal, element);
        }
        
        return dataOut;
    }
    
    // Compare the attributes of elements.  Elements stored in Vectors in the
    // reader object, this is first converted to a Hashtable of Hashtables.
    private int compareUniqueElemAtts(PrintWriter writer, int cntDiff, String parent, OOLocaleReader[] readers, String OOLocaleReaderVarName, String uniqueAttName)
    {
        Hashtable[] hashtables = new Hashtable[m_CurrNumFiles];
        Vector keys = new Vector(); // each key identifies one element through the
        // unique value of a specific attribute.
        Vector attributes = new Vector(); // list of Strings of all attribute
        //names found in all elements.
        
        
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            // Get hashtable from object.
            Class readerClass = readers[i].getClass();
            
            try
            {
                Field field = readerClass.getDeclaredField(OOLocaleReaderVarName);
                Vector fileElements = (Vector) field.get(readers[i]);
                // Change this Vector to a Hashtable, to be compatible with
                // other methods in this class.
                hashtables[i] = convertElemVectorToHashtable(fileElements, uniqueAttName);
                
                parseElementHashtables(hashtables[i], keys, attributes);
                
            }
            catch (NoSuchFieldException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        
        cntDiff = compareAttValues(writer, cntDiff, parent, hashtables, keys, attributes);
        
        return cntDiff;
    }
    
    // Compare elements' attributes, given that each element can be
    // identified by a unique attribute value (like msgID).  The name of that
    // attribute will be identified by inspecting the key to the hashtable.
    // Elements are held in a Hashtable (key=unique att, val=Hashtable) of
    // Hashtables (key=att name, val=att value).
    private int compareUniqueElemAtts(PrintWriter writer, int cntDiff, String parent, OOLocaleReader[] readers, String OOLocaleReaderVarName)
    {
        // First, get the hashtables of elements from the readers.
        Hashtable[] hashtables = new Hashtable[m_CurrNumFiles];
        Vector keys = new Vector(); // each key identifies one element through the
        // unique value of a specific attribute.
        Vector attributes = new Vector(); // list of Strings of all attribute
        //names found in all elements.
        
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            // Get hashtable from object.
            Class readerClass = readers[i].getClass();
            
            try
            {
                Field field = readerClass.getDeclaredField(OOLocaleReaderVarName);
                hashtables[i] = (Hashtable) field.get(readers[i]);
                parseElementHashtables(hashtables[i], keys, attributes);
            }
            catch (NoSuchFieldException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        
        cntDiff = compareAttValues(writer, cntDiff, parent, hashtables, keys, attributes);
        
        return cntDiff;
    }
    
    private int compareAttValues(PrintWriter writer, int cntDiff, String parent, Hashtable[] hashtables, Vector keys, Vector attributes)
    {
        // Print out warning for missing elements.
        Enumeration en = keys.elements();
        while (en.hasMoreElements())
        {
            String[] output = new String[m_CurrNumFiles];
            boolean diff = false;
            
            String key = (String) en.nextElement();
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                Hashtable elementTable = (Hashtable) hashtables[i].get(key);
                if (elementTable == null)
                {
                    output[i] = MISSING_ELEM;
                    diff = true;
                }
                else
                    output[i] = "";
            }
            if (diff)
            {
                printHTMLRow(writer, cntDiff, parent, "", key, output);
                cntDiff++;
            }
            else
                m_CommonCounter++;
            
        }
        
        // For each key (element), then for each attribute, then for each file, and
        // compare values.
        en = keys.elements();
        while (en.hasMoreElements())
        {
            String key = (String) en.nextElement();
            Hashtable[] elementInstances = new Hashtable[m_CurrNumFiles];
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                elementInstances[i] = (Hashtable) hashtables[i].get(key);
            }
            
            
            Enumeration enAtt = attributes.elements();
            // Loop through each attribute.
            while (enAtt.hasMoreElements())
            {
                String[] values = new String[m_CurrNumFiles];
                String attName = (String) enAtt.nextElement();
                boolean diff = false;
                boolean valueFound = false;
                String value = null;
                for (int i = 0; i < m_CurrNumFiles; i++)
                {
                    if (elementInstances[i] == null)
                        values[i] = MISSING_ELEM;
                    else
                    {
                        
                        values[i] = (String) elementInstances[i].get(attName);
                        
                        if (values[i] == null)
                            values[i] = MISSING_ELEM;
                        else if (!valueFound)
                        {
                            valueFound = true;
                            value = values[i];
                        }
                        else if ((i > 0) && (!diff))
                            diff = strDiff(value, values[i]);
                    }
                }
                
                if (diff)
                {
                    printHTMLRow(writer, cntDiff, parent, "_"+attName, key, values);
                    cntDiff++;
                }
                else
                    m_CommonCounter++;
            }
        }
        
        return cntDiff;
        
    }
    
    // Each file has a Vector of Hashtables.  Each hashtable represents one
    // element with its attributes and values, but none of the attributes
    // are assumed to be unique.
    private int compareVectorofHashtables(PrintWriter writer, int cntDiff, String parentName, String elementName, OOLocaleReader[] readers, String OOLocaleReaderVarName)
    {
        // First, build new vector of all vector (of hashtables).  Then, look for each
        // hashtable in each file.
        Vector[] vectors = new Vector[m_CurrNumFiles];
        Vector hashtables = new Vector();
        
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            // Get hashtable from object.
            Class readerClass = readers[i].getClass();
            
            try
            {
                Field field = readerClass.getDeclaredField(OOLocaleReaderVarName);
                vectors[i] = (Vector) field.get(readers[i]);
            }
            catch (NoSuchFieldException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        
        // create a vector of all hashtables, with no duplicate hashtables stored.
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            Enumeration en = vectors[i].elements();
            while (en.hasMoreElements())
            {
                Hashtable hashtable = (Hashtable) en.nextElement();
                boolean containsTable = false;
                
                // Loop through vector of all hashtables, and look for duplicate.
                // If no duplicate, add it to the vector.
                Enumeration enTabs = hashtables.elements();
                while (enTabs.hasMoreElements() && (!containsTable))
                {
                    Hashtable tab = (Hashtable) enTabs.nextElement();
                    if (tab.equals(hashtable))
                        containsTable = true;
                }
                
                if (!containsTable)
                    hashtables.add(hashtable);
            }
        }
        
        Enumeration en = hashtables.elements();
        while (en.hasMoreElements())
        {
            Hashtable hashtable = (Hashtable) en.nextElement();
            boolean diff = false;
            String[] output = new String[m_CurrNumFiles];
            
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                boolean foundTable = false;
                Enumeration enTabs = vectors[i].elements();
                while (enTabs.hasMoreElements() && (!foundTable))
                {
                    Hashtable element = (Hashtable) enTabs.nextElement();
                    if (element.equals(hashtable))
                        foundTable = true;
                }
                
                if (foundTable)
                    output[i] = "";
                else
                {
                    diff = true;
                    output[i] = MISSING_ELEM;
                }
            }
            
            // print out warning.
            if (diff)
            {
                StringBuffer id = new StringBuffer();
                Enumeration enTab = hashtable.keys();
                while (enTab.hasMoreElements())
                {
                    String key = (String) enTab.nextElement();
                    String value = (String) hashtable.get(key);
                    id.append(" _"+key+"=\""+value+"\"");
                    
                }
                
                printHTMLRow(writer, cntDiff, parentName, elementName, id.toString(), output);
                cntDiff++;
            }
            else
                m_CommonCounter++;
            
        }
        
        // Now, have list of all hashtables.  Check for existance of each
        // in each file.  If one is missing, print a warning.
        
        //cntDiff = compareAttValues(writer, cntDiff, parent, hashtables, keys, attributes);
        
        return cntDiff;
    }
    
    
        // Given 2+ OOLocaleReader ojects, compare a specified string variable.
    private int compareCurrencies(PrintWriter writer, int cntDiff, String parent, OOLocaleReader[] readers)
    {
        boolean diff = false;
        
        int max = 0;
        for (int i = 0; i < m_CurrNumFiles; i++)
        {
            //m_CurrencyData  is a vector of vectors, find out which OO file ahs the most currencies defined, should be the same
            if ( readers[i].m_CurrencyData.size () > max)
                max = readers[i].m_CurrencyData.size ();
        }

        // inner vector holds data in following order : CurrencyID,symbol,code,name, blank ,default, usedInCompatibleFormatCode, legacyOnly (if defined) 
        String[] values = new String[m_CurrNumFiles];
        for (int j=0; j < max; j++)
        {  //each OO.o file has multiple currencies
            String code = "";
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                code = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(2);
                values[i] = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(0);
                diff = strDiff(values[0], values[i]);
                if (diff) break;  //found a difference print all
            }
            if (diff)
            {
                printHTMLRow(writer, cntDiff, parent, OOConstants.CURRENCY_ID, code, values);
                cntDiff++;
            }
        }    
            
        values = new String[m_CurrNumFiles];
         for (int j=0; j < max; j++)
        {  //each OO.o file has multiple currencies
            String code = "";
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                code = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(2);
                values[i] = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(1);
                diff = strDiff(values[0], values[i]);
                if (diff) break;  //found a difference print all
            }
            if (diff)
            {
                printHTMLRow(writer, cntDiff, parent, OOConstants.CURRENCY_SYMBOL, code, values);
                cntDiff++;
            }
        }            
        
        values = new String[m_CurrNumFiles];
         for (int j=0; j < max; j++)
        {  //each OO.o file has multiple currencies
            String code = "";
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                code = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(2);
                values[i] = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(2);
                diff = strDiff(values[0], values[i]);
                if (diff) break;  //found a difference print all
            }
            if (diff)
            {
                printHTMLRow(writer, cntDiff, parent, OOConstants.CURRENCY, code, values);
                cntDiff++;
            }
        }     
        
        values = new String[m_CurrNumFiles];
         for (int j=0; j < max; j++)
        {  //each OO.o file has multiple currencies
            String code = "";
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                code = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(2);
                values[i] = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(3);
                diff = strDiff(values[0], values[i]);
                if (diff) break;  //found a difference print all
            }
            if (diff)
            {
                printHTMLRow(writer, cntDiff, parent, OOConstants.CURRENCY_NAME, code, values);
                cntDiff++;
            }
        }    
        
        values = new String[m_CurrNumFiles];
         for (int j=0; j < max; j++)
        {  //each OO.o file has multiple currencies
            String code = "";
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                code = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(2);
                values[i] = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(5);
                diff = strDiff(values[0], values[i]);
                if (diff) break;  //found a difference print all
            }
            if (diff)
            {
                printHTMLRow(writer, cntDiff, parent, OOConstants.DEFAULT, code, values);
                cntDiff++;
            }
        }    
        
        values = new String[m_CurrNumFiles];
         for (int j=0; j < max; j++)
        {  //each OO.o file has multiple currencies
            String code = "";
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                code = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(2);
                values[i] = (String) ((Vector)readers[i].m_CurrencyData.elementAt(j)).elementAt(6);
                diff = strDiff(values[0], values[i]);
                if (diff) break;  //found a difference print all
            }
            if (diff)
            {
                printHTMLRow(writer, cntDiff, parent, OOConstants.USED_IN_COMPARTIBLE_FORMATCODES_SMALL, code, values);
                cntDiff++;
            }
        }    
        
        //legacyOnly is optional attribute
        values = new String[m_CurrNumFiles];
         for (int j=0; j < max; j++)
        {  //each OO.o file has multiple currencies
            String code = "";
            for (int i = 0; i < m_CurrNumFiles; i++)
            {
                Vector v = (Vector) readers[i].m_CurrencyData.elementAt(j);
                code = (String) v.elementAt(2);
                if (v.size() >7) values[i] = (String) v.elementAt(7);   //it's optional'
                diff = strDiff(values[0], values[i]);
                if (diff) break;  //found a difference print all
            }
            if (diff)
            {
                printHTMLRow(writer, cntDiff, parent, OOConstants.LEGACY_ONLY, code, values);
                cntDiff++;
            }
        }    

        return cntDiff;
    }
    
    // Return true if both are null, or both are not null.  False otherwise.
    private boolean nullDiff(Object obj0, Object obj1)
    {
        boolean null0 = (obj0 == null);
        boolean null1 = (obj1 == null);
        
        return (null0 != null1);
    }
    
    // Compares strings, but firstly checks for null strings.  If both strings
    // are null, return false.
    private boolean strDiff(String str0, String str1)
    {
        boolean diff = false;
        
        boolean nullDiff = nullDiff(str0, str1);
        if (nullDiff == true)
            diff = nullDiff;
        else if (str0 == null)
            diff = false;
        else
            diff = (str0.compareTo(str1) != 0);
        
        return diff;
    }
    
    private void printHTMLRow(PrintWriter writer, int number, String parentNode, String name, String id, String[] values)
    {
        m_DiffCounter++;  // overall statistic
        
        writer.println("<tr>");
        writer.println("<td><a name=\"" + number + "\" href=\"#" + number + "\">" + number + "</a></td>");
        writer.println("<td>" + parentNode + "</td>");
        writer.println("<td>" + name + "</td>");
        writer.println("<td>" + id + "</td>");
        
        for (int i = 0; i < values.length; i++)
        {
            writer.println("<td>" + values[i] + "</td>");
        }
        
        writer.println("</tr>");
    }
    
    private void printWarning(String str)
    {
        System.err.println("Warning: " + str);
    }
    
    private void printError(String str)
    {
        System.err.println("Error: " + str);
    }
    
//========= utility methods below =========
    
    
    // extract filenmae without extension from full path and convert this to Locale instance
    private Locale extractLocale(String filename)
    {
        if (filename == null)
            return  null;
        
        Locale locale = null;
        
        int iLastSlash = filename.lastIndexOf('/');
        int iLastPeriod = filename.lastIndexOf('.');
        String loc = filename.substring(iLastSlash+1, iLastPeriod);
        
        String lang = null;
        String country = null;
        if (loc.indexOf('_') >= 0)
        {
            lang = loc.substring(0, loc.indexOf('_'));
            country = loc.substring(loc.indexOf('_')+1, loc.length());
            locale = new Locale(lang, country);
        }
        else
        {
            lang = loc.substring(0, loc.length());
            locale = new Locale(lang);
        }
        
        return locale;
        
    }
    
    private String localeToString(Locale locale)
    {
        if (locale == null)
            return "";
        
        //sometimes the country is not specified (like ia - interlingua)
        String loc = locale.getLanguage();
        
        //deal with legacy lang codes
        if (loc.equals("in")) loc = "id";
        if (loc.equals("iw")) loc = "he";
        if (loc.equals("ji")) loc = "yi";
        if (loc.equals("jw")) loc = "jv";
        
        String country = locale.getCountry();
        
        loc = loc + (locale.getCountry().equals("")==false ? ("_" + country) : "");
        return loc;
    }
    
    private void printSummaryPart1 (int diffs, int counter, Locale locale)
    {
        //print summary of conflicts to summary.txt
        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter("summary.txt",true));
            out.write (Integer.toString(counter) + "  " + localeToString(locale) + "  Conflicts = " + Integer.toString(diffs) + "\n");
            out.close();
        }
        catch (IOException e)
        {
        }   
        
        //count locales with 0,1, 2 etc conflicts
        Integer key = new Integer(diffs);
    //    Integer value = new Integer (1);
        if (m_DiffsPerLocale.get(key) != null)
        {
            //increment the value already in table
           Integer val = (Integer) m_DiffsPerLocale.get(key);
           m_DiffsPerLocale.remove (key);
           int v = val.intValue();
           m_DiffsPerLocale.put (key, new Integer (++v));
        }
        else
            m_DiffsPerLocale.put (key, new Integer (1));
        
    }
    
    private void printSummaryPart2 ()
    {
        //print summary of conflicts to summary.txt
        try
        {
            int total_locales = 0;
            int total_conflicts = 0;
            BufferedWriter out = new BufferedWriter(new FileWriter("summary.txt",true));
            out.write ("\n\n# Conflicts" + "  # Locales\n");
            Enumeration en = m_DiffsPerLocale.keys();
            while (en.hasMoreElements()) 
            {
                Integer num_conflicts = (Integer) en.nextElement();
                Integer num_locales = (Integer) m_DiffsPerLocale.get(num_conflicts);
                out.write ("  " + num_conflicts.toString() + "          " + num_locales.toString() + "\n");   
                total_conflicts += num_conflicts.intValue() * num_locales.intValue();
                total_locales += num_locales.intValue();
            }
            out.write ("\nTOTAL CONFLICTS = " + Integer.toString(total_conflicts) );
            out.write ("\nTOTAL LOCALES   = " + Integer.toString(total_locales) );
            out.write ("\nCreated on: " + cal.getTime());
            out.write ("\n===================================================\n");
            out.close();
        }
        catch (IOException e)
        {
        }       
    }
    
    protected class XMLFileFilter implements FileFilter
    {
        // Accept a file if its name ends in .xml.  However, it does not validate
        // the inner contents of the file.
        public boolean accept(File pathname)
        {
            boolean accepted = false;
            String filename = pathname.getPath();
            if ((filename != null) && (filename.length()>4))
                accepted = (filename.substring(filename.length()-4).toLowerCase().compareTo(".xml") == 0);
            return accepted;
        }
    }
    
}
