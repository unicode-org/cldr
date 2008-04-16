package org.unicode.cldr.unittest;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.DelegatingIterator;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UnicodeSet;

public class TestUtilities extends TestFmwk {
  public static void main(String[] args) {
    new TestUtilities().run(args);
  }
  
  public void TestDelegatingIterator() {
    Set<String> s = new TreeSet<String>(Arrays.asList(new String[]{"a", "b", "c"}));
    Set<String> t = new LinkedHashSet<String>(Arrays.asList(new String[]{"f", "d", "e"}));
    StringBuilder result = new StringBuilder();
    
    for (String u : DelegatingIterator.iterable(s,t)) {
      result.append(u);
    }
    assertEquals("Iterator fails", "abcfde", result.toString());

    result.setLength(0);
    for (String u : DelegatingIterator.array("s", "t", "u")) {
      result.append(u);
    }
    assertEquals("Iterator fails", "stu", result.toString());

    int count = 0;
    result.setLength(0);
    for (int u : DelegatingIterator.array(1, 3, 5)) {
      count += u;
    }
    assertEquals("Iterator fails", 9, count);

    result.setLength(0);
    for (Object u : DelegatingIterator.array(1, "t", "u", new UnicodeSet("[a-z]"))) {
      result.append(u);
    }
    assertEquals("Iterator fails", "1tu[a-z]", result.toString());
  }

}
