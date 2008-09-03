package org.unicode.cldr.draft;

import java.util.Random;

import com.ibm.icu.dev.test.TestFmwk;

public class GapStringTest extends TestFmwk {

  private static final int ITERATIONS = 100000;

  public static void main(String[] args) {
    new GapStringTest().run(args);
  }
  
  public static void TestCorrectness() {
    GapString a = new GapString();
    StringBuilder b = new StringBuilder();
    Random random = new Random(0);
    for (int iteration = ITERATIONS; iteration > 0; --iteration) {
      int randomPos = random.nextInt(b.length() + 1);
      switch(random.nextInt(3)) {
        case 0: // insert
          char randomChar = (char)('a' + random.nextInt(26));
          a.insert(randomPos, randomChar);
          b.insert(randomPos, randomChar);
          assertEqual(a,b);
          break;
        case 1: // insert string
          String randomString = getRandomString(random);
          a.insert(randomPos, randomString);
          b.insert(randomPos, randomString);
          assertEqual(a,b);
          break;
        case 2: // delete
          int randomEnd = randomPos + random.nextInt(b.length() - randomPos + 1);
          a.delete(randomPos, randomEnd);
          b.delete(randomPos, randomEnd);
          assertEqual(a,b);
          break;
      }
    }
  }

  private static String getRandomString(Random random) {
    StringBuilder result = new StringBuilder();
    for (int i = random.nextInt(5); i >= 0; --i) {
      result.append((char)('a' + random.nextInt(26)));
    }
    return result.toString();
  }

  private static void assertEqual(CharSequence a, CharSequence b) {
    if (!a.equals(b)) {
      a.equals(b);
      throw new IllegalArgumentException();
    }
    if (!b.toString().equals(a.toString())) {
      b.equals(a.toString());
      throw new IllegalArgumentException();  
    }
  }
}
