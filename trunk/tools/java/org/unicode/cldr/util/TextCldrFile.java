package org.unicode.cldr.util;

import java.util.Arrays;

// make this a JUnit test?
public class TextCldrFile {
  public static void main(String[] args) {
    testAlias();
    System.out.println("No Errors");
  }

  private static void testAlias() {
    String[][] testCases = {
        {"//ldml/foo[@fii=\"abc\"]", "//ldml"},
        {"//ldml/foo[@fii=\"ab/c\"]", "//ldml"},
        {"//ldml/foo[@fii=\"ab/[c\"]", "//ldml"},
    };
    for (String[] pair : testCases) {
      if (!XMLSource.Alias.stripLastElement(pair[0]).equals(pair[1])) {
        throw new IllegalArgumentException(Arrays.asList(pair).toString());
      }
    }
  }
}