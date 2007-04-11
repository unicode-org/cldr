package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRFile;

import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

import java.util.List;
import java.util.Map;

/**
 * Class for processing the input and output of CLDR data for use in the
 * Survey Tool and other tools.
 */
public class DisplayAndInputProcessor {
  
  private Collator col;

  private Collator spaceCol;
  
  static final UnicodeSet RTL = new UnicodeSet("[[:Bidi_Class=Arabic_Letter:][:Bidi_Class=Right_To_Left:]]");

  /**
   * Constructor, taking cldrFile.
   * @param cldrFileToCheck
   */
  public DisplayAndInputProcessor(CLDRFile cldrFileToCheck) {
    String locale = cldrFileToCheck.getLocaleID();
    col = Collator.getInstance(new ULocale(locale));
    spaceCol = Collator.getInstance(new ULocale(locale));
  }

  /**
   * Process the value for display. The result is a string for display in the
   * Survey tool or similar program.
   * 
   * @param path
   * @param value
   * @param fullPath
   * @return
   */
  public String processForDisplay(String path, String value) {
    if (path.contains("exemplarCharacters")) {
      if (RTL.containsSome(value) && value.startsWith("[") && value.endsWith("]")) {
        return "\u200E[\u200E" + value.substring(1,value.length()-2) + "\u200E]\u200E";
      }
    }
    return value;
  }

  /**
   * Process the value for input. The result is a cleaned-up value. For example,
   * an exemplar set is modified to be in the normal format, and any missing [ ]
   * are added (a common omission on entry). If there are any failures then the
   * original value is returned, so that the proper error message can be given.
   * 
   * @param path
   * @param value
   * @param fullPath
   * @return
   */
  public String processInput(String path, String value) {
    String original = value;
    // all of our values should not have leading or trailing spaces
    value = value.trim();

    // check specific cases
    if (path.contains("/exemplarCharacters")) {
      // clean up the user's input.
      // first, fix up the '['

      if (!value.startsWith("[") && !value.endsWith("]")) {
        value = "[" + value + "]";
      }

      try {
        UnicodeSet exemplar = new UnicodeSet(value);
        String fixedExemplar = CollectionUtilities.prettyPrint(exemplar,
            true, null, null, col, col);
        UnicodeSet doubleCheck = new UnicodeSet(fixedExemplar);
        if (!exemplar.equals(doubleCheck)) {
          return original;
        }
        return fixedExemplar;
      } catch (RuntimeException e) {
        return original;
      }
    }
    return value;
  }
}