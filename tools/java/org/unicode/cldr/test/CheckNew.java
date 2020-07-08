package org.unicode.cldr.test;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
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

    static final Pattern BAD_EMOJI = Pattern.compile("E\\d+(\\.\\d+)?-\\d+");

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {

        CLDRFile cldrFileToCheck = getCldrFileToCheck();
        if (!isRoot
            && value != null
            && path.startsWith("//ldml/annotations/annotation")
            && cldrFileToCheck.getUnresolved().getStringValue(path) != null) { // don't check inherited values
            // first see if the value is inherited or not
            Matcher matcher = BAD_EMOJI.matcher(value);
            if (matcher.matches()) {
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
