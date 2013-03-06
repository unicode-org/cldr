package org.unicode.cldr.unittest;

import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.VoteResolver.Organization;

import com.ibm.icu.dev.test.TestFmwk;

public class StandardCodesTest extends TestFmwk {
    public static void main(String[] args) {
        new StandardCodesTest().run(args);
    }

    TestInfo testInfo = TestInfo.getInstance();

    public void TestCoverage() {
        Map<String, Map<String, Level>> map = StandardCodes.make().getLocaleTypes();
        EnumSet<Organization> missing = EnumSet.noneOf(Organization.class);
        Set<String> extraOrgs = new TreeSet<String>();
        for (String org : map.keySet()) {
            extraOrgs.add(org.toLowerCase());
        }
        for (Organization org : Organization.values()) {
            // Sun ; ar ; modern
            final Map<String, Level> entrySet = map.get(org.toString());
            extraOrgs.remove(org.toString().toLowerCase());
            if (entrySet == null) {
                missing.add(org);
                continue;
            }
            for (Entry<String, Level> entry2 : entrySet.entrySet()) {
                final String locale = entry2.getKey();
                String name = locale.equals("*") ? "ALL" : testInfo.getEnglish().getName(locale);
                logln(org + "\t;\t" + locale + "\t;\t" + entry2.getValue() + "\t;\t" + name);
            }
        }
        for (Organization org : missing) {
            errln("Organization missing Locales.txt information " + org);
        }
        for (String org : extraOrgs) {
            errln("Organization in Locales.txt but not in Organization enum: " + org);
        }
    }

    public void TestGetLocaleCoverageLocales() {
        Factory cldrFactory = TestCLDRFile.getAllFactory();
        Set<String> availableLocales = cldrFactory.getAvailable();
        StandardCodes sc = StandardCodes.make();
        for (Organization org : Organization.values()) {
            Set<String> locs;
            try {
                locs = sc.getLocaleCoverageLocales(org.toString());
                for (String loc : locs) {
                    if (loc.equals("*")) continue;
                    if (!availableLocales.contains(loc)) {
                        warnln("Locales.txt:\t" + loc + " (" + CLDRLocale.getInstance(loc).getDisplayName() + ")"
                            + " for " + org + " isn't in CLDR (common/main or seed).");
                    }
                }
            } catch (NullPointerException npe) {
                errln("NPE trying to get coverage for " + org);
                continue;
            }
            // logln(org + " : " + locs.toString());
        }
    }
}
