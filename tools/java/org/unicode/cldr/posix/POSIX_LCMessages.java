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

import org.unicode.cldr.util.LDMLUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class POSIX_LCMessages {

   String yesstr;
   String nostr;
   String yesexpr;
   String noexpr;
   String SearchLocation;
   Node n;

   public POSIX_LCMessages ( Document doc )
   {
         SearchLocation = "//ldml/posix/messages/yesstr";
         String s;
         n = LDMLUtilities.getNode(doc, SearchLocation);
         if ( n != null && (( s = LDMLUtilities.getNodeValue(n)) != null ))
            yesstr = POSIXUtilities.POSIXCharNameNP(s);
         else
            yesstr = "yes:y:Y";

         SearchLocation = "//ldml/posix/messages/nostr";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         if ( n != null && (( s = LDMLUtilities.getNodeValue(n)) != null ))
            nostr = POSIXUtilities.POSIXCharNameNP(s);
         else
            nostr = "no:n:N";

         SearchLocation = "//ldml/posix/messages/yesexpr";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         if ( n != null && (( s = LDMLUtilities.getNodeValue(n)) != null ))
            yesexpr = POSIXUtilities.POSIXCharNameNP(s);
         else
            yesexpr = "^[yY]";

         SearchLocation = "//ldml/posix/messages/noexpr";
         n = LDMLUtilities.getNode(doc, SearchLocation);
         if ( n != null && (( s = LDMLUtilities.getNodeValue(n)) != null ))
            noexpr = POSIXUtilities.POSIXCharNameNP(s);
         else
            noexpr = "^[nN]";

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
};
