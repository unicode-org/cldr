package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.XPathParts;

public class CheckMetazones extends CheckCLDR {
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*Metazones.*

    // If you don't need any file initialization or postprocessing, you only need this one routine
    @Override
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

        if (isDSTPathForNonDSTMetazone(path)) {
            result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
            .setSubtype(Subtype.extraMetazoneString) // typically warningType or errorType
            .setMessage("Extra metazone string - should only contain standard value for a non-DST metazone"));
        }
        return this;
    }

    /**
     * True if this is a DST path, but a non DST metazone.
     * Such an XPath should not be present in a CLDRFile.
     * @param path (assumes it is a /metazone path)
     * @return
     */
    public static boolean isDSTPathForNonDSTMetazone(String path) {
        if (path.indexOf("/long") >= 0 || path.indexOf("/short") >= 0) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String metazoneName = parts.getAttributeValue(3, "type");
            if (!metazoneUsesDST(metazoneName) && path.indexOf("/standard") < 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean metazoneUsesDST(String name) {
        return LogicalGrouping.metazonesDSTSet.contains(name);
    }
}
