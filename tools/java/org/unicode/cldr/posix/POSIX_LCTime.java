/*
**********************************************************************
* Copyright (c) 2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons
**********************************************************************
*/

package org.unicode.cldr.posix;

import java.io.PrintWriter;
import java.lang.Character;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.unicode.cldr.util.LDMLUtilities;


public class POSIX_LCTime {
   String abday[];
   String day[];
   String abmon[];
   String mon[];
   String d_t_fmt;
   String d_fmt;
   String t_fmt;
   String am_pm[];
   String t_fmt_ampm;
   String alt_digits[];


   public POSIX_LCTime ( Document doc )
   {
      abday = new String[7];
      day = new String[7];
      abmon = new String[12];
      mon = new String[12];
      am_pm = new String[2];
      alt_digits = new String[100];

      String[] days = { "sun", "mon", "tue", "wed", "thu", "fri", "sat" };
      String SearchLocation;
      Node n;

      for ( int i = 0 ; i < 7 ; i++ )
      {
         // Get each value for abbreviated day names ( abday )
         SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/days/dayContext[@type='format']/dayWidth[@type='abbreviated']/day[@type='"+days[i]+"']";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         abday[i] = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));

         // Get each value for full month names ( day )
         SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/days/dayContext[@type='format']/dayWidth[@type='wide']/day[@type='"+days[i]+"']";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         day[i] = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));
      }

      for ( int i = 0 ; i < 12 ; i++ )
      {
         // Get each value for abbreviated month names ( abmon )
         SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/months/monthContext[@type='format']/monthWidth[@type='abbreviated']/month[@type='"+String.valueOf(i+1)+"']";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         abmon[i] = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));

         // Get each value for full month names ( mon )
         SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/months/monthContext[@type='format']/monthWidth[@type='wide']/month[@type='"+String.valueOf(i+1)+"']";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         mon[i] = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));
      }

      // alt_digits
         alt_digits[0] = "";
         SearchLocation = "//ldml/numbers/symbols/nativeZeroDigit";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         if ( (n != null) &&  !(LDMLUtilities.getNodeValue(n).equals("0")))
         {
            Character ThisDigit;
            String NativeZeroDigit = LDMLUtilities.getNodeValue(n);
            alt_digits[0] = POSIXUtilities.POSIXCharName(NativeZeroDigit);
            char base_value = NativeZeroDigit.charAt(0);
            for ( short i = 1 ; i < 10 ; i++ )
                alt_digits[i] = POSIXUtilities.POSIXCharName(Character.toString(((char)((short)base_value + i))));
            for ( short i = 10 ; i < 100 ; i++ )
                alt_digits[i] = alt_digits[i/10] + alt_digits[i%10];
         }

      // t_fmt - 
         SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='medium']/timeFormat/pattern";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         t_fmt = POSIXUtilities.POSIXDateTimeFormat(LDMLUtilities.getNodeValue(n),alt_digits[0].length()>0);

        
         if ( t_fmt.indexOf("%p") >= 0 )
            t_fmt_ampm = t_fmt;
         else
            t_fmt_ampm = "";

      // d_fmt - 
         SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/dateFormats/dateFormatLength[@type='short']/dateFormat/pattern";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         d_fmt = POSIXUtilities.POSIXDateTimeFormat(LDMLUtilities.getNodeValue(n),alt_digits[0].length()>0);

      // d_t_fmt - 
         SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/dateTimeFormatLength/dateTimeFormat/pattern";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         d_t_fmt = LDMLUtilities.getNodeValue(n);

         SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/timeFormats/timeFormatLength[@type='long']/timeFormat/pattern";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         d_t_fmt = d_t_fmt.replaceAll("\\{0\\}",LDMLUtilities.getNodeValue(n));


         SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/dateFormats/dateFormatLength[@type='long']/dateFormat/pattern";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         d_t_fmt = d_t_fmt.replaceAll("\\{1\\}",LDMLUtilities.getNodeValue(n));

         d_t_fmt = POSIXUtilities.POSIXDateTimeFormat(d_t_fmt,alt_digits[0].length()>0);
         d_t_fmt = POSIXUtilities.POSIXCharNameNP(d_t_fmt);


      // am_pm
         SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/am";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         am_pm[0] = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));

         SearchLocation = "//ldml/dates/calendars/calendar[@type='gregorian']/pm";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         am_pm[1] = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));
       
   }

   
   public void write ( PrintWriter out ) {
 
      out.println("*************");
      out.println("LC_TIME");
      out.println("*************");
      out.println();


      // abday
      out.print("abday   \"");
      for ( int i = 0 ; i < 7 ; i++ )
      {
         out.print(abday[i]);
         if ( i < 6 )
         {
            out.println("\";/");
            out.print("        \"");
         }
         else
            out.println("\"");
      }
      out.println();

      // day
      out.print("day     \"");
      for ( int i = 0 ; i < 7 ; i++ )
      {
         out.print(day[i]);
         if ( i < 6 )
         {
            out.println("\";/");
            out.print("        \"");
         }
         else
            out.println("\"");
      }
      out.println();

      // abmon
      out.print("abmon   \"");
      for ( int i = 0 ; i < 12 ; i++ )
      {
         out.print(abmon[i]);
         if ( i < 11 )
         {
            out.println("\";/");
            out.print("        \"");
         }
         else
            out.println("\"");
      }
      out.println();


      // mon
      out.print("mon     \"");
      for ( int i = 0 ; i < 12 ; i++ )
      {
         out.print(mon[i]);
         if ( i < 11 )
         {
            out.println("\";/");
            out.print("        \"");
         }
         else
            out.println("\"");
      }
      out.println();


      // d_fmt
      out.println("d_fmt    \"" + d_fmt + "\"");
      out.println();

      // t_fmt
      out.println("t_fmt    \"" + t_fmt + "\"");
      out.println();

      // d_t_fmt
      out.println("d_t_fmt  \"" + d_t_fmt + "\"");
      out.println();

      // am_pm
      out.println("am_pm    \"" + am_pm[0] + "\";\"" + am_pm[1] + "\"" );
      out.println();

      // t_fmt_ampm
      out.println("t_fmt_ampm  \"" + t_fmt_ampm + "\"");
      out.println();

      // alt_digits
      if ( ! alt_digits[0].equals("") )
      {
         out.print("alt_digits \"");
         for ( int i = 0 ; i < 100 ; i++ )
         {
            out.print(alt_digits[i]);
            if ( i < 99 )
            {
               out.println("\";/");
               out.print("           \"");
            }
            else
               out.println("\"");
         }
      }
      out.println("END LC_TIME");

   }
};
