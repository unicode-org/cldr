/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons
**********************************************************************
*/
package org.unicode.cldr.posix;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;

import org.unicode.cldr.util.LDMLUtilities;

public class POSIXUtilities {

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

   public static String POSIXDateTimeFormat ( String s )
   {

      //  This is an array of the POSIX date / time field descriptors and their corresponding representations
      //  in LDML.  We use these to replace the LDML fields with POSIX field descriptors.

      String[][] FieldDescriptors = {
           { "DDD",  "%j" },
           { "EEEE", "%A" },
           { "EEE",  "%a" },
           { "G",    "%N" },
           { "HH",   "%H" },
           { "MMMM", "%B" },
           { "MMM",  "%b" },
           { "MM",   "%m" },
           { "M",    "%m" },
           { "a",    "%p" },
           { "dd",   "%d" },
           { "d",    "%e" },
           { "hh",   "%I" },
           { "h",    "%I" },
           { "mm",   "%M" },
           { "yyyy", "%Y" },
           { "yy",   "%y" },
           { "zzzz", "%Z" },
           { "zzz",  "%Z" },
           { "zz",   "%Z" },
           { "z",    "%Z" },
           { "ss",   "%S" }
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
                  result.append(FieldDescriptors[i][1]);
                  replaced = true;
                  pos += FieldDescriptors[i][0].length();
               }
         }

