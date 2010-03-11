/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons
**********************************************************************
*/
package org.unicode.cldr.posix;

import java.text.StringCharacterIterator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

public class POSIXUtilities {

   private static UnicodeSet repertoire = new UnicodeSet(0x0000,0x10FFFF);
   private static CLDRFile char_fallbk;

   public static void setRepertoire ( UnicodeSet rep )
   {
      repertoire = rep;
   }

   public static void setCharFallback ( CLDRFile fallbk )
   {
      char_fallbk = fallbk;
   }


   public static String POSIXContraction ( String s )
   {
        int cp;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            result.append(POSIXCharName(cp));
        }
        return result.toString().replaceAll("><","-");
   }

   public static String POSIXCharName ( String s )
   {
        int cp;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            result.append(POSIXCharName(cp));
        }
        return result.toString();
   }

   public static String POSIXCharName ( int cp )
   {
        StringBuffer result = new StringBuffer();
        result.append("<");
        if (( cp >= 0x0041 && cp <= 0x005A ) ||
            ( cp >= 0x0061 && cp <= 0x007A )) // Latin letters
               result.append((char)cp);
        else if ( cp >= 0x0030 && cp <= 0x0039 ) // digits
        {
           String n = UCharacter.getExtendedName(cp);
           result.append(n.replaceAll(" ","_").replaceAll("DIGIT_","").toLowerCase());
        }
        else if ( cp == 0x0009 ) result.append("tab"); // Required elements for POSIX portable character set
        else if ( cp == 0x000A ) result.append("newline"); // Required elements for POSIX portable character set
        else if ( cp == 0x000B ) result.append("vertical-tab"); // Required elements for POSIX portable character set
        else if ( cp == 0x000C ) result.append("form-feed"); // Required elements for POSIX portable character set
        else if ( cp == 0x000D ) result.append("carriage-return"); // Required elements for POSIX portable character set
        else if ( cp == 0x0020 ) result.append("space"); // Required elements for POSIX portable character set
        else // everything else
        {
           String n = UCharacter.getExtendedName(cp);
           result.append(n.replaceAll(" ","_").replaceAll("<","").replaceAll(">","").toUpperCase());
        }

        int i = result.indexOf("_(");
        if ( i >= 0 )
           result.setLength(i);

        result.append(">");

        if ( !repertoire.contains(cp) )
        {
           System.out.println("WARNING: character "+result.toString()+" is not in the target codeset.");

           String substituteString = "";
           boolean SubFound = false;
           String SearchLocation = "//supplementalData/characters/character-fallback/character[@value='"+UCharacter.toString(cp)+"']/substitute";

           for ( Iterator<String> it = char_fallbk.iterator(SearchLocation,CLDRFile.ldmlComparator); it.hasNext() && !SubFound;)
           {
               String path = it.next();
               substituteString = char_fallbk.getStringValue(path);
               if ( repertoire.containsAll(substituteString) )
                   SubFound = true;
           }
 
           if ( SubFound )
           {
              System.out.println("	Substituted: "+POSIXUtilities.POSIXCharName(substituteString));
              result = new StringBuffer(POSIXUtilities.POSIXCharName(substituteString));
           }
           else
              System.out.println("	No acceptable substitute found. The resulting locale source may not compile.");
        }

        return result.toString();
   }    

   public static String POSIXCharFullName ( String s )
   {
        int cp;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            result.append(POSIXCharFullName(cp));
        }
        return result.toString();
   }

   public static String POSIXCharFullName ( int cp )
   {
        StringBuffer result = new StringBuffer();
        result.append("<");
        String n = UCharacter.getExtendedName(cp);
        result.append(n.replaceAll(" ","_").replaceAll("<","").replaceAll(">","").toUpperCase());

        int i = result.indexOf("_(");
        if ( i >= 0 )
           result.setLength(i);

        result.append(">");

        return result.toString();
   }    

