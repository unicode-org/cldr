package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.unicode.cldr.draft.FileUtilities;

public class SpreadSheet {
    static boolean DEBUG = CldrUtility.getProperty("SpreadSheetDebug", false);

    public static List<List<String>> convert(String filename) throws IOException {
        return convert(FileUtilities.openUTF8Reader("", filename));
    }

    public static List<List<String>> convert(BufferedReader r) throws IOException {
        List<List<String>> result = new ArrayList<List<String>>();
        // boolean inQuote = false;
        while (true) {
            String line = r.readLine();
            if (line == null) break;
            if (DEBUG) {
                System.out.println("Spreadsheet:\t" + line);
            }
            String[] parts = line.split("\t");
            List<String> row = new ArrayList<String>(parts.length);
            for (String part : parts) {
                if (part.startsWith("\"") && part.endsWith("\"")) {
                    row.add(part.substring(1, part.length() - 1));
                } else {
                    row.add(part);
                }
            }
            result.add(row);
        }
        return result;
    }

    // for (int i = 0; i < line.length(); ++i) {
    // char ch = line.charAt(i); // don't worry about supplementaries
    // if (inQuote) {
    // if (ch == '"') {
    // inQuote = false;
    // }
    // } else {
    // if (ch == ',' || ch == "\t") {
    //
    // }
    // }
    //
    // }

}