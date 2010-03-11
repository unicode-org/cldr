/*
**********************************************************************
* Copyright (c) 2005, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons
**********************************************************************
*/
package org.unicode.cldr.posix;

import java.io.PrintWriter;
import java.io.File;
import java.nio.charset.Charset;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

import org.unicode.cldr.icu.SimpleConverter;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.SupplementalDataInfo;


/**
 * Class to generate POSIX format from CLDR. 
 * @author jcemmons
 */

public class POSIXLocale {

   String locale_name;
   String codeset;
   POSIX_LCCtype lc_ctype;
   POSIX_LCCollate lc_collate;
   POSIX_LCNumeric lc_numeric;
   POSIX_LCMonetary lc_monetary;
   POSIX_LCTime lc_time;
   POSIX_LCMessages lc_messages;
   POSIXVariant variant;

   public POSIXLocale ( String locale_name , UnicodeSet repertoire, Charset cs, String codeset, UnicodeSet collateset , POSIXVariant variant ) throws Exception {

      this.locale_name = locale_name;
      this.codeset = codeset;
      this.variant = variant;
      
      Factory mainFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
      Factory suppFactory = Factory.make(CldrUtility.DEFAULT_SUPPLEMENTAL_DIRECTORY,".*");
      Factory collFactory = Factory.make(CldrUtility.COLLATION_DIRECTORY, ".*");
      CLDRFile doc = mainFactory.make(locale_name,true);
      SupplementalDataInfo supp = SupplementalDataInfo.getInstance(CldrUtility.DEFAULT_SUPPLEMENTAL_DIRECTORY);
      CLDRFile char_fallbk = suppFactory.make("characters", false );
      CLDRFile collrules = collFactory.makeWithFallback(locale_name);


     if ( repertoire.isEmpty() && codeset.equals("UTF-8")) // Generate default repertoire set from exemplar characters;
     {
        String SearchLocation = "//ldml/characters/exemplarCharacters";
        UnicodeSet ExemplarCharacters = new UnicodeSet(doc.getStringValue(SearchLocation));
        UnicodeSetIterator ec = new UnicodeSetIterator(ExemplarCharacters);
        while ( ec.next() )
        {
           if ( (ec.codepoint != UnicodeSetIterator.IS_STRING) && (ec.codepoint <= 0x00ffff) )
           repertoire.add(ec.codepoint);
        }
        UnicodeSet CaseFoldedExemplars = new UnicodeSet(ExemplarCharacters.closeOver(UnicodeSet.CASE));
        UnicodeSetIterator cfe = new UnicodeSetIterator(CaseFoldedExemplars);
        while ( cfe.next() )
        {
           if ( (cfe.codepoint != UnicodeSetIterator.IS_STRING) && (cfe.codepoint <= 0x00ffff) )
           repertoire.add(cfe.codepoint);
        }

        UnicodeSetIterator it = new UnicodeSetIterator(repertoire);
        int PreviousScript = UScript.INVALID_CODE;
        while ( it.next() )
        {
           if ( (it.codepoint != UnicodeSetIterator.IS_STRING) && (it.codepoint <= 0x00ffff) )
           {
              int Script = UScript.getScript(it.codepoint);
              if ( Script != UScript.COMMON && 
                   Script != UScript.INHERITED && 
                   Script != UScript.INVALID_CODE &&
                   Script != UScript.HAN &&
                   Script != PreviousScript ) // Hopefully this speeds up the process...
              {
                 UnicodeSet ThisScript = new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT,Script);
                 UnicodeSetIterator ts = new UnicodeSetIterator(ThisScript);
                 while ( ts.next() ) 
                 {
                    if ( (ts.codepoint != UnicodeSetIterator.IS_STRING) && (ts.codepoint <= 0x00ffff) )
                       repertoire.add(ts.codepoint);
                 }
                 PreviousScript = Script;
              }
           } 
        }
 
        repertoire.add(0x0000,0x007f);        // Always add the ASCII set
        
     }
     else if ( ! codeset.equals("UTF-8") )
     {
        UnicodeSet csset = new SimpleConverter(cs).getCharset();
        repertoire = new UnicodeSet(UnicodeSet.MIN_VALUE,UnicodeSet.MAX_VALUE).retainAll(csset);
        POSIXUtilities.setRepertoire(repertoire);
     }

