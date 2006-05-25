/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons
**********************************************************************
*/
package org.unicode.cldr.posix;

public class POSIXVariant {
   public String collation_type;
   public String currency;
   public String platform;
   public String yesno;
   
   // valid values for the "platform" type
   public static final String SOLARIS     = "solaris";
   public static final String AIX         = "aix";
   

   public POSIXVariant ( String variant_string ) {

      this.collation_type = "standard";
      this.currency = "default";
      this.platform = "common";
      this.yesno = "long";
      boolean more_values = true ;
      String buf = new String(variant_string);
      String rest;

      while ( more_values )
      {
         int comma_pos = buf.indexOf(',');
         if ( comma_pos >= 0 )
         {
            rest = buf.substring(comma_pos+1);
            buf = buf.substring(0,comma_pos);
         }
         else
         {
            more_values = false;
            rest = "";
         }

         int equal_pos = buf.indexOf('=');
         if ( equal_pos > 0 )
         {
            String field = buf.substring(0,equal_pos);
            String field_value = buf.substring(equal_pos+1);

            if ( field.equals("collation"))
               this.collation_type = field_value;
            if ( field.equals("currency"))
               this.currency = field_value;
            if ( field.equals("platform"))
               this.platform= field_value;
            if ( field.equals("yesno"))
               this.yesno = field_value;
         }
         buf = rest;
      }
         
   }   

   public POSIXVariant ( ) {
      this.collation_type = "standard";
      this.currency = "default";
      this.platform = "common";
      this.yesno = "long";
   }   
}
