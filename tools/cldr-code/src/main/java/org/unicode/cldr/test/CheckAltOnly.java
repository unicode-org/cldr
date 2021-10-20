package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.XPathParts;

/**
 * Check whether a path with alt is present but the corresponding path without alt is missing
 */
public class CheckAltOnly extends FactoryCheckCLDR {

    private final static String message = "A path with alt is present but the corresponding path without alt is missing";

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
        if (isPresentInParent(nonAltPath, localeId)) {
            return this;
        }
        if (file.getConstructedValue(nonAltPath) != null) {
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

    /**
     * Is the given path present in a parent non-root locale?
     *
     * @param nonAltPath
     * @param childLocaleId
     * @return true if present in parent, else false
     */
    private Boolean isPresentInParent(String nonAltPath, String childLocaleId) {
        final String parentLocaleId = LocaleIDParser.getParent(childLocaleId);
        if (parentLocaleId == null || "root".equals(parentLocaleId)) {
            return false;
        }
        final CLDRFile parentCldrFile = getFactory().make(parentLocaleId, true);
        return parentCldrFile.isHere(nonAltPath);
    }
}
