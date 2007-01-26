/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

import com.ibm.icu.lang.UCharacter;
import java.io.*;

/**
 *
 * class to map OO date and time patterns to LDML
 *
 * LDML  	OpenOffice  	Description (my comments in (..))
 * Dates
 *
 * M            M               Month as 3.
 * MM           MM              Month as 03.
 * MMM          MMM             Month as Jan-Dec
 * MMMM 	MMMM            Month as January-December
 * MMMMM 	MMMMM           First letter of Name of Month
 * d            D               Day as 2
 * dd           DD              Day as 02
 * EEE          NN or DDD 	Day as Sun-Sat
 * EEEE 	NNN or DDDD 	Day as Sunday to Saturday
 * EEEE, 	NNNN            Day followed by comma, as in "Sunday,"
 * yy            YY              Year as 00-99
 * yyyy         YYYY            Year as 1900-2078
 * ww           WW              Calendar week
 * QQQ   	Q               Quarterly as Q1 to Q4
 * QQQQ 	QQ              Quarterly as 1st quarter to 4th quarter
 * MTSH         G               Era on the Japanese Gengou calendar, single character (possible values are: M, T, S, H)  (workaround)
 * G            GG              Era, abbreviation
 * GGGG 	GGG             Era, full name
 * y            E               Number of the year within an era, without a leading zero for single-digit years
 * yy           EE or R 	Number of the year within an era, with a leading zero for single-digit years
 * GGGGyy       RR or GGGEE 	Era, full name and year
 *              AAA or AAAA 	Arabic Islamic format, 
 *                              AAA only used in es,fr,it,pt and ja locales (OO.o m134 data) so not an issue for now
 *                                                  
 *
 * Seems to be no way to get AD/BC
 *
 * Times
 *
 * H            h or H          Hours as 0-23
 * HH           hh or HH        Hours as 00-23
 * K            h with AM/PM    Hours as 0-11 (this is my assumption, OO.o doesn't say anything about 12 hour clock)
 * KK           hh with AM/PM   Hours as 00-11
 * m            m or M          Minutes as 0-59
 * mm           mm or MM        Minutes as 00-59
 * s            s or S          Seconds as 0-59
 * ss           ss or SS        Seconds as 00-59
 * SSS          000
 * a            AM/PM
 * [HH]         [HH]            hours, not modulus 24, same for mins and seconds
 *
 * Unclear how to get hours 1-24, 1-12 (k and K in LDML)
 *
 *
 * localisaed pattern characters used in OO.o are : 
 *  A G J K O P T U V X
 * of these there is potential conflict with A and P as they share with  AM/PM so 
 * the very first thing to do with any pattern string is to convert AM/PM to a
 *
 *
 * Contraints and Assumptions:
 * 1. some chars have special meaning for both OO and LDML, so order of replacement is important
 * 2. check that data in not in quotes before translating
 * 3. assuming mutex within data categories, i.e. can't have 2 types of Months within a single pattern
 * 4. M is used for minutes and months in OO, so need to check context
 * 5. " translates to '
 */



public class OOToLDMLPattern
{

    private String m_localeStr = null;
    private String m_type = null;
    private boolean m_bIsAmPM = false;
    
    OOToLDMLPattern(String localeStr)
    {
        m_localeStr = localeStr;
        
    }
    
