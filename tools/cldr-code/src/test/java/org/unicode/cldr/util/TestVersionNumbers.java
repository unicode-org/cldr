package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ibm.icu.util.VersionInfo;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.tool.CldrVersion;
import org.unicode.cldr.tool.ToolConstants;

public class TestVersionNumbers {
    @Test
    public void TestAllVersionNumbers() {
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();

        final Map<String, VersionInfo> m = new TreeMap<>();
        m.put(
                "SupplementalDataInfo.getCldrVersion(), from ldmlSupplemental.dtd",
                sdi.getCldrVersion());
        m.put("CLDRFile.GEN_VERSION", VersionInfo.getInstance(CLDRFile.GEN_VERSION));
        m.put("CldrVersion.baseline", CldrVersion.baseline.getVersionInfo());
        m.put("ToolConstants.DEV_VERSION", VersionInfo.getInstance(ToolConstants.DEV_VERSION));

        assertEquals(
                1,
                new HashSet<VersionInfo>(m.values()).size(),
                () -> "All versions should be identical, but got: " + m.toString());
    }
}
