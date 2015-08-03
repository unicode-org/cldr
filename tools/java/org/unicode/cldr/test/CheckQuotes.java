package org.unicode.cldr.test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.PatternCache;

public class CheckQuotes extends CheckCLDR {
    private static final Pattern ASCII_QUOTES = PatternCache.get("[\'\"]");
    private static final Pattern UNITS = PatternCache.get("//ldml/units/.*");

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (value == null || !UNITS.matcher(path).matches()) {
            return this;
        }
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
        return this;
    }
}
