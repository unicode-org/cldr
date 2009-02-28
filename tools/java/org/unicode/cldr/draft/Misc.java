package org.unicode.cldr.draft;

import com.ibm.icu.util.ULocale;

public class Misc {
public static void main(String[] args) {
  for (String s : ULocale.getISOCountries()) {
    System.out.println(s + "\t" + ULocale.getDisplayCountry("und-" + s, ULocale.ENGLISH));
  }
}
}
