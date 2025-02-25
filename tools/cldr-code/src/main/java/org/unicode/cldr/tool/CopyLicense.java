package org.unicode.cldr.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CldrUtility;

/**
 * Note: CLDR-18205: There are two copies of LICENSE to work around Eclipse bug
 * https://github.com/eclipse-m2e/m2e-core/issues/1912
 */
@CLDRTool(alias = "copylicense", description = "copy LICENSE file from root into subdirectory")
public class CopyLicense {

    static final Path ROOT_PATH =
            CLDRConfig.getInstance().getCldrBaseDirectory().toPath().resolve(CldrUtility.LICENSE);
    static final Path RESOURCE_PATH =
            CLDRConfig.getInstance()
                    .getCldrBaseDirectory()
                    .toPath()
                    .resolve(
                            "tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/"
                                    + CldrUtility.LICENSE);

    public static void main(String[] args) throws IOException {
        System.out.println("# Copying " + ROOT_PATH + " to " + RESOURCE_PATH);
        Files.copy(
                ROOT_PATH,
                RESOURCE_PATH,
                StandardCopyOption.COPY_ATTRIBUTES,
                StandardCopyOption.REPLACE_EXISTING);
    }
}
