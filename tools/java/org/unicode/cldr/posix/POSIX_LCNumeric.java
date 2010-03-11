/*
**********************************************************************
* Copyright (c) 2002-2010, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons
**********************************************************************
*/
package org.unicode.cldr.posix;

import java.io.PrintWriter;

import org.unicode.cldr.util.CLDRFile;

public class POSIX_LCNumeric {
   String decimal_point;
   String thousands_sep;
   String grouping;

   public POSIX_LCNumeric ( CLDRFile doc ) {

   decimal_point = POSIXUtilities.POSIXCharName(doc.getWinningValue("//ldml/numbers/symbols/decimal"));
   thousands_sep = POSIXUtilities.POSIXCharName(doc.getWinningValue("//ldml/numbers/symbols/group"));
   String grouping_pattern = doc.getWinningValue("//ldml/numbers/decimalFormats/decimalFormatLength/decimalFormat[@type='standard']/pattern[@type='standard']");
   
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
