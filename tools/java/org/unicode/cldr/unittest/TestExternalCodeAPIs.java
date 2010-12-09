package org.unicode.cldr.unittest;

import org.unicode.cldr.unittest.TestAll.TestInfo;

import com.ibm.icu.dev.test.TestFmwk;

public class TestExternalCodeAPIs extends TestFmwk {
  static TestInfo testInfo = TestInfo.getInstance();

  public static void main(String[] args) {
    new TestExternalCodeAPIs().run(args);
  }
}
