package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.UnicodeMap;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.Normalizer.Mode;

public class FrequencyData {

  private static final UnicodeSet NO_SCRIPT = new UnicodeSet("[[:script=common:][:script=inherited:][:script=unknown:]]");
  private static final UnicodeSet PRIVATE_USE = new UnicodeSet("[:Co:]");
  private UnicodeMap frequencies = new UnicodeMap();
  private int total;

  public FrequencyData(String frequencyFile) throws IOException {
    BufferedReader in = GenerateNormalizeForMatch.openUTF8Reader(frequencyFile);
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
      frequencies.put(code, count);
    }
    in.close();
    frequencies.freeze();
  }


  public long getCount(int codepoint) {
    Long result = (Long) frequencies.getValue(codepoint);
    return result == null ? 0 : result;
  }

  static final double[] standardDeviation = {
    0d,
    0.682689492137d, // 1
    0.954499736104d, // 2
    0.997300203937d, // 3
    0.999936657516d, // 4
    0.999999426697d, // 5
    0.999999998027d, // 6
    0.999999999997440d // 7
  };
  private static final UnicodeSet nonNFKC = new UnicodeSet("[:nfkcqc=n:]");
  
  public static double getStandardDeviationLimit(int i) {
    return standardDeviation[i];
  }

  public static int standardDeviationInterval(double totalFrequency) {
    for (int i = standardDeviation.length - 1; i > 0; --i) {
      if (totalFrequency > standardDeviation[i]) {
        return i + 1;
      }
    }
    return 1;
  }

  private static class Rank {
    int rank;
    double frequency;
    double cummulative;
  }

  public class RelativeFrequency {
    private int[] rank2codepoint;
    private Map<Integer,Rank> rankInfo = new HashMap<Integer,Rank>();
    private double totalRelative;

    public double getTotalRelative() {
      return totalRelative;
    }
    private RelativeFrequency(UnicodeSet withinSet, Mode compose) {
      Counter<Integer> counter = new Counter<Integer>();
      for (UnicodeSetIterator it = new UnicodeSetIterator(withinSet); it.next();) {
        final long frequency = getCount(it.codepoint);
        if (frequency == 0) continue;
        if (compose == null) {
          counter.add(it.codepoint, frequency);
        } else {
          String norm = Normalizer.normalize(it.codepoint, compose);
          norm = UCharacter.foldCase(norm, true);
          norm = Normalizer.normalize(norm, compose);
          int cp;
          for (int j = 0; j < norm.length(); j += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(norm, j);
            counter.add(cp, frequency);
          }
        }
      }
      rank2codepoint = new int[counter.getItemCount()];
      totalRelative = counter.getTotal();
      double totalFrequency = 0;
      int itemRank = 0;
      for (int cp : counter.getKeysetSortedByCount(false)) {
        Rank rank2 = new Rank();
        rank2codepoint[itemRank] = cp;
        rank2.rank = itemRank++;
        final long frequency = counter.getCount(cp);
        rank2.frequency = frequency/totalRelative;
        totalFrequency += frequency;
        rank2.cummulative = totalFrequency/totalRelative;
        rankInfo.put(cp, rank2);
      }
    }
    public long getRankCount() {
      return rank2codepoint.length;
    }
    public long getRank(int codepoint) {
      return rankInfo.get(codepoint).rank;
    }
    public double getFrequency(int codepoint) {
      final Rank rank = rankInfo.get(codepoint);
      return rank == null ? 0d : rank.frequency;
    }
    public double getCumulative(int codepoint) {
      final Rank rank = rankInfo.get(codepoint);
      return rank == null ? 0d : rank.cummulative;
    }
    public int getCodePointAtRank(int rankLevel) {
      return rank2codepoint[rankLevel];
    }
  }

  private RelativeFrequency getRelativeFrequency(UnicodeSet withinSet, Mode compose) {
    return new RelativeFrequency(withinSet, compose);
  }
  
  static     NumberFormat nf = NumberFormat.getInstance();
  static {
    nf.setGroupingUsed(true);
  }

  public static void main(String[] args) throws IOException {
    String frequencyFile = args[0];

    FrequencyData data = new FrequencyData(frequencyFile);
    

    System.out.print("Script" + "\t");
    System.out.print(0.0d + "\t");
    for (double item = 0.005; item < 1.0; item += item) {
      System.out.print(item + "\t");
    }
    System.out.print(1.0d + "\t");
    System.out.println("Total");

    data.showData(UCharacter.getPropertyEnum("script"), NO_SCRIPT);
    data.showData(UCharacter.getPropertyEnum("gc"), new UnicodeSet(NO_SCRIPT).complement());

//    data.showData("Private Use", PRIVATE_USE);
//    RelativeFrequency relative = data.getRelativeFrequency(new UnicodeSet("[:script=unknown:]"), Normalizer.NFKC);
//    System.out.println(relative.getTotalRelative());
//    for (int i = 0; i < 10; ++i) {
//      int cp = relative.getCodePointAtRank(i);
//      double totalFrequency = relative.getCumulative(cp);
//      System.out.println(Integer.toHexString(cp) + "\t" + totalFrequency);
//    }
  }


  private void showData(int propEnum, UnicodeSet exclusions) {
    for (int i = UCharacter.getIntPropertyMinValue(propEnum); i <= UCharacter.getIntPropertyMaxValue(propEnum); ++i) {
      String valueAlias = UCharacter.getPropertyValueName(propEnum, i, UProperty.NameChoice.LONG);
      String shortValueAlias = UCharacter.getPropertyValueName(propEnum, i, UProperty.NameChoice.SHORT);
      //if (valueAlias.equalsIgnoreCase("common") || valueAlias.equalsIgnoreCase("inherited")) continue;
      UnicodeSet valueChars = new UnicodeSet();

      valueChars.applyPropertyAlias(UCharacter.getPropertyName(propEnum, UProperty.NameChoice.SHORT), shortValueAlias);
      valueChars.removeAll(exclusions);
      if (valueChars.size() == 0) continue;
      showData(shortValueAlias + " - " + valueAlias, valueChars);
    }
  }


  private void showData(String title, UnicodeSet valueChars) {
    RelativeFrequency relative;
    relative = getRelativeFrequency(valueChars, Normalizer.NFKC);
    UnicodeMap sds = new UnicodeMap();
    for (int rank = 0; rank < relative.getRankCount(); ++rank) {
      int cp = relative.getCodePointAtRank(rank);
      double totalFrequency = relative.getCumulative(cp);
      final int sd = standardDeviationInterval(totalFrequency);
      sds.put(cp, sd);
      if (sd == standardDeviation.length) break;
      boolean isNFKC = Normalizer.isNormalized(cp, Normalizer.COMPOSE_COMPAT, 0);
      //System.out.println(new StringBuilder().appendCodePoint(cp) + "\t" + (totalFrequency*100) + "%\t" + sd + "\t" + (isNFKC ? "" : "K"));
    }
    
    double nfkcSize = new UnicodeSet(valueChars).removeAll(nonNFKC).size();

    int total = 0;
    UnicodeSet totalSet = new UnicodeSet();

    System.out.print(title + ": " + nf.format(nfkcSize) + "\t");
    System.out.print(0.0d + "\t");

    for (double item = 0.005; item < 1.0; item += item) {
      int intRank = (int) Math.round(item * nfkcSize);
      if (intRank >= relative.getRankCount()) {
        System.out.print(1.0 + "\t");
        continue;
      }
      int cp = relative.getCodePointAtRank(intRank);
      double totalFrequency = relative.getCumulative(cp);
      System.out.print(totalFrequency + "\t");
    }
    System.out.print(1.0d + "\t");

    System.out.println(relative.getTotalRelative());
  }

}
