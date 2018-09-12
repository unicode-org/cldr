package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.SimpleHtmlParser.Type;

public class SimpleHtmlParserTest {
    public static void main(String[] args) throws IOException {
        PrintWriter writer = FileUtilities.openUTF8Writer(
            CLDRPaths.GEN_DIRECTORY, "chart_messages2.html");
        try {
            BufferedReader reader = CldrUtility
                .getUTF8Data("chart_messages.html");
            try {
                SimpleHtmlParser simple = new SimpleHtmlParser()
                    .setReader(reader);
                StringBuilder result = new StringBuilder();
                Type x;
                do {
                    x = simple.next(result);
                    System.out.println(x + "\t\t{" + result + "}");
                    SimpleHtmlParser.writeResult(x, result, writer);
                } while (x != Type.DONE);
            } finally {
                reader.close();
            }
        } finally {
            writer.close();
        }
    }
}