     UnicodeSetIterator rep = new UnicodeSetIterator(repertoire);
     while ( rep.next() )
     {
        if ( !UCharacter.isDefined(rep.codepoint) && (rep.codepoint != UnicodeSetIterator.IS_STRING))
           repertoire.remove(rep.codepoint);
     }

     POSIXUtilities.setCharFallback(char_fallbk);

     lc_collate = new POSIX_LCCollate( doc, repertoire, collrules , collateset , codeset , variant );

      if ( codeset.equals("UTF-8") )
      {
         UnicodeSet tailored = lc_collate.col.getTailoredSet();

         // Add the tailored characters, and close over script

         UnicodeSetIterator it = new UnicodeSetIterator(tailored);
         int PreviousScript = UScript.INVALID_CODE;
         while ( it.next() )
         {
            if ( it.codepoint != UnicodeSetIterator.IS_STRING )
            {
               int Script = UScript.getScript(it.codepoint);
               if ( Script != UScript.COMMON && 
                    Script != UScript.INHERITED && 
                    Script != UScript.INVALID_CODE &&
                    Script != UScript.HAN &&
                    Script != PreviousScript ) // Hopefully this speeds up the process...
               {
                  UnicodeSet ThisScript = new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT,Script);
                  repertoire.addAll(ThisScript);
                  PreviousScript = Script;
               }
            } 
         }
       }
      
      lc_ctype = new POSIX_LCCtype ( doc, repertoire );
      lc_numeric = new POSIX_LCNumeric( doc );
      lc_monetary = new POSIX_LCMonetary( doc , supp , variant );
      lc_time = new POSIX_LCTime( doc , variant);
      lc_messages = new POSIX_LCMessages( doc , locale_name , variant );

   } // end POSIXLocale ( String locale_name, String cldr_data_location );

   public void write(PrintWriter out) {
   
      out.println("comment_char *");
      out.println("escape_char /");
      out.println("");
      out.println("*************************************************************************************************");
      out.println("* POSIX Locale                                                                                  *");
      out.println("* Generated automatically from the Unicode Character Database and Common Locale Data Repository *");
      out.println("* see http://www.opengroup.org/onlinepubs/009695399/basedefs/xbd_chap07.html                    *");
      out.println("* Locale Name : " + locale_name + "   Codeset : " + codeset ); 
      out.println("*************************************************************************************************");
      out.println("* Copyright 1991-2009 Unicode, Inc. All rights reserved. Distributed under the Terms of Use in  *");
      out.println("* http://www.unicode.org/copyright.html.                                                        *");
      out.println("*                                                                                               *");
      out.println("* Permission is hereby granted, free of charge, to any person obtaining a copy of the Unicode   *");
      out.println("* data files and any associated documentation (the \"Data Files\") or Unicode software and any    *");
      out.println("* associated documentation (the \"Software\") to deal in the Data Files or Software without       *");
      out.println("* restriction, including without limitation the rights to use, copy, modify, merge, publish,    *");
      out.println("* distribute, and/or sell copies of the Data Files or Software, and to permit persons to whom   *");
      out.println("* the Data Files or Software are furnished to do so, provided that (a) the above copyright      *");
      out.println("* notice(s) and this permission notice appear with all copies of the Data Files or Software,    *");
      out.println("* (b) both the above copyright notice(s) and this permission notice appear in associated        *");
      out.println("* documentation, and (c) there is clear notice in each modified Data File or in the Software as *");
      out.println("* well as in the documentation associated with the Data File(s) or Software that the data or    *");
      out.println("* software has been modified.                                                                   *");
      out.println("*                                                                                               *");
      out.println("* THE DATA FILES AND SOFTWARE ARE PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *");
      out.println("* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A        *");
      out.println("* PARTICULAR PURPOSE AND NONINFRINGEMENT OF THIRD PARTY RIGHTS. IN NO EVENT SHALL THE COPYRIGHT *");
      out.println("* HOLDER OR HOLDERS INCLUDED IN THIS NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR *");
      out.println("* CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, *");
      out.println("* WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN   *");
      out.println("* CONNECTION WITH THE USE OR PERFORMANCE OF THE DATA FILES OR SOFTWARE.                         *");
      out.println("*************************************************************************************************");
      out.println("");

      lc_ctype.write(out);
      lc_collate.write(out);
      lc_numeric.write(out);
      lc_monetary.write(out);
      lc_time.write(out, variant);
      lc_messages.write(out);

   } // end write(PrintWriter out);

}
