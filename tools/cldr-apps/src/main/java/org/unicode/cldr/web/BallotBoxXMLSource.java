package org.unicode.cldr.web;

import java.util.Date;
import java.util.logging.Logger;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.XMLSource;

/**
 * Formerly DataBackedSource. This XMLSource is not modifiable by the user (putFullPathAtDPath), but
 * only responds to changes via votes.
 */
public class BallotBoxXMLSource<T> extends DelegateXMLSource {
    static final Logger logger = SurveyLog.forClass(BallotBoxXMLSource.class);

    BallotBox<T> ballotBox;
    /** original data before any votes */
    XMLSource diskData;

    /** Writeable XMLSource to delegate to */
    BallotBoxXMLSource(XMLSource diskData, BallotBox<T> makeFrom) {
        super(diskData.cloneAsThawed());
        this.diskData = diskData;
        ballotBox = makeFrom;
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
    VoteResolver<String> setValueFromResolver(
            String path,
            VoteResolver<String> resolver,
            STFactory.VoteLoadingContext voteLoadingContext,
            STFactory.PerLocaleData.PerXPathData xpd) {
        String value;
        String fullPath;
        /*
         * If there are no votes, it may be more efficient (or anyway expected) to skip vote resolution
         * and use diskData instead. This has far-reaching effects and should be better documented.
         * When and how does it change the outcome and/or performance?
         * Currently only skip for VoteLoadingContext.ORDINARY_LOAD_VOTES with null/empty xpd.
         *
         * Do not skip vote resolution if VoteLoadingContext.SINGLE_VOTE, even for empty xpd. Otherwise an Abstain can
         * result in "no votes", "skip vote resolution", failure to get the right winning value, possibly inherited.
         */

        /**
         * TODO: move this into the caller somehow. Maybe just use a special resolver that just
         * calls into diskData.
         */
        if (voteLoadingContext == STFactory.VoteLoadingContext.ORDINARY_LOAD_VOTES
                && (xpd == null || xpd.isEmpty())) {
            /*
             * Skip vote resolution
             */
            value = diskData.getValueAtDPath(path);
            fullPath = diskData.getFullPathAtDPath(path);
        } else {
            resolver = ballotBox.getResolver(path, resolver);
            value = resolver.getWinningValue();
            fullPath = getFullPathWithResolver(path, resolver);
        }
        delegate.removeValueAtDPath(path);
        if (value != null) {
            delegate.putValueAtPath(fullPath, value);
        }
        return resolver;
    }

    private String getFullPathWithResolver(String path, VoteResolver<String> resolver) {
        String diskFullPath = diskData.getFullPathAtDPath(path);
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
     *     <p>As a rule, the name of each VoteResolver.Status is also the name of a DraftStatus. Any
     *     exceptions to that rule should be handled explicitly in this function. However,
     *     VoteResolver.Status.missing is currently NOT handled and will cause an exception to be
     *     logged. The caller should check for VoteResolver.Status.missing and avoid calling this
     *     function with it.
     *     <p>References: https://unicode.org/cldr/trac/ticket/11721
     *     https://unicode.org/cldr/trac/ticket/11766 https://unicode.org/cldr/trac/ticket/11103
     */
    private static final DraftStatus draftStatusFromWinningStatus(VoteResolver.Status win) {
        try {
            return DraftStatus.forString(win.toString());
        } catch (IllegalArgumentException e) {
            SurveyLog.logException(
                    logger, e, "Exception in draftStatusFromWinningStatus of " + win);
            return DraftStatus.unconfirmed;
        }
    }
}
