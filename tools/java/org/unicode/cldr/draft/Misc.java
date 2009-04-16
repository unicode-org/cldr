package org.unicode.cldr.draft;

import java.util.Locale;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class Misc {
public static void main(String[] args) {
  StringTransform unicode = Transliterator.getInstance("hex/unicode");
  UnicodeSet exclude = new UnicodeSet("[:bidimirrored:]");
  for (int i = 0; i < 0x110000; ++i) {
    if (exclude.contains(i)) continue;
    String name = UCharacter.getExtendedName(i);
    if (name == null) continue;
    String reverse = name.replaceAll("RIGHT", "LEFT");
    if (reverse.equals(name)) {
      reverse = name.replaceAll("REVERSED ", "");
      if (reverse.equals(name)) continue;
    }
    int rev = UCharacter.getCharFromName(reverse);
    if (rev == -1) continue;
    System.out.println(
            unicode.transform(UTF16.valueOf(i))
            + "\t" + UTF16.valueOf(i) 
            + "\t" + name
            + "\t" + UTF16.valueOf(rev) 
            + "\t" + unicode.transform(UTF16.valueOf(rev))
            + "\t" + reverse);
  }
  System.out.println(Locale.SIMPLIFIED_CHINESE);
  System.out.println(Locale.TRADITIONAL_CHINESE);
  for (String s : ULocale.getISOCountries()) {
    System.out.println(s + "\t" + ULocale.getDisplayCountry("und-" + s, ULocale.ENGLISH));
  }
}
}
