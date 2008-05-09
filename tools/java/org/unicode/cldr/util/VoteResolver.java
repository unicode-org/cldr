package org.unicode.cldr.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

/**
 * This class implements the vote resolution process agreed to by the CLDR
 * committee. Here is an example of usage:
 * 
 * <pre>
 * // before doing anything, initialize the voter data (who are the voters at what levels) with setVoterToInfo.
 * // We assume this doesn't change often
 * // here is some fake data:
 * VoteResolver.setVoterToInfo(Utility.asMap(new Object[][] {
 *     { 666, new VoterInfo(Organization.google, Level.vetter, &quot;J. Smith&quot;) },
 *     { 555, new VoterInfo(Organization.google, Level.street, &quot;S. Jones&quot;) },
 *     { 444, new VoterInfo(Organization.google, Level.vetter, &quot;S. Samuels&quot;) },
 *     { 333, new VoterInfo(Organization.apple, Level.vetter, &quot;A. Mutton&quot;) },
 *     { 222, new VoterInfo(Organization.adobe, Level.expert, &quot;A. Aldus&quot;) },
 *     { 111, new VoterInfo(Organization.ibm, Level.street, &quot;J. Henry&quot;) }, }));
 * 
 * // you can create a resolver and keep it around. It isn't thread-safe, so either have a separate one per thread (they are small), or synchronize.
 * VoteResolver resolver = new VoteResolver();
 * 
 * // For any particular base path, set the values
 * // set the 1.5 status (if we're working on 1.6). This &lt;b&gt;must&lt;/b&gt; be done for each new base path
 * resolver.newPath(oldValue, oldStatus);
 * 
 * // now add some values, with who voted for them
 * resolver.add(value1, voter1);
 * resolver.add(value1, voter2);
 * resolver.add(value2, voter3);
 * 
 * // Once you've done that, you can get the results for the base path
 * winner = resolver.getWinningValue();
 * status = resolver.getWinningStatus();
 * conflicts = resolver.getConflictedOrganizations();
 * </pre>
 */
public class VoteResolver<T> {
  private static final boolean           DEBUG                      = false;

  /**
   * The status levels according to the committee, in ascending order
   */
  public enum Status {
    missing, unconfirmed, provisional, contributed, approved;
    public static Status fromString(String source) {
      return source == null ? missing : Status.valueOf(source);
    }
  }

  /**
   * This list needs updating as a new organizations are added; that's by design
   * so that we know when new ones show up.
   */
  public enum Organization {
    //apple, adobe, google, ibm, gnome, pakistan, india, guest, iran_hci, kotoistus, lisa, openoffice_org, sun, surveytool, utilika
    adobe, apple, gnome, google, guest, ibm, india, iran_hci, kotoistus, lisa, openoffice_org, pakistan, sun, surveytool, utilika
  };

  /**
   * This is the level at which a vote counts. Each level also contains the
   * weight.
   */
  public enum Level {
    locked(0), street(1), vetter(4), expert(8), tc(8), admin(100);
    private int votes;

    private Level(int votes) {
      this.votes = votes;
    }

    /**
     * Get the votes for each level
     */
    public int getVotes() {
      return votes;
    }
  };

  /**
   * Internal class for voter information. It is public for testing only
   */
  public static class VoterInfo {
    Organization organization;
    Level        level;
    String       name;
    Set<String>  locales = new TreeSet<String>();

    public VoterInfo(Organization organization, Level level, String name) {
      this.organization = organization;
      this.level = level;
      this.name = name;
    }

    public VoterInfo() {
    }

    public String toString() {
      return "{" + name + ", " + level + ", " + organization + "}";
    }
  }

  /**
   * Internal class for getting from an organization to its vote.
   */
  private static class OrganizationToValueAndVote<T> {
    private Map<Organization, Counter<T>> orgToVotes = new HashMap<Organization, Counter<T>>();
    private Map<Organization, Integer>         orgToMax   = new HashMap<Organization, Integer>();
    private Counter<T>                    totals     = new Counter<T>(true);

