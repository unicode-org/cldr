package org.unicode.cldr.unittest;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DelegatingIterator;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.CandidateInfo;
import org.unicode.cldr.util.VoteResolver.Level;
import org.unicode.cldr.util.VoteResolver.Organization;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.VoteResolver.VoterInfo;
import org.unicode.cldr.util.VoteResolver.Type;
import org.unicode.cldr.util.VoteResolver.UnknownVoterException;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class TestUtilities extends TestFmwk {
  private static final UnicodeSet DIGITS = new UnicodeSet("[0-9]");
  private static final boolean DEBUG = false;
  static TestInfo testInfo = TestInfo.getInstance();

  public static void main(String[] args) {
    new TestUtilities().run(args);
  }

  public void TestDelegatingIterator() {
    Set<String> s = new TreeSet<String>(Arrays.asList(new String[] { "a", "b", "c" }));
    Set<String> t = new LinkedHashSet<String>(Arrays.asList(new String[] { "f", "d", "e" }));
    StringBuilder result = new StringBuilder();

    for (String u : DelegatingIterator.iterable(s, t)) {
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
    Counter<String> counter = new Counter<String>(true);
    Comparator<String> uca = Collator.getInstance(ULocale.ENGLISH);
    
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

    assertEquals("getMap", counter.getMap().toString(), "{c=95, b=151, a=95, d=-3}");

    assertEquals("getKeysetSortedByKey", Arrays.asList("a", "b", "c", "d"), new ArrayList(counter
            .getKeysetSortedByKey()));

    assertEquals("getKeysetSortedByCount(true, null)", Arrays.asList("d", "c", "a", "b"), new ArrayList(counter
            .getKeysetSortedByCount(true, null)));

    assertEquals("getKeysetSortedByCount(true, uca), value", Arrays.asList("d", "a", "c", "b"), new ArrayList(
            counter.getKeysetSortedByCount(true, uca)));

    assertEquals("getKeysetSortedByCount(false, null), descending", Arrays.asList("b", "c", "a", "d"),
            new ArrayList(counter.getKeysetSortedByCount(false, null)));

    assertEquals("getKeysetSortedByCount(false, uca), descending, value", Arrays.asList("b", "a", "c", "d"),
            new ArrayList(counter.getKeysetSortedByCount(false, uca)));
  }
  
  public void TestOrganizationOrder() {
    Map<String, Organization> stringToOrg = new TreeMap<String, Organization>();
    for (Organization org : Organization.values()) {
      stringToOrg.put(org.toString(), org);
    }
    List reordered = new ArrayList(stringToOrg.values());
    List plain = Arrays.asList(Organization.values());
    if (!plain.equals(reordered)) {
      errln("Items not in alphabetical order: use " + reordered);
    }
  }

  public void TestVoteResolverData() {
    final PrintWriter errorLogPrintWriter = this.getErrorLogPrintWriter();
    final PrintWriter logPrintWriter = this.getLogPrintWriter();
    String userFile = "/Users/markdavis/Documents/workspace/DATA/survey_voting/users.xml";
    String votesDirectory = "/Users/markdavis/Documents/workspace/DATA/survey_voting/vxml/votes/";
    

    VoteResolver.setVoterToInfo(userFile);
    Map<String, Map<Organization, Relation<Level, Integer>>> map = VoteResolver
            .getLocaleToVetters();
    for (String locale : map.keySet()) {
      Map<Organization, Relation<Level, Integer>> orgToLevelToVoter = map.get(locale);
      String localeName = null;
      try {
        localeName = testInfo.getEnglish().getName(locale);
      } catch (RuntimeException e) {
        errln("Invalid locale:\t" + locale);
        localeName = "UNVALID(" + locale + ")";
      }
      if (DEBUG) {
        for (Organization org : orgToLevelToVoter.keySet()) {
          log(locale + "\t" + localeName + "\t" + org + ":");
          final Relation<Level, Integer> levelToVoter = orgToLevelToVoter.get(org);
          for (Level level : levelToVoter.keySet()) {
            log("\t" + level + "=" + levelToVoter.getAll(level).size());
          }
          logln("");
        }
      }
    }
    
    File votesDir = new File(votesDirectory);
    for (String file : votesDir.list()) {
      if (file.startsWith("xpathTable")) {
        continue;
      }
      if (file.endsWith(".xml")) {
        final String locale = file.substring(0,file.length()-4);
        try {
          checkLocaleVotes(locale, votesDirectory, errorLogPrintWriter, logPrintWriter);
        } catch (RuntimeException e) {
          throw (RuntimeException) new IllegalArgumentException("Can't process " + locale).initCause(e);
        }
      }
    }
  }
  
  private String getValue(int item) {
    return String.valueOf(item);
  }
  
  static final boolean SHOW_DETAILS = Utility.getProperty("showdetails", false);

  private void checkLocaleVotes(final String locale, String votesDirectory, PrintWriter errorLog, PrintWriter warningLog) {
    //logln("*** Locale " + locale + ": \t***");
    Map<Organization, Level> orgToMaxVote = VoteResolver.getOrganizationToMaxVote(locale);
    if (orgToMaxVote.size() == 0) {
      logln("");
      warnln(locale + ": \tNo organizations with translators");
    } else if (!locale.contains("_")){
      logln("");
      logln(locale + ": \tOrganizations with translators:\t" + orgToMaxVote);
    }

    Map<Integer, Map<Integer, CandidateInfo>> info = VoteResolver.getBaseToAlternateToInfo(votesDirectory + locale + ".xml");
    Set<Organization> missingOrganizations = EnumSet.noneOf(Organization.class);
    Counter<Organization> missingOrganizationCounter = new Counter<Organization>(true);
    Counter<Organization> goodOrganizationCounter = new Counter<Organization>(true);
    Counter<Status> winningStatusCounter = new Counter<Status>(true);
    EnumSet<Organization> conflictedOrganizations = EnumSet.noneOf(Organization.class);
    Set<Integer> missingOptimals = new TreeSet<Integer>();
    
    Set<Integer> surveyVsVoteResolverDifferences = new TreeSet<Integer>();
    
    Set<Integer> unknownVotersSoFar = new HashSet<Integer>();

    Counter<Status> oldStatusCounter = new Counter<Status>(true);
    Counter<Status> surveyStatusCounter = new Counter<Status>(true);
    Counter<Type> surveyTypeCounter = new Counter<Type>(true);
    VoteResolver<String> voteResolver = new VoteResolver<String>();
    Map<String,Integer> valueToItem = new HashMap<String,Integer>();
    
    for (int basePath : info.keySet()) {
      final Map<Integer, CandidateInfo> itemInfo = info.get(basePath);
      // if there is any approved value, then continue;
      Status surveyWinningStatus = null;
      Integer surveyWinningValue = null;

      // find the last release status and value
      voteResolver.clear();
      boolean haveOldStatus = false;

      valueToItem.clear();
     
      for (int item : itemInfo.keySet()) {
        String itemValue = getValue(item);
        if (valueToItem.containsKey(itemValue)) {
          errln(locale + ": \tTwo alternatives with same value:\t" + item + ", " + itemValue);
        } else {
          valueToItem.put(itemValue, item);
        }
        
        CandidateInfo candidateInfo = itemInfo.get(item);
        oldStatusCounter.add(candidateInfo.oldStatus,1);
        surveyStatusCounter.add(candidateInfo.surveyStatus,1);
        surveyTypeCounter.add(candidateInfo.surveyType,1);
        if (candidateInfo.surveyType == Type.optimal) {
          if (surveyWinningValue != null) {
            errln(locale + ": \tDuplicate optimal item:\t" + item);
          }
          surveyWinningStatus = candidateInfo.surveyStatus;
          surveyWinningValue = item;
        }
        if (candidateInfo.oldStatus != null) {
          if (haveOldStatus) {
            errln(locale + ": \tDuplicate optimal item:\t" + item);
          }
          haveOldStatus = true;
          voteResolver.setLastRelease(itemValue, candidateInfo.oldStatus);
        }
        voteResolver.add(itemValue);
        for (int voter : candidateInfo.voters) {
          try {
            voteResolver.add(itemValue, voter);
          } catch (UnknownVoterException e) {
            if (!unknownVotersSoFar.contains(e.getVoter())) {
              errln(locale + ":\t" + e);
              unknownVotersSoFar.add(e.getVoter());
            }
          }
        }
      }
      if (surveyWinningValue == null) {
        missingOptimals.add(basePath);
        surveyWinningValue = -1;
      }

      EnumSet<Organization> basePathConflictedOrganizations = voteResolver.getConflictedOrganizations();
      conflictedOrganizations.addAll(basePathConflictedOrganizations);
      
      Status winningStatus = voteResolver.getWinningStatus();
      String winningValue = voteResolver.getWinningValue();
      
      winningStatusCounter.add(winningStatus,1);

      final boolean sameResults = surveyWinningStatus == winningStatus && surveyWinningValue.equals(winningValue);
      if (surveyWinningStatus == Status.approved && sameResults) {
        continue;
      } 
      if (!sameResults) {
        surveyVsVoteResolverDifferences.add(basePath);
        if (SHOW_DETAILS) {
          showPaths(locale, basePath, itemInfo);
          log("\t***Different results for:\t" + basePath);
          if (surveyWinningStatus != winningStatus) {
            log(", status ST:\t" + surveyWinningStatus);
            log(", VR:\t" + winningStatus);
          }
          if (!surveyWinningValue.equals(winningValue)) {
            log(", value ST:\t" + surveyWinningValue);
            log(", VR:\t" + winningValue);
          }
          logln("");
        }
      }
      
      CandidateInfo candidateInfo = itemInfo.get(valueToItem.get(winningValue));
      Map<Organization, Level> orgToMaxVoteHere = VoteResolver.getOrganizationToMaxVote(candidateInfo.voters);
      

      // if the winning item is less than contributed, record the organizations that haven't given their maximum vote to the winning item.
      if (winningStatus.compareTo(Status.contributed) < 0) {
        //       showPaths(basePath, itemInfo);
        missingOrganizations.clear();
        for (Organization org : orgToMaxVote.keySet()) {
          Level maxVote = orgToMaxVote.get(org);
          Level maxVoteHere = orgToMaxVoteHere.get(org);
          if (maxVoteHere == null || maxVoteHere.compareTo(maxVote) < 0) {
            missingOrganizations.add(org);
            missingOrganizationCounter.add(org,1);
          }
        }
        //logln("&Missing organizations:\t" + missingOrganizations);
      } else {
        for (Organization org : orgToMaxVote.keySet()) {
          Level maxVote = orgToMaxVote.get(org);
          Level maxVoteHere = orgToMaxVoteHere.get(org);
          if (maxVoteHere == null || maxVoteHere.compareTo(maxVote) < 0) {
          } else {
            goodOrganizationCounter.add(org,1);
          }
        }
      }
    }
    if (missingOptimals.size() != 0) {
      errln(locale + ": \tSurvey Tool missing optimal item for basePaths:\t" + missingOptimals);
    }
    if (surveyVsVoteResolverDifferences.size() > 0) {
      errln(locale + ": \tSurvey Tool vs VoteResolver differences (approx):\t" + surveyVsVoteResolverDifferences.size());
    }
    if (missingOrganizationCounter.size() > 0) {
      if (SHOW_DETAILS) {
        logln(locale + ": \toldStatus values:\t" + oldStatusCounter + ", TOTAL:\t" + oldStatusCounter.getTotal());
        logln(locale + ": \tsurveyType values:\t" + surveyTypeCounter + ", TOTAL:\t" + surveyTypeCounter.getTotal());
        logln(locale + ": \tsurveyStatus values:\t" + surveyStatusCounter + ", TOTAL:\t" + surveyStatusCounter.getTotal());
      }
      logln(locale + ": \tMIA organizations:\t" + missingOrganizationCounter);
      logln(locale + ": \tConflicted organizations:\t" + conflictedOrganizations);
      logln(locale + ": \tCool organizations!:\t" + goodOrganizationCounter);
    }
    logln(locale + ": \tOptimal Status:\t" + winningStatusCounter);
  }

  private void showPaths(String locale, int basePath, final Map<Integer, CandidateInfo> itemInfo) {
    logln(locale + " basePath:\t" + basePath);
    for (int item : itemInfo.keySet()) {
      CandidateInfo candidateInfo = itemInfo.get(item);
      logln("\tpath:\t" + item + ", " + candidateInfo);
    }
  }

  public void TestVoteResolver() {
    Map<Integer, VoterInfo> testdata = (Map<Integer, VoterInfo>) Utility.asMap(new Object[][] {
      { 888, new VoterInfo(Organization.guest, Level.street, "O. Henry") }, 
      { 777, new VoterInfo(Organization.gnome, Level.street, "S. Henry") }, 
      { 666, new VoterInfo(Organization.google, Level.vetter, "J. Smith") },
      { 555, new VoterInfo(Organization.google, Level.street, "S. Jones") },
      { 444, new VoterInfo(Organization.google, Level.vetter, "S. Samuels") },
      { 333, new VoterInfo(Organization.apple, Level.vetter, "A. Mutton") },
      { 222, new VoterInfo(Organization.adobe, Level.expert, "A. Aldus") },
      { 111, new VoterInfo(Organization.ibm, Level.street, "J. Henry") },
      });
    VoteResolver.setVoterToInfo(testdata);
    VoteResolver<String> resolver = new VoteResolver<String>();
    String[] tests = {
            "oldValue=old-value",
            "oldStatus=provisional",
            "comment=Check that identical values get the alphabetically lowest",
            "555=next",
            "666=best",
            "value=best",
            "conflicts=[]",
            "status=contributed",
            "check",
            
            "comment=now give next a slight edge (5 to 4)",
            "444=next",
            "value=next",
            "status=contributed",
            "check",
            
            "comment=set up a case of conflict within organization",
            "555=null",
            "value=old-value",
            "conflicts=[google]",
            "status=provisional",
            "check",
            
            "comment=now cross-organizational conflict, also check for max value in same organization (4, 1) => 4 not 5",
            "444=null",
            "555=best",
            "conflicts=[]",
            "333=app",
            "value=app",
            "check",
            
            "comment=now clear winner 8 over 4",
            "222=primo",
            "value=primo",
            "status=approved",
            "check",
            
            "comment=now not so clear, throw in a street value. So it is 8 to 5. (used to be provisional)",
            "111=best",
            "value=primo",
            "status=contributed",
            "check",
    };
    String expectedValue = null;
    String expectedConflicts = null;
    Status expectedStatus = null;
    String oldValue = null;
    Status oldStatus = null;
    Map<Integer, String> values = new TreeMap<Integer, String>();
    int counter = -1;

    for (String test : tests) {
      String[] item = test.split("=");
      String name = item[0];
      String value = item.length < 2 ? null : item[1];
      if (name.equalsIgnoreCase("comment")) {
        logln("#\t" + value);
      } else if (name.equalsIgnoreCase("oldValue")) {
        oldValue = value;
      } else if (name.equalsIgnoreCase("oldStatus")) {
        oldStatus = Status.valueOf(value);
      } else if (name.equalsIgnoreCase("value")) {
        expectedValue = value;
      } else if (name.equalsIgnoreCase("status")) {
        expectedStatus = Status.valueOf(value);
      } else if (name.equalsIgnoreCase("conflicts")) {
        expectedConflicts = value;
      } else if (name.equalsIgnoreCase("clear")) {
        values.clear();
      } else if (DIGITS.containsAll(name)) {
        final int voter = Integer.parseInt(name);
        if (value == null || value.equals("null")) {
          values.remove(voter);
        } else {
          values.put(voter, value);
        }
      } else if (name.equalsIgnoreCase("check")) {
        counter++;
        resolver.setLastRelease(oldValue, oldStatus);
        for (int voter : values.keySet()) {
          resolver.add(values.get(voter), voter);
        }
        logln(resolver.toString());
        assertEquals(counter + " value", expectedValue, resolver.getWinningValue());
        assertEquals(counter + " status", expectedStatus, resolver.getWinningStatus());
        assertEquals(counter + " org", expectedConflicts, resolver.getConflictedOrganizations()
                .toString());
        resolver.clear();
      } else {
        errln("unknown command:\t" + test);
      }
    }
  }
}
