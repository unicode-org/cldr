package org.unicode.cldr.unittest;

import org.unicode.cldr.test.CasingInfo;

import com.ibm.icu.dev.test.TestFmwk;

public class TestCasingInfo extends TestFmwk {
    public static void main(String[] args) {
        new TestCasingInfo().run(args);
    }

    public void TestGetLocaleCasing() {
        CasingInfo casingInfo = new CasingInfo();
        assertNull("Casing info shouldn't exist for imaginary locale",
            casingInfo.getLocaleCasing("xyz"));
        assertNotEquals(
            "Casing should not be the same for different languages",
            casingInfo.getLocaleCasing("en"),
            casingInfo.getLocaleCasing("pt"));
        assertEquals("regional casing should default to country",
            casingInfo.getLocaleCasing("en"),
            casingInfo.getLocaleCasing("en_AU"));
        // The following test is no longer valid with cldrbug 8757
        //assertNotEquals("pt_PT is a special case and should not default to pt",
        //    casingInfo.getLocaleCasing("pt"),
        //    casingInfo.getLocaleCasing("pt_PT"));
        assertNotEquals("Script variants should have their own casing",
            casingInfo.getLocaleCasing("uz"),
            casingInfo.getLocaleCasing("uz_Cyrl"));
        assertEquals(
            "Casing for regional variants of a script should be the same as the script variant",
            casingInfo.getLocaleCasing("zh_Hant_TW"),
            casingInfo.getLocaleCasing("zh_Hant"));
    }
}