    OrganizationToValueAndVote() {
      for (Organization org : Organization.values()) {
        orgToVotes.put(org, new Counter<T>(true));
      }
    }

    /**
     * Call clear before considering each new path
     */
    public void clear() {
      for (Organization org : orgToVotes.keySet()) {
        orgToVotes.get(org).clear();
      }
    }

    /**
     * Call this to add votes
     * 
     * @param value
     * @param voter
     */
    public void add(T value, int voter) {
      VoterInfo info = getVoterToInfo().get(voter);
      if (info == null) {
        throw new UnknownVoterException(voter);
      }
      final int votes = info.level.getVotes();
      orgToVotes.get(info.organization).add(value, votes);
      // add the new votes to orgToMax, if they are greater that what was there
      Integer max = orgToMax.get(info.organization);
      if (max == null || max < votes) {
        orgToMax.put(info.organization, votes);
      }
    }
    
    /**
     * Return the overall vote for each organization. It is the max, except when
     * the organization is conflicted (the top two values have the same vote).
     * In that case, it is zero and the organization is added to disputed
     */
    public Counter<T> getTotals(EnumSet<Organization> conflictedOrganizations) {
      conflictedOrganizations.clear();
      totals.clear();
      for (Organization org : orgToVotes.keySet()) {
        Counter<T> items = orgToVotes.get(org);
        if (items.size() == 0) {
          continue;
        }
        Iterator<T> iterator = items.getKeysetSortedByCount(false).iterator();
        T value = iterator.next();
        long weight = items.getCount(value);
        // if there is more than one item, check that it is less
        if (iterator.hasNext()) {
          T value2 = iterator.next();
          long weight2 = items.getCount(value2);
          // if the votes for #1 are not better than #2, we have a dispute
          if (weight == weight2) {
            conflictedOrganizations.add(org);
            continue;
          }
        }
        // we don't actually add the total votes; instead we add the max found
        // for the organization
        totals.add(value, orgToMax.get(org));
      }
      return totals;
    }

    public int getOrgCount(T winningValue) {
      int orgCount = 0;
      for (Organization org : orgToVotes.keySet()) {
        Counter<T> counter = orgToVotes.get(org);
        long count = counter.getCount(winningValue);
        if (count > 0) {
          orgCount++;
        }
      }
      return orgCount;
    }

    public String toString() {
      String orgToVotesString = "";
      for (Organization org: orgToVotes.keySet()) {
        Counter<T> counter = orgToVotes.get(org);
        if (counter.size() != 0) {
          if (orgToVotesString.length() != 0) {
            orgToVotesString += ", ";
          }
          orgToVotesString += org + "=" + counter;
        }
      }
      EnumSet<Organization> conflicted = EnumSet.noneOf(Organization.class);
      return "{orgToVotes:" + orgToVotesString + ", totals:" + getTotals(conflicted) + ", conflicted:" + conflicted + "}";
    }
  }

  /**
   * Static info read from file
   */
  private static Map<Integer, VoterInfo> voterToInfo;

  private static TreeMap<String, Map<Organization, Level>> localeToOrganizationToMaxVote;
  
  /**
   * Data built internally
   */
  private T                         winningValue;
  private Status                         winningStatus;
  private EnumSet<Organization>          conflictedOrganizations    = EnumSet
                                                                            .noneOf(Organization.class);
  private OrganizationToValueAndVote<T>     organizationToValueAndVote = new OrganizationToValueAndVote<T>();
  private T                         lastReleaseValue;
  private Status                         lastReleaseStatus;
  private boolean                        resolved;
  private final Comparator<T> ucaCollator = new Comparator<T>() {
    Collator col = Collator.getInstance(ULocale.ENGLISH);
    public int compare(T o1, T o2) {
      // TODO Auto-generated method stub
      return col.compare(String.valueOf(o1), String.valueOf(o2));
    }   
  };

