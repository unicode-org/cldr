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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.unicode.cldr.util.LDMLUtilities;

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

   public void write ( PrintWriter out ) {
 
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
}
