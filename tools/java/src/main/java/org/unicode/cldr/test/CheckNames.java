package org.unicode.cldr.test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.PatternCache;

public class CheckNames extends CheckCLDR {
    private static final Pattern YEAR_PATTERN = PatternCache.get("\\d{3,4}");
    private static final Pattern YEARS_NOT_ALLOWED = Pattern
        .compile(
            "//ldml/localeDisplayNames/(languages|currencies|scripts|territories|measurementSystemNames|transformNames)/.*");

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (value == null) {
            return this;
        }
        if (!YEARS_NOT_ALLOWED.matcher(path).matches() ||
            !getCldrFileToCheck().isNotRoot(path)) {
            return this;
        }
        Matcher matcher = YEAR_PATTERN.matcher(value);
        if (matcher.find()) {
            // If same as the code-fallback value (territories) then no error
            if (path.startsWith("//ldml/localeDisplayNames/territories") &&
                getCldrFileToCheck().getBaileyValue(path, null, null).equals(value)) {
                return this;
            }
            // Allow years in currencies if enclosed by brackets.
            if (path.startsWith("//ldml/localeDisplayNames/currencies") &&
                (isEnclosedByBraces(matcher, value, '(', ')') ||
                    isEnclosedByBraces(matcher, value, '[', ']'))) {
                // no errors
            } else {
                result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.nameContainsYear)
                    .setMessage("The name should not contain any years or region codes ({0})", matcher.group()));
            }
        }
        return this;
    }

    private boolean isEnclosedByBraces(Matcher matcher, String value, char startBrace, char endBrace) {
        return value.lastIndexOf(startBrace, matcher.start()) > -1 &&
            value.indexOf(endBrace, matcher.end()) >= -1;
    }
}
