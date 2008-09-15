package org.unicode.cldr.draft;

import java.util.Random;

import org.unicode.cldr.util.Timer;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;

public class GapStringTest extends TestFmwk {

  private static final int ITERATIONS = 100000;
  private static final int RANDOM_STRING_LENGTH = 9;

  public static void main(String[] args) {
    new GapStringTest().run(args);
  }

  public static void TestCorrectness() {
    GapString a = new GapString();
    StringBuilder b = new StringBuilder();

    int y = 1;

    for (int iteration = 0; iteration < ITERATIONS; ++iteration) {
      if (iteration == 24) {
        y = iteration; // for debugging
      }
      int randomPos = random.nextInt(b.length() + 1);
      String randomString;
      switch(random.nextInt(4)) {
        case 0: // delete
          int randomEnd = randomPos + random.nextInt(b.length() - randomPos + 1);
          b.delete(randomPos, randomEnd);
          a.delete(randomPos, randomEnd);
          assertEqual(a,b);
          break;
        case 1: // insert
          char randomChar = (char)('a' + random.nextInt(26));
          b.insert(randomPos, randomChar);
          a.insert(randomPos, randomChar);
          assertEqual(a,b);
          break;
        case 2: // insert string
          randomString = getRandomString(RANDOM_STRING_LENGTH);
          b.insert(randomPos, randomString);
          a.insert(randomPos, randomString);
          assertEqual(a,b);
          break;
        case 3: // insert char sequence (different code path)
          StringBuffer randomStringBuffer = new StringBuffer(getRandomString(RANDOM_STRING_LENGTH));
          b.insert(randomPos, randomStringBuffer);
          a.insert(randomPos, randomStringBuffer);
          assertEqual(a,b);
          break;
        case 4: // append string
          randomString = getRandomString(RANDOM_STRING_LENGTH);
          b.append(randomString);
          a.append(randomString);
          assertEqual(a,b);
          break;

      }
    }
  }

  static DecimalFormat percent = (DecimalFormat) NumberFormat.getPercentInstance();
  private static Random random = new Random(0);
  static {
    percent.setMaximumFractionDigits(6);
    percent.setPositivePrefix("+");
  }

  public static void TestTimeDeleteInsert() {
    checkTime(TimingStyle.fixed);
  }

  public static void TestTimeRandomDeleteInsert() {
    checkTime(TimingStyle.randomStart);
  }
  
  public static void TestTimeAppend() {
    checkTime(TimingStyle.append);
  }

  enum TimingStyle {fixed, randomStart, append}

  private static void checkTime(TimingStyle timingStyle) {
    GapString a = new GapString("abcdefghijklmonpqrstuvwxyz");
    StringBuilder b = new StringBuilder("abcdefghijklmonpqrstuvwxyz");
    double[] randomStarts = new double[256];
    double[] randomInserts = new double[256];
    if (timingStyle == TimingStyle.randomStart) {
      for (int i = 0; i < randomStarts.length; ++i) {
        randomStarts[i] = random.nextDouble();
        randomInserts[i] = random.nextDouble();
      }
    }
    Timer timer = new Timer();

    timer.start();
    for (int i = 0; i < ITERATIONS; ++i) {
      switch (timingStyle) {
        case append: 
          a.append("!@#$%X");
          break;
        case randomStart: case fixed:
          int deletePos = 5;
          int insertPos = 5;
          if (timingStyle == TimingStyle.randomStart) {
            final int length = a.length()-5;
            deletePos = (int) (length * randomStarts[i&0xFF]);
            insertPos = (int) (length * randomInserts[i&0xFF]);
          }
          a.delete(deletePos,deletePos+5);
          a.insert(insertPos,"!@#$%X");
      }
    }
    timer.stop();
    long gapDuration = timer.getDuration();

    timer.start();
    for (int i = 0; i < ITERATIONS; ++i) {
      switch (timingStyle) {
        case append: 
          b.append("!@#$%X");
          break;
        case randomStart: case fixed:      
          int deletePos = 5;
          int insertPos = 5;
          if (timingStyle == TimingStyle.randomStart) {
            final int length = b.length()-5;
            deletePos = (int) (length * randomStarts[i&0xFF]);
            insertPos = (int) (length * randomInserts[i&0xFF]);
          }
          b.delete(deletePos,deletePos+5);
          b.insert(insertPos,"!@#$%X");
      }
    }
    timer.stop();
    long builderDuration = timer.getDuration();
    assertEqual(a,b);
    System.out.println("\tGap - Builder% =\t" + percent.format(gapDuration*1.0/builderDuration - 1.0));
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
