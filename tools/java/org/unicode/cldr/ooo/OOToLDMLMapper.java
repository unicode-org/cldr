/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.unicode.cldr.icu.LDMLConstants;
/**
 *
 * class has static methods to map in memory OO data to in memory LDML data
 * thereby decoupling the data from OO
 */

public class OOToLDMLMapper
{
    /* input is Vector of Hashtables
     * where (key= calendar attr name , value = calendar attr value)
     * output = vector list of calendars with last entry in vector = position of default calendar
     */
    public static Vector MapCalendar(Vector OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Vector LDMLData = new Vector();
        String defaultPos = "-1";
        for (int i=0; i < OOData.size(); i++)
        {
            Hashtable attributes = (Hashtable) OOData.elementAt(i);
            String deflt = (String) attributes.get(OOConstants.DEFAULT);
            if ((deflt != null) && (deflt.compareTo(OOConstants.TRUE)==0))
            {
                defaultPos = Integer.toString(i);
            }
            
            String type = (String) attributes.get(OOConstants.UNOID);
            String calTypeLDML = MapCalendarType(type);
            if (calTypeLDML != null)
                LDMLData.add(calTypeLDML);
        }
        
        if (defaultPos.compareTo("-1")!=0)
            LDMLData.add((Object)defaultPos);
        return LDMLData;
    }
    
    
        /* input (and output) = Hashtable of Hasdhtables, outer key = calendar type (i.e. gregoirian) , inner key = day type (i.e. sun)
         * OO uses OOConstants.SUN -> OOConstants.SAT to identify days
         *  LDML uses LDMLConstants.SUN -> LDMLConstants.SAT
         */
    public static Hashtable MapDays(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData = new Hashtable();
        
        Enumeration enDays = OOData.elements();
        Enumeration enCalType = OOData.keys();
        while (enDays.hasMoreElements() == true)
        {
            Hashtable innerLDMLData = new Hashtable();
            Hashtable OODataI = (Hashtable) enDays.nextElement();
            String calTypeI = (String) enCalType.nextElement();
            String calTypeLDML = MapCalendarType(calTypeI);
            
            String day = (String) OODataI.get((Object) OOConstants.SUN);
            if (day != null) innerLDMLData.put(LDMLConstants.SUN, day);
            
            day = (String) OODataI.get((Object) OOConstants.MON);
            if (day != null) innerLDMLData.put(LDMLConstants.MON, day);
            
            day = (String) OODataI.get((Object) OOConstants.TUE);
            if (day != null) innerLDMLData.put(LDMLConstants.TUE, day);
            
            day = (String) OODataI.get((Object) OOConstants.WED);
            if (day != null) innerLDMLData.put(LDMLConstants.WED, day);
            
            day = (String) OODataI.get((Object) OOConstants.THU);
            if (day != null) innerLDMLData.put(LDMLConstants.THU, day);
            
            day = (String) OODataI.get((Object) OOConstants.FRI);
            if (day != null) innerLDMLData.put(LDMLConstants.FRI, day);
            
            day = (String) OODataI.get((Object) OOConstants.SAT);
            if (day != null) innerLDMLData.put(LDMLConstants.SAT, day);
            
            if ((calTypeLDML != null) &&  (innerLDMLData.size() >0))
                LDMLData.put( calTypeLDML, innerLDMLData);
        }
        return LDMLData;
        
        
    }
    
        /*  output = Hashtable of Hasdhtables, outer key = calendar type (always gregoirian) , inner key = quarter type (1 to 4)
         *  LDML uses LDMLConstants.Q_1 -> Q_4 to identify 
         */
    public static Hashtable MapWideQuarters(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData = new Hashtable();
        Hashtable innerLDMLData = new Hashtable();
        
        String q1Word = (String) OOData.get(OpenOfficeLDMLConstants.QUARTER_1_WORD);
        if (q1Word != null) innerLDMLData.put(LDMLConstants.Q_1, q1Word);
        String q2Word = (String) OOData.get(OpenOfficeLDMLConstants.QUARTER_2_WORD);
        if (q2Word != null) innerLDMLData.put(LDMLConstants.Q_2, q2Word);
        String q3Word = (String) OOData.get(OpenOfficeLDMLConstants.QUARTER_3_WORD);
        if (q3Word != null) innerLDMLData.put(LDMLConstants.Q_3, q3Word);
        String q4Word = (String) OOData.get(OpenOfficeLDMLConstants.QUARTER_4_WORD);
        if (q4Word != null) innerLDMLData.put(LDMLConstants.Q_4, q4Word);

        if (innerLDMLData.size() >0)
            LDMLData.put( LDMLConstants.GREGORIAN, innerLDMLData);   //OOO data is cal independent
        return LDMLData;   
    }
    
    
           /*  output = Hashtable of Hasdhtables, outer key = calendar type (always gregoirian) , inner key = quarter type (1 to 4)
         *  LDML uses LDMLConstants.Q_1 -> Q_4 to identify 
         */
    public static Hashtable MapAbbrQuarters(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData = new Hashtable();
        Hashtable innerLDMLData = new Hashtable();
        
        String q1Abbr = (String) OOData.get(OpenOfficeLDMLConstants.QUARTER_1_ABBREVIATION);
        if (q1Abbr != null) innerLDMLData.put(LDMLConstants.Q_1, q1Abbr);
        String q2Abbr = (String) OOData.get(OpenOfficeLDMLConstants.QUARTER_2_ABBREVIATION);
        if (q2Abbr != null) innerLDMLData.put(LDMLConstants.Q_2, q2Abbr);
        String q3Abbr = (String) OOData.get(OpenOfficeLDMLConstants.QUARTER_3_ABBREVIATION);
        if (q3Abbr != null) innerLDMLData.put(LDMLConstants.Q_3, q3Abbr);
        String q4Abbr = (String) OOData.get(OpenOfficeLDMLConstants.QUARTER_4_ABBREVIATION);
        if (q4Abbr != null) innerLDMLData.put(LDMLConstants.Q_4, q4Abbr);

        if (innerLDMLData.size() >0)
            LDMLData.put( LDMLConstants.GREGORIAN, innerLDMLData);   //OOO data is cal independent
        
        return LDMLData;   
    }
    
