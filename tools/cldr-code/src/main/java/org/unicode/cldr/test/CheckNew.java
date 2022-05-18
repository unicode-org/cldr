package org.unicode.cldr.test;

import java.util.Date;
import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.tool.CldrVersion;
import org.unicode.cldr.util.AnnotationUtil;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;

public class CheckNew extends FactoryCheckCLDR {
    private OutdatedPaths outdatedPaths;
    boolean isRoot;

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
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        isRoot = "root".equals(cldrFileToCheck.getLocaleID());

        return this;
    }

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {

        CLDRFile cldrFileToCheck = getCldrFileToCheck();
        // don't check inherited values
        // first see if the value is inherited or not
        if (!isRoot
            && value != null
            && AnnotationUtil.pathIsAnnotation(path)
            && cldrFileToCheck.getUnresolved().getStringValue(path) != null) {
            if (AnnotationUtil.matchesCode(value)) {
                result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.valueMustBeOverridden)
                    .setMessage("This value must be a real translation, NOT the name/keyword placeholder."));
            }
        }

        Date modified = cldrFileToCheck.getLastModifiedDate(path);
        if (modified != null) return this;

        boolean isOutdated = outdatedPaths.isOutdated(cldrFileToCheck.getLocaleID(), path)
            || SubmissionLocales.pathAllowedInLimitedSubmission(path);

        if (!isOutdated) {
            return this;
        }

        // we skip if certain other errors are present
        if (hasCoverageError(result)) {
            return this;
        }

        String englishValue = getEnglishFile().getStringValue(path);
        String oldEnglishValue = outdatedPaths.getPreviousEnglish(path);
        if (!OutdatedPaths.NO_VALUE.equals(oldEnglishValue)) {
            CldrVersion birth = outdatedPaths.getEnglishBirth(path);
            if (birth != null) {
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.modifiedEnglishValue)
                    .setMessage("In CLDR {2} the English value for this field changed from “{0}” to “{1}”, but the corresponding value for your locale didn't change.",
                        oldEnglishValue, englishValue, birth.toString()));
            }
        }
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
