package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UExtension;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;

public class TestUExtension extends TestFmwk {

  static SupplementalDataInfo data = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);

  public static void main(String[] args) {
    new TestUExtension().run(args);
  }

  public void TestBasic() {
    Relation<String,String> validKeyTypes = data.getBcp47Keys();
    for (String key : validKeyTypes.keySet()) {
      logln(key + "\t" + validKeyTypes.getAll(key));
    }

    UExtension uExtension;

    uExtension = new UExtension().parse("ca-buddhist-co-dict");
    assertTrue("", uExtension.getKeys().contains("co"));
    logln("ca-buddhist-co-dict" + "\t" + uExtension);
    
    uExtension = new UExtension().parse("vt-12345-0061");
    assertTrue("", uExtension.getKeys().contains("vt"));
    logln("vt-12345" + "\t" + uExtension);
    
    uExtension = new UExtension().parse("vt-1");
  }
}
