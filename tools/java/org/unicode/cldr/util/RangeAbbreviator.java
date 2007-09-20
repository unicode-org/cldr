package org.unicode.cldr.util;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;

public class RangeAbbreviator {
    private BreakIterator breaker1;
    private BreakIterator breaker2;
    private String separator;
    private StringBuilder buffer = new StringBuilder();
    
    public RangeAbbreviator(BreakIterator breaker, String separator) {
      this.breaker1 = (BreakIterator) breaker.clone();
      this.breaker2 = (BreakIterator) breaker.clone();
      this.separator = separator;
    }

    public RangeAbbreviator(ULocale breaker, String separator) {
      this(BreakIterator.getWordInstance(breaker), separator);
    }

    public String abbreviate(String firstString, String secondString) {
        if (firstString.equals(secondString)) {
            return firstString;
        }
        buffer.setLength(0);
        breaker1.setText(firstString);
        breaker2.setText(secondString);
 
        // find common initial section
        // we use two offset variables, in case we want to have some kind of equivalence later.
        int start1 = breaker1.first();
        int start2 = breaker2.first();
        while (true) {
            breaker1.next();
            final int current1 = breaker1.current();
            if (current1 == BreakIterator.DONE) {
                break;
            }
            breaker2.next();
            final int current2 = breaker2.current();
            if (current2 == BreakIterator.DONE) {
                break;
            }
            if (!firstString.regionMatches(start1, secondString, start2, current1-start1)) {
                break;
            }
            start1 = current1;
            start2 = current2;
        }
        
        // find common initial section
        int end1 = breaker1.last();
        int end2 = breaker2.last();
        while (true) {
            breaker1.previous();
            final int current1 = breaker1.current();
            if (current1 == BreakIterator.DONE) {
                break;
            }
            breaker2.previous();
            final int current2 = breaker2.current();
            if (current2 == BreakIterator.DONE) {
                break;
            }
            if (!firstString.regionMatches(current1, secondString, current2, end1-current1)) {
                break;
            }
            end1 = current1;
            end2 = current2;
        }
        return buffer.append(firstString.substring(0, end1)).append(separator).append(secondString.substring(start2)).toString();
    }
}