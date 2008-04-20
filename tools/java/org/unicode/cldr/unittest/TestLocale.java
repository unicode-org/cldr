package org.unicode.cldr.unittest;

import org.unicode.cldr.test.CheckDates;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;

public class TestLocale extends TestFmwk {
  static TestInfo testInfo = TestInfo.getInstance();

  public static void main(String[] args) {
    new TestLocale().run(args);
  }

  public void TestLocaleNamePattern() {
    assertEquals("Locale name", "Chinese", testInfo.getEnglish().getName("zh"));
    assertEquals("Locale name", "Chinese (United States)", testInfo.getEnglish().getName("zh-US"));
    assertEquals("Locale name", "Chinese (Arabic, United States)", testInfo.getEnglish().getName("zh-Arab-US"));
    CLDRFile japanese = testInfo.getCldrFactory().make("ja", true);
    assertEquals("Locale name", "中国語", japanese.getName("zh"));
    assertEquals("Locale name", "中国語(アメリカ合衆国)", japanese.getName("zh-US"));
    assertEquals("Locale name", "中国語(アラビア文字\uFF0Cアメリカ合衆国)", japanese.getName("zh-Arab-US"));
  }
  public void TestExtendedLanguage() {
    assertEquals("Extended language translation", "Simplified Chinese", testInfo.getEnglish().getName("zh_Hans"));
    assertEquals("Extended language translation", "Simplified Chinese (Singapore)", testInfo.getEnglish().getName("zh_Hans_SG"));
    assertEquals("Extended language translation", "U.S. English", testInfo.getEnglish().getName("en-US"));
    assertEquals("Extended language translation", "U.S. English (Arabic)", testInfo.getEnglish().getName("en-Arab-US"));
  }
  public void TestNarrowEnough() {
    BreakIterator bi = BreakIterator.getCharacterInstance(ULocale.ENGLISH);
    assertEquals("Narrow Enough", 1, CheckDates.isNarrowEnough("a", bi));
    assertEquals("Narrow Enough", 2, CheckDates.isNarrowEnough("ab", bi));
    assertEquals("Narrow Enough", 2, CheckDates.isNarrowEnough("abc", bi));
    assertEquals("Narrow Enough", 4, CheckDates.isNarrowEnough("a\u0308b\u0308", bi));
    assertEquals("Narrow Enough", 4, CheckDates.isNarrowEnough("a\u0308b\u0308c\u0308", bi));
  }
}