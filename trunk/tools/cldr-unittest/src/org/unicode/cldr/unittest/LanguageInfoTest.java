package org.unicode.cldr.unittest;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Pair;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.LocaleMatcher.LanguageMatcherData;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.ULocale;

public class LanguageInfoTest extends TestFmwk {
    static CLDRConfig testInfo = CLDRConfig.getInstance();
    static LanguageMatcherData data = LocaleMatcherTest.LANGUAGE_MATCHER_DATA;
    static Map<ULocale, ULocale> FALLBACKS = new LinkedHashMap<>();

    //	@Override
    //	protected void init() throws Exception {
    //		super.init();
    //		SupplementalDataInfo supp = testInfo.getSupplementalDataInfo();
    //		List<R4<String, String, Integer, Boolean>> languageData = supp
    //				.getLanguageMatcherData("written");
    //		for (R4<String, String, Integer, Boolean> item : languageData) {
    //			data.addDistance(item.get0().replace('_', '-'), item.get1()
    //					.replace('_', '-'), item.get2(), item.get3());
    //			logln(item.get0() + "\t" + getName(item.get0()) + "\t"
    //					+ item.get1() + "\t" + getName(item.get1()) + "\t"
    //					+ item.get2() + "\t" + item.get3());
    //			if (item.get2() == 10) {
    //				FALLBACKS.put(new ULocale(item.get0()),
    //						new ULocale(item.get1()));
    //			}
    //		}
    //		data.freeze();
    //	}

    public void testGetData() {
        Set<Pair<String, String>> alreadySeen = new HashSet<>();
        for (R4<String, String, Integer, Boolean> foo : testInfo.getSupplementalDataInfo().getLanguageMatcherData("written")) {
            //            assertTrue("check bounds", foo.get2() >= 0 && foo.get2() <= 100);

            String desired = foo.get0();
            String supported = foo.get1();
            Integer score = foo.get2();
            Boolean oneway = foo.get3();
            assertEquals("Same number of fields", count('_', desired), count('_', supported));

            Pair<String, String> source = Pair.of(desired, supported);
            if (alreadySeen.contains(source)) {
                errln("Duplicate entry for " + source);
                continue;
            }
            alreadySeen.add(source);
            logln(score
                + "\t" + desired + "\t" + getName(desired)
                + "\t" + supported + "\t" + getName(supported)
                + "\t" + oneway);
        }
    }

    private int count(char c, String string) {
        int count = 0;
        int pos = string.indexOf(c);
        while (pos >= 0) {
            ++count;
            pos = string.indexOf(c, pos + 1);
        }
        return count;
    }

    public static String getName(String item) {
        return item.contains("*") ? "n/a" : testInfo.getEnglish().getName(item);
    }

    public static void main(String[] args) {
        new LanguageInfoTest().run(args);
    }

    public void testBasics() {
        final LocaleMatcher matcher = new LocaleMatcher(LocalePriorityList
            .add(ULocale.FRENCH).add(ULocale.UK).add(ULocale.ENGLISH)
            .build(), data);
        logln(matcher.toString());

        assertEquals("UK in FR, UK, EN", ULocale.UK,
            matcher.getBestMatch(ULocale.UK));
        assertEquals("US in FR, UK, EN", ULocale.ENGLISH,
            matcher.getBestMatch(ULocale.US));
        assertEquals("FR in FR, UK, EN", ULocale.FRENCH,
            matcher.getBestMatch(ULocale.FRANCE));
        assertEquals("JA in FR, UK, EN", ULocale.FRENCH,
            matcher.getBestMatch(ULocale.JAPAN));
    }

    public void TestChinese() {
        //		if (logKnownIssue("Cldrbug:8169",
        //				"Problems with language matcher TestChinese.")) {
        //			return;
        //		}
        LocaleMatcher matcher = new LocaleMatcher(LocalePriorityList.add(
            "zh_CN, zh_TW, iw").build(), data);
        ULocale taiwanChinese = new ULocale("zh_TW");
        ULocale chinaChinese = new ULocale("zh_CN");
        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese,
            matcher.getBestMatch("zh_Hant_HK"));

        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese,
            matcher.getBestMatch("zh_Hant_TW"));
        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese,
            matcher.getBestMatch("zh_Hant"));
        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese,
            matcher.getBestMatch("zh_TW"));
        assertEquals("zh_CN, zh_TW, iw;", chinaChinese,
            matcher.getBestMatch("zh_Hans_CN"));
        assertEquals("zh_CN, zh_TW, iw;", chinaChinese,
            matcher.getBestMatch("zh_CN"));
        assertEquals("zh_CN, zh_TW, iw;", chinaChinese,
            matcher.getBestMatch("zh"));
    }

    static final ULocale MUL = new ULocale("mul");

    public void testFallbacks() {

        for (R4<String, String, Integer, Boolean> foo : testInfo.getSupplementalDataInfo().getLanguageMatcherData("written")) {
            String rawDesired = foo.get0();
            if (rawDesired.contains("*")) {
                continue;
            }
            if (rawDesired.equals("tlh")) {
                if (logKnownIssue("cldrbug:8919", "Hack until tlh has likely subtags")) {
                    continue;
                }
            }
            ULocale desired = new ULocale(rawDesired);
            ULocale supported = new ULocale(foo.get1());
            Integer score = foo.get2();
            Boolean oneway = foo.get3();
            if (!oneway) {
                continue;
            }

            // we put "mul" first in the list, to verify that the fallback works enough to be better than the default.

            @SuppressWarnings("deprecation")
            final LocaleMatcher matcher = new LocaleMatcher(
                LocalePriorityList
                    .add(MUL).add(supported)
                    .build(),
                data);

            ULocale bestMatch = matcher.getBestMatch(desired);
            if (!assertEquals("fallback for " + desired + ", " + score, supported, bestMatch)) {
                ULocale max = ULocale.addLikelySubtags(desired);
                warnln("Might be missing something like\n"
                    + "<languageMatch desired=\""
                    + desired.getLanguage() + "_" + max.getScript()
                    + "\" supported=\"en_Latn\" percent=\"90\" oneway=\"true\" />");
                bestMatch = matcher.getBestMatch(desired); // for debugging
            }
        }
    }
}
