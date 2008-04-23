package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DelegatingIterator;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.Level;
import org.unicode.cldr.util.VoteResolver.Organization;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.VoteResolver.VoterInfo;

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
    assertEquals("Iterator", "abcfde", result.toString());

    result.setLength(0);
    for (String u : DelegatingIterator.array("s", "t", "u")) {
      result.append(u);
    }
    assertEquals("Iterator", "stu", result.toString());

    int count = 0;
    result.setLength(0);
    for (int u : DelegatingIterator.array(1, 3, 5)) {
      count += u;
    }
    assertEquals("Iterator", 9, count);

    result.setLength(0);
    for (Object u : DelegatingIterator.array(1, "t", "u", new UnicodeSet("[a-z]"))) {
      result.append(u);
    }
    assertEquals("Iterator", "1tu[a-z]", result.toString());
  }

  public void TestCounter() {
    Counter<String> counter = new Counter<String>();
    counter.add("c", 95);
    counter.add("b", 50);
    counter.add("b", 101);
    counter.add("a", 100);
    counter.add("a", -5);
    counter.add("d", -3);
    assertEquals("getCount(b)", counter.getCount("b"), 151);
    assertEquals("getCount(a)", counter.getCount("a"), 95);
    assertEquals("getCount(a)", counter.getTotal(), 338);
    assertEquals("getItemCount", counter.getItemCount(), 4);

    assertEquals("getMap", 
            counter.getMap().toString(), "{c=95, b=151, a=95, d=-3}");

    assertEquals("getKeysetSortedByKey", 
            Arrays.asList("a", "b", "c", "d"),
            new ArrayList(counter.getKeysetSortedByKey()));

    assertEquals("getKeysetSortedByCount", 
            Arrays.asList("d", "c", "a", "b"),
            new ArrayList(counter.getKeysetSortedByCount(true, false)));

    assertEquals("getKeysetSortedByCount, value", 
            Arrays.asList("d", "a", "c", "b"), new ArrayList(counter.getKeysetSortedByCount(true, true)));

    assertEquals("getKeysetSortedByCount, descending", 
            Arrays.asList("b", "c", "a", "d"), new ArrayList(counter.getKeysetSortedByCount(false, false)));

    assertEquals("getKeysetSortedByCount, descending, value", 
            Arrays.asList("b", "a", "c", "d"), new ArrayList(counter.getKeysetSortedByCount(false, true)));
  }

  public void TestVoteResolverData() {
    VoteResolver.setTestData("/Users/markdavis/Downloads/users.xml");
  }
  
  public void TestVoteResolver() {
    VoteResolver.setTestData(Utility.asMap(new Object[][] {
            {666, new VoterInfo(Organization.google, Level.vetter, "J. Smith")},
            {555, new VoterInfo(Organization.google, Level.street, "S. Jones")},
            {444, new VoterInfo(Organization.google, Level.vetter, "S. Samuels")},
            {333, new VoterInfo(Organization.apple, Level.vetter, "A. Mutton")},
            {222, new VoterInfo(Organization.adobe, Level.expert, "A. Aldus")},
            {111, new VoterInfo(Organization.ibm, Level.street, "J. Henry")},
    }));
    VoteResolver resolver = new VoteResolver();
    String[] tests = {
            // check that identical values get the alphabetically lowest
            "oldValue=last", 
            "oldStatus=provisional",
            "555=next",
            "666=best",
            "value=best",
            "conflicts=[]", 
            "status=provisional",
            "check",
            // now give next a slight edge
            "444=next",
            "value=next",
            "status=provisional",
            "check",
            // set up a case of conflict within organization
            "555=null",
            "value=last",
            "conflicts=[google]", 
            "status=provisional",
            "check",
            // now cross-organizational conflict
            // also check for max value in same organization (4, 1) => 4 not 5
            "444=null",
            "555=best",
            "conflicts=[]", 
            "333=app",
            "value=app",
            "check",
            // now clear winner 8 over 4
            "222=primo",
            "value=primo",
            "status=approved",
            "check",
            // now not so clear, throw in a street value. So it is 8 to 5. This turns into provisional, which seems odd
            "111=best",
            "value=primo",
            "status=provisional",
            "check",
    };
    String expectedValue = null;
    String expectedConflicts = null;
    Status expectedStatus = null;
    String oldValue = null;
    Status oldStatus = null;
    Map<Integer,String> values = new TreeMap<Integer,String>();
    int counter = -1;
    
    for (String test : tests) {
      String[] item = test.split("=");
      String name = item[0];
      String value = item.length < 2 ? null : item[1];
      if (name.equalsIgnoreCase("oldValue")) {
        oldValue = value;
      } else if (name.equalsIgnoreCase("oldStatus")) {
        oldStatus = Status.valueOf(value);
        resolver.newPath(oldValue, oldStatus);
      } else if (name.equalsIgnoreCase("value")) {
        expectedValue = value;
      } else if (name.equalsIgnoreCase("status")) {
        expectedStatus = Status.valueOf(value);
      } else if (name.equalsIgnoreCase("conflicts")) {
        expectedConflicts = value;
      } else if (name.equalsIgnoreCase("clear")) {
        values.clear();
      } else if (!name.equalsIgnoreCase("check")) {
        final int voter = Integer.parseInt(name);
        if (value == null || value.equals("null")) {
          values.remove(voter);
        } else {
          
          values.put(voter, value);
        }
      } else {
        counter++;
        resolver.newPath(oldValue, oldStatus);
        for (int voter : values.keySet()) {
          resolver.add(values.get(voter), voter);
        }
        logln(resolver.toString());
        assertEquals(counter + " value", expectedValue, resolver.getWinningValue());
        assertEquals(counter + " status", expectedStatus, resolver.getWinningStatus());
        assertEquals(counter + " org", expectedConflicts, resolver.getConflictedOrganizations().toString());
      }
    }
  }
}
