/** */
package org.unicode.cldr.web;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.text.NumberFormat;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.TestCache;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.Level;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.VoteType;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;
import org.unicode.cldr.web.SurveyException.ErrorCode;
import org.unicode.cldr.web.UserRegistry.ModifyDenial;
import org.unicode.cldr.web.UserRegistry.User;

/**
 * @author srl
 */
public class STFactory extends Factory implements BallotBoxFactory<UserRegistry.User> {
    /** Q: Do we want different loggers for the multiplicity of inner classes? */
    static final Logger logger = SurveyLog.forClass(STFactory.class);

    enum VoteLoadingContext {
        /**
         * The ordinary context when loadVoteValues is called by makeSource, such as when displaying
         * the main vetting view in Survey Tool
         */
        ORDINARY_LOAD_VOTES,

        /**
         * The special context when loadVoteValues is called by makeVettedSource for generating VXML
         */
        VXML_GENERATION,

        /**
         * The context when a voting (or abstaining) event occurs and setValueFromResolver is called
         * by voteForValue (not used for loadVoteValues)
         */
        SINGLE_VOTE,
    }

    /** Names of some columns in DBUtils.Table.VOTE_VALUE */
    private static final String VOTE_OVERRIDE = "vote_override";

    private static final String VOTE_TYPE = "vote_type";

    /**
     * the STFactory maintains exactly one instance of this class per locale it is working with. It
     * contains the XMLSource, Example Generator, etc..
     *
     * @author srl
     */
    public final class PerLocaleData implements Comparable<PerLocaleData>, BallotBox<User> {
        /** Locale of this PLD */
        private final CLDRLocale locale;
        /** For readonly locales, there's no DB */
        private final boolean readonly;
        /** Stamp that tracks if this locale has been modified (by a vote) */
        private final MutableStamp stamp;
        /** unresolved XMLSource for on-disk data. */
        private final XMLSource diskData;
        /** resolved CLDRFile backed by disk data */
        private final CLDRFile diskFile;
        /** unresolved XMLSource backed by the DB, or null for readonly */
        private final BallotBoxXMLSource<User> dataBackedSource;
        /** unresolved XMLSource: == dataBackedSource, or for readonly == diskData */
        private final XMLSource xmlsource;
        /** Unresolved CLDRFile backed by {@link #xmlsource} */
        private final CLDRFile file;
        /** Resolved CLDRFile backed by {@link #xmlsource} */
        private final CLDRFile rFile;
        /** List of all XPaths present */
        private Set<String> pathsForFile;
        /** which XPaths had votes? */
        BitSet votesSometimeThisRelease = null;
        /** Voting information for each XPath */
        private final Map<String, PerXPathData> xpathToData = new HashMap<>();

        public void nextStamp() {
            stamp.next();
        }

        /** Per-xpath data. There's one of these per xpath- voting data, etc. */
        final class PerXPathData {
            /**
             * Per (voting) user data. For each xpath, there's one of these per user that is voting.
             *
             * @author srl
             */
            private final class PerUserData {
                /** What is this user voting for? */
                String vote;
                /** What is this user's override strength? */
                Integer override;

                Date when;

                VoteType voteType;

                public PerUserData(
                        String value, Integer voteOverride, Date when, VoteType voteType) {
                    this.vote = value;
                    this.override = voteOverride;
                    this.when = when;
                    this.voteType = voteType;
                    if (voteType == null || voteType == VoteType.NONE) {
                        logger.warning(
                                "PerUserData got vote type " + voteType + "; changed to UNKNOWN");
                        voteType = VoteType.UNKNOWN;
                    }
                    if (lastModDate == null || lastModDate.before(when)) {
                        lastModDate = when;
                    }
                }

                /**
                 * Has this user overridden their vote? Integer with override strength, null for no
                 * override
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

                public VoteType getVoteType() {
                    return voteType;
                }
            }

            Date lastModDate = null;
            Set<String> otherValues = null;
            Map<User, PerUserData> userToData = null;

            /**
             * Is there any user data (votes)?
             *
             * @return
             */
            public boolean isEmpty() {
                return userToData == null || userToData.isEmpty();
            }

            /**
             * Get all votes
             *
             * @return
             */
            public Iterable<Entry<User, PerUserData>> getVotes() {
                return userToData.entrySet();
            }

