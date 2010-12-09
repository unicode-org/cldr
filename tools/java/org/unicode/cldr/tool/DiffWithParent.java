package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;

public class DiffWithParent {
  private static Matcher fileMatcher;

  public static void main(String[] args) throws IOException {
    try {
      fileMatcher = Pattern.compile(CldrUtility.getProperty("FILE", ".*")).matcher(
          "");
      Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
      CLDRFile english = cldrFactory.make("en", true);
      TablePrinter table = new TablePrinter().addColumn("Path").setSpanRows(
          true).addColumn("Locale").addColumn("Value").addColumn("FullPath");
      PrettyPath pp = new PrettyPath();
      for (String locale : cldrFactory.getAvailable()) {
        if (fileMatcher.reset(locale).matches()) {
          System.out.println(locale + "\t" + english.getName(locale));
          CLDRFile file = cldrFactory.make(locale, false);
          String parentLocale = CLDRFile.getParent(locale);
          CLDRFile parent = cldrFactory.make(parentLocale, true); // use
                                                                  // resolved
                                                                  // parent
          for (Iterator<String> it = file.iterator(); it.hasNext();) {
            String path = it.next();
            String value = file.getStringValue(path);
            String fullPath = file.getFullXPath(path);
            String pvalue = parent.getStringValue(path);
            String pfullPath = parent.getFullXPath(path);
            if (!value.equals(pvalue) || !fullPath.equals(pfullPath)) {
              String pathName = pp.getPrettyPath(path);
              table.addRow().addCell(pathName).addCell(locale).addCell(value)
                  .addCell(showDistinguishingAttributes(fullPath)).finishRow();
              if (pvalue == null) {
                pvalue = "<i>none</i>";
              }
              if (pfullPath == null) {
                pfullPath = "<i>none</i>";
              } else {
                pfullPath = showDistinguishingAttributes(pfullPath);
              }
              table.addRow().addCell(pathName).addCell(parentLocale).addCell(
                  pvalue).addCell(pfullPath).finishRow();
            }
          }
          PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY,
              locale + "_diff.html");
          String title = locale + " " + english.getName(locale)
              + " Diff with Parent";
          out
              .println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'><title>"
                  + title + "</title></head><body>");
          out.println("<h1>" + title + "</h1>");
          out
              .println("<table  border='1' style='border-collapse: collapse' bordercolor='blue'>");
          out.println(table.toString());
          out.println("</table>");
          out.println(ShowData.ANALYTICS);
          out.println("</body></html>");
          out.close();
        }
      }
    } finally {
      System.out.println("DONE");
    }
  }

  static XPathParts fullParts = new XPathParts();

  static XPathParts parts = new XPathParts();

  private static String showDistinguishingAttributes(String fullPath) {
    fullParts.set(fullPath);
    String path = CLDRFile.getDistinguishingXPath(fullPath, null, false);
    parts.set(path);
    Set s = new TreeSet();
    for (int i = 0; i < fullParts.size(); ++i) {
      Map fullAttributes = fullParts.getAttributes(i);
      Map attributes = parts.getAttributes(i);
      for (String key : fullParts.getAttributeKeys(i)) {
        s.add(new Pair(key, fullParts.getAttributeValue(i, key)));
      }
      for (String key : parts.getAttributeKeys(i)) {
        s.remove(new Pair(key, parts.getAttributeValue(i, key)));
      }
    }
    return s.toString();
  }
}