package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.XPathParts;

public class DiffWithParent {
    private static Matcher fileMatcher;

    public static void main(String[] args) throws IOException {
        try {
            fileMatcher = PatternCache.get(CldrUtility.getProperty("FILE", ".*")).matcher(
                "");
            Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
            CLDRFile english = cldrFactory.make("en", true);
            TablePrinter table = new TablePrinter().addColumn("Path").setSpanRows(
                true).addColumn("Locale").addColumn("Value").addColumn("FullPath");
            PrettyPath pp = new PrettyPath();
            for (String locale : cldrFactory.getAvailable()) {
                if (fileMatcher.reset(locale).matches()) {
                    System.out.println(locale + "\t" + english.getName(locale));
                    CLDRFile file = cldrFactory.make(locale, false);
                    String parentLocale = LocaleIDParser.getParent(locale);
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
                    PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, locale + "_diff.html");
                    String title = locale + " " + english.getName(locale)
                        + " Diff with Parent";
                    out
                        .println(
                            "<!doctype HTML PUBLIC '-//W3C//DTD HTML 4.0 Transitional//EN'><html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'><title>"
                                + title + "</title></head><body>");
                    out.println("<h1>" + title + "</h1>");
                    out
                        .println("<table  border='1' style='border-collapse: collapse' bordercolor='blue'>");
                    out.println(table.toString());
                    out.println("</table>");
                    out.println(CldrUtility.ANALYTICS);
                    out.println("</body></html>");
                    out.close();
                }
            }
        } finally {
            System.out.println("DONE");
        }
    }

    private static String showDistinguishingAttributes(String fullPath) {
        XPathParts fullParts = XPathParts.getTestInstance(fullPath);
        String path = CLDRFile.getDistinguishingXPath(fullPath, null);
        XPathParts parts = XPathParts.getTestInstance(path);
        Set<Pair<String, String>> s = new TreeSet<Pair<String, String>>();
        for (int i = 0; i < fullParts.size(); ++i) {
            for (String key : fullParts.getAttributeKeys(i)) {
                s.add(new Pair<String, String>(key, fullParts.getAttributeValue(i, key)));
            }
            for (String key : parts.getAttributeKeys(i)) {
                s.remove(new Pair<String, String>(key, parts.getAttributeValue(i, key)));
            }
        }
        return s.toString();
    }
}