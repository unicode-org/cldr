package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.TestFmwk;

public class TestLocale extends TestFmwk {
  public static void main(String[] args) {
    new TestLocale().run(args);
  }

  Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*", DraftStatus.approved);
  CLDRFile english = cldrFactory.make("en", true);
  CLDRFile root = cldrFactory.make("root", true);
  SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(Utility.SUPPLEMENTAL_DIRECTORY);
  StandardCodes sc = StandardCodes.make();

  public void TestLocaleNamePattern() {
    assertEquals("Locale name", "Chinese", english.getName("zh"));
    assertEquals("Locale name", "Chinese (United States)", english.getName("zh-US"));
    assertEquals("Locale name", "Chinese (Arabic, United States)", english.getName("zh-Arab-US"));
    CLDRFile japanese = cldrFactory.make("ja", true);
    assertEquals("Locale name", "中国語", japanese.getName("zh"));
    assertEquals("Locale name", "中国語（アメリカ合衆国）", japanese.getName("zh-US"));
    assertEquals("Locale name", "中国語（アラビア文字、アメリカ合衆国）", japanese.getName("zh-Arab-US"));
  }
}