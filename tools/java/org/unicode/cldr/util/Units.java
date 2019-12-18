package org.unicode.cldr.util;

import java.util.Locale;
import java.util.regex.Pattern;

import org.unicode.cldr.test.ExampleGenerator;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetSpanner;

public class Units {
    
    private static final UnicodeSet WHITESPACE = new UnicodeSet("[:whitespace:]").freeze();
    public static Pattern NO_SPACE_PREFIX = Pattern.compile("\\}" + ExampleGenerator.backgroundEndSymbol + "?\\p{L}|\\p{L}" + ExampleGenerator.backgroundStartSymbol + "?\\{");

    public static String combinePattern(String unitFormat, String compoundPattern, boolean lowercaseUnitIfNoSpaceInCompound) {
        // meterFormat of the form {0} meters or {0} Meter
        // compoundPattern is of the form Z{0} or Zetta{0}

        // extract the unit
        String modUnit = (String) SPACE_SPANNER.trim(unitFormat.replace("{0}", ""));
        String[] parameters = { modUnit };

        String modFormat = unitFormat.replace(modUnit, MessageFormat.format(compoundPattern, parameters));
        if (modFormat.equals(unitFormat)) {
            // didn't work, so fall back
            String[] parameters1 = { unitFormat };
            modFormat = MessageFormat.format(compoundPattern, parameters1);
        }

        // hack to fix casing
        if (lowercaseUnitIfNoSpaceInCompound 
            && NO_SPACE_PREFIX.matcher(compoundPattern).find()) {
            modFormat = modFormat.replace(modUnit, modUnit.toLowerCase(Locale.ENGLISH));
        }

        return modFormat;
    }

    static final UnicodeSetSpanner SPACE_SPANNER = new UnicodeSetSpanner(WHITESPACE);

}
