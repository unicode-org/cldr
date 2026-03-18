/** */
package org.unicode.cldr.web;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteType;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.api.VoteAPIHelper;
import org.unicode.cldr.web.util.JSONObject;

/**
 * @author srl This is an abstract interface for allowing SurveyTool-like input to a CLDRFile. It
 *     could be considered as a getter on XMLSource/CLDRFile. TODO: T could eventually be nailed
 *     down to a concrete type.
 */
public interface BallotBox<T> {

    /**
     * This is thrown when an XPath isn't valid within this locale.
     *
     * @author srl
     */
    public class InvalidXPathException extends SurveyException {
        /** */
        private static final long serialVersionUID = 1310604068301637651L;

        public String xpath;

        public InvalidXPathException(String xpath) {
            super(ErrorCode.E_BAD_XPATH, "Invalid XPath: " + xpath);
            this.xpath = xpath;
        }
    }

    /**
     * @author srl
     */
    public class VoteNotAcceptedException extends SurveyException {
        /** */
        private static final long serialVersionUID = 1462132656348262950L;

        public VoteNotAcceptedException(ErrorCode r, String message) {
            super(r, message);
        }

        public VoteNotAcceptedException(ErrorCode r, String message, Throwable t) {
            super(r, message, t);
        }

        public VoteNotAcceptedException(ErrorCode r, String string, JSONObject err_data) {
            super(r, string, err_data);
        }
    }

    /**
     * @param value raw (non-display) value
     */
    public void voteForValue(T user, String distinguishingXpath, String value)
            throws InvalidXPathException, VoteNotAcceptedException;

    /**
     * @param value raw (non-display) value
     */
    public void voteForValueWithType(
            T user, String distinguishingXpath, String value, VoteType voteType)
            throws VoteNotAcceptedException, InvalidXPathException;

    /**
     * @param value raw (non-display) value
     */
    public void voteForValueWithType(
            T user, String distinguishingXpath, String value, Integer withVote, VoteType voteType)
            throws InvalidXPathException, VoteNotAcceptedException;

    /**
     * Return a vote for a value, as a string
     *
     * @param user user id who
     * @param distinguishingXpath
     * @return
     */
    public String getVoteValue(T user, String distinguishingXpath);

    /**
     * Get all of the voters for this xpath which are for a certain value.
     *
     * @param xpath
     * @param value
     * @return
     */
    public Set<User> getVotesForValue(String xpath, String value);

    /**
     * Get the overrides (if any) from user to votevalue
     *
     * @param xpath
     * @return
     */
    public Map<User, VoteAPIHelper.VoteDetails> getVoteDetailsPerUser(String xpath);

    /**
     * Get the possible user values at this path. Could be null.
     *
     * @param xpath
     * @return
     */
    Set<String> getValues(String xpath);

    /**
     * Get the vote resolver for this path.
     *
     * @param path
     * @return
     */
    VoteResolver<String> getResolver(String path);

    /**
     * Get the vote resolver for this path.
     *
     * @param path
     * @param r resolver to reuse (must be from a prior call)
     * @return
     */
    VoteResolver<String> getResolver(String path, VoteResolver<String> r);

    /**
     * Whether the user voted at all. Returns false if user voted for null (no opinion).
     *
     * @param myUser
     * @param somePath
     * @return
     */
    public boolean userDidVote(User myUser, String somePath);

    public VoteType getUserVoteType(User myUser, String somePath);

    /**
     * Were there any votes some-time this release?
     *
     * @see org.unicode.cldr.util.CLDRInfo.PathValueInfo#hadVotesSometimeThisRelease()
     * @return
     */
    public boolean hadVotesSometimeThisRelease(int xpid);

    /**
     * remove vote. same as voting for null
     *
     * @param user
     * @param xpath
     * @throws VoteNotAcceptedException
     */
    public void unvoteFor(User user, String xpath)
            throws InvalidXPathException, VoteNotAcceptedException;

    /**
     * re-vote for the current vote. Error if no current vote.
     *
     * @param user
     * @param xpath
     */
    public void revoteFor(User user, String xpath)
            throws InvalidXPathException, VoteNotAcceptedException;

    /**
     * Get the last mod date (if known) of the most recent vote.
     *
     * @param xpath
     * @return date or null
     */
    public Date getLastModDate(String xpath);
}
