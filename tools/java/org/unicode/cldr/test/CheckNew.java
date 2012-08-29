package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;

public class CheckNew extends CheckCLDR {

    private OutdatedPaths outdatedPaths = new OutdatedPaths();

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options, List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) {
            return this;
        }
        if (Phase.VETTING == getPhase()) {
            setSkipTest(false); // ok
        } else {
            setSkipTest(true);
            return this;
        }

        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        return this;
    }

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {

        boolean isOutdated = outdatedPaths.isOutdated(getCldrFileToCheck().getLocaleID(), path);
        if (!isOutdated) return this;

        // we skip if certain other errors are present
        if (hasCoverageError(result)) return this;

        result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType)
                .setSubtype(Subtype.modifiedEnglishValue)
                .setMessage("The English value for this field changed, but the value for the locale didn't. "));

        return this;
    }

    private boolean hasCoverageError(List<CheckStatus> result) {
        for (CheckStatus resultItem : result) {
            if (resultItem.getCause().getClass() == CheckCoverage.class) {
                return true;
            }
        }
        return false;
    }
}
