package org.unicode.cldr.util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckWidths;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.util.VettingViewer.VoteStatus;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
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
 * [TODO: function newPath doesn't exist, revise this documentation]
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
     * A placeholder for winningValue when it would otherwise be null.
     * It must match NO_WINNING_VALUE in the client JavaScript code.
     */
    private static String NO_WINNING_VALUE = "no-winning-value";

    /**
     * The status levels according to the committee, in ascending order
     *
     * Status corresponds to icons as follows:
     * A checkmark means it’s approved and is slated to be used. A cross means it’s a missing value.
     * Green/orange check: The item has enough votes to be used in CLDR.
     * Red/orange/black X: The item does not have enough votes to be used in CLDR, by most implementations (or is completely missing).
     * Reference: http://cldr.unicode.org/translation/getting-started/guide
     *
     * New January, 2019: When the item is inherited, i.e., winningValue is INHERITANCE_MARKER (↑↑↑),
     * then orange/red X are replaced by orange/red up-arrow. That change is made only on the client.
     * Reference: https://unicode.org/cldr/trac/ticket/11103
     *
     * Status.approved:    approved.png    = green check
     * Status.contributed: contributed.png = orange check
     * Status.provisional: provisional.png = orange X (or inherited_provisional.png orange up-arrow if inherited)
     * Status.unconfirmed: unconfirmed.png = red X (or inherited_unconfirmed.png red up-arrow if inherited
     * Status.missing:     missing.png     = black X
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
        locked(   0 /* votes */, 999 /* stlevel */),
        street(   1 /* votes */, 10  /* stlevel */),
        anonymous(0 /* votes */, 8   /* stlevel */),
        vetter(   4 /* votes */, 5   /* stlevel */),
        expert(   8 /* votes */, 3   /* stlevel */),
        manager(  4 /* votes */, 2   /* stlevel */),
        tc(      20 /* votes */, 1   /* stlevel */),
        admin(  100 /* votes */, 0   /* stlevel */);

        /**
         * PERMANENT_VOTES is used by TC voters to "lock" locale+path permanently (including future versions, until unlocked),
         * in the current VOTE_VALUE table. It is public for STFactory.java and PermanentVote.java.
         */
        public static final int PERMANENT_VOTES = 1000;

        /**
         * LOCKING_VOTES is used (nominally by ADMIN voter, but not really by someone logged in as ADMIN, instead
         * by combination of two PERMANENT_VOTES) to "lock" locale+path permanently in the LOCKED_XPATHS table.
         * It is public for STFactory.PerLocaleData.loadVoteValues.
         */
        public static final int LOCKING_VOTES = 2000;

        /**
         * The vote count a user of this level normally votes with
         */
        private final int votes;

        /**
         * The level as an integer, where 0 = admin, ..., 999 = locked
         */
        private final int stlevel;

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
         * @return the Level corresponding to the integer
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
         * Policy: can this user create or set a user to the specified level?
         *
         * @param otherLevel the desired new level for the other user
         *
         * Note: UserRegistry.canSetUserLevel enforces additional limitations depending
         * on more than this user's level and the other user's desired new level
         */
        public boolean canCreateOrSetLevelTo(Level otherLevel) {
            return otherLevel != expert && // expert can't be set by any user
                canManageSomeUsers() && // must be some sort of manager
                otherLevel.getSTLevel() >= getSTLevel(); // can't gain higher privs
        }

        /**
         * Can a user with this level vote with the given vote count?
         *
         * @param withVotes the given vote count
         * @return true if the user can vote with the given vote count, else false
         */
        public boolean canVoteWithCount(int withVotes) {
            /*
             * ADMIN is allowed to vote with LOCKING_VOTES, but not directly in the GUI, only
             * by two TC voting together with PERMANENT_VOTES. Therefore LOCKING_VOTES is omitted
             * from the GUI menu (voteCountMenu), but included in canVoteWithCount.
             */
            if (withVotes == LOCKING_VOTES && this == admin) {
                return true;
            }
            Set<Integer> menu = getVoteCountMenu();
            return menu == null ? withVotes == this.votes : menu.contains(withVotes);
        }

        /**
         * If not null, an array of different vote counts from which a user of this
         * level is allowed to choose.
         */
        private ImmutableSet<Integer> voteCountMenu = null;

        /**
         * Get the ordered immutable set of different vote counts a user of this level can vote with
         *
         * @return the set, or null if the user has no choice of vote count
         */
        public ImmutableSet<Integer> getVoteCountMenu() {
            return voteCountMenu;
        }

        /*
         * Set voteCountMenu for admin and tc in this static block, which will be run after
         * all the constructors have run, rather than in the constructor itself. For example,
         * vetter.votes needs to be defined before we can set admin.voteCountMenu.
         */
        static {
            admin.voteCountMenu = ImmutableSet.of(vetter.votes, admin.votes); /* Not LOCKING_VOTES; see canVoteWithCount */
            tc.voteCountMenu = ImmutableSet.of(vetter.votes, tc.votes, PERMANENT_VOTES);
        }
    }

    /**
     * Internal class for voter information. It is public for testing only
     */
    public static class VoterInfo {
        private Organization organization;
        private Level level;
        private String name;
        private Set<String> locales = new TreeSet<>();

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

        @Override
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

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            VoterInfo other = (VoterInfo) obj;
            return organization.equals(other.organization)
                && level.equals(other.level)
                && name.equals(other.name)
                && Objects.equal(locales, other.locales);
        }

        @Override
        public int hashCode() {
            return organization.hashCode() ^ level.hashCode() ^ name.hashCode();
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
        @Override
        public MaxCounter<T> add(T obj, long countValue, long time) {
            long value = getCount(obj);
            if ((value <= countValue)) {
                super.add(obj, countValue - value, time); // only add the difference!
            }
            return this;
        }
    }

    /**
     * Internal class for getting from an organization to its vote.
     */
    private static class OrganizationToValueAndVote<T> {
        private final Map<Organization, MaxCounter<T>> orgToVotes = new EnumMap<>(Organization.class);
        private final Counter<T> totalVotes = new Counter<>();
        private final Map<Organization, Integer> orgToMax = new EnumMap<>(Organization.class);
        private final Counter<T> totals = new Counter<>(true);
        private Map<String, Long> nameTime = new LinkedHashMap<>();
        // map an organization to what it voted for.
        private final Map<Organization, T> orgToAdd = new EnumMap<>(Organization.class);
        private T baileyValue;
        private boolean baileySet; // was the bailey value set

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
                entry.getValue().clear();
            }
            orgToAdd.clear();
            orgToMax.clear();
            totalVotes.clear();
            baileyValue = null;
            baileySet = false;
        }

        /**
         * Returns value of voted item, in case there is exactly 1.
         *
         * @return
         */
        private T getSingleVotedItem() {
            return totalVotes.size() != 1 ? null : totalVotes.iterator().next();
        }

        public Map<String, Long> getNameTime() {
            return nameTime;
        }

        /**
         * Call this to add votes
         *
         * @param value
         * @param voter
         * @param withVotes optionally, vote at a non-typical voting level. May not exceed voter's maximum allowed level. null = use default level.
         * @param date
         */
        public void add(T value, int voter, Integer withVotes, Date date) {
            final VoterInfo info = getVoterToInfo().get(voter);
            if (info == null) {
                throw new UnknownVoterException(voter);
            }
            Level level = info.getLevel();
            if (withVotes == null || !level.canVoteWithCount(withVotes)) {
                withVotes = level.getVotes();
            }
            addInternal(value, info, withVotes, date); // do the add
        }

        /**
         * Called by add(T,int,Integer) to actually add a value.
         *
         * @param value
         * @param info
         * @param votes
         * @param date
         * @see #add(Object, int, Integer)
         */
        private void addInternal(T value, final VoterInfo info, final int votes, Date time) {
            if (baileySet == false) {
                throw new IllegalArgumentException("setBaileyValue must be called before add");
            }
            totalVotes.add(value, votes, time.getTime());
            nameTime.put(info.getName(), time.getTime());
            if (DEBUG) {
                System.out.println("totalVotes Info: " + totalVotes.toString());
            }
            if (DEBUG) {
                System.out.println("VoteInfo: " + info.getName() + info.getOrganization());
            }
            Organization organization = info.getOrganization();
            //orgToVotes.get(organization).clear();
            orgToVotes.get(organization).add(value, votes, time.getTime());
            if (DEBUG) {
                System.out.println("Adding now Info: " + organization.displayName + info.getName() + " is adding: " + votes + value
                    + new Timestamp(time.getTime()).toString());
            }

            if (DEBUG) {
                System.out.println("addInternal: " + organization.displayName + " : " + orgToVotes.get(organization).toString());
            }

            // add the new votes to orgToMax, if they are greater that what was there
            Integer max = orgToMax.get(info.getOrganization());
            if (max == null || max < votes) {
                orgToMax.put(organization, votes);
            }
        }

        /**
         * Return the overall vote for each organization. It is the max for each value.
         * When the organization is conflicted (the top two values have the same vote), the organization is also added
         * to disputed.
         *
         * @param conflictedOrganizations if not null, to be filled in with the set of conflicted organizations.
         */
        public Counter<T> getTotals(EnumSet<Organization> conflictedOrganizations) {
            if (conflictedOrganizations != null) {
                conflictedOrganizations.clear();
            }
            totals.clear();

            for (Map.Entry<Organization, MaxCounter<T>> entry : orgToVotes.entrySet()) {
                Counter<T> items = entry.getValue();
                if (items.size() == 0) {
                    continue;
                }
                Iterator<T> iterator = items.getKeysetSortedByCount(false).iterator();
                T value = iterator.next();
                long weight = items.getCount(value);
                if (weight == 0) {
                    continue;
                }
                Organization org = entry.getKey();
                if (DEBUG) {
                    System.out.println("sortedKeys?? " + value + " " + org.displayName);
                }

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
                /*
                 * TODO: explain the above comment, and follow through. What is deprecated (orgToAdd, or getOrgVote)?
                 * Preserve until which method is removed (getOrgVote)?
                 */
                orgToAdd.put(org, value);

                // We add the max vote for each of the organizations choices
                long maxCount = 0;
                T considerItem = null;
                long considerCount = 0;
                long maxtime = 0;
                long considerTime = 0;
                for (T item : items.keySet()) {
                    if (DEBUG) {
                        System.out.println("Items in order: " + item.toString() + new Timestamp(items.getTime(item)).toString());
                    }
                    long count = items.getCount(item);
                    long time = items.getTime(item);
                    if (count > maxCount) {
                        maxCount = count;
                        maxtime = time;
                        considerItem = item;
                        if (DEBUG) {
                            System.out.println("count>maxCount: " + considerItem.toString() + ":" + new Timestamp(considerTime).toString() + " COUNT: "
                                + considerCount + "MAXCOUNT: " + maxCount);
                        }
                        considerCount = items.getCount(considerItem);
                        considerTime = items.getTime(considerItem);

                    } else if ((time > maxtime) && (count == maxCount)) {
                        maxCount = count;
                        maxtime = time;
                        considerItem = item;
                        considerCount = items.getCount(considerItem);
                        considerTime = items.getTime(considerItem);
                        if (DEBUG) {
                            System.out.println("time>maxTime: " + considerItem.toString() + ":" + new Timestamp(considerTime).toString());
                        }
                    }
                }
                orgToAdd.put(org, considerItem);
                totals.add(considerItem, considerCount, considerTime);

                if (DEBUG) {
                    System.out.println("Totals: " + totals.toString() + " : " + new Timestamp(considerTime).toString());
                }

            }

            if (DEBUG) {
                System.out.println("FINALTotals: " + totals.toString());
            }
            return totals;
        }

        public int getOrgCount(T winningValue) {
            int orgCount = 0;
            for (Map.Entry<Organization, MaxCounter<T>> entry : orgToVotes.entrySet()) {
                Counter<T> counter = entry.getValue();
                long count = counter.getCount(winningValue);
                if (count > 0) {
                    orgCount++;
                }
            }
            return orgCount;
        }

        private int getBestPossibleVote() {
            int total = 0;
            for (Map.Entry<Organization, Integer> entry : orgToMax.entrySet()) {
                total += entry.getValue();
            }
            return total;
        }

        @Override
        public String toString() {
            String orgToVotesString = "";
            for (Entry<Organization, MaxCounter<T>> entry : orgToVotes.entrySet()) {
                Counter<T> counter = entry.getValue();
                if (counter.size() != 0) {
                    if (orgToVotesString.length() != 0) {
                        orgToVotesString += ", ";
                    }
                    Organization org = entry.getKey();
                    orgToVotesString += org.toString() + "=" + counter.toString();
                }
            }
            EnumSet<Organization> conflicted = EnumSet.noneOf(Organization.class);
            return "{orgToVotes: " + orgToVotesString
                + ", totals: " + getTotals(conflicted)
                + ", conflicted: " + conflicted.toString()
                + "}";
        }

        /**
         * This is now deprecated, since the organization may have multiple votes.
         *
         * @param org
         * @return
         * @deprecated
         */
        @Deprecated
        public T getOrgVote(Organization org) {
            return orgToAdd.get(org);
        }

        public T getOrgVoteRaw(Organization orgOfUser) {
            return orgToAdd.get(orgOfUser);
        }

        public Map<T, Long> getOrgToVotes(Organization org) {
            Map<T, Long> result = new LinkedHashMap<>();
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
    private List<T> valuesWithSameVotes = new ArrayList<>();
    private Counter<T> totals = null;

    private Status winningStatus;
    private EnumSet<Organization> conflictedOrganizations = EnumSet
        .noneOf(Organization.class);
    private OrganizationToValueAndVote<T> organizationToValueAndVote = new OrganizationToValueAndVote<>();
    private T trunkValue;
    private Status trunkStatus;

    private boolean resolved;
    private boolean valueIsLocked;
    private int requiredVotes;
    private SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();

    /**
     * usingKeywordAnnotationVoting: when true, use a special voting method for keyword
     * annotations that have multiple values separated by bar, like "happy | joyful".
     * See http://unicode.org/cldr/trac/ticket/10973 .
     * public, set in STFactory.java; could make it private and add param to
     * the VoteResolver constructor.
     */
    private boolean usingKeywordAnnotationVoting = false;

    private static final Collator englishCollator = Collator.getInstance(ULocale.ENGLISH).freeze();

    /**
     * Used for comparing objects of type T
     */
    private final Comparator<T> objectCollator = new Comparator<T>() {
        @Override
        public int compare(T o1, T o2) {
            return englishCollator.compare(String.valueOf(o1), String.valueOf(o2));
        }
    };

    /**
     * Set the trunk value and status for this VoteResolver.
     *
     * Assume that we don't need to make any changes for INHERITANCE_MARKER here;
     * the input will have INHERITANCE_MARKER if appropriate; do nothing special
     * for a specific value that happens to match the Bailey value.
     *
     * Reference: https://unicode.org/cldr/trac/ticket/11857
     *
     * @param trunkValue the trunk value
     * @param trunkStatus the trunk status
     *
     * TODO: consider renaming: setTrunk to setBaseline; getTrunkValue to getBaselineValue; getTrunkStatus to getBaselineStatus
     */
    public void setTrunk(T trunkValue, Status trunkStatus) {
        this.trunkValue = trunkValue;
        this.trunkStatus = trunkValue == null ? Status.missing : trunkStatus;
    }

    public T getTrunkValue() {
        return trunkValue;
    }

    public Status getTrunkStatus() {
        return trunkStatus;
    }

    /**
     * Set the number of required votes for this VoteResolver, based on the given locale and PathHeader
     *
     * You must call this whenever you are using a VoteResolver with a new locale or a new PathHeader
     *
     * @param locale the CLDRLocale
     * @param pathHeader the PathHeader
     */
    public void setLocale(CLDRLocale locale, PathHeader pathHeader) {
        requiredVotes = supplementalDataInfo.getRequiredVotes(locale, pathHeader);
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
     * associated with that base path.
     */
    public void clear() {
        this.trunkValue = null;
        this.trunkStatus = Status.missing;
        this.setUsingKeywordAnnotationVoting(false);
        organizationToValueAndVote.clear();
        resolved = valueIsLocked = false;
        values.clear();
    }

    /**
     * Get the bailey value (what the inherited value would be if there were no
     * explicit value) for this VoteResolver.
     *
     * Throw an exception if !baileySet.
     *
     * @return the bailey value.
     *
     * Called by STFactory.PerLocaleData.getResolverInternal in the special
     * circumstance where getWinningValue has returned INHERITANCE_MARKER.
     */
    public T getBaileyValue() {
        if (organizationToValueAndVote == null
                || organizationToValueAndVote.baileySet == false) {
            throw new IllegalArgumentException("setBaileyValue must be called before getBaileyValue");
        }
        return organizationToValueAndVote.baileyValue;
    }

    /**
     * Set the Bailey value (what the inherited value would be if there were no explicit value).
     * This value is used in handling any {@link CldrUtility.INHERITANCE_MARKER}.
     * This value must be set <i>before</i> adding values. Usually by calling CLDRFile.getBaileyValue().
     */
    public void setBaileyValue(T baileyValue) {
        organizationToValueAndVote.baileySet = true;
        organizationToValueAndVote.baileyValue = baileyValue;
    }

    /**
     * Call once for each voter for a value. If there are no voters for an item, then call add(value);
     *
     * @param value
     * @param voter
     * @param withVotes override to lower the user's voting permission. May be null for default.
     * @param date
     *
     * Called by getResolverInternal
     */
    public void add(T value, int voter, Integer withVotes, Date date) {
        if (resolved) {
            throw new IllegalArgumentException("Must be called after clear, and before any getters.");
        }
        if (withVotes != null && withVotes == Level.LOCKING_VOTES) {
            valueIsLocked = true;
        }
        organizationToValueAndVote.add(value, voter, withVotes, date);
        values.add(value);
    }

    /**
     * Call once for each voter for a value. If there are no voters for an item, then call add(value);
     *
     * @param value
     * @param voter
     * @param withVotes override to lower the user's voting permission. May be null for default.
     *
     * Called only for TestUtilities, not used in Survey Tool.
     */
    public void add(T value, int voter, Integer withVotes) {
        if (resolved) {
            throw new IllegalArgumentException("Must be called after clear, and before any getters.");
        }
        Date date = new Date();
        organizationToValueAndVote.add(value, voter, withVotes, date);
        values.add(value);
    }

    /**
     * Used only in add(value, voter) for making a pseudo-Date
     */
    private int maxcounter = 100;

    /**
     * Call once for each voter for a value. If there are no voters for an item, then call add(value);
     *
     * @param value
     * @param voter
     *
     * Called by ConsoleCheckCLDR and TestUtilities; not used in SurveyTool.
     */
    public void add(T value, int voter) {
        Date date = new Date(++maxcounter);
        add(value, voter, null, date);
    }

    /**
     * Call if a value has no voters. It is safe to also call this if there is a voter, just unnecessary.
     *
     * @param value
     * @param voter
     *
     * Called by getResolverInternal for the baseline (trunk) value; also called for ConsoleCheckCLDR.
     */
    public void add(T value) {
        if (resolved) {
            throw new IllegalArgumentException("Must be called after clear, and before any getters.");
        }
        values.add(value);
    }

    private Set<T> values = new TreeSet<>(objectCollator);

    private final Comparator<T> votesThenUcaCollator = new Comparator<T>() {

        /**
         * Compare candidate items by vote count, highest vote first.
         * In the case of ties, favor (a) the baseline (trunk) value,
         * then (b) votes for inheritance (INHERITANCE_MARKER),
         * then (c) the alphabetical order (as a last resort).
         *
         * Return negative to favor o1, positive to favor o2.
         */
        @Override
        public int compare(T o1, T o2) {
            long v1 = organizationToValueAndVote.totalVotes.get(o1);
            long v2 = organizationToValueAndVote.totalVotes.get(o2);
            if (v1 != v2) {
                return v1 < v2 ? 1 : -1; // highest vote first
            }
            if (o1.equals(trunkValue)) {
                return -1;
            } else if (o2.equals(trunkValue)) {
                return 1;
            }
            if (o1.equals(CldrUtility.INHERITANCE_MARKER)) {
                return -1;
            } else if (o2.equals(CldrUtility.INHERITANCE_MARKER)) {
                return 1;
            }
            return englishCollator.compare(String.valueOf(o1), String.valueOf(o2));
        }
    };

    /**
     * This will be changed to true if both kinds of vote are present
     */
    private boolean bothInheritanceAndBaileyHadVotes = false;

    /**
     * Resolve the votes. Resolution entails counting votes and setting
     * members for this VoteResolver, including winningStatus, winningValue,
     * and many others.
     */
    private void resolveVotes() {
        resolved = true;
        // get the votes for each organization
        valuesWithSameVotes.clear();
        totals = organizationToValueAndVote.getTotals(conflictedOrganizations);
        /* Note: getKeysetSortedByCount actually returns a LinkedHashSet, "with predictable iteration order". */
        final Set<T> sortedValues = totals.getKeysetSortedByCount(false, votesThenUcaCollator);
        if (DEBUG) {
            System.out.println("sortedValues :" + sortedValues.toString());
        }

        /*
         * If there are no (unconflicted) votes, return baseline (trunk) if not null,
         * else INHERITANCE_MARKER if baileySet, else NO_WINNING_VALUE.
         * Avoid setting winningValue to null. VoteResolver should be fully in charge of vote resolution.
         * Note: formerly if trunkValue was null here, winningValue was set to null, such
         * as for http://localhost:8080/cldr-apps/v#/aa/Numbering_Systems/7b8ee7884f773afa
         * -- in spite of which the Survey Tool client displayed "latn" (bailey) in the Winning
         * column. The behavior was originally implemented on the client (JavaScript) and later
         * (temporarily) as fixWinningValue in DataSection.java.
         */
        if (sortedValues.size() == 0) {
            if (trunkValue != null) {
                winningValue = trunkValue;
                winningStatus = trunkStatus;
            } else if (organizationToValueAndVote.baileySet) {
                winningValue = (T) CldrUtility.INHERITANCE_MARKER;
                winningStatus = Status.missing;
            } else {
                /*
                 * TODO: When can this still happen? See https://unicode.org/cldr/trac/ticket/11299 "Example C".
                 * Also http://localhost:8080/cldr-apps/v#/en_CA/Gregorian/
                 * -- also http://localhost:8080/cldr-apps/v#/aa/Languages_A_D/
                 *    xpath //ldml/localeDisplayNames/languages/language[@type="zh_Hans"][@alt="long"]
                 * See also checkDataRowConsistency in DataSection.java.
                 */
                winningValue = (T) NO_WINNING_VALUE;
                winningStatus = Status.missing;
            }
            valuesWithSameVotes.add(winningValue);
            return;
        }
        if (values.size() == 0) {
            throw new IllegalArgumentException("No values added to resolver");
        }

        /*
         * Copy what is in the the totals field of this VoteResolver for all the
         * values in sortedValues. This local variable voteCount may be used
         * subsequently to make adjustments for vote resolution. Those adjustment
         * may affect the winners in vote resolution, while still preserving the original
         * voting data including the totals field.
         */
        HashMap<T, Long> voteCount = makeVoteCountMap(sortedValues);

        /*
         * Adjust sortedValues and voteCount as needed to combine "soft" votes for inheritance
         * with "hard" votes for the Bailey value. Note that sortedValues and voteCount are
         * both local variables.
         */
        bothInheritanceAndBaileyHadVotes = combineInheritanceWithBaileyForVoting(sortedValues, voteCount);

        /*
         * Adjust sortedValues and voteCount as needed for annotation keywords.
         */
        if (isUsingKeywordAnnotationVoting()) {
            adjustAnnotationVoteCounts(sortedValues, voteCount);
        }

        /*
         * Perform the actual resolution.
         */
        long weights[] = setBestNextAndSameVoteValues(sortedValues, voteCount);

        oValue = winningValue;

        winningStatus = computeStatus(weights[0], weights[1], trunkStatus);

        // if we are not as good as the trunk, use the trunk
        if (trunkStatus != null && winningStatus.compareTo(trunkStatus) < 0) {
            winningStatus = trunkStatus;
            winningValue = trunkValue;
            valuesWithSameVotes.clear();
            valuesWithSameVotes.add(winningValue);
        }
    }

    /**
     * Make a hash for the vote count of each value in the given sorted list, using
     * the totals field of this VoteResolver.
     *
     * This enables subsequent local adjustment of the effective votes, without change
     * to the totals field. Purposes include inheritance and annotation voting.
     *
     * @param sortedValues the sorted list of values (really a LinkedHashSet, "with predictable iteration order")
     * @return the HashMap
     */
    private HashMap<T, Long> makeVoteCountMap(Set<T> sortedValues) {
        HashMap<T, Long> map = new HashMap<>();
        for (T value : sortedValues) {
            map.put(value, totals.getCount(value));
        }
        return map;
    }

    /**
     * Adjust the given sortedValues and voteCount, if necessary, to combine "hard" and "soft" votes.
     * Do nothing unless both hard and soft votes are present.
     *
     * For voting resolution in which inheritance plays a role, "soft" votes for inheritance
     * are distinct from "hard" (explicit) votes for the Bailey value. For resolution, these two kinds
     * of votes are treated in combination. If that combination is winning, then the final winner will
     * be the hard item or the soft item, whichever has more votes, the soft item winning if they're tied.
     * Except for the soft item being favored as a tie-breaker, this function should be symmetrical in its
     * handling of hard and soft votes.
     *
     * Note: now that "↑↑↑" is permitted to participate directly in voting resolution, it becomes significant
     * that with Collator.getInstance(ULocale.ENGLISH), "↑↑↑" sorts before "AAA" just as "AAA" sorts before "BBB".
     *
     * @param sortedValues the set of sorted values, possibly to be modified
     * @param voteCount the hash giving the vote count for each value, possibly to be modified
     *
     * @return true if both "hard" and "soft" votes existed and were combined, else false
     */
    private boolean combineInheritanceWithBaileyForVoting(Set<T> sortedValues, HashMap<T, Long> voteCount) {
        if (organizationToValueAndVote == null
                || organizationToValueAndVote.baileySet == false
                || organizationToValueAndVote.baileyValue == null) {
            return false;
        }
        T hardValue = organizationToValueAndVote.baileyValue;
        T softValue = (T) CldrUtility.INHERITANCE_MARKER;
        /*
         * Check containsKey before get, to avoid NullPointerException.
         */
        if (!voteCount.containsKey(hardValue) || !voteCount.containsKey(softValue)) {
            return false;
        }
        long hardCount = voteCount.get(hardValue);
        long softCount = voteCount.get(softValue);
        if (hardCount == 0 || softCount == 0) {
            return false;
        }
        reallyCombineInheritanceWithBailey(sortedValues, voteCount, hardValue, softValue, hardCount, softCount);
        return true;
    }

    /**
     * Given that both "hard" and "soft" votes exist, combine them
     *
     * @param sortedValues the set of sorted values, to be modified
     * @param voteCount the hash giving the vote count for each value, to be modified
     * @param hardValue the bailey value
     * @param softValue the inheritance marker
     * @param hardCount the number of votes for hardValue
     * @param softCount the number of votes for softValue
     */
    private void reallyCombineInheritanceWithBailey(Set<T> sortedValues, HashMap<T, Long> voteCount,
            T hardValue, T softValue, long hardCount, long softCount) {
        final T combValue = (hardCount > softCount) ? hardValue : softValue;
        final T skipValue = (hardCount > softCount) ? softValue : hardValue;
        final long combinedCount = hardCount + softCount;
        voteCount.put(combValue, combinedCount);
        voteCount.put(skipValue, 0L);
        /*
         * Sort again
         */
        List<T> list = new ArrayList<>(sortedValues);
        Collections.sort(list, (v1, v2) -> {
            long c1 = voteCount.get(v1);
            long c2 = voteCount.get(v2);
            if (c1 != c2) {
                return (c1 < c2) ? 1 : -1; // decreasing numeric order (most votes wins)
            }
            return englishCollator.compare(String.valueOf(v1), String.valueOf(v2));
        });
        /*
         * Omit skipValue
         */
        sortedValues.clear();
        for (T value : list) {
            if (!value.equals(skipValue)) {
                sortedValues.add(value);
            }
        }
    }

    /**
     * Adjust the effective votes for bar-joined annotations,
     * and re-sort the array of values to reflect the adjusted vote counts.
     *
     * Note: "Annotations provide names and keywords for Unicode characters, currently focusing on emoji."
     * For example, an annotation "happy | joyful" has two components "happy" and "joyful".
     * References:
     *   http://unicode.org/cldr/charts/32/annotations/index.html
     *   http://unicode.org/repos/cldr/trunk/specs/ldml/tr35-general.html#Annotations
     *   http://unicode.org/repos/cldr/tags/latest/common/annotations/
     *
     * This function is where the essential algorithm needs to be implemented
     * for http://unicode.org/cldr/trac/ticket/10973
     *
     * @param sortedValues the set of sorted values
     * @param voteCount the hash giving the vote count for each value in sortedValues
     *
     * public for unit testing, see TestAnnotationVotes.java
     */
    public void adjustAnnotationVoteCounts(Set<T> sortedValues, HashMap<T, Long> voteCount) {
        if (voteCount == null || sortedValues == null) {
            return;
        }
        // Make compMap map individual components to cumulative vote counts.
        HashMap<T, Long> compMap = makeAnnotationComponentMap(sortedValues, voteCount);

        // Save a copy of the "raw" vote count before adjustment, since it's needed by promoteSuperiorAnnotationSuperset.
        HashMap<T, Long> rawVoteCount = new HashMap<>(voteCount);

        // Calculate new counts for original values, based on components.
        calculateNewCountsBasedOnAnnotationComponents(sortedValues, voteCount, compMap);

        // Re-sort sortedValues based on voteCount.
        resortValuesBasedOnAdjustedVoteCounts(sortedValues, voteCount);

        // If the set that so far is winning has supersets with superior raw vote count, promote the supersets.
        promoteSuperiorAnnotationSuperset(sortedValues, voteCount, rawVoteCount);
    }

    /**
     * Make a hash that maps individual annotation components to cumulative vote counts.
     *
     * For example, 3 votes for "a|b" and 2 votes for "a|c" makes 5 votes for "a", 3 for "b", and 2 for "c".
     *
     * @param sortedValues the set of sorted values
     * @param voteCount the hash giving the vote count for each value in sortedValues
     */
    private HashMap<T, Long> makeAnnotationComponentMap(Set<T> sortedValues, HashMap<T, Long> voteCount) {
        HashMap<T, Long> compMap = new HashMap<>();
        for (T value : sortedValues) {
            Long count = voteCount.get(value);
            List<T> comps = splitAnnotationIntoComponentsList(value);
            for (T comp : comps) {
                if (compMap.containsKey(comp)) {
                    compMap.replace(comp, compMap.get(comp) + count);
                }
                else {
                    compMap.put(comp, count);
                }
            }
        }
        if (DEBUG) {
            System.out.println("\n\tComponents in adjustAnnotationVoteCounts:");
            for (T comp : compMap.keySet()) {
                System.out.println("\t" + comp + ":" + compMap.get(comp));
            }
        }
        return compMap;
    }

    /**
     * Calculate new counts for original values, based on annotation components.
     *
     * Find the total votes for each component (e.g., "b" in "b|c"). As the "modified"
     * vote for the set, use the geometric mean of the components in the set.
     *
     * Order the sets by that mean value, then by the smallest number of items in
     * the set, then the fallback we always use (alphabetical).
     *
     * @param sortedValues the set of sorted values
     * @param voteCount the hash giving the vote count for each value in sortedValues
     * @param compMap the hash that maps individual components to cumulative vote counts
     *
     * See http://unicode.org/cldr/trac/ticket/10973
     */
    private void calculateNewCountsBasedOnAnnotationComponents(Set<T> sortedValues, HashMap<T, Long> voteCount, HashMap<T, Long> compMap) {
        voteCount.clear();
        for (T value : sortedValues) {
            List<T> comps = splitAnnotationIntoComponentsList(value);
            double product = 1.0;
            for (T comp : comps) {
                product *= compMap.get(comp);
            }
            /* Rounding to long integer here loses precision. We tried multiplying by ten before rounding,
             * to reduce problems with different doubles getting rounded to identical longs, but that had
             * unfortunate side-effects involving thresholds (see getRequiredVotes). An eventual improvement
             * may be to use doubles or floats for all vote counts.
             */
            Long newCount = Math.round(Math.pow(product, 1.0 / comps.size())); // geometric mean
            voteCount.put(value, newCount);
        }
    }

    /**
     * Split an annotation into a list of components.
     *
     * For example, split "happy | joyful" into ["happy", "joyful"].
     *
     * @param value the value like "happy | joyful"
     * @return the list like ["happy", "joyful"]
     *
     * Called by makeAnnotationComponentMap and calculateNewCountsBasedOnAnnotationComponents.
     * Short, but needs encapsulation, should be consistent with similar code in DisplayAndInputProcessor.java.
     */
    private List<T> splitAnnotationIntoComponentsList(T value) {
        return (List<T>) DisplayAndInputProcessor.SPLIT_BAR.splitToList((CharSequence) value);
    }

    /**
     * Re-sort the set of values to match the adjusted vote counts based on annotation components.
     *
     * Resolve ties using ULocale.ENGLISH collation for consistency with votesThenUcaCollator.
     *
     * @param sortedValues the set of sorted values, maybe no longer sorted the way we want
     * @param voteCount the hash giving the adjusted vote count for each value in sortedValues
     */
    private void resortValuesBasedOnAdjustedVoteCounts(Set<T> sortedValues, HashMap<T, Long> voteCount) {
        List<T> list = new ArrayList<>(sortedValues);
        Collections.sort(list, (v1, v2) -> {
            long c1 = voteCount.get(v1), c2 = voteCount.get(v2);
            if (c1 != c2) {
                return (c1 < c2) ? 1 : -1; // decreasing numeric order (most votes wins)
            }
            int size1 = splitAnnotationIntoComponentsList(v1).size();
            int size2 = splitAnnotationIntoComponentsList(v2).size();
            if (size1 != size2) {
                return (size1 < size2) ? -1 : 1; // increasing order of size (smallest set wins)
            }
            return englishCollator.compare(String.valueOf(v1), String.valueOf(v2));
        });
        sortedValues.clear();
        for (T value : list) {
            sortedValues.add(value);
        }
    }

    /**
     * For annotation votes, if the set that so far is winning has one or more supersets with "superior" (see
     * below) raw vote count, promote those supersets to become the new winner, and also the new second place
     * if there are two or more superior supersets.
     *
     * That is, after finding the set X with the largest geometric mean, check whether there are any supersets
     * with "superior" raw votes, and that don't exceed the width limit. If so, promote Y, the one of those
     * supersets with the most raw votes (using the normal tie breaker), to be the winning set.
     *
     * "Superior" here means that rawVote(Y) ≥ rawVote(X) + 2, where the value 2 (see requiredGap) is for the
     * purpose of requiring at least one non-guest vote.
     *
     * If any other "superior" supersets exist, promote to second place the one with the next most raw votes.
     *
     * Accomplish promotion by increasing vote counts in the voteCount hash.
     *
     * @param sortedValues the set of sorted values
     * @param voteCount the vote count for each value in sortedValues AFTER calculateNewCountsBasedOnAnnotationComponents;
     *             it gets modified if superior subsets exist
     * @param rawVoteCount the vote count for each value in sortedValues BEFORE calculateNewCountsBasedOnAnnotationComponents;
     *             rawVoteCount is not changed by this function
     *
     * Reference: https://unicode.org/cldr/trac/ticket/10973
     */
    private void promoteSuperiorAnnotationSuperset(Set<T> sortedValues, HashMap<T, Long> voteCount, HashMap<T, Long> rawVoteCount) {
        final long requiredGap = 2;
        T oldWinner = null;
        long oldWinnerRawCount = 0;
        LinkedHashSet<T> oldWinnerComps = null;
        LinkedHashSet<T> superiorSupersets = null;
        for (T value : sortedValues) {
            if (oldWinner == null) {
                oldWinner = value;
                oldWinnerRawCount = rawVoteCount.get(value);
                oldWinnerComps = new LinkedHashSet<>(splitAnnotationIntoComponentsList(value));
            } else {
                Set<T> comps = new LinkedHashSet<>(splitAnnotationIntoComponentsList(value));
                if (comps.size() <= CheckWidths.MAX_COMPONENTS_PER_ANNOTATION &&
                        comps.containsAll(oldWinnerComps) &&
                        rawVoteCount.get(value) >= oldWinnerRawCount + requiredGap) {
                    if (superiorSupersets == null) {
                        superiorSupersets = new LinkedHashSet<>();
                    }
                    superiorSupersets.add(value);
                }
            }
        }
        if (superiorSupersets != null) {
            // Sort the supersets by raw vote count, then make their adjusted vote counts higher than the old winner's.
            resortValuesBasedOnAdjustedVoteCounts(superiorSupersets, rawVoteCount);
            T newWinner = null, newSecond = null; // only adjust votes for first and second place
            for (T value : superiorSupersets) {
                if (newWinner == null) {
                    newWinner = value;
                    voteCount.put(newWinner, voteCount.get(oldWinner) + 2); // more than oldWinner and newSecond
                } else if (newSecond == null) { // TODO: fix redundant null check: newSecond can only be null
                    newSecond = value;
                    voteCount.put(newSecond, voteCount.get(oldWinner) + 1); // more than oldWinner, less than newWinner
                    break;
                }
            }
            resortValuesBasedOnAdjustedVoteCounts(sortedValues, voteCount);
        }
    }

    /**
     * Given a nonempty list of sorted values, and a hash with their vote counts, set these members
     * of this VoteResolver:
     *  winningValue, nValue, valuesWithSameVotes (which is empty when this function is called).
     *
     * @param sortedValues the set of sorted values
     * @param voteCount the hash giving the vote count for each value
     * @return an array of two longs, the weights for the best and next-best values.
     */
    private long[] setBestNextAndSameVoteValues(Set<T> sortedValues, HashMap<T, Long> voteCount) {

        long weightArray[] = new long[2];
        weightArray[0] = 0;
        weightArray[1] = 0;
        nValue = null;

        /*
         * Loop through the sorted values, at least the first (best) for winningValue,
         * and the second (if any) for nValue (else nValue stays null),
         * and subsequent values that have as many votes as the first,
         * to add to valuesWithSameVotes.
         */
        int i = -1;
        Iterator<T> iterator = sortedValues.iterator();
        for (T value : sortedValues) {
            ++i;
            long valueWeight = voteCount.get(value);
            if (i == 0) {
                winningValue = value;
                weightArray[0] = valueWeight;
                valuesWithSameVotes.add(value);
            } else {
                if (i == 1) {
                    // get the next item if there is one
                    if (iterator.hasNext()) {
                        nValue = value;
                        weightArray[1] = valueWeight;
                    }
                }
                if (valueWeight == weightArray[0]) {
                    valuesWithSameVotes.add(value);
                } else {
                    break;
                }
            }
        }
        return weightArray;
    }

    /**
     * Compute the status for the winning value.
     *
     * @param weight1 the weight (vote count) for the best value
     * @param weight2 the weight (vote count) for the next-best value
     * @param oldStatus the old status (trunkStatus)
     * @return the Status
     */
    private Status computeStatus(long weight1, long weight2, Status oldStatus) {
        if (weight1 > weight2 && weight1 >= requiredVotes) {
            return Status.approved;
        }
        if (weight1 > weight2 &&
            (weight1 >= 4 && Status.contributed.compareTo(oldStatus) > 0
                || weight1 >= 2 && organizationToValueAndVote.getOrgCount(winningValue) >= 2) ) {
            return Status.contributed;
        }
        if (weight1 >= weight2 && weight1 >= 2) {
            return Status.provisional;
        }
        return Status.unconfirmed;
    }

    private Status getPossibleWinningStatus() {
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
    private T getOValue() {
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
    private T getNValue() {
        if (!resolved) {
            resolveVotes();
        }
        return nValue;
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
        return new ArrayList<>(valuesWithSameVotes);
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

    public Map<String, Long> getNameTime() {
        return organizationToValueAndVote.getNameTime();
    }

    /**
     * Get a String representation of this VoteResolver.
     * This is sent to the client as "voteResolver.raw" and is used only for debugging.
     *
     * Compare SurveyAjax.JSONWriter.wrap(VoteResolver<String>) which creates the data
     * actually used by the client.
     */
    @Override
    public String toString() {
        return "{"
            + "bailey: " + (organizationToValueAndVote.baileySet ? ("“" + organizationToValueAndVote.baileyValue + "” ") : "none ")
            + "trunk: {" + trunkValue + ", " + trunkStatus + "}, "
            + organizationToValueAndVote
            + ", sameVotes: " + valuesWithSameVotes
            + ", O: " + getOValue()
            + ", N: " + getNValue()
            + ", totals: " + totals
            + ", winning: {" + getWinningValue() + ", " + getWinningStatus() + "}"
            + "}";
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
        localeToOrganizationToMaxVote = new TreeMap<>();
        for (int voter : getVoterToInfo().keySet()) {
            VoterInfo info = getVoterToInfo().get(voter);
            if (info.getLevel() == Level.tc || info.getLevel() == Level.locked) {
                continue; // skip TCs, locked
            }

            for (String locale : info.getLocales()) {
                Map<Organization, Level> organizationToMaxVote = localeToOrganizationToMaxVote.get(locale);
                if (organizationToMaxVote == null) {
                    localeToOrganizationToMaxVote.put(locale,
                        organizationToMaxVote = new TreeMap<>());
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
                    + ")",
                Pattern.COMMENTS);

        enum Group {
            all, userId, mainType, n, levelType, localeType, localeId;
            String get(Matcher matcher) {
                return matcher.group(this.ordinal());
            }
        }

        private static final boolean DEBUG_HANDLER = false;
        Map<Integer, VoterInfo> testVoterToInfo = new TreeMap<>();
        Matcher matcher = userPathMatcher.matcher("");

        @Override
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
        Map<Integer, String> pathIdToPath = new HashMap<>();

        @Override
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
    }

    public static class CandidateInfo {
        public Status oldStatus;
        public Type surveyType;
        public Status surveyStatus;
        public Set<Integer> voters = new TreeSet<>();

        @Override
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
        Map<Integer, Map<Integer, CandidateInfo>> basepathToInfo = new TreeMap<>();

        @Override
        public void handlePathValue(String path, String value) {
            try {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                if (parts.size() < 2) {
                    // empty data
                    return;
                }
                int baseId = Integer.parseInt(parts.getAttributeValue(1, "baseXpath"));
                Map<Integer, CandidateInfo> info = basepathToInfo.get(baseId);
                if (info == null) {
                    basepathToInfo.put(baseId, info = new TreeMap<>());
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
        Map<Organization, Level> orgToMaxVoteHere = new TreeMap<>();
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
        private static final long serialVersionUID = 3430877787936678609L;
        int voter;

        public UnknownVoterException(int voter) {
            this.voter = voter;
        }

        @Override
        public String toString() {
            return "Unknown voter: " + voter;
        }
    }

    private static String fixBogusDraftStatusValues(String attributeValue) {
        if (attributeValue == null) return "approved";
        if ("confirmed".equals(attributeValue)) return "approved";
        if ("true".equals(attributeValue)) return "unconfirmed";
        if ("unknown".equals(attributeValue)) return "unconfirmed";
        return attributeValue;
    }

    /*
     * TODO: either delete this or explain why it's needed
     */
    public int size() {
        return values.size();
    }

    /**
     * Returns a map from value to resolved vote count, in descending order.
     * If the winning item is not there, insert at the front.
     * If the baseline (trunk) item is not there, insert at the end.
     *
     * @return the map
     */
    public Map<T, Long> getResolvedVoteCounts() {
        if (!resolved) {
            resolveVotes();
        }
        Map<T, Long> result = new LinkedHashMap<>();
        if (winningValue != null && !totals.containsKey(winningValue)) {
            result.put(winningValue, 0L);
        }
        for (T value : totals.getKeysetSortedByCount(false, votesThenUcaCollator)) {
            result.put(value, totals.get(value));
        }
        if (trunkValue != null && !totals.containsKey(trunkValue)) {
            result.put(trunkValue, 0L);
        }
        for (T value : organizationToValueAndVote.totalVotes.getMap().keySet()) {
            if (!result.containsKey(value)) {
                result.put(value, 0L);
            }
        }
        if (DEBUG) {
            System.out.println("getResolvedVoteCounts :" + result.toString());
        }
        return result;
    }

    public VoteStatus getStatusForOrganization(Organization orgOfUser) {
        if (!resolved) {
            resolveVotes();
        }
        T orgVote = organizationToValueAndVote.getOrgVoteRaw(orgOfUser);
        if (!equalsOrgVote(winningValue, orgVote)) {
            // We voted and lost
            return VoteStatus.losing;
        }
        final int itemsWithVotes = countDistinctValuesWithVotes();
        if (itemsWithVotes > 1) {
            // If there are votes for two "distinct" items, we should look at them.
            return VoteStatus.disputed;
        }
        final T singleVotedItem = organizationToValueAndVote.getSingleVotedItem();
        if (!equalsOrgVote(winningValue, singleVotedItem)) {
            // If someone voted but didn't win
            return VoteStatus.disputed;
        }
        if (Status.provisional.compareTo(winningStatus) >= 0) {
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

    /**
     * Should these two values be treated as equivalent for getStatusForOrganization?
     *
     * @param value
     * @param orgVote
     * @return true if they are equivalent, false if they are distinct
     */
    private boolean equalsOrgVote(T value, T orgVote) {
        return orgVote == null
            || orgVote.equals(value)
            || (CldrUtility.INHERITANCE_MARKER.equals(value)
                && orgVote.equals(organizationToValueAndVote.baileyValue))
            || (CldrUtility.INHERITANCE_MARKER.equals(orgVote)
                && value.equals(organizationToValueAndVote.baileyValue));
    }

    /**
     * Count the distinct values that have votes.
     *
     * For this purpose, if there are both votes for inheritance and
     * votes for the specific value matching the inherited (bailey) value,
     * they are not "distinct": count them as a single value.
     *
     * @return the number of distinct values
     */
    private int countDistinctValuesWithVotes() {
        if (!resolved) { // must be resolved for bothInheritanceAndBaileyHadVotes
            throw new RuntimeException("countDistinctValuesWithVotes !resolved");
        }
        int count = organizationToValueAndVote.totalVotes.size();
        if (count > 1 && bothInheritanceAndBaileyHadVotes) {
            return count - 1; // prevent showing as "disputed" in dashboard
        }
        return count;
    }

    /**
     * Is this VoteResolver using keyword annotation voting?
     *
     * @return true or false
     */
    public boolean isUsingKeywordAnnotationVoting() {
        return usingKeywordAnnotationVoting;
    }

    /**
     * Set whether this VoteResolver should use keyword annotation voting.
     *
     * @param usingKeywordAnnotationVoting true or false
     */
    public void setUsingKeywordAnnotationVoting(boolean usingKeywordAnnotationVoting) {
        this.usingKeywordAnnotationVoting = usingKeywordAnnotationVoting;
    }

    /**
     * Is the value locked for this locale+path?
     *
     * @return true or false
     */
    public boolean isValueLocked() {
        return valueIsLocked;
    }

    /**
     * Can a user who makes a losing vote flag the locale+path?
     * I.e., is the locale+path locked and/or does it require HIGH_BAR votes?
     *
     * @return true or false
     */
    public boolean canFlagOnLosing() {
        return valueIsLocked || (requiredVotes == HIGH_BAR);
    }
}
