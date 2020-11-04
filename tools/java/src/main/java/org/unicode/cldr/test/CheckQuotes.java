package org.unicode.cldr.test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.PatternCache;

import com.ibm.icu.text.UnicodeSet;

public class CheckQuotes extends CheckCLDR {
    private static final Pattern ASCII_QUOTES = PatternCache.get("[\'\"]");
    private static final Pattern UNITS = PatternCache.get("//ldml/units/.*");
    private static final Pattern DELIMITERS = PatternCache.get("//ldml/delimiters/.*");
    private static final UnicodeSet VALID_DELIMITERS = new UnicodeSet()
        .add(0x2018, 0x201A)
        .add(0x201C, 0x201E)
        .add(0x300C, 0x300F)
        .add(0x2039, 0x203A)
        .add(0x00AB)
        .add(0x00BB);

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (value == null) {
            return this;
        }

        if (UNITS.matcher(path).matches()) {
            Matcher matcher = ASCII_QUOTES.matcher(value);
            CheckStatus.Type type = CheckStatus.warningType;
            if (this.getCldrFileToCheck().getLocaleID().equals("en")) {
                type = CheckStatus.errorType;
            }
            if (matcher.find()) {
                result.add(new CheckStatus().setCause(this)
                    .setMainType(type)
                    .setSubtype(Subtype.asciiQuotesNotAllowed)
                    .setMessage("Use of ASCII quote marks (' \") is discouraged. Use primes for units (′ ″) and curly quotes for text (‘ ’ “ ” …)"));
            }
        }
        if (DELIMITERS.matcher(path).matches()) {
            if (!VALID_DELIMITERS.contains(value)) {
                result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.invalidDelimiter)
                    .setMessage("Invalid delimiter. See https://sites.google.com/site/cldr/translation/characters for a list of valid delimiters."));
            }
        }

        return this;
    }

}
