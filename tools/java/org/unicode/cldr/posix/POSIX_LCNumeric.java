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

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.SortedBag;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.dev.tool.cldr.*;


public class POSIX_LCNumeric {
   String decimal_point;
   String thousands_sep;
   String grouping;

   public POSIX_LCNumeric ( Document doc ) {

   Node n = LDMLUtilities.getNode(doc, "//ldml/numbers/symbols/decimal");
   decimal_point = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));
   n = LDMLUtilities.getNode(doc, "//ldml/numbers/symbols/group");
   thousands_sep = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));
   n = LDMLUtilities.getNode(doc, "//ldml/numbers/decimalFormats/decimalFormatLength/decimalFormat/pattern");
   String grouping_pattern = LDMLUtilities.getNodeValue(n);

   grouping = POSIXUtilities.POSIXGrouping( grouping_pattern );

   }   

   public void write ( PrintWriter out ) throws IOException {
 
      out.println("*************");
      out.println("LC_NUMERIC");
      out.println("*************");
      out.println();
      out.println("decimal_point     \"" + decimal_point + "\"");
      out.println("thousands_sep     \"" + thousands_sep + "\"");
      out.println("grouping          " + grouping );
      out.println();
      out.println("END LC_NUMERIC");
      out.println();
      out.println();

   }
};
