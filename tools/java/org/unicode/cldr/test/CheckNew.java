package org.unicode.cldr.test;

import java.util.Date;
import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Factory;

public class CheckNew extends FactoryCheckCLDR {
    private OutdatedPaths outdatedPaths;
    private CLDRFile annotationsRoot;

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
        annotationsRoot = CLDRConfig.getInstance().getAnnotationsFactory().make("root", false);
        return this;
    }

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {

        CLDRFile cldrFileToCheck = getCldrFileToCheck();
        if (path.startsWith("//ldml/annotations/annotation") && value != null) {
            // first see if the value is inherited or not
            boolean skip = false;
            if (cldrFileToCheck.isResolved()) {
                Status status = new Status();
                String localeFound = cldrFileToCheck.getSourceLocaleID(path, status);
                if ("root".equals(localeFound)) {
                    skip = true;
                }
            }
            if (!skip) {
                String rootValue = annotationsRoot.getStringValue(path);
                if (value.equals(rootValue)) {
                    result.add(new CheckStatus().setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.valueMustBeOverridden)
                        .setMessage("This value must be a real translation, NOT the name/keyword placeholder."));
                }
                return this;
            }
        }

        Date modified = cldrFileToCheck.getLastModifiedDate(path);
        if (modified != null) return this;

        boolean isOutdated = outdatedPaths.isOutdated(cldrFileToCheck.getLocaleID(), path);
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