// POSIXCharNameNP replaces all non-portable characters with their expanded POSIX character name.

   public static String POSIXCharNameNP ( String s )
   {
        int cp;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if ( cp <= 0x007F )
               result.append((char)cp);
            else
               result.append(POSIXCharName(cp));
        }
        return result.toString();
   }

   public static String POSIXDateTimeFormat ( String s , boolean UseAltDigits, POSIXVariant variant )
   {

      //  This is an array of the POSIX date / time field descriptors and their corresponding representations
      //  in LDML.  We use these to replace the LDML fields with POSIX field descriptors.
               
      String[][] FieldDescriptors = {
           { "/",    "<SOLIDUS>" , "<SOLIDUS>" , "<SOLIDUS>" },
           { "DDD",  "%j" , "%j" , "%j" },
           { "EEEE", "%A" , "%A" , "%A" },
           { "EEE",  "%a" , "%a" , "%a" },
           { "G",    "%N" , "%N" , "%N" },
           { "HH",   "%H" , "%OH" , "%H" },
           { "H",    "%H" , "%OH" , "%k" }, //solaris defines exact mapping for "H""
           { "KK",   "%I" , "%OI" , "%I" },
           { "K",    "%I" , "%OI" , "%l" }, 
           { "MMMM", "%B" , "%B" , "%B" },
           { "MMM",  "%b" , "%b" , "%b" },
           { "MM",   "%m" , "%Om" , "%m" },
           { "M",    "%m" , "%Om" , "%m" },
           { "VVVV", "%Z" , "%Z" , "%Z" },
           { "V",    "%Z" , "%Z" , "%Z" },
           { "a",    "%p" , "%p" , "%p" },
           { "dd",   "%d" , "%Od" , "%d" },
           { "d",    "%e" , "%Oe" , "%e" },
           { "hh",   "%I" , "%OI" , "%I" },
           { "h",    "%I" , "%OI" , "%l" }, //solaris defines exact mapping for "h"
           { "kk",   "%H" , "%OH" , "%H" },
           { "k",    "%H" , "%OH" , "%k" },
           { "mm",   "%M" , "%OM" , "%M" },
           { "m",    "%M" , "%OM" , "%M" },
           { "vvvv", "%Z" , "%Z" , "%Z" },
           { "v",    "%Z" , "%Z" , "%Z" },
           { "yyyy", "%Y" , "%Oy" , "%Y" },
           { "yy",   "%y" , "%Oy" , "%y" },
           { "y",    "%Y" , "%Oy" , "%Y" },
           { "zzzz", "%Z" , "%Z" , "%Z" },
           { "zzz",  "%Z" , "%Z" , "%Z" },
           { "zz",   "%Z" , "%Z" , "%Z" },
           { "z",    "%Z" , "%Z" , "%Z" },
           { "ss",   "%S" , "%OS" , "%S" },
           { "s",    "%S" , "%OS" ,"%S" }
      };

      boolean inquotes = false;
      StringBuffer result = new StringBuffer("");

      for (int pos = 0; pos < s.length() ; ) 
      {
         boolean replaced = false;
         for (int i = 0; i < FieldDescriptors.length && !replaced && !inquotes; i++ )
         {
               if ( s.indexOf(FieldDescriptors[i][0],pos) == pos)
               {
                  if ( UseAltDigits )
                     result.append(FieldDescriptors[i][2]);
                 else if ( variant.platform.equals(POSIXVariant.SOLARIS))
                     result.append(FieldDescriptors[i][3]);
                  else
                     result.append(FieldDescriptors[i][1]);
                  replaced = true;
                  pos += FieldDescriptors[i][0].length();
               }
         }

         if ( !replaced )
         {
            if  ( s.charAt(pos) == '\'' )
            {
               if  ( pos < ( s.length() - 1) && s.charAt(pos+1) == '\'' )
               {
                    result.append('\'');
                    pos++;
               }
               else
                  inquotes = !inquotes;
            }
            else
               result.append(s.charAt(pos));
         pos++;
         }
      }
      return result.toString();

   }


   public static String POSIXGrouping ( String grouping_pattern ) {

   //  Parse the decimal pattern to get the number of digits to use in the POSIX style pattern.

   int i = grouping_pattern.indexOf(".");
   int j;
   boolean first_grouping = true;
   String result;

   if ( i < 0 )
      result = "-1";
   else
      {
         result = new String();
         while( (j = grouping_pattern.lastIndexOf(",",i-1)) > 0 )
         {
            if ( !first_grouping )
               result = result.concat(";");
            Integer num_digits = new Integer(i-j-1);
            result = result.concat(num_digits.toString());

            first_grouping = false;
            i = j;
         }
      }

   if ( result.length() == 0 )
      result = "-1";

   return result;

   }

   public static boolean isBetween ( int a, int b, int c ) {
     return ( ( a < b && b < c ) || ( c < b && b < a ) );
   }

   public static String CollationSettingString ( CLDRFile collrules, String path ) {
      StringBuffer result = new StringBuffer("");
      String settings = collrules.getFullXPath(path);
      XPathParts xp = new XPathParts();
      xp.set(settings);
      
      String[] Strengths = { "primary","secondary","tertiary","quarternary","identical" };
      if ( xp.containsAttribute("strength") )
         for ( int i = 0 ; i < Strengths.length ; i++ )
            if ( xp.getAttributeValue(0,"strength").equals(Strengths[i]))
            {
               result.append("[strength ");
               result.append(i+1);
               result.append("]");
            }

      if ( xp.containsAttribute("alternate")) {
          String value = xp.getAttributeValue(0,"alternate");
          if ( value.matches("non\\x2dignorable|shifted") )
            result.append("[alternate "+value+"]");
      }
      
      if ( xp.containsAttribute("backwards")) {
          String value = xp.getAttributeValue(0,"backwards");
          if ( value.matches("on"))
              result.append("[backwards 2]");
      }

      if ( xp.containsAttribute("caseFirst")) {
          String value = xp.getAttributeValue(0,"caseFirst");
          if ( value.matches("upper|lower|off") )
              result.append("[casefirst "+value+"]");
      }
      
      if ( xp.containsAttribute("normalization")) {
          String value = xp.getAttributeValue(0,"normalization");
          if ( value.matches("on|off"))
              result.append("[normalization "+value+"]");
      }

      if ( xp.containsAttribute("caseLevel")) {
          String value = xp.getAttributeValue(0,"caseLevel");
          if ( value.matches("on|off"))
              result.append("[caseLevel "+value+"]");
      }
      if ( xp.containsAttribute("hiraganaQuarternary")) {
          String value = xp.getAttributeValue(0,"hiraganaQuarternary");
          if ( value.matches("on|off"))
            result.append("[hiraganaQ "+value+"]");
      }

      if ( xp.containsAttribute("numeric")) {
          String value = xp.getAttributeValue(0,"numeric");
          if ( value.matches("on|off"))
            result.append("[numeric "+value+"]");
      }


      return result.toString();

   }

   public static String CollationRuleString ( CLDRFile collrules, String path ) {
      StringBuffer result = new StringBuffer("");
      String previousContext = new String("");
      XPathParts xp = new XPathParts();
      
      Iterator<String> ri;
       for(ri=collrules.iterator(path) ; ri.hasNext() ;  ) {
           String rulePath = collrules.getFullXPath(ri.next());
           xp.set(rulePath);
           String ruleName = xp.getElement(-1);
           String ruleValue = collrules.getStringValue(rulePath);
           String before;
           if (xp.containsAttribute("before")) {
               before = xp.findAttributeValue(ruleName,"before");
           } else {
               before = null;
           }
           String ICURuleString = XML2ICURuleString(ruleName,ruleValue,before,previousContext);
           if ( ICURuleString.length() > 0 )
               if ( ICURuleString.startsWith("|"))
                   previousContext = ICURuleString.substring(1);
               else
               {
                   result.append(ICURuleString);
                   previousContext = "";
               }
           }

        return result.toString();
   }
    
   public static String XML2ICURuleString( String ruleName, String ruleValue, String before, String previousContext)
   {
       String [] logicalPositionsArray = { 
           "first_variable",
           "first_trailing",
           "first_tertiary_ignorable",
           "first_secondary_ignorable",
           "first_primary_ignorable",
           "first_non_ignorable",
           "last_variable",
           "last_trailing",
           "last_tertiary_ignorable",
           "last_secondary_ignorable",
           "last_primary_ignorable",
           "last_non_ignorable" };

       Set<String> logicalPositions = new HashSet<String>();
       logicalPositions.clear();
       for ( int i = 0 ; i < logicalPositionsArray.length ; i++) {
           logicalPositions.add(logicalPositionsArray[i]);
       }
       
       StringBuffer result = new StringBuffer("");
       if ( ruleName.equals("p") )
       {
          result.append("<");
          if (previousContext.length() > 0 )
             result.append(previousContext+"|");
          result.append(RuleData(ruleValue));
       }
       else if ( ruleName.equals("s") )
       {
          result.append("<<");
          if (previousContext.length() > 0 )
             result.append(previousContext+"|");
          result.append(RuleData(ruleValue));
       }
       else if ( ruleName.equals("t") )
       {
          result.append("<<<");
          if (previousContext.length() > 0 )
             result.append(previousContext+"|");
          result.append(RuleData(ruleValue));
       }
       else if ( ruleName.equals("i") )
       {
          result.append("=");
          if (previousContext.length() > 0 )
             result.append(previousContext+"|");
          result.append(RuleData(ruleValue));
       }
       else if ( ruleName.equals("extend"))
       {
          result.append("/");
          if (previousContext.length() > 0 )
             result.append(previousContext+"|");
          result.append(RuleData(ruleValue));
       }
       else if ( ruleName.equals("context"))
       {
          result.append("|");
          result.append(RuleData(ruleValue));
       }
       else if ( ruleName.equals("pc") )
       {
          for ( int i = 0 ; i < ruleValue.length() ; i++ )
          {
             result.append("<");
             result.append(RuleData(ruleValue.substring(i,i+1)));
          }
       } 
       else if ( ruleName.equals("sc") )
       {
          for ( int i = 0 ; i < ruleValue.length() ; i++ )
          {
             result.append("<<");
             result.append(RuleData(ruleValue.substring(i,i+1)));
          }
       } 
       else if ( ruleName.equals("tc") )
       {
          for ( int i = 0 ; i < ruleValue.length() ; i++ )
          {
             result.append("<<<");
             result.append(RuleData(ruleValue.substring(i,i+1)));
          }
       } 
       else if ( ruleName.equals("ic") )
       {
          for ( int i = 0 ; i < ruleValue.length() ; i++ )
          {
             result.append("=");
             result.append(RuleData(ruleValue.substring(i,i+1)));
          }
       } 
       else if ( ruleName.equals("reset"))
       {
          result.append("&");

          if ( before != null )
             if ( before.equals("primary"))
                result.append("[before 1]");
             else if ( before.equals("secondary"))
                result.append("[before 2]");
             else if ( before.equals("tertiary"))
                result.append("[before 3]");

          result.append(RuleData(ruleValue));
       }
       else if ( logicalPositions.contains(ruleName)) {
           
          result.append("&[");
          result.append(ruleName.replaceAll("non_ignorable","regular").replace('_',' '));
          result.append("]");
 
               
       
      }
       
       return(result.toString());
    }

   public static String RuleData ( String DataString ) {
       StringBuffer result = new StringBuffer(DataString);
       for ( int i = 0 ; i < result.length() ; i++ )
       {
          String Current = result.substring(i,i+1);
          if ( Current.matches("[\\ \\p{Punct}]"))
          {
             result.insert(i,"\\");
             i++;
          }
       }

       return(result.toString()
                    .replaceAll("&quot;","''")
                    .replaceAll("&amp;", "\\\\&")
                    .replaceAll("&lt;",  "\\\\<")
                    .replaceAll("&gt;",  "\\\\>"));
  }

   public static String POSIXYesNoExpr ( String s )
   {
      StringBuffer result = new StringBuffer();
      String [] YesNoElements;
      YesNoElements = s.split(":");
      for ( int i = 0 ; i < YesNoElements.length ; i++ )      
      {
         String cur = YesNoElements[i];
         if ( cur.length() > 1 && cur.toLowerCase().equals(cur) )
         {   
            if ( result.length() > 0 )
               result.append(")|(");
            else
               result.append("^((");

            StringCharacterIterator si = new StringCharacterIterator(cur);
            boolean OptLastChars = false;
            for ( char c = si.first(); c != StringCharacterIterator.DONE; c = si.next() )
            {
               if ( c != Character.toUpperCase(c) )
               {
                  if ( si.getIndex() == 1 )
                  {
                     result.append("(");
                     OptLastChars = true;   
                  }
                  result.append("[");
                  result.append(c);
                  result.append(Character.toUpperCase(c));
                  result.append("]");
               }
               else
                  result.append(c);
            }
            if ( OptLastChars )
               result.append(")?");
         }
      }
      result.append("))");
      return(POSIXCharNameNP(result.toString()));
   }
}
