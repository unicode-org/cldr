/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

/* class to map LDML elements and sttributes to their OpenOffice.org equivalent
 */

package org.unicode.cldr.ooo;

import java.util.*;
import org.unicode.cldr.icu.LDMLConstants;

public class LDMLToOOMapper
{
    // contains static classes
    
    // map LDML's various format elements (eg dateFormat) to
    // OO's FormatElement elements
    // Requirement: hashtables outFormatElements, outFormatElements and
    // outFormatDefaultNames cannot be null when passed in.
    public static void MapFormatElements( Hashtable inFormatElements,
    Hashtable inFormatPattern,
    Hashtable inFormatOO,
    Hashtable inFormatDefaultNames,
    Hashtable outFormatElements,
    Hashtable outFormatCodes,
    Hashtable outFormatDefaultNames)
    {
        if ((inFormatElements==null) || (inFormatPattern==null) || (inFormatOO==null))
            return;
        
        String defaultKey = null;
        String type = null;
        String usage = null;
        String formatIndex = null;
        
        Hashtable OOData_outer = new Hashtable();
        Enumeration data = inFormatElements.elements();
        Enumeration keys = inFormatElements.keys();
        
        while (keys.hasMoreElements() == true)
        {
            // type/msgid
            String key = (String) keys.nextElement();
            Hashtable innerLDML = (Hashtable) inFormatOO.get(key);
            
            if ((inFormatOO != null) && (inFormatOO.size()>0))
            {
                // Holder for attributes of OO's FormatElement tag.
                Hashtable formatElementAttribs = new Hashtable();
                formatElementAttribs.put(OOConstants.MSGID, key);
                // Assumption: the key which is taken from the LMDL format element's
                // type attribute is identical to it's subelemnt "special"'s attribute
                // openOffice:msgtype.  If this is not the case, the element
                // will not be written, as required attribute would be missing
                // in the output formatElement.
                
                defaultKey = (String) innerLDML.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT);
                if ((defaultKey != null) && (defaultKey.length()>0))
                    formatElementAttribs.put(OOConstants.DEFAULT, defaultKey);
                defaultKey = null;
                
                type = (String) innerLDML.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TYPE);
                if ((type != null) && (type.length()>0))
                    formatElementAttribs.put(OOConstants.TYPE, type);
                type = null;
                
                usage = (String) innerLDML.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.USAGE);
                if ((usage != null) && (usage.length()>0))
                    formatElementAttribs.put(OOConstants.USAGE, usage);
                type = null;
                
