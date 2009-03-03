package org.unicode.cldr.draft;

import java.util.Locale;

import com.ibm.icu.util.ULocale;

public class Misc {
public static void main(String[] args) {
  System.out.println(Locale.SIMPLIFIED_CHINESE);
  System.out.println(Locale.TRADITIONAL_CHINESE);
  for (String s : ULocale.getISOCountries()) {
    System.out.println(s + "\t" + ULocale.getDisplayCountry("und-" + s, ULocale.ENGLISH));
  }
}
}
