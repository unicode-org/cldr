package org.unicode.cldr.unittest;

import org.unicode.cldr.util.SimpleHtmlParser;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.SimpleHtmlParser.Type;

import com.ibm.icu.dev.test.util.BagFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class SimpleHtmlParserTest {
  public static void main(String[] args) throws IOException {
    PrintWriter writer = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY,
        "chart_messages2.html");
    try {
      BufferedReader reader = Utility.getUTF8Data("chart_messages.html");
      try {
        SimpleHtmlParser simple = new SimpleHtmlParser().setReader(reader);
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