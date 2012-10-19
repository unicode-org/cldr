package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;

public class CheckNames extends CheckCLDR {
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\d{3,4}");
    private static final Pattern YEARS_NOT_ALLOWED = Pattern
        .compile(
        "//ldml/localeDisplayNames/(languages|currencies|scripts|territories|measurementSystemNames|transformNames)/.*");

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options,
        List<CheckStatus> result) {
        if (!YEARS_NOT_ALLOWED.matcher(path).matches() ||
            !getCldrFileToCheck().isNotRoot(path)) {
            return this;
        }
        Matcher matcher = YEAR_PATTERN.matcher(value);
        if (matcher.find()) {
            // Allow years in currencies if enclosed by brackets.
            if (path.startsWith("//ldml/localeDisplayNames/currencies") &&
                (isEnclosedByBraces(matcher, value, '(', ')') ||
                 isEnclosedByBraces(matcher, value, '[', ']'))) {
                // no errors
            } else {
                result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.warningType) // TODO (jchye): This should be an error.
                    .setSubtype(Subtype.nameContainsYear)
                    .setMessage("The name should not contain any years ({0})", matcher.group()));
            }
        }
        return this;
    }

    private boolean isEnclosedByBraces(Matcher matcher, String value, char startBrace, char endBrace) {
        return value.lastIndexOf(startBrace, matcher.start()) > -1 &&
            value.indexOf(endBrace, matcher.end()) >= -1;
    }
}