    public String map(String OOPattern, String type)
    {
        if (OOPattern == null)
            return null;
        
   /* incopmplete    if (validatePattern (OOPattern) == false)
        {
            System.err.println ("Skipping invalid pattern : " + OOPattern + " in " + m_localeStr);
            return null;
        }*/
        
        m_type = type;
        String LDMLPattern = OOPattern;
          
        //do AM/PM first as the Ms in AM/PM can be ambiguous
        // and A and P can be used for localised patterns also
        if (translate(LDMLPattern, "AM/PM"))
        {
            LDMLPattern = LDMLPattern.replaceAll("AM/PM", "a");   
            m_bIsAmPM = true;
        }

        LDMLPattern = deLocalisePattern(LDMLPattern);
        
        if (m_type.compareTo(OOConstants.FEU_DATE)==0)
        {
            LDMLPattern = mapDate(LDMLPattern);
            
        }
        else if (m_type.compareTo(OOConstants.FEU_TIME)==0)
        {
            LDMLPattern = mapTime(LDMLPattern);
        }
        else if (m_type.compareTo(OOConstants.FEU_DATE_TIME)==0)
        {
            LDMLPattern = mapDateTime(LDMLPattern);
        }
        
        LDMLPattern = LDMLPattern.replaceAll("\"", "'");
        LDMLPattern = quoteIt (LDMLPattern);
        return LDMLPattern;
    }
    
