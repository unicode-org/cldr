package org.unicode.cldr.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;

public class CheckAlt extends CheckCLDR {

    Set<String> seenSoFar = new HashSet<>();

    // determine if we have an alt=...proposed
    // if we have one, and there is not a non-proposed version -- in this same file, unaliased,
    // there's a problem.
    @Override
    public CheckCLDR handleCheck(
            String path, String fullPath, String value, Options options, List<CheckStatus> result) {
        if (fullPath == null) return this; // skip paths that we don't have

        // quick checks
        if (path.indexOf("[@alt=") <= 0) {
            return this;
        }
        if (path.indexOf("proposed") <= 0) {
            return this;
        }

        if (!accept(result)) return this;

        String strippedPath = CLDRFile.getNondraftNonaltXPath(path);
        if (strippedPath.equals(path)) {
            return this; // paths equal, skip
        }

        String otherValue = getCldrFileToCheck().getStringValue(strippedPath);
        if (otherValue != null) {
            return this;
        }
        result.add(
                new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.warningType)
                        .setSubtype(Subtype.noUnproposedVariant)
                        .setCheckOnSubmit(false)
                        .setMessage("Proposed item but no unproposed variant", new Object[] {}));
        seenSoFar.add(strippedPath);

        return this;
    }

    @Override
    public CheckCLDR handleSetCldrFileToCheck(
            CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        // Skip if the phase is not final testing
        if (Phase.FINAL_TESTING == getPhase() || Phase.BUILD == getPhase()) {
            setSkipTest(false); // ok
        } else {
            setSkipTest(true);
            return this;
        }

        super.handleSetCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        seenSoFar.clear();
        return this;
    }
}
