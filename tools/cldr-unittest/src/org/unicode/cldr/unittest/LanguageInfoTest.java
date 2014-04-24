package org.unicode.cldr.unittest;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.LocaleMatcher.LanguageMatcherData;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.LocalePriorityList.Builder;
import com.ibm.icu.util.ULocale;

public class LanguageInfoTest extends TestFmwk {
    static TestInfo testInfo = TestInfo.getInstance();
    static LanguageMatcherData data = new LanguageMatcherData();
    static Map<ULocale, ULocale> FALLBACKS = new LinkedHashMap<>();

    @Override
    protected void init() throws Exception {
        super.init();
        SupplementalDataInfo supp = testInfo.getSupplementalDataInfo();
        List<R4<String, String, Integer, Boolean>> languageData = supp.getLanguageMatcherData("written");
        for (R4<String, String, Integer, Boolean> item : languageData) {
            data.addDistance(item.get0().replace('_', '-'), item.get1().replace('_', '-'), item.get2(), item.get3());
            logln(item.get0() + "\t" + getName(item.get0())
                + "\t" + item.get1() + "\t" + getName(item.get1())
                + "\t" + item.get2()
                + "\t" + item.get3());
            if (item.get2() == 10) {
                FALLBACKS.put(new ULocale(item.get0()), new ULocale(item.get1()));
            }
        }
        data.freeze();
    }

    public String getName(String item) {
        return item.contains("*") ? "n/a" : testInfo.getEnglish().getName(item);
    }

    public static void main(String[] args) {
        new LanguageInfoTest().run(args);
    }

    public void testBasics() {
        final LocaleMatcher matcher = new LocaleMatcher(LocalePriorityList
            .add(ULocale.FRENCH).add(ULocale.UK)
            .add(ULocale.ENGLISH).build(), data);
        logln(matcher.toString());

        assertEquals("UK in FR, UK, EN", ULocale.UK, matcher.getBestMatch(ULocale.UK));
        assertEquals("US in FR, UK, EN", ULocale.ENGLISH, matcher.getBestMatch(ULocale.US));
        assertEquals("FR in FR, UK, EN", ULocale.FRENCH, matcher.getBestMatch(ULocale.FRANCE));
        assertEquals("JA in FR, UK, EN", ULocale.FRENCH, matcher.getBestMatch(ULocale.JAPAN));
    }

    public void TestChinese() {
        LocaleMatcher matcher = new LocaleMatcher(
            LocalePriorityList.add("zh_CN, zh_TW, iw").build(), data);
        ULocale taiwanChinese = new ULocale("zh_TW");
        ULocale chinaChinese = new ULocale("zh_CN");
        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese, matcher.getBestMatch("zh_Hant_HK"));

        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese, matcher.getBestMatch("zh_Hant_TW"));
        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese, matcher.getBestMatch("zh_Hant"));
        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese, matcher.getBestMatch("zh_TW"));
        assertEquals("zh_CN, zh_TW, iw;", chinaChinese, matcher.getBestMatch("zh_Hans_CN"));
        assertEquals("zh_CN, zh_TW, iw;", chinaChinese, matcher.getBestMatch("zh_CN"));
        assertEquals("zh_CN, zh_TW, iw;", chinaChinese, matcher.getBestMatch("zh"));
    }

    public void testFallbacks() {
        if (logKnownIssue("Cldrbug:7133", "Problems with LocaleMatcher fallback test.")) {
            return;
        }
        Builder priorities = LocalePriorityList.add(new ULocale("mul")); // the default
        for (ULocale supported : new LinkedHashSet<>(FALLBACKS.values())) {
            priorities.add(supported);
        }
        final LocaleMatcher matcher = new LocaleMatcher(priorities.build(), data);
        logln(matcher.toString());
        for (Entry<ULocale, ULocale> entry : FALLBACKS.entrySet()) {
            ULocale bestMatch = matcher.getBestMatch(entry.getKey());
            assertEquals(entry.getKey() + " => " + entry.getValue(), entry.getValue(), bestMatch);
        }
    }
}
