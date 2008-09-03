package org.unicode.cldr.draft;

import java.util.Random;

import org.unicode.cldr.util.Timer;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.NumberFormat;

public class GapStringTest extends TestFmwk {

  private static final int ITERATIONS = 20000;

  public static void main(String[] args) {
    new GapStringTest().run(args);
  }
  
  public static void TestCorrectness() {
    GapString a = new GapString();
    StringBuilder b = new StringBuilder();

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
          String randomString = getRandomString(5);
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
  
  static NumberFormat percent = NumberFormat.getPercentInstance();
  private static Random random = new Random(0);
  static {
    percent.setMaximumFractionDigits(6);
  }
  
  public static void TestTimeDeleteInsert() {
    checkTime(false);
  }
  
  public static void TestTimeRandomDeleteInsert() {
    checkTime(true);
  }

  private static void checkTime(boolean randomStart) {
    GapString a = new GapString("abcdefghijklmonpqrstuvwxyz");
    StringBuilder b = new StringBuilder("abcdefghijklmonpqrstuvwxyz");
    double[] randomStarts = new double[256];
    double[] randomInserts = new double[256];
    if (randomStart) {
      for (int i = 0; i < randomStarts.length; ++i) {
        randomStarts[i] = random.nextDouble();
        randomInserts[i] = random.nextDouble();
      }
    }
    Timer timer = new Timer();
    
    timer.start();
    for (int i = 0; i < ITERATIONS; ++i) {
      int deletePos = 5;
      int insertPos = 5;
      if (randomStart) {
        final int length = a.length()-5;
        deletePos = (int) (length * randomStarts[i&0xFF]);
        insertPos = (int) (length * randomInserts[i&0xFF]);
      }
      a.delete(deletePos,deletePos+5);
      a.insert(insertPos,"!@#$%X");
    }
    timer.stop();
    long gapDuration = timer.getDuration();
    
    timer.start();
    for (int i = 0; i < ITERATIONS; ++i) {
      int deletePos = 5;
      int insertPos = 5;
      if (randomStart) {
        final int length = b.length()-5;
        deletePos = (int) (length * randomStarts[i&0xFF]);
        insertPos = (int) (length * randomInserts[i&0xFF]);
      }
      b.delete(deletePos,deletePos+5);
      b.insert(insertPos,"!@#$%X");
    }
    timer.stop();
    long builderDuration = timer.getDuration();
    assertEqual(a,b);
    System.out.println("\tGap/Builder% =\t" + percent.format(gapDuration*1.0/builderDuration));
  }

  private static String getRandomString(int maxLength) {
    StringBuilder result = new StringBuilder();
    for (int i = random.nextInt(maxLength); i >= 0; --i) {
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