  /**
   * Call this method first, for a new path. You'll then call add for each value
   * associated with that path
   * 
   * @param valueToVoter
   * @param lastReleaseValue
   * @param lastReleaseStatus
   */

  public void setLastRelease(T lastReleaseValue, Status lastReleaseStatus) {
    this.lastReleaseValue = lastReleaseValue;
    this.lastReleaseStatus = lastReleaseStatus == null ? Status.missing : lastReleaseStatus;
  }


  /**
   * Call this method first, for a new base path. You'll then call add for each value
   * associated with that base path
   */

  public void clear() {
    this.lastReleaseValue = null;
    this.lastReleaseStatus = Status.missing;
    organizationToValueAndVote.clear();
    resolved = false;
    values.clear();
  }

  /**
   * Call once for each voter for a value. If there are no voters for an item, then call add(value);
   * @param value
   * @param voter
   */
  public void add(T value, int voter) {
    if (resolved) {
      throw new IllegalArgumentException("Must be called after clear, and before any getters.");
    }
    organizationToValueAndVote.add(value, voter);
    values.add(value);
  }
  
  /**
   * Call if a value has no voters. It is safe to also call this if there is a voter, just unnecessary.
   * @param value
   * @param voter
   */
  public void add(T value) {
    if (resolved) {
      throw new IllegalArgumentException("Must be called after clear, and before any getters.");
    }
    values.add(value);
  }

  private Set<T> values = new HashSet<T>();

  private void resolveVotes() {
    resolved = true;
    // get the votes for each organization
    Counter<T> totals = organizationToValueAndVote.getTotals(conflictedOrganizations);
    Iterator<T> iterator = totals.getKeysetSortedByCount(false, ucaCollator).iterator();
    // if there are no (unconflicted) votes, return lastRelease
    if (!iterator.hasNext()) {
      // if there *was* a real winning status, then return it.
      if (lastReleaseStatus != Status.missing) {
        winningStatus = lastReleaseStatus;
        winningValue = lastReleaseValue;
        return;
      }
      // otherwise pick the smallest value.
      if (values.size() == 0) {
        throw new IllegalArgumentException("No values added to resolver");
      }
      winningStatus = Status.unconfirmed;
      winningValue = values.iterator().next();
      return;
    }
    // get the optimal value
    winningValue = iterator.next();
    long weight = totals.getCount(winningValue);
    // could optimize the following line by only computing later.
    int orgCount = organizationToValueAndVote.getOrgCount(winningValue);
    T value2 = null;
    long weight2 = 0;
    // if there is a tie
    // get the next item if there is one
    if (iterator.hasNext()) {
      value2 = iterator.next();
      weight2 = totals.getCount(value2);
    }
    // here is the meat.
    winningStatus = weight >= 2 * weight2 && weight >= 8 ? Status.approved
            : (weight > weight2 && weight >= 4
               || weight >= 2 * weight2 && weight >= 2 && orgCount >= 2
              ) ? Status.contributed
            : weight >= weight2 && weight >= 2 ? Status.provisional
            : Status.unconfirmed;
    // if we are not as good as the last release, use the last release
    if (winningStatus.compareTo(lastReleaseStatus) < 0) {
      winningStatus = lastReleaseStatus;
      winningValue = lastReleaseValue;
    }
  }

  public Status getWinningStatus() {
    if (!resolved) {
      resolveVotes();
    }
    return winningStatus;
  }

  public T getWinningValue() {
    if (!resolved) {
      resolveVotes();
    }
    return winningValue;
  }

  public EnumSet<Organization> getConflictedOrganizations() {
    if (!resolved) {
      resolveVotes();
    }
    return conflictedOrganizations;
  }

  public String toString() {
    return "{lastRelease:" + lastReleaseValue + ", " + lastReleaseStatus + ", " + organizationToValueAndVote
            + "}";
  }

