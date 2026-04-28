package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrDateTimePatternGenerator;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.TempPrintWriter;

public class GenerateDateSkeletonTestData {

    private static final String OUTPUT_SUBDIR = "datetime";
    private static final String OUTPUT_FILENAME = "skeletons.json";

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final Factory CLDR_FACTORY = CLDR_CONFIG.getCldrFactory();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final ImmutableSet<String> LOCALES =
            ImmutableSet.of(
                    "en_US", "en_GB", "zh_Hant_TW", "vi", "ar", "mt_MT", "bn", "zu", "ja", "ru");

    private static final ImmutableSet<String> SKELETONS =
            ImmutableSet.of(
                    "yMd", "yMMMMd", "yMdE", "GyMd", "hm", "Hms", "yMdHmsv", "Bh", "yQQQ", "jjm");

    private static final ImmutableSet<String> CALENDARS =
            ImmutableSet.of("gregorian", "japanese", "islamic", "buddhist");

    static class TestCaseSerde {
        String locale;
        String skeleton;
        String calendar;
        String pattern;
    }

    public static void main(String[] args) throws IOException {
        List<TestCaseSerde> results = new ArrayList<>();

        for (String locale : LOCALES) {
            CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);
            for (String calendar : CALENDARS) {
                CldrDateTimePatternGenerator generator =
                        new CldrDateTimePatternGenerator(cldrFile, calendar, false);
                for (String skeleton : SKELETONS) {
                    List<String> trace = new ArrayList<>();
                    String pattern = generator.getBestPattern(skeleton, trace);

                    TestCaseSerde result = new TestCaseSerde();
                    result.locale = locale;
                    result.skeleton = skeleton;
                    result.calendar = calendar;
                    result.pattern = pattern;
                    results.add(result);
                }
            }
        }

        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(
                        CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, OUTPUT_FILENAME)) {
            pw.println(GSON.toJson(results));
        }
    }
}
