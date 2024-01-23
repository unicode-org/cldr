package org.unicode.cldr.util;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.Phase;
import org.unicode.cldr.test.CheckWidths;
import org.unicode.cldr.test.DisplayAndInputProcessor;

/**
 * This class implements the vote resolution process agreed to by the CLDR committee. Here is an
 * example of usage:
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
    public static final boolean DROP_HARD_INHERITANCE = true;

    private final VoterInfoList voterInfoList;

    public VoteResolver(VoterInfoList vil) {
        voterInfoList = vil;
    }

    private static final boolean DEBUG = false;

    /** This enables a prose discussion of the voting process. */
    private DeferredTranscript transcript = null;

    public void enableTranscript() {
        if (transcript == null) {
            transcript = new DeferredTranscript();
        }
    }

    public void disableTranscript() {
        transcript = null;
    }

    public String getTranscript() {
        if (transcript == null) {
            return null;
        } else {
            return transcript.get();
        }
    }

    /**
     * Add an annotation
     *
     * @param fmt
     * @param args
     */
    private final void annotateTranscript(String fmt, Object... args) {
        if (transcript != null) {
            transcript.add(fmt, args);
        }
    }

    /**
     * A placeholder for winningValue when it would otherwise be null. It must match
     * NO_WINNING_VALUE in the client JavaScript code.
     */
    private static final String NO_WINNING_VALUE = "no-winning-value";

    /**
     * The status levels according to the committee, in ascending order
     *
     * <p>Status corresponds to icons as follows: A checkmark means it’s approved and is slated to
     * be used. A cross means it’s a missing value. Green/orange check: The item has enough votes to
     * be used in CLDR. Red/orange/black X: The item does not have enough votes to be used in CLDR,
     * by most implementations (or is completely missing). Reference: <a
     * href="https://cldr.unicode.org/translation/getting-started/guide">guide</a>
     *
     * <p>When the item is inherited, i.e., winningValue is INHERITANCE_MARKER (↑↑↑), then
     * orange/red X are replaced by orange/red up-arrow. That change is made only on the client.
     *
     * <p>Status.approved: green check Status.contributed: orange check Status.provisional: orange X
     * (or orange up-arrow if inherited) Status.unconfirmed: red X (or red up-arrow if inherited
     * Status.missing: black X
     *
     * <p>Not to be confused with VoteResolver.VoteStatus
     */
    public enum Status {
        missing,
        unconfirmed,
        provisional,
        contributed,
        approved;

        public static Status fromString(String source) {
            return source == null ? missing : Status.valueOf(source);
        }
    }

    /**
     * This is the "high bar" level where flagging is required.
     *
     * @see #getRequiredVotes()
     */
    public static final int HIGH_BAR = Level.tc.votes;

    public static final int LOWER_BAR = (2 * Level.vetter.votes);

    /**
     * This is the level at which a vote counts. Each level also contains the weight.
     *
     * <p>Code related to Level.expert removed 2021-05-18 per CLDR-14597
     */
    public enum Level {
        locked(0 /* votes */, 999 /* stlevel */),
        guest(1 /* votes */, 10 /* stlevel */),
        anonymous(0 /* votes */, 8 /* stlevel */),
        vetter(4 /* votes */, 5 /* stlevel */, /* tcorgvotes */ 6), // org dependent- see getVotes()
        // Manager and below can manage users
        manager(4 /* votes */, 2 /* stlevel */),
        tc(50 /* votes */, 1 /* stlevel */),
        admin(100 /* votes */, 0 /* stlevel */);

        /**
         * PERMANENT_VOTES is used by TC voters to "lock" locale+path permanently (including future
         * versions, until unlocked), in the current VOTE_VALUE table. It is public for
         * STFactory.java and PermanentVote.java.
         */
        public static final int PERMANENT_VOTES = 1000;

        /**
         * LOCKING_VOTES is used (nominally by ADMIN voter, but not really by someone logged in as
         * ADMIN, instead by combination of two PERMANENT_VOTES) to "lock" locale+path permanently
         * in the LOCKED_XPATHS table. It is public for STFactory.PerLocaleData.loadVoteValues.
         */
        public static final int LOCKING_VOTES = 2000;

        /** The vote count a user of this level normally votes with */
        private final int votes;

        /** The vote count a user of this level normally votes with if a tc org */
        private final int tcorgvotes;

        /** The level as an integer, where 0 = admin, ..., 999 = locked */
        private final int stlevel;

        Level(int votes, int stlevel, int tcorgvotes) {
            this.votes = votes;
            this.stlevel = stlevel;
            this.tcorgvotes = tcorgvotes;
        }

        Level(int votes, int stlevel) {
            this(votes, stlevel, votes);
        }

        /**
         * Get the votes for each level and organization
         *
         * @param o the given organization
         */
        public int getVotes(Organization o) {
            if (this == vetter && o.isTCOrg()) {
                return tcorgvotes;
            }
            return votes;
        }

        /** Get the Survey Tool userlevel for each level. (0=admin, 999=locked) */
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
         * @param myOrg the current organization
         * @param otherLevel the other user's level
         * @param otherOrg the other user's organization
         * @return
         */
        public boolean isManagerFor(Organization myOrg, Level otherLevel, Organization otherOrg) {
            return (this == admin
                    || (canManageSomeUsers()
                            && (myOrg == otherOrg)
                            && atLeastAsPowerfulAs(otherLevel)));
        }

        /**
         * Policy: Can this user manage any users?
         *
         * @return
         */
        public boolean canManageSomeUsers() {
            return atLeastAsPowerfulAs(manager);
        }

        /** Internal: uses the ST Level as a measure of 'power' */
        boolean morePowerfulThan(Level other) {
            return getSTLevel() < other.getSTLevel();
        }

        /** Internal: uses the ST Level as a measure of 'power' */
        boolean atLeastAsPowerfulAs(Level other) {
            return getSTLevel() <= other.getSTLevel();
        }

        /**
         * Policy: can this user create or set a user to the specified level?
         *
         * @param otherLevel the desired new level for the other user
         *     <p>Note: UserRegistry.canSetUserLevel enforces additional limitations depending on
         *     more than this user's level and the other user's desired new level
         */
        public boolean canCreateOrSetLevelTo(Level otherLevel) {
            // Must be a manager at all
            if (!canManageSomeUsers()) return false;
            // Cannot elevate privilege
            return !otherLevel.morePowerfulThan(this);
        }

        /**
         * Can a user with this level and organization vote with the given vote count?
         *
         * @param org the given organization
         * @param withVotes the given vote count
         * @return true if the user can vote with the given vote count, else false
         */
        public boolean canVoteWithCount(Organization org, int withVotes) {
            /*
             * ADMIN is allowed to vote with LOCKING_VOTES, but not directly in the GUI, only
             * by two TC voting together with PERMANENT_VOTES. Therefore LOCKING_VOTES is omitted
             * from the GUI menu (voteCountMenu), but included in canVoteWithCount.
             */
            if (withVotes == LOCKING_VOTES && this == admin) {
                return true;
            }
            Set<Integer> menu = getVoteCountMenu(org);
            return menu == null ? withVotes == getVotes(org) : menu.contains(withVotes);
        }

        /**
         * If not null, an array of different vote counts from which a user of this level is allowed
         * to choose.
         */
        private ImmutableSet<Integer> voteCountMenu = null;

        /**
         * Get the ordered immutable set of different vote counts a user of this level can vote with
         *
         * @param ignoredOrg the given organization
         * @return the set, or null if the user has no choice of vote count
         */
        public ImmutableSet<Integer> getVoteCountMenu(Organization ignoredOrg) {
            // Right now, the organization does not affect the menu.
            // but update the API to future proof.
            return voteCountMenu;
        }

        /*
         * Set voteCountMenu for admin and tc in this static block, which will be run after
         * all the constructors have run, rather than in the constructor itself. For example,
         * vetter.votes needs to be defined before we can set admin.voteCountMenu.
         */
        static {
            admin.voteCountMenu =
                    ImmutableSet.of(
                            guest.votes,
                            vetter.votes,
                            vetter.tcorgvotes,
                            tc.votes,
                            admin.votes,
                            PERMANENT_VOTES);
            /* Not LOCKING_VOTES; see canVoteWithCount */
            tc.voteCountMenu =
                    ImmutableSet.of(
                            guest.votes,
                            vetter.votes,
                            vetter.tcorgvotes,
                            tc.votes,
                            PERMANENT_VOTES);
        }

        // The following methods were moved here from UserRegistry
        // TODO: remove this todo notice

        public boolean isAdmin() {
            return stlevel <= admin.stlevel;
        }

        public boolean isTC() {
            return stlevel <= tc.stlevel;
        }

        public boolean isExactlyManager() {
            return stlevel == manager.stlevel;
        }

        public boolean isManagerOrStronger() {
            return stlevel <= manager.stlevel;
        }

        public boolean isVetter() {
            return stlevel <= vetter.stlevel;
        }

        public boolean isGuest() {
            return stlevel <= guest.stlevel;
        }

        public boolean isLocked() {
            return stlevel == locked.stlevel;
        }

        public boolean isExactlyAnonymous() {
            return stlevel == anonymous.stlevel;
        }

        /**
         * Is this user an administrator 'over' this user? Always true if admin, or if TC in same
         * org.
         *
         * @param myOrg
         */
        public boolean isAdminForOrg(Organization myOrg, Organization target) {
            return isAdmin() || ((isTC() || stlevel == manager.stlevel) && (myOrg == target));
        }

        public boolean canImportOldVotes(CheckCLDR.Phase inPhase) {
            return isVetter() && (inPhase == Phase.SUBMISSION);
        }

        public boolean canDoList() {
            return isVetter();
        }

        public boolean canCreateUsers() {
            return isTC() || isExactlyManager();
        }

        public boolean canEmailUsers() {
            return isTC() || isExactlyManager();
        }

        public boolean canModifyUsers() {
            return isTC() || isExactlyManager();
        }

        public boolean canCreateOtherOrgs() {
            return isAdmin();
        }

        public boolean canUseVettingSummary() {
            return isManagerOrStronger();
        }

        public boolean canSubmit(CheckCLDR.Phase inPhase) {
            if (inPhase == Phase.FINAL_TESTING) {
                return false;
                // TODO: Note, this will mean not just READONLY, but VETTING_CLOSED will return
                // false here.
                // This is probably desired!
            }
            return isGuest();
        }

        public boolean canCreateSummarySnapshot() {
            return isAdmin();
        }

        public boolean canMonitorForum() {
            return isTC() || isExactlyManager();
        }

        public boolean canSetInterestLocales() {
            return isManagerOrStronger();
        }

        public boolean canGetEmailList() {
            return isManagerOrStronger();
        }

        /** If true, can delete users at their user level or lower. */
        public boolean canDeleteUsers() {
            return isAdmin();
        }
    }

    /**
     * See getStatusForOrganization to see how this is computed.
     *
     * <p>Not to be confused with VoteResolver.Status
     */
    public enum VoteStatus {
        /**
         * The value for the path is either contributed or approved, and the user's organization
         * didn't vote.
         */
        ok_novotes,

        /**
         * The value for the path is either contributed or approved, and the user's organization
         * chose the winning value.
         */
        ok,

        /** The winning value is neither contributed nor approved. */
        provisionalOrWorse,

        /**
         * The user's organization's choice is not winning, and the winning value is either
         * contributed or approved. There may be insufficient votes to overcome a previously
         * approved value, or other organizations may be voting against it.
         */
        losing,

        /**
         * There is a dispute, meaning more than one item with votes, or the item with votes didn't
         * win.
         */
        disputed
    }

    /** Internal class for voter information. It is public for testing only */
    public static class VoterInfo {
        private Organization organization;
        private Level level;
        private String name;
        /**
         * A set of locales associated with this voter; it is often empty (as when the user has "*"
         * for their set of locales); it may not serve any purpose in ordinary operation of Survey
         * Tool; its main (only?) purpose seems to be for computeMaxVotes, whose only purpose seems
         * to be creation of localeToOrganizationToMaxVote, which is used only by ConsoleCheckCLDR
         * (for obscure reason), not by Survey Tool itself.
         */
        private final Set<CLDRLocale> locales = new TreeSet<>();

        public Iterable<CLDRLocale> getLocales() {
            return locales;
        }

        public VoterInfo(Organization organization, Level level, String name, LocaleSet localeSet) {
            this.setOrganization(organization);
            this.setLevel(level);
            this.setName(name);
            if (!localeSet.isAllLocales()) {
                this.locales.addAll(localeSet.getSet());
            }
        }

        public VoterInfo(Organization organization, Level level, String name) {
            this.setOrganization(organization);
            this.setLevel(level);
            this.setName(name);
        }

        public VoterInfo() {}

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

        void addLocale(CLDRLocale locale) {
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
     * @param <T>
     */
    static class MaxCounter<T> extends Counter<T> {
        public MaxCounter(boolean b) {
            super(b);
        }

        /** Add, but only to bring up to the maximum value. */
        @Override
        public MaxCounter<T> add(T obj, long countValue, long time) {
            long value = getCount(obj);
            if ((value <= countValue)) {
                super.add(obj, countValue - value, time); // only add the difference!
            }
            return this;
        }
    }

    /** Internal class for getting from an organization to its vote. */
    private class OrganizationToValueAndVote<T> {
        private final Map<Organization, MaxCounter<T>> orgToVotes =
                new EnumMap<>(Organization.class);
        /**
         * All votes, even those that aren't any org's vote because they lost an intra-org dispute
         */
        private final Counter<T> allVotesIncludingIntraOrgDispute = new Counter<>();

        private final Map<Organization, Integer> orgToMax = new EnumMap<>(Organization.class);
        /** The result of {@link #getTotals(EnumSet)} */
        private final Counter<T> totals = new Counter<>(true);

        private final Map<String, Long> nameTime = new LinkedHashMap<>();

        /** map an organization to the value it voted for. */
        private final Map<Organization, T> orgToAdd = new EnumMap<>(Organization.class);

        private T baileyValue;
        private boolean baileySet; // was the bailey value set

        OrganizationToValueAndVote() {
            for (Organization org : Organization.values()) {
                orgToVotes.put(org, new MaxCounter<>(true));
            }
        }

        /** Call clear before considering each new path */
        public void clear() {
            for (Map.Entry<Organization, MaxCounter<T>> entry : orgToVotes.entrySet()) {
                entry.getValue().clear();
            }
            orgToAdd.clear();
            orgToMax.clear();
            allVotesIncludingIntraOrgDispute.clear();
            baileyValue = null;
            baileySet = false;
            if (transcript != null) {
                // there was a transcript before, so retain it
                transcript = new DeferredTranscript();
            }
        }

        public Map<String, Long> getNameTime() {
            return nameTime;
        }

        /**
         * Call this to add votes
         *
         * @param value
         * @param voter
         * @param withVotes optionally, vote at a non-typical voting level. May not exceed voter's
         *     maximum allowed level. null = use default level.
         * @param date
         */
        public void add(T value, int voter, Integer withVotes, Date date) {
            final VoterInfo info = voterInfoList.get(voter);
            if (info == null) {
                throw new UnknownVoterException(voter);
            }
            Level level = info.getLevel();
            if (withVotes == null || !level.canVoteWithCount(info.organization, withVotes)) {
                withVotes = level.getVotes(info.organization);
            }
            addInternal(value, info, withVotes, date); // do the add
        }

        /**
         * Called by add(T,int,Integer) to actually add a value.
         *
         * @param value
         * @param info
         * @param votes
         * @param time
         * @see #add(Object, int, Integer)
         */
        private void addInternal(T value, final VoterInfo info, final int votes, Date time) {
            if (DROP_HARD_INHERITANCE) {
                value = changeBaileyToInheritance(value);
            }
            /* All votes are added here, even if they will later lose an intra-org dispute. */
            allVotesIncludingIntraOrgDispute.add(value, votes, time.getTime());
            nameTime.put(info.getName(), time.getTime());
            if (DEBUG) {
                System.out.println(
                        "allVotesIncludingIntraOrgDispute Info: "
                                + allVotesIncludingIntraOrgDispute);
            }
            if (DEBUG) {
                System.out.println("VoteInfo: " + info.getName() + info.getOrganization());
            }
            Organization organization = info.getOrganization();
            orgToVotes.get(organization).add(value, votes, time.getTime());
            if (DEBUG) {
                System.out.println(
                        "Adding now Info: "
                                + organization.getDisplayName()
                                + info.getName()
                                + " is adding: "
                                + votes
                                + value
                                + new Timestamp(time.getTime()));
            }

            if (DEBUG) {
                System.out.println(
                        "addInternal: "
                                + organization.getDisplayName()
                                + " : "
                                + orgToVotes.get(organization).toString());
            }

            // add the new votes to orgToMax, if they are greater that what was there
            Integer max = orgToMax.get(info.getOrganization());
            if (max == null || max < votes) {
                orgToMax.put(organization, votes);
            }
        }

        /**
         * Return the overall vote for each organization. It is the max for each value. When the
         * organization is conflicted (the top two values have the same vote), the organization is
         * also added to disputed.
         *
         * @param conflictedOrganizations if not null, to be filled in with the set of conflicted
         *     organizations.
         */
        public Counter<T> getTotals(EnumSet<Organization> conflictedOrganizations) {
            if (conflictedOrganizations != null) {
                conflictedOrganizations.clear();
            }
            totals.clear();

            annotateTranscript("- Getting all totals by organization:");
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
                annotateTranscript(
                        "-- Considering %s which has %d item(s)",
                        entry.getKey().getDisplayName(), items.size());
                Organization org = entry.getKey();
                if (DEBUG) {
                    System.out.println("sortedKeys?? " + value + " " + org.getDisplayName());
                }
                // if there is more than one item, check that it is less
                if (iterator.hasNext()) {
                    T value2 = iterator.next();
                    long weight2 = items.getCount(value2);
                    // if the votes for #1 are not better than #2, we have a dispute
                    if (weight == weight2) {
                        if (conflictedOrganizations != null) {
                            annotateTranscript(
                                    "--- There are conflicts due to different values by users of this organization.");
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
                        System.out.println(
                                "Items in order: "
                                        + item.toString()
                                        + new Timestamp(items.getTime(item)));
                    }
                    long count = items.getCount(item);
                    long time = items.getTime(item);
                    if (count > maxCount) {
                        maxCount = count;
                        maxtime = time;
                        // tell the 'losing' item
                        if (considerItem != null) {
                            annotateTranscript(
                                    "---- Org is not voting for '%s': there is a higher ranked vote",
                                    considerItem);
                        }
                        considerItem = item;
                        if (DEBUG) {
                            System.out.println(
                                    "count>maxCount: "
                                            + considerItem
                                            + ":"
                                            + new Timestamp(considerTime)
                                            + " COUNT: "
                                            + considerCount
                                            + "MAXCOUNT: "
                                            + maxCount);
                        }
                        considerCount = items.getCount(considerItem);
                        considerTime = items.getTime(considerItem);
                    } else if ((time > maxtime) && (count == maxCount)) {
                        maxtime = time;
                        // tell the 'losing' item
                        if (considerItem != null) {
                            annotateTranscript(
                                    "---- Org is not voting for '%s': there is a later vote",
                                    considerItem);
                        }
                        considerItem = item;
                        considerCount = items.getCount(considerItem);
                        considerTime = items.getTime(considerItem);
                        if (DEBUG) {
                            System.out.println(
                                    "time>maxTime: "
                                            + considerItem
                                            + ":"
                                            + new Timestamp(considerTime));
                        }
                    }
                }
                annotateTranscript(
                        "--- %s vote is for '%s' with strength %d",
                        org.getDisplayName(), considerItem, considerCount);
                orgToAdd.put(org, considerItem);
                totals.add(considerItem, considerCount, considerTime);

                if (DEBUG) {
                    System.out.println("Totals: " + totals + " : " + new Timestamp(considerTime));
                }
            }

            if (DEBUG) {
                System.out.println("FINALTotals: " + totals);
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
                    orgToVotesString += org.toString() + "=" + counter;
                }
            }
            EnumSet<Organization> conflicted = EnumSet.noneOf(Organization.class);
            return "{orgToVotes: "
                    + orgToVotesString
                    + ", totals: "
                    + getTotals(conflicted)
                    + ", conflicted: "
                    + conflicted
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

    /** Data built internally */
    private T winningValue;

    private T oValue; // optimal value; winning if better approval status than old
    private T nValue; // next to optimal value
    private final List<T> valuesWithSameVotes = new ArrayList<>();
    private Counter<T> totals = null;

    private Status winningStatus;
    private final EnumSet<Organization> conflictedOrganizations =
            EnumSet.noneOf(Organization.class);
    private final OrganizationToValueAndVote<T> organizationToValueAndVote =
            new OrganizationToValueAndVote<>();
    private T baselineValue;
    private Status baselineStatus;

    private boolean resolved;
    private boolean valueIsLocked;
    private int requiredVotes = 0;
    private final SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
    private CLDRLocale locale;
    private PathHeader pathHeader;

    private static final Collator englishCollator = Collator.getInstance(ULocale.ENGLISH).freeze();

    /** Used for comparing objects of type T */
    private final Comparator<T> objectCollator =
            (o1, o2) -> englishCollator.compare(String.valueOf(o1), String.valueOf(o2));

    /**
     * Set the baseline (or "trunk") value and status for this VoteResolver.
     *
     * @param baselineValue the baseline value
     * @param baselineStatus the baseline status
     */
    public void setBaseline(T baselineValue, Status baselineStatus) {
        this.baselineValue = baselineValue;
        this.baselineStatus = baselineValue == null ? Status.missing : baselineStatus;
    }

    public T getBaselineValue() {
        return baselineValue;
    }

    public Status getBaselineStatus() {
        return baselineStatus;
    }

    /**
     * Set the locale and PathHeader for this VoteResolver
     *
     * <p>You must call this whenever you are using a VoteResolver with a new locale or a new
     * PathHeader
     *
     * @param locale the CLDRLocale
     * @param pathHeader the PathHeader
     */
    public void setLocale(CLDRLocale locale, PathHeader pathHeader) {
        this.locale = locale;
        this.pathHeader = pathHeader;
    }

    /**
     * What are the required votes for this item?
     *
     * @return the number of votes (as of this writing: usually 4, 8 for established locales)
     */
    public int getRequiredVotes() {
        if (requiredVotes == 0) {
            int preliminaryRequiredVotes =
                    supplementalDataInfo.getRequiredVotes(locale, pathHeader);
            if (preliminaryRequiredVotes == HIGH_BAR && baselineStatus != Status.approved) {
                requiredVotes = LOWER_BAR;
            } else {
                requiredVotes = preliminaryRequiredVotes;
            }
        }
        return requiredVotes;
    }

    /**
     * Call this method first, for a new base path. You'll then call add for each value associated
     * with that base path.
     */
    public void clear() {
        baselineValue = null;
        baselineStatus = Status.missing;
        requiredVotes = 0;
        locale = null;
        pathHeader = null;
        organizationToValueAndVote.clear();
        resolved = valueIsLocked = false;
        values.clear();

        // TODO: clear these out between reuse
        // Are there other values that should be cleared?
        oValue = null;
        setWinningValue(null);
        nValue = null;

        if (transcript != null) {
            transcript.clear();
        }
    }

    /**
     * Get the bailey value (what the inherited value would be if there were no explicit value) for
     * this VoteResolver.
     *
     * <p>Throw an exception if !baileySet.
     *
     * @return the bailey value.
     *     <p>Called by STFactory.PerLocaleData.getResolverInternal in the special circumstance
     *     where getWinningValue has returned INHERITANCE_MARKER.
     */
    public T getBaileyValue() {
        if (!organizationToValueAndVote.baileySet) {
            throw new IllegalArgumentException(
                    "setBaileyValue must be called before getBaileyValue");
        }
        return organizationToValueAndVote.baileyValue;
    }

    /**
     * Set the Bailey value (what the inherited value would be if there were no explicit value).
     * This value is used in handling any CldrUtility.INHERITANCE_MARKER. This value must be set
     * <i>before</i> adding values. Usually by calling CLDRFile.getBaileyValue().
     */
    public void setBaileyValue(T baileyValue) {
        organizationToValueAndVote.baileySet = true;
        organizationToValueAndVote.baileyValue = baileyValue;
    }

    /**
     * Call once for each voter for a value. If there are no voters for an item, then call
     * add(value);
     *
     * @param value
     * @param voter
     * @param withVotes override to lower the user's voting permission. May be null for default.
     * @param date
     *     <p>Called by getResolverInternal in STFactory, and elsewhere
     */
    public void add(T value, int voter, Integer withVotes, Date date) {
        if (DROP_HARD_INHERITANCE) {
            value = changeBaileyToInheritance(value);
        }
        if (resolved) {
            throw new IllegalArgumentException(
                    "Must be called after clear, and before any getters.");
        }
        if (withVotes != null && withVotes == Level.LOCKING_VOTES) {
            valueIsLocked = true;
        }
        organizationToValueAndVote.add(value, voter, withVotes, date);
        values.add(value);
    }

    /**
     * Call once for each voter for a value. If there are no voters for an item, then call
     * add(value);
     *
     * @param value
     * @param voter
     * @param withVotes override to lower the user's voting permission. May be null for default.
     *     <p>Called only for TestUtilities, not used in Survey Tool.
     */
    public void add(T value, int voter, Integer withVotes) {
        if (DROP_HARD_INHERITANCE) {
            value = changeBaileyToInheritance(value);
        }
        if (resolved) {
            throw new IllegalArgumentException(
                    "Must be called after clear, and before any getters.");
        }
        Date date = new Date();
        organizationToValueAndVote.add(value, voter, withVotes, date);
        values.add(value);
    }

    private <T> T changeBaileyToInheritance(T value) {
        if (value != null && value.equals(getBaileyValue())) {
            return (T) CldrUtility.INHERITANCE_MARKER;
        }
        return value;
    }

    /** Used only in add(value, voter) for making a pseudo-Date */
    private int maxcounter = 100;

    /**
     * Call once for each voter for a value. If there are no voters for an item, then call
     * add(value);
     *
     * @param value
     * @param voter
     *     <p>Called by ConsoleCheckCLDR and TestUtilities; not used in SurveyTool.
     */
    public void add(T value, int voter) {
        Date date = new Date(++maxcounter);
        add(value, voter, null, date);
    }

    /**
     * Call if a value has no voters. It is safe to also call this if there is a voter, just
     * unnecessary.
     *
     * @param value
     *     <p>Called by getResolverInternal for the baseline (trunk) value; also called for
     *     ConsoleCheckCLDR.
     */
    public void add(T value) {
        if (resolved) {
            throw new IllegalArgumentException(
                    "Must be called after clear, and before any getters.");
        }
        values.add(value);
    }

    private final Set<T> values = new TreeSet<>(objectCollator);

    private final Comparator<T> votesThenUcaCollator =
            new Comparator<>() {

                /**
                 * Compare candidate items by vote count, highest vote first. In the case of ties,
                 * favor (a) the baseline (trunk) value, then (b) votes for inheritance
                 * (INHERITANCE_MARKER), then (c) the alphabetical order (as a last resort).
                 *
                 * <p>Return negative to favor o1, positive to favor o2.
                 *
                 * @see VoteResolver#setBestNextAndSameVoteValues(Set, HashMap)
                 * @see VoteResolver#annotateNextBestValue(long, long, T, T)
                 */
                @Override
                public int compare(T o1, T o2) {
                    long v1 = organizationToValueAndVote.allVotesIncludingIntraOrgDispute.get(o1);
                    long v2 = organizationToValueAndVote.allVotesIncludingIntraOrgDispute.get(o2);
                    if (v1 != v2) {
                        return v1 < v2 ? 1 : -1; // highest vote first
                    }
                    if (o1.equals(baselineValue)) {
                        return -1;
                    } else if (o2.equals(baselineValue)) {
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
     * Annotate why the O (winning) value is winning vs the N (next) value. Assumes that the prior
     * annotation mentioned the O value.
     *
     * @param O optimal value
     * @param N next-best value
     */
    private void annotateNextBestValue(long O, long N, final T oValue, final T nValue) {
        // See the Comparator<> defined immediately above.

        // sortedValues.size() >= 2 - explain why O won and N lost.
        // We have to perform the function of the votesThenUcaCollator one more time
        if (O > N) {
            annotateTranscript(
                    "- This is the optimal value because it has the highest weight (voting score).");
        } else if (winningValue.equals(baselineValue)) {
            annotateTranscript(
                    "- This is the optimal value because it is the same as the baseline value, though the weight was otherwise equal to the next-best."); // aka blue star
        } else if (winningValue.equals(CldrUtility.INHERITANCE_MARKER)) {
            annotateTranscript(
                    "- This is the optimal value because it is the inheritance marker, though the weight was otherwise equal to the next-best."); // triple up arrow
        } else {
            annotateTranscript(
                    "- This is the optimal value because it comes earlier than '%s' when the text was sorted, though the weight was otherwise equal to the next-best.",
                    nValue);
        }
        annotateTranscript("The Next-best (N) value is '%s', with weight %d", nValue, N);
    }

    /** This will be changed to true if both kinds of vote are present */
    private boolean bothInheritanceAndBaileyHadVotes = false;

    /**
     * Resolve the votes. Resolution entails counting votes and setting members for this
     * VoteResolver, including winningStatus, winningValue, and many others.
     */
    private void resolveVotes() {
        annotateTranscript("Resolving votes:");
        resolved = true;
        // get the votes for each organization
        valuesWithSameVotes.clear();
        totals = organizationToValueAndVote.getTotals(conflictedOrganizations);
        /* Note: getKeysetSortedByCount actually returns a LinkedHashSet, "with predictable iteration order". */
        final Set<T> sortedValues = totals.getKeysetSortedByCount(false, votesThenUcaCollator);
        if (DEBUG) {
            System.out.println("sortedValues :" + sortedValues.toString());
        }
        // annotateTranscript("all votes by org: %s", sortedValues);

        /*
         * If there are no (unconflicted) votes, return baseline (trunk) if not null,
         * else INHERITANCE_MARKER if baileySet, else NO_WINNING_VALUE.
         * Avoid setting winningValue to null. VoteResolver should be fully in charge of vote resolution.
         */
        if (sortedValues.size() == 0) {
            if (baselineValue != null) {
                setWinningValue(baselineValue);
                winningStatus = baselineStatus;
                annotateTranscript(
                        "Winning Value: '%s' with status '%s' because there were no unconflicted votes.",
                        winningValue, winningStatus);
                // Declare the winner here, because we're about to return from the function
            } else if (organizationToValueAndVote.baileySet) {
                setWinningValue((T) CldrUtility.INHERITANCE_MARKER);
                winningStatus = Status.missing;
                annotateTranscript(
                        "Winning Value: '%s' with status '%s' because there were no unconflicted votes, and there was a Bailey value set.",
                        winningValue, winningStatus);
                // Declare the winner here, because we're about to return from the function
            } else {
                /*
                 * TODO: When can this still happen? See https://unicode.org/cldr/trac/ticket/11299 "Example C".
                 * Also http://localhost:8080/cldr-apps/v#/en_CA/Gregorian/
                 * -- also http://localhost:8080/cldr-apps/v#/aa/Languages_A_D/
                 *    xpath //ldml/localeDisplayNames/languages/language[@type="zh_Hans"][@alt="long"]
                 * See also checkDataRowConsistency in DataSection.java.
                 */
                setWinningValue((T) NO_WINNING_VALUE);
                winningStatus = Status.missing;
                annotateTranscript(
                        "No winning value! status '%s' because there were no unconflicted votes",
                        winningStatus);
                // Declare the non-winner here, because we're about to return from the function
            }
            valuesWithSameVotes.add(winningValue);
            return; // sortedValues.size() == 0, no candidates
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
        if (!DROP_HARD_INHERITANCE) {
            bothInheritanceAndBaileyHadVotes =
                    combineInheritanceWithBaileyForVoting(sortedValues, voteCount);
        }

        /*
         * Adjust sortedValues and voteCount as needed for annotation keywords.
         */
        if (isUsingKeywordAnnotationVoting()) {
            adjustAnnotationVoteCounts(sortedValues, voteCount);
        }

        /*
         * Perform the actual resolution.
         * This sets winningValue to the top element of
         * sortedValues.
         */
        long[] weights = setBestNextAndSameVoteValues(sortedValues, voteCount);

        oValue = winningValue;

        winningStatus = computeStatus(weights[0], weights[1]);

        // if we are not as good as the baseline (trunk), use the baseline
        // TODO: how could baselineStatus be null here??
        if (baselineStatus != null && winningStatus.compareTo(baselineStatus) < 0) {
            setWinningValue(baselineValue);
            annotateTranscript(
                    "The optimal value so far with status '%s' would not be as good as the baseline status. "
                            + "Therefore, the winning value is '%s' with status '%s'.",
                    winningStatus, winningValue, baselineStatus);
            winningStatus = baselineStatus;
            valuesWithSameVotes.clear();
            valuesWithSameVotes.add(winningValue);
        } else {
            // Declare the final winner
            annotateTranscript(
                    "The winning value is '%s' with status '%s'.", winningValue, winningStatus);
        }
    }

    /**
     * Make a hash for the vote count of each value in the given sorted list, using the totals field
     * of this VoteResolver.
     *
     * <p>This enables subsequent local adjustment of the effective votes, without change to the
     * totals field. Purposes include inheritance and annotation voting.
     *
     * @param sortedValues the sorted list of values (really a LinkedHashSet, "with predictable
     *     iteration order")
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
     * Adjust the given sortedValues and voteCount, if necessary, to combine "hard" and "soft"
     * votes. Do nothing unless both hard and soft votes are present.
     *
     * <p>For voting resolution in which inheritance plays a role, "soft" votes for inheritance are
     * distinct from "hard" (explicit) votes for the Bailey value. For resolution, these two kinds
     * of votes are treated in combination. If that combination is winning, then the final winner
     * will be the hard item or the soft item, whichever has more votes, the soft item winning if
     * they're tied. Except for the soft item being favored as a tie-breaker, this function should
     * be symmetrical in its handling of hard and soft votes.
     *
     * <p>Note: now that "↑↑↑" is permitted to participate directly in voting resolution, it becomes
     * significant that with Collator.getInstance(ULocale.ENGLISH), "↑↑↑" sorts before "AAA" just as
     * "AAA" sorts before "BBB".
     *
     * @param sortedValues the set of sorted values, possibly to be modified
     * @param voteCount the hash giving the vote count for each value, possibly to be modified
     * @return true if both "hard" and "soft" votes existed and were combined, else false
     */
    private boolean combineInheritanceWithBaileyForVoting(
            Set<T> sortedValues, HashMap<T, Long> voteCount) {
        if (organizationToValueAndVote.baileySet == false
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
        reallyCombineInheritanceWithBailey(
                sortedValues, voteCount, hardValue, softValue, hardCount, softCount);
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
    private void reallyCombineInheritanceWithBailey(
            Set<T> sortedValues,
            HashMap<T, Long> voteCount,
            T hardValue,
            T softValue,
            long hardCount,
            long softCount) {
        final T combValue = (hardCount > softCount) ? hardValue : softValue;
        final T skipValue = (hardCount > softCount) ? softValue : hardValue;
        final long combinedCount = hardCount + softCount;
        voteCount.put(combValue, combinedCount);
        voteCount.put(skipValue, 0L);
        /*
         * Sort again
         */
        List<T> list = new ArrayList<>(sortedValues);
        list.sort(
                (v1, v2) -> {
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
     * Adjust the effective votes for bar-joined annotations, and re-sort the array of values to
     * reflect the adjusted vote counts.
     *
     * <p>Note: "Annotations provide names and keywords for Unicode characters, currently focusing
     * on emoji." For example, an annotation "happy | joyful" has two components "happy" and
     * "joyful". References: http://unicode.org/cldr/charts/32/annotations/index.html
     * https://www.unicode.org/reports/tr35/tr35-general.html#Annotations
     *
     * <p>http://unicode.org/repos/cldr/tags/latest/common/annotations/
     *
     * @param sortedValues the set of sorted values
     * @param voteCount the hash giving the vote count for each value in sortedValues
     *     <p>public for unit testing, see TestAnnotationVotes.java
     */
    public void adjustAnnotationVoteCounts(Set<T> sortedValues, HashMap<T, Long> voteCount) {
        if (voteCount == null || sortedValues == null) {
            return;
        }
        annotateTranscript("Vote weights are being adjusted due to annotation keywords.");

        // Make compMap map individual components to cumulative vote counts.
        HashMap<T, Long> compMap = makeAnnotationComponentMap(sortedValues, voteCount);

        // Save a copy of the "raw" vote count before adjustment, since it's needed by
        // promoteSuperiorAnnotationSuperset.
        HashMap<T, Long> rawVoteCount = new HashMap<>(voteCount);

        // Calculate new counts for original values, based on components.
        calculateNewCountsBasedOnAnnotationComponents(sortedValues, voteCount, compMap);

        // Re-sort sortedValues based on voteCount.
        resortValuesBasedOnAdjustedVoteCounts(sortedValues, voteCount);

        // If the set that so far is winning has supersets with superior raw vote count, promote the
        // supersets.
        promoteSuperiorAnnotationSuperset(sortedValues, voteCount, rawVoteCount);
    }

    /**
     * Make a hash that maps individual annotation components to cumulative vote counts.
     *
     * <p>For example, 3 votes for "a|b" and 2 votes for "a|c" makes 5 votes for "a", 3 for "b", and
     * 2 for "c".
     *
     * @param sortedValues the set of sorted values
     * @param voteCount the hash giving the vote count for each value in sortedValues
     */
    private HashMap<T, Long> makeAnnotationComponentMap(
            Set<T> sortedValues, HashMap<T, Long> voteCount) {
        HashMap<T, Long> compMap = new HashMap<>();
        annotateTranscript("- First, components are split up and total votes calculated");
        for (T value : sortedValues) {
            Long count = voteCount.get(value);
            List<T> comps = splitAnnotationIntoComponentsList(value);
            for (T comp : comps) {
                if (compMap.containsKey(comp)) {
                    compMap.replace(comp, compMap.get(comp) + count);
                } else {
                    compMap.put(comp, count);
                }
            }
        }
        if (transcript != null && !DEBUG) {
            for (Entry<T, Long> comp : compMap.entrySet()) {
                // TODO: could sort here, or not.
                annotateTranscript(
                        "-- component '%s' has weight %d",
                        comp.getKey().toString(), comp.getValue());
            }
        }
        return compMap;
    }

    /**
     * Calculate new counts for original values, based on annotation components.
     *
     * <p>Find the total votes for each component (e.g., "b" in "b|c"). As the "modified" vote for
     * the set, use the geometric mean of the components in the set.
     *
     * <p>Order the sets by that mean value, then by the smallest number of items in the set, then
     * the fallback we always use (alphabetical).
     *
     * @param sortedValues the set of sorted values
     * @param voteCount the hash giving the vote count for each value in sortedValues
     * @param compMap the hash that maps individual components to cumulative vote counts
     *     <p>See http://unicode.org/cldr/trac/ticket/10973
     */
    private void calculateNewCountsBasedOnAnnotationComponents(
            Set<T> sortedValues, HashMap<T, Long> voteCount, HashMap<T, Long> compMap) {
        voteCount.clear();
        annotateTranscript(
                "- Next, the original values get new counts, each based on the geometric mean of the products of all components.");
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
            // Don't annotate these here, annotate them once sorted
        }
    }

    /**
     * Split an annotation into a list of components.
     *
     * <p>For example, split "happy | joyful" into ["happy", "joyful"].
     *
     * @param value the value like "happy | joyful"
     * @return the list like ["happy", "joyful"]
     *     <p>Called by makeAnnotationComponentMap and
     *     calculateNewCountsBasedOnAnnotationComponents. Short, but needs encapsulation, should be
     *     consistent with similar code in DisplayAndInputProcessor.java.
     */
    private List<T> splitAnnotationIntoComponentsList(T value) {
        return (List<T>) DisplayAndInputProcessor.SPLIT_BAR.splitToList((CharSequence) value);
    }

    /**
     * Re-sort the set of values to match the adjusted vote counts based on annotation components.
     *
     * <p>Resolve ties using ULocale.ENGLISH collation for consistency with votesThenUcaCollator.
     *
     * @param sortedValues the set of sorted values, maybe no longer sorted the way we want
     * @param voteCount the hash giving the adjusted vote count for each value in sortedValues
     */
    private void resortValuesBasedOnAdjustedVoteCounts(
            Set<T> sortedValues, HashMap<T, Long> voteCount) {
        List<T> list = new ArrayList<>(sortedValues);
        list.sort(
                (v1, v2) -> {
                    long c1 = voteCount.get(v1), c2 = voteCount.get(v2);
                    if (c1 != c2) {
                        return (c1 < c2) ? 1 : -1; // decreasing numeric order (most votes wins)
                    }
                    int size1 = splitAnnotationIntoComponentsList(v1).size();
                    int size2 = splitAnnotationIntoComponentsList(v2).size();
                    if (size1 != size2) {
                        return (size1 < size2)
                                ? -1
                                : 1; // increasing order of size (smallest set wins)
                    }
                    return englishCollator.compare(String.valueOf(v1), String.valueOf(v2));
                });
        sortedValues.clear();
        sortedValues.addAll(list);
    }

    /**
     * For annotation votes, if the set that so far is winning has one or more supersets with
     * "superior" (see below) raw vote count, promote those supersets to become the new winner, and
     * also the new second place if there are two or more superior supersets.
     *
     * <p>That is, after finding the set X with the largest geometric mean, check whether there are
     * any supersets with "superior" raw votes, and that don't exceed the width limit. If so,
     * promote Y, the one of those supersets with the most raw votes (using the normal tie breaker),
     * to be the winning set.
     *
     * <p>"Superior" here means that rawVote(Y) ≥ rawVote(X) + 2, where the value 2 (see
     * requiredGap) is for the purpose of requiring at least one non-guest vote.
     *
     * <p>If any other "superior" supersets exist, promote to second place the one with the next
     * most raw votes.
     *
     * <p>Accomplish promotion by increasing vote counts in the voteCount hash.
     *
     * @param sortedValues the set of sorted values
     * @param voteCount the vote count for each value in sortedValues AFTER
     *     calculateNewCountsBasedOnAnnotationComponents; it gets modified if superior subsets exist
     * @param rawVoteCount the vote count for each value in sortedValues BEFORE
     *     calculateNewCountsBasedOnAnnotationComponents; rawVoteCount is not changed by this
     *     function
     *     <p>Reference: https://unicode.org/cldr/trac/ticket/10973
     */
    private void promoteSuperiorAnnotationSuperset(
            Set<T> sortedValues, HashMap<T, Long> voteCount, HashMap<T, Long> rawVoteCount) {
        final long requiredGap = 2;
        T oldWinner = null;
        long oldWinnerRawCount = 0;
        LinkedHashSet<T> oldWinnerComps = null;
        LinkedHashSet<T> superiorSupersets = null;
        for (T value : sortedValues) {
            // Annotate the means here
            final long rawCount = rawVoteCount.get(value);
            final long newCount = voteCount.get(value);
            if (rawCount != newCount) {
                annotateTranscript("-- Value '%s' has updated value '%d'", value, newCount);
            }
            if (oldWinner == null) {
                oldWinner = value;
                oldWinnerRawCount = rawVoteCount.get(value);
                oldWinnerComps = new LinkedHashSet<>(splitAnnotationIntoComponentsList(value));
            } else {
                Set<T> comps = new LinkedHashSet<>(splitAnnotationIntoComponentsList(value));
                if (comps.size() <= CheckWidths.MAX_COMPONENTS_PER_ANNOTATION
                        && comps.containsAll(oldWinnerComps)
                        && rawVoteCount.get(value) >= oldWinnerRawCount + requiredGap) {
                    if (superiorSupersets == null) {
                        superiorSupersets = new LinkedHashSet<>();
                    }
                    superiorSupersets.add(value);
                }
            }
        }
        if (superiorSupersets != null) {
            // Sort the supersets by raw vote count, then make their adjusted vote counts higher
            // than the old winner's.
            resortValuesBasedOnAdjustedVoteCounts(superiorSupersets, rawVoteCount);
            T newWinner = null, newSecond; // only adjust votes for first and second place
            for (T value : superiorSupersets) {
                if (newWinner == null) {
                    newWinner = value;
                    long newWinnerCount = voteCount.get(oldWinner) + 2;
                    annotateTranscript(
                            "- Optimal value (O) '%s' was promoted to value '%d' due to having a superior raw vote count",
                            newWinner, newWinnerCount);
                    voteCount.put(newWinner, newWinnerCount); // more than oldWinner and newSecond
                } else {
                    newSecond = value;
                    long newSecondCount = voteCount.get(oldWinner) + 1;
                    annotateTranscript(
                            "- Next value (N) '%s' was promoted to value '%d' due to having a superior raw vote count",
                            newSecond, newSecondCount);
                    voteCount.put(
                            newSecond, newSecondCount); // more than oldWinner, less than newWinner
                    break;
                }
            }
            resortValuesBasedOnAdjustedVoteCounts(sortedValues, voteCount);
        }
    }

    /**
     * Given a nonempty list of sorted values, and a hash with their vote counts, set these members
     * of this VoteResolver: winningValue, nValue, valuesWithSameVotes (which is empty when this
     * function is called).
     *
     * @param sortedValues the set of sorted values
     * @param voteCount the hash giving the vote count for each value
     * @return an array of two longs, the weights for the best and next-best values.
     */
    private long[] setBestNextAndSameVoteValues(Set<T> sortedValues, HashMap<T, Long> voteCount) {

        long[] weightArray = new long[2];
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
                setWinningValue(value);
                weightArray[0] = valueWeight;
                valuesWithSameVotes.add(value);
                annotateTranscript(
                        "The optimal value (O) is '%s', with a weight of %d",
                        winningValue, valueWeight);
                if (sortedValues.size() == 1) {
                    annotateTranscript("- No other values received votes."); // uncontested
                }
            } else {
                if (i == 1) {
                    // get the next item if there is one
                    if (iterator.hasNext()) {
                        nValue = value;
                        weightArray[1] = valueWeight;
                        annotateNextBestValue(weightArray[0], weightArray[1], winningValue, nValue);
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
     * Compute the status for the winning value. See: https://cldr.unicode.org/index/process
     *
     * @param O the weight (vote count) for the best value
     * @param N the weight (vote count) for the next-best value
     * @return the Status
     */
    private Status computeStatus(long O, long N) {
        if (O > N) {
            final int requiredVotes = getRequiredVotes();
            if (O >= requiredVotes) {
                final Status computedStatus = Status.approved;
                annotateTranscript("O>N, and O>%d: %s", requiredVotes, computedStatus);
                return computedStatus;
            }
            if (O >= 4 && Status.contributed.compareTo(baselineStatus) > 0) {
                final Status computedStatus = Status.contributed;
                annotateTranscript(
                        "O>=4, and oldstatus (%s)<contributed: %s", baselineStatus, computedStatus);
                return computedStatus;
            }
            if (O >= 2) {
                final int G = organizationToValueAndVote.getOrgCount(winningValue);
                if (G >= 2) {
                    final Status computedStatus = Status.contributed;
                    annotateTranscript("O>=2, and G (%d)>=2: %s", G, computedStatus);
                    return computedStatus;
                }
            }
        }
        if (O >= N) {
            if (O >= 2) {
                final Status computedStatus = Status.provisional;
                annotateTranscript("O>=N and O>=2: %s", computedStatus);
                return computedStatus;
            }
        }

        // otherwise: unconfirmed
        final Status computedStatus = Status.unconfirmed;
        annotateTranscript("O was not high enough: %s", computedStatus);
        return computedStatus;
    }

    private Status getPossibleWinningStatus() {
        if (!resolved) {
            resolveVotes();
        }
        Status possibleStatus = computeStatus(organizationToValueAndVote.getBestPossibleVote(), 0);
        return possibleStatus.compareTo(winningStatus) > 0 ? possibleStatus : winningStatus;
    }

    /**
     * If the winning item is not approved, and if all the people who voted had voted for the
     * winning item, would it have made contributed or approved?
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
        return possibleStatus.compareTo(Status.contributed) >= 0;
    }

    public Status getWinningStatus() {
        if (!resolved) {
            resolveVotes();
        }
        return winningStatus;
    }

    /**
     * Returns O Value as described in http://cldr.unicode.org/index/process#TOC-Voting-Process. Not
     * always the same as the Winning Value.
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
     * Returns N Value as described in http://cldr.unicode.org/index/process#TOC-Voting-Process. Not
     * always the same as the Winning Value.
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
     * Returns Winning Value as described in
     * http://cldr.unicode.org/index/process#TOC-Voting-Process. Not always the same as the O Value.
     *
     * @return
     */
    public T getWinningValue() {
        if (!resolved) {
            resolveVotes();
        }
        return winningValue;
    }

    /**
     * Set the Winning Value; if the given value matches Bailey, change it to INHERITANCE_MARKER
     *
     * @param value the value to set (prior to changeBaileyToInheritance)
     */
    private void setWinningValue(T value) {
        if (DROP_HARD_INHERITANCE) {
            winningValue = changeBaileyToInheritance(value);
        } else {
            winningValue = value;
        }
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
     * Get a String representation of this VoteResolver. This is sent to the client as
     * "voteResolver.raw" and is used only for debugging.
     *
     * <p>Compare SurveyAjax.JSONWriter.wrap(VoteResolver<String>) which creates the data actually
     * used by the client.
     */
    @Override
    public String toString() {
        return "{"
                + "bailey: "
                + (organizationToValueAndVote.baileySet
                        ? ("“" + organizationToValueAndVote.baileyValue + "” ")
                        : "none ")
                + "baseline: {"
                + baselineValue
                + ", "
                + baselineStatus
                + "}, "
                + organizationToValueAndVote
                + ", sameVotes: "
                + valuesWithSameVotes
                + ", O: "
                + getOValue()
                + ", N: "
                + getNValue()
                + ", totals: "
                + totals
                + ", winning: {"
                + getWinningValue()
                + ", "
                + getWinningStatus()
                + "}"
                + "}";
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

    public static Map<Integer, Map<Integer, CandidateInfo>> getBaseToAlternateToInfo(
            String fileName, VoterInfoList vil) {
        try {
            VotesHandler myHandler = new VotesHandler(vil);
            XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
            xfr.read(fileName, XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER, false);
            return myHandler.basepathToInfo;
        } catch (Exception e) {
            throw new IllegalArgumentException("Can't handle file: " + fileName, e);
        }
    }

    public enum Type {
        proposal,
        optimal
    }

    public static class CandidateInfo {
        public Status oldStatus;
        public Type surveyType;
        public Status surveyStatus;
        public Set<Integer> voters = new TreeSet<>();
        private final VoterInfoList voterInfoList;

        CandidateInfo(VoterInfoList vil) {
            this.voterInfoList = vil;
        }

        @Override
        public String toString() {
            StringBuilder voterString = new StringBuilder("{");
            for (int voter : voters) {
                VoterInfo voterInfo = voterInfoList.get(voter);
                if (voterString.length() > 1) {
                    voterString.append(" ");
                }
                voterString.append(voter);
                if (voterInfo != null) {
                    voterString.append(" ").append(voterInfo);
                }
            }
            voterString.append("}");
            return "{oldStatus: "
                    + oldStatus
                    + ", surveyType: "
                    + surveyType
                    + ", surveyStatus: "
                    + surveyStatus
                    + ", voters: "
                    + voterString
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
        private final VoterInfoList voterInfoList;

        VotesHandler(VoterInfoList vil) {
            this.voterInfoList = vil;
        }

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
                Map<Integer, CandidateInfo> info =
                        basepathToInfo.computeIfAbsent(baseId, k -> new TreeMap<>());
                int itemId = Integer.parseInt(parts.getAttributeValue(2, "xpath"));
                CandidateInfo candidateInfo = info.get(itemId);
                if (candidateInfo == null) {
                    info.put(itemId, candidateInfo = new CandidateInfo(voterInfoList));
                    candidateInfo.surveyType = Type.valueOf(parts.getAttributeValue(2, "type"));
                    candidateInfo.surveyStatus =
                            Status.valueOf(
                                    fixBogusDraftStatusValues(
                                            parts.getAttributeValue(2, "status")));
                    // ignore id
                }
                if (parts.size() < 4) {
                    return;
                }
                final String lastElement = parts.getElement(3);
                if (lastElement.equals("old")) {
                    candidateInfo.oldStatus =
                            Status.valueOf(
                                    fixBogusDraftStatusValues(
                                            parts.getAttributeValue(3, "status")));
                } else if (lastElement.equals("vote")) {
                    candidateInfo.voters.add(Integer.parseInt(parts.getAttributeValue(3, "user")));
                } else {
                    throw new IllegalArgumentException("unknown option: " + path);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Can't handle path: " + path, e);
            }
        }
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
     * Returns a map from value to resolved vote count, in descending order. If the winning item is
     * not there, insert at the front. If the baseline (trunk) item is not there, insert at the end.
     *
     * <p>This map includes intra-org disputes.
     *
     * @return the map
     */
    public Map<T, Long> getResolvedVoteCountsIncludingIntraOrgDisputes() {
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
        if (baselineValue != null && !totals.containsKey(baselineValue)) {
            result.put(baselineValue, 0L);
        }
        for (T value :
                organizationToValueAndVote.allVotesIncludingIntraOrgDispute.getMap().keySet()) {
            if (!result.containsKey(value)) {
                result.put(value, 0L);
            }
        }
        if (DEBUG) {
            System.out.println("getResolvedVoteCountsIncludingIntraOrgDisputes :" + result);
        }
        return result;
    }

    public VoteStatus getStatusForOrganization(Organization orgOfUser) {
        if (!resolved) {
            resolveVotes();
        }
        if (Status.provisional.compareTo(winningStatus) >= 0) {
            // If the value is provisional, it needs more votes.
            return VoteStatus.provisionalOrWorse;
        }
        T orgVote = organizationToValueAndVote.getOrgVoteRaw(orgOfUser);
        if (!equalsOrgVote(winningValue, orgVote)) {
            // We voted and lost
            return VoteStatus.losing;
        }
        final int itemsWithVotes =
                DROP_HARD_INHERITANCE ? totals.size() : countDistinctValuesWithVotes();
        if (itemsWithVotes > 1) {
            // If there are votes for two "distinct" items, we should look at them.
            return VoteStatus.disputed;
        }
        final T singleVotedItem = getSingleVotedItem();
        if (!equalsOrgVote(winningValue, singleVotedItem)) {
            // If someone voted but didn't win
            return VoteStatus.disputed;
        }
        if (itemsWithVotes == 0) {
            // The value is ok, but we capture that there are no votes, for revealing items like
            // unsync'ed
            return VoteStatus.ok_novotes;
        } else {
            // We voted, we won, value is approved, no disputes, have votes
            return VoteStatus.ok;
        }
    }

    /**
     * Returns value of voted item, in case there is exactly 1.
     *
     * @return
     */
    private T getSingleVotedItem() {
        return totals.size() != 1 ? null : totals.iterator().next();
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
     * <p>For this purpose, if there are both votes for inheritance and votes for the specific value
     * matching the inherited (bailey) value, they are not "distinct": count them as a single value.
     *
     * @return the number of distinct values
     */
    private int countDistinctValuesWithVotes() {
        if (!resolved) { // must be resolved for bothInheritanceAndBaileyHadVotes
            throw new RuntimeException("countDistinctValuesWithVotes !resolved");
        }
        int count = organizationToValueAndVote.allVotesIncludingIntraOrgDispute.size();
        if (count > 1 && bothInheritanceAndBaileyHadVotes) {
            return count - 1; // prevent showing as "disputed" in dashboard
        }
        return count;
    }

    /**
     * Should this VoteResolver use keyword annotation voting?
     *
     * <p>Apply special voting method adjustAnnotationVoteCounts only to certain keyword annotations
     * that can have bar-separated values like "happy | joyful".
     *
     * <p>The paths for keyword annotations start with "//ldml/annotations/annotation" and do NOT
     * include Emoji.TYPE_TTS. Both name paths (cf. namePath, getNamePaths) and keyword paths (cf.
     * keywordPath, getKeywordPaths) have "//ldml/annotations/annotation". Name paths include
     * Emoji.TYPE_TTS, and keyword paths don't. Special voting is only for keyword paths, not for
     * name paths. Compare path dependencies in DisplayAndInputProcessor.java. See also
     * VoteResolver.splitAnnotationIntoComponentsList.
     *
     * @return true or false
     */
    private boolean isUsingKeywordAnnotationVoting() {
        if (pathHeader == null) {
            return false; // this happens in some tests
        }
        final String path = pathHeader.getOriginalPath();
        return AnnotationUtil.pathIsAnnotation(path) && !path.contains(Emoji.TYPE_TTS);
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
     * Can a user who makes a losing vote flag the locale+path? I.e., is the locale+path locked
     * and/or does it require HIGH_BAR votes?
     *
     * @return true or false
     */
    public boolean canFlagOnLosing() {
        return valueIsLocked || (getRequiredVotes() == HIGH_BAR);
    }

    /**
     * Calculate VoteResolver.Status
     *
     * @param baselineFile the 'baseline' file to use
     * @param path path the xpath
     * @return the Status
     */
    public static Status calculateStatus(CLDRFile baselineFile, String path) {
        String fullXPath = baselineFile.getFullXPath(path);
        if (fullXPath == null) {
            fullXPath = path;
        }
        final XPathParts xpp = XPathParts.getFrozenInstance(fullXPath);
        final String draft = xpp.getAttributeValue(-1, LDMLConstants.DRAFT);
        Status status = draft == null ? Status.approved : VoteResolver.Status.fromString(draft);

        /*
         * Reset to missing if the value is inherited from root or code-fallback, unless the XML actually
         * contains INHERITANCE_MARKER. Pass false for skipInheritanceMarker so that status will not be
         * missing for explicit INHERITANCE_MARKER.
         */
        final String srcid =
                baselineFile.getSourceLocaleIdExtended(
                        path, null, false /* skipInheritanceMarker */);
        if (srcid.equals(XMLSource.CODE_FALLBACK_ID)) {
            status = Status.missing;
        } else if (srcid.equals("root")) {
            if (!srcid.equals(baselineFile.getLocaleID())) {
                status = Status.missing;
            }
        }
        return status;
    }
    /**
     * Get the possibly modified value. If value matches the bailey value or inheritance marker,
     * possibly change it from bailey value to inheritance marker, or vice-versa, as needed to meet
     * these requirements: 1. If the path changes when getting bailey, then we are inheriting
     * sideways. We need to use a hard value. 2. If the value is different from the bailey value,
     * can't use inheritance; we need a hard value. 3. Otherwise we use inheritance marker.
     *
     * <p>These requirements are pragmatic, to work around limitations of the current inheritance
     * algorithm, which is hyper-sensitive to the distinction between inheritance marker and bailey,
     * which, depending on that distinction, unintentionally tends to change lateral inheritance to
     * vertical inheritance, or vice-versa.
     *
     * <p>This method has consequences affecting vote resolution. For example, assume
     * DROP_HARD_INHERITANCE is true. If a user votes for what is currently the inherited value, and
     * these requirements call for using inheritance marker, then their vote is stored as
     * inheritance marker in the db; if the parent value then changes (even during same release
     * cycle), the vote is still a vote for inheritance -- that is how soft inheritence has long
     * been intended to work. In the cases where this method returns the hard value matching bailey,
     * the user's vote is stored in the db as that hard value; if the parent value then changes, the
     * user's vote does not change -- this differs from what we'd like ideally (which is for all
     * inh. votes to be "soft"). If and when the inheritance algorithm changes to reduce or
     * eliminate the problematic aspects of the hard/soft distinction, this method might no longer
     * be needed.
     *
     * <p>Reference: https://unicode-org.atlassian.net/browse/CLDR-16560
     *
     * @param path the path
     * @param value the input value
     * @param cldrFile the CLDRFile for determining inheritance
     * @return the possibly modified value
     */
    public static String reviseInheritanceAsNeeded(String path, String value, CLDRFile cldrFile) {
        if (!DROP_HARD_INHERITANCE) {
            return value;
        }
        if (!cldrFile.isResolved()) {
            throw new InternalCldrException("must be resolved");
        }
        Output<String> pathWhereFound = new Output<>();
        Output<String> localeWhereFound = new Output<>();
        String baileyValue = cldrFile.getBaileyValue(path, pathWhereFound, localeWhereFound);
        if (baileyValue != null
                && (CldrUtility.INHERITANCE_MARKER.equals(value) || baileyValue.equals(value))) {
            // TODO: decide whether to continue treating GlossonymConstructor.PSEUDO_PATH
            // (constructed values) as lateral inheritance. This method originally did not
            // take constructed values into account, so it implicitly treated constructed
            // values as laterally inherited, given that pathWhereFound doesn't equal path.
            // This original behavior corresponds to CONSTRUCTED_PSEUDO_PATH_NOT_LATERAL = false.
            // Reference: https://unicode-org.atlassian.net/browse/CLDR-16372
            final boolean CONSTRUCTED_PSEUDO_PATH_NOT_LATERAL = false;
            value =
                    (pathWhereFound.value.equals(path)
                                    || (CONSTRUCTED_PSEUDO_PATH_NOT_LATERAL
                                            && GlossonymConstructor.PSEUDO_PATH.equals(
                                                    pathWhereFound.value)))
                            ? CldrUtility.INHERITANCE_MARKER
                            : baileyValue;
        }
        return value;
    }
}