  public static Map<String, Map<Organization, Relation<Level, Integer>>> getLocaleToVetters() {
    Map<String, Map<Organization, Relation<Level, Integer>>> result = new TreeMap<String, Map<Organization, Relation<Level, Integer>>>();
    for (int voter : getVoterToInfo().keySet()) {
      VoterInfo info = getVoterToInfo().get(voter);
      if (info.level == Level.locked) {
        continue;
      }
      for (String locale : info.locales) {
        Map<Organization, Relation<Level, Integer>> orgToVoter = result.get(locale);
        if (orgToVoter == null) {
          result.put(locale, orgToVoter = new TreeMap<Organization, Relation<Level, Integer>>());
        }
        Relation<Level, Integer> rel = orgToVoter.get(info.organization);
        if (rel == null) {
          orgToVoter.put(info.organization, rel = new Relation(new TreeMap(), TreeSet.class));
        }
        rel.put(info.level, voter);
      }
    }
    return result;
  }

  private static Map<Integer, VoterInfo> getVoterToInfo() {
    synchronized (VoteResolver.class) {
      return voterToInfo;
    }
  }
  
  public static VoterInfo getInfoForVoter(int voter) {
    return getVoterToInfo().get(voter);
  }

  /**
   * Set the voter info.
   * <p>Synchronized, however, once this is called, you must
   * NOT change the contents of your copy of testVoterToInfo. You can create a whole new one
   * and set it.
   */
  public static void setVoterToInfo(Map<Integer, VoterInfo> testVoterToInfo) {
    synchronized (VoteResolver.class) {
      VoteResolver.voterToInfo = testVoterToInfo;
    }
    if (DEBUG) {
      for (int id : testVoterToInfo.keySet()) {
        System.out.println("\t" + id + "=" + testVoterToInfo.get(id));
      }
    }
  }

  /**
   * Set the voter info from a users.xml file.
   * <p>Synchronized, however, once this is called, you must
   * NOT change the contents of your copy of testVoterToInfo. You can create a whole new one
   * and set it.
   */
  public static void setVoterToInfo(String fileName) {
    MyHandler myHandler = new MyHandler();
    XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
    xfr.read(fileName, XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER, false);
    setVoterToInfo(myHandler.testVoterToInfo);
    
    // compute the localeToOrganizationToMaxVote
    localeToOrganizationToMaxVote = new TreeMap<String, Map<Organization,Level>>();
    for (int voter : getVoterToInfo().keySet()) {
      VoterInfo info = getVoterToInfo().get(voter);
      if (info.level == Level.tc || info.level == Level.locked) {
        continue; // skip TCs, locked
      }

      for (String locale : info.locales) {
        Map<Organization, Level> organizationToMaxVote = localeToOrganizationToMaxVote.get(locale);
        if (organizationToMaxVote == null) {
          localeToOrganizationToMaxVote.put(locale, organizationToMaxVote = new TreeMap<Organization, Level>());
        }
        Level maxVote = organizationToMaxVote.get(info.organization);
        if (maxVote == null || info.level.compareTo(maxVote) > 0) {
          organizationToMaxVote.put(info.organization, info.level);
          // System.out.println("Example best voter for " + locale + " for " + info.organization + " is " + info);
        }
      }
    }
    Utility.protectCollection(localeToOrganizationToMaxVote);
  }

  /**
   * Handles fine in xml format, turning into:
   * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/level[@n="1"][@type="TC"]
   * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/name
   * Mike Tardif
   * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/org
   * Adobe
   * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/locales[@type="edit"]
   */

  static class MyHandler extends XMLFileReader.SimpleHandler {
    private static final boolean DEBUG_HANDLER           = false;
    Map<Integer, VoterInfo>      testVoterToInfo = new TreeMap<Integer, VoterInfo>();
    Matcher                      matcher         = Pattern
                                                         .compile(
                                                                 "//users\\[@host=\"([^\"]*)\"]"
                                                                         + "/user\\[@id=\"([^\"]*)\"]\\[@email=\"([^\"]*)\"]"
                                                                         + "/("
                                                                         + "org|name|level\\[@n=\"([^\"]*)\"]\\[@type=\"([^\"]*)\"]" +
                                                                         		"|locales\\[@type=\"([^\"]*)\"]/locale\\[@id=\"([^\"]*)\"]"
                                                                         + ")").matcher("");

