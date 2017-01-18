package org.unicode.cldr.test;

import java.util.Date;
import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;

public class CheckNew extends FactoryCheckCLDR {
    private OutdatedPaths outdatedPaths;

    public CheckNew(Factory factory) {
        super(factory);
        outdatedPaths = OutdatedPaths.getInstance();
    }

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) {
            return this;
        }
        //        if (Phase.VETTING == getPhase()) {
        //            setSkipTest(false); // ok
        //        } else {
        //            setSkipTest(true);
        //            return this;
        //        }

        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        return this;
    }

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {

        Date modified = getCldrFileToCheck().getLastModifiedDate(path);
        if (modified != null) return this;

        boolean isOutdated = outdatedPaths.isOutdated(getCldrFileToCheck().getLocaleID(), path);
        if (!isOutdated) return this;

        // we skip if certain other errors are present
        if (hasCoverageError(result)) return this;

        String englishValue = getEnglishFile().getStringValue(path);
        String oldEnglishValue = outdatedPaths.getPreviousEnglish(path);

        result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType)
            .setSubtype(Subtype.modifiedEnglishValue)
            .setMessage("The English value for this field changed from “{0}” to “{1}’, but the corresponding value for your locale didn't change.",
                oldEnglishValue, englishValue));

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
