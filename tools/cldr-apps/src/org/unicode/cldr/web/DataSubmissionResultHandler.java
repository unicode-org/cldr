package org.unicode.cldr.web;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.DataSection.DataRow.CandidateItem;

/**
 * Callback interface for handling the results of user input changes to survey
 * tool data. The survey tool will call these functions to record changes.
 *
 * @author srl
 *
 */
public interface DataSubmissionResultHandler {

    /**
     * If nonzero, some results were updated.
     *
     * @param resultCount
     *            number of results which were updated.
     */
    void handleResultCount(int resultCount);

    /**
     * Items were removed from a row.
     *
     * @param row
     *            which row
     * @param item
     *            item which was removed
     * @param voteRemoved
     *            The item had the user's vote on it, which was also removed.
     */
    void handleRemoveItem(DataRow row, CandidateItem item, boolean voteRemoved);

    /**
     * Failure: You didn't have permission to do this operation
     *
     * @param p
     *            Row in question
     * @param optionalItem
     *            item in question, or null
     * @param string
     *            informational string
     */
    void handleNoPermission(DataRow p, CandidateItem optionalItem, String string);

    /**
     * A vote was removed
     *
     * @param p
     *            which row
     * @param voter
     *            which users' vote
     * @param item
     *            which item
     */
    void handleRemoveVote(DataRow p, UserRegistry.User voter, CandidateItem item);

    /**
     * An requested change was empty.
     *
     * @param p
     *            row
     */
    void handleEmptyChangeto(DataRow p);

    /**
     * User was already voting for this item
     *
     * @param p
     *            row
     * @param item
     *            item
     */
    void warnAlreadyVotingFor(DataRow p, CandidateItem item);

    /**
     * User voted for an extant item, it was accepted as a vote for that item
     *
     * @param p
     *            row
     * @param item
     *            the extant item
     */
    void warnAcceptedAsVoteFor(DataRow p, CandidateItem item);

    /**
     * A new value was accepted
     *
     * @param p
     *            row
     * @param choice_v
     *            new value
     * @param hadFailures
     *            true if there were failures
     */
    void handleNewValue(DataRow p, String choice_v, boolean hadFailures);

    /**
     * Error when adding/changing value.
     *
     * @param p
     *            row
     * @param status
     *            which error
     * @param choice_v
     *            which new value
     */
    void handleError(DataRow p, CheckStatus status, String choice_v);

    /**
     * Item was removed
     *
     * @param p
     */
    void handleRemoved(DataRow p);

    /**
     * Vote was accepted
     *
     * @param p
     * @param oldVote
     * @param value
     */
    void handleVote(DataRow p, String oldVote, String value);

    /**
     * Internal error, an unknown choice was made.
     *
     * @param p
     * @param choice
     */
    void handleUnknownChoice(DataRow p, String choice);

    /**
     * @return true if this errored item should NOT be added to the data.
     * @param p
     */
    boolean rejectErrorItem(DataRow p);

    /**
     * Note that an item was proposed as an option. Used for UI, to get back the
     * user's previous request.
     *
     * @param p
     * @param choice_v
     */
    void handleProposedValue(DataRow p, String choice_v);

}