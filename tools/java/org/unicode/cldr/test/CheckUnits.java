package org.unicode.cldr.test;

import java.util.List;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.SimpleFormatter;

public class CheckUnits extends CheckCLDR {
    private static final Pattern HOUR_SYMBOL = PatternCache.get("h{1,2}");
    private static final Pattern MINUTE_SYMBOL = PatternCache.get("m{1,2}");
    private static final Pattern SECONDS_SYMBOL = PatternCache.get("ss");
    private static XPathParts xpp = new XPathParts();

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {

        if (!path.startsWith("//ldml/units")) {
            return this;
        }
        // Note, the following test has some overlaps with the checkAndReplacePlaceholders
        // test in CheckForExamplars (why there?). That is probably OK, they check in
        // different ways, but some errors will produce two somewhat different error messages.
        if (path.startsWith("//ldml/units/unitLength")) {
            int min = 0;
            int max = 0;
            if (path.contains("compoundUnitPattern")) {
                min = 2;
                max = 2;
            } else if (path.contains("perUnitPattern") || path.contains("coordinateUnitPattern")) {
                min = 1;
                max = 1;
            } else if (path.contains("unitPattern")) {
                min = 0;
                max = 1;
            } // else we have displayName, with min/max = 0
            if (max > 0) {
                try {
                    SimpleFormatter sf = SimpleFormatter.compileMinMaxArguments(value, min, max);
                } catch (Exception e) {
                    result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Invalid unit pattern, must have min " + min + " and max " + max + " distinct placeholders of the form {n}"));
                }
            }
        }

        if (!path.contains("durationUnitPattern")) {
            return this;
        }

        xpp.set(path);
        String durationUnitType = xpp.findAttributeValue("durationUnit", "type");
        boolean hasHourSymbol = HOUR_SYMBOL.matcher(value).find();
        boolean hasMinuteSymbol = MINUTE_SYMBOL.matcher(value).find();
        boolean hasSecondsSymbol = SECONDS_SYMBOL.matcher(value).find();

        if (durationUnitType.contains("h") && !hasHourSymbol) {
            /* Changed message from "The hour symbol (h or hh) is missing"
             *  to "The hour indicator should be either h or hh for duration"
             *  per http://unicode.org/cldr/trac/ticket/10999
             */
            result.add(new CheckStatus().setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.invalidDurationUnitPattern)
                .setMessage("The hour indicator should be either h or hh for duration."));
        } else if (durationUnitType.contains("m") && !hasMinuteSymbol) {
            result.add(new CheckStatus().setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.invalidDurationUnitPattern)
                .setMessage("The minutes symbol (m or mm) is missing."));
        } else if (durationUnitType.contains("s") && !hasSecondsSymbol) {
            result.add(new CheckStatus().setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.invalidDurationUnitPattern)
                .setMessage("The seconds symbol (ss) is missing."));
        }
        return this;
    }
}
