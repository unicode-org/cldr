package org.unicode.cldr.icu;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.PluralRanges;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row.R2;

/**
 * Class for converting CLDR plurals files to a format suitable for outputting
 * ICU data with. It might be possible for PluralsMapper and LdmlLocaleMapper to
 * share a parent class, but there isn't currently a need for that so they're
 * kept separate for the time being.
 * 
 * @author markdavis
 * Based on PluralsMapper
 */
public class PluralRangesMapper {
    private static final boolean DO_NUMBER = false;
    private static final boolean DO_INT_VECTOR = DO_NUMBER && true;

    private String supplementalDir;

    /**
     * Constructor. A SupplementalDataInfo object is used rather than the
     * supplemental directory because the supplemental data parsing is already
     * done for us. The RegexLookup method used by LdmlLocaleMapper wouldn't
     * work well, since there would only be one regex.
     * 
     * @param supplementalDataInfo
     */
    public PluralRangesMapper(String supplementalDir) {
        this.supplementalDir = supplementalDir;
    }

    /**
     * @return CLDR data converted to an ICU-friendly format
     */
    public IcuData fillFromCldr() {
        IcuData icuData = new IcuData("pluralRanges.xml", "pluralRanges", false);
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance(supplementalDir);
        Map<PluralRanges, Integer> rulesToSet = new TreeMap<>();
        for (String locale : sdi.getPluralRangesLocales()) {
            PluralRanges ranges = sdi.getPluralRanges(locale);
            Integer number = rulesToSet.get(ranges);
            if (number == null) {
                rulesToSet.put(ranges, number = rulesToSet.size());
            }
            icuData.add("/locales/" + locale, format(number));
        }
        String intStyle = DO_INT_VECTOR ? ":intvector" : "" ;
        
        for (Entry<PluralRanges, Integer> rulesAndSet : rulesToSet.entrySet()) {
            PluralRanges ranges = rulesAndSet.getKey();
            Integer setNumber = rulesAndSet.getValue();
            int numberWritten = 0;
            for (Count start : Count.VALUES) {
                for (Count end : Count.VALUES) {
                    Count value = ranges.getExplicit(start, end);
                    if (value != null) {
                        ++numberWritten;
                        icuData.add("/rules/" + format(setNumber) + intStyle,
                            getString(start),
                            getString(end),
                            getString(value));
                    }
                }
            }
        }
        return icuData;
    }

    private String format(int number) {
        return "set" + (number < 10 ? "0" + number : String.valueOf(number));
    }

    public String getString(Count start) {
        return DO_NUMBER ? String.valueOf(start.ordinal()) : start.toString();
    }
}
