/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CoverageLevel.Level;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XMLSource;


/**
 * Checks locale data for coverage.<br>
 * Options:<br>
 * CheckCoverage.requiredLevel=value to override the required level. values: comprehensive, modern, moderate, basic<br>
 * CheckCoverage.skip=true to skip a locale. For console testing, you want to skip the non-language locales, since
 * they don't typically add, just replace. See CheckCLDR for an example.
 * CoverageLevel.localeType=organization to override the organization.
 * @author davis
 *
 */
public class CheckCoverage extends CheckCLDR {
    static final boolean DEBUG = false;
    static final boolean DEBUG_SET = false;
    private static CoverageLevel coverageLevel = new CoverageLevel();
    private Level requiredLevel;
    private boolean skip; // set to null if we should not be checking this file
    private boolean requireConfirmed = true;
    private Matcher specialsToTestMatcher = CLDRFile.specialsToKeep.matcher("");

    public CheckCLDR handleCheck(String path, String fullPath, String value,
            Map<String, String> options, List<CheckStatus> result) {
        // for now, skip all but localeDisplayNames
        if (skip) return this;
        if (options.get("submission") == null) return this;
        
//        if (false && path.indexOf("localeDisplayNames") >= 0 && path.indexOf("\"wo") >= 0) {
//        	System.out.println("debug: " + value);
//        }
//
//        if (path.indexOf("localeDisplayNames") < 0 && path.indexOf("currencies") < 0 && path.indexOf("exemplarCity") < 0) return this;
//        
//        // skip all items that are in anything but raw codes

        String source = getResolvedCldrFileToCheck().getSourceLocaleID(path, null);
        boolean isConfirmed = !fullPath.contains("[@draft=");
        // && (isConfirmed || !requireConfirmed)
        
        // we test stuff matching specialsToKeep, or code fallback
        // skip anything else
        if (!source.equals(XMLSource.CODE_FALLBACK_ID) 
            && !specialsToTestMatcher.reset(path).matches()
//          && ( path.indexOf("metazone") < 0 ) || ( value != null && value.length() > 0)
            ) {
          // don't test!
            return this;
        }
        
        if(path == null) { 
            throw new InternalError("Empty path!");
        } else if(getCldrFileToCheck() == null) {
            throw new InternalError("no file to check!");
        }

        // check to see if the level is good enough
        CoverageLevel.Level level = coverageLevel.getCoverageLevel(fullPath, path);
        
        if (level == CoverageLevel.Level.UNDETERMINED) return this; // continue if we don't know what the status is
        if (requiredLevel.compareTo(level) >= 0 || !isConfirmed) {
            result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType)
                .setCheckOnSubmit(false)
                    .setMessage(isConfirmed ? 
                        "Needed to meet {0} coverage level." 
                        : "Confirmed value needed to meet {0} coverage level.", new Object[] { level }));
        } else if (DEBUG) {
            System.out.println(level + "\t" + path);
        }
        return this;
    }

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map options, List possibleErrors) {
        if (cldrFileToCheck == null) return this;
        skip = true;
        if (options != null && options.get("CheckCoverage.skip") != null) return this;   
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        if (cldrFileToCheck.getLocaleID().equals("root")) return this;
        coverageLevel.setFile(cldrFileToCheck, options, this, possibleErrors);
        requiredLevel = null;
        if (options != null) {
            String optionLevel = (String) options.get("CheckCoverage.requiredLevel");
            if (optionLevel != null) requiredLevel = CoverageLevel.Level.get(optionLevel);
        }
        if (requiredLevel == null) {
        	requiredLevel = coverageLevel.getRequiredLevel(cldrFileToCheck.getLocaleID(), options);
        }

        if (requiredLevel == null) { 
             requiredLevel = Level.BASIC; 
        }
        if (DEBUG) {
          System.out.println("requiredLevel: " + requiredLevel);
        }

        skip = false;
        return this;
    }
    public void setRequiredLevel(Level level){
        requiredLevel = level;
    }
    public Level getRequiredLevel(){
        return requiredLevel;
    }
}
