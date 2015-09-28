package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.VettingViewer.VoteStatus;

import com.ibm.icu.impl.Relation;
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
 * // you can create a resolver and keep it around. It isn't thread-safe, so either have a separate one per thread (they
 * // are small), or synchronize.
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
    private static final boolean DEBUG = false;

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
     * This is the "high bar" level where flagging is required.
     * @see #getRequiredVotes()
     */
    public static final int HIGH_BAR = Level.tc.votes;

    /**
     * This is the level at which a vote counts. Each level also contains the
     * weight.
     */
    public enum Level {
        locked(0, 999), street(1, 10), vetter(4, 5), expert(8, 3), manager(4, 2), tc(20, 1), admin(100, 0);
        private int votes;
        private int stlevel;

        private Level(int votes, int stlevel) {
            this.votes = votes;
            this.stlevel = stlevel;
        }

        /**
         * Get the votes for each level
         */
        public int getVotes() {
            return votes;
        }

        /**
         * Get the Survey Tool userlevel for each level. (0=admin, 999=locked)
         */
        public int getSTLevel() {
            return stlevel;
        }

        /**
         * Find the Level, given ST Level
         *
         * @param stlevel
         * @return
         */
        public static Level fromSTLevel(int stlevel) {
            for (Level l : Level.values()) {
                if (l.getSTLevel() == stlevel) {
                    return l;
                }
            }
            return null;
        }

        /**
         * Policy: can this user manage the "other" user's settings?
         *
         * @param myOrg
         *            the current organization
         * @param otherLevel
         *            the other user's level
         * @param otherOrg
         *            the other user's organization
         * @return
         */
        public boolean isManagerFor(Organization myOrg, Level otherLevel, Organization otherOrg) {
            return (this == admin || (canManageSomeUsers() &&
                (myOrg == otherOrg) && this.getSTLevel() <= otherLevel.getSTLevel()));
        }

        /**
         * Policy: Can this user manage any users?
         *
         * @return
         */
        public boolean canManageSomeUsers() {
            return this.getSTLevel() <= manager.getSTLevel();
        }

        /**
         * Can this user vote at a reduced level?
         * @return the vote count this user can vote at, or null if it must vote at its assigned level
         */
        public Integer canVoteAtReducedLevel() {
            if (this.getSTLevel() <= tc.getSTLevel()) {
                return vetter.votes;
            } else {
                return null;
            }
        }

        /**
         * Policy: can this user create or set a user to the specified level?
         */
        public boolean canCreateOrSetLevelTo(Level otherLevel) {
            return (this == admin) || // admin can set any level
                (otherLevel != expert && // expert can't be set by any users but admin
                canManageSomeUsers() && // must be some sort of manager
                otherLevel.getSTLevel() >= getSTLevel()); // can't gain higher privs
        }

    };

    /**
     * Internal class for voter information. It is public for testing only
     */
    public static class VoterInfo {
        private Organization organization;
        private Level level;
        private String name;
        private Set<String> locales = new TreeSet<String>();

        public VoterInfo(Organization organization, Level level, String name, Set<String> locales) {
            this.setOrganization(organization);
            this.setLevel(level);
            this.setName(name);
            this.locales.addAll(locales);
        }

        public VoterInfo(Organization organization, Level level, String name) {
            this.setOrganization(organization);
            this.setLevel(level);
            this.setName(name);
        }

        public VoterInfo() {
        }

        public String toString() {
            return "{" + getName() + ", " + getLevel() + ", " + getOrganization() + "}";
        }

        public void setOrganization(Organization organization) {
            this.organization = organization;
        }

        public Organization getOrganization() {
            return organization;
        }

        public void setLevel(Level level) {
            this.level = level;
        }

        public Level getLevel() {
            return level;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setLocales(Set<String> locales) {
            this.locales = locales;
        }

        public void addLocales(Set<String> locales) {
            this.locales.addAll(locales);
        }

        public Set<String> getLocales() {
            return locales;
        }

        public void addLocale(String locale) {
            this.locales.add(locale);
        }
    }

    /**
     * MaxCounter: make sure that we are always only getting the maximum of the values.
     *
     * @author markdavis
     *
     * @param <T>
     */
    static class MaxCounter<T> extends Counter<T> {
        public MaxCounter(boolean b) {
            super(b);
        }

        /**
         * Add, but only to bring up to the maximum value.
         */
        public MaxCounter<T> add(T obj, long countValue) {
            long value = getCount(obj);
            if (value < countValue) {
                super.add(obj, countValue - value); // only add the difference!
            }
            return this;
        };
    }

    /**
     * Internal class for getting from an organization to its vote.
     */
    private static class OrganizationToValueAndVote<T> {
        private final Map<Organization, MaxCounter<T>> orgToVotes = new EnumMap<>(Organization.class);
        private final Counter<T> totalVotes = new Counter<T>();
        private final Map<Organization, Integer> orgToMax = new EnumMap<>(Organization.class);
        private final Counter<T> totals = new Counter<T>(true);
        // map an organization to what it voted for.
        private final Map<Organization, T> orgToAdd = new EnumMap<>(Organization.class);

        OrganizationToValueAndVote() {
            for (Organization org : Organization.values()) {
                orgToVotes.put(org, new MaxCounter<T>(true));
            }
        }

        /**
         * Call clear before considering each new path
         */
        public void clear() {
            for (Map.Entry<Organization, MaxCounter<T>> entry : orgToVotes.entrySet()) {
                //  for (Organization org : orgToVotes.keySet()) {
                // orgToVotes.get(org).clear();
                entry.getValue().clear();
            }
            orgToAdd.clear();
            orgToMax.clear();
            totalVotes.clear();
        }

        public int countValuesWithVotes() {
            return totalVotes.size();
        }

        /**
         * Returns value of voted item, in case there is exactly 1.
         *
         * @return
         */
        public T getSingleVotedItem() {
            return totalVotes.size() != 1 ? null : totalVotes.iterator().next();
        }

        /**
         * Call this to add votes
         *
         * @param value
         * @param voter
         * @param withVotes optionally, vote at a reduced voting level. May not exceed voter's typical level. null = use default level.
         */
        public void add(T value, int voter, Integer withVotes) {
            final VoterInfo info = getVoterToInfo().get(voter);
            if (info == null) {
                throw new UnknownVoterException(voter);
            }
            final int maxVotes = info.getLevel().getVotes(); // max votes available for user
            if (withVotes == null) {
                withVotes = maxVotes; // use max (default)
            } else {
                withVotes = Math.min(withVotes, maxVotes); // override to lower vote count
            }
            addInternal(value, voter, info, withVotes); // do the add
        }

        /**
         * Called by add(T,int,Integer) to actually add a value.
         *
         * @param value
         * @param voter
         * @param info
         * @param votes
         * @see #add(Object, int, Integer)
         */
        private void addInternal(T value, int voter, final VoterInfo info, final int votes) {
            totalVotes.add(value, votes);
            Organization organization = info.getOrganization();
            orgToVotes.get(organization).add(value, votes);
            // add the new votes to orgToMax, if they are greater that what was there
            Integer max = orgToMax.get(info.getOrganization());
            if (max == null || max < votes) {
                orgToMax.put(organization, votes);
            }
        }

        /**
         * Return the overall vote for each organization. It is the max for each value.
         * When the organization is conflicted (the top two values have the same vote), the organization is also added
         * to disputed
         */
        public Counter<T> getTotals(EnumSet<Organization> conflictedOrganizations) {
            if (conflictedOrganizations != null) {
                conflictedOrganizations.clear();
            }
            totals.clear();
            for (Map.Entry<Organization, MaxCounter<T>> entry : orgToVotes.entrySet()) {
                //   for (Organization org : orgToVotes.keySet()) {
//                Counter<T> items = orgToVotes.get(org);
                Counter<T> items = entry.getValue();
                if (items.size() == 0) {
                    continue;
                }
                Iterator<T> iterator = items.getKeysetSortedByCount(false).iterator();
                T value = iterator.next();
                long weight = items.getCount(value);
                Organization org = entry.getKey();
                // if there is more than one item, check that it is less
                if (iterator.hasNext()) {
                    T value2 = iterator.next();
                    long weight2 = items.getCount(value2);
                    // if the votes for #1 are not better than #2, we have a dispute
                    if (weight == weight2) {
                        if (conflictedOrganizations != null) {
                            conflictedOrganizations.add(org);
                        }
                    }
                }
                // This is deprecated, but preserve it until the method is removed.
                orgToAdd.put(org, value);

                // We add the max vote for each of the organizations choices

                for (T item : items.keySet()) {
                    long count = items.getCount(item);
                    totals.add(item, count);
                }
            }
            return totals;
        }

        public int getOrgCount(T winningValue) {
            int orgCount = 0;
            for (Map.Entry<Organization, MaxCounter<T>> entry : orgToVotes.entrySet()) {
//            for (Organization org : orgToVotes.keySet()) {
//                Counter<T> counter = orgToVotes.get(org);
                Counter<T> counter = entry.getValue();
                long count = counter.getCount(winningValue);
                if (count > 0) {
                    orgCount++;
                }
            }
            return orgCount;
        }

        public int getBestPossibleVote() {
            int total = 0;
            for (Map.Entry<Organization, Integer> entry : orgToMax.entrySet()) {
                //    for (Organization org : orgToMax.keySet()) {
//                total += orgToMax.get(org);
                total += entry.getValue();
            }
            return total;
        }

        public String toString() {
            String orgToVotesString = "";
            for (Entry<Organization, MaxCounter<T>> entry : orgToVotes.entrySet()) {
//            for (Organization org : orgToVotes.keySet()) {
//                Counter<T> counter = orgToVotes.get(org);
                Counter<T> counter = entry.getValue();
                if (counter.size() != 0) {
                    if (orgToVotesString.length() != 0) {
                        orgToVotesString += ", ";
                    }
                    Organization org = entry.getKey();
                    orgToVotesString += org + "=" + counter;
                }
            }
            EnumSet<Organization> conflicted = EnumSet.noneOf(Organization.class);
            return "{orgToVotes: " + orgToVotesString + ", totals: " + getTotals(conflicted) + ", conflicted: "
            + conflicted + "}";
        }

        /**
         * This is now deprecated, since the organization may have multiple votes.
         *
         * @param org
         * @return
         * @deprecated
         */
        public T getOrgVote(Organization org) {
            return orgToAdd.get(org);
        }

        public Map<T, Long> getOrgToVotes(Organization org) {
            Map<T, Long> result = new LinkedHashMap<T, Long>();
            MaxCounter<T> counter = orgToVotes.get(org);
            for (T item : counter) {
                result.put(item, counter.getCount(item));
            }
            return result;
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
    private T winningValue;
    private T oValue; // optimal value; winning if better approval status than old
    private T nValue; // next to optimal value
    private List<T> valuesWithSameVotes = new ArrayList<T>();
    private Counter<T> totals = null;

    private Status winningStatus;
    private EnumSet<Organization> conflictedOrganizations = EnumSet
        .noneOf(Organization.class);
    private OrganizationToValueAndVote<T> organizationToValueAndVote = new OrganizationToValueAndVote<T>();
    private T lastReleaseValue;
    private Status lastReleaseStatus;
    private T trunkValue;
    private Status trunkStatus;

    private boolean resolved;
    private int requiredVotes;
    private SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();

    private final Comparator<T> ucaCollator = new Comparator<T>() {
        Collator col = Collator.getInstance(ULocale.ENGLISH);

        public int compare(T o1, T o2) {
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

    public void setTrunk(T trunkValue, Status trunkStatus) {
        this.trunkValue = trunkValue;
        this.trunkStatus = trunkStatus;
    }

    public T getLastReleaseValue() {
        return lastReleaseValue;
    }

    public Status getLastReleaseStatus() {
        return lastReleaseStatus;
    }

    public T getTrunkValue() {
        return trunkValue;
    }

    public Status getTrunkStatus() {
        return trunkStatus;
    }

    /**
     * You must call this locale whenever you are using a VoteResolver with a new locale.
     * More efficient to call the CLDRLocale version.
     *
     * @param locale
     * @return
     * @deprecated need to use the other version to get path-based voting requirements right.
     */
    @Deprecated
    public VoteResolver<T> setLocale(String locale) {
        setLocale(CLDRLocale.getInstance(locale), null);
        return this;
    }

    /**
     * You must call this locale whenever you are using a VoteResolver with a new locale or a new Pathheader
     *
     * @param locale
     * @return
     */
    public VoteResolver<T> setLocale(CLDRLocale locale, PathHeader path) {
        requiredVotes = supplementalDataInfo.getRequiredVotes(locale.getLanguageLocale(), path);
        return this;
    }

    /**
     * Is this an established locale? If so, the requiredVotes is higher.
     * @return
     * @deprecated use {@link #getRequiredVotes()}
     */
    @Deprecated
    public boolean isEstablished() {
        return (requiredVotes == 8);
    }

    /**
     * What are the required votes for this item?
     * @return the number of votes (as of this writing: usually 4, 8 for established locales)
     */
    public int getRequiredVotes() {
        return requiredVotes;
    }

    /**
     * Call this method first, for a new base path. You'll then call add for each value
     * associated with that base path
     */

    public void clear() {
        this.lastReleaseValue = null;
        this.lastReleaseStatus = Status.missing;
        this.trunkValue = null;
        this.trunkStatus = Status.missing;
        organizationToValueAndVote.clear();
        resolved = false;
        values.clear();
    }

    /**
     * Call once for each voter for a value. If there are no voters for an item, then call add(value);
     *
     * @param value
     * @param voter
     * @param withVotes override to lower the user's voting permission. May be null for default.
     */
    public void add(T value, int voter, Integer withVotes) {
        if (resolved) {
            throw new IllegalArgumentException("Must be called after clear, and before any getters.");
        }
        organizationToValueAndVote.add(value, voter, withVotes);
        values.add(value);
    }

    /**
     * Call once for each voter for a value. If there are no voters for an item, then call add(value);
     *
     * @param value
     * @param voter
     */
    public void add(T value, int voter) {
        add(value, voter, null);
    }

    /**
     * Call if a value has no voters. It is safe to also call this if there is a voter, just unnecessary.
     *
     * @param value
     * @param voter
     */
    public void add(T value) {
        if (resolved) {
            throw new IllegalArgumentException("Must be called after clear, and before any getters.");
        }
        values.add(value);
    }

    private Set<T> values = new TreeSet<T>(ucaCollator);

    private final Comparator<T> votesThenUcaCollator = new Comparator<T>() {
        Collator col = Collator.getInstance(ULocale.ENGLISH);

        public int compare(T o1, T o2) {
            long v1 = organizationToValueAndVote.totalVotes.get(o1);
            long v2 = organizationToValueAndVote.totalVotes.get(o2);
            if (v1 != v2) {
                return v1 < v2 ? 1 : -1; // use reverse order, biggest first!
            }
            return col.compare(String.valueOf(o1), String.valueOf(o2));
        }
    };

    private void resolveVotes() {
        resolved = true;
        // get the votes for each organization
        valuesWithSameVotes.clear();
        totals = organizationToValueAndVote.getTotals(conflictedOrganizations);
        final Set<T> sortedValues = totals.getKeysetSortedByCount(false, votesThenUcaCollator);
        Iterator<T> iterator = sortedValues.iterator();
        // if there are no (unconflicted) votes, return lastRelease
        if (sortedValues.size() == 0) {
            if (trunkStatus != null && (lastReleaseStatus == null || trunkStatus.compareTo(lastReleaseStatus) >= 0)) {
                winningStatus = trunkStatus;
                winningValue = trunkValue;
            } else {
                winningStatus = lastReleaseStatus;
                winningValue = lastReleaseValue;
            }
            valuesWithSameVotes.add(winningValue); // may be null
            return;
        }
        // otherwise pick the smallest value.
        if (values.size() == 0) {
            throw new IllegalArgumentException("No values added to resolver");
        }
        // get the optimal value, and the penoptimal value
        long weight1 = 0;
        long weight2 = 0;
        T value2 = null;
        int i = -1;
        for (T value : sortedValues) {
            ++i;
            long valueWeight = totals.getCount(value);
            if (i == 0) {
                winningValue = value;
                weight1 = valueWeight;
                valuesWithSameVotes.add(value);
            } else {
                if (i == 1) {
                    // get the next item if there is one
                    if (iterator.hasNext()) {
                        value2 = value;
                        weight2 = valueWeight;
                    }
                }
                if (valueWeight == weight1) {
                    valuesWithSameVotes.add(value);
                } else {
                    break;
                }
            }
        }
        oValue = winningValue;
        nValue = value2; // save this
        // here is the meat.
        winningStatus = computeStatus(weight1, weight2, trunkStatus);
        // if we are not as good as the trunk, use the trunk
        if (trunkStatus != null && winningStatus.compareTo(trunkStatus) < 0) {
            winningStatus = trunkStatus;
            winningValue = trunkValue;
            valuesWithSameVotes.clear();
            valuesWithSameVotes.add(winningValue);
        }
    }

    private Status computeStatus(long weight1, long weight2, Status oldStatus) {
        int orgCount = organizationToValueAndVote.getOrgCount(winningValue);
        return weight1 > weight2 &&
            (weight1 >= requiredVotes) ? Status.approved
                : weight1 > weight2 &&
                (weight1 >= 4 && Status.contributed.compareTo(oldStatus) > 0
                || weight1 >= 2 && orgCount >= 2) ? Status.contributed
                    : weight1 >= weight2 && weight1 >= 2 ? Status.provisional
                        : Status.unconfirmed;
    }

    public Status getPossibleWinningStatus() {
        if (!resolved) {
            resolveVotes();
        }
        Status possibleStatus = computeStatus(organizationToValueAndVote.getBestPossibleVote(), 0, trunkStatus);
        return possibleStatus.compareTo(winningStatus) > 0 ? possibleStatus : winningStatus;
    }

    /**
     * If the winning item is not approved, and if all the people who voted had voted for the winning item,
     * would it have made contributed or approved?
     *
     * @return
     */
    public boolean isDisputed() {
        if (!resolved) {
            resolveVotes();
        }
        if (winningStatus.compareTo(VoteResolver.Status.contributed) >= 0) {
            return false;
        }
        VoteResolver.Status possibleStatus = getPossibleWinningStatus();
        if (possibleStatus.compareTo(VoteResolver.Status.contributed) >= 0) {
            return true;
        }
        return false;
    }

    public Status getWinningStatus() {
        if (!resolved) {
            resolveVotes();
        }
        return winningStatus;
    }

    /**
     * Returns O Value as described in http://cldr.unicode.org/index/process#TOC-Voting-Process.
     * Not always the same as the Winning Value.
     *
     * @return
     */
    public T getOValue() {
        if (!resolved) {
            resolveVotes();
        }
        return oValue;
    }

    /**
     * Returns N Value as described in http://cldr.unicode.org/index/process#TOC-Voting-Process.
     * Not always the same as the Winning Value.
     *
     * @return
     */
    public T getNValue() {
        if (!resolved) {
            resolveVotes();
        }
        return nValue;
    }

    /**
     * @deprecated
     */
    public T getNextToWinningValue() {
        return getNValue();
    }

    /**
     * Returns Winning Value as described in http://cldr.unicode.org/index/process#TOC-Voting-Process.
     * Not always the same as the O Value.
     *
     * @return
     */
    public T getWinningValue() {
        if (!resolved) {
            resolveVotes();
        }
        return winningValue;
    }

    public List<T> getValuesWithSameVotes() {
        if (!resolved) {
            resolveVotes();
        }
        return new ArrayList<T>(valuesWithSameVotes);
    }

    public EnumSet<Organization> getConflictedOrganizations() {
        if (!resolved) {
            resolveVotes();
        }
        return conflictedOrganizations;
    }

    /**
     * What value did this organization vote for?
     *
     * @param org
     * @return
     */
    public T getOrgVote(Organization org) {
        return organizationToValueAndVote.getOrgVote(org);
    }

    public Map<T, Long> getOrgToVotes(Organization org) {
        return organizationToValueAndVote.getOrgToVotes(org);
    }

    public String toString() {
        return "{"
            + "lastRelease: {" + lastReleaseValue + ", " + lastReleaseStatus + "}, "
            + "trunk: {" + trunkValue + ", " + trunkStatus + "}, "
            + organizationToValueAndVote
            + ", sameVotes: " + valuesWithSameVotes
            + ", O: " + getOValue()
            + ", N: " + getNValue()
            + ", totals: " + totals
            + ", winning: {" + getWinningValue() + ", " + getWinningStatus() + "}"
            + "}";
    }

    public static Map<String, Map<Organization, Relation<Level, Integer>>> getLocaleToVetters() {
        Map<String, Map<Organization, Relation<Level, Integer>>> result = new TreeMap<String, Map<Organization, Relation<Level, Integer>>>();
        for (int voter : getVoterToInfo().keySet()) {
            VoterInfo info = getVoterToInfo().get(voter);
            if (info.getLevel() == Level.locked) {
                continue;
            }
            for (String locale : info.getLocales()) {
                Map<Organization, Relation<Level, Integer>> orgToVoter = result.get(locale);
                if (orgToVoter == null) {
                    result.put(locale, orgToVoter = new TreeMap<Organization, Relation<Level, Integer>>());
                }
                Relation<Level, Integer> rel = orgToVoter.get(info.getOrganization());
                if (rel == null) {
                    orgToVoter.put(info.getOrganization(), rel = Relation.of(new TreeMap<Level, Set<Integer>>(), TreeSet.class));
                }
                rel.put(info.getLevel(), voter);
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
     * <p>
     * Synchronized, however, once this is called, you must NOT change the contents of your copy of testVoterToInfo. You
     * can create a whole new one and set it.
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
        computeMaxVotes();
    }

    /**
     * Set the voter info from a users.xml file.
     * <p>
     * Synchronized, however, once this is called, you must NOT change the contents of your copy of testVoterToInfo. You
     * can create a whole new one and set it.
     */
    public static void setVoterToInfo(String fileName) {
        MyHandler myHandler = new MyHandler();
        XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
        xfr.read(fileName, XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER, false);
        setVoterToInfo(myHandler.testVoterToInfo);

        computeMaxVotes();
    }

    private static synchronized void computeMaxVotes() {
        // compute the localeToOrganizationToMaxVote
        localeToOrganizationToMaxVote = new TreeMap<String, Map<Organization, Level>>();
        for (int voter : getVoterToInfo().keySet()) {
            VoterInfo info = getVoterToInfo().get(voter);
            if (info.getLevel() == Level.tc || info.getLevel() == Level.locked) {
                continue; // skip TCs, locked
            }

            for (String locale : info.getLocales()) {
                Map<Organization, Level> organizationToMaxVote = localeToOrganizationToMaxVote.get(locale);
                if (organizationToMaxVote == null) {
                    localeToOrganizationToMaxVote.put(locale,
                        organizationToMaxVote = new TreeMap<Organization, Level>());
                }
                Level maxVote = organizationToMaxVote.get(info.getOrganization());
                if (maxVote == null || info.getLevel().compareTo(maxVote) > 0) {
                    organizationToMaxVote.put(info.getOrganization(), info.getLevel());
                    // System.out.println("Example best voter for " + locale + " for " + info.organization + " is " +
                    // info);
                }
            }
        }
        CldrUtility.protectCollection(localeToOrganizationToMaxVote);
    }

    /**
     * Handles fine in xml format, turning into:
     * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/level[@n="1"][@type="TC"]
     * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/name
     * Mike Tardif
     * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/org
     * Adobe
     * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/locales[@type="edit"]
     *
     * Steven's new format:
     * //users[@generated="Wed May 07 15:57:15 PDT 2008"][@host="tintin"][@obscured="true"]
     * /user[@id="286"][@email="?@??.??"]
     * /level[@n="1"][@type="TC"]
     */

    static class MyHandler extends XMLFileReader.SimpleHandler {
        private static final Pattern userPathMatcher = Pattern
            .compile(
                "//users(?:[^/]*)"
                    + "/user\\[@id=\"([^\"]*)\"](?:[^/]*)"
                    + "/("
                    + "org" +
                    "|name" +
                    "|level\\[@n=\"([^\"]*)\"]\\[@type=\"([^\"]*)\"]" +
                    "|locales\\[@type=\"([^\"]*)\"]" +
                    "(?:/locale\\[@id=\"([^\"]*)\"])?"
                    + ")", Pattern.COMMENTS);

        enum Group {
            all, userId, mainType, n, levelType, localeType, localeId;
            String get(Matcher matcher) {
                return matcher.group(this.ordinal());
            };
        }

        private static final boolean DEBUG_HANDLER = false;
        Map<Integer, VoterInfo> testVoterToInfo = new TreeMap<Integer, VoterInfo>();
        Matcher matcher = userPathMatcher.matcher("");

        public void handlePathValue(String path, String value) {
            if (DEBUG_HANDLER)
                System.out.println(path + "\t" + value);
            if (matcher.reset(path).matches()) {
                if (DEBUG_HANDLER) {
                    for (int i = 1; i <= matcher.groupCount(); ++i) {
                        Group group = Group.values()[i];
                        System.out.println(i + "\t" + group + "\t" + group.get(matcher));
                    }
                }
                int id = Integer.parseInt(Group.userId.get(matcher));
                VoterInfo voterInfo = testVoterToInfo.get(id);
                if (voterInfo == null) {
                    testVoterToInfo.put(id, voterInfo = new VoterInfo());
                }
                final String mainType = Group.mainType.get(matcher);
                if (mainType.equals("org")) {
                    Organization org = Organization.fromString(value);
                    voterInfo.setOrganization(org);
                    value = org.name(); // copy name back into value
                } else if (mainType.equals("name")) {
                    voterInfo.setName(value);
                } else if (mainType.startsWith("level")) {
                    String level = Group.levelType.get(matcher).toLowerCase();
                    voterInfo.setLevel(Level.valueOf(level));
                } else if (mainType.startsWith("locale")) {
                    final String localeIdString = Group.localeId.get(matcher);
                    if (localeIdString != null) {
                        voterInfo.addLocale(localeIdString.split("_")[0]);
                    } else if (DEBUG_HANDLER) {
                        System.out.println("\tskipping");
                    }
                } else if (DEBUG_HANDLER) {
                    System.out.println("\tFailed match* with " + path + "=" + value);
                }
            } else {
                System.out.println("\tFailed match with " + path + "=" + value);
            }
        }
    }

    public static Map<Integer, String> getIdToPath(String fileName) {
        XPathTableHandler myHandler = new XPathTableHandler();
        XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
        xfr.read(fileName, XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER, false);
        return myHandler.pathIdToPath;
    }

    static class XPathTableHandler extends XMLFileReader.SimpleHandler {
        Matcher matcher = Pattern.compile("id=\"([0-9]+)\"").matcher("");
        Map<Integer, String> pathIdToPath = new HashMap<Integer, String>();

        public void handlePathValue(String path, String value) {
            // <xpathTable host="tintin.local" date="Tue Apr 29 14:34:32 PDT 2008" count="18266" >
            // <xpath
            // id="1">//ldml/dates/calendars/calendar[@type="gregorian"]/dateFormats/dateFormatLength[@type="short"]/dateFormat[@type="standard"]/pattern[@type="standard"]</xpath>
            if (!matcher.reset(path).find()) {
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

    public enum Type {
        proposal, optimal
    };

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
            return "{oldStatus: " + oldStatus
                + ", surveyType: " + surveyType
                + ", surveyStatus: " + surveyStatus
                + ", voters: " + voterString
                + "};";
        }
    }

    /*
     * <locale-votes host="tintin.local" date="Tue Apr 29 14:34:32 PDT 2008"
     * oldVersion="1.5.1" currentVersion="1.6" resolved="false" locale="zu">
     * <row baseXpath="1">
     * <item xpath="2855" type="proposal" id="1" status="unconfirmed">
     * <old status="unconfirmed"/>
     * </item>
     * <item xpath="1" type="optimal" id="56810" status="confirmed">
     * <vote user="210"/>
     * </item>
     * </row>
     * ...
     * A base path has a set of candidates. Each candidate has various items of information.
     */
    static class VotesHandler extends XMLFileReader.SimpleHandler {
        Map<Integer, Map<Integer, CandidateInfo>> basepathToInfo = new TreeMap<Integer, Map<Integer, CandidateInfo>>();
        XPathParts parts = new XPathParts();

        public void handlePathValue(String path, String value) {
            try {
                parts.set(path);
                if (parts.size() < 2) {
                    // empty data
                    return;
                }
                int baseId = Integer.parseInt(parts.getAttributeValue(1, "baseXpath"));
                Map<Integer, CandidateInfo> info = basepathToInfo.get(baseId);
                if (info == null) {
                    basepathToInfo.put(baseId, info = new TreeMap<Integer, CandidateInfo>());
                }
                int itemId = Integer.parseInt(parts.getAttributeValue(2, "xpath"));
                CandidateInfo candidateInfo = info.get(itemId);
                if (candidateInfo == null) {
                    info.put(itemId, candidateInfo = new CandidateInfo());
                    candidateInfo.surveyType = Type.valueOf(parts.getAttributeValue(2, "type"));
                    candidateInfo.surveyStatus = Status.valueOf(fixBogusDraftStatusValues(parts.getAttributeValue(2,
                        "status")));
                    // ignore id
                }
                if (parts.size() < 4) {
                    return;
                }
                final String lastElement = parts.getElement(3);
                if (lastElement.equals("old")) {
                    candidateInfo.oldStatus = Status.valueOf(fixBogusDraftStatusValues(parts.getAttributeValue(3,
                        "status")));
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
            Level maxVote = orgToMaxVoteHere.get(info.getOrganization());
            if (maxVote == null || info.getLevel().compareTo(maxVote) > 0) {
                orgToMaxVoteHere.put(info.getOrganization(), info.getLevel());
                // System.out.println("*Best voter for " + info.organization + " is " + info);
            }
        }
        return orgToMaxVoteHere;
    }

    public static class UnknownVoterException extends RuntimeException {
        /**
         *
         */
        private static final long serialVersionUID = 3430877787936678609L;
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
        if (attributeValue == null) return "approved";
        if ("confirmed".equals(attributeValue)) return "approved";
        if ("true".equals(attributeValue)) return "unconfirmed";
        if ("unknown".equals(attributeValue)) return "unconfirmed";
        return attributeValue;
    }

    public int size() {
        return values.size();
    }

    /**
     * Returns a map from value to resolved vote count, in descending order.
     * If the winning item is not there, insert at the front.
     * If the last-release item is not there, insert at the end.
     *
     * @return
     */
    public Map<T, Long> getResolvedVoteCounts() {
        if (!resolved) {
            resolveVotes();
        }
        Map<T, Long> result = new LinkedHashMap<T, Long>();
        if (winningValue != null && !totals.containsKey(winningValue)) {
            result.put(winningValue, 0L);
        }
        for (T value : totals.getKeysetSortedByCount(false, votesThenUcaCollator)) {
            result.put(value, totals.get(value));
        }
        if (lastReleaseValue != null && !totals.containsKey(lastReleaseValue)) {
            result.put(lastReleaseValue, 0L);
        }
        return result;
    }

    public VoteStatus getStatusForOrganization(Organization orgOfUser) {
        if (!resolved) {
            resolveVotes();
        }

        T win = getWinningValue();
        Status winStatus = getWinningStatus();
        boolean provisionalOrWorse = Status.provisional.compareTo(winStatus) >= 0;

        T orgVote = orgOfUser == null ? null : getOrgVote(orgOfUser);
        // get the number of other values with votes.
        int itemsWithVotes = organizationToValueAndVote.countValuesWithVotes();
        T singleVotedItem = organizationToValueAndVote.getSingleVotedItem();

        if (orgVote != null && !orgVote.equals(win)) {
            // We voted and lost
            return VoteStatus.losing;
        } else if (itemsWithVotes > 1) {
            // If there are votes for two items, we should look at them.
            return VoteStatus.disputed;
        } else if (singleVotedItem != null && !singleVotedItem.equals(win)) {
            // If someone voted but didn't win
            return VoteStatus.disputed;
        } else if (provisionalOrWorse) {
            // If the value is provisional, it needs more votes.
            return VoteStatus.provisionalOrWorse;
        } else if (itemsWithVotes == 0) {
            // The value is ok, but we capture that there are no votes, for revealing items like unsync'ed
            return VoteStatus.ok_novotes;
        } else {
            // We voted, we won, value is approved, no disputes, have votes
            return VoteStatus.ok;
        }
    }
}
