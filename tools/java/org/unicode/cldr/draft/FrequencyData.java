package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Set;

import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.UnicodeMap;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class FrequencyData {

  public static long loadFrequencies(String frequencyFile, UnicodeMap result) throws IOException {
    BufferedReader in = GenerateNormalizeForMatch.openUTF8Reader(frequencyFile);
    long total = 0;
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      int commentPos = line.indexOf("#");
      if (commentPos >= 0) {
        line = line.substring(0,commentPos);
      }
      line = line.trim();
      if (line.length() == 0) continue;
      String[] pieces = line.split("\\s*;\\s*");
      String code = GenerateNormalizeForMatch.fromHex(pieces[0]);
      long count = Long.parseLong(pieces[1]);
      total += count;
      result.put(code, count);
    }
    in.close();
    result.freeze();
    return total;
  }
  
  static final double[] standardDeviation = {
          0,
          68.27d/100,
          95.450d/100,
          99.7300d/100,
          99.993666d/100,
          99.99994267d/100,
          99.9999998027d/100,
          99.9999999997440d/100
  };
  
  /*
  1σ  68.27%
  1.645σ  90%
  1.960σ  95%
  2σ  95.450%
  2.576σ  99%
  3σ  99.7300%
  3.2906σ   99.9%
  4σ  99.993666%
  5σ  99.99994267%
  6σ  99.9999998027%
  7σ  99.9999999997440%
  */

  public static void main(String[] args) throws IOException {
    String frequencyFile = args[0];
    UnicodeMap frequencies = new UnicodeMap();
    long count = loadFrequencies(frequencyFile, frequencies);
    Counter<Integer> counter = new Counter();
    
    int propEnum = UCharacter.getPropertyEnum("script");
    UnicodeSet valueChars = new UnicodeSet();
    
    for (int i = UCharacter.getIntPropertyMinValue(propEnum); i <= UCharacter.getIntPropertyMaxValue(propEnum); ++i) {
      String valueAlias = UCharacter.getPropertyValueName(propEnum, i, UProperty.NameChoice.LONG);
      if (valueAlias.equalsIgnoreCase("common") || valueAlias.equalsIgnoreCase("inherited")) continue;
      System.out.println("@" + valueAlias);
      valueChars.clear();
      valueChars.applyPropertyAlias("script", valueAlias);
      if (valueChars.size() == 0) continue;
      
      counter.clear();
      for (UnicodeSetIterator it = new UnicodeSetIterator(valueChars); it.next();) {
        final Long frequency = (Long) frequencies.getValue(it.codepoint);
        String norm = Normalizer.normalize(it.codepoint, Normalizer.COMPOSE);
        norm = UCharacter.foldCase(norm, true);
        norm = Normalizer.normalize(norm, Normalizer.COMPOSE);
        int cp;
        for (int j = 0; j < norm.length(); j += UTF16.getCharCount(cp)) {
          cp = UTF16.charAt(norm, j);
          counter.add(cp, frequency == null ? 0 : frequency);
        }
      }
      double total = counter.getTotal();
      double totalFrequency = 0;
      
      for (int cp : counter.getKeysetSortedByCount(false)) {
        totalFrequency += counter.getCount(cp)/total;
        final int sd = sd(totalFrequency);
        if (sd == standardDeviation.length) break;
        boolean isNFKC = Normalizer.isNormalized(cp, Normalizer.COMPOSE_COMPAT, 0);
        System.out.println(new StringBuilder().appendCodePoint(cp) + "\t" + (totalFrequency*100) + "%\t" + sd + "\t" + (isNFKC ? "" : "K"));
      }
      System.out.println();
    }
  }

  private static int sd(double totalFrequency) {
    for (int i = standardDeviation.length - 1; i > 0; --i) {
      if (totalFrequency > standardDeviation[i]) {
        return i + 1;
      }
    }
    return 1;
  }
}
