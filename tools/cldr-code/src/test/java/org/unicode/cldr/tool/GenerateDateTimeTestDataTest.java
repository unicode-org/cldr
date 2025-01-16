package org.unicode.cldr.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.ibm.icu.util.ULocale;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.tool.GenerateDateTimeTestData.FieldStyleCombo;
import org.unicode.cldr.tool.GenerateDateTimeTestData.SemanticSkeleton;
import org.unicode.cldr.tool.GenerateDateTimeTestData.SemanticSkeletonLength;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;

public class GenerateDateTimeTestDataTest {

  @Test
  public void testComputeSkeletonFromSemanticSkeleton() {

    Object[][] casesData = {
        {"en", "gregorian",  SemanticSkeleton.YMDE, SemanticSkeletonLength.LONG,   "yMMMMd"},
        {"en", "gregorian",  SemanticSkeleton.YMDE, SemanticSkeletonLength.MEDIUM, "yMMMd"},
        {"en", "gregorian",  SemanticSkeleton.YMDE, SemanticSkeletonLength.SHORT,  "yyMd"},
        {"en", "japanese", SemanticSkeleton.YMDE, SemanticSkeletonLength.LONG,   "GyMMMMd"},
        {"en", "japanese", SemanticSkeleton.YMDE, SemanticSkeletonLength.MEDIUM, "GyMMMd"},
        {"en", "japanese", SemanticSkeleton.YMDE, SemanticSkeletonLength.SHORT,  "GGGGGyMd"},
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
      ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
      icuServiceBuilder.clearCache();
      icuServiceBuilder.setCldrFile(localeCldrFile);

      FieldStyleCombo fieldStyleCombo = new FieldStyleCombo();
      fieldStyleCombo.semanticSkeleton = semanticSkeleton;
      fieldStyleCombo.semanticSkeletonLength = semanticSkeletonLength;

      String actual = GenerateDateTimeTestData.computeSkeletonFromSemanticSkeleton(
          icuServiceBuilder, localeCldrFile, fieldStyleCombo, calendarStr);

      assertEquals(actual, expected);
    }
  }

}
