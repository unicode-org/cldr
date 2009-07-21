package org.unicode.cldr.unittest;

import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.IsoCurrencyParser;
import com.ibm.icu.dev.test.util.Relation;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.IsoCurrencyParser.Data;

import com.ibm.icu.dev.test.TestFmwk;

public class TestExternalCodeAPIs extends TestFmwk {
  static TestInfo testInfo = TestInfo.getInstance();

  public static void main(String[] args) {
    new TestExternalCodeAPIs().run(args);
  }
}
