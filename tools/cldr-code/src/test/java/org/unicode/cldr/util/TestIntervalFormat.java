package org.unicode.cldr.util;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.unicode.cldr.unittest.TestShim.getInclusion;

import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.unicode.cldr.util.DatetimeUtilities.DatePatternInfo;
import org.unicode.cldr.util.NestedMap.Map2;

public class TestIntervalFormat {

    /**
     * copied from org.unicode.cldr.unittest.TestDateOrder#testIntervalConstructor rewritten in
     * JUnit so that we can get per-locale failures
     */
    public static Stream<Arguments> testLocales() {
        final Factory cldrFactory = CLDRConfig.getInstance().getCldrFactory();
        Collection<String> locales =
                getInclusion() < 6
                        ? List.of("en", "ja", "de", "cs", "zh", "zh_Hant")
                        : getInclusion() < 7
                                ? StandardCodes.make().getLocaleCoverageLocales(Organization.cldr)
                                : cldrFactory.getAvailable();

        return locales.stream().map(str -> arguments(str));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testLocales")
    public void testIntervalConstructor(final String locale) {
        final Factory cldrFactory = CLDRConfig.getInstance().getCldrFactory();
        String calendar = "gregorian";
        CLDRFile cldrFile = cldrFactory.make(locale, true);
        DatePatternInfo datePatternInfo =
                DatetimeUtilities.DatePatternInfo.from(cldrFile, calendar);
        CldrIntervalFormat.IntervalPatternConstructor ipu =
                new CldrIntervalFormat.IntervalPatternConstructor(cldrFile, calendar);
        ICUServiceBuilder isb = cldrFactory.getICUServiceBuilder(CLDRLocale.getInstance(locale));
        TimeZone timeZone = TimeZone.getTimeZone("UTC");

        Map2<String, String, String> formatted =
                true
                        ? Map2.create(
                                () ->
                                        (TreeMap<Object, Object>)
                                                new TreeMap(DatetimeUtilities.PATTERN_COMPARATOR),
                                () ->
                                        (TreeMap<Object, Object>)
                                                new TreeMap(DatetimeUtilities.PATTERN_COMPARATOR))
                        : null;
        Output<String> available = new Output<>();
        Output<String> availablePath = new Output<>();
        {
            // simple case for debugging
            String greatestDifference = "H";
            String constructedPattern =
                    ipu.construct("Hv", greatestDifference, availablePath, available);
            try {

                Date sampleEndDate = CldrIntervalFormat.getSampleEndDate(greatestDifference);
                CldrIntervalFormat cif =
                        CldrIntervalFormat.getInstance(calendar, constructedPattern);
                String actualSample =
                        cif.format(
                                CldrIntervalFormat.getSampleStartDate(),
                                sampleEndDate,
                                isb,
                                timeZone,
                                ICUServiceBuilder.NUMBERING_SYSTEM_DEFAULT);
            } catch (IllegalArgumentException e) {
                // TODO CLDR-18980 make this a JUnit parameterized test, so that each
                // locale can fail independently. For now, just fail the whole test, but give some
                // context.
                throw new RuntimeException(
                        "In locale " + locale + " " + " with " + constructedPattern, e);
            }
        }
    }
}
