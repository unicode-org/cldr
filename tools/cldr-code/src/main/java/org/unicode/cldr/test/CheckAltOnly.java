package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

/**
 * Check whether a path with alt is present but the corresponding path without alt is missing
 */
public class CheckAltOnly extends FactoryCheckCLDR {

    private final static String message = "A path with alt is present but the corresponding path without alt is missing; " +
        "a solution might be to confirm the non-alt path";

    public CheckAltOnly(Factory factory) {
        super(factory);
    }

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value,
        Options options, List<CheckStatus> result) {

        if (path == null || fullPath == null || path.indexOf("[@alt=") <= 0) {
            return this;
        }
        final CLDRFile file = getCldrFileToCheck();
        final String localeId = file.getLocaleID();
        if ("root".equals(localeId)) {
            return this;
        }
        if (!file.isHere(path)) {
            return this;
        }
        final String nonAltPath = XPathParts.getPathWithoutAlt(path);
        if (file.isHere(nonAltPath)) {
            return this;
        }
        if (file.getConstructedValue(nonAltPath) != null) {
            return this;
        }
        /*
         * If the source locale is code-fallback, it's an error.
         * getSourceLocaleID skips inheritance marker (it doesn't stop when it gets to inheritance marker).
         * If the parent has inheritance marker, then we get the locale from which it would inherit.
         */
        String sourceLocaleId = file.getSourceLocaleID(nonAltPath, null);
        if (!XMLSource.CODE_FALLBACK_ID.equals(sourceLocaleId)) {
            return this;
        }
        final CheckStatus item = new CheckStatus()
            .setCause(this)
            .setMainType(CheckStatus.errorType)
            .setSubtype(Subtype.missingNonAltPath)
            .setMessage(message);
        result.add(item);
        return this;
    }
}