    private String quoteIt (String LDMLPattern)
    {
        String temp = LDMLPattern;
        StringBuffer sb = new StringBuffer();

          //non ascii chars in OO.o date don't seem to be in quotes, so need to quote them now
        boolean bStartedQuotes = false;
        for (int i=0; i < LDMLPattern.length(); i++)
        {
            //assuming OO.o has put latin and latin 1 in quotes, seems to be true
            if (LDMLPattern.charAt(i) > 255)
            {
                //if not in quotes then  quote it
                int quotes=0;
                for (int j=0; j < i; j++)
                {
                    if (LDMLPattern.charAt(j) == '\'') quotes++;
                }
                if ((quotes % 2) ==0)
                {  //it's not in quotes so quote it
                    if (bStartedQuotes == false)
                    {
                        sb.append ('\'');
                        bStartedQuotes = true;
                    }
                }
                sb.append(LDMLPattern.charAt(i));
             
           /*     try
                {
                    BufferedWriter out = new BufferedWriter(new FileWriter("chars",true));
                    out.write(LDMLPattern + "  " + Integer.toString( UCharacter.getCodePoint(LDMLPattern.charAt(i)))  + "\n");
                    out.close();
                }
                catch (IOException e)
                {}*/
           //     System.err.println (LDMLPattern + "  " + Integer.toString( UCharacter.getCodePoint(LDMLPattern.charAt(i) )));
            }
            else 
            {
                if (bStartedQuotes == true) 
                {
                    sb.append ('\'');
                    bStartedQuotes = false;
                }
                sb.append (LDMLPattern.charAt(i));
            }
       
        }
        
        //final check
        if (bStartedQuotes == true)
        {
            sb.append('\'');
            bStartedQuotes = false;
        }
        
        LDMLPattern = sb.toString();
                
        if (temp.compareTo(LDMLPattern) != 0)
            System.err.println ("replacing :" + temp + "   with :" + LDMLPattern);
        
        return LDMLPattern;
    }
    
    
    //check that chars not in "" are valid OO specials
    //not used , not finished
   /* private boolean validatePattern (String OOPattern)
    {
        StringBuffer buf = new StringBuffer ();
        boolean bInQuotes = false;
        boolean bInSquare = false;
        
        for (int i=0; i < OOPattern.length(); i++)
        {
            if (bInQuotes == true)
            {
                if (OOPattern.charAt(i) == '"') bInQuotes = false;
                continue;
            }
            if (bInSquare == true)
            {
                if (OOPattern.charAt(i) == ']') bInSquare = false;
                continue;
            }
            
            //not in quotes or brackets
            if (OOPattern.charAt(i) == '"')
            {
                bInQuotes = true;
            }
            else if (OOPattern.charAt(i) == '[')
            {
                bInSquare = true;
            }
            else
            {
                buf.append(OOPattern.charAt(i));
            }
        }
        
        boolean bIsValid = false;
        //System.err.println (buf.toString());
        String validChars = "AGJKOPTUVXMDNYWQGMTSHERAHhMmSs0[]/,.:-() \u6708\u65E5\u5E74\u6642\u5206\u79D2";
        for (int i=0; i < buf.length(); i++)
        {
            bIsValid = false;
            for (int j=0; j < validChars.length(); j++)
            {
                if (validChars.charAt(j) == buf.charAt(i))
                {
                    bIsValid = true;
                    break;
                }
            }
            if (bIsValid == false)
            {
                System.err.println (buf.charAt(i));
                System.err.println (buf.toString());
                break;
            }
        }
        
        return bIsValid;
    }*/
    
    
    //the order in which replacements are done is important
    private String mapDate(String LDMLPattern)
    {
        //eras
     //   if (translate(LDMLPattern, "GGGEE"))
     //       LDMLPattern = LDMLPattern.replaceAll("GGGEE", "GGGGyy");
  
        if (translate(LDMLPattern, "GGG"))
            LDMLPattern = LDMLPattern.replaceAll("GGG", "%%%%");  //workaround
          //  LDMLPattern = LDMLPattern.replaceAll("GGG", "GGGG");
        
        else if (translate(LDMLPattern, "GG"))
            LDMLPattern = LDMLPattern.replaceAll("GG", "&");  //workaround
           // LDMLPattern = LDMLPattern.replaceAll("GG", "G");
        
        else if (translate(LDMLPattern, "G"))
            LDMLPattern = LDMLPattern.replaceAll("G", "MTSH");
        
        else if (translate(LDMLPattern, "RR"))
            LDMLPattern = LDMLPattern.replaceAll("RR", "GGGGyy");
        
        //now undo workaround
        if (translate(LDMLPattern, "%%%%"))
            LDMLPattern = LDMLPattern.replaceAll("%%%%", "GGGG");
         else if (translate(LDMLPattern, "&"))
             LDMLPattern = LDMLPattern.replaceAll("&", "G");
        
        
        //year
        if (translate(LDMLPattern, "EE"))
            LDMLPattern = LDMLPattern.replaceAll("EE", "yy");
        else if (translate(LDMLPattern, "R"))
            LDMLPattern = LDMLPattern.replaceAll("R", "yy");
        
        else if (translate(LDMLPattern, "E"))
            LDMLPattern = LDMLPattern.replaceAll("E", "y");
        
        else if (translate(LDMLPattern, "YYYY"))
            LDMLPattern = LDMLPattern.replaceAll("YYYY", "yyyy");
        
        else if (translate(LDMLPattern, "YY"))
            LDMLPattern = LDMLPattern.replaceAll("YY", "yy");
        
        //month patterns are same , do nothing
        
        //day patterns - day names
        if (translate(LDMLPattern, "NNNN"))
            LDMLPattern = LDMLPattern.replaceAll("NNNN", "EEEE,");
        else if (translate(LDMLPattern, "DDDD"))
            LDMLPattern = LDMLPattern.replaceAll("DDDD", "EEEE");
        else if (translate(LDMLPattern, "NNN"))
            LDMLPattern = LDMLPattern.replaceAll("NNN", "EEEE");
        else if (translate(LDMLPattern, "DDD"))
            LDMLPattern = LDMLPattern.replaceAll("DDD", "EEE");
        else if (translate(LDMLPattern, "NN"))
            LDMLPattern = LDMLPattern.replaceAll("NN", "EEE");
        
        //day patterns - day of the month
        if (translate(LDMLPattern, "DD"))
            LDMLPattern = LDMLPattern.replaceAll("DD", "dd");
        else if (translate(LDMLPattern, "D"))
            LDMLPattern = LDMLPattern.replaceAll("D", "d");
        
        //week
        if (translate(LDMLPattern, "WW"))
            LDMLPattern = LDMLPattern.replaceAll("WW", "ww");
        
        //year quarters
        if (translate(LDMLPattern, "QQ"))
            LDMLPattern = LDMLPattern.replaceAll("QQ", "QQQQ");
        else if (translate(LDMLPattern, "Q"))
            LDMLPattern = LDMLPattern.replaceAll("Q", "QQQ");
        
        return LDMLPattern;
    }
    
