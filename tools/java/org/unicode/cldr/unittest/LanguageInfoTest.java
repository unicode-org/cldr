package org.unicode.cldr.unittest;

import java.util.List;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.LocaleMatcher.LanguageMatcherData;

public class LanguageInfoTest extends TestFmwk {
    static TestInfo testInfo = TestInfo.getInstance();
    static LanguageMatcherData data = new LanguageMatcherData();

    static {
        SupplementalDataInfo supp = testInfo.getSupplementalDataInfo();
        List<R4<String, String, Integer, Boolean>> languageData = supp.getLanguageMatcherData("written");
        for (R4<String, String, Integer, Boolean> item : languageData) {
            data.addDistance(item.get0(), item.get1(), item.get2(), item.get3());
        }
        data.freeze();
    }


    public static void main(String[] args) {
        new LanguageInfoTest().run(args);
    }

    public void testBasics() {
        final LocaleMatcher matcher = new LocaleMatcher(LocalePriorityList
                .add(ULocale.FRENCH).add(ULocale.UK)
                .add(ULocale.ENGLISH).build(),
                data);
        logln(matcher.toString());

        assertEquals("UK in FR, UK, EN", ULocale.UK, matcher.getBestMatch(ULocale.UK));
        assertEquals("US in FR, UK, EN", ULocale.ENGLISH, matcher.getBestMatch(ULocale.US));
        assertEquals("FR in FR, UK, EN", ULocale.FRENCH, matcher.getBestMatch(ULocale.FRANCE));
        assertEquals("JA in FR, UK, EN", ULocale.FRENCH, matcher.getBestMatch(ULocale.JAPAN));
    }
}
