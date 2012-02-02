/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

import org.unicode.cldr.icu.LDMLConstants;

/*
 * class has ONLY generic methods for writing to LDML
 * these methods are platform independent
 */

public class LDMLLocaleWriter extends XMLWriter
{
    protected Vector m_LegacyCurrencies = new Vector();
    
    public LDMLLocaleWriter(PrintStream out)
    {
        super(out);
    }
    
    public LDMLLocaleWriter(PrintStream out, PrintStream err)
    {
        super(out, err);
    }
    
    public void open()
    {
        println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        String dtd = "<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/1.2/ldml.dtd\">";
        println(dtd);
        println(LDMLConstants.LDML_O);
        indent();
    }

    //normally dtdLocation should be null
    //if it is specified then cldrVersion is ignored
    // as cldrVersion is used to compose the URLs of the DTDs
    // nsDTD = name of dtd file for the namespace, must be specified
    public boolean open(String namespace, String dtdLocation, String nsDTD, String cldrVersion)
    {
        println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        
        //do ldml.dtd stuff
        String dtd = "<!DOCTYPE ldml SYSTEM \"";
        if ((dtdLocation != null) && (dtdLocation.compareTo("")!=0))
            dtd = dtd + dtdLocation + "/ldml.dtd\"";
        else
            dtd = dtd + "http://www.unicode.org/cldr/dtd/" +  cldrVersion + "/ldml.dtd\"";
        
        //do <namespace.dtd> stuff
        if ((namespace == null) || (namespace.compareTo("")==0))
        {
            dtd += ">";
        }
        else
        {
            if ((dtdLocation != null) && (dtdLocation.compareTo("")!=0))
            {
                dtd = dtd + "\n" + "[\n" + "\t<!ENTITY % " + namespace + " SYSTEM \"" + dtdLocation + "/" + nsDTD + "\">\n%" + namespace +";\n]\n>" ;
            }
            else
            {
                dtd = dtd + "\n" + "[\n" + "\t<!ENTITY % " + namespace + " SYSTEM \"http://www.unicode.org/cldr/dtd/" + cldrVersion + "/" + nsDTD + "\">\n%" + namespace +";\n]\n>" ;
            }
        }
               
        println(dtd);
        println(LDMLConstants.LDML_O);
        indent();
        return true;
    }
    
    public void close()
    {
        outdent();
        println(LDMLConstants.LDML_C);
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
    
    public String getDate()
    {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1 ;  //Calendar first month is 0
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String date = year + "/" + month + "/" + day;
        return date;
    }
    
        public String getTime()
    {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE) ;  
        int sec = cal.get(Calendar.SECOND);
        String time = hour + ":" + min + ":" + sec;
        return time;
    }
    
    protected void writeIdentity(Locale locale, String message)
    {
        if (locale == null)
            return;
        
        String lang = locale.getLanguage();
        String terr = locale.getCountry();
        String var = locale.getVariant();
        
        println(LDMLConstants.IDENTITY_O);
        indent();
        
        String version = "$Revision$";  //tihs is the CVS version not the CLDR version
        String msgStr = "";
        if (message != null) msgStr = message;
        String versionStr = "<" + LDMLConstants.VERSION + " " + LDMLConstants.NUMBER + "=\"" + version + "\">" + msgStr + LDMLConstants.VERSION_C;
        println(versionStr);
        
        String generationStr = "<" + LDMLConstants.GENERATION + " " + LDMLConstants.DATE + "=\"$Date$\"/>";
        println(generationStr);
        
        String language = "<" + LDMLConstants.LANGUAGE + " " + LDMLConstants.TYPE + " = \"" + lang + "\"/>";
        println(language);
        //no script
        if (terr.compareTo("")!=0)
        {
            String territory = "<" + LDMLConstants.TERRITORY + " " + LDMLConstants.TYPE + " = \"" + terr + "\"/>";
            println(territory);
        }
        if (var.compareTo("")!=0)
        {
            String variant = "<" + LDMLConstants.VARIANT + " " + LDMLConstants.TYPE + " = \"" + var + "\"/>";
            println(variant);
        }
        outdent();
        println(LDMLConstants.IDENTITY_C);
    }
    