    //order of things are done is important
    private String mapTime(String LDMLPattern)
    {   
        if (m_type.compareTo(OOConstants.FEU_DATE_TIME)==0)
        {
            LDMLPattern = doMinutes(LDMLPattern);
        }
        else if (m_type.compareTo(OOConstants.FEU_TIME)==0)
        {
            if (translate(LDMLPattern, "M"))
            {   //takes care of M and MM
                LDMLPattern = LDMLPattern.replace('M', 'm');
            }
        }
        
        //hours
        if (translate(LDMLPattern, "h"))
        {   //takes care of h and hh
            if (m_bIsAmPM == true)
                LDMLPattern = LDMLPattern.replace('h', 'K');
            else
                LDMLPattern = LDMLPattern.replace('h', 'H');
        }
        else if (translate(LDMLPattern, "H"))
        {   //takes care of H and HH where AM/PM is present, apttern is same if not present
            if (m_bIsAmPM == true)
                LDMLPattern = LDMLPattern.replace('H', 'K');
        }
        
        //seconds
        if (translate(LDMLPattern, "S"))
        {   //takes care of S and SS
            LDMLPattern = LDMLPattern.replace('S', 's');
        }
        
        //milliseconds
        if (translate(LDMLPattern, "0"))
        {   //takes care of 0, 00, 000 etc
            LDMLPattern = LDMLPattern.replace('0', 'S');
        }
        
        
        
        return LDMLPattern;
    }
    
    
    private String mapDateTime(String LDMLPattern)
    {   
        //M and MM in OO can mean months or minutes
        //do time first as it handles AM/PM
        LDMLPattern = mapTime(LDMLPattern);
        LDMLPattern = mapDate(LDMLPattern);
     //   System.out.println (LDMLPattern);
        
        return LDMLPattern;
    }
    
    
    //returns true if the string is found and not in quotes, otherwise false
    // the string must be found and the number of " before str must be even for method to return true
    private boolean translate(String pattern, String str)
    {
        boolean doTranslation = false;
        
        int index = pattern.indexOf(str);
        if (index == -1)
            return false;
        //else do translation provided it's not in quotes
        
        int QuoteCounter = 0;
        for (int i=0; i < index; i++)
        {
            if (pattern.charAt(i) == '"')
                QuoteCounter++;
        }
        
        if ((QuoteCounter % 2) == 0)
            doTranslation = true;
        
        return doTranslation;
    }
    
