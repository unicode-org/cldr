/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons
**********************************************************************
*/

package org.unicode.cldr.posix;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.FactoryConfigurationError;  
import javax.xml.parsers.ParserConfigurationException;
 

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.dev.tool.cldr.*;

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

   public void write ( PrintWriter out ) throws IOException {
 
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
