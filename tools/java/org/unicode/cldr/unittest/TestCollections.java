package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

import org.unicode.cldr.util.DumbTreeMap;
import org.unicode.cldr.util.Timer;

import com.ibm.icu.dev.test.TestFmwk;

public class TestCollections extends TestFmwk {
  
  public static void main(String[] args) {
    new TestCollections().run(args);
  }
  
  public void TestDumbMapTiming() {
    int iterations = 10000000;
    Timer t1 = new Timer();
    Timer t2 = new Timer();
    
    DumbTreeMap<String,String> dumb = new DumbTreeMap<String,String>();
    for (int j = 0; j < testItems.length; ++j) {
      dumb.put(testItems[j][0],testItems[j][1]);
    }

    t1.start();
    for (int i = 0; i < iterations; ++i) {
      dumb = new DumbTreeMap<String,String>();
      for (int j = 0; j < testItems.length; ++j) {
        dumb.put(testItems[j][0],testItems[j][1]);
      }
    }
    t1.stop();
    logln("Dumb timing: " + t1);
    
    
    TreeMap<String,String> real = new TreeMap<String,String>();
    for (int j = 0; j < testItems.length; ++j) {
      real.put(testItems[j][0],testItems[j][1]);
    }
    t2.start();
    for (int i = 0; i < iterations; ++i) {
      real = new TreeMap<String,String>();
      for (int j = 0; j < testItems.length; ++j) {
        real.put(testItems[j][0],testItems[j][1]);
      }
    }
    t2.stop();
    logln("Real timing: " + t2);

  }
  
  final String[][] testItems = {
          {"delta", "5"},
          {"beta", "2"},
          {"gamma", "1"},
          {"epsilon", "99"},
          {"alpha", "1"},
        };
  
  public void TestDumbMap() {

    int testNumber = 0;
    
    // randomly do stuff, and check if same
    DumbTreeMap<String,String> dumb = new DumbTreeMap<String,String>();
    TreeMap<String,String> real = new TreeMap<String,String>();
    Random random = new Random(0);
    for (int i = 0; i < 100; ++i) {
      testNumber = random.nextInt(testItems.length);
      Object oldDumb = null;
      Object oldReal = null;
      boolean checkSets = false;
      switch(random.nextInt(6)) {
        case 0: case 1: case 2:
          logln(i + "\tPut: " + testItems[testNumber][0] + "=" + testItems[testNumber][1]);
          oldDumb = dumb.put(testItems[testNumber][0],testItems[testNumber][1]);
          oldReal = real.put(testItems[testNumber][0],testItems[testNumber][1]);
          checkSets = true;
          break;
        case 3: case 4: case 5:
          logln(i + "\tRemove: " + testItems[testNumber][0]);
          oldDumb = dumb.remove(testItems[testNumber][0]);
          oldReal = real.remove(testItems[testNumber][0]);
          break;
        case 6:
          logln(i + "\tClear");
          dumb.clear();
          real.clear();
          break;
      }
      if (oldDumb != oldReal) {
        errln(i + "\tValue Failure " + ":\t" + oldDumb + ", " + oldReal);        
      }
      
      checkEquals(i, dumb, real);
    }
  }

  enum FailureType {ok, equals, keyset, values}
  
  private FailureType checkEquals(int i, DumbTreeMap<String, String> dumb, TreeMap<String, String> real) {
    FailureType result = FailureType.ok;
    if (!dumb.equals(real)) {
      result = FailureType.equals;
      errln(i + "\tFailure " + result + ":\t" + dumb + ", " + real);
    }
    if (!new ArrayList(dumb.keySet()).equals(new ArrayList(real.keySet()))) {
      result = FailureType.keyset;
      errln(i + "\tFailure " + result + ":\t" + dumb.keySet() + ", " + real.keySet());
    }
    if (!dumb.values().equals(new ArrayList(real.values()))) {
      result = FailureType.values;
      errln(i + "\tFailure " + result + ":\t" + dumb.values() + ", " + real.values());
    }
    return result;
  }
}
