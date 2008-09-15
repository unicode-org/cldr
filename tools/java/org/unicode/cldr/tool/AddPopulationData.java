package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import org.unicode.cldr.util.Utility;

public class AddPopulationData {
  public static void main(String[] args) throws IOException {
    BufferedReader in = Utility.getUTF8Data("external/world_bank_data.txt");
    while (true) {
      String line = in.readLine();
      if (line == null)
        break;
      String[] pieces = line.split("\t");
      System.out.println(Arrays.asList(pieces));
    }
  }
}
