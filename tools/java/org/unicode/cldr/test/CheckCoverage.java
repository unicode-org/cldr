/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;

import org.unicode.cldr.test.CoverageLevel.Level;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLSource;


/**
 * Checks locale data for coverage.<br>
 * Options:<br>
 * CheckCoverage.requiredLevel=value to override the required level. values: comprehensive, modern, moderate, basic<br>
 * CheckCoverage.skip=true to skip a locale. For console testing, you want to skip the non-language locales, since
 * they don't typically add, just replace. See CheckCLDR for an example.
 * @author davis
 *
 */
public class CheckCoverage extends CheckCLDR {
    static final boolean DEBUG = false;
    static final boolean DEBUG_SET = false;
    private static CoverageLevel coverageLevel = new CoverageLevel();
    private Level requiredLevel;
    private boolean skip; // set to null if we should not be checking this file

    public CheckCLDR handleCheck(String path, String fullPath, String value,
            Map options, List result) {
        // for now, skip all but localeDisplayNames
        if (skip) return this;
        if (path.indexOf("localeDisplayNames") < 0 && path.indexOf("currencies") < 0 && path.indexOf("exemplarCity") < 0) return this;

        // skip all items that are in anything but raw codes
        String source = getCldrFileToCheck().getSourceLocaleID(path);
        if (!source.equals(XMLSource.CODE_FALLBACK_ID)) return this;
        
        // check to see if the level is good enough
        CoverageLevel.Level level = coverageLevel.getCoverageLevel(fullPath);
        if (level == CoverageLevel.Level.UNDETERMINED) return this; // continue if we don't know what the status is
        if (options != null) {
            String optionLevel = (String) options.get("CheckCoverage.requiredLevel");
            if (optionLevel != null) requiredLevel = CoverageLevel.Level.get(optionLevel);
        }
        if (requiredLevel.compareTo(level) >= 0) {
            result.add(new CheckStatus().setCause(this).setType(CheckStatus.errorType)
                    .setMessage("Needed to meet {0} coverage level.", new Object[] { level }));
        } else if (DEBUG) {
            System.out.println(level + "\t" + path);
        }
        return this;
    }

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck,
            Map options, List possibleErrors) {
        if (cldrFileToCheck == null) return this;
        skip = true;
        if (options != null && options.get("CheckCoverage.skip") != null) return this;
        
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);

        if (cldrFileToCheck.getLocaleID().equals("root")) return this;
        coverageLevel.setFile(cldrFileToCheck, options, this, possibleErrors);
        requiredLevel = coverageLevel.getRequiredLevel(cldrFileToCheck.getLocaleID(), options);
        skip = false;
        return this;
    }

}