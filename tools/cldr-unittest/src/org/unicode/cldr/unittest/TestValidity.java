package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.tool.ToolConstants;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.collect.ImmutableSet;

public class TestValidity extends TestFmwkPlus {

    public static void main(String[] args) {
        new TestValidity().run(args);
    }

    Validity validity = Validity.getInstance(CLDRPaths.COMMON_DIRECTORY);

    public void TestBasicValidity() {
        Object[][] tests = {
            { LstrType.language, Validity.Status.regular, true, "aa", "en" },
            { LstrType.language, null, false, "eng" }, // null means never found under any status
            { LstrType.language, null, false, "root" },
            { LstrType.language, Validity.Status.special, true, "mul" },
            { LstrType.language, Validity.Status.deprecated, true, "aju" },
            { LstrType.language, Validity.Status.private_use, true, "qaa" },
            { LstrType.language, Validity.Status.unknown, true, "und" },

            { LstrType.script, Validity.Status.regular, true, "Zyyy" },
            { LstrType.script, Validity.Status.special, true, "Zsye" },
            { LstrType.script, Validity.Status.regular, true, "Zyyy" },
            { LstrType.script, Validity.Status.unknown, true, "Zzzz" },

            { LstrType.region, Validity.Status.deprecated, true, "QU" },
            { LstrType.region, Validity.Status.macroregion, true, "EU" },
            { LstrType.region, Validity.Status.regular, true, "XK" },
            { LstrType.region, Validity.Status.macroregion, true, "001" },
            { LstrType.region, Validity.Status.private_use, true, "AA" },
            { LstrType.region, Validity.Status.unknown, true, "ZZ" },
            
            { LstrType.subdivision, Validity.Status.unknown, true, "KZ-ZZZZ" },
            { LstrType.subdivision, Validity.Status.regular, true, "US-CA" },
            { LstrType.subdivision, Validity.Status.deprecated, true, "AL-BR" },
            
            { LstrType.currency, Validity.Status.regular, true, "USD" },
            { LstrType.currency, Validity.Status.unknown, true, "XXX" },
            { LstrType.currency, Validity.Status.deprecated, true, "ADP" },
            
            { LstrType.unit, Validity.Status.regular, true, "area-acre"},
        };
        for (Object[] test : tests) {
            LstrType lstr = (LstrType) test[0];
            Validity.Status subtypeRaw = (Validity.Status) test[1];
            Boolean desired = (Boolean) test[2];
            for (int i = 3; i < test.length; ++i) {
                String code = (String) test[i];
                List<Status> subtypes = subtypeRaw == null ? Arrays.asList(Status.values()) : Collections.singletonList(subtypeRaw);
                for (Status subtype : subtypes) {
                    Set<String> actual = validity.getData().get(lstr).get(subtype);
                    assertRelation("Validity", desired, CldrUtility.ifNull(actual,Collections.EMPTY_SET), TestFmwkPlus.CONTAINS, code);
                }
            }
        }
        if (isVerbose()) {
            for (Entry<LstrType, Map<Validity.Status, Set<String>>> entry : validity.getData().entrySet()) {
                logln(entry.getKey().toString());
                for (Entry<Validity.Status, Set<String>> entry2 : entry.getValue().entrySet()) {
                    logln("\t" + entry2.getKey());
                    logln("\t\t" + entry2.getValue());
                }
            }
        }
    }

    static final Set<String> ALLOWED_UNDELETIONS = ImmutableSet.of("NL-BQ1", "NL-BQ2", "NL-BQ3", "NO-21", "NO-22");
    static final Set<String> ALLOWED_MISSING = ImmutableSet.of("root");

    public void TestCompatibility() {
        // Only run the rest in exhaustive mode, since it requires CLDR_ARCHIVE_DIRECTORY
        if (getInclusion() <= 5) {
            return;
        }

        final String oldCommon = CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + ToolConstants.PREVIOUS_CHART_VERSION + "/common/";
        Validity oldValidity = Validity.getInstance(oldCommon);
        for (Entry<LstrType, Map<Status, Set<String>>> e1 : oldValidity.getData().entrySet()) {
            LstrType type = e1.getKey();
            for (Entry<Status, Set<String>> e2 : e1.getValue().entrySet()) {
                Status oldStatus = e2.getKey();
                for (String code : e2.getValue()) {
                    Status newStatus = getNewStatus(type, code);
                    if (oldStatus == newStatus) {
                        continue;
                    }
                    if (newStatus == null && !ALLOWED_MISSING.contains(code)) {
                        errln(type + ":" + code + ":" + oldStatus + " — missing in new data");
                    } 
                    if (oldStatus == Status.deprecated && !ALLOWED_UNDELETIONS.contains(code)) {
                        errln(type + ":" + code + ":" + oldStatus + " => " + newStatus 
                            + " // add to exception list if really un-deprecated");
                    } else {
                        logln(type + ":" + code + " was " + oldStatus + " => " + newStatus);
                    }
                }
            }
        }
    }

    private Status getNewStatus(LstrType type, String code) {
        Map<Status, Set<String>> info = validity.getData().get(type);
        for (Entry<Status, Set<String>> e : info.entrySet()) {
            if (e.getValue().contains(code)) {
                return e.getKey();
            }
        }
        return null;
    }
}
