/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons
**********************************************************************
*/

package org.unicode.cldr.posix;

import java.io.PrintWriter;
import java.util.Locale;

import org.unicode.cldr.util.CLDRFile;

public class POSIX_LCMessages {

   String yesstr;
   String nostr;
   String yesexpr;
   String noexpr;
   String SearchLocation;
   Locale loc;

   public POSIX_LCMessages ( CLDRFile doc , String locale_name , POSIXVariant variant )
   {
         int i = locale_name.indexOf('_');
         if ( i > 0 )
            loc = new Locale(locale_name.substring(0,i));
         else
            loc = Locale.getDefault();

         SearchLocation = "//ldml/posix/messages/yesstr";
         String s;
         s = doc.getWinningValue(SearchLocation);
         if ( s != null ) {
            StringBuffer buf;
            if ( variant.yesno.equals("short"))
            {
               i = s.indexOf(":");
               if ( i > 0 )
                  buf = new StringBuffer(s.substring(0,i));
               else
                  buf = new StringBuffer(s);
            }
            else
            {
               buf = new StringBuffer(s);
               if ( ! s.equals(s.toUpperCase(loc)))
                 buf.append(":"+s.toUpperCase(loc));
               if ( ! s.startsWith("yes:"))
                 buf.append(":yes:y:YES:Y");
            }
            yesstr = POSIXUtilities.POSIXCharNameNP(buf.toString());
            yesexpr = POSIXUtilities.POSIXYesNoExpr(buf.toString());
         }
         else if ( variant.yesno.equals("short"))
         {
            yesstr = "yes";
            yesexpr = POSIXUtilities.POSIXYesNoExpr(yesstr);
         }
         else
         {
            yesstr = "yes:y:YES:Y";
            yesexpr = POSIXUtilities.POSIXYesNoExpr(yesstr);
         }

         SearchLocation = "//ldml/posix/messages/nostr";
         s = doc.getWinningValue(SearchLocation);
         if ( s != null ) {
            StringBuffer buf;
            if ( variant.yesno.equals("short")) {
               i = s.indexOf(":");
               if ( i > 0 )
                  buf = new StringBuffer(s.substring(0,i));
               else
                  buf = new StringBuffer(s);
            }
            else
            {
               buf = new StringBuffer(s);
               if ( ! s.equals(s.toUpperCase(loc)))
                 buf.append(":"+s.toUpperCase(loc));
               if ( ! s.startsWith("no:"))
                 buf.append(":no:n:NO:N");
            }
            nostr = POSIXUtilities.POSIXCharNameNP(buf.toString());
            noexpr = POSIXUtilities.POSIXYesNoExpr(buf.toString());
         }
         else if ( variant.yesno.equals("short"))
         {
            nostr = "no";
            noexpr = POSIXUtilities.POSIXYesNoExpr(nostr);
         }
         else
         {
            nostr = "no:n:NO:N";
            noexpr = POSIXUtilities.POSIXYesNoExpr(nostr);
         }

   }

   public void write ( PrintWriter out ){
 
      out.println("*************");
      out.println("LC_MESSAGES");
      out.println("*************");
      out.println();

      // yesstr
      out.println("yesstr   \"" + yesstr + "\"");
      out.println();

      // nostr
      out.println("nostr    \"" + nostr + "\"");
      out.println();

      // yesexpr
      out.println("yesexpr  \"" + yesexpr + "\"");
      out.println();

      // noexpr
      out.println("noexpr   \"" + noexpr + "\"");
      out.println();

      out.println();
      out.println("END LC_MESSAGES");
   }
}
