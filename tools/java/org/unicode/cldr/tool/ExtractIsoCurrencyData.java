package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.SimpleHtmlParser.Type;

import com.ibm.icu.dev.test.util.BagFormatter;

/**
 * Run this code to extract the ISO currency data from a file.
 * Use -Dinput=xxx for the input file, and -Doutput=xxx for the output file
 * @author markdavis
 */
public class ExtractIsoCurrencyData {
  private static final boolean VERBOSE  = true;

  private static final Matcher HAS_DATE = Pattern.compile(
                                                "last modified.*([0-9]{4}-[0-9]{2}-[0-9]{2})",
                                                Pattern.DOTALL).matcher("");

  public static void main(String[] args) throws IOException {
    final String inputFile = CldrUtility.getProperty("input", CldrUtility.UTIL_DATA_DIR + "/currency_codes_list-1.htm");
    BufferedReader in = BagFormatter.openUTF8Reader("", inputFile);
    final String outputFile = CldrUtility.getProperty("output", CldrUtility.UTIL_DATA_DIR + "/currencycodeslist.txt");
    PrintWriter out = BagFormatter.openUTF8Writer("", outputFile);
    try {
      String version = null;
      String[][] parts = new String[5][5];
      int count = 0;

      boolean inContent = false;
      // if the table level is 1 (we are in the main table), then we look for <td>...</td><td>...</td>. That means that we have column 1 and column 2.

      SimpleHtmlParser simple = new SimpleHtmlParser().setReader(in);
      StringBuilder result = new StringBuilder();
      boolean hadPop = false;
      int column = -1;
      int row = -1;
      main: while (true) {
        Type x = simple.next(result);
        //System.out.println(x + "\t" + result);
        switch (x) {
          case ELEMENT: // with /table we pop the count
            if (SimpleHtmlParser.equals("tr", result)) {
              if (hadPop) {
                for (int i = 0; i < parts.length; ++i) {
                  boolean empty = true;
                  for (int j = 0; j < parts[i].length; ++j) {
                    parts[i][j] = parts[i][j].replace("&nbsp;", " ");
                    parts[i][j] = parts[i][j].replace("\u2020", " ");
                    parts[i][j] = parts[i][j].replace("\u2021", " ");
                    parts[i][j] = parts[i][j].replace("\u00A0", " ");
                    parts[i][j] = parts[i][j].trim();
                    empty &= parts[i][j].length() == 0;
                  }
                  if (empty) {
                    continue;
                  }
                  if (parts[i][0].length() == 0) {
                    parts[i][0] = i == 0 ? "ZZ" : parts[0][0]; // hack because of iso format
                  } else if (parts[i][0].equals("Entity")) {
                    continue;
                  }
                  if (parts[i][1].equals("Special settlement currencies")) {
                    continue;
                  } else if (parts[i][1].equals("No universal currency")) {
                    parts[i][2] = "XXX";
                    parts[i][3] = "999";
                  }
                  // fix numbers to match old style
                  if (VERBOSE)
                    System.out.println("\tDATA: " + Arrays.asList(parts[i]));
                  int num = parts[i][3].equals("Nil") ? -1 : Integer.parseInt(parts[i][3]);
                  parts[i][3] = String.valueOf(num);
                  out.println(CldrUtility.join(parts[i], "\t").trim());
                  count++;
                  //Data data = new Data(country, parts[i][1], parts[i][3]);
                  //codeList.put(parts[i][2], data);
                }
                column = -1;
                row = -1;
              } else {
                column = 0;
                row = 0;
                for (int i = 0; i < parts.length; ++i) {
                  for (int j = 0; j < parts[i].length; ++j) {
                    parts[i][j] = "";
                  }
                }
              }
            } else if (SimpleHtmlParser.equals("td", result)
                    || SimpleHtmlParser.equals("th", result)) {
              if (hadPop) {
                column++;
                row = 0;
              }
            } else if (SimpleHtmlParser.equals("br", result)) { // because ISO has screwy format
              row++;
            }
            break;
          case ELEMENT_CONTENT:
            if (column >= 0) {
              parts[row][column] += result;
            }
            break;
          case QUOTE:
            if (HAS_DATE.reset(result).find()) {
              version = HAS_DATE.group(1);
            }
            break;
          case ELEMENT_POP:
            hadPop = true;
            break;
          case ELEMENT_START:
            hadPop = false;
            break;
          case DONE:
            break main;
          case ELEMENT_END:
          case ATTRIBUTE:
          case ATTRIBUTE_CONTENT:
            break; // for debugging
        }
      }
      in.close();
      if (version == null) {
        throw new IllegalArgumentException("Missing version; ISO file format probably changed.");
      }
      if (count < 50) {
        throw new IllegalArgumentException("Data too small; ISO file format probably changed.");
      }
      out.println("Last modified " + version);
    } catch (IOException e) {
      throw new IllegalArgumentException("Can't read currency file " + e.getMessage());
    }
    out.close();
  }

  /**
   * Was code to check when we moved from flat file to html to alert on differences. Not necessary any more.
   * @throws IOException
   */
  //  public void CheckISOCurrencyParser() throws IOException {
  //    Relation<String, Data> codeList = new Relation(new TreeMap(), TreeSet.class, null);
  //    Relation<String, Data> codeListHtml = new Relation(new TreeMap(), TreeSet.class, null);
  //
  //    String version = IsoCurrencyParser.getFlatList(codeList);
  //    String versionHtml = IsoCurrencyParser.getHtmlList(codeListHtml); // getFlatList
  //    assertEquals("Versions don't match", version, versionHtml);
  //    Set<String> keys = new TreeSet(codeList.keySet());
  //    keys.addAll(codeListHtml.keySet());
  //    for (String key : keys) {
  //      Set<Data> flat = codeList.getAll(key);
  //      Set<Data> html = codeListHtml.getAll(key);
  //      if (flat == null || !flat.equals(html)) {
  //        if (flat != null) {
  //          Set inFlatOnly = new TreeSet(flat);
  //          if (html != null) inFlatOnly.removeAll(html);
  //          if (inFlatOnly.size() != 0) errln(key + "\t\tflat: " + inFlatOnly);
  //        }
  //        if (html != null) {
  //          Set inHtmlOnly = new TreeSet(html);
  //          if (flat != null) inHtmlOnly.removeAll(flat);
  //          if (inHtmlOnly.size() != 0) errln("\t" + key + "\thtml: " + inHtmlOnly);
  //        }
  //      }
  //    }
  //    System.out.println(codeList);
  //  }
}
