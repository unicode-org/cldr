package org.unicode.cldr.util;

import org.unicode.cldr.util.SupplementalDataInfo.CoverageVariableInfo;

/**
 * This interface was extracted from SupplementalDataInfo, and contains the two methods
 * CoverageLevel2 calls to extract the information it needs. Extracting became necessary
 * when the CoverageInfo class was extracted from SupplementalDataInfo
 * 
 * @author ribnitz
 *
 */
public interface CoverageInformationGettable {

    /**
     * 
     * @return
     */
    RegexLookup<Level> getCoverageLookup();

    /**
     *
     * @param targetLanguage
     * @return
     */
    CoverageVariableInfo getCoverageVariableInfo(String targetLanguage);

}