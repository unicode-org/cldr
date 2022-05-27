/**
 *
 */
package org.unicode.cldr.web;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.TestCache;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.CLDRConfig.Environment;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.VoteResolver.Level;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.XPathParts.Comments;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;
import org.unicode.cldr.web.SurveyException.ErrorCode;
import org.unicode.cldr.web.UserRegistry.ModifyDenial;
import org.unicode.cldr.web.UserRegistry.User;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.VersionInfo;

/**
 * @author srl
 *
 */
public class STFactory extends Factory implements BallotBoxFactory<UserRegistry.User>, UserRegistry.UserChangedListener {
    /**
     * Q: Do we want different loggers for the multiplicity of inner classes?
     */
    static final Logger logger = SurveyLog.forClass(STFactory.class);

    private enum VoteLoadingContext {
        /**
         * The ordinary context when loadVoteValues is called by makeSource, such as when displaying the
         * main vetting view in Survey Tool
         */
        ORDINARY_LOAD_VOTES,

        /**
         * The special context when loadVoteValues is called by makeVettedSource for generating VXML
         */
        VXML_GENERATION,

        /**
         * The context when a voting (or abstaining) event occurs and setValueFromResolver is
         * called by voteForValue (not used for loadVoteValues)
         */
        SINGLE_VOTE,
    }

    /**
     * This class tracks the expected maximum size of strings in the locale.
     * @author srl
     *
     */
    public static class LocaleMaxSizer {
        public static final int EXEMPLAR_CHARACTERS_MAX = 8192;

        public static final String EXEMPLAR_CHARACTERS = "//ldml/characters/exemplarCharacters";

        Map<CLDRLocale, Map<String, Integer>> sizeExceptions;

        TreeMap<String, Integer> exemplars_prefix = new TreeMap<>();
        Set<CLDRLocale> exemplars_set = new TreeSet<>();

        /**
         * Construct a new sizer.
         */
        public LocaleMaxSizer() {
            // set up the map
            sizeExceptions = new TreeMap<>();
            exemplars_prefix.put(EXEMPLAR_CHARACTERS, EXEMPLAR_CHARACTERS_MAX);
            String locs[] = { "ja", "ko", "zh", "zh_Hant" /*because of cross-script inheritance*/ };
            for (String loc : locs) {
                exemplars_set.add(CLDRLocale.getInstance(loc));
            }
        }

        /**
         * It's expected that this is called with EVERY locale, so we do not recurse into parents.
         * @param l
         */
        public void add(CLDRLocale l) {
            if (l == null) return; // attempt to add null
            CLDRLocale hnr = l.getHighestNonrootParent();
            if (hnr == null) return; // Exit if l is root
            if (exemplars_set.contains(hnr)) { // are we a child of ja, ko, zh?
                sizeExceptions.put(l, exemplars_prefix);
            }
        }

        /**
         * For the specified locale, what is the expected string size?
         * @param locale
         * @param xpath
         * @return
         */
        public int getSize(CLDRLocale locale, String xpath) {
            Map<String, Integer> prefixes = sizeExceptions.get(locale);
            if (prefixes != null) {
                for (Map.Entry<String, Integer> e : prefixes.entrySet()) {
                    if (xpath.startsWith(e.getKey())) {
                        return e.getValue();
                    }
                }
            }
            return MAX_VAL_LEN;
        }

        /**
         * The max string length accepted of any value.
         */
        public static final int MAX_VAL_LEN = 4096;

    }

    private static final String VOTE_OVERRIDE = "vote_override";

    private class DataBackedSource extends DelegateXMLSource {
        PerLocaleData ballotBox;