    public void handlePathValue(String path, String value) {
      if (DEBUG_HANDLER)
        System.out.println(path + "\t" + value);
      if (matcher.reset(path).matches()) {
        if (DEBUG_HANDLER) {
          for (int i = 1; i <= matcher.groupCount(); ++i) {
            System.out.println(i + "\t" + matcher.group(i));
          }
        }
        int id = Integer.parseInt(matcher.group(2));
        VoterInfo voterInfo = testVoterToInfo.get(id);
        if (voterInfo == null) {
          testVoterToInfo.put(id, voterInfo = new VoterInfo());
        }
        final String mainType = matcher.group(4);
        if (mainType.equals("org")) {
          value = value.toLowerCase().replace('-', '_').replace('.', '_');
          if (value.contains("pakistan")) {
            value = "pakistan";
          } else if (value.contains("utilika foundation")) {
            value = "utilika";
          }
          voterInfo.organization = Organization.valueOf(value);
        } else if (mainType.equals("name")) {
          voterInfo.name = value;
        } else if (mainType.startsWith("level")) {
          String level = matcher.group(6).toLowerCase();
          voterInfo.level = Level.valueOf(level);
        } else if (mainType.startsWith("locale")) {
          voterInfo.locales.add(matcher.group(8).split("_")[0]);
        }
      } else {
        //System.out.println("\tFailed match with " + path + "=" + value);
      }
    }
  }
  public static Map<Integer,String> getVoteInfo(String fileName) {
    XPathTableHandler myHandler = new XPathTableHandler();
    XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
    xfr.read(fileName, XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER, false);
    return myHandler.pathIdToPath;
  }

  static class XPathTableHandler extends XMLFileReader.SimpleHandler {
    Matcher matcher = Pattern.compile("id=\"([0-9]+)\"").matcher("");
    Map<Integer,String> pathIdToPath = new HashMap<Integer,String>();
    
    public void handlePathValue(String path, String value) {
      // <xpathTable host="tintin.local" date="Tue Apr 29 14:34:32 PDT 2008"  count="18266" >
      // <xpath id="1">//ldml/dates/calendars/calendar[@type="gregorian"]/dateFormats/dateFormatLength[@type="short"]/dateFormat[@type="standard"]/pattern[@type="standard"]</xpath>
      if (!matcher.reset(path).matches()) {
        throw new IllegalArgumentException("Unknown path " + path);
      }
      pathIdToPath.put(Integer.parseInt(matcher.group(1)), value);
    }
  }
  
  public static Map<Integer, Map<Integer, CandidateInfo>> getBaseToAlternateToInfo(String fileName) {
    try {
      VotesHandler myHandler = new VotesHandler();
      XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
      xfr.read(fileName, XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER, false);
      return myHandler.basepathToInfo;
    } catch (Exception e) {
      throw (RuntimeException) new IllegalArgumentException("Can't handle file: " + fileName).initCause(e);
    }
  }
  
  public enum Type {proposal, optimal};
  
  public static class CandidateInfo {
    public Status oldStatus;
    public Type surveyType;
    public Status surveyStatus;
    public Set<Integer> voters = new TreeSet<Integer>();
    public String toString() {
      StringBuilder voterString = new StringBuilder("{");
      for (int voter : voters) {
        VoterInfo voterInfo = getInfoForVoter(voter);
        if (voterString.length() > 1) {
          voterString.append(" ");
        }
        voterString.append(voter);
        if (voterInfo != null) {
          voterString.append(" ").append(voterInfo);
        }
      }
      voterString.append("}");
      return 
      "{oldStatus: " + oldStatus
      + ", surveyType: " + surveyType
      + ", surveyStatus: " + surveyStatus
      + ", voters: " + voterString
      + "};";
    }
  }