         if ( !replaced )
         {
            if  ( s.charAt(pos) == '\'' )
            {
               inquotes = !inquotes;
               if  ( pos > 0 && s.charAt(pos-1) == '\'' )
                  result.append('\'');
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

   return result;

   }

   public static boolean isBetween ( int a, int b, int c ) {
     if ( ( a < b && b < c ) || ( c < b && b < a ) )
        return true;
     else
        return false;
   }

   public static String CollationSettingString ( Node node ) {
      StringBuffer result = new StringBuffer("");
      NamedNodeMap settings = node.getAttributes();
      Node s;

      String[] Strengths = { "primary","secondary","tertiary","quarternary","identical" };
      if ( (s = settings.getNamedItem("strength")) != null )
         for ( int i = 0 ; i < Strengths.length ; i++ )
            if ( s.getNodeValue().equals(Strengths[i]))
            {
               result.append("[strength ");
               result.append(i+1);
               result.append("]");
            }

      if ( (s = settings.getNamedItem("alternate")) != null )
         if ( s.getNodeValue().matches("non\\x2dignorable|shifted") )
            result.append("[alternate "+s.getNodeValue()+"]");

      if ( (s = settings.getNamedItem("backwards")) != null )
         if ( s.getNodeValue().matches("on") )
            result.append("[backwards 2]");

      if ( (s = settings.getNamedItem("caseFirst")) != null )
         if ( s.getNodeValue().matches("upper|lower|off") )
            result.append("[caseFirst "+s.getNodeValue()+"]");

      if ( (s = settings.getNamedItem("normalization")) != null )
         if ( s.getNodeValue().matches("on|off") )
            result.append("[normalization "+s.getNodeValue()+"]");

      if ( (s = settings.getNamedItem("caseLevel")) != null )
         if ( s.getNodeValue().matches("on|off") )
            result.append("[caseLevel "+s.getNodeValue()+"]");

      if ( (s = settings.getNamedItem("hiraganaQuarternary")) != null )
         if ( s.getNodeValue().matches("on|off") )
            result.append("[hiraganaQ "+s.getNodeValue()+"]");

      if ( (s = settings.getNamedItem("numeric")) != null )
         if ( s.getNodeValue().matches("on|off") )
            result.append("[numeric "+s.getNodeValue()+"]");

      return result.toString();

   }

   public static String CollationRuleString ( Node node ) {
      StringBuffer result = new StringBuffer("");
      String PreviousContext = new String("");
      NodeList rules = node.getChildNodes();

       for(int i = 0 ; i < rules.getLength() ; i++ ) {
           Node rule = rules.item(i);
           if(rule.getNodeType()==Node.ELEMENT_NODE) {
              String rule_name = XML2ICURuleString(rule,PreviousContext);
              if ( rule_name.length() > 0 )
                 if ( rule_name.startsWith("|"))
                    PreviousContext = rule_name.substring(1);
                 else
                 {
                    result.append(rule_name);
                    PreviousContext = "";
                 }
           }
        }

        return result.toString();
   }
    public static String XML2ICURuleString( Node rule , String PreviousContext)
    {

       String s = rule.getNodeName();
       StringBuffer result = new StringBuffer("");
       if ( s.equals("p") )
       {
          result.append("<");
          if (PreviousContext.length() > 0 )
             result.append(PreviousContext+"|");
          result.append(RuleData(LDMLUtilities.getNodeValue(rule)));
       }
       else if ( s.equals("s") )
       {
          result.append("<<");
          if (PreviousContext.length() > 0 )
             result.append(PreviousContext+"|");
          result.append(RuleData(LDMLUtilities.getNodeValue(rule)));
       }
       else if ( s.equals("t") )
       {
          result.append("<<<");
          if (PreviousContext.length() > 0 )
             result.append(PreviousContext+"|");
          result.append(RuleData(LDMLUtilities.getNodeValue(rule)));
       }
       else if ( s.equals("i") )
       {
          result.append("=");
          if (PreviousContext.length() > 0 )
             result.append(PreviousContext+"|");
          result.append(RuleData(LDMLUtilities.getNodeValue(rule)));
       }
       else if ( s.equals("x") )     return(CollationRuleString(rule));
       else if ( s.equals("extend"))
       {
          result.append("/");
          if (PreviousContext.length() > 0 )
             result.append(PreviousContext+"|");
          result.append(RuleData(LDMLUtilities.getNodeValue(rule)));
       }
       else if ( s.equals("context"))
       {
          result.append("|");
          result.append(RuleData(LDMLUtilities.getNodeValue(rule)));
       }
       else if ( s.equals("pc") )
       {
          String chars = LDMLUtilities.getNodeValue(rule);
          for ( int i = 0 ; i < chars.length() ; i++ )
          {
             result.append("<");
             result.append(RuleData(chars.substring(i,i+1)));
          }
       } 
       else if ( s.equals("sc") )
       {
          String chars = LDMLUtilities.getNodeValue(rule);
          for ( int i = 0 ; i < chars.length() ; i++ )
          {
             result.append("<<");
             result.append(RuleData(chars.substring(i,i+1)));
          }
       } 
       else if ( s.equals("tc") )
       {
          String chars = LDMLUtilities.getNodeValue(rule);
          for ( int i = 0 ; i < chars.length() ; i++ )
          {
             result.append("<<<");
             result.append(RuleData(chars.substring(i,i+1)));
          }
       } 
       else if ( s.equals("ic") )
       {
          String chars = LDMLUtilities.getNodeValue(rule);
          for ( int i = 0 ; i < chars.length() ; i++ )
          {
             result.append("=");
             result.append(RuleData(chars.substring(i,i+1)));
          }
       } 
       else if ( s.equals("reset"))
       {
          result.append("&");
          String WeightType = LDMLUtilities.getAttributeValue(rule,"before");

          if ( WeightType != null )
             if ( WeightType.equals("primary"))
                result.append("[before 1]");
             else if ( WeightType.equals("secondary"))
                result.append("[before 2]");
             else if ( WeightType.equals("tertiary"))
                result.append("[before 3]");

          for (Node child=rule.getFirstChild(); child!=null; child=child.getNextSibling())
             if (child.getNodeType()==Node.TEXT_NODE)
                result.append(RuleData(child.getNodeValue()));
             else if ( child.getNodeType()==Node.ELEMENT_NODE)
             {
                String[] LogicalPositions = { 
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

               s = child.getNodeName();

               
               if ( s.equals("cp")) // Explicit codepoint definition
               {
                  String Codepoint = LDMLUtilities.getAttributeValue(child,"hex");
                  char CodepointValue = 0;
                  for ( int i = 0 ; i< Codepoint.length(); i++ )
                  {
                     CodepointValue *= 16;
                     CodepointValue += Character.digit(Codepoint.charAt(i),16);
                  }
                  result.append(CodepointValue);
               }
               else
               {
                  boolean found = false;
                  for ( int i = 0 ; i < LogicalPositions.length && !found; i++ )
                     if ( s.equals(LogicalPositions[i]))
                     {
                        result.append("["+s.replaceAll("non_ignorable","regular").replaceAll("_"," ")+"]");
                        found = true;
                     }
               }
             }
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
}
