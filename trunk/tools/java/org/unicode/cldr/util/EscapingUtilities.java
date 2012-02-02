package org.unicode.cldr.util;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import com.ibm.icu.text.UnicodeSet;

public class EscapingUtilities {
  public static UnicodeSet OK_TO_NOT_QUOTE = (UnicodeSet) new UnicodeSet("[!(-*,-\\:A-Z_a-z~]").freeze();
  
  public static String urlEscape(String path) {
    try {
      StringBuilder result = new StringBuilder();
      byte[] bytes = path.getBytes("utf-8");
      for (byte b : bytes) {
        char c = (char)(b & 0xFF);
        if (OK_TO_NOT_QUOTE.contains(c)) {
          result.append(c);
        } else {
          result.append('%');
          if (c < 16) {
            result.append('0');
          }
          result.append(Integer.toHexString(c).toUpperCase(Locale.ENGLISH));
        }
      }
      return result.toString();
    } catch (UnsupportedEncodingException e) {
      throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
    }
  }
}
