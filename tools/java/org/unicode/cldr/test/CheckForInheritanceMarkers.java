package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CldrUtility;

public class CheckForInheritanceMarkers extends CheckCLDR {

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (value == null) {
            return this;
        }

        if (value.contains(CldrUtility.INHERITANCE_MARKER)) {
            result.add(new CheckStatus().setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.inheritanceMarkerNotAllowed)
                .setMessage("Inheritance marker ↑↑↑ not allowed in a data value."));
        }
        return this;
    }
}
