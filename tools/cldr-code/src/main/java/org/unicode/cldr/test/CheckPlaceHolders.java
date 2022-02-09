package org.unicode.cldr.test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.XPathParts;

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
        // eg
        //ldml/listPatterns/listPattern/listPatternPart[@type="start"]
        //ldml/listPatterns/listPattern[@type="standard-short"]/listPatternPart[@type="2"]
        if (path.startsWith("//ldml/listPatterns/listPattern") && !path.endsWith("/alias")) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            // check order, {0} must be before {1}

            switch(parts.getAttributeValue(-1, "type")) {
            case "start":
                checkNothingAfter1(value, result);
                break;
            case "middle":
                checkNothingBefore0(value, result);
                checkNothingAfter1(value, result);
                break;
            case "end":
                checkNothingBefore0(value, result);
                break;
            case "2": {
                int pos1 = value.indexOf("{0}");
                int pos2 = value.indexOf("{1}");
                if (pos1 > pos2) {
                    result.add(new CheckStatus().setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Invalid list pattern «" + value + "»: the placeholder {0} must be before {1}."));
                }}
                break;
            case "3": {
                int pos1 = value.indexOf("{0}");
                int pos2 = value.indexOf("{1}");
                int pos3 = value.indexOf("{2}");
                if (pos1 > pos2 || pos2 > pos3) {
                    result.add(new CheckStatus().setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Invalid list pattern «" + value + "»: the placeholders {0}, {1}, {2} must appear in that order."));
                }}
                break;
            }
        }
        return this;
    }

    private void checkNothingAfter1(String value, List<CheckStatus> result) {
        if (!value.endsWith("{1}")) {
            result.add(new CheckStatus().setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.invalidPlaceHolder)
                .setMessage("Invalid list pattern «" + value + "», no text can come after {1}."));
        }

    }

    private void checkNothingBefore0(String value, List<CheckStatus> result) {
        if (!value.startsWith("{0}")) {
            result.add(new CheckStatus().setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.invalidPlaceHolder)
                .setMessage("Invalid list pattern «" + value + "», no text can come before {0}."));
        }
    }
}