                formatIndex = (String) innerLDML.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FORMAT_INDEX_SMALL);
                
                if ((formatIndex != null) && (formatIndex.length()>0))
                    formatElementAttribs.put(OOConstants.FORMAT_INDEX, formatIndex);
                
                formatIndex = null;
                
                outFormatElements.put(key, formatElementAttribs);
                
                // get FormatCode
                String formatCode = (String) inFormatPattern.get(key);
                if ((formatCode != null) && (formatCode.length()>0))
                    outFormatCodes.put(key, formatCode);
                
                // get DefaultName
                String defaultName = (String) inFormatDefaultNames.get(key);
                if ((defaultName != null) && (defaultName.length()>0))
                    outFormatDefaultNames.put(key, defaultName);
            }
            
        }
    }
    
    public static String MapMeasurementSystem(String ms)
    {
        String ms_out = "";
        if (ms == null)
            return null;
        
        if (ms.compareTo(LDMLConstants.METRIC)==0)
            ms_out = OOConstants.METRIC_2;
        else if (ms.compareTo(LDMLConstants.US)==0)
            ms_out = OOConstants.US;
        
        return ms_out;
    }
    
    public static void MapCollators(Vector inCollators)
    {
        // change Collator's OpenOffice:unoid to unoid.
        
        Enumeration collations = inCollators.elements();
        
        while (collations.hasMoreElements() == true)
        {
            Hashtable collationElement = (Hashtable) collations.nextElement();
            //outCollations.add(collationElement);
            
            if ((collationElement != null) && (collationElement.size()>0))
            {
                String unoid = (String) collationElement.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.UNOID);
                if (unoid != null)
                {
                    collationElement.remove(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.UNOID);
                    collationElement.put(OOConstants.UNOID,  unoid);
                }
                String def = (String) collationElement.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT);
                if (def != null)
                {
                    collationElement.remove(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT);
                    collationElement.put(OOConstants.DEFAULT,  def);
                }
            }
        }//end while
    }
    
    /*
     *OpenOffice:unoid => unoid
     *OpenOffice:phonetic => phonetic
     *OpenOffice:module => module
     */
    public static void MapIndexKeys(Vector inIndexKeys)
    {
        Enumeration indexkeyTables = inIndexKeys.elements();
        
        while (indexkeyTables.hasMoreElements())
        {
            Hashtable indexkeyTable = (Hashtable) indexkeyTables.nextElement();
            if ((indexkeyTable != null) && (indexkeyTable.size()>0))
            {
                Hashtable attribs = (Hashtable) indexkeyTable.get("attributes");
                if ((attribs != null) && (attribs.size()>0))
                {
                    //unoid
                    String unoid = (String) attribs.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.UNOID);
                    if ((unoid != null) && (unoid.length()>0))
                    {
                        attribs.remove(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.UNOID);
                        attribs.put(OOConstants.UNOID, unoid);
                    }
                    
                    //phonetic
                    String phonetic = (String) attribs.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.PHONETIC);
                    if ((phonetic != null) && (phonetic.length()>0))
                    {
                        attribs.remove(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.PHONETIC);
                        attribs.put(OOConstants.PHONETIC, phonetic);
                    }
                    
                   //module
                    String module = (String) attribs.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.MODULE);
                    if ((module != null) && (module.length()>0))
                    {
                        attribs.remove(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.MODULE);
                        attribs.put(OOConstants.MODULE, module);
                    }      
                    
                    //default
                    String def = (String) attribs.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT);
                    if (def != null)
                    {
                        attribs.remove(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.DEFAULT);
                        attribs.put(OOConstants.DEFAULT,  def);
                    }
                }
            }
        }
        
    }
    
    public static void MapCalendar(Vector inCalendarTypes, Vector outCalendarTypes, String localeStr)
    {
        // inCalendar
        if ((inCalendarTypes != null) && (outCalendarTypes != null)  || (localeStr == null))
        {
            
            // Place hashtables key=unoid, value=LDML type attribute in the
            // vector outCalendar.
            Enumeration calEnum = inCalendarTypes.elements();
            while (calEnum.hasMoreElements())
            {
                Hashtable ldmlCal = (Hashtable) calEnum.nextElement();
                String calType = (String) ldmlCal.get(LDMLConstants.TYPE);
                if (calType != null)
                {
                    String ooUnoid = MapCalendarType(calType);
                    if (OOLocales.needCalendar(ooUnoid, localeStr)==true)
                    {
                        Hashtable ooCal = new Hashtable();
                        ooCal.put(OOConstants.UNOID, ooUnoid);
                        outCalendarTypes.add(ooCal);
                    }
                }
            }
        }
    }
    
    private static String MapCalendarType(String LDMLCalendar)
    {
        if (LDMLCalendar == null)
            return null;
        
        String OOCalendar = null;
        
        if (LDMLCalendar.compareTo(LDMLConstants.GREGORIAN)==0)
        {
            LDMLCalendar = OOConstants.GREGORIAN;
        }
        else if (LDMLCalendar.compareTo(LDMLConstants.CHINESE)==0)
        {
            LDMLCalendar = OOConstants.ROC;   //TODO, this is wrong, needs to be fixed
        }
        else if (LDMLCalendar.compareTo(LDMLConstants.ISLAMIC_CIVIL)==0)
        {
            LDMLCalendar = OOConstants.HIJRI;  //not sure if this is islamic or islamic-civil
        }
        else if (LDMLCalendar.compareTo(LDMLConstants.HEBREW)==0)
        {
            LDMLCalendar = OOConstants.JEWISH;
        }
        else if (LDMLCalendar.compareTo(LDMLConstants.BUDDHIST)==0)
        {
            LDMLCalendar = OOConstants.BUDDHIST;
        }
        else if (LDMLCalendar.compareTo(OOConstants.HANJA)==0)
        {
            LDMLCalendar = OOConstants.HANJA;
        }
        else if (LDMLCalendar.compareTo(LDMLConstants.JAPANESE)==0)
        {
            LDMLCalendar = OOConstants.GENGOU;
        }
        
        return LDMLCalendar;
    }
    
    // the Vector calAttribs should only contain one object, a hashtable.
    public static String MapDefaultCalendar(Vector calAttribs)
    {
        String defaultCal = null;
        
        if (calAttribs == null)
            return null;
        
        Enumeration calEnum = calAttribs.elements();
        if (calEnum.hasMoreElements())
        {
            Hashtable attribs = (Hashtable) calEnum.nextElement();
            
            if (attribs != null)
                defaultCal = (String) attribs.get(LDMLConstants.TYPE);
        }
        return defaultCal;
    }
    
    public static Hashtable MapDays(Hashtable LDMLData, String localeStr)
    {
        if ((LDMLData == null) || (LDMLData.size()==0)  || (localeStr == null))
            return null;
        
        Hashtable OOData = new Hashtable();
        
        Enumeration enDays = LDMLData.elements(); // hashtables
        Enumeration enCalType = LDMLData.keys(); // calendar types to identify each hashtable
        
        while (enDays.hasMoreElements() == true)
        {
            Hashtable innerOOData = new Hashtable();
            Hashtable LDMLDataI = (Hashtable) enDays.nextElement();
            String calTypeI = (String) enCalType.nextElement();
            
            String calTypeOO = MapCalendarType(calTypeI);
            
            if (OOLocales.needCalendar(calTypeOO, localeStr)==true)
            {  //only write the calendars needed by OO
                
                String day = (String) LDMLDataI.get(LDMLConstants.SUN);
                if (day != null) innerOOData.put(OOConstants.SUN, day);
                else
                {
                    Enumeration en = LDMLDataI.keys();
                    Enumeration enumVal = LDMLDataI.elements();
                    String key = (String) en.nextElement();
                    String value = (String) enumVal.nextElement();
                }
                day = (String) LDMLDataI.get((Object) LDMLConstants.MON);
                if (day != null) innerOOData.put(OOConstants.MON, day);
                
                day = (String) LDMLDataI.get((Object) LDMLConstants.TUE);
                if (day != null) innerOOData.put(OOConstants.TUE, day);
                
                day = (String) LDMLDataI.get((Object) LDMLConstants.WED);
                if (day != null) innerOOData.put(OOConstants.WED, day);
                
                day = (String) LDMLDataI.get((Object) LDMLConstants.THU);
                if (day != null) innerOOData.put(OOConstants.THU, day);
                
                day = (String) LDMLDataI.get((Object) LDMLConstants.FRI);
                if (day != null) innerOOData.put(OOConstants.FRI, day);
                
                day = (String) LDMLDataI.get((Object) LDMLConstants.SAT);
                if (day != null) innerOOData.put(OOConstants.SAT, day);
                
                if ((calTypeOO != null) &&  (innerOOData.size() >0))
                    OOData.put( calTypeOO, innerOOData);
            }
        }
        
        return OOData;
    }
    
    
   /* input (and output) = Hashtable of Hasdhtables, outer key = calendar type (i.e. gregorian) , inner key = month type (i.e. jan or 1)
    * OO uses OOConstants.JAN -> OOConstants.DEC to identify months
    *  LDML uses LDMLConstants.MONTH_1 -> LDMLConstants.MONTH_13
    */
    public static Hashtable MapMonths(Hashtable LDMLData, String localeStr)
    {
        if ((LDMLData == null)  || (LDMLData.size()==0)  || (localeStr == null))
            return null;
        
        Hashtable OOData = new Hashtable();
        
        Enumeration enMonths = LDMLData.elements();
        Enumeration enCalType = LDMLData.keys();
        
        while (enMonths.hasMoreElements() == true)
        {
            Hashtable innerOOData = new Hashtable();
            Hashtable LDMLDataI = (Hashtable) enMonths.nextElement();
            String calTypeI = (String) enCalType.nextElement();
            String calTypeOO = MapCalendarType(calTypeI);
            
            if (OOLocales.needCalendar(calTypeOO, localeStr)==true)
            {  //only write the calendars needed by OO
                
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
                String m13 = "";  //only have month 13 in jewish calerndar in OO
                if (calTypeOO.compareTo(OOConstants.JEWISH)==0)
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
                
                String month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_1);
                if (month != null) innerOOData.put(m1, month);
                
                month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_2);
                if (month != null) innerOOData.put(m2, month);
                
                month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_3);
                if (month != null) innerOOData.put(m3, month);
                
                month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_4);
                if (month != null) innerOOData.put(m4, month);
                
                month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_5);
                if (month != null) innerOOData.put(m5, month);
                
                month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_6);
                if (month != null) innerOOData.put(m6, month);
                
                month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_7);
                if (month != null) innerOOData.put(m7, month);
                
                month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_8);
                if (month != null) innerOOData.put(m8, month);
                
                month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_9);
                if (month != null) innerOOData.put(m9, month);
                
                month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_10);
                if (month != null) innerOOData.put(m10, month);
                
                month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_11);
                if (month != null) innerOOData.put(m11, month);
                
                month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_12);
                if (month != null) innerOOData.put(m12, month);
                
                if (calTypeOO.compareTo(OOConstants.JEWISH)==0)
                {
                    month = (String) LDMLDataI.get((Object) LDMLConstants.MONTH_13);
                    if (month != null) innerOOData.put(m13, month);
                }
                
                if ((calTypeOO != null) && (innerOOData.size() >0))
                    OOData.put( calTypeOO, innerOOData);
            }
        }
        return OOData;
    }
    
    
    /* input Hashtable of Hashtables of Hashtables, outer key = calendar type (i.e. gregorian) , inner key = era type (i.e. bc or 0)
     * Output: Hashtable of Hashtables, outer key = calendar type, inner key = OO eraID, inner inner key = OOConstants.DEFAULT_ABBRV_NAME or OOConstants.DEFAULT_FULL_NAME,
     * containing the relevant era names.
     * OO uses OOConstants.BC etc to identify eras
     *  LDML uses LDMLConstants.ERA_0 -> LDMLConstants.ERA_235 (ja)
     * Assumes that each era in the LDML data will at least have their full names,
     * and possibly abbreviated names.
     */
    public static Hashtable MapEras(Hashtable LDMLEraNames, Hashtable LDMLAbbrEras, String localeStr)
    {
        if (((LDMLEraNames == null) && (LDMLEraNames.size()==0)) || ((LDMLAbbrEras == null) && (LDMLAbbrEras.size()==0))  || (localeStr == null))
            return null;
        
        Hashtable OOData = new Hashtable();
        Enumeration enMonths = LDMLAbbrEras.elements();
        Enumeration enCalType = LDMLAbbrEras.keys();
        
        while (enMonths.hasMoreElements() == true)
        {
            Hashtable innerOOData = new Hashtable();
            Hashtable ooEraNames_Era0 = new Hashtable();
            Hashtable ooEraNames_Era1 = new Hashtable();
            Hashtable ooAbbrEraNames_Era0 = new Hashtable();
            Hashtable ooAbbrEraNames_Era1 = new Hashtable();
            
            Hashtable LDMLabbrDataI = (Hashtable) enMonths.nextElement();
            
            String calTypeI = (String) enCalType.nextElement();
            String calTypeOO = MapCalendarType(calTypeI); // eg gregorian
            
            if (OOLocales.needCalendar(calTypeOO, localeStr)==true)
            {  //only write the calendars needed by OO         
                // get the abbreviated names for this calendar
                Hashtable LDMLDataI = (Hashtable) LDMLEraNames.get(calTypeI);
                
                //go through all possible OO calendar types
                if ((calTypeOO.compareTo(OOConstants.GREGORIAN)==0) || (calTypeOO.compareTo(OOConstants.HANJA)==0))
                    mapEra(LDMLDataI, LDMLabbrDataI, ooEraNames_Era0, ooEraNames_Era1, innerOOData, OOConstants.BC, OOConstants.AD);
                else if (calTypeOO.compareTo(OOConstants.HIJRI)==0)
                {
                    mapEra(LDMLDataI, LDMLabbrDataI, ooEraNames_Era0, ooEraNames_Era1, innerOOData, OOConstants.BEFORE_HIJRA, OOConstants.AFTER_HIJRA);
                }
                else if (calTypeOO.compareTo(OOConstants.JEWISH)==0)
                    mapEra(LDMLDataI, LDMLabbrDataI, ooEraNames_Era0, ooEraNames_Era1, innerOOData, OOConstants.BEFORE, OOConstants.AFTER);
                else if (calTypeOO.compareTo(OOConstants.BUDDHIST)==0)
                    mapEra(LDMLDataI, LDMLabbrDataI, ooEraNames_Era0, ooEraNames_Era1, innerOOData, OOConstants.BEFORE, OOConstants.AFTER);
                else if (calTypeOO.compareTo(OOConstants.ROC)==0)
                    mapEra(LDMLDataI, LDMLabbrDataI, ooEraNames_Era0, ooEraNames_Era1, innerOOData, OOConstants.BEFORE_ROC, OOConstants.MINGUO);
                else if (calTypeOO.compareTo(OOConstants.HANJA)==0)  //korean
                    mapEra(LDMLDataI, LDMLabbrDataI, ooEraNames_Era0, ooEraNames_Era1, innerOOData, OOConstants.BC, OOConstants.AD);
                else if (calTypeOO.compareTo(OOConstants.GENGOU)==0)
                {
                    Hashtable ooEraNames_1 = new Hashtable();
                    mapOneEra(LDMLDataI, LDMLabbrDataI, ooEraNames_1, innerOOData, OOConstants.DUMMY, "-1");
                    Hashtable ooEraNames232 = new Hashtable();
                    mapOneEra(LDMLDataI, LDMLabbrDataI, ooEraNames232, innerOOData, OOConstants.MEIJI, LDMLConstants.ERA_232);
                    Hashtable ooEraNames233 = new Hashtable();
                    mapOneEra(LDMLDataI, LDMLabbrDataI, ooEraNames233, innerOOData, OOConstants.TAISHO, LDMLConstants.ERA_233);
                    Hashtable ooEraNames234 = new Hashtable();
                    mapOneEra(LDMLDataI, LDMLabbrDataI, ooEraNames234, innerOOData, OOConstants.SHOWA, LDMLConstants.ERA_234);
                    Hashtable ooEraNames235 = new Hashtable();
                    mapOneEra(LDMLDataI, LDMLabbrDataI, ooEraNames235, innerOOData, OOConstants.HEISEI, LDMLConstants.ERA_235);
                }
                
                if ((calTypeOO != null) && (innerOOData.size() >0))
                    OOData.put( calTypeOO, innerOOData);
            }
        }
        
        return OOData;
    }
    
    private static void mapEra(Hashtable LDMLDataI, Hashtable LDMLabbrDataI, Hashtable ooEraNames_Era0, Hashtable ooEraNames_Era1, Hashtable innerOOData, String era0, String era1)
    {
        mapOneEra(LDMLDataI, LDMLabbrDataI, ooEraNames_Era0, innerOOData, era0, LDMLConstants.ERA_0);
        mapOneEra(LDMLDataI, LDMLabbrDataI, ooEraNames_Era1, innerOOData, era1, LDMLConstants.ERA_1);
    }
    
    private static void mapOneEra(Hashtable LDMLDataI, Hashtable LDMLabbrDataI, Hashtable ooEraNames_Era, Hashtable innerOOData, String eraOO, String ldmlEraConstant)
    {
        if ((LDMLDataI != null) && (LDMLDataI.size()>0))
        {
            String era = (String) LDMLDataI.get((Object) ldmlEraConstant);
            if (era != null) ooEraNames_Era.put(OOConstants.DEFAULT_FULL_NAME, era);
        }
        if ((LDMLabbrDataI != null) && (LDMLabbrDataI.size()>0))
        {
            String era = (String) LDMLabbrDataI.get((Object) ldmlEraConstant);
            if (era != null) ooEraNames_Era.put(OOConstants.DEFAULT_ABBRV_NAME, era);
        }
        if (ooEraNames_Era.size()>0) innerOOData.put(eraOO, ooEraNames_Era);
    }
    
    public static Hashtable MapStartOfWeeks(Hashtable startOfWeeks, String localeStr)
    {
        if ((startOfWeeks == null) || (localeStr == null))
            return null;
        
        Hashtable ooStartOfWeeks = new Hashtable();
        
        Enumeration keys = startOfWeeks.keys();
        while (keys.hasMoreElements())
        {
            String calendar = (String) keys.nextElement();  // eg gregorian
            String calendarOO = MapCalendarType(calendar);
            
            if (OOLocales.needCalendar(calendarOO, localeStr)==true)
            {  //only write the calendars needed by OO
                
                if ((calendarOO != null) && (calendarOO.length()>0))
                {
                    String dayLDML = (String) startOfWeeks.get(calendar);
                    String sow = null;
                    if (dayLDML.compareTo(LDMLConstants.MON)==0) sow = OOConstants.MON;
                    else if (dayLDML.compareTo(LDMLConstants.TUE)==0) sow = OOConstants.TUE;
                    else if (dayLDML.compareTo(LDMLConstants.WED)==0) sow = OOConstants.WED;
                    else if (dayLDML.compareTo(LDMLConstants.THU)==0) sow = OOConstants.THU;
                    else if (dayLDML.compareTo(LDMLConstants.FRI)==0) sow = OOConstants.FRI;
                    else if (dayLDML.compareTo(LDMLConstants.SAT)==0) sow = OOConstants.SAT;
                    else if (dayLDML.compareTo(LDMLConstants.SUN)==0) sow = OOConstants.SUN;
                    
                    if (sow != null)
                        ooStartOfWeeks.put(calendarOO, sow);
                }
            }
        }
        
        return ooStartOfWeeks;
    }
    
    public static Hashtable MapMinDays(Hashtable minDaysLDML, String localeStr)
    {
        if ((minDaysLDML == null)  || (localeStr == null))
            return null;
        
        Hashtable ooMinDays = new Hashtable();
        
        Enumeration keys = minDaysLDML.keys();
        while (keys.hasMoreElements())
        {
            String calendar = (String) keys.nextElement();
            String calendarOO = MapCalendarType(calendar);
            
            if (OOLocales.needCalendar(calendarOO, localeStr)==true)
            {  //only write the calendars needed by OO
                String minDays = null;
                minDays = (String) minDaysLDML.get(calendar);
                if (minDays != null) ooMinDays.put(calendarOO, minDays);
            }
        }
        
        return ooMinDays;
    }
    
    // Takes in vetor of hashtables.
    // return a vector of strings
    public static Vector MapToCurrencyID(Vector currencies)
    {
        if (currencies == null)
            return null;
        
        Vector currencyIDs = new Vector();
        
        Enumeration currEnum = currencies.elements();
        while (currEnum.hasMoreElements())
        {
            Hashtable currency = (Hashtable) currEnum.nextElement();
            if (currency != null)
            {
                String currID = (String) currency.get(LDMLConstants.TYPE);
                
                if ((currID != null) && (currID.length()>0))
                    currencyIDs.add(currID);
            }
        }
        
        return currencyIDs;
    }
    
    
    // Return Vector of Strings, each string being an LDML currency type.
    public static Vector ExtractLDMLCurrencyTypes(Vector ldmlCurrencies, supplementalData suppData, String localeStr, boolean bRoundTrip)
    {
        if ((ldmlCurrencies == null) || (ldmlCurrencies.size()==0) || (suppData == null))
            return null;
        
        Vector ldmlCurrencyTypes = null;
        String region = getRegion(localeStr);
        if (bRoundTrip == false)
        {
            //just return currenct and most recent deprecated currency (if any) , OO.o not interested in older ones
            Vector all = suppData.getCurrencies (region);   //all currencies past and present for the region
            Vector v = new Vector ();
            if (all.size()>0) v.add (all.elementAt(0));
            if (all.size()>1) v.add (all.elementAt(1));
            return v;   
        }
        else   
            return ldmlCurrencies;
    }
    
    public static Vector MapTransliterations(Vector ldmlTransliterationsAtts)
    {
        Vector trans = new Vector();
        
        if ((ldmlTransliterationsAtts == null) || (ldmlTransliterationsAtts.size()==0))
            return null;
        
        Enumeration transEnum = ldmlTransliterationsAtts.elements();
        while (transEnum.hasMoreElements())
        {
            Hashtable attribs = (Hashtable) transEnum.nextElement();
            if (attribs != null)
            {
                String unoid = (String) attribs.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.UNOID);
                if ((unoid != null) && (unoid.length()>0))
                    trans.add(unoid);
            }
        }
        
        return trans;
    }
    
    public static Hashtable MapReservedWords(Hashtable reservedWords)
    {
        if ((reservedWords == null) || (reservedWords.size()==0))
            return null;
        
        Hashtable mappedWords = new Hashtable();
        addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.TRUE_WORD, OOConstants.TRUE_WORD);
        addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.FALSE_WORD, OOConstants.FALSE_WORD);
   //     addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.QUARTER_1_WORD, OOConstants.QUARTER_1_WORD);
   //     addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.QUARTER_2_WORD, OOConstants.QUARTER_2_WORD);
   //     addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.QUARTER_3_WORD, OOConstants.QUARTER_3_WORD);
  //      addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.QUARTER_4_WORD, OOConstants.QUARTER_4_WORD);
        addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.ABOVE_WORD, OOConstants.ABOVE_WORD);
        addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.BELOW_WORD, OOConstants.BELOW_WORD);
  //      addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.QUARTER_1_ABBREVIATION, OOConstants.QUARTER_1_ABBREVIATION);
  //      addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.QUARTER_2_ABBREVIATION, OOConstants.QUARTER_2_ABBREVIATION);
  //      addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.QUARTER_3_ABBREVIATION, OOConstants.QUARTER_3_ABBREVIATION);
  //      addReservedWord(mappedWords, reservedWords, OpenOfficeLDMLConstants.QUARTER_4_ABBREVIATION, OOConstants.QUARTER_4_ABBREVIATION);
        
        return mappedWords;
    }
    
    //add abbr and wide quarters to reservedWords table
  // abbrQuarters/wideQuarters = Hashtable of Hasdhtables, outer key = calendar type (i.e. gregorian) , inner key = quarter type (1-4)
    public static void MapQuarters(Hashtable mappedWords, Hashtable abbrQuarters, Hashtable wideQuarters)
    {
        if (mappedWords == null || abbrQuarters==null || wideQuarters==null )
            return;
        
        Hashtable abQ = (Hashtable) abbrQuarters.get (LDMLConstants.GREGORIAN);
        addReservedWord(mappedWords, abQ, LDMLConstants.Q_1, OOConstants.QUARTER_1_ABBREVIATION);
        addReservedWord(mappedWords, abQ, LDMLConstants.Q_2, OOConstants.QUARTER_2_ABBREVIATION);
        addReservedWord(mappedWords, abQ, LDMLConstants.Q_3, OOConstants.QUARTER_3_ABBREVIATION);
        addReservedWord(mappedWords, abQ, LDMLConstants.Q_4, OOConstants.QUARTER_4_ABBREVIATION);
  
        Hashtable wideQ = (Hashtable) wideQuarters.get (LDMLConstants.GREGORIAN);
        addReservedWord(mappedWords, wideQ, LDMLConstants.Q_1, OOConstants.QUARTER_1_WORD);
        addReservedWord(mappedWords, wideQ, LDMLConstants.Q_2, OOConstants.QUARTER_2_WORD);
        addReservedWord(mappedWords, wideQ, LDMLConstants.Q_3, OOConstants.QUARTER_3_WORD);
        addReservedWord(mappedWords, wideQ, LDMLConstants.Q_4, OOConstants.QUARTER_4_WORD);
        
     }
    
    // called from MapReservedWords.
    private static void addReservedWord(Hashtable mappedWords, Hashtable ldmlWords, String ldmlKey, String ooKey)
    {
        String word = (String) ldmlWords.get(ldmlKey);
        if ((word != null) && (word.length()>0))
            mappedWords.put(ooKey, word);
    }
    
    // Passed in and out a Vector of Hashtables, each Hashtable holding the
    // attributes of a NumberingLevel element.
    public static Vector MapNumberingLevels(Vector ldmlNumberingLevelAtts)
    {
        if ((ldmlNumberingLevelAtts == null) || (ldmlNumberingLevelAtts.size()==0))
            return null;
        
        Vector levels = new Vector();
        
        Enumeration numEnum = ldmlNumberingLevelAtts.elements(); // :)
        while (numEnum.hasMoreElements())
        {
            Hashtable atts = (Hashtable) numEnum.nextElement();
            Hashtable ooAtts = new Hashtable();
            if ((atts != null) && (atts.size()>0))
            {
                String value;
                value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.PREFIX);
                if ((value != null) && (value.length()>0))
                    ooAtts.put(OOConstants.PREFIX, value);
                value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.NUM_TYPE);
                if ((value != null) && (value.length()>0))
                    ooAtts.put(OOConstants.NUM_TYPE, value);
                value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.SUFFIX);
                if ((value != null) && (value.length()>0))
                    ooAtts.put(OOConstants.SUFFIX, value);
                value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TRANSLITERATION);
                if ((value != null) && (value.length()>0))
                    ooAtts.put(OOConstants.TRANSLITERATION, value);
                value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.NAT_NUM);
                if ((value != null) && (value.length()>0))
                    ooAtts.put(OOConstants.NAT_NUM, value);
            }
            if (ooAtts.size()>0)
                levels.add(ooAtts);
        }
        
        return levels;
    }
    
    /*
     *Input and output is a Vector containing Vectors.  Each inner Vector
     *represents all the relevant elements of one parent element.  The inner
     *Vectors contain Hashtables of attributes of the inner elements.
     */
    public static Vector MapOutlineNumberingLevels(Vector ldmlOutlineNumberingLevels)
    {
        if (((ldmlOutlineNumberingLevels) == null) || (ldmlOutlineNumberingLevels.size()==0))
            return null;
        
        Vector outlineNumberingLevels = new Vector();
        Enumeration levelsEnum = ldmlOutlineNumberingLevels.elements();
        while (levelsEnum.hasMoreElements())
        {
            Vector parentGroup = (Vector) levelsEnum.nextElement();
            Vector parentGroupOO = new Vector();
            if ((parentGroup != null) && (parentGroup.size()>0))
            {
                // parentGroup is a Vector of Hashtables.
                // The attribute (keys) of the Hashtables must be converted
                // to OpenOffice format.
                Enumeration elemEnum = parentGroup.elements();
                while (elemEnum.hasMoreElements())
                {
                    Hashtable atts = (Hashtable) elemEnum.nextElement();
                    Hashtable attsOO = new Hashtable();
                    if ((atts != null) && (atts.size()>0))
                    {
                        // convert the Keys from LDML format to OO.
                        
                        String value;
                        value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.PREFIX);
                        if ((value != null))
                            attsOO.put(OOConstants.PREFIX, value);
                        value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.NUM_TYPE);
                        if ((value != null))
                            attsOO.put(OOConstants.NUM_TYPE, value);
                        value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.SUFFIX);
                        if ((value != null))
                            attsOO.put(OOConstants.SUFFIX, value);
                        value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.BULLET_CHAR);
                        if ((value != null))
                            attsOO.put(OOConstants.BULLET_CHAR, value);
                        value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.BULLET_FONT_NAME);
                        if ((value != null))
                            attsOO.put(OOConstants.BULLET_FONT_NAME, value);
                        value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.PARENT_NUMBERING);
                        if ((value != null))
                            attsOO.put(OOConstants.PARENT_NUMBERING, value);
                        value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.LEFT_MARGIN);
                        if ((value != null))
                            attsOO.put(OOConstants.LEFT_MARGIN, value);
                        value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.SYMBOL_TEXT_DISTANCE);
                        if ((value != null))
                            attsOO.put(OOConstants.SYMBOL_TEXT_DISTANCE, value);
                        value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.FIRST_LINE_OFFSET);
                        if ((value != null))
                            attsOO.put(OOConstants.FIRST_LINE_OFFSET, value);
                        value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.TRANSLITERATION);
                        if ((value != null))
                            attsOO.put(OOConstants.TRANSLITERATION, value);
                        value = (String) atts.get(XMLNamespace.OPEN_OFFICE + ":" + OpenOfficeLDMLConstants.NAT_NUM);
                        if ((value != null))
                            attsOO.put(OOConstants.NAT_NUM, value);
                    }
                    
                    if (attsOO.size()>0)
                        parentGroupOO.add(attsOO);
                } // end while
                
                if (parentGroupOO.size()>0)
                    outlineNumberingLevels.add(parentGroupOO);
            }
        }
        
        return outlineNumberingLevels;
    }
    
    private static String getRegion(String localeStr)
    {
        String region = null;
        String str [] = localeStr.split("_");
        if (str.length > 1)
        {
            region = str[1];
            if (region.compareTo("CB")==0) region = "US";
            if (region.compareTo("YU")==0) region = "CS";
        }
        else if (str.length == 1)
        {
            if (str[0].compareTo("eu")==0) region = "ES";
        }
        
        return region;
    }
    
    public static Hashtable mapLocaleInfo(Hashtable localeInfo)
    {
        Hashtable info = new Hashtable();
        
        String data = (String) localeInfo.get(OpenOfficeLDMLConstants.VERSION_DTD);
        if (data != null) info.put(OOConstants.VERSION_DTD, data);
        
        data = (String) localeInfo.get(OpenOfficeLDMLConstants.ALLOW_UPDATE_FROM_CLDR);
        if (data != null) info.put(OOConstants.ALLOW_UPDATE_FROM_CLDR, data);
        
        data = (String) localeInfo.get(OpenOfficeLDMLConstants.VERSION);
        if (data != null) info.put(OOConstants.VERSION, data);
        
        return info;
    }
}
