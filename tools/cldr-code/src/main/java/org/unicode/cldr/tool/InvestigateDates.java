package org.unicode.cldr.tool;

import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.ibm.icu.text.SimpleFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.DatetimeUtilities;
import org.unicode.cldr.util.DatetimeUtilities.Calendar;
import org.unicode.cldr.util.DatetimeUtilities.DatePatternInfo;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

public class InvestigateDates {
    private static final boolean VERBOSE = true;
    private static final boolean SHORT_RUN = false;
    private static final String DEBUG_STOP = "zzz";

    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    private static final Factory CLDR_FACTORY = CONFIG.getCldrFactory();
    private static final Comparator<Iterable<String>> LEX_ITERABLE_COMPARATOR =
            Ordering.natural().lexicographical();

    public static void main(String[] args) {
        Set<String> localesToCheck =
                SHORT_RUN
                        ? Set.of("de", "fr", "en", "root")
                        : Sets.union(
                                StandardCodes.make().getLocaleCoverageLocales(Organization.cldr),
                                Set.of("root"));

        System.out.println(
                Joiners.TAB.join(
                        "locale",
                        "calendar",
                        "ok?",
                        "skeleton",
                        "pattern",
                        "trial pattern",
                        "pattern for skeleton without v",
                        "composing pattern"));

        for (String locale : Sets.newTreeSet(localesToCheck)) {
            if (locale.equals(DEBUG_STOP)) {
                break;
            }
            CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);
            Map<Calendar, DatePatternInfo> calToDPI =
                    DatetimeUtilities.calendarToDatePatternInfo(cldrFile, false);
            for (Entry<Calendar, DatePatternInfo> entry : calToDPI.entrySet()) {
                Calendar calendar = entry.getKey();
                DatePatternInfo dpi = entry.getValue();
                check(locale, calendar, dpi);
            }
        }
    }

    enum Compare {
        ok,
        fix_space,
        fix_other;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }

    private static void check(String locale, Calendar calendar, DatePatternInfo dpi) {
        // first, see if the v patterns are always composable
        Map<String, String> availableSkeletonToPattern = dpi.getAvailableSkeletonToPattern();
        for (Entry<String, String> entry : availableSkeletonToPattern.entrySet()) {
            String skeleton = entry.getKey();
            String pattern = entry.getValue();
            if (skeleton.contains("v")) {
                String skeletonWithoutV = skeleton.replace("v", "");
                String patternForSkeletonWithoutV =
                        availableSkeletonToPattern.get(skeletonWithoutV);
                String composingPattern = dpi.getAppendItems().get("Timezone");
                String trialPattern =
                        SimpleFormatter.compile(composingPattern)
                                .format(patternForSkeletonWithoutV, "v");
                Compare ok = compare(pattern, trialPattern);
                if (VERBOSE || ok != Compare.ok) {
                    System.out.println(
                            Joiners.TAB.join(
                                    locale,
                                    calendar,
                                    ok,
                                    skeleton,
                                    pattern,
                                    trialPattern,
                                    patternForSkeletonWithoutV,
                                    composingPattern));
                }
            }
        }
    }

    static final Pattern regex =
            Pattern.compile(
                    "[\u0009 \u0085 \u2028 \u2029 \u0020\u3000\u1680\u2000-\u2006\u2008-\u200A\u205F\u00A0\u2007\u202F]+");

    private static Compare compare(String pattern, String trialPattern) {
        if (pattern.equals(trialPattern)) return Compare.ok;
        pattern = regex.matcher(pattern).replaceAll(" ");
        trialPattern = regex.matcher(trialPattern).replaceAll(" ");
        if (pattern.equals(trialPattern)) {
            return Compare.fix_space;
        } else {
            return Compare.fix_other;
        }
    }
}
