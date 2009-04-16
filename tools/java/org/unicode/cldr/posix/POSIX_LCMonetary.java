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
import java.lang.Float;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.unicode.cldr.util.LDMLUtilities;

public class POSIX_LCMonetary {
   String int_curr_symbol;
   String currency_symbol;
   String mon_decimal_point;
   String mon_thousands_sep;
   String mon_grouping;
   String positive_sign;
   String negative_sign;
   String frac_digits;
   String p_cs_precedes;
   String p_sep_by_space;
   String p_sign_posn;
   String n_cs_precedes;
   String n_sep_by_space;
   String n_sign_posn;
   String int_frac_digits;
   String int_p_cs_precedes;
   String int_p_sep_by_space;
   String int_p_sign_posn;
   String int_n_cs_precedes;
   String int_n_sep_by_space;
   String int_n_sign_posn;
   private static final int POSITIVE = 0;
   private static final int NEGATIVE = 1;

   public POSIX_LCMonetary ( Document doc , Document supp , POSIXVariant variant ) {

   Node n; 

   n = LDMLUtilities.getNode(doc, "//ldml/numbers/currencyFormats/currencyFormatLength/currencyFormat/pattern");
   String grouping_pattern = LDMLUtilities.getNodeValue(n);

   String [] monetary_formats = new String[2];
   if ( grouping_pattern.indexOf(";") > 0 )
      monetary_formats = grouping_pattern.split(";",2);
   else
   {
      monetary_formats[POSITIVE] = grouping_pattern;
      monetary_formats[NEGATIVE] = "-" + grouping_pattern;
   }

   mon_grouping = POSIXUtilities.POSIXGrouping( monetary_formats[POSITIVE] );

   n = LDMLUtilities.getNode(doc, "//ldml/identity/territory");
   String territory = LDMLUtilities.getAttributeValue(n,"type");

   if ( variant.currency.equals("default") )
   {
      n = LDMLUtilities.getNode(supp, "//supplementalData/currencyData/region[@iso3166='"+territory+"']/currency");
      int_curr_symbol = LDMLUtilities.getAttributeValue(n,"iso4217");
   }
   else
      int_curr_symbol = variant.currency;

   String tmp_currency_symbol;
   n = LDMLUtilities.getNode(doc, "//ldml/numbers/currencies/currency[@type='"+int_curr_symbol+"']/symbol");
   if ( n != null )
       tmp_currency_symbol = LDMLUtilities.getNodeValue(n);
   else
       tmp_currency_symbol = int_curr_symbol;
   

// Check to see if currency symbol has a choice pattern

   if ( tmp_currency_symbol.indexOf("|") >= 0 )
   {
       String [] choices = tmp_currency_symbol.split("\\u007c");
       for ( int i = choices.length - 1 ; i >= 0 ; i-- )
       {
          String [] numvalue = choices[i].split("[<\\u2264]",2);
          Float num = Float.valueOf(numvalue[0]);
          Float ten = new Float(10);
          if ( num.compareTo(ten) <= 0 || i == 0 )
          {
             currency_symbol = POSIXUtilities.POSIXCharName(numvalue[1]);
             i = 0;
          }
       }
   }
   else
      currency_symbol = POSIXUtilities.POSIXCharName(tmp_currency_symbol);

   n = LDMLUtilities.getNode(doc, "//ldml/numbers/currencies/currency[@type='"+int_curr_symbol+"']/decimal");
   if ( n == null )
      n = LDMLUtilities.getNode(doc, "//ldml/numbers/symbols/currencySeparator");
   if ( n == null )
      n = LDMLUtilities.getNode(doc, "//ldml/numbers/symbols/decimal");

   mon_decimal_point = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));

   n = LDMLUtilities.getNode(doc, "//ldml/numbers/currencies/currency[@type='"+int_curr_symbol+"']/group");
   if ( n == null )
      n = LDMLUtilities.getNode(doc, "//ldml/numbers/symbols/currencyGroup");
   if ( n == null )
      n = LDMLUtilities.getNode(doc, "//ldml/numbers/symbols/group");

   mon_thousands_sep = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));

   n = LDMLUtilities.getNode(supp, "//supplementalData/currencyData/fractions/info[@iso4217='"+int_curr_symbol+"']");
   if ( n == null )
      n = LDMLUtilities.getNode(supp, "//supplementalData/currencyData/fractions/info[@iso4217='DEFAULT']");

   frac_digits = LDMLUtilities.getAttributeValue(n,"digits");
   int_frac_digits = frac_digits;

   
   if ( monetary_formats[POSITIVE].indexOf('+') >= 0 )
   {
      n = LDMLUtilities.getNode(doc, "//ldml/numbers/symbols/plusSign");
      positive_sign = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));
   }
   else
      positive_sign = "";
   
   if ( monetary_formats[NEGATIVE].indexOf('-') >= 0 )
   {
      n = LDMLUtilities.getNode(doc, "//ldml/numbers/symbols/minusSign");
      negative_sign = POSIXUtilities.POSIXCharName(LDMLUtilities.getNodeValue(n));
   }
   else
      negative_sign = POSIXUtilities.POSIXCharName("-");

   
   // Parse Positive Monetary Formats
   int currency_symbol_position = monetary_formats[POSITIVE].indexOf('\u00a4');
   int decimal_symbol_position = monetary_formats[POSITIVE].indexOf('.');
   int sign_position = monetary_formats[POSITIVE].indexOf('+');
   int leftparen_position = monetary_formats[POSITIVE].lastIndexOf('(',decimal_symbol_position);
   int rightparen_position = monetary_formats[POSITIVE].indexOf(')',decimal_symbol_position);
   int space_position = monetary_formats[POSITIVE].indexOf(' ');

   if (space_position == -1)
      space_position = monetary_formats[POSITIVE].indexOf('\u00a0');

   if ( currency_symbol_position >= 0) 
      if ( currency_symbol_position > decimal_symbol_position )
         p_cs_precedes = "0";
      else
         p_cs_precedes = "1";
   else
      p_cs_precedes = "-1";
   
   int_p_cs_precedes = p_cs_precedes;

   if ( positive_sign == "" )
   {
     if (( leftparen_position < decimal_symbol_position ) &&
         ( rightparen_position > decimal_symbol_position ))
        p_sign_posn = "0";
     else
        p_sign_posn = "1";
   }
   else if ( sign_position < currency_symbol_position )
   {
      if ( sign_position < decimal_symbol_position )
         p_sign_posn = "1";
      else
         p_sign_posn = "3";
   }
   else // sign_position >= currency_symbol_position
   {
      if ( sign_position > decimal_symbol_position )
         p_sign_posn = "2";
      else
         p_sign_posn = "4";
   }
      
   int_p_sign_posn = p_sign_posn;

   p_sep_by_space = "0";
   boolean currency_and_sign_are_adjacent = false;

   if ((( currency_symbol_position < decimal_symbol_position ) && ( sign_position < decimal_symbol_position )) ||
       (( currency_symbol_position > decimal_symbol_position ) && ( sign_position > decimal_symbol_position )))
      currency_and_sign_are_adjacent = true;
   

   if ( currency_and_sign_are_adjacent && ( sign_position >= 0 ))
   {
      if ( POSIXUtilities.isBetween(currency_symbol_position,space_position,decimal_symbol_position) &&
           POSIXUtilities.isBetween(sign_position,space_position,decimal_symbol_position))
         p_sep_by_space = "1";
      if ( POSIXUtilities.isBetween(currency_symbol_position,space_position,sign_position))
         p_sep_by_space = "2";
   }
   else
   {
      if (( currency_symbol_position > decimal_symbol_position && space_position == currency_symbol_position - 1 ) ||
          ( currency_symbol_position < decimal_symbol_position && space_position == currency_symbol_position + 1 ))
         p_sep_by_space = "1";
      if (( sign_position > decimal_symbol_position && space_position == sign_position - 1 ) ||
          ( sign_position < decimal_symbol_position && space_position == sign_position + 1 ))
         p_sep_by_space = "2";
   }

   int_p_sep_by_space = p_sep_by_space;

   // Parse Negative Monetary Formats
   currency_symbol_position = monetary_formats[NEGATIVE].indexOf('\u00a4');
   decimal_symbol_position = monetary_formats[NEGATIVE].indexOf('.');
   sign_position = monetary_formats[NEGATIVE].indexOf('-');
   leftparen_position = monetary_formats[NEGATIVE].lastIndexOf('(',decimal_symbol_position);
   rightparen_position = monetary_formats[NEGATIVE].indexOf(')',decimal_symbol_position);
   space_position = monetary_formats[NEGATIVE].indexOf(' ');

   if (space_position == -1)
      space_position = monetary_formats[NEGATIVE].indexOf('\u00a0');

   if ( currency_symbol_position >= 0) 
      if ( currency_symbol_position > decimal_symbol_position )
         n_cs_precedes = "0";
      else
         n_cs_precedes = "1";
   else
      n_cs_precedes = "-1";
   
   int_n_cs_precedes = n_cs_precedes;

   if ( negative_sign == "" )
   {
     if (( leftparen_position < decimal_symbol_position ) &&
         ( rightparen_position > decimal_symbol_position ))
        n_sign_posn = "0";
     else
        n_sign_posn = "1";
   }
   else if ( sign_position < currency_symbol_position )
   {
      if ( sign_position < decimal_symbol_position )
         n_sign_posn = "1";
      else
         n_sign_posn = "3";
   }
   else // sign_position >= currency_symbol_position
   {
      if ( sign_position > decimal_symbol_position )
         n_sign_posn = "2";
      else
         n_sign_posn = "4";
   }
      
   int_n_sign_posn = n_sign_posn;

   n_sep_by_space = "0";
   currency_and_sign_are_adjacent = false;

   if ((( currency_symbol_position < decimal_symbol_position ) && ( sign_position < decimal_symbol_position )) ||
       (( currency_symbol_position > decimal_symbol_position ) && ( sign_position > decimal_symbol_position )))
      currency_and_sign_are_adjacent = true;
   

   if ( currency_and_sign_are_adjacent && ( sign_position >= 0 ))
   {
      if ( POSIXUtilities.isBetween(currency_symbol_position,space_position,decimal_symbol_position) &&
           POSIXUtilities.isBetween(sign_position,space_position,decimal_symbol_position))
         n_sep_by_space = "1";
      if ( POSIXUtilities.isBetween(currency_symbol_position,space_position,sign_position))
         n_sep_by_space = "2";
   }
   else
   {
      if (( currency_symbol_position > decimal_symbol_position && space_position == currency_symbol_position - 1 ) ||
          ( currency_symbol_position < decimal_symbol_position && space_position == currency_symbol_position + 1 ))
         n_sep_by_space = "1";
      if (( sign_position > decimal_symbol_position && space_position == sign_position - 1 ) ||
          ( sign_position < decimal_symbol_position && space_position == sign_position + 1 ))
         n_sep_by_space = "2";
   }

   int_n_sep_by_space = n_sep_by_space;

   } // end POSIX_LCMonetary( doc, supp );

   public void write ( PrintWriter out ) {
 
      out.println("*************");
      out.println("LC_MONETARY");
      out.println("*************");
      out.println();


      out.print("int_curr_symbol       \"");
      out.println(POSIXUtilities.POSIXCharName(int_curr_symbol+" ")+"\"");
      out.println("currency_symbol       \"" + currency_symbol   + "\"");
      out.println("mon_decimal_point     \"" + mon_decimal_point + "\"");
      out.println("mon_thousands_sep     \"" + mon_thousands_sep + "\"");
      out.println("mon_grouping          " + mon_grouping );
      out.println("positive_sign         \"" + positive_sign + "\"");
      out.println("negative_sign         \"" + negative_sign + "\"");
      out.println("int_frac_digits       " + int_frac_digits);
      out.println("frac_digits           " + frac_digits);
      out.println("p_cs_precedes         " + p_cs_precedes);
      out.println("p_sep_by_space        " + p_sep_by_space);
      out.println("n_cs_precedes         " + n_cs_precedes);
      out.println("n_sep_by_space        " + n_sep_by_space);
      out.println("p_sign_posn           " + p_sign_posn);
      out.println("n_sign_posn           " + n_sign_posn);
      out.println("int_p_cs_precedes     " + int_p_cs_precedes);
      out.println("int_p_sep_by_space    " + int_p_sep_by_space);
      out.println("int_n_cs_precedes     " + int_n_cs_precedes);
      out.println("int_n_sep_by_space    " + int_n_sep_by_space);
      out.println("int_p_sign_posn       " + int_p_sign_posn);
      out.println("int_n_sign_posn       " + int_n_sign_posn);


      out.println();
      out.println("END LC_MONETARY");
      out.println();
      out.println();

   }
}
