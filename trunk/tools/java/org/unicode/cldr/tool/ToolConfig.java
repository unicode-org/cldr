package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfig.Environment;

/**
 * Shim for CLDRConfig. It shouldn't be called outside of tool (command line) usage.
 *
 * Formerly: unittest.TestAll.TestInfo
 *
 */
public class ToolConfig {

    private static CLDRConfig INSTANCE = null;

    public static final synchronized CLDRConfig getToolInstance() {
        if (INSTANCE == null) {
            INSTANCE = CLDRConfig.getInstance();
            if (INSTANCE.getEnvironment() != Environment.LOCAL) { // verify we aren't in the server, unittests, etc.
                throw new InternalError("Error: ToolConfig can only be used in the LOCAL environment.");
            }
        }
        return INSTANCE;
    }
}