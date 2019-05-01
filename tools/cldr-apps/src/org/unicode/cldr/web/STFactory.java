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

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.TestCache;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfig.Environment;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Emoji;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.LruMap;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.XPathParts.Comments;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;
import org.unicode.cldr.web.SurveyException.ErrorCode;
import org.unicode.cldr.web.UserRegistry.ModifyDenial;
import org.unicode.cldr.web.UserRegistry.User;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.VersionInfo;

/**
 * @author srl
 *
 */
public class STFactory extends Factory implements BallotBoxFactory<UserRegistry.User>, UserRegistry.UserChangedListener {
    /**
     * This class tracks the expected maximum size of strings in the locale.
     * @author srl
     *
     */
    public static class LocaleMaxSizer {
        public static final int EXEMPLAR_CHARACTERS_MAX = 8192;

        public static final String EXEMPLAR_CHARACTERS = "//ldml/characters/exemplarCharacters";

        Map<CLDRLocale, Map<String, Integer>> sizeExceptions;

        TreeMap<String, Integer> exemplars_prefix = new TreeMap<String, Integer>();
        Set<CLDRLocale> exemplars_set = new TreeSet<CLDRLocale>();

        /**
         * Construct a new sizer.
         */
        public LocaleMaxSizer() {
            // set up the map
            sizeExceptions = new TreeMap<CLDRLocale, Map<String, Integer>>();
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

    public class DataBackedSource extends DelegateXMLSource {
        PerLocaleData ballotBox;
        XMLSource aliasOf; // original XMLSource

        public DataBackedSource(PerLocaleData makeFrom) {
            super((XMLSource) makeFrom.diskData.cloneAsThawed());
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
            // SurveyLog.logger.warning("Note: DBS.getFullPathAtDPath() todo!!");
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
         * @param resolver the VoteResolver
         * @param resolveMorePaths true for making vxml, else false
         * @return the VoteResolver
         */
        public VoteResolver<String> setValueFromResolver(String path, VoteResolver<String> resolver, boolean resolveMorePaths) {
            org.unicode.cldr.web.STFactory.PerLocaleData.PerXPathData xpd = ballotBox.peekXpathData(path);
            String res;
            String fullPath = null;
            if (resolveMorePaths == false && (xpd == null || xpd.isEmpty())) {
                /*
                 * If resolveMorePaths is false and there are no votes, it may be more efficient
                 * (or anyway expected) to skip vote resolution and use diskData instead.
                 * This has far-reaching effects and should be better documented.
                 */
                res = ballotBox.diskData.getValueAtDPath(path);
                fullPath = ballotBox.diskData.getFullPathAtDPath(path);
            } else {
                /*
                 * If resolveMorePaths is true, especially for generating vxml, we need to call
                 * getWinningValue for vote resolution for a larger set of paths to get baseline etc. even
                 * if there are no votes.
                 */
                res = (resolver = ballotBox.getResolver(xpd, path, resolver)).getWinningValue();
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
                 * Catch VoteResolver.Status.missing, or it will trigger an exception
                 * in draftStatusFromWinningStatus since there is no "missing" in DraftStatus.
                 * This may happen especially when resolveMorePaths is true for making vxml.
                 */
                if (win == Status.missing) {
                   return resolver;
                } else if (win == Status.approved) {
                    fullPath = baseXPath;
                } else {
                    DraftStatus draftStatus = draftStatusFromWinningStatus(win);
                    fullPath = baseXPath + "[@draft=\"" + draftStatus.toString() + "\"]";
                }
            }
            if (res != null) {
                /*
                 * TODO: needed to clear fullpath? Otherwise, fullpath may be ignored if
                 * value is extant.
                 */
                delegate.removeValueAtDPath(path);
                delegate.putValueAtPath(fullPath, res);
            } else {
                delegate.removeValueAtDPath(path);
            }
            notifyListeners(path);
            return resolver;
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
                SurveyLog.logException(e, "Exception in draftStatusFromWinningStatus of " + win);
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
        private CLDRFile oldFile;
        private CLDRFile oldFileUnresolved;
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
            };

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
                TreeSet<User> ts = new TreeSet<User>();
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

//            /**
//             * Get the per user data, create if not found
//             * @param user
//             * @return
//             */
//            private PerUserData getPerUserData(User user) {
//                if(userToData==null) {
//                    userToData = new ConcurrentHashMap<UserRegistry.User, STFactory.PerLocaleData.PerXPathData.PerUserData>();
//                }
//                PerUserData pud = peekUserToData(user);
//                if(pud!=null) {
//                    userToData.put(user, pud);
//                }
//                return pud;
//            }
            /**
             * Set this user's vote.
             * @param user
             * @param pud
             * @return
             */
            private PerUserData setPerUserData(User user, PerUserData pud) {
                if (userToData == null) {
                    userToData = new ConcurrentHashMap<UserRegistry.User, STFactory.PerLocaleData.PerXPathData.PerUserData>();
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
                Map<User, Integer> rv = new HashMap<User, Integer>(userToData.size());
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
        };

        private Map<String, PerXPathData> xpathToData = new HashMap<String, PerXPathData>();
//        private Map<String, Map<User, String>> xpathToVotes = new HashMap<String, Map<User, String>>();
//        private Map<String, Map<User, Integer>> xpathToOverrides = new HashMap<String, Map<User, Integer>>();
//        private Map<Integer, Set<String>> xpathToOtherValues = new HashMap<Integer, Set<String>>();
        private boolean oldFileMissing;
        private XMLSource resolvedXmlsource = null;
        /**
         * Parent locale - or null.
         */
        private final String nextParent;
        /**
         * CLDR file for loading Bailey value
         */
        private final CLDRFile fallbackParent;

        PerLocaleData(CLDRLocale locale) {
            this.locale = locale;
            this.nextParent = LocaleIDParser.getParent(locale.getBaseName());
            readonly = isReadOnlyLocale(locale);
            diskData = (XMLSource) sm.getDiskFactory().makeSource(locale.getBaseName()).freeze();
            sm.xpt.loadXPaths(diskData);
            diskFile = sm.getDiskFactory().make(locale.getBaseName(), true).freeze();
            pathsForFile = phf.pathsForFile(diskFile);

            if (nextParent == null) {
                fallbackParent = null; // no fallback parent
            } else {
                /**
                 * This will cause a load of the parent before the child.
                 */
                fallbackParent = get(nextParent).getFile(true);
            }

//            if (checkHadVotesSometimeThisRelease) {
//                votesSometimeThisRelease = loadVotesSometimeThisRelease(locale);
//                if (votesSometimeThisRelease == null) {
//                    SurveyLog.warnOnce("Note: giving up on loading 'sometime this release' votes. The database name would be "
//                        + getVotesSometimeTableName());
//                    checkHadVotesSometimeThisRelease = false; // don't try
//                                                              // anymore.
//                }
//            }
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
         * Get the Status for the given CLDRFile, path, and value.
         * 
         * @param cldrFile
         * @param path
         * @param value
         * @return the Status
         */
        private Status getStatus(CLDRFile cldrFile, String path, final String value) {
            String fullXPath = cldrFile.getFullXPath(path);
            if (fullXPath == null) {
                fullXPath = path;
            }
            XPathParts xpp = XPathParts.getTestInstance(fullXPath);
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
         * Load internal data (votes, etc.) for this PerLocaleData, and push it into this.xmlsource,
         * which is a DataBackedSource.
         *
         * @param resolveMorePaths false to do vote resolution only on paths with votes in current votes table, or
         *                         true to do vote resolution also on all paths in trunk (for making vxml).
         *                        
         *
         * Called by PerLocaleData.makeSource(resolve = false), and by PerLocaleData.makeVettedSource.
         */
        private void loadVoteValues(boolean resolveMorePaths) {
            if (!readonly) {
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
                    conn = DBUtils.getInstance().getDBConnection();
                    ps = openQueryByLocaleRW(conn);
                    ps.setString(1, locale.getBaseName());
                    rs = ps.executeQuery();

                    while (rs.next()) {
                        int xp = rs.getInt(1);
                        String xpath = sm.xpt.getById(xp);
                        int submitter = rs.getInt(2);
                        String value = DBUtils.getStringUTF8(rs, 3);
                        // 4 = locale -- unused; TODO: remove from openQueryByLocaleRW
                        Integer voteOverride = rs.getInt(5); // 5 override
                        if (voteOverride == 0 && rs.wasNull()) { // if override was a null..
                            voteOverride = null;
                        }
                        Timestamp last_mod = rs.getTimestamp(6); // last mod
                        User theSubmitter = sm.reg.getInfo(submitter);
                        if (theSubmitter == null) {
                            SurveyLog.warnOnce("Ignoring votes for deleted user #" + submitter);
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
                            System.err.println("InvalidXPathException: Deleting vote for " + theSubmitter + ":" + locale + ":" + xpath);
                            rs.deleteRow();
                            del++;
                        }
                    }
                    if (del > 0) {
                        System.out.println("Committing delete of " + del + " invalid votes from " + locale);
                        conn.commit();
                    }
                } catch (SQLException e) {
                    SurveyLog.logException(e);
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
                 * When producing vxml, resolveMorePaths is true and we use all paths in diskData (trunk) in
                 * addition to allPXDPaths(); otherwise, vxml produced by admin-OutputAllFiles.jsp is missing some paths.
                 * allPXDPaths() may return an empty array if there are no votes in current votes table.
                 * (However, we assume that last-release value soon won't be used anymore for vote resolution.
                 * If we did need paths from last-release, or any paths missing from trunk and current votes table,
                 * we could loop through sm.getSTFactory().getPathsForFile(locale); however, that would generally
                 * include more paths than are wanted for vxml.)
                 * Reference: https://unicode.org/cldr/trac/ticket/11909
                 */
                // Set<String> xpathSet = resolveMorePaths ? sm.getSTFactory().getPathsForFile(locale) : allPXDPaths();
                Set<String> xpathSet;
                if (resolveMorePaths) {
                    xpathSet = new HashSet<String>(allPXDPaths());
                    for (String xp : diskData) {
                        xpathSet.add(xp);
                    }
                } else {
                    xpathSet = allPXDPaths();
                }
                int j = 0;
                for (String xp : xpathSet) {
                    resolver = xmlsource.setValueFromResolver(xp, resolver, resolveMorePaths);
                    j++;
                }
                SurveyLog.debug(et + " - resolved " + j + " items, " + n + " total.");
            }
            stamp.next();
            xmlsource.addListener(gTestCache);
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

        public synchronized CLDRFile getOldFileResolved() {
            if (oldFileMissing) { // common flag across both resolve and unresolved
                return null;
            } else if (oldFile == null) {
                oldFile = sm.getOldFile(locale, true);
                oldFileMissing = (oldFile == null); // if we get null, it's because the file wasn't available.
            }
            return oldFile;
        }

        public synchronized CLDRFile getOldFileUnresolved() {
            if (oldFileMissing) { // common flag across both resolve and unresolved
                return null;
            } else if (oldFileUnresolved == null) {
                oldFileUnresolved = sm.getOldFile(locale, false);
                oldFileMissing = (oldFileUnresolved == null); // if we get null, it's because the file wasn't available.
            }
            return oldFileUnresolved;
        }

        /**
         * Utility class for testing values
         * @author srl
         *
         */
        private class ValueChecker {
            private final String path;
            HashSet<String> allValues = new HashSet<String>(8); // 16 is
            // probably too
            // many values
            HashSet<String> badValues = new HashSet<String>(8); // 16 is
            // probably too
            // many values

            LinkedList<CheckCLDR.CheckStatus> result = null;
            TestResultBundle testBundle = null;

            ValueChecker(String path) {
                this.path = path;
            }

            boolean canUseValue(String value) {
                if (value == null || allValues.contains(value)) {
                    return true;
                } else if (badValues.contains(value)) {
                    return false;
                } else {
                    if (testBundle == null) {
                        testBundle = getDiskTestBundle(locale);
                        result = new LinkedList<CheckCLDR.CheckStatus>();
                    } else {
                        result.clear();
                    }

                    testBundle.check(path, result, value);
                    if (false) System.out.println("Checking result of " + path + " = " + value + " := haserr " + CheckCLDR.CheckStatus.hasError(result));
                    if (CheckCLDR.CheckStatus.hasError(result)) {
                        badValues.add(value);
                        return false;
                    } else {
                        allValues.add(value);
                        return true; // OK
                    }
                }
            }

        }

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
            if (path == null)
                throw new IllegalArgumentException("path must not be null");

            if (r == null) {
                r = new VoteResolver<String>(); // create
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
            r.setUsingKeywordAnnotationVoting(path.startsWith("//ldml/annotations/annotation") && !path.contains(Emoji.TYPE_TTS));

            // Workaround (workaround what?)
            CLDRFile.Status status = new CLDRFile.Status();
            diskFile.getSourceLocaleID(path, status); // ask disk file

            /*
             * TODO: Fix bug: the baileyValue set here is not, in general, the same as the one in updateInheritedValue
             * in DataSection.java! There,
             * inheritedValue = ourSrc.getConstructedBaileyValue(xpath, inheritancePathWhereFound, localeWhereFound);
             * For example, here we get baileyValue = "Veräifachts Chineesisch",
             * but in updateInheritedValue we get inheritedValue = "Chineesisch (Veräifachti Chineesischi Schrift)".
             * That's for http://localhost:8080/cldr-apps/v#/gsw_FR/Languages_A_D/3f16ed8804cebb7d
             * Reference https://unicode.org/cldr/trac/ticket/11420
             */
            String baileyValue = null;
            if (status.pathWhereFound.equals(path)) {
                // we found it on the same path, so no aliasing
                // for that case, it is safe to use the parent's value
                if (fallbackParent == null) {
                    // we are in root. 
                    baileyValue = diskData.getBaileyValue(path, null, null);
                } else {
                    baileyValue = fallbackParent.getStringValue(path);
                }
            } else {
                // the path changed, so use that path to get the right value
                // from the *current* file (not the parent)
                r = getResolverInternal(peekXpathData(status.pathWhereFound), status.pathWhereFound, r);
                /*
                 * Caution: never set baileyValue = INHERITANCE_MARKER!
                 * That happened here, with "baileyValue = r.getWinningValue()" when
                 * getWinningValue returned INHERITANCE_MARKER.
                 * http://localhost:8080/cldr-apps/v#/ko/Gregorian/42291caf2163ca8d
                 * Third "BCE" row ("eraNarrow"), which inherits from second "BCE" row ("eraAbbr").
                 * We got hard BCE in Winning column, soft BCE in Others column.
                 * Recursive call to PerLocaleData.getResolverInternal was involved.
                 * If r.getWinningValue() returns INHERITANCE_MARKER, then set
                 * this.baileyValue = r.getBaileyValue().
                 * Reference https://unicode.org/cldr/trac/ticket/11611
                 */
                String wv = r.getWinningValue();
                if (CldrUtility.INHERITANCE_MARKER.equals(wv)) {
                    wv = r.getBaileyValue();
                }
                baileyValue = wv;                    
                r.clear(); // clear it again
            }

            final ValueChecker vc = ERRORS_ALLOWED_IN_VETTING ? null : new ValueChecker(path);

            // Set established locale
            r.setLocale(locale, getPathHeader(path));

            // set current Trunk (baseline) value (if present)
            final String currentValue = diskData.getValueAtDPath(path);
            final Status currentStatus = getStatus(diskFile, path, currentValue);
            if (ERRORS_ALLOWED_IN_VETTING || vc.canUseValue(currentValue)) {
                r.setTrunk(currentValue, currentStatus);
                r.add(currentValue);
            }

            r.setBaileyValue(baileyValue);

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
                    SurveyLog.logException(uve2);
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

            Set<String> ts = new TreeSet<String>(); // return set

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

        public synchronized XMLSource makeSource(boolean resolved) {
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
                        loadVoteValues(false /* resolveMorePaths */);
                    }
                    return xmlsource;
                }
            }
        }

        /**
         * Make a vetted source for this PerLocaleData, suitable for producing vxml
         * with vote-resolution done on more paths.
         *
         * This function is similar to makeSource, but with resolveMorePaths true.
         *
         * @return this.xmlsource, the new XMLSource for this PerLocaleData.
         */
        public synchronized XMLSource makeVettedSource() {
            xmlsource = new DataBackedSource(this);
            loadVoteValues(true /* resolveMorePaths */);
            return xmlsource;
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

            if (withVote != null) {
                if (withVote == user.getLevel().getVotes()) {
                    withVote = null; // not an override
                } else if (withVote != user.getLevel().canVoteAtReducedLevel()) {
                    throw new VoteNotAcceptedException(ErrorCode.E_NO_PERMISSION, "User " + user + " cannot vote at " + withVote + " level ");
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

            if (!readonly) {
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
                int xpathId = sm.xpt.getByXpath(distinguishingXpath);
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
                        add2 = "and not exists (select * from " + DBUtils.Table.VOTE_VALUE_ALT + " where " + DBUtils.Table.VOTE_VALUE_ALT + ".locale="
                            + DBUtils.Table.VOTE_VALUE
                            + ".locale and " + DBUtils.Table.VOTE_VALUE_ALT + ".xpath=" + DBUtils.Table.VOTE_VALUE + ".xpath " + " and "
                            + DBUtils.Table.VOTE_VALUE_ALT
                            + ".value=" + DBUtils.Table.VOTE_VALUE + ".value )";
                    }
                    String sql = "insert " + add0 + " into " + DBUtils.Table.VOTE_VALUE_ALT + "   " + add1 + " select " + DBUtils.Table.VOTE_VALUE + ".locale,"
                        + DBUtils.Table.VOTE_VALUE + ".xpath," + DBUtils.Table.VOTE_VALUE + ".value "
                        + " from " + DBUtils.Table.VOTE_VALUE + " where locale=? and xpath=? and submitter=? and value is not null " + add2;
                    // if(DEBUG) System.out.println(sql);
                    saveOld = DBUtils.prepareStatementWithArgs(conn, sql, locale.getBaseName(), xpathId, user.id);

                    int oldSaved = saveOld.executeUpdate();
                    // System.err.println("SaveOld: saved " + oldSaved +
                    // " values");

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

                    {
                        int colNum = 1;
                        ps.setString(colNum++, locale.getBaseName());
                        ps.setInt(colNum++, xpathId);
                        ps.setInt(colNum++, submitter);
                        DBUtils.setStringUTF8(ps, colNum++, value);
                        DBUtils.setInteger(ps, colNum++, withVote);
                    }
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
                    SurveyLog.logException(e);
                    SurveyMain.busted("Could not vote for value in locale locale " + locale, e);
                    throw new InternalError("Could not load locale " + locale + " : " + DBUtils.unchainSqlException(e));
                } finally {
                    DBUtils.close(saveOld, rs, ps, ps2, conn);
                }
                SurveyLog.debug(et);

                if (didClearFlag) {
                    // now, outside of THAT txn, make a forum post about clearing the flag.
                    final String forum = SurveyForum.localeToForum(locale.toULocale());
                    final int forumNumber = sm.fora.getForumNumber(forum);
                    int newPostId;
                    try {
                        newPostId = sm.fora.doPostInternal(xpathId, -1, locale, "Flag Removed", "(The flag was removed.)", false, user);
                        //sm.fora.emailNotify(ctx, forum, base_xpath, subj, text, postId);
                        SurveyLog.warnOnce("TODO: no email notify on flag clear. This may be OK, it could be a lot of mail.");
                        System.out.println("NOTE: flag was removed from " + locale + " " + distinguishingXpath + " - post ID=" + newPostId + "  by "
                            + user.toString());
                    } catch (SurveyException e) {
                        SurveyLog.logException(e, "Error trying to post that a flag was removed from " + locale + " " + distinguishingXpath);
                    }
                }

            } else {
                readonly();
            }

            internalSetVoteForValue(user, distinguishingXpath, value, withVote, new Date());
            xmlsource.setValueFromResolver(distinguishingXpath, null, false /* resolveMorePaths */);
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
        public synchronized void deleteValue(User user, String distinguishingXpath, String value) throws BallotBox.InvalidXPathException {
            if (!getPathsForFile().contains(distinguishingXpath)) {
                throw new BallotBox.InvalidXPathException(distinguishingXpath);
            }

            //make sure user is not deleting a path with 1 or more votes
            if (getVotesForValue(distinguishingXpath, value) != null) {
                SurveyLog.debug("failed to delete value: " + value + " because it has 1 or more votes");
                return;
            }

            SurveyLog.debug("V4v: " + locale + " " + distinguishingXpath + " : " + user + " deleting '" + value + "'");
            ModifyDenial denial = UserRegistry.userCanModifyLocaleWhy(user, locale); // this
            // has
            // to
            // do
            // with
            // changing
            // a
            // vote
            // -
            // not
            // counting
            // it.
            if (denial != null) {
                throw new IllegalArgumentException("User " + user + " cannot modify " + locale + " " + denial);
            }
            if (!readonly) {
                makeSource(false);
                ElapsedTimer et = !SurveyLog.DEBUG ? null : new ElapsedTimer("{0} Recording PLD for " + locale + " "
                    + distinguishingXpath + " : " + user + " deleting '" + value);
                Connection conn = null;
                PreparedStatement ps = null;
                try {
                    conn = DBUtils.getInstance().getDBConnection();

                    ps = DBUtils.prepareForwardReadOnly(conn, "DELETE FROM " + DBUtils.Table.VOTE_VALUE_ALT + " where value=? ");

                    DBUtils.setStringUTF8(ps, 1, value);

                    ps.executeUpdate();

                    conn.commit();
                } catch (SQLException e) {
                    SurveyLog.logException(e);
                    SurveyMain.busted("Could not delete value " + value + " in locale locale " + locale, e);
                    throw new InternalError("Could not load locale " + locale + " : " + DBUtils.unchainSqlException(e));
                } finally {
                    DBUtils.close(ps, conn);
                }
                SurveyLog.debug(et);
            } else {
                readonly();
            }

            internalDeleteValue(user, distinguishingXpath, value, null, xmlsource); // will create/throw away a resolver.
        }

        /**
         * @param user
         * @param distinguishingXpath
         * @param value
         * @param source
         * @return
         * 
         * TODO: does this function accomplish anything? Deleted stub removeFromOthers
         * Called by deleteValue only.
         */
        private final VoteResolver<String> internalDeleteValue(User user, String distinguishingXpath, String value,
            VoteResolver<String> resolver, DataBackedSource source) throws InvalidXPathException {
            if (!getPathsForFile().contains(distinguishingXpath)) {
                throw new InvalidXPathException(distinguishingXpath);
            }
            stamp.next();
            return resolver = source.setValueFromResolver(distinguishingXpath, resolver, false /* resolveMorePaths */);
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

    private static boolean checkHadVotesSometimeThisRelease = true;

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
            // SurveyLog.logger.warning("@@@@ ("+this.getLocaleID()+")" +
            // path+"="+v);
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
    private Map<CLDRLocale, Reference<PerLocaleData>> locales = new HashMap<CLDRLocale, Reference<PerLocaleData>>();

    private LruMap<CLDRLocale, PerLocaleData> rLocales = new LruMap<CLDRLocale, PerLocaleData>(5);

    private Map<CLDRLocale, MutableStamp> localeStamps = new ConcurrentHashMap<CLDRLocale, MutableStamp>(SurveyMain.getLocales().length);

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
        PerLocaleData pld = rLocales.get(locale);
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
                locales.put(locale, (new SoftReference<PerLocaleData>(pld)));
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

    public ExampleGenerator getExampleGenerator() {
        CLDRFile fileForGenerator = sm.getBaselineFile();

        if (fileForGenerator == null) {
            SurveyLog.logger.warning("Err: fileForGenerator is null for ");
        }
        ExampleGenerator exampleGenerator = new ExampleGenerator(fileForGenerator, sm.getBaselineFile(), SurveyMain.fileBase
            + "/../supplemental/");
        exampleGenerator.setVerboseErrors(sm.twidBool("ExampleGenerator.setVerboseErrors"));
        return exampleGenerator;
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

    private Map<CLDRLocale, Set<CLDRLocale>> subLocaleMap = new HashMap<CLDRLocale, Set<CLDRLocale>>();
    Set<CLDRLocale> allLocales = null;

    /**
     * Cache..
     */
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
         * TODO: remove unused locale from SELECT (not from WHERE)
         */
        return DBUtils
            .prepareForwardUpdateable(conn, "SELECT xpath,submitter,value,locale," + VOTE_OVERRIDE + ",last_mod FROM " + DBUtils.Table.VOTE_VALUE
                + " WHERE locale = ?");
    }

    private synchronized final void setupDB() {
        if (dbIsSetup)
            return;
        dbIsSetup = true; // don't thrash.
        Connection conn = null;
        String sql = "(none)"; // this points to
        Statement s = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
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
                // SurveyLog.logger.info(sql);
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
                // SurveyLog.logger.info(sql);
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
                // SurveyLog.logger.info(sql);
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
        } catch (SQLException se) {
            SurveyLog.logException(se, "SQL: " + sql);
            SurveyMain.busted("Setting up DB for STFactory, SQL: " + sql, se);
            throw new InternalError("Setting up DB for STFactory, SQL: " + sql);
        } finally {
            DBUtils.close(s, conn);
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
                final Pair<CLDRLocale, Integer> theKey = new Pair<CLDRLocale, Integer>(locale, xpath);
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
                loadFlag().remove(new Pair<CLDRLocale, Integer>(locale, xpath));
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
            return loadFlag().contains(new Pair<CLDRLocale, Integer>(locale, xpath));
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

            flagList = new HashSet<Pair<CLDRLocale, Integer>>();

            System.out.println("Loading flagged items from .." + DBUtils.Table.VOTE_FLAGGED);
            try {
                for (Map<String, Object> r : DBUtils.queryToArrayAssoc("select * from " + DBUtils.Table.VOTE_FLAGGED)) {
                    flagList.add(new Pair<CLDRLocale, Integer>(CLDRLocale.getInstance(r.get("locale").toString()),
                        (Integer) r.get("xpath")));
                }
                System.out.println("Loaded " + flagList.size() + " items into flagged list.");
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
            SurveyLog.warnOnce("PH for path " + xpath + t.toString());
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
     * Resolving old file, or null if none.
     *
     * @param locale
     * @return
     */
    public CLDRFile getOldFileResolved(CLDRLocale locale) {
        return get(locale).getOldFileResolved();
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
        Set<String> ret = new HashSet<String>();
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
            conn = DBUtils.getInstance().getDBConnection();

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
            SurveyLog.logException(e);
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

        Vector<Integer> ret = new Vector<Integer>();

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

            final PreparedStatement myInsert = ps2 = DBUtils.prepareStatementForwardReadOnly(conn, "myInser", "INSERT INTO  "
                + DBUtils.Table.VOTE_VALUE + " (locale,xpath,submitter,value," + VOTE_OVERRIDE + ") VALUES (?,?,?,?) ");
            final SurveyMain sm2 = sm;
            myReader.setHandler(new XMLFileReader.SimpleHandler() {
                int nusers = 0;
                int maxUserId = 1;

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
                        SurveyLog.logException(e, "importing  - " + path + " = " + value);
                        throw new IllegalArgumentException(e);
                    }
                };
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
            SurveyLog.logException(e, "importing locale data from files");
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
}