    public void writeIdentity(Hashtable data)
    {
        if ((data == null) || (data.size()==0))
        {
            Logging.Log1("No LDML Identity data to write ");
            return;
        }
        
        String version_number = (String) data.get(LDMLConstants.VERSION + " " + LDMLConstants.NUMBER);
        
        //the generation comment is written under version element as generation element is empty
        String generation_info = (String) data.get(LDMLConstants.GENERATION);
        
        String language_type = (String) data.get(LDMLConstants.LANGUAGE + " " + LDMLConstants.TYPE);
        String script_type = (String) data.get(LDMLConstants.SCRIPT + " " + LDMLConstants.TYPE);
        String territory_type = (String) data.get(LDMLConstants.TERRITORY + " " + LDMLConstants.TYPE);
        String variant_type = (String) data.get(LDMLConstants.VARIANT + " " + LDMLConstants.TYPE);
        
        //specials
        String platformID = (String) data.get(OpenOfficeLDMLConstants.PLATFORM_ID);
        
        println(LDMLConstants.IDENTITY_O);
        indent();
        
        if (version_number != null)
        {
            String versionStr = "<" + LDMLConstants.VERSION + " " + LDMLConstants.NUMBER + "=\"" + version_number + "\"";
            if (generation_info == null)
                versionStr += "/>";
            else
                versionStr = versionStr + ">" + generation_info + LDMLConstants.VERSION_C;
            println(versionStr);
        }
        
        String generationStr = "<" + LDMLConstants.GENERATION + " " + LDMLConstants.DATE + "=\"$Date$\"/>";
        println(generationStr);
        
        if (language_type != null)
        {
            String language = "<" + LDMLConstants.LANGUAGE + " " + LDMLConstants.TYPE + "=\"" + language_type + "\"/>";
            println(language);
        }
        if (script_type != null)
        {
            String language = "<" + LDMLConstants.SCRIPT + " " + LDMLConstants.TYPE + "=\"" + script_type + "\"/>";
            println(language);
        }
        if ((territory_type != null) && (territory_type != ""))
        {
            String territory = "<" + LDMLConstants.TERRITORY + " " + LDMLConstants.TYPE + "=\"" + territory_type + "\"/>";
            println(territory);
        }
        if (variant_type != null)
        {
            String variant = "<" + LDMLConstants.VARIANT + " " + LDMLConstants.TYPE + "=\"" + variant_type + "\"/>";
            println(variant);
        }
        
        if (platformID != null)  //special
        {
            println("<" + LDMLConstants.SPECIAL + " " + LDMLConstants.XMLNS + ":" + XMLNamespace.OPEN_OFFICE + "=\"" + XMLNamespace.OPEN_OFFICE_WWW + "\">" );
            indent();
            printNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.PLATFORM_ID, true);
            print(platformID);
            printlnNS(XMLNamespace.OPEN_OFFICE, OpenOfficeLDMLConstants.PLATFORM_ID, false);
            
            outdent();
            println(LDMLConstants.SPECIAL_C);
        }
        
        outdent();
        println(LDMLConstants.IDENTITY_C);
        
    }
    
