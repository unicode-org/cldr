package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.EscapingUtilities;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class GenerateComparison {

  private static PrettyPath prettyPathMaker;

  private static Collator collator = Collator.getInstance();

  static class EnglishRowComparator implements Comparator<R2<String,String>> {
    private static Comparator<String> unicode = new UTF16.StringComparator(true, false, 0);
    public int compare(R2<String, String> arg0, R2<String, String> arg1) {
      int result = collator.compare(arg0.get0(), arg1.get0());
      if (result != 0) return result;
      result = unicode.compare(arg0.get0(), arg1.get0());
      if (result != 0) return result;
      result = collator.compare(arg0.get1(), arg1.get1());
      if (result != 0) return result;
      result = unicode.compare(arg0.get1(), arg1.get1());
      return result;
    }
  }

  static EnglishRowComparator ENG = new EnglishRowComparator();
  
  static final String warningMessage = "<p><b>Warning: this chart is still under development. For how to use it, see <a href=\"http://unicode.org/cldr/data/docs/survey/vetting.html\">Help: How to Vet</a>.</b></p>";

  public static void main(String[] args) throws IOException {

    // Setup
    Timer timer = new Timer();
    Timer totalTimer = new Timer();
    long totalPaths = 0;
    format = NumberFormat.getNumberInstance();
    format.setGroupingUsed(true);

    Counter<String> totalCounter = new Counter<String>();

    // Get the args

    String oldDirectory = CldrUtility.getProperty("oldDirectory", new File(CldrUtility.BASE_DIRECTORY, "common/main").getCanonicalPath() + "/");
    String newDirectory = CldrUtility.getProperty("newDirectory", new File(CldrUtility.BASE_DIRECTORY, "../cldr-release-1-7/common/main").getCanonicalPath() + "/");
    String changesDirectory = CldrUtility.getProperty("changesDirectory", new File(CldrUtility.CHART_DIRECTORY + "/changes/").getCanonicalPath() + "/");

    String filter = CldrUtility.getProperty("localeFilter", ".*");
    boolean SHOW_ALIASED = CldrUtility.getProperty("showAliased", "false").toLowerCase().startsWith("t");

    // Create the factories

    Factory oldFactory = Factory.make(oldDirectory, filter);
    Factory newFactory = Factory.make(newDirectory, filter);
    CLDRFile english = newFactory.make("en", true);
    CLDRFile oldRoot = oldFactory.make("root", true);
    CLDRFile newRoot = newFactory.make("root", true);

    // Get the union of all the language locales, sorted by English name

    Set<String> oldList = oldFactory.getAvailableLanguages();
    Set<String> newList = newFactory.getAvailableLanguages();
    Set<String> unifiedList = new HashSet<String>(oldList);
    unifiedList.addAll(newList);
    Set<R2<String, String>> pairs = new TreeSet<R2<String, String>>();
    for (String code : unifiedList) {
      pairs.add(Row.of(english.getName(code), code));
    }

    prettyPathMaker = new PrettyPath();
    int totalDifferences = 0;
    int differences = 0;

    Set<R2<String,String>> indexInfo = new TreeSet<R2<String,String>>(ENG);

    // iterate through those
    for (R2<String, String> pair : pairs) {
      timer.start();
      final String locale = pair.get1();
      final String localeName = pair.get0();
      System.out.println(locale);
      differences = 0;
      System.out.println();

      // Create CLDR files for both; null if can't open

      CLDRFile oldFile = null;
      if (oldList.contains(locale)) {
        try {
          oldFile = oldFactory.make(locale, true, true);
        } catch (Exception e) {
          addToIndex(indexInfo, "ERROR1.6 ", locale, localeName);
          continue;
        }
      } else {
        oldFile = CLDRFile.make(locale); // make empty file
      }
      CLDRFile newFile = null;
      if (newList.contains(locale)) {
        try {
          newFile = newFactory.make(locale, true, true);
        } catch (Exception e) {
          addToIndex(indexInfo, "ERROR1.7 ", locale, localeName);
          continue;
        }
      } else {
        newFile = CLDRFile.make(locale); // make empty file
      }

//      for(String str : newFile) {
//        String xo = newFile.getFullXPath(str);
//        String v = newFile.getStringValue(str);
//
//        System.out.println(xo+"\t"+v+"\n");
//
//      }
      // Check for null cases

      if (oldFile == null) {
        addToIndex(indexInfo, "NEW ", locale, localeName);
        continue;
      } else if (newFile == null) {
        addToIndex(indexInfo, "DELETED ", locale, localeName);
        continue;
      }
      System.out.println("*** " + localeName + "\t" + locale);
      System.out.println();
      
      // exclude aliased locales
      if (newFile.isAliasedAtTopLevel()) {
        continue;
      }

      // Get the union of all the paths

      Set<String> paths;
      try {
        paths = new HashSet<String>();
        CollectionUtilities.addAll(oldFile.iterator(), paths);
        if (oldList.contains(locale)) {
          paths.addAll(oldFile.getExtraPaths());
        }
        CollectionUtilities.addAll(newFile.iterator(), paths);
        if (newList.contains(locale)) {
          paths.addAll(newFile.getExtraPaths());
        }
      } catch (Exception e) {
	System.err.println("Locale: " + locale + ", "+localeName);
	e.printStackTrace();
        addToIndex(indexInfo, "ERROR ", locale, localeName);
        continue;
      }

      // We now have the full set of all the paths for old and new files
      // TODO Sort by the pretty form
      // Set<R2<String,String>> pathPairs = new TreeSet();
      // for (String code : unifiedList) {
      // pairs.add(Row.make(code, english.getName(code)));
      // }

      // Initialize sets
      //       .addColumn("Code", "class='source'", "<a name=\"{0}\" href='likely_subtags.html#und_{0}'>{0}</a>", "class='source'", true)

      final String localeDisplayName = english.getName(locale);
      TablePrinter table = new TablePrinter()
      .setCaption("Changes in " + localeDisplayName + " (" + locale + ")")
      .addColumn("PRETTY_SORT1").setSortPriority(1).setHidden(true).setRepeatHeader(true)
      .addColumn("PRETTY_SORT2").setSortPriority(2).setHidden(true)
      .addColumn("PRETTY_SORT3").setSortPriority(3).setHidden(true)
      .addColumn("ESCAPED_PATH").setHidden(true)
      .addColumn("Inh.").setCellAttributes("class=\"{0}\"").setSortPriority(0).setSpanRows(true).setRepeatHeader(true)
      .addColumn("Section").setSpanRows(true).setCellAttributes("class='section'")
      .addColumn("Subsection").setSpanRows(true).setCellAttributes("class='subsection'")
      .addColumn("Item").setSpanRows(true).setCellPattern("<a href=\"{4}\">{0}</a>").setCellAttributes("class='item'")
      .addColumn("English").setCellAttributes("class='english'")
      .addColumn("Status").setSortPriority(4).setCellAttributes("class=\"{0}\"")
      .addColumn("Old" + localeDisplayName).setCellAttributes("class='old'")
      .addColumn("New" + localeDisplayName).setCellAttributes("class='new'")
      ;
      Counter<String> fileCounter = new Counter<String>();

      for (String path : paths) {
        if (path.contains("/alias") || path.contains("/identity")) {
          continue;
        }
        String cleanedPath = CLDRFile.getNondraftNonaltXPath(path);

        String oldValue = oldFile.getStringValue(cleanedPath);
        String newValue = newFile.getStringValue(path);
        String englishValue = english.getStringValue(cleanedPath);

        // for debugging
        if (oldValue != null && oldValue.contains("{1} {0}")) {
          System.out.print("");
        }

        if (equals(newValue, oldValue)) {
          continue;
        }

        // get the actual place the data is stored
        // AND adjust if the same as root!

        Status oldStatus = new Status();
        String oldFoundLocale = getStatus(oldFile, oldRoot, cleanedPath, oldValue, oldStatus);

        Status newStatus = new Status();
        String newFoundLocale = getStatus(newFile, newRoot, path, newValue, newStatus);

        // At this point, we have two unequal values
        // TODO check for non-distinguishing attribute value differences

        boolean isAliased = false;


        // Skip deletions of alt-proposed

        //          if (newValue == null) {
        //            if (path.contains("@alt=\"proposed")) {
        //              continue;
        //            }
        //          }

        // Skip if both inherited from the same locale, since we should catch it
        // in that locale.

        // Mark as aliased if new locale or path is different
        if (!newStatus.pathWhereFound.equals(path)) {
          isAliased=true;
          //continue;
        }         

        if (!newFoundLocale.equals(locale)) {
          isAliased=true;
          //continue;
        }

        //          // skip if old locale or path is aliased
        //          if (!oldFoundLocale.equals(locale)) {
        //            //isAliased=true;
        //            continue;
        //          }
        //          
        //          // Skip if either found path is are different
        //          if (!oldStatus.pathWhereFound.equals(cleanedPath)) {
        //            //isAliased=true;
        //            continue;
        //          }

        // Now check other aliases

        //          final boolean newIsAlias = !newStatus.pathWhereFound.equals(path);
        //          if (newIsAlias) { // new is alias
        //            // filter out cases of a new string that is found via alias
        //            if (oldValue == null) {
        //              continue;
        //            }
        //
        //          }

        if (isAliased && !SHOW_ALIASED) {
          continue;
        }

        // We definitely have a difference worth recording, so do so

        String newFullPath = newFile.getFullXPath(path);
        final boolean reject = newFullPath != null && newFullPath.contains("@draft") && !newFullPath.contains("@draft=\"contributed\"");
        String status;
        if (reject) {
          status = "NOT-ACC";
        } else if (newValue == null) {
          status = "deleted";
        } else if (oldValue == null) {
          status = "added";
        } else {
          status = "changed";
        }
        String coreStatus = status;
        if (isAliased) {
          status = "I+" + status;
        }
        fileCounter.increment(status);
        totalCounter.increment(status);

        String pretty_sort = prettyPathMaker.getPrettyPath(cleanedPath);
        String[] prettyPartsSort = pretty_sort.split("[|]");
        if (prettyPartsSort.length != 3) {
          System.out.println("Bad pretty path: " + pretty_sort + ", original: " + cleanedPath);
        }
        String prettySort1 = prettyPartsSort[0];
        String prettySort2 = prettyPartsSort[1];
        String prettySort3 = prettyPartsSort[2];
        
        String pretty = prettyPathMaker.getOutputForm(pretty_sort);
        String escapedPath = "http://unicode.org/cldr/apps/survey?_=" + locale + "&xpath=" + EscapingUtilities.urlEscape(cleanedPath);
        String[] prettyParts = pretty.split("[|]");
        if (prettyParts.length != 3) {
          System.out.println("Bad pretty path: " + pretty + ", original: " + cleanedPath);
        }
        String pretty1 = prettyParts[0];
        String pretty2 = prettyParts[1];
        String pretty3 = prettyParts[2];
          
          // http://kwanyin.unicode.org/cldr-apps/survey?_=kw_GB&xpath=%2F%2Fldml%2FlocaleDisplayNames%2Flanguages%2Flanguage%5B%40type%3D%22mt%22%5D

        table.addRow()
        .addCell(prettySort1)
        .addCell(prettySort2)
        .addCell(prettySort3)
        .addCell(escapedPath)
        .addCell(isAliased ? "I" : "")
        .addCell(pretty1)
        .addCell(pretty2)
        .addCell(pretty3)
        .addCell(englishValue == null ? "-" : englishValue)
        .addCell(coreStatus)
        .addCell(oldValue == null ? "-" : oldValue)
        .addCell(newValue == null ? "-" : newValue)
        .finishRow();

        totalDifferences++;
        differences++;
      }

      addToIndex(indexInfo, "", locale, localeName, fileCounter);
      PrintWriter out = BagFormatter.openUTF8Writer(changesDirectory, locale + ".html");
      String title = "Changes in " + localeDisplayName ;
      out.println("<html>" +
              "<head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>" + CldrUtility.LINE_SEPARATOR +
              "<title>" + title + "</title>" + CldrUtility.LINE_SEPARATOR +
              "<link rel='stylesheet' href='index.css' type='text/css'>" + CldrUtility.LINE_SEPARATOR +
              "<base target='_blank'>" + CldrUtility.LINE_SEPARATOR +
              "</head><body>" + CldrUtility.LINE_SEPARATOR +
              "<h1>" + title + "</h1>" + CldrUtility.LINE_SEPARATOR
              + "<a href='index.html'>Index</a> | <a href=\"http://unicode.org/cldr/data/docs/survey/vetting.html\"><b style=\"background-color: yellow;\"><i>Help: How to Vet</i></b></a>"
              + warningMessage
              );

      TablePrinter table2 = new TablePrinter()
      .setCaption("Totals")
      .addColumn("Inh.").setSortPriority(0)
      .addColumn("Status").setSortPriority(1)
      .addColumn("Total")
      ;

      for (String key : fileCounter.getKeysetSortedByKey()) {
        boolean inherited = key.startsWith("I+");
        table2.addRow()
        .addCell(inherited ? "I" : "")
        .addCell(inherited ? key.substring(2) : key)
        .addCell(format.format(fileCounter.getCount(key)))
        .finishRow();
      }
      out.println(table2);
      out.println("<br>");
      out.println(table);

      // show status on console

      System.out.println(locale + "\tDifferences:\t" + format.format(differences)
              + "\tPaths:\t" + format.format(paths.size())
              + "\tTime:\t" + timer);

      totalPaths += paths.size();
      out.println(ShowData.dateFooter());
      out.println(ShowData.ANALYTICS);
      out.println("</body></html>");
      out.close();
    }
    PrintWriter indexFile = BagFormatter.openUTF8Writer(changesDirectory, "index.html");
    indexFile.println("<html>" +
            "<head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>" + CldrUtility.LINE_SEPARATOR +
            "<title>" + "Change Summary" + "</title>" + CldrUtility.LINE_SEPARATOR +
            "<link rel='stylesheet' href='index.css' type='text/css'>" + CldrUtility.LINE_SEPARATOR +
            "<base target='_blank'>" + CldrUtility.LINE_SEPARATOR +
            "</head><body>" + CldrUtility.LINE_SEPARATOR +
            "<h1>" + "Change Summary" + "</h1>" + CldrUtility.LINE_SEPARATOR
            + "<a href=\"http://unicode.org/cldr/data/docs/survey/vetting.html\"><b style=\"background-color: yellow;\"><i>Help: How to Vet</i></b></a>"
            + warningMessage
            + "<table><tr>");

    String separator = "";
    int last = 0;
    for (R2<String,String> indexPair : indexInfo) {
      int firstChar = indexPair.get0().codePointAt(0);
      indexFile.append(firstChar == last ? separator
              : (last == 0 ? "" : "</td></tr>\r\n<tr>") + "<th>" + String.valueOf((char)firstChar) + "</th><td>")
                      .append(indexPair.get1());
      separator = " | ";
      last = indexPair.get0().codePointAt(0);
    }
    indexFile.println("</tr></table>");
    indexFile.println(ShowData.dateFooter());
    indexFile.println(ShowData.ANALYTICS);
    indexFile.println("</body></html>");
    indexFile.close();

    System.out.println();


    for (String key : totalCounter.getKeysetSortedByKey()) {
      System.out.println(key + "\t" + totalCounter.getCount(key));
    }

    System.out.println("Total Differences:\t" + format.format(totalDifferences) 
            + "\tPaths:\t" + format.format(totalPaths)
            + "\tTotal Time:\t" + format.format(totalTimer.getDuration()) + "ms");
  }
  
//  static Transliterator urlHex = Transliterator.createFromRules("foo", 
//          "([^!(-*,-\\:A-Z_a-z~]) > &hex($1) ;" +
//          ":: null;" +
//          "'\\u00' > '%' ;"
//          , Transliterator.FORWARD);

  private static NumberFormat format;

  private static void addToIndex(Set<R2<String,String>> indexInfo, String title, final String locale,
          final String localeName) {
    addToIndex(indexInfo, title, locale, localeName, null);
  }

  private static void addToIndex(Set<R2<String,String>> indexInfo, String title, final String locale,
          final String localeName, Counter<String> fileCounter) {
    if (title.startsWith("ERROR")) {
      indexInfo.add(R2.of(localeName, 
              title + " " + localeName + " (" + locale + ")"));
      return;
    }
    String counterString = "";
    if (fileCounter != null) {
      for (String s : fileCounter) {
        if (counterString.length() != 0) {
          counterString += "; ";
        }
        counterString += s.charAt(0) + ":" + format.format(fileCounter.getCount(s));
      }
    }
    indexInfo.add(R2.of(localeName, 
            "<a href='" + locale + ".html'>" + title + localeName + " (" + locale + ")</a>" 
            + (counterString.length() == 0 ? "" : " [" + counterString + "]")));
  }

  //  private static int accumulate(Set<R2<String,String>> rejected, int totalRejected,
  //          final String locale, String indicator, String oldValue, String newValue, String path) {
  //    String pretty = prettyPathMaker.getPrettyPath(path, false);
  //    String line = locale + "\t" + indicator +"\t\u200E[" + oldValue + "]\u200E\t\u200E[" + newValue + "]\u200E\t" + pretty;
  //    String pretty2 = prettyPathMaker.getOutputForm(pretty);
  //    rejected.add(Row.make(pretty2, line));
  //    totalRejected++;
  //    return totalRejected;
  //  }

  private static String getStatus(CLDRFile oldFile, CLDRFile oldRoot, String path,
          String oldString, Status oldStatus) {
    String oldLocale = oldFile.getSourceLocaleID(path, oldStatus);
    if (!oldLocale.equals("root")) {
      String oldRootValue = oldRoot.getStringValue(oldStatus.pathWhereFound);
      if (equals(oldString, oldRootValue)) {
        oldLocale = "root";
      }
    }
    return oldLocale;
  }

  private static void showSet(PrintWriter out, Set<R2<String,String>> rejected, final String locale, String title) {
    if (rejected.size() != 0) {
      out.println();
      out.println(locale + "\t" + title + "\t" + rejected.size());
      for (R2<String,String> prettyAndline : rejected) {
        out.println(prettyAndline.get1());
      }
    }
  }

  private static boolean equals(String newString, String oldString) {
    if (newString == null) {
      return oldString == null;
    }
    return newString.equals(oldString);
  }
  
}
