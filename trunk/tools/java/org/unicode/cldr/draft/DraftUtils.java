package org.unicode.cldr.draft;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;

/**
 * Stuff used by the 'draft' class that doesn't belong in CLDR core.
 * @author srl
 *
 */
public class DraftUtils {

    /**
     * This actually refers into the unicodetools project.
     */
    public static final String UCD_DIRECTORY = CldrUtility.getPath(CLDRPaths.EXTERNAL_DIRECTORY, "unicodetools/data/ucd/9.0.0-Update");

}
