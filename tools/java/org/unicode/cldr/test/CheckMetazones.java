package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.XPathParts;

public class CheckMetazones extends CheckCLDR {
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*Metazones.*

    // If you don't need any file initialization or postprocessing, you only need this one routine
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        // it helps performance to have a quick reject of most paths
        if (fullPath == null) return this; // skip paths that we don't have
        if (value == null) return this; // skip empty values
        if (path.indexOf("/metazone") < 0) return this;

        // we're simply going to test to make sure that metazone values don't contain any digits
        if (value.matches(".*\\p{Nd}.*")) {
            if (!getCldrFileToCheck().getSourceLocaleID(path, null).equals(getCldrFileToCheck().getLocaleID())) { // skip
                // if
                // inherited
                // --
                // we
                // only
                // need
                // parent
                // instance
                return this;
            }
            // the following is how you signal an error or warning (or add a demo....)
            result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.metazoneContainsDigit) // typically warningType or errorType
                .setMessage("Metazone name contains digits - translate only the name")); // the message; can be
            // MessageFormat with arguments
        }

        if (path.indexOf("/long") >= 0) {
            XPathParts parts = XPathParts.getTestInstance(path);
            String metazoneName = parts.getAttributeValue(3, "type");
            if (!metazoneUsesDST(metazoneName) && path.indexOf("/standard") < 0) {
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.extraMetazoneString) // typically warningType or errorType
                    .setMessage("Extra metazone string - should only contain standard value for a non-DST metazone"));
                // the message can be MessageFormat with arguments
            }
        }
        return this;
    }

    private boolean metazoneUsesDST(String name) {
        return LogicalGrouping.metazonesDSTSet.contains(name);
    }
}