        private DataBackedSource(PerLocaleData makeFrom) {
            super(makeFrom.diskData.cloneAsThawed());
            ballotBox = makeFrom;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.icu.util.Freezable#freeze()
         */
        @Override
        public XMLSource freeze() {
            readonly();
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#getFullPathAtDPath(java.lang.String)
         */
        @Override
        public String getFullPathAtDPath(String path) {
            // Map<User,String> m = ballotBox.peekXpathToVotes(path);
            // if(m==null || m.isEmpty()) {
            // return aliasOf.getFullPathAtDPath(path);
            // } else {
            // logger.warning("Note: DBS.getFullPathAtDPath() todo!!");
            // TODO: show losing values
            return delegate.getFullPathAtDPath(path);
            // }
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#getValueAtDPath(java.lang.String)
         */
        @Override
        public String getValueAtDPath(String path) {
            return delegate.getValueAtDPath(path);
        }

        @Override
        public Date getChangeDateAtDPath(String path) {
            return ballotBox.getLastModDate(path);
        }

        /**
         * Set the value for the given path for this DataBackedSource, using the given VoteResolver.
         * This is the bottleneck for processing values.
         *
         * @param path the xpath
         * @param resolver the VoteResolver (for recycling), or null
         * @param voteLoadingContext the VoteLoadingContext
         * @return the VoteResolver
         */
        private VoteResolver<String> setValueFromResolver(String path, VoteResolver<String> resolver, VoteLoadingContext voteLoadingContext) {
            PerLocaleData.PerXPathData xpd = ballotBox.peekXpathData(path);
            String value = null;
            String fullPath = null;
            /*
             * If there are no votes, it may be more efficient (or anyway expected) to skip vote resolution
             * and use diskData instead. This has far-reaching effects and should be better documented.
             * When and how does it change the outcome and/or performance?
             * Currently only skip for VoteLoadingContext.ORDINARY_LOAD_VOTES with null/empty xpd.
             *
             * Do not skip vote resolution if VoteLoadingContext.SINGLE_VOTE, even for empty xpd. Otherwise an Abstain can
             * result in "no votes", "skip vote resolution", failure to get the right winning value, possibly inherited.
             *
             * Do not skip vote resolution if VoteLoadingContext.VXML_GENERATION, even for empty xpd. We may need to call
             * getWinningValue for vote resolution for a larger set of paths to get baseline, etc.
             */
            if (voteLoadingContext == VoteLoadingContext.ORDINARY_LOAD_VOTES && (xpd == null || xpd.isEmpty())) {
                /*
                 * Skip vote resolution
                 */
                value = ballotBox.diskData.getValueAtDPath(path);
                fullPath = ballotBox.diskData.getFullPathAtDPath(path);
            } else {
                resolver = ballotBox.getResolver(xpd, path, resolver);
                value = resolver.getWinningValue();
                fullPath = getFullPathWithResolver(path, resolver);
            }
            if (value != null) {
                /*
                 * TODO: needed to clear fullPath? Otherwise, fullPath may be ignored if
                 * value is extant.
                 */
                delegate.removeValueAtDPath(path);
                delegate.putValueAtPath(fullPath, value);
            } else {
                delegate.removeValueAtDPath(path);
            }
            return resolver;
        }

        private String getFullPathWithResolver(String path, VoteResolver<String> resolver) {
            String diskFullPath = ballotBox.diskData.getFullPathAtDPath(path);
            if (diskFullPath == null) {
                /*
                 * If the disk didn't have a full path, just use the inbound path.
                 */
                diskFullPath = path;
            }
            /*
             * Remove JUST draft alt proposed. Leave 'numbers=' etc.
             */
            String baseXPath = XPathTable.removeDraftAltProposed(diskFullPath);
            Status win = resolver.getWinningStatus();
            /*
             * Catch Status.missing, or it will trigger an exception in draftStatusFromWinningStatus
             * since there is no "missing" in DraftStatus.
             * This may happen especially for VoteLoadingContext.VXML_GENERATION.
             *
             * Status.missing can also occur for VoteLoadingContext.SINGLE_VOTE, when a user abstains
             * after submitting a new value. Then, delegate.removeValueAtDPath and/or delegate.putValueAtPath
             * is required to clear out the submitted value; then possibly res = inheritance marker
             */
            if (win == Status.missing || win == Status.approved) {
                return baseXPath;
            } else {
                DraftStatus draftStatus = draftStatusFromWinningStatus(win);
                return baseXPath + "[@draft=\"" + draftStatus.toString() + "\"]";
            }
        }

        /**
         * Map the given VoteResolver.Status to a CLDRFile.DraftStatus
         *
         * @param win the VoteResolver.Status (winning status)
         * @return the DraftStatus
         *
         * As a rule, the name of each VoteResolver.Status is also the name of a DraftStatus.
         * Any exceptions to that rule should be handled explicitly in this function.
         * However, VoteResolver.Status.missing is currently NOT handled and will cause an
         * exception to be logged. The caller should check for VoteResolver.Status.missing
         * and avoid calling this function with it.
         *
         * References:
         *     https://unicode.org/cldr/trac/ticket/11721
         *     https://unicode.org/cldr/trac/ticket/11766
         *     https://unicode.org/cldr/trac/ticket/11103
         */
        private DraftStatus draftStatusFromWinningStatus(VoteResolver.Status win) {
            try {
                DraftStatus draftStatus = DraftStatus.forString(win.toString());
                return draftStatus;
            } catch (IllegalArgumentException e) {
                SurveyLog.logException(logger, e, "Exception in draftStatusFromWinningStatus of " + win);
                return DraftStatus.unconfirmed;
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see org.unicode.cldr.util.XMLSource#getXpathComments()
         */
        @Override
        public Comments getXpathComments() {
            return delegate.getXpathComments();
        }

        /*
         * (non-Javadoc)
         *
         * @see org.unicode.cldr.util.XMLSource#iterator()
         */
        @Override
        public Iterator<String> iterator() {
            if (ballotBox.isEmpty()) {
                return delegate.iterator();
            } else {
                // SurveyLog.debug("Note: DBS.iterator() todo -- iterate over losing values?");
                // // losing values are available in the raw xml.
                return delegate.iterator();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#putFullPathAtDPath(java.lang.String,
         * java.lang.String)
         */
        @Override
        public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
            readonly();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#putValueAtDPath(java.lang.String,
         * java.lang.String)
         */
        @Override
        public void putValueAtDPath(String distinguishingXPath, String value) {
            readonly();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#removeValueAtDPath(java.lang.String)
         */
        @Override
        public void removeValueAtDPath(String distinguishingXPath) {
            readonly();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#setXpathComments(org.unicode.cldr
         * .util.XPathParts.Comments)
         */
        @Override
        public void setXpathComments(Comments comments) {
            readonly();
        }
    }

    /**
     * the STFactory maintains exactly one instance of this class per locale it
     * is working with. It contains the XMLSource, Example Generator, etc..
     *
     * @author srl
     *
     */
    private final class PerLocaleData implements Comparable<PerLocaleData>, BallotBox<User> {
        private CLDRFile file = null, rFile = null;
        private CLDRLocale locale;
        private boolean readonly;
        private MutableStamp stamp = null;

        /**
         * The held XMLSource.
         */
        private DataBackedSource xmlsource = null;
        /**
         * The on-disk data. May be == to xmlsource for readonly data.
         */
        private XMLSource diskData = null;
        private CLDRFile diskFile = null;

        /**
         * Per-xpath data. There's one of these per xpath- voting data, etc.
         * Does not contain the actual xpath, at least for now.
         * @author srl
         *
         */
        private final class PerXPathData {
            /**
             * Per (voting) user data. For each xpath, there's one of these per user that is voting.
             * @author srl
             *
             */
            private final class PerUserData {
                /**
                 * What is this user voting for?
                 */
                String vote;
                /**
                 * What is this user's override strength?
                 */
                Integer override = null;
                Date when = null;

                public PerUserData(String value, Integer voteOverride, Date when) {
                    this.vote = value;
                    this.override = voteOverride;
                    this.when = when;
                    if (lastModDate == null || lastModDate.before(when)) {
                        lastModDate = when;
                    }
                }

                /**
                 * Has this user overridden their vote? Integer or null.
                 * @return
                 */
                public Integer getOverride() {
                    return override;
                }

                public String getValue() {
                    return vote;
                }

                public Date getWhen() {
                    return when;
                }
            }

            Date lastModDate = null;
            Set<String> otherValues = null;
            Map<User, PerUserData> userToData = null;

            /**
             * Is there any user data (votes)?
             * @return
             */
            public boolean isEmpty() {
                return userToData == null || userToData.isEmpty();
            }

            /**
             * Get all votes
             * @return
             */
            public Iterable<Entry<User, PerUserData>> getVotes() {
                return userToData.entrySet();
            }

            /**
             * Get the set of other values. May be null.
             * @return
             */
            public Set<String> getOtherValues() {
                return otherValues;
            }

            public Set<User> getVotesForValue(String value) {
                if (isEmpty()) {
                    return null;
                }
                TreeSet<User> ts = new TreeSet<>();
                for (Entry<User, PerUserData> e : userToData.entrySet()) {
                    if (e.getValue().getValue().equals(value)) {
                        ts.add(e.getKey());
                    }
                }
                if (ts.isEmpty())
                    return null;
                return ts;
            }

            public String getVoteValue(User user) {
                if (isEmpty()) {
                    return null;
                } else {
                    PerUserData pud = peekUserToData(user);
                    if (pud == null) return null;
                    return pud.getValue();
                }
            }

            private PerUserData peekUserToData(User user) {
                if (userToData == null) return null;
                return userToData.get(user);
            }

            public void setVoteForValue(User user, String distinguishingXpath, String value, Integer voteOverride, Date when) {
                if (value != null) {
                    setPerUserData(user, new PerUserData(value, voteOverride, when));
                } else {
                    removePerUserData(user);
                }
            }

            private PerUserData removePerUserData(User user) {
                if (userToData != null) {
                    PerUserData removed = userToData.remove(user);
                    if (userToData.isEmpty()) {
                        lastModDate = null; // date is now null- object is empty
                        //userToData=null /*not needed*/
                    }
                    return removed;
                } else {
                    return null;
                }
            }

            /**
             * Remove all votes from this PerXPathData that match the given count
             * for getOverride.
             *
             * @param overrideVoteCount
             */
            private void removeOverrideVotes(int overrideVoteCount) {
                if (userToData != null) {
                    HashSet<User> toDelete = new HashSet<>();
                    userToData.forEach((k, v) -> {
                        Integer override = v.getOverride();
                        if (override != null && override == overrideVoteCount) {
                            toDelete.add(k);
                        }
                    });
                    for (User k : toDelete) {
                        userToData.remove(k);
                    }
                }
            }

            /**
             * Set this user's vote.
             * @param user
             * @param pud
             * @return
             */
            private PerUserData setPerUserData(User user, PerUserData pud) {
                if (userToData == null) {
                    userToData = new ConcurrentHashMap<>();
                }
                userToData.put(user, pud);
                return pud;
            }

            /**
             * Did this user vote?
             * @param myUser
             * @return
             */
            public boolean userDidVote(User myUser) {
                if (userToData == null) {
                    return false;
                }
                PerUserData pud = peekUserToData(myUser);
                return (pud != null && pud.getValue() != null);
            }

            public Map<User, Integer> getOverridesPerUser() {
                if (isEmpty()) return null;
                Map<User, Integer> rv = new HashMap<>(userToData.size());
                for (Entry<User, PerUserData> e : userToData.entrySet()) {
                    if (e.getValue().getOverride() != null) {
                        rv.put(e.getKey(), e.getValue().getOverride());
                    }
                }
                return rv;
            }

            public Date getLastModDate() {
                return lastModDate;
            }
        }

        private Map<String, PerXPathData> xpathToData = new HashMap<>();

        private XMLSource resolvedXmlsource = null;

        PerLocaleData(CLDRLocale locale) {
            this.locale = locale;
            readonly = isReadOnlyLocale(locale);
            diskData = sm.getDiskFactory().makeSource(locale.getBaseName()).freeze();
            sm.xpt.loadXPaths(diskData);
            diskFile = sm.getDiskFactory().make(locale.getBaseName(), true).freeze();
            pathsForFile = phf.pathsForFile(diskFile);
            stamp = mintLocaleStamp(locale);
        }

        public boolean isEmpty() {
            return xpathToData.isEmpty();
        }

        /**
         * Get all of the PerXPathData paths
         * @return
         */
        public Set<String> allPXDPaths() {
            return xpathToData.keySet();
        }

        public final Stamp getStamp() {
            return stamp;
        }

        /**
         *
         * @param user
         *            - The user voting on the path
         * @param xpath
         *            - The xpath being voted on.
         * @return true - If pathHeader and coverage would indicate a value that
         *         the user should have been able to vote on.
         *
         */
        private boolean isValidSurveyToolVote(UserRegistry.User user, String xpath) {
            PathHeader ph = getPathHeader(xpath);
            if (ph == null)
                return false;
            if (ph.getSurveyToolStatus() == PathHeader.SurveyToolStatus.DEPRECATED)
                return false;
            if (ph.getSurveyToolStatus() == PathHeader.SurveyToolStatus.HIDE
                || ph.getSurveyToolStatus() == PathHeader.SurveyToolStatus.READ_ONLY) {
                if (user == null || !UserRegistry.userIsTC(user))
                    return false;
            }

            if (sm.getSupplementalDataInfo().getCoverageValue(xpath, locale.getBaseName()) > org.unicode.cldr.util.Level.COMPREHENSIVE.getLevel()) {
                return false;
            }
            return true;
        }

        /**
         * Load internal data (votes, etc.) for this PerLocaleData, and push it into the given DataBackedSource.
         *
         * @param targetXmlSource the DataBackedSource which might or might not equal this.xmlsource;
         *                        for makeVettedSource, it is a different (uncached) DataBackedSource.
         *
         * @param voteLoadingContext VoteLoadingContext.ORDINARY_LOAD_VOTES or VoteLoadingContext.VXML_GENERATION
         *                           (not VoteLoadingContext.SINGLE_VOTE)
         *
         * Called by PerLocaleData.makeSource (with VoteLoadingContext.ORDINARY_LOAD_VOTES)
         * and by PerLocaleData.makeVettedSource (with VoteLoadingContext.VXML_GENERATION).
         */
        private void loadVoteValues(DataBackedSource targetXmlSource, VoteLoadingContext voteLoadingContext) {
            VoteResolver<String> resolver = null; // save recalculating this.
            ElapsedTimer et = (SurveyLog.DEBUG) ? new ElapsedTimer("Loading PLD for " + locale) : null;
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            int n = 0;
            int del = 0;

            try {
                /*
                 * Select several columns (xp, submitter, value, override, last_mod),
                 * from all rows with the given locale in the votes table.
                 */
                conn = DBUtils.getInstance().getAConnection();
                ps = openQueryByLocaleRW(conn);
                ps.setString(1, locale.getBaseName());
                rs = ps.executeQuery();

                while (rs.next()) {
                    int xp = rs.getInt(1);
                    String xpath = sm.xpt.getById(xp);
                    int submitter = rs.getInt(2);
                    String value = DBUtils.getStringUTF8(rs, 3);
                    /*
                     * 4 = locale is unused here, but is required in the query in case deleteRow
                     * is called below. See openQueryByLocaleRW for explanation.
                     */
                    Integer voteOverride = rs.getInt(5); // 5 override
                    if (voteOverride == 0 && rs.wasNull()) { // if override was a null..
                        voteOverride = null;
                    }
                    Timestamp last_mod = rs.getTimestamp(6); // last mod
                    User theSubmitter = sm.reg.getInfo(submitter);
                    if (theSubmitter == null) {
                        SurveyLog.warnOnce(logger, "Ignoring votes for deleted user #" + submitter);
                    }
                    if (!UserRegistry.countUserVoteForLocale(theSubmitter, locale)) { // check user permission to submit
                        continue;
                    }
                    if (!isValidSurveyToolVote(theSubmitter, xpath)) { // Make sure it is a visible path
                        continue;
                    }
                    try {
                        internalSetVoteForValue(theSubmitter, xpath, value, voteOverride, last_mod);
                        n++;
                    } catch (BallotBox.InvalidXPathException e) {
                        logger.severe("InvalidXPathException: Deleting vote for " + theSubmitter + ":" + locale + ":" + xpath);
                        rs.deleteRow();
                        del++;
                    }
                }
                if (del > 0) {
                    logger.warning ("Summary: delete of " + del + " invalid votes from " + locale);
                }
                DBUtils.close(rs, ps);
                ps = openPermVoteQuery(conn);
                ps.setString(1, locale.getBaseName());
                rs = ps.executeQuery();
                while (rs.next()) {
                    int xp = rs.getInt(1);
                    String xpath = sm.xpt.getById(xp);
                    String value = DBUtils.getStringUTF8(rs, 2);
                    Timestamp last_mod = rs.getTimestamp(3);
                    try {
                        internalSetVoteForValue(sm.reg.getInfo(UserRegistry.ADMIN_ID), xpath, value, VoteResolver.Level.LOCKING_VOTES, last_mod);
                        n++;
                    } catch (BallotBox.InvalidXPathException e) {
                        System.err.println("InvalidXPathException: Ignoring permanent vote for:" + locale + ":" + xpath);
                    }
                }
            } catch (SQLException e) {
                SurveyLog.logException(logger, e, "In setValueFromResolver");
                SurveyMain.busted("Could not read locale " + locale, e);
                throw new InternalError("Could not load locale " + locale + " : " + DBUtils.unchainSqlException(e));
            } finally {
                DBUtils.close(rs, ps, conn);
            }
            SurveyLog.debug(et + " - read " + n + " items  (" + xpathToData.size() + " xpaths.)");

            et = (SurveyLog.DEBUG) ? new ElapsedTimer("Resolver loading for xpaths in " + locale) : null;
            /*
             * Now that we've loaded all the votes, resolve the votes for each path.
             *
             * For VoteLoadingContext.VXML_GENERATION we use all paths in diskData (trunk) in
             * addition to allPXDPaths(); otherwise, vxml produced by OutputFileManager is missing some paths.
             * allPXDPaths() may return an empty array if there are no votes in current votes table.
             * (However, we assume that last-release value soon won't be used anymore for vote resolution.
             * If we did need paths from last-release, or any paths missing from trunk and current votes table,
             * we could loop through sm.getSTFactory().getPathsForFile(locale); however, that would generally
             * include more paths than are wanted for vxml.)
             * Reference: https://unicode-org.atlassian.net/browse/CLDR-11909
             *
             * TODO: revisit whether this difference for VoteLoadingContext.VXML_GENERATION is still necessary; when added
             * cases where last-release value made a difference to vote resolution; now that "baseline" = trunk not
             * last-release it's possible that vote resolution isn't needed for items without current votes.
             */
            Set<String> xpathSet;
            if (voteLoadingContext == VoteLoadingContext.VXML_GENERATION) {
                xpathSet = new HashSet<>(allPXDPaths());
                for (String xp : diskData) {
                    xpathSet.add(xp);
                }
            } else { // voteLoadingContext == VoteLoadingContext.ORDINARY_LOAD_VOTES
                xpathSet = allPXDPaths();
            }
            int j = 0;
            for (String xp : xpathSet) {
                try {
                    resolver = targetXmlSource.setValueFromResolver(xp, resolver, voteLoadingContext);
                } catch (Exception e) {
                    SurveyLog.logException(logger, e, "In setValueFromResolver, xp = " + xp);
                }
                j++;
            }
            SurveyLog.debug(et + " - resolved " + j + " items, " + n + " total.");
        }

        @Override
        public int compareTo(PerLocaleData arg0) {
            if (this == arg0) {
                return 0;
            } else {
                return locale.compareTo(arg0.locale);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (!(other instanceof PerLocaleData)) {
                return false;
            } else {
                return ((PerLocaleData) other).locale.equals(locale);
            }
        }

        public synchronized CLDRFile getFile(boolean resolved) {
            if (resolved) {
                if (rFile == null) {
                    if (getSupplementalDirectory() == null)
                        throw new InternalError("getSupplementalDirectory() == null!");
                    rFile = new CLDRFile(makeSource(true)).setSupplementalDirectory(getSupplementalDirectory());
                    rFile.getSupplementalDirectory();
                }
                return rFile;
            } else {
                if (file == null) {
                    if (getSupplementalDirectory() == null)
                        throw new InternalError("getSupplementalDirectory() == null!");
                    file = new CLDRFile(makeSource(false)).setSupplementalDirectory(getSupplementalDirectory());
                }
                return file;
            }
        }

        /**
         * Utility class for testing values
         * @author srl
         *
         */
        private class ValueChecker {
            private final String path;
            /*
             * Use 8 for initial HashSet size; 16 is probably too many values for best performance
             */
            HashSet<String> goodValues = new HashSet<>(8);
            HashSet<String> badValues = new HashSet<>(8);

            LinkedList<CheckCLDR.CheckStatus> result = null;
            TestResultBundle testBundle = null;

            ValueChecker(String path) {
                this.path = path;
            }

            boolean canUseValue(String value) {
                if (value == null || goodValues.contains(value)) {
                    return true;
                } else if (badValues.contains(value)) {
                    return false;
                } else {
                    if (testBundle == null) {
                        testBundle = getDiskTestBundle(locale);
                        result = new LinkedList<>();
                    } else {
                        result.clear();
                    }

                    testBundle.check(path, result, value);
                    if (false) System.out.println("Checking result of " + path + " = " + value + " := haserr " + CheckCLDR.CheckStatus.hasError(result));
                    if (CheckCLDR.CheckStatus.hasError(result)) {
                        badValues.add(value);
                        return false;
                    } else {
                        goodValues.add(value);
                        return true; // OK
                    }
                }
            }

        }

        /**
         * Disable ValueChecker
         */
        private static final boolean ERRORS_ALLOWED_IN_VETTING = true;

        /**
         * Create or update a VoteResolver for this item
         *
         * @param perXPathData
         *            map of users to vote values
         * @param path
         *            xpath voted on
         * @param r
         *            if non-null, resolver to re-use.
         * @return the new or updated resolver
         *
         * This function is called by getResolver, and may also call itself recursively.
         */
        private VoteResolver<String> getResolverInternal(PerXPathData perXPathData, String path, VoteResolver<String> r) {
            if (path == null) {
                throw new IllegalArgumentException("path must not be null");
            }
            if (r == null) {
                r = new VoteResolver<>(); // create
            } else {
                r.clear(); // reuse
            }
            /* Apply special voting method adjustAnnotationVoteCounts only to certain bar-separated keyword annotations.
             * See http://unicode.org/cldr/trac/ticket/10973
             * The paths for keyword annotations start with "//ldml/annotations/annotation" and do NOT include Emoji.TYPE_TTS.
             * Both name paths (cf. namePath, getNamePaths) and keyword paths (cf. keywordPath, getKeywordPaths)
             * have "//ldml/annotations/annotation". Name paths include Emoji.TYPE_TTS, and keyword paths don't.
             * Special voting is only for keyword paths, not for name paths.
             * Compare path dependencies in DisplayAndInputProcessor.java. See also VoteResolver.splitAnnotationIntoComponentsList.
             * Note: this does not affect the occurrences of "new VoteResolver" in ConsoleCheckCLDR.java or TestUtilities.java;
             * if those tests ever involve annotation keywords, they could call setUsingKeywordAnnotationVoting as needed.
             */
            r.setUsingKeywordAnnotationVoting(AnnotationUtil.pathIsAnnotation(path) && !path.contains(Emoji.TYPE_TTS));

            final ValueChecker vc = ERRORS_ALLOWED_IN_VETTING ? null : new ValueChecker(path);

            // Set established locale
            r.setLocale(locale, getPathHeader(path));

            // set current Trunk (baseline) value (if present)
            final String currentValue = diskData.getValueAtDPath(path);
            final Status currentStatus = calculateStatus(diskFile, diskFile, path);
            if (ERRORS_ALLOWED_IN_VETTING || vc.canUseValue(currentValue)) {
                r.setBaseline(currentValue, currentStatus);
                r.add(currentValue);
            }

            CLDRFile cf = make(locale, true);
            r.setBaileyValue(cf.getBaileyValue(path, null, null));

            // add each vote
            if (perXPathData != null && !perXPathData.isEmpty()) {
                for (Entry<User, PerLocaleData.PerXPathData.PerUserData> e : perXPathData.getVotes()) {
                    PerLocaleData.PerXPathData.PerUserData v = e.getValue();

                    if (ERRORS_ALLOWED_IN_VETTING || vc.canUseValue(v.getValue())) {
                        r.add(v.getValue(), // user's vote
                            e.getKey().id, v.getOverride(), v.getWhen()); // user's id
                    }
                }
            }
            return r;
        }

        public VoteResolver<String> getResolver(PerXPathData perXPathData, String path, VoteResolver<String> r) {
            try {
                r = getResolverInternal(perXPathData, path, r);
            } catch (VoteResolver.UnknownVoterException uve) {
                handleUserChanged(null);
                try {
                    r = getResolverInternal(perXPathData, path, r);
                } catch (VoteResolver.UnknownVoterException uve2) {
                    SurveyLog.logException(logger, uve2, "Exception in getResolver");
                    SurveyMain.busted(uve2.toString(), uve2);
                    throw new InternalError(uve2.toString());
                }
            }
            return r;
        }

        @Override
        public VoteResolver<String> getResolver(String path) {
            return getResolver(peekXpathData(path), path, null);
        }

        @Override
        public Set<String> getValues(String xpath) {
            PerXPathData xpd = peekXpathData(xpath); // peek - may be empty

            Set<String> ts = new TreeSet<>(); // return set

            if (xpd != null) {
                // add the "other" values - non-votes.
                Set<String> other = xpd.getOtherValues();
                if (other != null) {
                    ts.addAll(other);
                }
                // add the actual votes
                if (!xpd.isEmpty()) {
                    for (Entry<User, PerXPathData.PerUserData> ud : xpd.getVotes()) {
                        ts.add(ud.getValue().getValue());
                    }
                }
            }
            // include the on-disk value, if not present.
            String fbValue = diskData.getValueAtDPath(xpath);
            if (fbValue != null) {
                ts.add(fbValue);
            }

            if (ts.isEmpty())
                return null; // return null if empty

            return ts;
        }

        /**
         * Return data for this xpath if available - don't create it.
         * @param xpath
         * @return
         */
        private final PerXPathData peekXpathData(String xpath) {
            return xpathToData.get(xpath);
        }

        /**
         * Get the PerXPathData for the given xpath, for this PerLocaleData;
         * create per-xpath data if not there.
         *
         * @param xpath the path string, like "//ldml/localeDisplayNames/languages/language[@type="ko"]"
         * @return the PerXPathData
         *
         * Called by internalSetVoteForValue only.
         */
        private final PerXPathData getXPathData(String xpath) {
            PerXPathData xpd = peekXpathData(xpath);
            if (xpd == null) {
                xpd = new PerXPathData();
                xpathToData.put(xpath, xpd);
            }
            return xpd;
        }

        @Override
        public Set<User> getVotesForValue(String xpath, String value) {
            PerXPathData xpd = peekXpathData(xpath);
            if (xpd != null && !xpd.isEmpty()) {
                return xpd.getVotesForValue(value);
            } else {
                return null;
            }
        }

        @Override
        public String getVoteValue(User user, String distinguishingXpath) {
            PerXPathData xpd = peekXpathData(distinguishingXpath);
            if (xpd != null) {
                return xpd.getVoteValue(user);
            } else {
                return null;
            }
        }

        private synchronized XMLSource makeSource(boolean resolved) {
            if (resolved == true) {
                if (resolvedXmlsource == null) {
                    resolvedXmlsource = makeResolvingSource(locale.getBaseName(), getMinimalDraftStatus());
                }
                return resolvedXmlsource;
            } else {
                if (readonly) {
                    return diskData;
                } else {
                    if (xmlsource == null) {
                        xmlsource = new DataBackedSource(this);
                        if (!readonly) {
                            loadVoteValues(xmlsource, VoteLoadingContext.ORDINARY_LOAD_VOTES);
                        }
                        stamp.next();
                        xmlsource.addListener(gTestCache);
                    }
                    return xmlsource;
                }
            }
        }

        /**
         * Make a vetted source for this PerLocaleData, suitable for producing vxml
         * with vote-resolution done on more paths.
         *
         * This function is similar to makeSource, but with VoteLoadingContext.VXML_GENERATION.
         *
         * @return the DataBackedSource (NOT the same as PerLocaleData.xmlsource)
         */
        private synchronized XMLSource makeVettedSource() {
            DataBackedSource vxmlSource = new DataBackedSource(this);
            if (!readonly) {
                loadVoteValues(vxmlSource, VoteLoadingContext.VXML_GENERATION);
            }
            return vxmlSource;
        }

        @Override
        public void unvoteFor(User user, String distinguishingXpath) throws BallotBox.InvalidXPathException, VoteNotAcceptedException {
            voteForValue(user, distinguishingXpath, null);
        }

        @Override
        public void revoteFor(User user, String distinguishingXpath) throws BallotBox.InvalidXPathException, VoteNotAcceptedException {
            String oldValue = getVoteValue(user, distinguishingXpath);
            voteForValue(user, distinguishingXpath, oldValue);
        }

        @Override
        public void voteForValue(User user, String distinguishingXpath, String value) throws InvalidXPathException, VoteNotAcceptedException {
            voteForValue(user, distinguishingXpath, value, null);
        }

        @Override
        public synchronized void voteForValue(User user, String distinguishingXpath, String value, Integer withVote) throws BallotBox.InvalidXPathException,
            BallotBox.VoteNotAcceptedException {
            if (!getPathsForFile().contains(distinguishingXpath)) {
                throw new BallotBox.InvalidXPathException(distinguishingXpath);
            }
            SurveyLog.debug("V4v: " + locale + " " + distinguishingXpath + " : " + user + " voting for '" + value + "'");
            /*
             * this has to do with changing a vote - not counting it.
             */
            ModifyDenial denial = UserRegistry.userCanModifyLocaleWhy(user, locale);
            if (denial != null) {
                throw new VoteNotAcceptedException(ErrorCode.E_NO_PERMISSION, "User " + user + " cannot modify " + locale + " " + denial);
            }

            int xpathId = sm.xpt.getByXpath(distinguishingXpath);
            boolean voteIsAutoImported = false;
            if (VOTE_IS_AUTO_IMPORTED == withVote) {
                withVote = null;
                voteIsAutoImported = true;
            } else if (withVote != null) {
                Level level = user.getLevel();
                if (withVote == level.getVotes(user.getOrganization())) {
                    withVote = null; // not an override
                } else if (!level.canVoteWithCount(user.getOrganization(), withVote)) {
                    throw new VoteNotAcceptedException(ErrorCode.E_NO_PERMISSION, "User " + user + " cannot vote at " + withVote + " level ");
                } else if (withVote == VoteResolver.Level.PERMANENT_VOTES) {
                    if (sm.fora.postCountFor(locale, xpathId) < 1) {
                        throw new VoteNotAcceptedException(ErrorCode.E_PERMANENT_VOTE_NO_FORUM, "Forum entry is required for Permanent vote");
                    }
                }
            }

            // check for too-long
            if (value != null) {
                final int valueLimit = SurveyMain.localeSizer.getSize(locale, distinguishingXpath);
                final int valueLength = value.length();
                if (valueLength > valueLimit) {
                    NumberFormat nf = NumberFormat.getInstance();
                    throw new VoteNotAcceptedException(ErrorCode.E_BAD_VALUE, "Length " + nf.format(valueLength) + " exceeds limit of "
                        + nf.format(valueLimit) + " - please file a bug if you need a longer value.");
                }
            }

            String oldVal = xmlsource.getValueAtDPath(distinguishingXpath);

            if (!readonly) {
                saveVoteToDb(user, distinguishingXpath, value, withVote, xpathId, voteIsAutoImported);
            } else {
                readonly();
            }

            internalSetVoteForValue(user, distinguishingXpath, value, withVote, new Date());

            if (withVote != null && withVote == VoteResolver.Level.PERMANENT_VOTES) {
                doPermanentVote(distinguishingXpath, xpathId, value);
            }

            xmlsource.setValueFromResolver(distinguishingXpath, null, VoteLoadingContext.SINGLE_VOTE);

            String newVal = xmlsource.getValueAtDPath(distinguishingXpath);
            if (newVal != null && !newVal.equals(oldVal)) {
                xmlsource.notifyListeners(distinguishingXpath);
            }
        }

        /**
         * Save the vote to the database
         *
         * @param user
         * @param distinguishingXpath
         * @param value
         * @param withVote
         * @param xpathId
         */
        private void saveVoteToDb(final User user, final String distinguishingXpath, final String value,
                final Integer withVote, final int xpathId, boolean voteIsAutoImported) {
            boolean didClearFlag = false;
            makeSource(false);
            ElapsedTimer et = !SurveyLog.DEBUG ? null : new ElapsedTimer("{0} Recording PLD for " + locale + " "
                + distinguishingXpath + " : " + user + " voting for '" + value);
            Connection conn = null;
            PreparedStatement saveOld = null; // save off old value
            PreparedStatement ps = null; // all for mysql, or 1st step for
            // derby
            PreparedStatement ps2 = null; // 2nd step for derby
            ResultSet rs = null;
            final boolean wasFlagged = getFlag(locale, xpathId); // do this outside of the txn..
            int submitter = user.id;
            try {
                conn = DBUtils.getInstance().getDBConnection();

                String add0 = "", add1 = "", add2 = "";

                // #1 - save the "VOTE_VALUE_ALT"  ( possible proposal) value.
                if (DBUtils.db_Mysql) {
                    add0 = "IGNORE";
                    // add1="ON DUPLICATE KEY IGNORE";
                } else {
                    add2 = "and not exists (select * from " + DBUtils.Table.VOTE_VALUE_ALT
                        + " where " + DBUtils.Table.VOTE_VALUE_ALT + ".locale="
                        + DBUtils.Table.VOTE_VALUE + ".locale and "
                        + DBUtils.Table.VOTE_VALUE_ALT + ".xpath=" + DBUtils.Table.VOTE_VALUE + ".xpath and "
                        + DBUtils.Table.VOTE_VALUE_ALT + ".value=" + DBUtils.Table.VOTE_VALUE + ".value )";
                }
                String sql = "insert " + add0 + " into " + DBUtils.Table.VOTE_VALUE_ALT + " " + add1
                    + " select " + DBUtils.Table.VOTE_VALUE + ".locale,"
                    + DBUtils.Table.VOTE_VALUE + ".xpath," + DBUtils.Table.VOTE_VALUE + ".value "
                    + " from " + DBUtils.Table.VOTE_VALUE
                    + " where locale=? and xpath=? and submitter=? and value is not null " + add2;
                saveOld = DBUtils.prepareStatementWithArgs(conn, sql, locale.getBaseName(), xpathId, user.id);
                saveOld.executeUpdate();

                // #2 - save the actual vote.
                if (DBUtils.db_Mysql) { // use 'on duplicate key' syntax
                    ps = DBUtils.prepareForwardReadOnly(conn, "INSERT INTO " + DBUtils.Table.VOTE_VALUE
                        + " (locale,xpath,submitter,value,last_mod," + VOTE_OVERRIDE + ") values (?,?,?,?,CURRENT_TIMESTAMP,?) "
                        + "ON DUPLICATE KEY UPDATE locale=?,xpath=?,submitter=?,value=?,last_mod=CURRENT_TIMESTAMP," + VOTE_OVERRIDE + "=?");
                    int colNum = 6;
                    ps.setString(colNum++, locale.getBaseName());
                    ps.setInt(colNum++, xpathId);
                    ps.setInt(colNum++, submitter);
                    DBUtils.setStringUTF8(ps, colNum++, value);
                    DBUtils.setInteger(ps, colNum++, withVote);
                } else { // derby
                    ps2 = DBUtils.prepareForwardReadOnly(conn, "DELETE FROM " + DBUtils.Table.VOTE_VALUE
                        + " where locale=? and xpath=? and submitter=? ");
                    ps = DBUtils.prepareForwardReadOnly(conn, "INSERT INTO " + DBUtils.Table.VOTE_VALUE
                        + " (locale,xpath,submitter,value,last_mod," + VOTE_OVERRIDE + ") VALUES (?,?,?,?,CURRENT_TIMESTAMP,?) ");
                    int colNum = 1;
                    ps2.setString(colNum++, locale.getBaseName());
                    ps2.setInt(colNum++, xpathId);
                    ps2.setInt(colNum++, submitter);
                    // NB:  no "VOTE_OVERRIDE" column on delete.
                }

                int colNum = 1;
                ps.setString(colNum++, locale.getBaseName());
                ps.setInt(colNum++, xpathId);
                ps.setInt(colNum++, submitter);
                DBUtils.setStringUTF8(ps, colNum++, value);
                DBUtils.setInteger(ps, colNum++, withVote);
                if (ps2 != null) {
                    ps2.executeUpdate();
                }
                ps.executeUpdate();

                if (wasFlagged && UserRegistry.userIsTC(user)) {
                    clearFlag(conn, locale, xpathId, user);
                    didClearFlag = true;
                }
                conn.commit();
            } catch (SQLException e) {
                SurveyLog.logException(logger, e, "Exception in saveVoteToDb");
                SurveyMain.busted("Could not vote for value in locale locale " + locale, e);
                throw new InternalError("Could not load locale " + locale + " : " + DBUtils.unchainSqlException(e));
            } finally {
                DBUtils.close(saveOld, rs, ps, ps2, conn);
            }
            SurveyLog.debug(et);

            if (sm.fora != null && !voteIsAutoImported) {
                sm.fora.doForumAfterVote(locale, user, distinguishingXpath, xpathId, value, didClearFlag);
            }
        }

        /**
         * Handle a Permanent Vote.
         *
         * @param distinguishingXpath the path String
         * @param xpathId the path id
         * @param value the String value for the candidate item voted for, or null for Abstain
         */
        private void doPermanentVote(String distinguishingXpath, int xpathId, String value) {
            PermanentVote pv = new PermanentVote(locale.getBaseName(), xpathId, value);
            if (pv.didLock()) {
                User admin = sm.reg.getInfo(UserRegistry.ADMIN_ID);
                peekXpathData(distinguishingXpath).setVoteForValue(admin, distinguishingXpath,
                        value, VoteResolver.Level.LOCKING_VOTES, new Date());
            } else if (pv.didUnlock()) {
                peekXpathData(distinguishingXpath).removeOverrideVotes(VoteResolver.Level.LOCKING_VOTES);
            }
            if (pv.didCleanSlate()) {
                peekXpathData(distinguishingXpath).removeOverrideVotes(VoteResolver.Level.PERMANENT_VOTES);
            }
        }

        /**
         * @param user
         * @param distinguishingXpath
         * @param value
         * @param when
         *
         * Called by loadVoteValues and voteForValue.
         */
        private void internalSetVoteForValue(User user, String distinguishingXpath, String value,
            Integer voteOverride, Date when) throws InvalidXPathException {

            // Don't allow illegal xpaths to be set.
            if (!getPathsForFile().contains(distinguishingXpath)) {
                throw new InvalidXPathException(distinguishingXpath);
            }
            getXPathData(distinguishingXpath).setVoteForValue(user, distinguishingXpath, value, voteOverride, when);
            stamp.next();
        }

        @Override
        public boolean userDidVote(User myUser, String somePath) {
            PerXPathData xpd = peekXpathData(somePath);
            return (xpd != null && xpd.userDidVote(myUser));
        }

        public TestResultBundle getTestResultData(CheckCLDR.Options options) {
            synchronized (gTestCache) {
                return gTestCache.getBundle(options);
            }
        }

        public Set<String> getPathsForFile() {
            return pathsForFile;
        }

        private Set<String> pathsForFile = null;

        BitSet votesSometimeThisRelease = null;

        @Override
        public boolean hadVotesSometimeThisRelease(int xpath) {
            if (votesSometimeThisRelease != null) {
                return votesSometimeThisRelease.get(xpath);
            } else {
                return false; // unknown.
            }
        }

        @Override
        public Map<User, Integer> getOverridesPerUser(String xpath) {
            PerXPathData xpd = peekXpathData(xpath);
            if (xpd == null) {
                return null;
            } else {
                return xpd.getOverridesPerUser();
            }
        }

        @Override
        public Date getLastModDate(String xpath) {
            PerXPathData xpd = peekXpathData(xpath);
            if (xpd == null) {
                return null;
            } else {
                return xpd.getLastModDate();
            }
        }
    }

    /**
     * @author srl
     *
     */
    public class DelegateXMLSource extends XMLSource {
        protected XMLSource delegate;

        public DelegateXMLSource(CLDRLocale locale) {
            setLocaleID(locale.getBaseName());

            delegate = sm.getDiskFactory().makeSource(locale.getBaseName());
        }

        public DelegateXMLSource(XMLSource source) {
            setLocaleID(source.getLocaleID());
            delegate = source;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.icu.util.Freezable#freeze()
         */
        @Override
        public XMLSource freeze() {
            readonly();
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#getFullPathAtDPath(java.lang.String)
         */
        @Override
        public String getFullPathAtDPath(String path) {
            return delegate.getFullPathAtDPath(path);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#getValueAtDPath(java.lang.String)
         */
        @Override
        public String getValueAtDPath(String path) {
            String v = delegate.getValueAtDPath(path);
            return v;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.unicode.cldr.util.XMLSource#getXpathComments()
         */
        @Override
        public Comments getXpathComments() {
            return delegate.getXpathComments();
        }

        /*
         * (non-Javadoc)
         *
         * @see org.unicode.cldr.util.XMLSource#iterator()
         */
        @Override
        public Iterator<String> iterator() {
            return delegate.iterator();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#putFullPathAtDPath(java.lang.String,
         * java.lang.String)
         */
        @Override
        public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
            readonly();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#putValueAtDPath(java.lang.String,
         * java.lang.String)
         */
        @Override
        public void putValueAtDPath(String distinguishingXPath, String value) {
            readonly();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#removeValueAtDPath(java.lang.String)
         */
        @Override
        public void removeValueAtDPath(String distinguishingXPath) {
            readonly();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.unicode.cldr.util.XMLSource#setXpathComments(org.unicode.cldr
         * .util.XPathParts.Comments)
         */
        @Override
        public void setXpathComments(Comments comments) {
            readonly();
        }

        @Override
        public void getPathsWithValue(String valueToMatch, String pathPrefix, Set<String> result) {
            delegate.getPathsWithValue(valueToMatch, pathPrefix, result);

        }

        @Override
        public VersionInfo getDtdVersionInfo() {
            return delegate.getDtdVersionInfo();
        }
    }

    /**
     * Is this a locale that can't be modified?
     *
     * @param loc
     * @return
     */
    public static final boolean isReadOnlyLocale(CLDRLocale loc) {
        return SurveyMain.getReadOnlyLocales().contains(loc);
    }

    /**
     * Is this a locale that can't be modified?
     *
     * @param loc
     * @return
     */
    public static final boolean isReadOnlyLocale(String loc) {
        return isReadOnlyLocale(CLDRLocale.getInstance(loc));
    }

    private static void readonly() {
        throw new InternalError("This is a readonly instance.");
    }

    /**
     * Throw an error.
     * This is a bottleneck called whenever something unimplemented is called.
     */
    static public void unimp() {
        throw new InternalError("NOT YET IMPLEMENTED - TODO!.");
    }

    boolean dbIsSetup = false;

    /**
     * Test cache against (this)
     */
    TestCache gTestCache = new TestCache();
    /**
     * Test cache against disk. For rejecting items.
     */
    TestCache gDiskTestCache = new TestCache();

    /**
     * The infamous back-pointer.
     */
    public SurveyMain sm = null;

    private org.unicode.cldr.util.PathHeader.Factory phf;

    /**
     * Construct one.
     */
    public STFactory(SurveyMain sm) {
        super();
        if (sm == null) {
            throw new IllegalArgumentException("sm must not be null");
        }
        this.sm = sm;
        try (CLDRProgressTask progress = sm.openProgress("STFactory")) {
            progress.update("setup supplemental data");
            setSupplementalDirectory(sm.getDiskFactory().getSupplementalDirectory());

            progress.update("setup test cache");
            gTestCache.setFactory(this, "(?!.*(CheckCoverage).*).*");
            progress.update("setup disk test cache");
            gDiskTestCache.setFactory(sm.getDiskFactory(), "(?!.*(CheckCoverage).*).*");
            sm.reg.addListener(this);
            progress.update("reload all users");
            handleUserChanged(null);
            progress.update("setup pathheader factory");
            phf = PathHeader.getFactory(sm.getEnglishFile());
        }
    }

    /**
     * For statistics
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("-cache:");
        int good = 0;
        for (Entry<CLDRLocale, Reference<PerLocaleData>> e : locales.entrySet()) {
            if (e.getValue().get() != null) {
                good++;
            }
        }
        sb.append(good + "/" + locales.size() + " locales. TestCache:" + gTestCache + ", diskTestCache:" + gDiskTestCache + "}");
        return sb.toString();
    }

    @Override
    public BallotBox<User> ballotBoxForLocale(CLDRLocale locale) {
        return get(locale);
    }

    /**
     * Per locale map
     */
    private Map<CLDRLocale, Reference<PerLocaleData>> locales = new HashMap<>();

    private Cache<CLDRLocale, PerLocaleData> rLocales = CacheBuilder.newBuilder().softValues().build();

    private Map<CLDRLocale, MutableStamp> localeStamps = new ConcurrentHashMap<>(SurveyMain.getLocales().length);

    /**
     * Peek at the stamp (changetime) for a locale. May be null, meaning we don't know what the stamp is.
     * If the locale has gone out of scope (GC) it will return the old stamp, rather than
     * @param loc
     * @return
     */
    public Stamp peekLocaleStamp(CLDRLocale loc) {
        MutableStamp ms = localeStamps.get(loc);
        return ms;
    }

    /**
     * Return changetime.
     * @param locale
     * @return
     */
    public MutableStamp mintLocaleStamp(CLDRLocale locale) {
        MutableStamp s = localeStamps.get(locale);
        if (s == null) {
            s = MutableStamp.getInstance();
            localeStamps.put(locale, s);
        }
        return s;
    }

    /**
     * Get the locale stamp, loading the locale if not loaded.
     * @param loc
     * @return
     */
    public Stamp getLocaleStamp(CLDRLocale loc) {
        return get(loc).getStamp();
    }

    /**
     * Fetch a locale from the per locale data, create if not there.
     *
     * @param locale
     * @return
     */
    private synchronized final PerLocaleData get(CLDRLocale locale) {
        PerLocaleData pld = rLocales.getIfPresent(locale);
        if (pld == null) {
            Reference<PerLocaleData> ref = locales.get(locale);
            if (ref != null) {
                SurveyLog.debug("STFactory: " + locale + " was not in LRUMap.");
                pld = ref.get();
                if (pld == null) {
                    SurveyLog.debug("STFactory: " + locale + " was GC'ed." + SurveyMain.freeMem());
                    ref.clear();
                }
            }
            if (pld == null) {
                pld = new PerLocaleData(locale);
                rLocales.put(locale, pld);
                locales.put(locale, (new SoftReference<>(pld)));
                // update the locale display name cache.
                OutputFileManager.updateLocaleDisplayName(pld.getFile(true), locale);
            } else {
                rLocales.put(locale, pld); // keep it in the lru
            }
        }
        return pld;
    }

    private final PerLocaleData get(String locale) {
        return get(CLDRLocale.getInstance(locale));
    }

    public TestCache.TestResultBundle getTestResult(CLDRLocale loc, CheckCLDR.Options options) {
//        System.err.println("Fetching: " + options);
        return get(loc).getTestResultData(options);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.util.Factory#getMinimalDraftStatus()
     */
    @Override
    public DraftStatus getMinimalDraftStatus() {
        return DraftStatus.unconfirmed;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.util.Factory#getSourceDirectory()
     */
    @Override
    public File[] getSourceDirectories() {
        return sm.getDiskFactory().getSourceDirectories();
    }

    @Override
    public List<File> getSourceDirectoriesForLocale(String localeID) {
        return sm.getDiskFactory().getSourceDirectoriesForLocale(localeID);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.util.Factory#handleGetAvailable()
     */
    @Override
    protected Set<String> handleGetAvailable() {
        return sm.getDiskFactory().getAvailable();
    }

    private Map<CLDRLocale, Set<CLDRLocale>> subLocaleMap = new HashMap<>();
    Set<CLDRLocale> allLocales = null;

    /**
     * Cache..
     */
    @Override
    public Set<CLDRLocale> subLocalesOf(CLDRLocale forLocale) {
        Set<CLDRLocale> result = subLocaleMap.get(forLocale);
        if (result == null) {
            result = calculateSubLocalesOf(forLocale, getAvailableCLDRLocales());
            subLocaleMap.put(forLocale, result);
        }
        return result;
    }

    /**
     * Cache..
     */
    @Override
    public Set<CLDRLocale> getAvailableCLDRLocales() {
        if (allLocales == null) {
            allLocales = CLDRLocale.getInstance(getAvailable());
        }
        return allLocales;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.util.Factory#handleMake(java.lang.String, boolean,
     * org.unicode.cldr.util.CLDRFile.DraftStatus)
     */
    @Override
    protected CLDRFile handleMake(String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus) {
        return get(localeID).getFile(resolved);
    }

    public CLDRFile make(String loc) {
        return make(loc, true /* resolved */);
    }

    public CLDRFile make(CLDRLocale loc, boolean resolved) {
        return make(loc.getBaseName(), resolved);
    }

    public XMLSource makeSource(String localeID, boolean resolved) {
        if (localeID == null)
            return null; // ?!
        return get(localeID).makeSource(resolved);
    }

    /**
     * Make a "vetted" CLDRFile with more paths resolved, for generating VXML (vetted XML).
     *
     * See loadVoteValues for what exactly "more paths" means.
     *
     * This kind of CLDRFile should not be confused with ordinary (not-fully-vetted) files,
     * or re-used for anything other than vxml. Avoid mixing data for the two kinds of CLDRFile
     * in caches (such as rLocales).
     *
     * @param loc the CLDRLocale
     * @return the vetted CLDRFile with more paths resolved
     */
    public CLDRFile makeVettedFile(CLDRLocale loc) {
        PerLocaleData pld = get(loc.getBaseName());
        XMLSource xmlSource = pld.makeVettedSource();
        CLDRFile cldrFile = new CLDRFile(xmlSource);
        cldrFile.setSupplementalDirectory(getSupplementalDirectory());
        return cldrFile;
    }

    /**
     * Prepare statement. Args: locale Result: xpath,submitter,value
     *
     * @param conn
     * @return
     * @throws SQLException
     *
     * Called only by loadVoteValues.
     */
    private PreparedStatement openQueryByLocaleRW(Connection conn) throws SQLException {
        setupDB();
        /*
         * locale must be included in the SELECT list as well as the WHERE clause,
         * to prevent SQL exception in the special case where deleteRow is called on
         * the result set for votes for invalid (e.g., obsolete) paths. The query
         * "must select all primary keys from that table".
         */
        return DBUtils
            .prepareForwardUpdateable(conn, "SELECT xpath,submitter,value,locale," + VOTE_OVERRIDE + ",last_mod FROM " + DBUtils.Table.VOTE_VALUE
                + " WHERE locale = ?");
    }

    private PreparedStatement openPermVoteQuery(Connection conn) throws SQLException {
        setupDB();
        return DBUtils
            .prepareForwardUpdateable(conn, "SELECT xpath,value,last_mod FROM " + DBUtils.Table.LOCKED_XPATHS
                + " WHERE locale = ?");
    }

    private synchronized final void setupDB() {
        if (dbIsSetup)
            return;
        dbIsSetup = true; // don't thrash.
        String sql = "(none)"; // this points to
        Statement s = null;
        try (Connection conn = DBUtils.getInstance().getDBConnection();) {
            if (!DBUtils.hasTable(conn, DBUtils.Table.VOTE_VALUE.toString())) {
                /*
                 * CREATE TABLE cldr_votevalue ( locale VARCHAR(20), xpath INT
                 * NOT NULL, submitter INT NOT NULL, value BLOB );
                 *
                 * CREATE UNIQUE INDEX cldr_votevalue_unique ON cldr_votevalue
                 * (locale,xpath,submitter);
                 */
                s = conn.createStatement();

                sql = "create table " + DBUtils.Table.VOTE_VALUE + "( "
                    + "locale VARCHAR(20), "
                    + "xpath  INT NOT NULL, "
                    + "submitter INT NOT NULL, " + "value " + DBUtils.DB_SQL_UNICODE + ", "
                    + DBUtils.DB_SQL_LAST_MOD + ", "
                    + VOTE_OVERRIDE + " INT DEFAULT NULL, "
                    + " PRIMARY KEY (locale,submitter,xpath) " +

                    " )";
                // logger.info(sql);
                s.execute(sql);

                sql = "CREATE UNIQUE INDEX  " + DBUtils.Table.VOTE_VALUE + " ON " + DBUtils.Table.VOTE_VALUE + " (locale,xpath,submitter)";
                s.execute(sql);
                s.close();
                s = null; // don't close twice.
                conn.commit();
                System.err.println("Created table " + DBUtils.Table.VOTE_VALUE);
            }
            if (!DBUtils.hasTable(conn, DBUtils.Table.VOTE_VALUE_ALT.toString())) {
                s = conn.createStatement();
                String valueLen = DBUtils.db_Mysql ? "(750)" : "";
                sql = "create table " + DBUtils.Table.VOTE_VALUE_ALT + "( " + "locale VARCHAR(20), " + "xpath  INT NOT NULL, " + "value "
                    + DBUtils.DB_SQL_UNICODE + ", " +
                    // DBUtils.DB_SQL_LAST_MOD + " " +
                    " PRIMARY KEY (locale,xpath,value" + valueLen + ") " + " )";
                // logger.info(sql);
                s.execute(sql);

                sql = "CREATE UNIQUE INDEX  " + DBUtils.Table.VOTE_VALUE_ALT + " ON " + DBUtils.Table.VOTE_VALUE_ALT + " (locale,xpath,value" + valueLen + ")";
                s.execute(sql);
                // sql = "CREATE INDEX  " + DBUtils.Table.VOTE_VALUE_ALT +
                // " ON cldr_votevalue_alt (locale)";
                // s.execute(sql);
                s.close();
                s = null; // don't close twice.
                conn.commit();
                System.err.println("Created table " + DBUtils.Table.VOTE_VALUE_ALT);
            }
            if (!DBUtils.hasTable(conn, DBUtils.Table.VOTE_FLAGGED.toString())) {
                s = conn.createStatement();

                sql = "create table " + DBUtils.Table.VOTE_FLAGGED + "( " + "locale VARCHAR(20), " + "xpath  INT NOT NULL, "
                    + "submitter INT NOT NULL, " + DBUtils.DB_SQL_LAST_MOD + " "
                    + ", PRIMARY KEY (locale,xpath) " +
                    " )";
                // logger.info(sql);
                s.execute(sql);

                sql = "CREATE UNIQUE INDEX  " + DBUtils.Table.VOTE_FLAGGED + " ON " + DBUtils.Table.VOTE_FLAGGED + " (locale,xpath)";
                s.execute(sql);
                s.close();
                s = null; // don't close twice.
                conn.commit();
                System.err.println("Created table " + DBUtils.Table.VOTE_FLAGGED);
            }
            if (!DBUtils.hasTable(conn, DBUtils.Table.IMPORT.toString())) {
                /*
                 * Create the IMPORT table, for keeping track of imported old losing votes.
                 * Use DB_SQL_BINCOLLATE for compatibility with existing vote tables, which
                 * (on st.unicode.org as of 2018-11-08) have "DEFAULT CHARSET=latin1 COLLATE=latin1_bin".
                 */
                s = conn.createStatement();
                String valueLen = DBUtils.db_Mysql ? "(750)" : "";
                sql = "CREATE TABLE " + DBUtils.Table.IMPORT + "( " + "locale VARCHAR(20), " + "xpath INT NOT NULL, " + "value "
                    + DBUtils.DB_SQL_UNICODE + ", "
                    + " PRIMARY KEY (locale,xpath,value" + valueLen + ") " + " ) "
                    + DBUtils.DB_SQL_BINCOLLATE;
                s.execute(sql);

                sql = "CREATE UNIQUE INDEX  " + DBUtils.Table.IMPORT + " ON " + DBUtils.Table.IMPORT + " (locale,xpath,value" + valueLen + ")";
                s.execute(sql);
                s.close();
                s = null; // don't close twice.
                conn.commit();
                System.err.println("Created table " + DBUtils.Table.IMPORT);
            }
            if (!DBUtils.hasTable(conn, DBUtils.Table.IMPORT_AUTO.toString())) {
                /*
                 * Create the IMPORT_AUTO table, for keeping track of which users have auto-imported old winning votes.
                 * Use DB_SQL_BINCOLLATE for compatibility with existing vote tables, which
                 * (on st.unicode.org as of 2018-12-19) have "DEFAULT CHARSET=latin1 COLLATE=latin1_bin".
                 */
                s = conn.createStatement();
                sql = "CREATE TABLE " + DBUtils.Table.IMPORT_AUTO + "(userid INT NOT NULL, PRIMARY KEY (userid) ) "
                    + DBUtils.DB_SQL_BINCOLLATE;
                s.execute(sql);

                sql = "CREATE UNIQUE INDEX  " + DBUtils.Table.IMPORT_AUTO + " ON " + DBUtils.Table.IMPORT_AUTO + " (userid)";
                s.execute(sql);
                s.close();
                s = null; // don't close twice.
                conn.commit();
                System.err.println("Created table " + DBUtils.Table.IMPORT_AUTO);
            }
            String tableName = DBUtils.Table.LOCKED_XPATHS.toString();
            if (!DBUtils.hasTable(conn, tableName)) {
                /*
                 * Create the LOCKED_XPATHS table, for keeping track of paths that have been "locked" for specific locales.
                 * Reference: https://unicode-org.atlassian.net/browse/CLDR-11677
                 * Use DB_SQL_BINCOLLATE for compatibility with existing vote tables, which
                 * (on st.unicode.org as of 2020-01-07) have "DEFAULT CHARSET=latin1 COLLATE=latin1_bin".
                 */
                s = conn.createStatement();
                sql = "CREATE TABLE " + tableName
                    + "(locale VARCHAR(20), xpath INT NOT NULL, "
                    + "value " + DBUtils.DB_SQL_UNICODE + ", "
                    + DBUtils.DB_SQL_LAST_MOD
                    + ", PRIMARY KEY (locale,xpath))"
                    + DBUtils.DB_SQL_BINCOLLATE;
                s.execute(sql);
                s.close();
                s = null; // don't close twice.
                conn.commit();
                System.err.println("Created table " + tableName);
             }
        } catch (SQLException se) {
            SurveyLog.logException(logger, se, "SQL: " + sql);
            SurveyMain.busted("Setting up DB for STFactory, SQL: " + sql, se);
            throw new InternalError("Setting up DB for STFactory, SQL: " + sql);
        } finally {
            DBUtils.close(s);
        }
    }

    /**
     * Flag the specified xpath for review.
     * @param conn
     * @param locale
     * @param xpath
     * @param user
     * @throws SQLException
     * @return number of rows changed
     */
    public int setFlag(Connection conn, CLDRLocale locale, int xpath, User user) throws SQLException {
        PreparedStatement ps = null;
        try {
            synchronized (STFactory.class) {
                final Pair<CLDRLocale, Integer> theKey = new Pair<>(locale, xpath);
                final Set<Pair<CLDRLocale, Integer>> m = loadFlag();
                if (m.contains(theKey)) {
                    return 0; // already there.
                }
                m.add(theKey);
            } // make sure that the DB is loaded before we attempt to update.
            if (DBUtils.db_Mysql) {
                ps = DBUtils.prepareStatementWithArgs(conn, "INSERT IGNORE INTO " + DBUtils.Table.VOTE_FLAGGED +
                    " (locale,xpath,submitter) VALUES (?,?,?)", locale.toString(), xpath, user.id);
            } else {
                ps = DBUtils.prepareStatementWithArgs(conn, "INSERT INTO " + DBUtils.Table.VOTE_FLAGGED +
                    " (locale,xpath,submitter) VALUES (?,?,?)", locale.toString(), xpath, user.id);
            }
            int rv = ps.executeUpdate();
            return rv;
        } finally {
            DBUtils.close(ps);
        }
    }

    /**
     * Flag the specified xpath for review.
     * @param conn
     * @param locale
     * @param xpath
     * @param user
     * @throws SQLException
     * @return number of rows changed
     */
    public int clearFlag(Connection conn, CLDRLocale locale, int xpath, User user) throws SQLException {
        PreparedStatement ps = null;
        try {
            synchronized (STFactory.class) {
                loadFlag().remove(new Pair<>(locale, xpath));
            } // make sure DB is loaded before we attempt to update
            ps = DBUtils.prepareStatementWithArgs(conn, "DELETE FROM " + DBUtils.Table.VOTE_FLAGGED +
                " WHERE locale=? AND xpath=?", locale.toString(), xpath);
            int rv = ps.executeUpdate();
            return rv;
        } finally {
            DBUtils.close(ps);
        }
    }

    /**
     * Does the list of flags contain one for this locale and xpath?
     *
     * @param locale
     * @param xpath
     * @return true or false
     */
    public boolean getFlag(CLDRLocale locale, int xpath) {
        synchronized (STFactory.class) {
            return loadFlag().contains(new Pair<>(locale, xpath));
        }
    }

    public boolean haveFlags() {
        synchronized (STFactory.class) {
            return !(loadFlag().isEmpty());
        }
    }

    /**
     * Bottleneck for flag functions.
     * @return
     */
    private Set<Pair<CLDRLocale, Integer>> loadFlag() {
        if (flagList == null) {
            setupDB();

            flagList = new HashSet<>();

            logger.fine("Loading flagged items from .." + DBUtils.Table.VOTE_FLAGGED);
            try {
                for (Map<String, Object> r : DBUtils.queryToArrayAssoc("select * from " + DBUtils.Table.VOTE_FLAGGED)) {
                    flagList.add(new Pair<>(CLDRLocale.getInstance(r.get("locale").toString()),
                        (Integer) r.get("xpath")));
                }
                if (flagList.isEmpty()) {
                    // quell loading of empty votes
                    logger.fine("Loaded " + flagList.size() + " items into flagged list.");
                } else {
                    logger.info("Loaded " + flagList.size() + " items into flagged list.");

                }
            } catch (SQLException sqe) {
                SurveyMain.busted("loading flagged votes from " + DBUtils.Table.VOTE_FLAGGED, sqe);
            } catch (IOException ioe) {
                SurveyMain.busted("loading flagged votes from " + DBUtils.Table.VOTE_FLAGGED, ioe);
            }
        }
        return flagList;
    }

    /**
     * In memory cache.
     */
    private Set<Pair<CLDRLocale, Integer>> flagList = null;

    /**
     * Close and re-open the factory. For testing only!
     *
     * @return
     */
    public STFactory TESTING_shutdownAndRestart() {
        sm.TESTING_removeSTFactory();
        return sm.getSTFactory();
    }

    @Override
    public synchronized void handleUserChanged(User u) {
        VoteResolver.setVoterToInfo(sm.reg.getVoterToInfo());
    }

    public final PathHeader getPathHeader(String xpath) {
        try {
            return phf.fromPath(xpath);
        } catch (Throwable t) {
            SurveyLog.warnOnce(logger, "PH for path " + xpath + t.toString());
            return null;
        }
    }

    private SurveyMenus surveyMenus = null;

    public final synchronized SurveyMenus getSurveyMenus() {
        if (surveyMenus == null) {
            try (CLDRProgressTask progress = sm.openProgress("STFactory: setup surveymenus")) {
                progress.update("setup surveymenus");
                surveyMenus = new SurveyMenus(this, phf);
            }
        }
        return surveyMenus;
    }

    /**
     * Resolving disk file, or null if none.
     *
     * @param locale
     * @return
     */
    public CLDRFile getDiskFile(CLDRLocale locale) {
        return sm.getDiskFactory().make(locale.getBaseName(), true);
    }

    /**
     * Return all xpaths for this locale. uses CLDRFile iterator, etc
     * @param locale
     * @return
     */
    public Set<String> getPathsForFile(CLDRLocale locale) {
        return get(locale).getPathsForFile();
    }

    /**
     * Get paths for file matching a prefix. Does not cache.
     * @param locale
     * @param xpathPrefix
     * @return
     */
    public Set<String> getPathsForFile(CLDRLocale locale, String xpathPrefix) {
        Set<String> ret = new HashSet<>();
        for (String s : getPathsForFile(locale)) {
            if (s.startsWith(xpathPrefix)) {
                ret.add(s);
            }
        }
        return ret;
    }

    /*
     * votes sometime table
     *
     * DERBY create table cldr_v22submission ( xpath integer not null, locale
     * varchar(20) ); create unique index cldr_v22submission_uq on
     * cldr_v22submission ( xpath, locale );
     *
     * insert into cldr_v22submission select distinct
     * cldr_votevalue.xpath,cldr_votevalue.locale from cldr_votevalue where
     * cldr_votevalue.value is not null;
     *
     *
     * MYSQL drop table if exists cldr_v22submission; create table
     * cldr_v22submission ( primary key(xpath,locale),key(locale) ) select
     * distinct cldr_votevalue.xpath,cldr_votevalue.locale from cldr_votevalue
     * where cldr_votevalue.value is not null;
     */
    public CLDRFile makeProposedFile(CLDRLocale locale) {

        Connection conn = null;
        PreparedStatement ps = null; // all for mysql, or 1st step for derby
        ResultSet rs = null;
        SimpleXMLSource sxs = new SimpleXMLSource(locale.getBaseName());
        try {
            conn = DBUtils.getInstance().getAConnection();

            ps = DBUtils.prepareStatementWithArgsFRO(conn, "select xpath,submitter,value," + VOTE_OVERRIDE + " from " + DBUtils.Table.VOTE_VALUE
                + " where locale=? and value IS NOT NULL", locale);

            rs = ps.executeQuery();
            while (rs.next()) {
                String xp = sm.xpt.getById(rs.getInt(1));
                int sub = rs.getInt(2);
                Integer voteValue = rs.getInt(4);
                if (voteValue == 0 && rs.wasNull()) {
                    voteValue = null;
                }

                StringBuilder sb = new StringBuilder(xp);
                String alt = null;
                if (xp.contains("[@alt")) {
                    alt = XPathTable.getAlt(xp);
                    sb = new StringBuilder(XPathTable.removeAlt(xp)); // replace
                }

                sb.append("[@alt=\"");
                if (alt != null) {
                    sb.append(alt);
                    sb.append('-');
                }
                XPathTable.appendAltProposedPrefix(sb, sub, voteValue);
                sb.append("\"]");

                // value is never null, due to SQL
                sxs.putValueAtPath(sb.toString(), DBUtils.getStringUTF8(rs, 3));
            }

            CLDRFile f = new CLDRFile(sxs);
            return f;
        } catch (SQLException e) {
            SurveyLog.logException(logger, e, "In makeProposedFile");
            SurveyMain.busted("Could not read locale " + locale, e);
            throw new InternalError("Could not load locale " + locale + " : " + DBUtils.unchainSqlException(e));
        } finally {
            DBUtils.close(rs, ps, conn);
        }
    }

    /**
     * Read back a dir full of pxml files
     *
     * @param sm
     * @param inFile
     *            dir containing pxmls
     * @return
     */
    public Integer[] readPXMLFiles(final File inFileList[]) {
        int nusers = 0;
        if (CLDRConfig.getInstance().getEnvironment() != Environment.SMOKETEST) {
            throw new InternalError("Error: can only do this in SMOKETEST"); // insanity
            // check
        }

        Vector<Integer> ret = new Vector<>();

        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        try { // do this in 1 transaction. just in case.
            conn = DBUtils.getInstance().getDBConnection();

            ps = DBUtils.prepareStatementWithArgs(conn, "delete from " + DBUtils.Table.VOTE_VALUE);
            int del = ps.executeUpdate();
            ps2 = DBUtils.prepareStatementWithArgs(conn, "delete from " + DBUtils.Table.VOTE_VALUE_ALT);
            del += ps2.executeUpdate();
            System.err.println("DELETED " + del + "regular votes .. reading from files");

            XMLFileReader myReader = new XMLFileReader();

            DBUtils.close(ps2);
            final PreparedStatement myInsert = ps2 = DBUtils.prepareStatementForwardReadOnly(conn, "myInser", "INSERT INTO  "
                + DBUtils.Table.VOTE_VALUE + " (locale,xpath,submitter,value," + VOTE_OVERRIDE + ") VALUES (?,?,?,?) ");
            final SurveyMain sm2 = sm;
            myReader.setHandler(new XMLFileReader.SimpleHandler() {

                @Override
                public void handlePathValue(String path, String value) {
                    String alt = XPathTable.getAlt(path);

                    if (alt == null || !alt.contains(XPathTable.PROPOSED_U)) {
                        return; // not an alt proposed
                    }
                    String altParts[] = LDMLUtilities.parseAlt(alt);
                    StringBuilder newPath = new StringBuilder(XPathTable.removeAlt(path));
                    if (altParts[0] != null) {
                        newPath.append("[@alt=\"" + altParts[0] + "\"]");
                    }

                    try {
                        myInsert.setInt(2, sm2.xpt.getByXpath(newPath.toString()));
                        Integer voteValueArray[] = new Integer[1];
                        // TODO: need to handle a string like 'proposed-u8v4-' to re-introduce the voting override at vote level 4.
                        if (true) throw new InternalError("TODO: don't know how to handle voting overrides on read-in");
                        //myInsert.setInt(3, XPathTable.altProposedToUserid(altParts[1], voteValueArray));
                        DBUtils.setStringUTF8(myInsert, 4, value);
                        myInsert.executeUpdate();
                    } catch (SQLException e) {
                        SurveyLog.logException(logger, e, "importing  - " + path + " = " + value);
                        throw new IllegalArgumentException(e);
                    }
                }
            });

            for (File inFile : inFileList) {
                System.out.println("Reading pxmls from " + inFile.getAbsolutePath());
                for (File theFile : inFile.listFiles()) {
                    if (!theFile.isFile())
                        continue;
                    CLDRLocale loc = SurveyMain.getLocaleOf(theFile.getName());
                    System.out.println("Reading: " + loc + " from " + theFile.getAbsolutePath());
                    myInsert.setString(1, loc.getBaseName());
                    myReader.read(theFile.getAbsolutePath(), -1, false);
                    nusers++;
                }
                ret.add(nusers); // add to the list
                System.out.println("  .. read " + nusers + "  pxmls from " + inFile.getAbsolutePath());
                nusers = 0;
            }

            conn.commit();
        } catch (SQLException e) {
            SurveyLog.logException(logger, e, "importing locale data from files");
        } finally {
            DBUtils.close(ps2, ps, conn);
        }
        return ret.toArray(new Integer[2]);
    }

    /**
     * get the bundle for testing against on-disk data.
     * @return
     */
    private TestResultBundle getDiskTestBundle(CLDRLocale locale) {
        synchronized (gDiskTestCache) {
            TestResultBundle q;
            q = gDiskTestCache.getBundle(new CheckCLDR.Options(locale, SurveyMain.getTestPhase(), null, null));
            return q;
        }
    }

    /**
     * Return the table for old votes
     */
    public static final String getLastVoteTable() {
        final String dbName = DBUtils.Table.VOTE_VALUE.forVersion(SurveyMain.getLastVoteVersion(), false).toString();
        return dbName;
    }

    /**
     * Utility function to calculate CLDRFile.Status
     * @param cldrFile the CLDR File to use
     * @param diskFile the 'baseline' file to use
     * @param path xpath
     * @param value current string value
     * @return
     */
    public static final Status calculateStatus(CLDRFile cldrFile, CLDRFile diskFile, String path) {
        String fullXPath = cldrFile.getFullXPath(path);
        if (fullXPath == null) {
            fullXPath = path;
        }
        XPathParts xpp = XPathParts.getFrozenInstance(fullXPath);
        String draft = xpp.getAttributeValue(-1, LDMLConstants.DRAFT);
        Status status = draft == null ? Status.approved : VoteResolver.Status.fromString(draft);

        /*
         * Reset to missing if the value is inherited from root or code-fallback, unless the XML actually
         * contains INHERITANCE_MARKER. Pass false for skipInheritanceMarker so that status will not be
         * missing for explicit INHERITANCE_MARKER. Reference: https://unicode.org/cldr/trac/ticket/11857
         */
        final String srcid = cldrFile.getSourceLocaleIdExtended(path, null, false /* skipInheritanceMarker */);
        if (srcid.equals(XMLSource.CODE_FALLBACK_ID)) {
            status = Status.missing;
        } else if (srcid.equals("root")) {
            if (!srcid.equals(diskFile.getLocaleID())) {
                status = Status.missing;
            }
        }
        return status;
    }
}
