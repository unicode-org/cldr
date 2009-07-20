package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRFile;

import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.util.ULocale;

/**
 * Class for processing the input and output of CLDR data for use in the
 * Survey Tool and other tools.
 */
public class DisplayAndInputProcessor {
  
  private static final UnicodeSet RTL = new UnicodeSet("[[:Bidi_Class=Arabic_Letter:][:Bidi_Class=Right_To_Left:]]");

  private static final UnicodeSet TO_QUOTE = (UnicodeSet) new UnicodeSet(
          "[[:Cn:]" +
          "[:Default_Ignorable_Code_Point:]" +
          "[:patternwhitespace:]" +
          "[:Me:][:Mn:]]" // add non-spacing marks
          ).freeze();
  
  private Collator col;

  private Collator spaceCol;
  
  private FormatParser formatDateParser = new FormatParser();

  private PrettyPrinter pp;

  /**
   * Constructor, taking cldrFile.
   * @param cldrFileToCheck
   */
  public DisplayAndInputProcessor(CLDRFile cldrFileToCheck) {
    String locale = cldrFileToCheck.getLocaleID();
    col = Collator.getInstance(new ULocale(locale));
    spaceCol = Collator.getInstance(new ULocale(locale));
    pp = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT)).setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY))
    .setCompressRanges(true)
    .setToQuote(new UnicodeSet(TO_QUOTE))
    .setOrdering(col)
    .setSpaceComparator(spaceCol);
  }

  /**
   * Constructor, taking locale.
   * @param locale
   */
  public DisplayAndInputProcessor(ULocale locale) {
    col = Collator.getInstance(locale);
    spaceCol = Collator.getInstance(locale);
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
   * @param internalException TODO
   * @param fullPath
   * @return
   */
  public String processInput(String path, String value, Exception[] internalException) {
    String original = value;
    if (internalException != null) {
      internalException[0] = null;
    }
    try {
      // fix grouping separator if space
      if (path.startsWith("//ldml/numbers/symbols/group")) {
        if (value.equals(" ")) {
          value = "\u00A0";
        }
      }
      // all of our values should not have leading or trailing spaces, except insertBetween
      if (!path.contains("/insertBetween") && !path.contains("/localeSeparator")) {
        value = value.trim();
      }

      // fix date patterns
      if (path.indexOf("/dates") >= 0
          && ((path.indexOf("/pattern") >= 0 && path.indexOf("/dateTimeFormat") < 0) || path
              .indexOf("/dateFormatItem") >= 0)) {
        formatDateParser.set(value);
        String newValue = formatDateParser.toString();
        if (!value.equals(newValue)) {
          value = newValue;
        }
      }
      
      if (path.startsWith("//ldml/numbers") && path.indexOf("Format[")>=0&& path.indexOf("/pattern")>=0) {
          String newValue = value.replaceAll("([%\u00A4]) ", "$1\u00A0")
                                 .replaceAll(" ([%\u00A4])", "\u00A0$1");
          if (!value.equals(newValue)) {
              value = newValue;
          }
      }

      // check specific cases
      if (path.contains("/exemplarCharacters")) {
        // clean up the user's input.
        // first, fix up the '['
        value = value.trim();

        if (!value.startsWith("[")) {
          value = "[" + value;
        }

        if (!value.endsWith("]")) {
          value = value + "]";
        }

        UnicodeSet exemplar = new UnicodeSet(value);
        UnicodeSet toAdd = new UnicodeSet();
        
        for (UnicodeSetIterator usi = new UnicodeSetIterator(exemplar); usi.next(); ) {
          final String string = usi.getString();
          if (string.equals("ß")) {
            continue;
          }
          final String newString = Normalizer.compose(UCharacter.toLowerCase(ULocale.ENGLISH, string), false);
          toAdd.add(newString);
        }
        exemplar.addAll(toAdd);

        exemplar.removeAll(CheckExemplars.TO_REMOVE_FROM_EXEMPLARS);

        String fixedExemplar = pp.toPattern(exemplar);
        UnicodeSet doubleCheck = new UnicodeSet(fixedExemplar);
        if (!exemplar.equals(doubleCheck)) {
          // don't change; something went wrong
        } else if (!value.equals(fixedExemplar)) { // put in this condition just for debugging
          value = fixedExemplar;
        }
      }
      return value;
    } catch (RuntimeException e) {
      if (internalException != null) {
        internalException[0] = e;
      }
      return original;
    }
  }
}
