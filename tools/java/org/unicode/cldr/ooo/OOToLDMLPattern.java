/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;


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
 * y            YY              Year as 00-99
 * yy           YYYY            Year as 1900-2078
 * ww           WW              Calendar week
 * QQQ   	Q               Quarterly as Q1 to Q4
 * QQQQ 	QQ              Quarterly as 1st quarter to 4th quarter
 * MTSH         G               Era on the Japanese Gengou calendar, single character (possible values are: M, T, S, H)  (workaround)
 * G            GG              Era, abbreviation
 * GGGG 	GGG             Era, full name
 * y            E               Number of the year within an era, without a leading zero for single-digit years
 * yy           EE or R 	Number of the year within an era, with a leading zero for single-digit years
 * GGGGyy       RR or GGGEE 	Era, full name and year
 * AAA or AAAA  AAA or AAAA 	Arabic Islamic format (wotkaround)
 *
 * Seems to be no way to get AD/BC
 *
 * Times
 *
 * H            h or H          Hours as 0-23
 * HH           hh or HH        Hours as 00-23
 * K            h with AM/PM    Hours as 0-11
 * KK           hh with AM/PM   Hours as 00-21
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
    
    OOToLDMLPattern(String localeStr)
    {
        m_localeStr = localeStr;
        
    }
    
    public String map(String OOPattern, String type)
    {
        if (OOPattern == null)
            return null;
        
        m_type = type;
        String LDMLPattern = deLocalisePattern(OOPattern);
        
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
        return LDMLPattern;
    }
    
    
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
            LDMLPattern = LDMLPattern.replaceAll("YYYY", "yy");
        
        else if (translate(LDMLPattern, "YY"))
            LDMLPattern = LDMLPattern.replaceAll("YY", "y");
        
        //month patterns are same , do nothing
        
        //day patterns
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
        
        else if (translate(LDMLPattern, "DD"))
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
        boolean bIsAmPM = false;
        //do AM/PM first as the Ms in AM/PM can be ambiguous
        if (translate(LDMLPattern, "AM/PM"))
        {
            LDMLPattern = LDMLPattern.replaceAll("AM/PM", "a");
            bIsAmPM = true;
        }
        
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
            if (bIsAmPM == true)
                LDMLPattern = LDMLPattern.replace('h', 'K');
            else
                LDMLPattern = LDMLPattern.replace('h', 'H');
        }
        else if (translate(LDMLPattern, "H"))
        {   //takes care of H and HH where AM/PM is present, apttern is same if not present
            if (bIsAmPM == true)
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
        || (m_localeStr.startsWith("es")))
        {
            OOPattern = OOPattern.replace('A', 'Y');
        }
        else if ((m_localeStr.startsWith("da"))
        || (m_localeStr.startsWith("nb"))
        || (m_localeStr.startsWith("nn"))
        || (m_localeStr.startsWith("no"))
        || (m_localeStr.startsWith("sv"))
        || (m_localeStr.startsWith("fi")))
        {
            OOPattern = OOPattern.replace('T', 'H');
        }
        else if (m_localeStr.startsWith("fi"))
        {
            OOPattern = OOPattern.replace('V', 'Y');
            OOPattern = OOPattern.replace('K', 'M');
            OOPattern = OOPattern.replace('P', 'D');
        }
        
        return OOPattern;
    }
   
    
    //method to check if M or MM are for months or minutes, translates minutes if found
    private String doMinutes(String pattern)
    {
        //a bit of a hack:
        //if M has a H before it then it means minutes, have verified this
        // +> the first M encountered after H is minutes
   
        int pos_H = pattern.indexOf('H');
        if (pos_H == -1)
            pos_H = pattern.indexOf('h');
        
        //can be M or MM
        if (pos_H != -1)
        {
            int index1 = pattern.indexOf('M', pos_H);
            int index2 = pattern.indexOf("MM", pos_H);
            char [] ca = pattern.toCharArray();
            
            if (index2 != -1)
            {
                ca [index2] = 'm';
                ca [index2+1] = 'm';
            }
            else if (index1 != -1)
                ca [index1] = 'm';
            
            pattern = String.copyValueOf(ca);
        }
        return pattern;
    }
}
