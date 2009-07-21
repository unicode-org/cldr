package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.Counter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.Normalizer.Mode;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class FrequencyData {

  private static final boolean MARKUP = false;
  private static final boolean MAP_CASE = true;

  private static final UnicodeSet NO_SCRIPT = new UnicodeSet("[[:script=common:][:script=inherited:][:script=unknown:]]");
  private static final UnicodeSet PRIVATE_USE = new UnicodeSet("[:Co:]");
  static final UnicodeSet NfcNo = (UnicodeSet) new UnicodeSet("[:nfcqc=no:]").freeze();
  static final UnicodeSet NfcMaybe = (UnicodeSet) new UnicodeSet("[:nfcqc=maybe:]").freeze();
  static final Transliterator fixOutput = Transliterator.createFromRules("fix", "" +
  		"([[:di:][:whitespace:][:co:]\"'']) > &any-hex/unicode($1) ;" +
  		"", Transliterator.FORWARD);
  
  private int total;
//  private Counter<String> langNfcNo = new Counter<String>();
//  private Counter<String> langNfcMaybe = new Counter<String>();
//  private Counter<String> langTotal = new Counter<String>();
//  private Counter<String> langUpper = new Counter<String>();
  private Map<String,Counter<Integer>> langData = new HashMap();
  private Counter<Integer> frequencies = new Counter<Integer>();
  {
    langData.put("mul", frequencies);
  }

  
  /**
The 1st column is the code point.

2nd is detected language

Then there are 3 groups of 4 columns, where each group is:

pre-HTML code point count post-HTML code point count, document count, UTF-8 document count

The 1st group includes "bad" docs (error during input conversion or
contains unassigned or high private use), 2nd group excludes "bad"
docs, 3rd group is multiplied by pagerank (and excludes "bad" docs).

Then there are up to 3 groups, where each group is:

navboost, pagerank, language, encoding, url

   * @param frequencyFile
   * @throws IOException
   */
  static final int postFrequencyIndex = 2 + 4 + 1;
  static final int preFrequencyIndex = 2 + 4 + 0;

  public FrequencyData(String frequencyFile, boolean showProgress) throws IOException {
    BufferedReader in = GenerateNormalizeForMatch.openUTF8Reader(frequencyFile);
    for (int lineCount = 0; ; ++lineCount) {
      String line = in.readLine();
      if (line == null) break;
      int commentPos = line.indexOf("#");
      if (commentPos >= 0) {
        line = line.substring(0,commentPos);
      }
      line = line.trim();
      if (line.length() == 0) continue;
      String[] pieces = line.split("\\s+");
      int code = Integer.parseInt(pieces[0], 16);
      
      if (showProgress && lineCount < 100 || (lineCount % 1000000) == 0 || code == 0x03C2) {
        System.out.println(lineCount + "\t" + line);
      }

      if (code < 0x20) code = 0x20;
      if (MAP_CASE) {
        code = UCharacter.toLowerCase(code);
      }
      long count = MARKUP
      ? Math.max(0,Long.parseLong(pieces[preFrequencyIndex]) - Long.parseLong(pieces[postFrequencyIndex]))
              : Long.parseLong(pieces[postFrequencyIndex]);
      String lang = pieces[1];
      Counter<Integer> langCounter = langData.get(lang);
      if (langCounter == null) {
        langData.put(lang, langCounter = new Counter<Integer>());
      }
      langCounter.add(code, count);
//      if (NfcNo.contains(code)) {
//        langNfcNo.add(lang, count);
//      } else if (NfcMaybe.contains(code)) {
//        langNfcMaybe.add(lang, count);
//      }
//      if (UCharacter.isUpperCase(code)) {
//        langUpper.add(lang, count);
//      }
//      langTotal.add(lang, count);
      total += count;
      frequencies.add(code, count);
    }
    in.close();
  }


  public long getCount(int codepoint) {
    Long result = (Long) frequencies.getCount(codepoint);
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
    public double getFractionOfWhole() {
      return totalRelative/frequencies.getTotal();
    }
  }

  private RelativeFrequency getRelativeFrequency(UnicodeSet withinSet, Mode compose) {
    return new RelativeFrequency(withinSet, compose);
  }
  
  static     NumberFormat nf = NumberFormat.getInstance();
  static {
    nf.setGroupingUsed(true);
  }

  private void showData(String category, int propEnum, UnicodeSet exclusions) {
    for (int i = UCharacter.getIntPropertyMinValue(propEnum); i <= UCharacter.getIntPropertyMaxValue(propEnum); ++i) {
      String valueAlias = UCharacter.getPropertyValueName(propEnum, i, UProperty.NameChoice.LONG);
      String shortValueAlias = UCharacter.getPropertyValueName(propEnum, i, UProperty.NameChoice.SHORT);
      //if (valueAlias.equalsIgnoreCase("common") || valueAlias.equalsIgnoreCase("inherited")) continue;
      UnicodeSet valueChars = new UnicodeSet();

      valueChars.applyPropertyAlias(UCharacter.getPropertyName(propEnum, UProperty.NameChoice.SHORT), shortValueAlias);
      valueChars.removeAll(exclusions);
      if (valueChars.size() == 0) continue;
      showData(category, shortValueAlias + " - " + valueAlias, valueChars);
    }
  }
  
  private void showData2(String category, UnicodeProperty prop, UnicodeSet exclusions, boolean differences) {
    UnicodeSet last = new UnicodeSet();
    for (Object value : prop.getAvailableValues()) {
      String valueAlias = (String) value;
      //if (valueAlias.equalsIgnoreCase("common") || valueAlias.equalsIgnoreCase("inherited")) continue;
      UnicodeSet valueChars = new UnicodeSet();

      valueChars.applyPropertyAlias(prop.getName(), valueAlias);
      valueChars.removeAll(exclusions);
      if (differences) {
        valueChars.removeAll(last);
        last.addAll(valueChars);
      }
      if (valueChars.size() == 0) continue;
      showData(category, valueAlias, valueChars);
    }
  }


  private void showData(String category, String title, UnicodeSet valueChars) {
    RelativeFrequency relative = getRelativeFrequency(valueChars, null); // Normalizer.NFKC
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

    System.out.print(category + "\t" + title + "\t" + nf.format(nfkcSize) + "\t");
    System.out.print(relative.getFractionOfWhole() + "\t");
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

    System.out.print(relative.getTotalRelative());
    long maxCount = relative.getRankCount();
    if (maxCount > 10) {
      maxCount = 10;
    }
    System.out.print('\t');
    for (int i = 0; i < maxCount; ++i) {
      if (i != 0) {
        System.out.print(", ");
      }
      final int codePointAtRank = relative.getCodePointAtRank(i);
      System.out.print(fixOutput.transform(UTF16.valueOf(codePointAtRank)));
    }
    if (relative.getRankCount() > maxCount) {
      System.out.print(", ...");
    }
    System.out.println();
  }

  public static void main(String[] args) throws IOException {
    String frequencyFile = args[0];

    FrequencyData data = new FrequencyData(frequencyFile, true);
    writeSummary(data);
    
    System.out.print("Category" + "\t");
    System.out.print(0.0d + "\t");
    for (double item = 0.005; item < 1.0; item += item) {
      System.out.print(item + "\t");
    }
    System.out.print(1.0d + "\t");
    System.out.println("Total");

    data.showData2("Age", ICUPropertyFactory.make().getProperty("age"), new UnicodeSet("[[:cn:][:co:]]"), true);
    data.showData("Script/Cat", UCharacter.getPropertyEnum("script"), NO_SCRIPT);
    data.showData("Script/Cat", UCharacter.getPropertyEnum("gc"), new UnicodeSet(NO_SCRIPT).complement());


//    data.showData("Private Use", PRIVATE_USE);
//    RelativeFrequency relative = data.getRelativeFrequency(new UnicodeSet("[:script=unknown:]"), Normalizer.NFKC);
//    System.out.println(relative.getTotalRelative());
//    for (int i = 0; i < 10; ++i) {
//      int cp = relative.getCodePointAtRank(i);
//      double totalFrequency = relative.getCumulative(cp);
//      System.out.println(Integer.toHexString(cp) + "\t" + totalFrequency);
//    }
  }

  static class CountLang implements Comparable<CountLang> {
    long total;
    String code;

    public CountLang(String code, long total) {
      super();
      this.total = total;
      this.code = code;
    }

    public int compareTo(CountLang other) {
      if (total != other.total) {
        return total < other.total ? 1 : -1;
      }
      return code.compareTo(other.code);
    } 
  }
  
  private static void writeSummary(FrequencyData data) throws IOException {
    PrintWriter log = new PrintWriter(System.out);
    //mul 101 1380717913  0.000529173715836 U+0026  & Po  AMPERSAND

    
    Set<CountLang> ordered = new TreeSet<CountLang>();
    for (String lang : data.langData.keySet()) {
      ordered.add(new CountLang(lang, data.langData.get(lang).getTotal()));
    }
    
    for (CountLang countLang : ordered) {
      String lang = countLang.code;
      long total = countLang.total;
      Counter<String> normCounter = new Counter();
      Counter<Integer> langCounter = data.langData.get(lang);
      StringBuilder b = new StringBuilder();
      int count = 0;
      int rank = 1;
      long nfcNoCount = 0;
      long nfcMaybeCount = 0;
      long runningTotal = 0;
      double threshold = standardDeviation[4] * total;
      
      PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY, "/char_frequencies/" + lang + (MARKUP ? "_markup" : "") + ".txt");
      out.println("lang\trank\tcount\tlangPPB\tNFC\tcat\tscript\tcodepoint\tchar\tname");

      writeLine(out, lang, 0, total, total, 0, null);
      writeLine(log, lang, 0, total, total, 0, null);

      for (Integer code : langCounter.getKeysetSortedByCount(false)) {
        final long langCount = langCounter.getCount(code);
        writeLine(out, lang, code, langCount, total, rank, normCounter);
        runningTotal += langCount;
        if (runningTotal >= threshold) {
          break;
        }
//        if (true || NfcNo.contains(code)) {
//          if (count++ < 10) {
//            b.append("\t" + "U+" + Integer.toHexString(code) + "\t" + toChar(code)
//                  + "\t" + langCount + "\t" + rank);
//          }
//          nfcNoCount += langCount;
//        } else if (NfcMaybe.contains(code)) {
//          nfcMaybeCount += langCount;
//        }
        rank++;
        
      }
      out.close();
      for (String s : normCounter.getKeysetSortedByKey()) {
        final long count2 = normCounter.getCount(s);
        System.out.println("NFC:\t" + lang + "\t" + s + "\t" + count2 + "\t" + total + "\t" + (count/(double)total));
      }
      log.flush();
//      System.out.println(s + "\t" + new ULocale(s).getDisplayName() 
//              + "\t" + langCounter.getTotal() 
//              + "\t" + nfcNoCount 
//              + "\t" + nfcMaybeCount + "\t\t\t" + b);
//              + "\t" + data.langNfcNo.getCount(s)
//              + "\t" + data.langNfcMaybe.getCount(s)
//              + "\t" + data.langUpper.getCount(s)
//              + "\t" + data.langTotal.getCount(s));
    }

  }

  private static void writeLine(PrintWriter out, String lang, int code, long langCount, long total2, int rank, Counter<String> normCounter) {
    if (code == 0) {
      out.println(lang 
              + "\t" + 0
              + "\t" + langCount 
              + "\t" + 1000000000*langCount/total2 
              + "\t" + "Total");
    } else {
    final String normalizationType = getNormalizationType(code);
    if (normCounter != null) {
      normCounter.add(normalizationType, langCount);
    }
    out.println(lang 
            + "\t" + rank
            + "\t" + langCount 
            + "\t" + 1000000000*langCount/total2
            + "\t" + normalizationType 
            + "\t" + getValueAlias(code, UProperty.GENERAL_CATEGORY, UProperty.NameChoice.SHORT).charAt(0)
            + "\t" + getValueAlias(code, UProperty.SCRIPT, UProperty.NameChoice.SHORT)
            + "\t" + "U+" + com.ibm.icu.impl.Utility.hex(code, 4) 
            + "\t" + toChar(code) 
            + "\t" + UCharacter.getExtendedName(code));
    }

  }


  private static String getValueAlias(int code, int propEnum, int nameChoice) {
    if (propEnum == UProperty.SCRIPT && code < 0x80) {
      return "ASCII";
    }
    return UCharacter.getPropertyValueName(propEnum, UCharacter.getIntPropertyValue(code, propEnum), nameChoice);
  }


  private static String getNormalizationType(Integer code) {
    String nfd = UCharacter.getPropertyValueName(UProperty.NFD_QUICK_CHECK, UCharacter.getIntPropertyValue(code, UProperty.NFD_QUICK_CHECK), UProperty.NameChoice.SHORT);
    String nfc = UCharacter.getPropertyValueName(UProperty.NFC_QUICK_CHECK, UCharacter.getIntPropertyValue(code, UProperty.NFC_QUICK_CHECK), UProperty.NameChoice.SHORT);
    String nfkd = UCharacter.getPropertyValueName(UProperty.NFKD_QUICK_CHECK, UCharacter.getIntPropertyValue(code, UProperty.NFKD_QUICK_CHECK), UProperty.NameChoice.SHORT);
    String nfkc = UCharacter.getPropertyValueName(UProperty.NFKC_QUICK_CHECK, UCharacter.getIntPropertyValue(code, UProperty.NFKC_QUICK_CHECK), UProperty.NameChoice.SHORT);
    String result = nfc+nfd+nfkc+nfkd;
    result = result.replace("Y", "+").replace("N", "-").replace("M", "?");
    if (result.equals("++++")) result ="+";
    else if (result.equals("----")) result = "-";
    else if (result.substring(0,2).equals(result.substring(2,4))) result = result.substring(0,2);
    return "'" + result;
  }


  private static String toChar(int code) {
    if (code == '"' || code == '=') {
      return "'" + (char) code;
    }
    return new StringBuilder().appendCodePoint(code).toString();
  }


  public long getTotal() {
    // TODO Auto-generated method stub
    return frequencies.getTotal();
  }
}
