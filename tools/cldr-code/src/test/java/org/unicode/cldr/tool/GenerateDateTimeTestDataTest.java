package org.unicode.cldr.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ibm.icu.util.ULocale;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.tool.GenerateDateTimeTestData.FieldStyleCombo;
import org.unicode.cldr.tool.GenerateDateTimeTestData.SemanticSkeleton;
import org.unicode.cldr.tool.GenerateDateTimeTestData.SemanticSkeletonLength;
import org.unicode.cldr.tool.GenerateDateTimeTestData.YearStyle;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;

public class GenerateDateTimeTestDataTest {

    @Test
    public void testComputeSkeletonFromSemanticSkeleton() {

        Object[][] casesData = {
            {"en", "gregorian", SemanticSkeleton.YMDE, SemanticSkeletonLength.LONG, "yMMMMdEEEE"},
            {"en", "gregorian", SemanticSkeleton.YMDE, SemanticSkeletonLength.MEDIUM, "yMMMdEEE"},
            {"en", "gregorian", SemanticSkeleton.YMDE, SemanticSkeletonLength.SHORT, "yyMdEEE"},
            {"en", "japanese", SemanticSkeleton.YMDE, SemanticSkeletonLength.LONG, "GyMMMMdEEEE"},
            {"en", "japanese", SemanticSkeleton.YMDE, SemanticSkeletonLength.MEDIUM, "GyMMMdEEE"},
            {"en", "japanese", SemanticSkeleton.YMDE, SemanticSkeletonLength.SHORT, "GGGGGyMdEEE"},
        };

        for (Object[] caseDatum : casesData) {
            String localeTag = (String) caseDatum[0];
            String calendarStr = (String) caseDatum[1];
            SemanticSkeleton semanticSkeleton = (SemanticSkeleton) caseDatum[2];
            SemanticSkeletonLength semanticSkeletonLength = (SemanticSkeletonLength) caseDatum[3];
            String expected = (String) caseDatum[4];

            ULocale locale = ULocale.forLanguageTag(localeTag);
            String localeStr = locale.getName();
            CLDRFile localeCldrFile = GenerateDateTimeTestData.getCLDRFile(localeStr).orElse(null);
            assert localeCldrFile != null;
            ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder(localeCldrFile);

            FieldStyleCombo fieldStyleCombo = new FieldStyleCombo();
            fieldStyleCombo.semanticSkeleton = semanticSkeleton;
            fieldStyleCombo.semanticSkeletonLength = semanticSkeletonLength;

            String actual =
                    GenerateDateTimeTestData.computeSkeletonFromSemanticSkeleton(
                            icuServiceBuilder, localeCldrFile, fieldStyleCombo, calendarStr);

            assertEquals(
                    expected,
                    actual,
                    "skeleton string for locale "
                            + localeStr
                            + ", calendar "
                            + calendarStr
                            + ", semantic skeleton "
                            + semanticSkeleton.toString()
                            + ", skeleton length "
                            + semanticSkeletonLength.getLabel());
        }
    }

    @Test
    public void testComputeSkeletonFromSemanticSkeleton_applyYearStyle() {

        Object[][] casesData = {
            {
                "en",
                "gregorian",
                SemanticSkeleton.YMDE,
                SemanticSkeletonLength.SHORT,
                YearStyle.WITH_ERA,
                "GyMdEEE"
            },
            {
                "en",
                "gregorian",
                SemanticSkeleton.YMDE,
                SemanticSkeletonLength.SHORT,
                YearStyle.FULL,
                "yMdEEE"
            },
            {
                "en",
                "gregorian",
                SemanticSkeleton.YMDE,
                SemanticSkeletonLength.SHORT,
                YearStyle.AUTO,
                "yyMdEEE"
            },
            {
                "en",
                "japanese",
                SemanticSkeleton.YMDE,
                SemanticSkeletonLength.SHORT,
                YearStyle.WITH_ERA,
                "GGGGGyMdEEE"
            },
            {
                "en",
                "japanese",
                SemanticSkeleton.YMDE,
                SemanticSkeletonLength.SHORT,
                YearStyle.FULL,
                "GGGGGyMdEEE"
            },
            {
                "en",
                "japanese",
                SemanticSkeleton.YMDE,
                SemanticSkeletonLength.SHORT,
                YearStyle.AUTO,
                "GGGGGyMdEEE"
            },
        };

        for (Object[] caseDatum : casesData) {
            String localeTag = (String) caseDatum[0];
            String calendarStr = (String) caseDatum[1];
            SemanticSkeleton semanticSkeleton = (SemanticSkeleton) caseDatum[2];
            SemanticSkeletonLength semanticSkeletonLength = (SemanticSkeletonLength) caseDatum[3];
            YearStyle yearStyle = (YearStyle) caseDatum[4];
            String expected = (String) caseDatum[5];

            ULocale locale = ULocale.forLanguageTag(localeTag);
            String localeStr = locale.getName();
            CLDRFile localeCldrFile = GenerateDateTimeTestData.getCLDRFile(localeStr).orElse(null);
            assert localeCldrFile != null;
            ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder(localeCldrFile);

            FieldStyleCombo fieldStyleCombo = new FieldStyleCombo();
            fieldStyleCombo.semanticSkeleton = semanticSkeleton;
            fieldStyleCombo.semanticSkeletonLength = semanticSkeletonLength;
            fieldStyleCombo.yearStyle = yearStyle;

            String actual =
                    GenerateDateTimeTestData.computeSkeletonFromSemanticSkeleton(
                            icuServiceBuilder, localeCldrFile, fieldStyleCombo, calendarStr);

            assertEquals(
                    expected,
                    actual,
                    "skeleton string for locale "
                            + localeStr
                            + ", calendar "
                            + calendarStr
                            + ", semantic skeleton "
                            + semanticSkeleton.toString()
                            + ", skeleton length "
                            + semanticSkeletonLength.getLabel()
                            + ", year style length "
                            + yearStyle.getLabel());
        }
    }
}
