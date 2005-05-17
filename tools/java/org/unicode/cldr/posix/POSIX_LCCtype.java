/*
**********************************************************************
* Copyright (c) 2002-2005, International Business Machines
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

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class POSIX_LCCtype {

   UnicodeSet chars;

   public POSIX_LCCtype ( Document doc, UnicodeSet chars ) {

      String SearchLocation = "//ldml/characters/exemplarCharacters";
      Node n = LDMLUtilities.getNode(doc,SearchLocation);

      UnicodeSet ExemplarCharacters = new UnicodeSet(LDMLUtilities.getNodeValue(n));

      boolean ExemplarError = false;
      UnicodeSetIterator it = new UnicodeSetIterator(ExemplarCharacters);
      while (it.next())
           if ( it.codepoint != -1 && !chars.contains(it.codepoint))
           {
              System.out.println("WARNING: Target codeset does not contain exemplar character : "+
                                  POSIXUtilities.POSIXCharName(it.codepoint));
              ExemplarError = true;
           }

      if ( ExemplarError )
      {
         System.out.println("Locale not generated due to exemplar character errors.");
         System.exit(-1);
      }

      this.chars = chars;

   }

   public void write ( PrintWriter out ) {
 

      out.println("*************");
      out.println("LC_CTYPE");
      out.println("*************");
      out.println();

      String[][] types = { 
                { "upper", "[:Uppercase:]" },
		{ "lower", "[:Lowercase:]" }, 
		{ "alpha", "[[:Alphabetic:]-[[:Uppercase:][:Lowercase:]]]" },
                { "space", "[:Whitespace:]" },
		{ "cntrl", "[:Control:]" }, 
                { "graph", "[^[:Whitespace:][:Control:][:Format:][:Surrogate:][:Unassigned:]]" },
                { "print", "[^[:Control:][:Format:][:Surrogate:][:Unassigned:]]" },
                { "punct", "[:Punctuation:]" },
		{ "digit", "[0-9]" }, 
                { "xdigit", "[0-9 a-f A-F]" },
		{ "blank", "[[:Whitespace:]-[\\u000A-\\u000D \\u0085 [:Line_Separator:][:Paragraph_Separator:]]]" } };

        // print character types, restricted to the charset
        int item, last;
        for (int i = 0; i < types.length; ++i) {
            UnicodeSet us = new UnicodeSet(types[i][1]).retainAll(chars);
            item = 0;
            last = us.size() - 1;
        	for (UnicodeSetIterator it = new UnicodeSetIterator(us); it.next(); ++item) {
                if (item == 0) out.print(types[i][0]);
                out.print("\t" + POSIXUtilities.POSIXCharName(it.codepoint));
                if (item != last) out.print(";/");
                out.println("");
            }
            out.println();
        }

        // toupper processing

        UnicodeSet us = new UnicodeSet();
        for (UnicodeSetIterator it = new UnicodeSetIterator(chars); it.next();) {
        	int low = UCharacter.toUpperCase(it.codepoint);
            if (low != it.codepoint && chars.contains(low)) us.add(it.codepoint);
        }
        item = 0;
        last = us.size() - 1;
        for (UnicodeSetIterator it = new UnicodeSetIterator(us); it.next(); ++item) {
            if (item == 0) out.print("toupper");
        	out.print("\t(" + POSIXUtilities.POSIXCharName(it.codepoint) + "," + 
                    POSIXUtilities.POSIXCharName(UCharacter.toUpperCase(it.codepoint)) + ")");
            if (item != last) out.print(";/");
            out.println("");
        }
        out.println("");

        // tolower processing

        us = new UnicodeSet();
        for (UnicodeSetIterator it = new UnicodeSetIterator(chars); it.next();) {
        	int low = UCharacter.toLowerCase(it.codepoint);
            if (low != it.codepoint && chars.contains(low)) us.add(it.codepoint);
        }
        item = 0;
        last = us.size() - 1;
        for (UnicodeSetIterator it = new UnicodeSetIterator(us); it.next(); ++item) {
            if (item == 0) out.print("tolower");
        	out.print("\t(" + POSIXUtilities.POSIXCharName(it.codepoint) + "," + 
                    POSIXUtilities.POSIXCharName(UCharacter.toLowerCase(it.codepoint)) + ")");
            if (item != last) out.print(";/");
            out.println("");
        }

      out.println();
      out.println("END LC_CTYPE");
      out.println();
      out.println();
   }

}