/*    public void writeLocaleDisplaynames(Hashtable data)
    {
        if ((data == null) || (data.size()==0))
        {
            Logging.Log1("No LDML localeDisplayNames data to write ");
            return;
        }
 
        String language_type = (String) data.get(LDMLConstants.LANGUAGE + " " + LDMLConstants.TYPE);
        String language = (String) data.get(LDMLConstants.LANGUAGE);
        String territory_type = (String) data.get(LDMLConstants.TERRITORY + " " + LDMLConstants.TYPE);
        String territory = (String) data.get(LDMLConstants.TERRITORY);
 
        println(LDMLConstants.LDN_O);
        indent();
 
        if ( (language_type != null) && (language != null))
        {
            println(LDMLConstants.LANGUAGES_O);
            indent();
            String lang = "<" + LDMLConstants.LANGUAGE + " " + LDMLConstants.TYPE + "=\"" + language_type + "\">" + language + LDMLConstants.LANGUAGE_C;
            println(lang);
            outdent();
            println(LDMLConstants.LANGUAGES_C);
        }
 
        if ((territory_type != null) && (territory_type.compareTo("")!=0))
        {
            println(LDMLConstants.TERRITORIES_O);
            indent();
            String terr = "<" + LDMLConstants.TERRITORY + " " + LDMLConstants.TYPE + "=\"" + territory_type + "\">" + territory + LDMLConstants.TERRITORY_C;
            println(terr);
            outdent();
            println(LDMLConstants.TERRITORIES_C);
        }
 
        outdent();
        println(LDMLConstants.LDN_C);
    }
 */
    
    protected void writeDayWidth(Hashtable days, String dayWidthType)
    {
        //check inputs and dayWidthType value which is #REQUIRED
        if ((days == null) || (dayWidthType == null) || (dayWidthType == ""))
            return;
        
        String dayWidthString = "<" + LDMLConstants.DAY_WIDTH + " type =\"" + dayWidthType + "\">";
        println(dayWidthString);
        indent();
        
        //write the days in ordered fashion, starting at Sunday
        String dayStr = null;
        String day = (String) days.get(LDMLConstants.SUN);
        if (day != null)
        {
            dayStr = "<" + LDMLConstants.DAY + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.SUN + "\">" + day + LDMLConstants.DAY_C;
            println(dayStr);
        }
        day = (String) days.get(LDMLConstants.MON);
        if (day != null)
        {
            dayStr = "<" + LDMLConstants.DAY + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MON + "\">" + day + LDMLConstants.DAY_C;
            println(dayStr);
        }
        day = (String) days.get(LDMLConstants.TUE);
        if (day != null)
        {
            dayStr = "<" + LDMLConstants.DAY + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.TUE + "\">" + day + LDMLConstants.DAY_C;
            println(dayStr);
        }
        day = (String) days.get(LDMLConstants.WED);
        if (day != null)
        {
            dayStr = "<" + LDMLConstants.DAY + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.WED + "\">" + day + LDMLConstants.DAY_C;
            println(dayStr);
        }
        day = (String) days.get(LDMLConstants.THU);
        if (day != null)
        {
            dayStr = "<" + LDMLConstants.DAY + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.THU + "\">" + day + LDMLConstants.DAY_C;
            println(dayStr);
        }
        day = (String) days.get(LDMLConstants.FRI);
        if (day != null)
        {
            dayStr = "<" + LDMLConstants.DAY + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.FRI + "\">" + day + LDMLConstants.DAY_C;
            println(dayStr);
        }
        day = (String) days.get(LDMLConstants.SAT);
        if (day != null)
        {
            dayStr = "<" + LDMLConstants.DAY + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.SAT + "\">" + day + LDMLConstants.DAY_C;
            println(dayStr);
        }
        outdent();
        println(LDMLConstants.DAY_WIDTH_C);
    }
    
    protected void writeMonthWidth(Hashtable months, String monthWidthType)
    {
        //check inputs and dayWidthType value which is #REQUIRED
        if ((months == null) || (monthWidthType == null) || (monthWidthType == ""))
            return;
        
        String monthWidthString = "<" + LDMLConstants.MONTH_WIDTH + " type =\"" + monthWidthType + "\">";
        println(monthWidthString);
        indent();
        
        //write the days in ordered fashion, starting at Sunday
        String monthStr = null;
        String month = (String) months.get(LDMLConstants.MONTH_1);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_1 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_2);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_2 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_3);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_3 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_4);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_4 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_5);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_5 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_6);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_6 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_7);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_7 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_8);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_8 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_9);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_9 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_10);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_10 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_11);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_11 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_12);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_12 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        month = (String) months.get(LDMLConstants.MONTH_13);
        if (month != null)
        {
            monthStr = "<" + LDMLConstants.MONTH + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.MONTH_13 + "\">" + month + LDMLConstants.MONTH_C;
            println(monthStr);
        }
        outdent();
        println(LDMLConstants.MONTH_WIDTH_C);
    }
    
    protected void writeQuarterWidth(Hashtable data, String widthType)
    {
        //check inputs and widthType value which is #REQUIRED
        if ((data == null) || (widthType == null) || (widthType.compareTo( "")==0))
            return;
        
        String widthString = "<" + LDMLConstants.QUARTER_WIDTH + " type =\"" + widthType + "\">";
        println(widthString);
        indent();
        
        //write the days in ordered fashion, starting at Sunday
        
        String quarterStr = null;
        String quarter = (String) data.get(LDMLConstants.Q_1);
        if (quarter != null)
        {
            quarterStr = "<" + LDMLConstants.QUARTER + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.Q_1 + "\">" + quarter + LDMLConstants.QUARTER_C;
            println(quarterStr);
        }
        quarter = (String) data.get(LDMLConstants.Q_2);
        if (quarter != null)
        {
            quarterStr = "<" + LDMLConstants.QUARTER + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.Q_2 + "\">" + quarter + LDMLConstants.QUARTER_C;
            println(quarterStr);
        }
        quarter = (String) data.get(LDMLConstants.Q_3);
        if (quarter != null)
        {
            quarterStr = "<" + LDMLConstants.QUARTER + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.Q_3 + "\">" + quarter + LDMLConstants.QUARTER_C;
            println(quarterStr);
        }
        quarter = (String) data.get(LDMLConstants.Q_4);
        if (quarter != null)
        {
            quarterStr = "<" + LDMLConstants.QUARTER + " " + LDMLConstants.TYPE + "=\"" + LDMLConstants.Q_4 + "\">" + quarter + LDMLConstants.QUARTER_C;
            println(quarterStr);
        }
        outdent();
        println(LDMLConstants.QUARTER_WIDTH_C);
    }
    
    protected void writeEras(Hashtable eras)
    {
        if (eras == null)
            return;
        
        Enumeration enKeys = eras.keys();
        Enumeration enValues = eras.elements();
        while (enKeys.hasMoreElements() == true)
        {
            String key = (String) enKeys.nextElement();
            String value = (String) enValues.nextElement();
            if ((key != null) && (value != null))
            {
                String eraStr = "<" + LDMLConstants.ERA + " " + LDMLConstants.TYPE + "=\"" + key + "\">" + value + LDMLConstants.ERA_C;
                println(eraStr);
            }
        }
    }
    
    
    protected void writeDelimiterss(Hashtable data)
    {
        if (data == null)
            return;
        
        String quoteStart = (String) data.get(LDMLConstants.QS);
        String quoteEnd = (String) data.get(LDMLConstants.QE);
        String altQuoteStart = (String) data.get(LDMLConstants.AQS);
        String altQuoteEnd = (String) data.get(LDMLConstants.AQE);
        
        if ((quoteStart!=null) || (quoteEnd!=null) || (altQuoteStart!=null) || (altQuoteEnd!=null))
        {
            println(LDMLConstants.DELIMITERS_O);
            indent();
            
            if (quoteStart != null)
                println(LDMLConstants.QS_O + quoteStart + LDMLConstants.QS_C);
            if (quoteEnd != null)
                println(LDMLConstants.QE_O + quoteEnd + LDMLConstants.QE_C);
            if (altQuoteStart != null)
                println(LDMLConstants.AQS_O + altQuoteStart + LDMLConstants.AQS_C);
            if (altQuoteEnd != null)
                println(LDMLConstants.AQE_O + altQuoteEnd + LDMLConstants.AQE_C);
            outdent();
            println(LDMLConstants.DELIMITERS_C);
        }
    }
    
    protected void writeMeasurement(Hashtable data)
    {
        if (data == null)
            return;
        
        String measurement = (String) data.get(LDMLConstants.MEASUREMENT);
        if (measurement != null)
        {
            println(LDMLConstants.MEASUREMENT_O);
            indent();
            println("<" + LDMLConstants.MS + " " + LDMLConstants.TYPE + "=\"" + measurement + "\"/>");
            outdent();
            println(LDMLConstants.MEASUREMENT_C);
        }
    }
    
    // input Vector[0] = locale, Vector[1] = type (optional)
    protected void writeAlias(Vector alias)
    {
        if ((alias == null) || (alias.size()==0))
            return;
        print("<" + LDMLConstants.ALIAS + " " + LDMLConstants.SOURCE + "=\"" + alias.elementAt(0) + "\"");
        if (alias.size() >1)
            println(" " + LDMLConstants.TYPE + "=\"" + alias.elementAt(1) + "\"/>");
        else
            println("/>");
    }
    
    
    protected void writeWeek(String minDays, String firstDay, String weStart, String weEnd, String calendar)
    {
        if ((firstDay != null) || (minDays != null) || (weStart != null) || (weEnd != null))
        {
            println(LDMLConstants.WEEK_O);
            indent();
            if (minDays != null)
                println("<" + LDMLConstants.MINDAYS + " " + LDMLConstants.COUNT + "=\"" + minDays +"\"/>");
            else
                Logging.Log3("\tNo minDays to write for calendar : " + calendar);
            
            if (firstDay != null)
                println("<" + LDMLConstants.FIRSTDAY + " " + LDMLConstants.DAY + "=\"" + firstDay +"\"/>");
            else
                Logging.Log3("\tNo firstDay to write for calendar : " + calendar);
            
            if (weStart != null)
                println("<" + LDMLConstants.WENDSTART + " " + LDMLConstants.DAY + "=\"" + weStart +"\"/>");
            
            if (weEnd != null)
                println("<" + LDMLConstants.WENDEND + " " + LDMLConstants.DAY + "=\"" + weEnd +"\"/>");
            
            outdent();
            println(LDMLConstants.WEEK_C);
        }
        else
            Logging.Log3("\tNo week information to write for calendar : " + calendar);
    }
    
    protected void writeAmPm(String am, String pm, String calendar)
    {
        if (am != null)
            println(LDMLConstants.AM_O + am + LDMLConstants.AM_C);
        else
            Logging.Log3("\tNo am to write for calendar : " + calendar);
        
        //pm
        if (pm != null)
            println(LDMLConstants.PM_O + pm + LDMLConstants.PM_C);
        else
            Logging.Log3("\tNo pm to write for calendar : " + calendar);
    }
    
    // write a single date format pattern
    protected void  WriteDateFormatLength(String pattern, String dateFormatLengthType)
    {
        //check inputs
        if ((pattern == null) || (dateFormatLengthType == null))
            return;
        
        String dateFormatLengtStr = "<" + LDMLConstants.DFL + " " + LDMLConstants.TYPE + " = \"" + dateFormatLengthType + "\">";
        println(dateFormatLengtStr);
        indent();
        println(LDMLConstants.DATE_FORMAT_O);
        indent();
        println(LDMLConstants.PATTERN_O + pattern + LDMLConstants.PATTERN_C);
        outdent();
        println(LDMLConstants.DATE_FORMAT_C);
        outdent();
        println(LDMLConstants.DFL_C);
    }
    
    protected void  WriteTimeFormatLength(String pattern, String timeFormatLengthType)
    {
        //check inputs
        if ((pattern == null) || (timeFormatLengthType == null))
            return;
        
        String timeFormatLengtStr = "<" + LDMLConstants.TFL + " " + LDMLConstants.TYPE + " = \"" + timeFormatLengthType + "\">";
        println(timeFormatLengtStr);
        indent();
        println(LDMLConstants.TIME_FORMAT_O);
        indent();
        println(LDMLConstants.PATTERN_O + pattern + LDMLConstants.PATTERN_C);
        outdent();
        println(LDMLConstants.TIME_FORMAT_C);
        outdent();
        println(LDMLConstants.TFL_C);
    }
    
    protected void WritePattern(String pattern, String outerEl_o, String outerEl_c, String innerEl_o, String innerEl_c)
    {
        if ((pattern == null) || (outerEl_o==null) || (outerEl_c==null) || (innerEl_o==null) || (innerEl_c==null))
            return;
        
        println(outerEl_o);
        indent();
        println(innerEl_o);
        indent();
        println(LDMLConstants.PATTERN_O + pattern + LDMLConstants.PATTERN_C);
        outdent();
        println(innerEl_c);
        outdent();
        println(outerEl_c);
    }
    
    
    protected void writeSymbols(Hashtable data)
    {
        if ((data == null) || (data.size() == 0))
        {
            Logging.Log1("No symbols to write to LDML");
            return;
        }
        
        String decimal = (String) data.get(LDMLConstants.DECIMAL);
        String group = (String) data.get(LDMLConstants.GROUP);
        String list = (String) data.get(LDMLConstants.LIST);
        String percent = (String) data.get(LDMLConstants.PERCENT_SIGN);
        String zero = (String) data.get(LDMLConstants.NATIVE_ZERO_SIGN);
        String pattern = (String) data.get(LDMLConstants.PATTERN_DIGIT);
        String minus = (String) data.get(LDMLConstants.MINUS_SIGN);
        String exp = (String) data.get(LDMLConstants.EXPONENTIAL);
        String permille = (String) data.get(LDMLConstants.PER_MILLE);
        String infinity = (String) data.get(LDMLConstants.INFINITY);
        String nan = (String) data.get(LDMLConstants.NAN);
        
        println(LDMLConstants.SYMBOLS_O);
        indent();
        if (decimal != null) println(LDMLConstants.DECIMAL_O + decimal + LDMLConstants.DECIMAL_C);
        if (group != null) println(LDMLConstants.GROUP_O + group + LDMLConstants.GROUP_C);
        if (list != null) println(LDMLConstants.LIST_O + list + LDMLConstants.LIST_C);
        if (percent != null) println(LDMLConstants.PERCENT_SIGN_O + percent + LDMLConstants.PERCENT_SIGN_C);
        if (zero != null) println(LDMLConstants.NATIVE_ZERO_SIGN_O + zero + LDMLConstants.NATIVE_ZERO_SIGN_C);
        if (pattern != null) println(LDMLConstants.PATTERN_DIGIT_O + pattern + LDMLConstants.PATTERN_DIGIT_C);
        if (minus != null) println(LDMLConstants.MINUS_SIGN_O + minus + LDMLConstants.MINUS_SIGN_C);
        if (exp != null) println(LDMLConstants.EXPONENTIAL_O + exp + LDMLConstants.EXPONENTIAL_C);
        if (permille != null) println(LDMLConstants.PER_MILLE_O + permille + LDMLConstants.PER_MILLE_C);
        if (infinity != null) println(LDMLConstants.INFINITY_O + infinity + LDMLConstants.INFINITY_C);
        if (nan != null) println(LDMLConstants.NAN_O + nan + LDMLConstants.NAN_C);
        
        outdent();
        println(LDMLConstants.SYMBOLS_C);
    }
    
    protected void writeCurrency(Hashtable data)
    {
        if (data == null) return;
        
        String type = (String) data.get(LDMLConstants.TYPE);
        String symbol = (String) data.get(LDMLConstants.SYMBOL);
        String decimal = (String) data.get(LDMLConstants.DECIMAL);
        String psttern = (String) data.get(LDMLConstants.PATTERN);
        
        if (type != null)
            println("<" + LDMLConstants.CURRENCY + " " + LDMLConstants.TYPE + "=\"" + type + "\">");
        else
            println(LDMLConstants.CURRENCY_O);
        indent();
        
        if (psttern != null) println(LDMLConstants.PATTERN_O + psttern + LDMLConstants.PATTERN_C);
        if (symbol != null) println(LDMLConstants.SYMBOL_O + symbol + LDMLConstants.SYMBOL_C);
        if (decimal != null) println(LDMLConstants.DECIMAL_O + decimal + LDMLConstants.DECIMAL_C);
        
        outdent();
        println(LDMLConstants.CURRENCY_C);
    }
    
    
    //################# helper methods  ##########################33
    
    /** the Hashtable data object can be of several types
     *   tis object casts thhe data to String [], with exception of collation
     */
    protected String [] cast(final Object dataObject)
    {
        String [] retStr = null;
        if (dataObject == null)
            return retStr;
        
        if (dataObject instanceof String )
        {
            retStr = new String [1];
            retStr[0] = (String)dataObject;
        }
        else if (dataObject instanceof String [])
        {
            retStr =  (String []) dataObject;
        }
        else if  (dataObject instanceof String [][])
        {
            //type = pos 0, value  = pos 1, just copy te values
            int iLen = ((String[][])dataObject).length;
            retStr = new String [iLen];
            for (int i=0; i < iLen; i++)
            {
                retStr [i] = ((String [][]) dataObject) [i][1];
            }
        }
        return retStr;
    }
    
    //prints a namaespace tag
    protected void printNS(String namespace, String element, boolean isOpen)
    {
        if ((namespace==null) || (element==null))
            return;
        String start = "<";
        if (isOpen == false)
            start += "/";
        
        print(start + namespace + ":" + element + ">");
    }
    
    //prints a namaespace qualified tag with a carriage return
    protected void printlnNS(String namespace, String element, boolean isOpen)
    {
        if ((namespace==null) || (element==null))
            return;
        String start = "<";
        if (isOpen == false)
            start += "/";
        
        println(start + namespace + ":" + element + ">");
    }
    
    //prints a namespace qualified tag and attributes
    protected void printNS(String namespace, String element, Hashtable attribs, boolean bIsOpen, boolean bHasSubElementsOrData, boolean bNewLine)
    {
        if ((namespace==null) || (element==null) || (attribs==null))
            return;
        
        String start = "<";
        if (bIsOpen == false)
            start += "/";
        
        String str = "";
        Enumeration keys = attribs.keys();
        Enumeration data = attribs.elements();
        while (keys.hasMoreElements())
        {
            String key = (String) keys.nextElement();
            String datum = (String) data.nextElement();
            
            //prepend namespace name to attrib name except where attrib name is a std one like "default"
     //       if ((key.compareTo(LDMLConstants.DEFAULT)==0) || (key.compareTo(LDMLConstants.TYPE)==0))
     //           str = str + " " + key + "=\"" + datum +"\"";
     //       else
                str = str + " " + namespace + ":" + key + "=\"" + datum +"\"";
        }
        
        if (bHasSubElementsOrData == false)
            str += "/>";
        else
            str += ">";
        
        if (bNewLine == true)
            println(start + namespace + ":" + element + str);
        else
            print(start + namespace + ":" + element + str);
    }
    
    
    //prints special element with xmlns and namespace qualified tag and attributes
    protected void printSpecial(String namespace, String www, Hashtable attribs, boolean bHasSubElementsOrData, boolean bNewLine)
    {
        if ((namespace == null) || (www == null) || (attribs == null))
            return;
        
        String start = "<special xmlns:" + namespace + "=\"" + www + "\"";
        
        String str = "";
        Enumeration keys = attribs.keys();
        Enumeration data = attribs.elements();
        while (keys.hasMoreElements())
        {
            String key = (String) keys.nextElement();
            String datum = (String) data.nextElement();
            
            //prepend namespace name to attrib name except where attrib name is a std one like "default"
      //      if ((key.compareTo(LDMLConstants.DEFAULT)==0) || (key.compareTo(LDMLConstants.TYPE)==0))
      //          str = str + " " + key + "=\"" + datum +"\"";
      //      else
                str = str + " " + namespace + ":" + key + "=\"" + datum +"\"";
        }
        
        if (bHasSubElementsOrData == false)
            str += "/>";
        else
            str += ">";
        
        if (bNewLine == true)
            println(start + str);
        else
            print(start + str);
    }
    
    
    
    //prints a tag and attributes
    protected void print(String element, Hashtable attribs, boolean bIsOpen, boolean bHasSubElementsOrData, boolean bNewLine)
    {
        if ((element==null) || (attribs==null))
            return;
        
        String start = "<";
        if (bIsOpen == false)
            start += "/";
        
        String str = "";
        Enumeration keys = attribs.keys();
        Enumeration data = attribs.elements();
        while (keys.hasMoreElements())
        {
            String key = (String) keys.nextElement();
            String datum = (String) data.nextElement();
            str = str + " " + key + "=\"" + datum +"\"";
        }
        
        if (bHasSubElementsOrData == false)
            str += "/>";
        else
            str += ">";
        
        if (bNewLine == true)
            println(start + element + str);
        else
            print(start + element + str);
    }
    
    //these currencies should be written under currencies element not currencyFormat
    protected void getLegacyCurrencies()
    {
        m_LegacyCurrencies.add("ATS");
        m_LegacyCurrencies.add("BEF");
        m_LegacyCurrencies.add("FIM");
        m_LegacyCurrencies.add("FRF");
        m_LegacyCurrencies.add("DEM");
        m_LegacyCurrencies.add("GRD");
        m_LegacyCurrencies.add("IEP");
        m_LegacyCurrencies.add("ITL");
        m_LegacyCurrencies.add("LUF");
        m_LegacyCurrencies.add("NLG");
        m_LegacyCurrencies.add("PTE");
        m_LegacyCurrencies.add("ESP");
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