  /* input (and output) = Hashtable of Hasdhtables, outer key = calendar type (i.e. gregoirian) , inner key = month type (i.e. jan or 1)
   * OO uses OOConstants.JAN -> OOConstants.DEC to identify months
   *  LDML uses LDMLConstants.MONTH_1 -> LDMLConstants.MONTH_13
   */
    public static Hashtable MapMonths(Hashtable OOData)
    {
        if ((OOData == null)  || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData = new Hashtable();
        
        Enumeration enMonths = OOData.elements();
        Enumeration enCalType = OOData.keys();
        while (enMonths.hasMoreElements() == true)
        {
            Hashtable innerLDMLData = new Hashtable();
            Hashtable OODataI = (Hashtable) enMonths.nextElement();
            String calTypeI = (String) enCalType.nextElement();
            String calTypeLDML = MapCalendarType(calTypeI);
            
            String month = (String) OODataI.get((Object) OOConstants.MONTH_1);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_1_ALT);   //he_IL.xml jewish calendar
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_1, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_2);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_2_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_2, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_3);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_3_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_3, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_4);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_4_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_4, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_5);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_5_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_5, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_6);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_6_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_6, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_7);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_7_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_7, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_8);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_8_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_8, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_9);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_9_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_9, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_10);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_10_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_10, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_11);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_11_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_11, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_12);
            if (month == null) month = (String) OODataI.get((Object) OOConstants.MONTH_12_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_12, month);
            
            month = (String) OODataI.get((Object) OOConstants.MONTH_13_ALT);
            if (month != null) innerLDMLData.put(LDMLConstants.MONTH_13, month);
            
            if ((calTypeLDML != null) && (innerLDMLData.size() >0))
                LDMLData.put( calTypeLDML, innerLDMLData);
        }
        return LDMLData;
    }
    
   /* input (and output) = Hashtable of Hasdhtables, outer key = calendar type (i.e. gregorian) , inner key = era type (i.e. bc or 0)
    * OO uses OOConstants.BC etc to identify eras
    *  LDML uses LDMLConstants.ERA_0 -> LDMLConstants.ERA_235 (ja)
    */
    public static Hashtable MapEras(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData = new Hashtable();
        
        Enumeration enMonths = OOData.elements();
        Enumeration enCalType = OOData.keys();
        while (enMonths.hasMoreElements() == true)
        {
            Hashtable innerLDMLData = new Hashtable();
            Hashtable OODataI = (Hashtable) enMonths.nextElement();
            String calTypeI = (String) enCalType.nextElement();
            String calTypeLDML = MapCalendarType(calTypeI);
            
            //go through all possible OO calendar types
            if (calTypeI.compareTo(OOConstants.GREGORIAN)==0)
            {
                String era = (String) OODataI.get((Object) OOConstants.BC);
                if (era != null)
                    innerLDMLData.put(LDMLConstants.ERA_0, era);
                else
                {  //for rw_RW
                    era = (String) OODataI.get((Object) "BC");
                    if (era != null)
                        innerLDMLData.put(LDMLConstants.ERA_0, era);
                }
                
                era = (String) OODataI.get((Object) OOConstants.AD);
                if (era != null)
                    innerLDMLData.put(LDMLConstants.ERA_1, era);
                else
                {
                    era = (String) OODataI.get((Object) "AD");
                    if (era != null)
                        innerLDMLData.put(LDMLConstants.ERA_1, era);
                }
                
            }
            else if (calTypeI.compareTo(OOConstants.HANJA)==0)
            {
                String era = (String) OODataI.get((Object) OOConstants.BC);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_0, era);
                era = (String) OODataI.get((Object) OOConstants.AD);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_1, era);
            }
            else if (calTypeI.compareTo(OOConstants.HIJRI)==0)
            {
                String era = (String) OODataI.get((Object) OOConstants.BEFORE_HIJRA);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_0, era);
                era = (String) OODataI.get((Object) OOConstants.AFTER_HIJRA);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_1, era);
            }
            else if (calTypeI.compareTo(OOConstants.JEWISH)==0)
            {
                String era = (String) OODataI.get((Object) OOConstants.BEFORE);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_0, era);
                era = (String) OODataI.get((Object) OOConstants.AFTER);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_1, era);
            }
            else if (calTypeI.compareTo(OOConstants.BUDDHIST)==0)
            {
                String era = (String) OODataI.get((Object) OOConstants.BEFORE);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_0, era);
                era = (String) OODataI.get((Object) OOConstants.AFTER);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_1, era);
            }
            else if (calTypeI.compareTo(OOConstants.ROC)==0)
            {
                String era = (String) OODataI.get((Object) OOConstants.BEFORE_ROC);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_0, era);
                era = (String) OODataI.get((Object) OOConstants.MINGUO);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_1, era);
            }
            else if (calTypeI.compareTo(OOConstants.GENGOU)==0)
            {
                String era = (String) OODataI.get((Object) OOConstants.DUMMY);
                if (era != null) innerLDMLData.put("-1", era);  //workaround, no such thing as dummy era
                era = (String) OODataI.get((Object) OOConstants.MEIJI);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_232, era);
                era = (String) OODataI.get((Object) OOConstants.TAISHO);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_233, era);
                era = (String) OODataI.get((Object) OOConstants.SHOWA);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_234, era);
                era = (String) OODataI.get((Object) OOConstants.HEISEI);
                if (era != null) innerLDMLData.put(LDMLConstants.ERA_235, era);
            }
            
            if ((calTypeLDML != null) && (innerLDMLData.size() >0))
                LDMLData.put( calTypeLDML, innerLDMLData);
        }
        return LDMLData;
    }
    
    
    /* input and output Hashtables of the form : key = calendar type, value = start day of week
     *
     */
    public static Hashtable MapStartDayOfWeek(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData = new Hashtable();
        
        Enumeration enDays = OOData.elements();
        Enumeration enCalType = OOData.keys();
        while (enDays.hasMoreElements() == true)
        {
            String calTypeI = (String) enCalType.nextElement();
            String calTypeLDML = MapCalendarType(calTypeI);
            
            String day = (String) enDays.nextElement();
            
            if (day.compareTo(OOConstants.SUN)==0)
                LDMLData.put(calTypeLDML, LDMLConstants.SUN);
            else if (day.compareTo(OOConstants.MON)==0)
                LDMLData.put(calTypeLDML, LDMLConstants.MON);
            else if (day.compareTo(OOConstants.TUE)==0)
                LDMLData.put(calTypeLDML, LDMLConstants.TUE);
            else if (day.compareTo(OOConstants.WED)==0)
                LDMLData.put(calTypeLDML, LDMLConstants.WED);
            else if (day.compareTo(OOConstants.THU)==0)
                LDMLData.put(calTypeLDML, LDMLConstants.THU);
            else if (day.compareTo(OOConstants.FRI)==0)
                LDMLData.put(calTypeLDML, LDMLConstants.FRI);
            else if (day.compareTo(OOConstants.SAT)==0)
                LDMLData.put(calTypeLDML, LDMLConstants.SAT);
        }
        return LDMLData;
    }
    
    /* input and output Hashtables of the form : key = calendar type, value = a number
     *  the calendar types must be mapped from OO types to LDML types
     */
    public static Hashtable MapMinDaysInFirstWeek(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData = new Hashtable();
        
        Enumeration enDays = OOData.elements();
        Enumeration enCalType = OOData.keys();
        while (enDays.hasMoreElements() == true)
        {
            String calTypeI = (String) enCalType.nextElement();
            String calTypeLDML = MapCalendarType(calTypeI);
            
            String numDays = (String) enDays.nextElement();
            LDMLData.put(calTypeLDML, numDays);
        }
        return LDMLData;
    }
    
    private static String MapCalendarType(String OOCalendar)
    {
        if (OOCalendar == null)
            return null;
        
        String LDMLCalendar = null;
        
        if (OOCalendar.compareTo(OOConstants.GREGORIAN)==0)
        {
            LDMLCalendar = LDMLConstants.GREGORIAN;
        }
        else if (OOCalendar.compareTo(OOConstants.HANJA)==0)
        {
            LDMLCalendar = OOConstants.HANJA;  //not sure what "Hanja" calendar is ??
        }
        else if (OOCalendar.compareTo(OOConstants.HIJRI)==0)
        {
            LDMLCalendar = LDMLConstants.ISLAMIC_CIVIL;  //not sure if this is islamic or islamic-civil
        }
        else if (OOCalendar.compareTo(OOConstants.JEWISH)==0)
        {
            LDMLCalendar = LDMLConstants.HEBREW;
        }
        else if (OOCalendar.compareTo(OOConstants.BUDDHIST)==0)
        {
            LDMLCalendar = LDMLConstants.BUDDHIST;
        }
        else if (OOCalendar.compareTo(OOConstants.ROC)==0)
        {
            LDMLCalendar = LDMLConstants.CHINESE;
        }
        else if (OOCalendar.compareTo(OOConstants.GENGOU)==0)
        {
            LDMLCalendar = LDMLConstants.JAPANESE;
        }
        
        return LDMLCalendar;
    }
    
    
    public static String MapMeasurementSystem(String OO_MS)
    {
        if (OO_MS == null)
            return null;
        
        String LDML_MS = null;
        
        if ((OO_MS.compareTo(OOConstants.METRIC_1)==0)
        || (OO_MS.compareTo(OOConstants.METRIC_2)==0))
        {
            LDML_MS = LDMLConstants.METRIC;
        }
        else if (OO_MS.compareTo(OOConstants.US)==0)
        {
            LDML_MS = LDMLConstants.US;
        }
        return LDML_MS;
    }
    
    //currencies held in Hashtable of Hashtables
    // method finds default currency and adds it to end of returned Hashtable (key=LDMLConstants.DEFAULT, key = intlCurrCode)
    public static Hashtable MapCurrency(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
   //     String defaultCurr = null;
        
  /*      Enumeration keys = OOData.keys();
        Enumeration data = OOData.elements();
        while (keys.hasMoreElements()==true)
        {
            String intlCurrCode = (String) keys.nextElement();
            Hashtable inner = (Hashtable) data.nextElement();
            System.err.println (inner.size());
            
            String deflt = (String) inner.get(OOConstants.DEFAULT);
            if ((deflt != null) && (deflt.compareTo(OOConstants.TRUE)==0))
            {
                defaultCurr = intlCurrCode;
                break;
            }
        }*/
        Hashtable currency = MapFirstCharToLowerCase2(OOData);  //make attrib names lower case
  //      if (defaultCurr != null)
 //           currency.put(LDMLConstants.DEFAULT, defaultCurr);
        return currency;
    }
    
    //symbols held in Hashtable, key = OOConstant element name, value = element value
    //return same values but mapped to LDMLConstants
    public static Hashtable MapSymbols(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData = new Hashtable();
               
        String thousandSeparator = (String) OOData.get( OOConstants.THOUSAND_SEPARATOR);
        if (thousandSeparator != null) LDMLData.put(LDMLConstants.GROUP, thousandSeparator);
        
        String decimalSeparator = (String) OOData.get( OOConstants.DECIMAL_SEPARATOR);
        if (decimalSeparator != null) LDMLData.put(LDMLConstants.DECIMAL, decimalSeparator);
               
        String listSeparator = (String) OOData.get( OOConstants.LIST_SEPARATOR);
        if (listSeparator != null) LDMLData.put(LDMLConstants.LIST, listSeparator);
               
        return LDMLData;
    }
    
     //symbols held in Hashtable, key = OOConstant element name, value = element value
    //return same values but mapped to LDMLConstants
    public static Hashtable MapOOSymbols(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData = new Hashtable();
        
        String dateSeparator = (String) OOData.get(OOConstants.DATE_SEPARATOR);
        if (dateSeparator != null) LDMLData.put(OpenOfficeLDMLConstants.DATE_SEPARATOR, dateSeparator);
             
        String timeSeparator = (String) OOData.get( OOConstants.TIME_SEPARATOR);
        if (timeSeparator != null) LDMLData.put(OpenOfficeLDMLConstants.TIME_SEPARATOR, timeSeparator);
        
        String time100SecSeparator = (String) OOData.get( OOConstants.TIME_100SEC_SEPARATOR);
        if (time100SecSeparator != null) LDMLData.put(OpenOfficeLDMLConstants.TIME_100SEC_SEPARATOR, time100SecSeparator);
               
        String longDateDayOfWeekSeparator = (String) OOData.get( OOConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR);
        if (longDateDayOfWeekSeparator != null) LDMLData.put(OpenOfficeLDMLConstants.LONG_DATE_DAY_OF_WEEK_SEPARATOR, longDateDayOfWeekSeparator);
        
        String longDateDaySeparator = (String) OOData.get( OOConstants.LONG_DATE_DAY_SEPARATOR);
        if (longDateDaySeparator != null) LDMLData.put(OpenOfficeLDMLConstants.LONG_DATE_DAY_SEPARATOR, longDateDaySeparator);
        
        String longDateMnothSeparator = (String) OOData.get( OOConstants.LONG_DATE_MONTH_SEPARATOR);
        if (longDateMnothSeparator != null) LDMLData.put(OpenOfficeLDMLConstants.LONG_DATE_MONTH_SEPARATOR, longDateMnothSeparator);
        
        String longDateYearSeparator = (String) OOData.get( OOConstants.LONG_DATE_YEAR_SEPARATOR);
        if (longDateYearSeparator != null) LDMLData.put(OpenOfficeLDMLConstants.LONG_DATE_YEAR_SEPARATOR, longDateYearSeparator);
        
        return LDMLData;
    }
    
    //delimiters held in Hashtable, key = OOConstant element name, value = element value
    //return same values but mapped to LDMLConstants
    public static Hashtable MapDelimiters(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData = new Hashtable();
        
        String quotationStart = (String) OOData.get(OOConstants.QUOTATION_START);
        if (quotationStart!=null) LDMLData.put(LDMLConstants.QS, quotationStart);
        
        String quotationEnd = (String) OOData.get(OOConstants.QUOTATION_END);
        if (quotationEnd!=null) LDMLData.put(LDMLConstants.QE, quotationEnd);
        
        String doubleQuotationStart = (String) OOData.get(OOConstants.DOUBLE_QUOTATION_START);
        if (doubleQuotationStart!=null) LDMLData.put(LDMLConstants.AQS, doubleQuotationStart);
        
        String doubleQuotationEnd = (String) OOData.get(OOConstants.DOUBLE_QUOTATION_END);
        if (doubleQuotationEnd!=null) LDMLData.put(LDMLConstants.AQE, doubleQuotationEnd);
        
        return LDMLData;
    }
    
    
    //copy only the data matching fmtElementUsage (DATE, TIME etc) to outout Hashtables
    // if bConvertDateTime == true then OO date and time format is converted to be LDML compliant else not converted
    public static void MapFormatElements( Hashtable inFormatElements,
    Hashtable inFormatCodes,
    Hashtable inFormatDefaultNames,
    Hashtable outFormatElements,
    Hashtable outFormatCodes,   //for OO.o Format Codes
    Hashtable outFormatDefaultNames,
    String fmtElementUsage, String localeStr /*i.e. it_IT*/, boolean bConvertDateTime, Hashtable outFlexDateTime)  
    {
        if ((inFormatElements==null) || (inFormatCodes==null) || (inFormatDefaultNames==null))
            return;
        
        String defaultKey = null;
        String type = null;
        
        Hashtable LDMLData_outer = new Hashtable();
        Enumeration data = inFormatElements.elements();
        Enumeration keys = inFormatElements.keys();
        
        while (keys.hasMoreElements() == true)
        {
            boolean bMatchedMsgId = false;
            boolean bIsDefault = false;
            
            Hashtable LDMLData_inner = new Hashtable();
            String key = (String) keys.nextElement();
            Hashtable inner = (Hashtable) data.nextElement();
            
            Enumeration keys_inner = inner.keys();
            Enumeration data_inner = inner.elements();
            while (keys_inner.hasMoreElements() == true)
            {
                String key_inner = (String) keys_inner.nextElement();
                String datum_inner = (String) data_inner.nextElement();
                
                if ((key_inner.compareTo(OOConstants.DEFAULT)==0)
                && (datum_inner.compareTo(OOConstants.TRUE)==0))
                    bIsDefault = true;
                
                if ((key_inner.compareTo(OOConstants.USAGE)==0)
                && (datum_inner.compareTo(fmtElementUsage)==0))
                    bMatchedMsgId = true;
                
                if (key_inner.compareTo(LDMLConstants.TYPE)==0)
                    type = datum_inner;
                
                //most ldmlOpenOffice.dtd attributes differ from oo.dtd ones by the case of first letter only
                // except for oo.dtd's msgid which maps to msgtype in LDML
                String ldml_key = null;
                if (key_inner.compareTo(OOConstants.MSGID)==0)
                    ldml_key = OpenOfficeLDMLConstants.MSGTYPE;
                else
                    ldml_key = makeFirstCharLowerCase(key_inner);
                
                LDMLData_inner.put(ldml_key, datum_inner);
            }
            
            //only pass back data with matching msgId
            if (bMatchedMsgId == true)
            {
                if (bIsDefault == true)
                    defaultKey = key;
                outFormatElements.put(key, LDMLData_inner);
                
                String fmtCode = (String) inFormatCodes.get(key);
                //map the OO pattern to LDML compliant pattern
                String LDMLPattern  = null;   //for flex date and time
                String outPattern = null;   //for OO.o FormatCode
                
                OOToLDMLPattern mapper = new OOToLDMLPattern(localeStr);
                
                //temp for diagnostics
                String buff = "";
                for (int ii=0; ii < (50-fmtCode.length());ii++)
                    buff += " ";
                
                if (fmtElementUsage.compareTo(OOConstants.FEU_DATE)==0)
                {
                    LDMLPattern  = mapper.map(fmtCode, OOConstants.FEU_DATE);
                    if (LDMLPattern != null)
                        outFlexDateTime.put(key, LDMLPattern);
                 //   System.err.println (fmtCode + "\t\t" + LDMLPattern );
                    try
                    {
                        BufferedWriter out = new BufferedWriter(new FileWriter("date_time",true));
                        if (LDMLPattern.indexOf('[')== -1) 
                            out.write(fmtCode + buff + " " + LDMLPattern +"\n");
                        else
                            out.write(fmtCode + buff + " " + "SKIPPED" +"\n");
                        out.close();
                    }
                    catch (IOException e)
                    {} 
                }
                else if (fmtElementUsage.compareTo(OOConstants.FEU_TIME)==0)
                {
                    LDMLPattern  = mapper.map(fmtCode, OOConstants.FEU_TIME);
                    if (LDMLPattern != null) outFlexDateTime.put(key, LDMLPattern);
               //     System.err.println (fmtCode + "\t\t" + LDMLPattern );
                    try
                    {
                        BufferedWriter out = new BufferedWriter(new FileWriter("date_time",true));
                       if (LDMLPattern.indexOf('[')== -1) 
                            out.write(fmtCode + buff + " " + LDMLPattern +"\n");
                        else
                            out.write(fmtCode + buff + " " + "SKIPPED" +"\n");
                        out.close();
                    }
                    catch (IOException e)
                    {}
                }
                else if (fmtElementUsage.compareTo(OOConstants.FEU_DATE_TIME)==0)
                {
                    LDMLPattern  = mapper.map(fmtCode, OOConstants.FEU_DATE_TIME);
                    if (LDMLPattern != null) outFlexDateTime.put(key, LDMLPattern);
               //     System.err.println (fmtCode + "\t\t" + LDMLPattern );
                    try
                    {
                        BufferedWriter out = new BufferedWriter(new FileWriter("date_time",true));
                       if (LDMLPattern.indexOf('[')== -1) 
                            out.write(fmtCode + buff + " " + LDMLPattern +"\n");
                        else
                            out.write(fmtCode + buff + " " + "SKIPPED" +"\n");
                        out.close();
                    }
                    catch (IOException e)
                    {}
                }
                else
                    LDMLPattern = fmtCode;   //currency, percentage, numbers etc
                   
                if (bConvertDateTime == true)
                    outPattern = LDMLPattern;
                else //we choose not to write data in LDML format
                    outPattern = fmtCode;
                
                if (outPattern != null)
                    outFormatCodes.put(key, outPattern);
                else
                    System.err.println ("WARNING : pattern " + fmtCode + " not written");
                
                
                String fmtName = (String) inFormatDefaultNames.get(key);
                if (fmtName != null) outFormatDefaultNames.put(key, fmtName);
                
                if ((defaultKey != null) && (type != null))
                    outFormatElements.put(LDMLConstants.DEFAULT + " " + type, defaultKey);
            }
        }
    }
    
    
    
    /* most OO.DTD elements differ from their corresponding ldmlOpenOffice.dtd elements by case of first letter only
     * input is Hashtable of Strings
     */
    public static Hashtable MapFirstCharToLowerCase(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData = new Hashtable();
        Enumeration data = OOData.elements();
        Enumeration keys = OOData.keys();
        while (keys.hasMoreElements() == true)
        {
            String key = (String) keys.nextElement();
            String datum = (String) data.nextElement();
            if (key != null)
            {
                String key_lowerCase = makeFirstCharLowerCase(key);
                LDMLData.put(key_lowerCase, datum);
            }
        }
        return LDMLData;
    }
    
    /* most OO.DTD elements differ from their corresponding ldmlOpenOffice.dtd elements by case of first letter only
     *  input is Hashtable of Hashtables
     */
    public static Hashtable MapFirstCharToLowerCase2(Hashtable OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Hashtable LDMLData_outer = new Hashtable();
        Enumeration data = OOData.elements();
        Enumeration keys = OOData.keys();
        while (keys.hasMoreElements() == true)
        {
            Hashtable LDMLData_inner = new Hashtable();
            String key = (String) keys.nextElement();
            Hashtable inner = (Hashtable) data.nextElement();
            
            Enumeration keys_inner = inner.keys();
            Enumeration data_inner = inner.elements();
            while (keys_inner.hasMoreElements() == true)
            {
                String key_inner = (String) keys_inner.nextElement();
                String datum_inner = (String) data_inner.nextElement();
                String key_lowerCase = makeFirstCharLowerCase(key_inner);
                LDMLData_inner.put(key_lowerCase, datum_inner);
            }
            LDMLData_outer.put(key, LDMLData_inner);
        }
        return LDMLData_outer;
    }
    
    // operates on Vector of Hashtables, converting the Haahtable keys' first char to lower case
    public static Vector MapFirstCharToLowerCase(Vector OOData)
    {
        if ((OOData == null) || (OOData.size()==0))
            return null;
        
        Vector LDMLData = new Vector();
        for (int i=0; i < OOData.size(); i++)
        {
            Hashtable inner = new Hashtable();
            Enumeration data = ((Hashtable)(OOData.elementAt(i))).elements();
            Enumeration keys = ((Hashtable)(OOData.elementAt(i))).keys();
            while (keys.hasMoreElements() == true)
            {
                String key = (String) keys.nextElement();
                String datum = (String) data.nextElement();
                if (key != null)
                {
                    String key_lowerCase = makeFirstCharLowerCase(key);
                    inner.put(key_lowerCase, datum);
                }
            }
            LDMLData.add(inner);
        }
        return LDMLData;
    }
    
    public static Hashtable MapRefsToAlias(String filename, Hashtable refs)
    {
        if ((refs == null) || (refs.size()==0))
            return null;
        
        Hashtable Aliases = new Hashtable();
        Vector localeAndType = null;  //pos 0 = locale, pos 1 = type (if any)
        
        String ref = (String) refs.get(OOConstants.LC_FORMAT);
        if (ref != null)
        {
            localeAndType = getLocaleAndType(filename, ref);
            //TODO finish ....
        }
        
        ref = (String) refs.get(OOConstants.LC_CALENDAR);
        localeAndType = getLocaleAndType(filename, ref);
        if (ref != null)
        {
            localeAndType = getLocaleAndType(filename, ref);
            if (localeAndType != null) Aliases.put(LDMLConstants.CALENDARS, localeAndType);
        }
        
        Hashtable refTable = (Hashtable) refs.get(OOConstants.DAYS_OF_WEEK);
        if (refTable != null)
        {
            //refTable : key = Calendar type, data = ref
            Hashtable alias = new Hashtable();
            Enumeration keys = refTable.keys();
            Enumeration data = refTable.elements();
            while (keys.hasMoreElements()==true)
            {
                String LDMLCal = MapCalendarType( (String)keys.nextElement());
                localeAndType = getLocaleAndType(filename, ref);
                if (localeAndType != null) alias.put(LDMLCal, localeAndType);
            }
            Aliases.put(LDMLConstants.DAYS, alias);
        }
        
        refTable = (Hashtable) refs.get(OOConstants.MONTHS_OF_YEAR);
        if (refTable != null)
        {
            //refTable : key = Calendar type, data = ref
            Hashtable alias = new Hashtable();
            Enumeration keys = refTable.keys();
            Enumeration data = refTable.elements();
            while (keys.hasMoreElements()==true)
            {
                String LDMLCal = MapCalendarType( (String)keys.nextElement());
                localeAndType = getLocaleAndType(filename, ref);
                if (localeAndType != null) alias.put(LDMLCal, localeAndType);
            }
            Aliases.put(LDMLConstants.MONTHS, alias);
        }
        
        refTable = (Hashtable) refs.get(OOConstants.ERAS);
        if (refTable != null)
        {
            //refTable : key = Calendar type, data = ref
            Hashtable alias = new Hashtable();
            Enumeration keys = refTable.keys();
            Enumeration data = refTable.elements();
            while (keys.hasMoreElements()==true)
            {
                String LDMLCal = MapCalendarType( (String)keys.nextElement());
                localeAndType = getLocaleAndType(filename, ref);
                if (localeAndType != null) alias.put(LDMLCal, localeAndType);
            }
            Aliases.put(LDMLConstants.ERAS, alias);
        }
        
        ref = (String) refs.get(OOConstants.LC_CURRENCY);
        if (ref != null)
        {
            localeAndType = getLocaleAndType(filename, ref);
            if (localeAndType != null)
            {
                Aliases.put(OpenOfficeLDMLConstants.CURRENCY, localeAndType);
                Aliases.put(LDMLConstants.CURRENCIES, localeAndType);
            }
        }
        
        ref = (String) refs.get(OOConstants.LC_COLLATION);
        if (ref != null)
        {
            localeAndType = getLocaleAndType(filename, ref);
            if (localeAndType != null) Aliases.put(OpenOfficeLDMLConstants.COLLATIONS, localeAndType);
        }
        
        ref = (String) refs.get(OOConstants.LC_SEARCH);
        if (ref != null)
        {
            localeAndType = getLocaleAndType(filename, ref);
            if (localeAndType != null) Aliases.put(OpenOfficeLDMLConstants.SEARCH, localeAndType);
        }
        
        ref = (String) refs.get(OOConstants.LC_INDEX);
        if (ref != null)
        {
            localeAndType = getLocaleAndType(filename, ref);
            if (localeAndType != null) Aliases.put(OpenOfficeLDMLConstants.INDEX, localeAndType);
        }
        
        ref = (String) refs.get(OOConstants.LC_TRANSLITERATION);
        if (ref != null)
        {
            localeAndType = getLocaleAndType(filename, ref);
            if (localeAndType != null) Aliases.put(OpenOfficeLDMLConstants.TRANSLITERATIONS, localeAndType);
        }
        
        ref = (String) refs.get(OOConstants.LC_MISC);
        if (ref != null)
        {
            localeAndType = getLocaleAndType(filename, ref);
            if (localeAndType != null)
            {
                Aliases.put(OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS, localeAndType);
                Aliases.put(OpenOfficeLDMLConstants.RESERVED_WORDS, localeAndType);
            }
        }
        
        ref = (String) refs.get(OOConstants.LC_NUMBERING_LEVEL);
        if (ref != null)
        {
            localeAndType = getLocaleAndType(filename, ref);
            if (localeAndType != null) Aliases.put(OpenOfficeLDMLConstants.NUMBERING_LEVELS, localeAndType);
        }
        
        ref = (String) refs.get(OOConstants.LC_OUTLINE_NUMBERING_LEVEL);
        if (ref != null)
        {
            localeAndType = getLocaleAndType(filename, ref);
            if (localeAndType != null) Aliases.put(OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVELS, localeAndType);
        }
        return Aliases;
    }
    
    /* method takes OO ref as input and returns Vector where pos 0 = locale, pso 1 = type (if any
     * OO refs of type locale_type i.e. zh_CN_gregorian
     */
    public static Vector getLocaleAndType(String filename, String reference)
    {
        //reference can be of type :
        // 1 locale_type    i.e. "en_US_gregorian"
        // 2 locale         i.e. "en_US"
        // 3 type           i.e. "gregorian"
        // NB there are no known refs of type language_type i.e. en_gregorian so theis case is not handled here
        
        if (reference == null) //no reference so no processing needed
            return null;
        
        Vector vect = new Vector();
        
        //check does ref contain only type :
        if ((reference.compareTo(OOConstants.GREGORIAN)==0)
        || (reference.compareTo(OOConstants.HIJRI)==0)
        || (reference.compareTo(OOConstants.JEWISH)==0)
        || (reference.compareTo(OOConstants.GENGOU)==0)
        || (reference.compareTo(OOConstants.HANJA)==0)
        || (reference.compareTo(OOConstants.BUDDHIST)==0)
        || (reference.compareTo(OOConstants.ROC)==0))
        {
            //need to extract the locale from the filename
            int lastSlash = filename.lastIndexOf('/');
            int lastDot = filename.lastIndexOf('.');
            String locale = filename.substring(lastSlash+1, lastDot);
            vect.add(locale);
            vect.add(reference);
        }
        else
        {
            int first = reference.indexOf('_');
            int second = reference.indexOf('_', first+1);
            if (second == -1)
            {
                //we only have locale
                vect.add(reference);
            }
            else
            {
                vect.add(reference.substring(0, second));  //locale
                vect.add(reference.substring(second+1, reference.length()));   //type
            }
        }
        
        return vect;
    }
    
    
    /* makes the first char of inStr lower case and returns the resulting str
     * the rest of the string is left in tact
     */
    private static String makeFirstCharLowerCase(String inStr)
    {
        String outStr = "";
        
        String firstChar = inStr.substring(0,1);
        String theRest = inStr.substring(1, inStr.length());
        firstChar = firstChar.toLowerCase();
        outStr = firstChar + theRest;
        return outStr;
    }
    
    public static Hashtable mapRefs(Hashtable refs)
    {
        Hashtable LDMLRefs = new Hashtable();
        
        String ref = (String) refs.get(OOConstants.LC_FORMAT);
        if (ref!=null) LDMLRefs.put(OpenOfficeLDMLConstants.FORMAT, ref);

        ref = (String) refs.get(OOConstants.LC_COLLATION);
        if (ref!=null) LDMLRefs.put(OpenOfficeLDMLConstants.COLLATIONS, ref);
        
        ref = (String) refs.get(OOConstants.LC_SEARCH);
        if (ref!=null) LDMLRefs.put(OpenOfficeLDMLConstants.SEARCH, ref);
        
        ref = (String) refs.get(OOConstants.LC_INDEX);
        if (ref!=null) LDMLRefs.put(OpenOfficeLDMLConstants.INDEX, ref);
        
        ref = (String) refs.get(OOConstants.LC_TRANSLITERATION);
        if (ref!=null) LDMLRefs.put(OpenOfficeLDMLConstants.TRANSLITERATIONS, ref);
        
        ref = (String) refs.get(OOConstants.LC_MISC);
        if (ref!=null)
        {
            LDMLRefs.put(OpenOfficeLDMLConstants.RESERVED_WORDS, ref);
            LDMLRefs.put(OpenOfficeLDMLConstants.FORBIDDEN_CHARACTERS, ref);
        }
        
        ref = (String) refs.get(OOConstants.LC_NUMBERING_LEVEL);
        if (ref!=null) LDMLRefs.put(OpenOfficeLDMLConstants.NUMBERING_LEVELS, ref);
        
        ref = (String) refs.get(OOConstants.LC_OUTLINE_NUMBERING_LEVEL);
        if (ref!=null) LDMLRefs.put(OpenOfficeLDMLConstants.OUTLUNE_NUMBERING_LEVELS, ref);
        
        return LDMLRefs;
    }
    
    
    public static Hashtable mapLocaleInfo(Hashtable localeInfo)
    {
        Hashtable info = new Hashtable();
        
        String data = (String) localeInfo.get(OOConstants.VERSION_DTD);
        if (data != null) info.put(OpenOfficeLDMLConstants.VERSION_DTD, data);
        
        data = (String) localeInfo.get(OOConstants.ALLOW_UPDATE_FROM_CLDR);
        if (data != null) info.put(OpenOfficeLDMLConstants.ALLOW_UPDATE_FROM_CLDR, data);
        
        data = (String) localeInfo.get(OOConstants.VERSION);
        if (data != null) info.put(OpenOfficeLDMLConstants.VERSION, data);
        
        return info;
    }
}