            /**
             * Get the set of other values. May be null.
             *
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
                if (ts.isEmpty()) return null;
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

            private void setVoteForValue(
                    User user, String value, Integer voteOverride, Date when, VoteType voteType) {
                if (value != null) {
                    setPerUserData(user, new PerUserData(value, voteOverride, when, voteType));
                } else {
                    removePerUserData(user);
                }
            }

            private void removePerUserData(User user) {
                if (userToData != null) {
                    userToData.remove(user);
                    if (userToData.isEmpty()) {
                        lastModDate = null; // date is now null- object is empty
                    }
                }
            }

            /**
             * Remove all votes from this PerXPathData that match the given count for getOverride.
             *
             * @param overrideVoteCount
             */
            private void removeOverrideVotes(int overrideVoteCount) {
                if (userToData != null) {
                    HashSet<User> toDelete = new HashSet<>();
                    userToData.forEach(
                            (k, v) -> {
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
             *
             * @param user
             * @param pud
             */
            private void setPerUserData(User user, PerUserData pud) {
                if (userToData == null) {
                    userToData = new ConcurrentHashMap<>();
                }
                userToData.put(user, pud);
            }

            /**
             * Did this user vote?
             *
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

            public VoteType getUserVoteType(User myUser) {
                if (userToData == null) {
                    return VoteType.NONE;
                }
                PerUserData pud = peekUserToData(myUser);
                if (pud == null) {
                    return VoteType.NONE;
                }
                return pud.getVoteType();
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

        /**
         * Constructor is called from the 'locales' cache, and in turn by STFactory.get() All parent
         * locales have already been initialized.
         *
         * <p>It's important that handleMake() not be called from this constructor, as that will
         * cause reentrancy.
         *
         * <p>The task before us is to initialize all XMLSources and CLDRFiles needed.
         *
         * @param locale
         */
        PerLocaleData(CLDRLocale locale) {
            logger.info("Load: " + locale);
            this.locale = locale;
            readonly = isReadOnlyLocale(locale);
            diskData = sm.getDiskFactory().makeSource(locale.getBaseName()).freeze();
            sm.xpt.loadXPaths(diskData);
            diskFile = sm.getDiskFactory().make(locale.getBaseName(), true).freeze();
            pathsForFile = phf.pathsForFile(diskFile);
            stamp = mintLocaleStamp(locale);

            if (readonly) {
                rFile = diskFile;
                xmlsource = diskData;

                // null for readonly
                dataBackedSource = null;
            } else {
                xmlsource =
                        dataBackedSource =
                                new BallotBoxXMLSource<User>(diskData.cloneAsThawed(), this);
                registerXmlSource(dataBackedSource);
                loadVoteValues(dataBackedSource, VoteLoadingContext.ORDINARY_LOAD_VOTES);
                nextStamp();
                XMLSource resolvedXmlsource = makeResolvingSource();
                rFile =
                        new CLDRFile(resolvedXmlsource)
                                .setSupplementalDirectory(getSupplementalDirectory());
            }
            file = new CLDRFile(xmlsource).setSupplementalDirectory(getSupplementalDirectory());
        }

        /** Create a new ResolvingSource for this PLD and all parents */
        private XMLSource makeResolvingSource() {
            List<XMLSource> sourceList = new ArrayList<>();
            // add this and parents
            addXMLSources(sourceList);
            logger.finest(
                    () ->
                            "makeResolvingSource() sourceList: "
                                    + sourceList.stream()
                                            .map(l -> l.getLocaleID())
                                            .collect(Collectors.joining("»")));
            return registerXmlSource(new XMLSource.ResolvingSource(sourceList));
        }

        void addXMLSources(List<XMLSource> sourceList) {
            sourceList.add(xmlsource); // DB or disk file
            CLDRLocale parent = locale.getParent();
            // recurse with parents
            if (parent != null) {
                get(parent).addXMLSources(sourceList);
            }
        }

        /**
         * Get all of the PerXPathData paths
         *
         * @return
         */
        public Set<String> allPXDPaths() {
            return xpathToData.keySet();
        }

        public Stamp getStamp() {
            return stamp;
        }

        /**
         * @param user - The user voting on the path
         * @param xpath - The xpath being voted on.
         * @return true - If pathHeader and coverage would indicate a value that the user should
         *     have been able to vote on.
         */
        private boolean isValidSurveyToolVote(UserRegistry.User user, String xpath) {
            PathHeader ph = getPathHeader(xpath);
            if (ph == null) return false;
            if (ph.getSurveyToolStatus() == PathHeader.SurveyToolStatus.DEPRECATED) return false;
            if (ph.getSurveyToolStatus() == PathHeader.SurveyToolStatus.HIDE
                    || ph.getSurveyToolStatus() == PathHeader.SurveyToolStatus.READ_ONLY) {
                if (!UserRegistry.userIsTC(user)) return false;
            }

            if (sm.getSupplementalDataInfo().getCoverageValue(xpath, locale.getBaseName())
                    > org.unicode.cldr.util.Level.COMPREHENSIVE.getLevel()) {
                return false;
            }
            return true;
        }

        /**
         * Load internal data (votes, etc.) for this PerLocaleData, and push it into the given
         * DataBackedSource.
         *
         * @param targetXmlSource the DataBackedSource which might or might not equal
         *     this.xmlsource; for makeVettedSource, it is a different (uncached) DataBackedSource.
         * @param voteLoadingContext VoteLoadingContext.ORDINARY_LOAD_VOTES or
         *     VoteLoadingContext.VXML_GENERATION (not VoteLoadingContext.SINGLE_VOTE)
         *     <p>Called by PerLocaleData.makeSource (with VoteLoadingContext.ORDINARY_LOAD_VOTES)
         *     and by PerLocaleData.makeVettedSource (with VoteLoadingContext.VXML_GENERATION).
         */
        private void loadVoteValues(
                BallotBoxXMLSource<User> targetXmlSource, VoteLoadingContext voteLoadingContext) {
            VoteResolver<String> resolver = null; // save recalculating this.
            ElapsedTimer et =
                    (SurveyLog.DEBUG) ? new ElapsedTimer("Loading PLD for " + locale) : null;
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            int n = 0;
            int del = 0;

            try {
                /*
                 * Select several columns (xp, submitter, value, override, last_mod, vote_type),
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
                    VoteType voteType = VoteType.fromId(rs.getInt(7)); // vote_type
                    User theSubmitter = sm.reg.getInfo(submitter);
                    if (theSubmitter == null) {
                        SurveyLog.warnOnce(logger, "Ignoring votes for deleted user #" + submitter);
                    }
                    if (!UserRegistry.countUserVoteForLocale(
                            theSubmitter, locale)) { // check user permission to submit
                        continue;
                    }
                    if (!isValidSurveyToolVote(
                            theSubmitter, xpath)) { // Make sure it is a visible path
                        continue;
                    }
                    try {
                        if (voteType == null || voteType == VoteType.NONE) {
                            logger.warning(
                                    "loadVoteValues got vote type "
                                            + voteType
                                            + "; changed to UNKNOWN");
                            voteType = VoteType.UNKNOWN;
                        }
                        internalSetVoteForValue(
                                theSubmitter, xpath, value, voteOverride, last_mod, voteType);
                        n++;
                    } catch (BallotBox.InvalidXPathException e) {
                        logger.severe(
                                "InvalidXPathException: Deleting vote for "
                                        + theSubmitter
                                        + ":"
                                        + locale
                                        + ":"
                                        + xpath);
                        rs.deleteRow();
                        del++;
                    }
                }
                if (del > 0) {
                    logger.warning("Summary: delete of " + del + " invalid votes from " + locale);
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
                        internalSetVoteForValue(
                                sm.reg.getInfo(UserRegistry.ADMIN_ID),
                                xpath,
                                value,
                                VoteResolver.Level.LOCKING_VOTES,
                                last_mod,
                                VoteType.DIRECT);
                        n++;
                    } catch (BallotBox.InvalidXPathException e) {
                        System.err.println(
                                "InvalidXPathException: Ignoring permanent vote for:"
                                        + locale
                                        + ":"
                                        + xpath);
                    }
                }
            } catch (SQLException e) {
                SurveyLog.logException(logger, e, "In setValueFromResolver");
                SurveyMain.busted("Could not read locale " + locale, e);
                throw new InternalError(
                        "Could not load locale " + locale + " : " + DBUtils.unchainSqlException(e));
            } finally {
                DBUtils.close(rs, ps, conn);
            }
            SurveyLog.debug(et + " - read " + n + " items  (" + xpathToData.size() + " xpaths.)");

            et =
                    (SurveyLog.DEBUG)
                            ? new ElapsedTimer("Resolver loading for xpaths in " + locale)
                            : null;
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
                    resolver =
                            targetXmlSource.setValueFromResolver(
                                    xp, resolver, voteLoadingContext, peekXpathData(xp));
                } catch (Exception e) {
                    e.printStackTrace();
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

        public CLDRFile getFile(boolean resolved) {
            return (resolved) ? rFile : file;
        }

        /**
         * Create or update a VoteResolver for this item
         *
         * @param perXPathData map of users to vote values
         * @param path xpath voted on
         * @param r if non-null, resolver to re-use.
         * @return the new or updated resolver
         *     <p>This function is called by getResolver, and may also call itself recursively.
         */
        private VoteResolver<String> getResolverInternal(
                PerXPathData perXPathData, String path, VoteResolver<String> r) {
            if (path == null) {
                throw new IllegalArgumentException("path must not be null");
            }
            if (r == null) {
                r = new VoteResolver<>(sm.reg.getVoterInfoList()); // create
            } else {
                r.clear(); // reuse
            }

            r.enableTranscript();

            // Set established locale
            r.setLocale(locale, getPathHeader(path));

            // set current Trunk (baseline) value (if present)
            final String currentValue = diskData.getValueAtDPath(path);
            final Status currentStatus = VoteResolver.calculateStatus(diskFile, path);
            r.setBaseline(currentValue, currentStatus);
            r.add(currentValue);

            /** Note that rFile may not have all votes filled in yet as we're in startup phase */
            final CLDRFile baseFile = (rFile != null) ? rFile : diskFile;
            r.setBaileyValue(baseFile.getBaileyValue(path, null, null));

            // add each vote
            if (perXPathData != null && !perXPathData.isEmpty()) {
                for (Entry<User, PerLocaleData.PerXPathData.PerUserData> e :
                        perXPathData.getVotes()) {
                    PerLocaleData.PerXPathData.PerUserData v = e.getValue();
                    r.add(
                            v.getValue(), // user's vote
                            e.getKey().id,
                            v.getOverride(),
                            v.getWhen()); // user's id
                }
            }
            return r;
        }

        @Override
        public VoteResolver<String> getResolver(String path) {
            return getResolver(peekXpathData(path), path, null);
        }

        /**
         * called by getResolver()
         *
         * @param perXPathData
         * @param path
         * @param r
         * @return
         */
        public VoteResolver<String> getResolver(
                PerXPathData perXPathData, String path, VoteResolver<String> r) {
            try {
                r = getResolverInternal(perXPathData, path, r);
            } catch (VoteResolver.UnknownVoterException uve) {
                sm.reg.userModified(); // try reloading user table
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
        public VoteResolver<String> getResolver(String path, VoteResolver<String> r) {
            return getResolver(peekXpathData(path), path, r);
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

            if (ts.isEmpty()) return null; // return null if empty

            return ts;
        }

        /**
         * Return data for this xpath if available - don't create it.
         *
         * @param xpath
         * @return
         */
        private PerXPathData peekXpathData(String xpath) {
            return xpathToData.get(xpath);
        }

        /**
         * Get the PerXPathData for the given xpath, for this PerLocaleData; create per-xpath data
         * if not there.
         *
         * @param xpath the path string, like
         *     "//ldml/localeDisplayNames/languages/language[@type="ko"]"
         * @return the PerXPathData
         *     <p>Called by internalSetVoteForValue only.
         */
        private PerXPathData getXPathData(String xpath) {
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

        /**
         * Make a vetted source for this PerLocaleData, suitable for producing vxml with
         * vote-resolution done on more paths.
         *
         * <p>This function is similar to makeSource, but with VoteLoadingContext.VXML_GENERATION.
         *
         * @return the DataBackedSource (NOT the same as PerLocaleData.xmlsource)
         */
        private synchronized XMLSource makeVettedSource() {
            BallotBoxXMLSource<User> vxmlSource =
                    new BallotBoxXMLSource<User>(diskData.cloneAsThawed(), this);
            if (!readonly) {
                loadVoteValues(vxmlSource, VoteLoadingContext.VXML_GENERATION);
            }
            return vxmlSource;
        }

        @Override
        public void unvoteFor(User user, String distinguishingXpath)
                throws BallotBox.InvalidXPathException, VoteNotAcceptedException {
            voteForValue(user, distinguishingXpath, null);
        }

        @Override
        public void revoteFor(User user, String distinguishingXpath)
                throws BallotBox.InvalidXPathException, VoteNotAcceptedException {
            String oldValue = getVoteValue(user, distinguishingXpath);
            voteForValue(user, distinguishingXpath, oldValue);
        }

        @Override
        public void voteForValue(User user, String distinguishingXpath, String value)
                throws InvalidXPathException, VoteNotAcceptedException {
            voteForValueWithType(user, distinguishingXpath, value, null, VoteType.DIRECT);
        }

        @Override
        public void voteForValueWithType(
                User user, String distinguishingXpath, String value, VoteType voteType)
                throws VoteNotAcceptedException, InvalidXPathException {
            voteForValueWithType(user, distinguishingXpath, value, null, voteType);
        }

        @Override
        public void voteForValueWithType(
                User user,
                String distinguishingXpath,
                String value,
                Integer withVote,
                VoteType voteType)
                throws BallotBox.InvalidXPathException, BallotBox.VoteNotAcceptedException {
            makeSureInPathsForFile(distinguishingXpath, user, value);
            value = reviseInheritanceAsNeeded(distinguishingXpath, value);
            SurveyLog.debug(
                    "V4v: "
                            + locale
                            + " "
                            + distinguishingXpath
                            + " : "
                            + user
                            + " voting for '"
                            + value
                            + "'");
            /*
             * this has to do with changing a vote - not counting it.
             */
            ModifyDenial denial = UserRegistry.userCanModifyLocaleWhy(user, locale);
            if (denial != null) {
                throw new VoteNotAcceptedException(
                        ErrorCode.E_NO_PERMISSION,
                        "User " + user + " cannot modify " + locale + " " + denial);
            }

            int xpathId = sm.xpt.getByXpath(distinguishingXpath);
            if (withVote != null) {
                Level level = user.getLevel();
                if (withVote == level.getVotes(user.getOrganization())) {
                    withVote = null; // not an override
                } else if (!level.canVoteWithCount(user.getOrganization(), withVote)) {
                    throw new VoteNotAcceptedException(
                            ErrorCode.E_NO_PERMISSION,
                            "User " + user + " cannot vote at " + withVote + " level ");
                } else if (withVote == VoteResolver.Level.PERMANENT_VOTES) {
                    if (sm.fora.postCountFor(locale, xpathId) < 1) {
                        throw new VoteNotAcceptedException(
                                ErrorCode.E_PERMANENT_VOTE_NO_FORUM,
                                "Forum entry is required for Permanent vote");
                    }
                }
            }

            // check for too-long
            if (value != null) {
                final int valueLimit = SurveyMain.localeSizer.getSize(locale, distinguishingXpath);
                final int valueLength = value.length();
                if (valueLength > valueLimit) {
                    NumberFormat nf = NumberFormat.getInstance();
                    throw new VoteNotAcceptedException(
                            ErrorCode.E_BAD_VALUE,
                            "Length "
                                    + nf.format(valueLength)
                                    + " exceeds limit of "
                                    + nf.format(valueLimit)
                                    + " - please file a bug if you need a longer value.");
                }
            }

            String oldVal = dataBackedSource.getValueAtDPath(distinguishingXpath);
            String oldFullPath = dataBackedSource.getFullPathAtDPath(distinguishingXpath);

            // sanity check, should have been caught before
            if (readonly) {
                readonly();
                return;
            }

            // small critical section for actual vote
            synchronized (this) {
                saveVoteToDb(user, distinguishingXpath, value, withVote, xpathId, voteType);

                internalSetVoteForValue(
                        user, distinguishingXpath, value, withVote, new Date(), voteType);

                if (withVote != null && withVote == VoteResolver.Level.PERMANENT_VOTES) {
                    doPermanentVote(distinguishingXpath, xpathId, value);
                }

                dataBackedSource.setValueFromResolver(
                        distinguishingXpath,
                        null,
                        VoteLoadingContext.SINGLE_VOTE,
                        peekXpathData(distinguishingXpath));
            }

            String newVal = dataBackedSource.getValueAtDPath(distinguishingXpath);
            String newFullPath = dataBackedSource.getFullPathAtDPath(distinguishingXpath);
            if (newVal != null && (!newVal.equals(oldVal) || !oldFullPath.equals(newFullPath))) {
                dataBackedSource.notifyListeners(distinguishingXpath);
            }
        }

        /**
         * Get the possibly modified value. If value matches the bailey value or inheritance marker,
         * possibly change it from bailey value to inheritance marker, or vice-versa, as needed to
         * meet requirements described and implemented in VoteResolver.
         *
         * @param path the path
         * @param value the input value
         * @return the possibly modified value
         */
        private String reviseInheritanceAsNeeded(String path, String value) {
            if (value != null) {
                CLDRFile cldrFile = getFile(true);
                if (cldrFile == null) {
                    throw new InternalCldrException("getFile failure in reviseInheritanceAsNeeded");
                }
                value = VoteResolver.reviseInheritanceAsNeeded(path, value, cldrFile);
            }
            return value;
        }

        /**
         * If the path is not in pathsForFile, then if the user has permission, add the path, else
         * throw an exception
         *
         * <p>Normally when a user votes, the path needs already to exist in pathsForFile. As a
         * special exception, TC/Admin users can add new "alt" paths with null (abstain) vote.
         *
         * @param xpath the path in question
         * @param user the user who is voting
         * @param value the value they're voting for -- must be null for TC exception
         * @throws InvalidXPathException
         */
        private void makeSureInPathsForFile(String xpath, User user, String value)
                throws InvalidXPathException {
            if (!getPathsForFile().contains(xpath)) {
                if (value == null
                        && UserRegistry.userIsTC(user)
                        && XPathTable.getAlt(xpath) != null) {
                    synchronized (this) {
                        Set<String> set = new HashSet<>(pathsForFile);
                        set.add(xpath);
                        pathsForFile = Collections.unmodifiableSet(set);
                    }
                } else {
                    throw new BallotBox.InvalidXPathException(xpath);
                }
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
        private void saveVoteToDb(
                final User user,
                final String distinguishingXpath,
                final String value,
                final Integer withVote,
                final int xpathId,
                VoteType voteType) {
            boolean didClearFlag = false;
            ElapsedTimer et =
                    !SurveyLog.DEBUG
                            ? null
                            : new ElapsedTimer(
                                    "{0} Recording PLD for "
                                            + locale
                                            + " "
                                            + distinguishingXpath
                                            + " : "
                                            + user
                                            + " voting for '"
                                            + value);
            Connection conn = null;
            PreparedStatement saveOld = null; // save off old value
            PreparedStatement ps = null;
            final boolean wasFlagged = getFlag(locale, xpathId); // do this outside of the txn..
            int submitter = user.id;
            try {
                conn = DBUtils.getInstance().getDBConnection();

                String add0 = "", add1 = "", add2 = "";

                // #1 - save the "VOTE_VALUE_ALT"  ( possible proposal) value.
                if (DBUtils.db_Mysql) {
                    add0 = "IGNORE";
                } else {
                    throw new RuntimeException("Unexpected db type, expected " + DBUtils.db_Mysql);
                }
                String sql =
                        "insert "
                                + add0
                                + " into "
                                + DBUtils.Table.VOTE_VALUE_ALT
                                + " "
                                + add1
                                + " select "
                                + DBUtils.Table.VOTE_VALUE
                                + ".locale,"
                                + DBUtils.Table.VOTE_VALUE
                                + ".xpath,"
                                + DBUtils.Table.VOTE_VALUE
                                + ".value "
                                + " from "
                                + DBUtils.Table.VOTE_VALUE
                                + " where locale=? and xpath=? and submitter=? and value is not null "
                                + add2;
                saveOld =
                        DBUtils.prepareStatementWithArgs(
                                conn, sql, locale.getBaseName(), xpathId, user.id);
                saveOld.executeUpdate();

                // #2 - save the actual vote.
                ps =
                        DBUtils.prepareForwardReadOnly(
                                conn,
                                "INSERT INTO "
                                        + DBUtils.Table.VOTE_VALUE
                                        + " (locale,xpath,submitter,value,last_mod,"
                                        + VOTE_OVERRIDE
                                        + ","
                                        + VOTE_TYPE
                                        + ") values (?,?,?,?,CURRENT_TIMESTAMP,?,?) "
                                        + "ON DUPLICATE KEY UPDATE locale=?,xpath=?,submitter=?,value=?,last_mod=CURRENT_TIMESTAMP,"
                                        + VOTE_OVERRIDE
                                        + "=?,"
                                        + VOTE_TYPE
                                        + "=?");
                int colNum = 1;
                for (int repeat = 1; repeat <= 2; repeat++) {
                    ps.setString(colNum++, locale.getBaseName());
                    ps.setInt(colNum++, xpathId);
                    ps.setInt(colNum++, submitter);
                    DBUtils.setStringUTF8(ps, colNum++, value);
                    DBUtils.setInteger(ps, colNum++, withVote);
                    DBUtils.setInteger(ps, colNum++, voteType.id());
                }
                ps.executeUpdate();

                if (wasFlagged && UserRegistry.userIsTC(user)) {
                    clearFlag(conn, locale, xpathId);
                    didClearFlag = true;
                }
                conn.commit();
            } catch (SQLException e) {
                SurveyLog.logException(logger, e, "Exception in saveVoteToDb");
                SurveyMain.busted("Could not vote for value in locale " + locale, e);
                throw new InternalError(
                        "Could not load locale " + locale + " : " + DBUtils.unchainSqlException(e));
            } finally {
                DBUtils.close(saveOld, ps, conn);
            }
            SurveyLog.debug(et);

            // Voting can trigger adding a forum post (agree/decline) and/or closing a forum thread.
            // AUTO_IMPORT and MANUAL_IMPORT votes are excluded; DIRECT and BULK_UPLOAD are not
            // excluded.
            if (sm.fora != null
                    && (voteType != VoteType.AUTO_IMPORT && voteType != VoteType.MANUAL_IMPORT)) {
                final boolean clearFlag = didClearFlag;
                SurveyThreadManager.getExecutorService()
                        .submit(
                                () ->
                                        sm.fora.doForumAfterVote(
                                                locale,
                                                user,
                                                distinguishingXpath,
                                                xpathId,
                                                value,
                                                clearFlag));
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
                peekXpathData(distinguishingXpath)
                        .setVoteForValue(
                                admin,
                                value,
                                VoteResolver.Level.LOCKING_VOTES,
                                new Date(),
                                VoteType.DIRECT);
            } else if (pv.didUnlock()) {
                peekXpathData(distinguishingXpath)
                        .removeOverrideVotes(VoteResolver.Level.LOCKING_VOTES);
            }
            if (pv.didCleanSlate()) {
                peekXpathData(distinguishingXpath)
                        .removeOverrideVotes(VoteResolver.Level.PERMANENT_VOTES);
            }
        }

        /**
         * @param user
         * @param distinguishingXpath
         * @param value
         * @param when
         *     <p>Called by loadVoteValues and voteForValue.
         */
        private void internalSetVoteForValue(
                User user,
                String distinguishingXpath,
                String value,
                Integer voteOverride,
                Date when,
                VoteType voteType)
                throws InvalidXPathException {
            if (voteType == null || voteType == VoteType.NONE) {
                logger.warning(
                        "internalSetVoteForValue got vote type "
                                + voteType
                                + "; changed to UNKNOWN");
                voteType = VoteType.UNKNOWN;
            }
            makeSureInPathsForFile(distinguishingXpath, user, value);
            getXPathData(distinguishingXpath)
                    .setVoteForValue(user, value, voteOverride, when, voteType);
            nextStamp();
        }

        @Override
        public boolean userDidVote(User myUser, String somePath) {
            PerXPathData xpd = peekXpathData(somePath);
            return (xpd != null && xpd.userDidVote(myUser));
        }

        @Override
        public VoteType getUserVoteType(User myUser, String somePath) {
            PerXPathData xpd = peekXpathData(somePath);
            if (xpd == null) {
                logger.warning("getUserVoteType got xpd null, returning NONE");
                return VoteType.NONE;
            }
            VoteType voteType = xpd.getUserVoteType(myUser);
            if (voteType == null || voteType == VoteType.NONE) {
                logger.warning(
                        "getUserVoteType got vote type " + voteType + "; changed to UNKNOWN");
                voteType = VoteType.UNKNOWN;
            }
            return voteType;
        }

        public Set<String> getPathsForFile() {
            return pathsForFile;
        }

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

        public XMLSource getSource() {
            return xmlsource;
        }
    }

    /**
     * Is this a locale that can't be modified?
     *
     * @param loc
     * @return
     */
    public static boolean isReadOnlyLocale(CLDRLocale loc) {
        return SurveyMain.getReadOnlyLocales().contains(loc);
    }

    /**
     * Is this a locale that can't be modified?
     *
     * @param loc
     * @return
     */
    public static boolean isReadOnlyLocale(String loc) {
        return isReadOnlyLocale(CLDRLocale.getInstance(loc));
    }

    private static void readonly() {
        throw new InternalError("This is a readonly instance.");
    }

    /** Throw an error. This is a bottleneck called whenever something unimplemented is called. */
    public static void unimp() {
        throw new InternalError("NOT YET IMPLEMENTED - TODO!.");
    }

    boolean dbIsSetup = false;

    /** The infamous back-pointer. */
    public SurveyMain sm;

    private final org.unicode.cldr.util.PathHeader.Factory phf;

    /** Construct one. */
    public STFactory(SurveyMain sm) {
        super();
        if (sm == null) {
            throw new IllegalArgumentException("sm must not be null");
        }
        this.sm = sm;
        try (CLDRProgressTask progress = sm.openProgress("STFactory")) {
            progress.update("setup supplemental data");
            setSupplementalDirectory(sm.getDiskFactory().getSupplementalDirectory());
            if (getSupplementalDirectory() == null) {
                throw new NullPointerException("getSupplementalDirectory() == null!");
            }

            progress.update("reload all users");
            sm.reg.getVoterInfoList();
            progress.update("setup pathheader factory");
            phf = PathHeader.getFactory(sm.getEnglishFile());
        }
    }

    /** For statistics */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("-cache:");
        int good = 0;
        for (Entry<CLDRLocale, PerLocaleData> e : locales.asMap().entrySet()) {
            if (e.getValue() != null) {
                good++;
            }
        }
        sb.append(good + "/" + locales.size() + " locales. }");
        return sb.toString();
    }

    @Override
    public BallotBox<User> ballotBoxForLocale(CLDRLocale locale) {
        return get(locale);
    }

    // Note:  not static, so that CLDRConfig.getInstance() is deferred.

    /** Config: # of hours before a locale is expired from the cache */
    private final int CLDR_LOCALE_CACHE_HOURS =
            CLDRConfig.getInstance().getProperty("CLDR_LOCALE_EXPIRE_HOURS", 12);
    /** Config: Max # of concurrent locales/sublocales in teh cache */
    private final int CLDR_LOCALE_CACHE_MAX =
            CLDRConfig.getInstance().getProperty("CLDR_LOCALE_CACHE_MAX", 100);

    /** Per locale map */
    private final LoadingCache<CLDRLocale, PerLocaleData> locales =
            CacheBuilder.newBuilder()
                    .softValues()
                    .expireAfterAccess(Duration.ofHours(CLDR_LOCALE_CACHE_HOURS))
                    .maximumSize(CLDR_LOCALE_CACHE_MAX)
                    .removalListener(
                            notification ->
                                    logger.info(
                                            () ->
                                                    "Locale expired: "
                                                            + notification.getKey()
                                                            + " due to "
                                                            + notification.getCause()))
                    .build(
                            new CacheLoader<CLDRLocale, PerLocaleData>() {

                                @Override
                                public PerLocaleData load(CLDRLocale key) throws Exception {
                                    if (!getAvailableCLDRLocales().contains(key)) {
                                        return null; // not available
                                    }
                                    return new PerLocaleData(key);
                                }
                            });

    private final Map<CLDRLocale, MutableStamp> localeStamps =
            new ConcurrentHashMap<>(SurveyMain.getLocalesSet().size());

    /**
     * Return changetime.
     *
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
     * Fetch a locale from the per locale data, create if not there.
     *
     * @param locale
     * @return
     */
    public PerLocaleData get(CLDRLocale locale) {
        // Make sure the parent data is loaded and accessed. Yes, this recurses.
        CLDRLocale parent = locale.getParent();
        if (parent != null) {
            // Parent must be loaded first.
            // Also, this makes sure that the parent is re-accessed (kept in cache)
            get(parent);
        }

        // now load the actual locale
        try {
            return locales.get(locale);
        } catch (ExecutionException e) {
            SurveyLog.logException(logger, e, "get(" + locale + ")");
            e.printStackTrace();
            SurveyMain.busted("get(" + locale + ")", e);
            throw new RuntimeException("get(" + locale + ") failed", e); // busted
        }
    }

    private PerLocaleData get(String locale) {
        return get(CLDRLocale.getInstance(locale));
    }

    public TestCache.TestResultBundle getTestResult(CLDRLocale loc, CheckCLDR.Options options) {
        return getTestCache().getBundle(options);
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

    private final Map<CLDRLocale, Set<CLDRLocale>> subLocaleMap = new HashMap<>();
    Set<CLDRLocale> allLocales = null;

    /** Cache.. */
    @Override
    public Set<CLDRLocale> subLocalesOf(CLDRLocale forLocale) {
        Set<CLDRLocale> result = subLocaleMap.get(forLocale);
        if (result == null) {
            result = calculateSubLocalesOf(forLocale, getAvailableCLDRLocales());
            subLocaleMap.put(forLocale, result);
        }
        return result;
    }

    /** Cache.. */
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
    protected CLDRFile handleMake(
            String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus) {
        return get(localeID).getFile(resolved);
    }

    public CLDRFile make(String loc) {
        return make(loc, true);
    }

    public CLDRFile make(CLDRLocale loc, boolean resolved) {
        return make(loc.getBaseName(), resolved);
    }

    /**
     * Make a "vetted" CLDRFile with more paths resolved, for generating VXML (vetted XML).
     *
     * <p>See loadVoteValues for what exactly "more paths" means.
     *
     * <p>This kind of CLDRFile should not be confused with ordinary (not-fully-vetted) files, or
     * re-used for anything other than vxml. Avoid mixing data for the two kinds of CLDRFile in
     * caches (such as rLocales).
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
     *     <p>Called only by loadVoteValues.
     */
    private PreparedStatement openQueryByLocaleRW(Connection conn) throws SQLException {
        setupDB();
        /*
         * locale must be included in the SELECT list as well as the WHERE clause,
         * to prevent SQL exception in the special case where deleteRow is called on
         * the result set for votes for invalid (e.g., obsolete) paths. The query
         * "must select all primary keys from that table".
         */
        return DBUtils.prepareForwardUpdateable(
                conn,
                "SELECT xpath,submitter,value,locale,"
                        + VOTE_OVERRIDE
                        + ",last_mod, "
                        + VOTE_TYPE
                        + " FROM "
                        + DBUtils.Table.VOTE_VALUE
                        + " WHERE locale = ?");
    }

    private PreparedStatement openPermVoteQuery(Connection conn) throws SQLException {
        setupDB();
        return DBUtils.prepareForwardUpdateable(
                conn,
                "SELECT xpath,value,last_mod FROM "
                        + DBUtils.Table.LOCKED_XPATHS
                        + " WHERE locale = ?");
    }

    public synchronized void setupDB() {
        if (dbIsSetup) return;
        dbIsSetup = true; // don't thrash.
        String sql = "(none)"; // this points to
        Statement s = null;
        try (Connection conn = DBUtils.getInstance().getDBConnection()) {
            if (!DBUtils.hasTable(DBUtils.Table.VOTE_VALUE.toString())) {
                s = conn.createStatement();
                sql =
                        "CREATE TABLE "
                                + DBUtils.Table.VOTE_VALUE
                                + "( "
                                + "locale VARCHAR(20), "
                                + "xpath  INT NOT NULL, "
                                + "submitter INT NOT NULL, "
                                + "value "
                                + DBUtils.DB_SQL_UNICODE
                                + ", "
                                + DBUtils.DB_SQL_LAST_MOD
                                + ", "
                                + VOTE_OVERRIDE
                                + " INT DEFAULT NULL, "
                                + VOTE_TYPE
                                + " TINYINT NOT NULL, "
                                + "PRIMARY KEY (locale,submitter,xpath) "
                                + ")";
                // logger.info(sql);
                s.execute(sql);

                sql =
                        "CREATE UNIQUE INDEX  "
                                + DBUtils.Table.VOTE_VALUE
                                + " ON "
                                + DBUtils.Table.VOTE_VALUE
                                + " (locale,xpath,submitter)";
                s.execute(sql);
                s.close();
                s = null; // don't close twice.
                conn.commit();
                System.err.println("Created table " + DBUtils.Table.VOTE_VALUE);
            } else if (!DBUtils.tableHasColumn(
                    conn, DBUtils.Table.VOTE_VALUE.toString(), VOTE_TYPE)) {
                s = conn.createStatement();
                sql =
                        "ALTER TABLE "
                                + DBUtils.Table.VOTE_VALUE
                                + " ADD COLUMN "
                                + VOTE_TYPE
                                + " TINYINT NOT NULL";
                s.execute(sql);
                s.close();
                s = null;
                conn.commit();
                System.err.println(
                        "Added column " + VOTE_TYPE + " to table " + DBUtils.Table.VOTE_VALUE);
            }
            if (!DBUtils.hasTable(DBUtils.Table.VOTE_VALUE_ALT.toString())) {
                s = conn.createStatement();
                String valueLen = DBUtils.db_Mysql ? "(750)" : "";
                sql =
                        "create table "
                                + DBUtils.Table.VOTE_VALUE_ALT
                                + "( "
                                + "locale VARCHAR(20), "
                                + "xpath  INT NOT NULL, "
                                + "value "
                                + DBUtils.DB_SQL_UNICODE
                                + ", "
                                +
                                // DBUtils.DB_SQL_LAST_MOD + " " +
                                " PRIMARY KEY (locale,xpath,value"
                                + valueLen
                                + ") "
                                + " )";
                // logger.info(sql);
                s.execute(sql);

                sql =
                        "CREATE UNIQUE INDEX  "
                                + DBUtils.Table.VOTE_VALUE_ALT
                                + " ON "
                                + DBUtils.Table.VOTE_VALUE_ALT
                                + " (locale,xpath,value"
                                + valueLen
                                + ")";
                s.execute(sql);
                s.close();
                s = null; // don't close twice.
                conn.commit();
                System.err.println("Created table " + DBUtils.Table.VOTE_VALUE_ALT);
            }
            if (!DBUtils.hasTable(DBUtils.Table.VOTE_FLAGGED.toString())) {
                s = conn.createStatement();

                sql =
                        "create table "
                                + DBUtils.Table.VOTE_FLAGGED
                                + "( "
                                + "locale VARCHAR(20), "
                                + "xpath  INT NOT NULL, "
                                + "submitter INT NOT NULL, "
                                + DBUtils.DB_SQL_LAST_MOD
                                + " "
                                + ", PRIMARY KEY (locale,xpath) "
                                + " )";
                // logger.info(sql);
                s.execute(sql);

                sql =
                        "CREATE UNIQUE INDEX  "
                                + DBUtils.Table.VOTE_FLAGGED
                                + " ON "
                                + DBUtils.Table.VOTE_FLAGGED
                                + " (locale,xpath)";
                s.execute(sql);
                s.close();
                s = null; // don't close twice.
                conn.commit();
                System.err.println("Created table " + DBUtils.Table.VOTE_FLAGGED);
            }
            if (!DBUtils.hasTable(DBUtils.Table.IMPORT.toString())) {
                /*
                 * Create the IMPORT table, for keeping track of imported old losing votes.
                 * Use DB_SQL_BINCOLLATE for compatibility with existing vote tables, which
                 * (on st.unicode.org as of 2018-11-08) have "DEFAULT CHARSET=latin1 COLLATE=latin1_bin".
                 */
                s = conn.createStatement();
                String valueLen = DBUtils.db_Mysql ? "(750)" : "";
                sql =
                        "CREATE TABLE "
                                + DBUtils.Table.IMPORT
                                + "( "
                                + "locale VARCHAR(20), "
                                + "xpath INT NOT NULL, "
                                + "value "
                                + DBUtils.DB_SQL_UNICODE
                                + ", "
                                + " PRIMARY KEY (locale,xpath,value"
                                + valueLen
                                + ") "
                                + " ) "
                                + DBUtils.DB_SQL_BINCOLLATE;
                s.execute(sql);

                sql =
                        "CREATE UNIQUE INDEX  "
                                + DBUtils.Table.IMPORT
                                + " ON "
                                + DBUtils.Table.IMPORT
                                + " (locale,xpath,value"
                                + valueLen
                                + ")";
                s.execute(sql);
                s.close();
                s = null; // don't close twice.
                conn.commit();
                System.err.println("Created table " + DBUtils.Table.IMPORT);
            }
            if (!DBUtils.hasTable(DBUtils.Table.IMPORT_AUTO.toString())) {
                /*
                 * Create the IMPORT_AUTO table, for keeping track of which users have auto-imported old winning votes.
                 * Use DB_SQL_BINCOLLATE for compatibility with existing vote tables, which
                 * (on st.unicode.org as of 2018-12-19) have "DEFAULT CHARSET=latin1 COLLATE=latin1_bin".
                 */
                s = conn.createStatement();
                sql =
                        "CREATE TABLE "
                                + DBUtils.Table.IMPORT_AUTO
                                + "(userid INT NOT NULL, PRIMARY KEY (userid) ) "
                                + DBUtils.DB_SQL_BINCOLLATE;
                s.execute(sql);

                sql =
                        "CREATE UNIQUE INDEX  "
                                + DBUtils.Table.IMPORT_AUTO
                                + " ON "
                                + DBUtils.Table.IMPORT_AUTO
                                + " (userid)";
                s.execute(sql);
                s.close();
                s = null; // don't close twice.
                conn.commit();
                System.err.println("Created table " + DBUtils.Table.IMPORT_AUTO);
            }
            String tableName = DBUtils.Table.LOCKED_XPATHS.toString();
            if (!DBUtils.hasTable(tableName)) {
                /*
                 * Create the LOCKED_XPATHS table, for keeping track of paths that have been "locked" for specific locales.
                 * Reference: https://unicode-org.atlassian.net/browse/CLDR-11677
                 * Use DB_SQL_BINCOLLATE for compatibility with existing vote tables, which
                 * (on st.unicode.org as of 2020-01-07) have "DEFAULT CHARSET=latin1 COLLATE=latin1_bin".
                 */
                s = conn.createStatement();
                sql =
                        "CREATE TABLE "
                                + tableName
                                + "(locale VARCHAR(20), xpath INT NOT NULL, "
                                + "value "
                                + DBUtils.DB_SQL_UNICODE
                                + ", "
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
     *
     * @param conn
     * @param locale
     * @param xpath
     * @param user
     * @throws SQLException
     * @return number of rows changed
     */
    public int setFlag(Connection conn, CLDRLocale locale, int xpath, User user)
            throws SQLException {
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
                ps =
                        DBUtils.prepareStatementWithArgs(
                                conn,
                                "INSERT IGNORE INTO "
                                        + DBUtils.Table.VOTE_FLAGGED
                                        + " (locale,xpath,submitter) VALUES (?,?,?)",
                                locale.toString(),
                                xpath,
                                user.id);
            } else {
                ps =
                        DBUtils.prepareStatementWithArgs(
                                conn,
                                "INSERT INTO "
                                        + DBUtils.Table.VOTE_FLAGGED
                                        + " (locale,xpath,submitter) VALUES (?,?,?)",
                                locale.toString(),
                                xpath,
                                user.id);
            }
            return ps.executeUpdate();
        } finally {
            DBUtils.close(ps);
        }
    }

    /**
     * Flag the specified xpath for review.
     *
     * @param conn
     * @param locale
     * @param xpath
     * @throws SQLException
     */
    private void clearFlag(Connection conn, CLDRLocale locale, int xpath) throws SQLException {
        PreparedStatement ps = null;
        try {
            synchronized (STFactory.class) {
                loadFlag().remove(new Pair<>(locale, xpath));
            } // make sure DB is loaded before we attempt to update
            ps =
                    DBUtils.prepareStatementWithArgs(
                            conn,
                            "DELETE FROM "
                                    + DBUtils.Table.VOTE_FLAGGED
                                    + " WHERE locale=? AND xpath=?",
                            locale.toString(),
                            xpath);
            ps.executeUpdate();
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
     *
     * @return
     */
    private Set<Pair<CLDRLocale, Integer>> loadFlag() {
        if (flagList == null) {
            setupDB();

            flagList = new HashSet<>();

            logger.fine("Loading flagged items from .." + DBUtils.Table.VOTE_FLAGGED);
            try {
                for (Map<String, Object> r :
                        DBUtils.queryToArrayAssoc("select * from " + DBUtils.Table.VOTE_FLAGGED)) {
                    flagList.add(
                            new Pair<>(
                                    CLDRLocale.getInstance(r.get("locale").toString()),
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

    /** In memory cache. */
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

    public final PathHeader getPathHeader(String xpath) {
        try {
            return phf.fromPath(xpath);
        } catch (Throwable t) {
            SurveyLog.warnOnce(logger, "PH for path " + xpath + t);
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
     *
     * @param locale
     * @return
     */
    public Set<String> getPathsForFile(CLDRLocale locale) {
        return get(locale).getPathsForFile();
    }

    /**
     * Get paths for file matching a prefix. Does not cache.
     *
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
     *
     * MYSQL drop table if exists cldr_v22submission; create table
     * cldr_v22submission ( primary key(xpath,locale),key(locale) ) select
     * distinct cldr_votevalue.xpath,cldr_votevalue.locale from cldr_votevalue
     * where cldr_votevalue.value is not null;
     */
    public CLDRFile makeProposedFile(CLDRLocale locale) {

        Connection conn = null;
        PreparedStatement ps = null; // all for mysql
        ResultSet rs = null;
        SimpleXMLSource sxs = new SimpleXMLSource(locale.getBaseName());
        try {
            conn = DBUtils.getInstance().getAConnection();

            ps =
                    DBUtils.prepareStatementWithArgsFRO(
                            conn,
                            "select xpath,submitter,value,"
                                    + VOTE_OVERRIDE
                                    + " from "
                                    + DBUtils.Table.VOTE_VALUE
                                    + " where locale=? and value IS NOT NULL",
                            locale);

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

            return new CLDRFile(sxs);
        } catch (SQLException e) {
            SurveyLog.logException(logger, e, "In makeProposedFile");
            SurveyMain.busted("Could not read locale " + locale, e);
            throw new InternalError(
                    "Could not load locale " + locale + " : " + DBUtils.unchainSqlException(e));
        } finally {
            DBUtils.close(rs, ps, conn);
        }
    }

    /** Return the table for old votes */
    public static String getLastVoteTable() {
        return DBUtils.Table.VOTE_VALUE
                .forVersion(SurveyMain.getLastVoteVersion(), false)
                .toString();
    }

    /** Somewhat expensive operation. Query whether an XPath is visible. */
    public boolean isVisibleInSurveyTool(String locale, String x) {
        return isVisibleInSurveyTool(
                CLDRLocale.getInstance(locale), CLDRFile.getDistinguishingXPath(x, null));
    }

    /** Somewhat expensive operation. Query whether an XPath is visible. */
    public boolean isVisibleInSurveyTool(CLDRLocale l, String dPath) {
        // see also {@link #isValidSurveyToolVote}
        if (!getAvailableCLDRLocales().contains(l)) {
            return false; // bad locale ID
        }

        PathHeader ph = getPathHeader(dPath);
        if (ph == null) return false; // out of scope
        if (ph.shouldHide()) return false; // PH says hide it

        if (sm.getSupplementalDataInfo().getCoverageValue(dPath, l.getBaseName())
                > org.unicode.cldr.util.Level.COMPREHENSIVE.getLevel()) {
            return false; // out of coverage
        }

        if (!get(l).getPathsForFile().contains(dPath)) {
            return false; // not in file paths.
            // Note: Not sure why it wasn't caught before!
        }

        return true; // OK.
    }

    /** remove tests excluded by SurveyTool */
    public static List<CheckStatus> removeExcludedChecks(List<CheckStatus> tests) {
        tests.removeIf((status) -> status.getSubtype() == Subtype.coverageLevel);
        return tests;
    }
}