  /*
   * <locale-votes host="tintin.local" date="Tue Apr 29 14:34:32 PDT 2008"
   * oldVersion="1.5.1" currentVersion="1.6" resolved="false" locale="zu">
   *  <row baseXpath="1">
   *    <item xpath="2855" type="proposal" id="1" status="unconfirmed">
   *      <old status="unconfirmed"/>
   *    </item>
   *    <item xpath="1" type="optimal" id="56810" status="confirmed">
   *      <vote user="210"/>
   *    </item>
   *  </row>
   *  ...
   * A base path has a set of candidates. Each candidate has various items of information.
   */
  static class VotesHandler extends XMLFileReader.SimpleHandler {
    Map<Integer,Map<Integer,CandidateInfo>> basepathToInfo = new TreeMap<Integer,Map<Integer,CandidateInfo>>();
    XPathParts parts = new XPathParts();
    
    public void handlePathValue(String path, String value) {
      try {
        parts.set(path);
        if (parts.size() < 2) {
          // empty data
          return;
        }
        int baseId = Integer.parseInt(parts.getAttributeValue(1, "baseXpath"));
        Map<Integer,CandidateInfo> info = basepathToInfo.get(baseId);
        if (info == null) {
          basepathToInfo.put(baseId, info = new TreeMap<Integer,CandidateInfo>());
        }
        int itemId = Integer.parseInt(parts.getAttributeValue(2, "xpath"));
        CandidateInfo candidateInfo = info.get(itemId);
        if (candidateInfo == null) {
          info.put(itemId, candidateInfo = new CandidateInfo());
          candidateInfo.surveyType = Type.valueOf(parts.getAttributeValue(2, "type"));
          candidateInfo.surveyStatus = Status.valueOf(fixBogusDraftStatusValues(parts.getAttributeValue(2, "status")));
          // ignore id
        }
        if (parts.size() < 4) {
          return;
        }
        final String lastElement = parts.getElement(3);
        if (lastElement.equals("old")) {
          candidateInfo.oldStatus = Status.valueOf(fixBogusDraftStatusValues(parts.getAttributeValue(3, "status")));
        } else if (lastElement.equals("vote")) {
          candidateInfo.voters.add(Integer.parseInt(parts.getAttributeValue(3, "user")));
        } else {
          throw new IllegalArgumentException("unknown option: " + path);
        }
      } catch (Exception e) {
        throw (RuntimeException) new IllegalArgumentException("Can't handle path: " + path).initCause(e);
      }
    }

  }
  public static Map<Organization, Level> getOrganizationToMaxVote(String locale) {
    locale = locale.split("_")[0]; // take base language
    Map<Organization, Level> result = localeToOrganizationToMaxVote.get(locale);
    if (result == null) {
      result = Collections.emptyMap();
    }
    return result;
  }


  public static Map<Organization, Level> getOrganizationToMaxVote(Set<Integer> voters) {
    Map<Organization, Level> orgToMaxVoteHere = new TreeMap<Organization, Level>();
    for (int voter : voters) {
      VoterInfo info = getInfoForVoter(voter);
      if (info == null) {
        continue; // skip unknown voter
      }
      Level maxVote = orgToMaxVoteHere.get(info.organization);
      if (maxVote == null || info.level.compareTo(maxVote) > 0) {
        orgToMaxVoteHere.put(info.organization, info.level);
        //System.out.println("*Best voter for " + info.organization + " is " + info);
      }
    }
    return orgToMaxVoteHere;
  }
  
  public static class UnknownVoterException extends RuntimeException {
    int voter;
    public UnknownVoterException(int voter) {
      this.voter = voter;
    }
    public String toString() {
      return "Unknown voter: " + voter;
    }
    public int getVoter() {
      return voter;
    }
  }
  public static String fixBogusDraftStatusValues(String attributeValue) {
      if(attributeValue==null) return "approved";
      if ("confirmed".equals(attributeValue)) return "approved";
      if ("true".equals(attributeValue)) return "unconfirmed";
      if ("unknown".equals(attributeValue)) return "unconfirmed";
      return attributeValue;
  }
}
