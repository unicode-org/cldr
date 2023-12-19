package org.unicode.cldr.test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.PatternCache;

public class CheckAnnotations extends CheckCLDR {
    private static final Pattern ANNOTATION_PATH = Pattern.compile("//ldml/annotations/.*");

    @Override
    public CheckCLDR handleCheck(
            String path, String fullPath, String value, Options options, List<CheckStatus> result) {
        if (value == null) {
            return this;
        } else if (!ANNOTATION_PATH.matcher(path).matches()
                || !getCldrFileToCheck().isNotRoot(path)) {
            return this;
        }
        if (!accept(result)) return this;
        final String ecode = hasAnnotationECode(value);

        if (ecode != null) {
            result.add(
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.illegalAnnotationCode)
                            .setMessage(
                                    "The annotation must be a translation and not contain the E… code from root, or anything like it. ({0})",
                                    ecode));
        }
        return this;
    }

    static String hasAnnotationECode(String value) {
        Matcher m = HAS_ANNOTATION_ECODE.matcher(value);
        if (m.find()) {
            return m.group();
        } else {
            return null;
        }
    }

    static final Pattern HAS_ANNOTATION_ECODE =
            PatternCache.get("E[0-9]{1,3}(?:[-\u2013:—.][0-9]{1,3}){0,2}");
}
