package org.unicode.cldr.test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.PatternCache;

public class CheckPlaceHolders extends CheckCLDR {

    private static final Pattern PLACEHOLDER_PATTERN = PatternCache.get("([0-9]|[1-9][0-9]+)");
    private static final Pattern SKIP_PATH_LIST = Pattern
        .compile("//ldml/characters/(exemplarCharacters|parseLenient).*");

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (SKIP_PATH_LIST.matcher(path).matches()) {
            return this;
        }
        int startPlaceHolder = 0;
        int endPlaceHolder;
        if (value == null) {
            return this;
        }
        while (startPlaceHolder != -1 && startPlaceHolder < value.length()) {
            startPlaceHolder = value.indexOf('{', startPlaceHolder + 1);
            if (startPlaceHolder != -1) {
                endPlaceHolder = value.indexOf('}', startPlaceHolder + 1);
                if (endPlaceHolder == -1) {
                    result.add(new CheckStatus().setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Invalid placeholder in value \"" + value + "\""));
                } else {
                    String placeHolderString = value.substring(startPlaceHolder + 1, endPlaceHolder);
                    Matcher matcher = PLACEHOLDER_PATTERN.matcher(placeHolderString);
                    if (!matcher.matches()) {
                        result.add(new CheckStatus().setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.invalidPlaceHolder)
                            .setMessage("Invalid placeholder in value \"" + value + "\""));
                    }
                    startPlaceHolder = endPlaceHolder;
                }
            }
        }
        return this;
    }
}