    //OO legacy stuff :
    // some pattern chars were localised in past
    // see http://l10n.openoffice.org/i18n_framework/LocaleData.html
    private String deLocalisePattern(String OOPattern)
    {
        if (m_localeStr == null)
            return OOPattern;
        
        if (m_localeStr.startsWith("de"))
        {
            OOPattern = OOPattern.replace('J', 'Y');
            OOPattern = OOPattern.replace('T', 'D');
        }
        else if (m_localeStr.startsWith("nl"))
        {
            OOPattern = OOPattern.replace('J', 'Y');
            OOPattern = OOPattern.replace('U', 'H');
        }
        else if (m_localeStr.startsWith("fr"))
        {
            OOPattern = OOPattern.replace('A', 'Y');
            OOPattern = OOPattern.replace('J', 'D');
        }
        else if (m_localeStr.startsWith("it"))
        {
            OOPattern = OOPattern.replace('A', 'Y');
            OOPattern = OOPattern.replace('G', 'D');
        }
        else if ((m_localeStr.startsWith("pt"))
        || (m_localeStr.startsWith("es"))
        || (m_localeStr.startsWith("ja")))   //ja_JP has AAAA even though it's not documented
        {
            OOPattern = OOPattern.replace('A', 'Y');
        }
        else if ((m_localeStr.startsWith("da"))
        || (m_localeStr.startsWith("nb"))
        || (m_localeStr.startsWith("nn"))
        || (m_localeStr.startsWith("no"))
        || (m_localeStr.startsWith("sv")))
        {
            OOPattern = OOPattern.replace('T', 'H');
        }
        else if (m_localeStr.startsWith("fi"))
        {
            OOPattern = OOPattern.replace('V', 'Y');
            OOPattern = OOPattern.replace('K', 'M');
            OOPattern = OOPattern.replace('P', 'D');
            OOPattern = OOPattern.replace('T', 'H');
        }
        
        return OOPattern;
    }
   
    
    //method to check if M or MM are for months or minutes, translates minutes if found
    private String doMinutes(String pattern)
    {
   //temp diagnostics      String orig = pattern;
        
        //from Eike :
        //Solely determined by context. The numberformatter does it by inspecting
        //the previous respectively next keyword. If the previous keyword is H or
        //HH, or the next keyword is S or SS, or a '[' character directly precedes
        //the M or MM, then minutes are used, else month.

        // if the first special char encountered before or after the M or MM is H or S then it's minutes
        //the logic assumes date is before time
        String dateChars = "DNYWQGERA";
        boolean bIsTime = false;
        boolean bInQuotes = false;
        int i =0;
        int start = 0;
        int pos_M = 0;
        while ((pos_M = pattern.indexOf('M',start)) != -1)  //there could be both Months and minutes in the pattern
        {
                //search preceding chars for the first special
                for (i=pos_M; i >0; i--)
                {
                    //skip stuff in quotes
                    if (pattern.charAt(i) == '"')
                    {
                        if (bInQuotes == false)
                            bInQuotes = true;
                        else
                            bInQuotes = false;
                        continue;
                    }
                    
                    //now check if first preceding special is a date char
                    if (dateChars.indexOf(pattern.charAt(i)) != -1)
                    {  //first preceding char is a date special
                        break;
                    }
                    else if ((pattern.charAt(i) == 'H') || (pattern.charAt(i) == 'S')
                        || (pattern.charAt(i) == 'h') || (pattern.charAt(i) == 's'))
                    {
                        bIsTime = true;
                        break;
                    }
                }
                
                //if first preceding special was not a time special then we still don't know so look after the M
                if (bIsTime == false)
                {
                    bInQuotes = false;
                    for (i=pos_M; i<pattern.length();i++)
                    {
                        //skip stuff in quotes
                        if (pattern.charAt(i) == '"')
                        {
                            bInQuotes = (bInQuotes == false ? true : false);
                            continue;
                        }
                        
                        //now check if first next special is a date char
                        if (dateChars.indexOf(pattern.charAt(i)) != -1)
                        {  //first next char is a date special
                            break;
                        }
                        else if ((pattern.charAt(i) == 'H') || (pattern.charAt(i) == 'S')
                        || (pattern.charAt(i) == 'h') || (pattern.charAt(i) == 's'))
                        {
                            bIsTime = true;
                            break;
                        }
                    }
                }
            
            if (bIsTime == true)
            {  
                break;
            }
            
            //skip to next occurance of M or MM
            start = pos_M +1;
            if (pattern.charAt(pos_M+1)=='M') start++;
        }
        
        if (bIsTime == true)
        { //can be M or MM
            char [] ca = pattern.toCharArray();
            ca [pos_M] = 'm';
            if ((pos_M < pattern.length()-1) &&  (pattern.charAt(pos_M+1) == 'M'))
                ca [pos_M+1] = 'm';
            
            pattern = String.copyValueOf(ca);
        }
           
  /*temp diagnostics          try {
        BufferedWriter out = new BufferedWriter(new FileWriter("date_time",true));
        out.write("LDML fmt = " + pattern + "\t\t OO.o fmt = " + orig +"\n");
        out.close();
    } catch (IOException e) {
    }*/
        return pattern;
    }
}
