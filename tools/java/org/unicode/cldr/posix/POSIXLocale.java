/*
**********************************************************************
* Copyright (c) 2005, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons
**********************************************************************
*/
package org.unicode.cldr.posix;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
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
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
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


/**
 * Class to generate POSIX format from CLDR. 
 * @author jcemmons
 */

public class POSIXLocale {

   String locale_name;
   String codeset;
   Document doc;
   Document supp;
   Document collrules;
   POSIX_LCCtype lc_ctype;
   POSIX_LCCollate lc_collate;
   POSIX_LCNumeric lc_numeric;
   POSIX_LCMonetary lc_monetary;
   POSIX_LCTime lc_time;
   POSIX_LCMessages lc_messages;
   

   public POSIXLocale ( String locale_name , String cldr_data_location , UnicodeSet repertoire, Charset cs, String codeset, UnicodeSet collateset ) throws Exception {

      this.locale_name = locale_name;
      this.codeset = codeset;
      doc = LDMLUtilities.getFullyResolvedLDML ( cldr_data_location+File.separator+"main", locale_name, false, false, false );
      supp = LDMLUtilities.parse ( cldr_data_location+File.separator+"main"+File.separator+"supplementalData.xml",  true );
      collrules = LDMLUtilities.getFullyResolvedLDML ( cldr_data_location+File.separator+"collation", locale_name, true, true, true );


     if ( repertoire.isEmpty() ) // Generate default repertoire set from exemplar characters;
     {
        String SearchLocation = "//ldml/characters/exemplarCharacters";
        Node n = LDMLUtilities.getNode(doc,SearchLocation);
        UnicodeSet ExemplarCharacters = new UnicodeSet(LDMLUtilities.getNodeValue(n));
        UnicodeSet CaseFoldedExemplars = new UnicodeSet(ExemplarCharacters.closeOver(ExemplarCharacters.CASE));
        repertoire.addAll(CaseFoldedExemplars);
        UnicodeSetIterator it = new UnicodeSetIterator(CaseFoldedExemplars);
        int PreviousScript = UScript.INVALID_CODE;
        while ( it.next() )
        {
           if ( it.codepoint != UnicodeSetIterator.IS_STRING )
           {
              int Script = UScript.getScript(it.codepoint);
              if ( Script != UScript.COMMON && 
                   Script != UScript.INHERITED && 
                   Script != UScript.INVALID_CODE &&
                   Script != PreviousScript ) // Hopefully this speeds up the process...
              {
                 UnicodeSet ThisScript = new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT,Script);
                 repertoire.addAll(ThisScript);
                 PreviousScript = Script;
              }
           } 
        }
 
        repertoire.add(0x0000,0x007f);        // Always add the ASCII set
        
     }

      lc_collate = new POSIX_LCCollate( doc, repertoire, cs , collrules , collateset );

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
                 Script != PreviousScript ) // Hopefully this speeds up the process...
            {
               UnicodeSet ThisScript = new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT,Script);
               repertoire.addAll(ThisScript);
               PreviousScript = Script;
            }
         } 
      }
      
      lc_ctype = new POSIX_LCCtype ( doc, repertoire, cs );
      lc_numeric = new POSIX_LCNumeric( doc );
      lc_monetary = new POSIX_LCMonetary( doc , supp );
      lc_time = new POSIX_LCTime( doc );
      lc_messages = new POSIX_LCMessages( doc );

   } // end POSIXLocale ( String locale_name, String cldr_data_location );

   public void write(PrintWriter out) throws IOException {
   
      out.println("comment_char *");
      out.println("escape_char /");
      out.println("");
      out.println("*************************************************************************************************");
      out.println("* POSIX Locale                                                                                  *");
      out.println("* Generated automatically from the Unicode Character Database and Common Locale Data Repository *");
      out.println("* see http://www.opengroup.org/onlinepubs/009695399/basedefs/xbd_chap07.html                    *");
      out.println("* Locale Name : " + locale_name + "   Codeset : " + codeset ); 
      out.println("*************************************************************************************************");
      out.println("");

      lc_ctype.write(out);
      lc_collate.write(out);
      lc_numeric.write(out);
      lc_monetary.write(out);
      lc_time.write(out);
      lc_messages.write(out);

   } // end write(PrintWriter out);

};
