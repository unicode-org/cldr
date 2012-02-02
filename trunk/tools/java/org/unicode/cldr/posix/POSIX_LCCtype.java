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

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class POSIX_LCCtype {

   UnicodeSet chars;

   public POSIX_LCCtype ( CLDRFile doc, UnicodeSet chars ) {

      String SearchLocation = "//ldml/characters/exemplarCharacters";

      UnicodeSet ExemplarCharacters = new UnicodeSet(doc.getWinningValue(SearchLocation));

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
         System.out.println("WARNING: Not all exemplar characters are in the target codeset.");
         System.out.println("    The resulting locale source might not compile.");
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

        UnicodeSet lowers = new UnicodeSet(types[1][1]).retainAll(chars);

        UnicodeSet us = new UnicodeSet();
        for (UnicodeSetIterator it = new UnicodeSetIterator(chars); it.next();) {
        	int upp = UCharacter.toUpperCase(it.codepoint);
            if (upp != it.codepoint && chars.contains(upp) && lowers.contains(it.codepoint)) us.add(it.codepoint);
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

        UnicodeSet uppers = new UnicodeSet(types[0][1]).retainAll(chars);
        us = new UnicodeSet();
        for (UnicodeSetIterator it = new UnicodeSetIterator(chars); it.next();) {
        	int low = UCharacter.toLowerCase(it.codepoint);
            if (low != it.codepoint && chars.contains(low) && uppers.contains(it.codepoint)) us.add(it.codepoint);
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
