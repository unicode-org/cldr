/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.ValuePathStatus;
import org.unicode.cldr.util.XMLSource;

/**
 * Checks locale data for coverage.<br>
 * Options:<br>
 * CheckCoverage.requiredLevel=value to override the required level. values:
 * comprehensive, modern, moderate, basic...<br>
 * Use the option CheckCoverage.skip=true to skip a locale. For console testing,
 * you want to skip the non-language locales, since they don't typically add,
 * just replace. See CheckCLDR for an example.
 * CoverageLevel.localeType=organization to override the organization.
 *
 * @author davis
 *
 */
public class CheckCoverage extends FactoryCheckCLDR {
    static final boolean DEBUG = false;
    static final boolean DEBUG_SET = false;
    private static CoverageLevel2 coverageLevel;
    private Level requiredLevel;

    SupplementalDataInfo supplementalData;
    private boolean latin;

    // private boolean requireConfirmed = true;
    // private Matcher specialsToTestMatcher = CLDRFile.specialsToPushFromRoot.matcher("");

    public CheckCoverage(Factory factory) {
        super(factory);
    }

    public CheckCLDR handleCheck(String path, String fullPath, String value,
        Options options, List<CheckStatus> result) {

//        if (path.startsWith("//ldml/characters/parseLenients")) {
//            int debug = 0;
//        }

        if (isSkipTest()) return this;

        CLDRFile resolvedCldrFileToCheck = getResolvedCldrFileToCheck();
        if (resolvedCldrFileToCheck.isPathExcludedForSurvey(path)) return this;

        // skip if we are not the winning path
        if (!resolvedCldrFileToCheck.isWinningPath(path)) {
            return this;
        }

        Status status = new Status();
        String source = resolvedCldrFileToCheck.getSourceLocaleID(path, status);

        // if the source is a language locale (that is, not root or code fallback) then we have something already, so
        // skip.
        // we test stuff matching specialsToKeep, or code fallback
        // skip anything else
        if (!source.equals(XMLSource.CODE_FALLBACK_ID)
            && !source.equals("root")
            && (path.indexOf("metazone") < 0 || value != null && value.length() > 0)) {
            return this; // skip!
        }

        if (path == null) {
            throw new InternalCldrException("Empty path!");
        } else if (getCldrFileToCheck() == null) {
            throw new InternalCldrException("No file to check!");
        }

        boolean isAliased = path.equals(status.pathWhereFound);
        if (ValuePathStatus.isMissingOk(resolvedCldrFileToCheck, path, latin, isAliased)) {
            return this; // skip!
        }

        // check to see if the level is good enough
        Level level = coverageLevel != null ? coverageLevel.getLevel(path) : Level.UNDETERMINED;

        if (level == Level.UNDETERMINED) return this; // continue if we don't know what the status is
        if (requiredLevel.compareTo(level) >= 0) {
            CheckStatus.Type coverageErrorType = CheckStatus.warningType;
            if (this.getPhase().equals(CheckCLDR.Phase.VETTING)) {
                coverageErrorType = CheckStatus.errorType;
            }
            result.add(new CheckStatus().setCause(this).setMainType(coverageErrorType)
                .setSubtype(Subtype.coverageLevel)
                .setCheckOnSubmit(false)
                .setMessage("Needed to meet {0} coverage level.", new Object[] { level }));
        } else if (DEBUG) {
            System.out.println(level + "\t" + path);
        }
        return this;
    }

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        setSkipTest(true);
        final String localeID = cldrFileToCheck.getLocaleID();
        if (localeID.equals(new LanguageTagParser().set(localeID).getLanguageScript())) {
            supplementalData = SupplementalDataInfo.getInstance(cldrFileToCheck.getSupplementalDirectory());
            coverageLevel = CoverageLevel2.getInstance(supplementalData, localeID);
            PluralInfo pluralInfo = supplementalData.getPlurals(PluralType.cardinal, localeID);
            if (pluralInfo == supplementalData.getPlurals(PluralType.cardinal, "root")) {
                possibleErrors.add(new CheckStatus()
                    .setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.missingPluralInfo)
                    .setMessage("Missing Plural Information - see supplemental plural charts to file bug.",
                        new Object[] {}));
            }
        }

        if (options != null && options.get(Options.Option.CheckCoverage_skip) != null) return this;
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        if (localeID.equals("root")) return this;

        requiredLevel = options.getRequiredLevel(localeID);
        if (DEBUG) {
            System.out.println("requiredLevel: " + requiredLevel);
        }

        setSkipTest(false);
        latin = ValuePathStatus.isLatinScriptLocale(cldrFileToCheck);

        return this;
    }

    public void setRequiredLevel(Level level) {
        requiredLevel = level;
    }

    public Level getRequiredLevel() {
        return requiredLevel;
    }
}
