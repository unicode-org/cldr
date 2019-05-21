package org.unicode.cldr.util;

import java.util.Collection;
import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.VoteResolver.VoterInfo;

/**
 * Provides detailed information about paths and voters
 *
 * @author markdavis
 *
 */
public class CLDRInfo {

    public interface PathValueInfo { // DataSection.DataRow will implement
        Collection<? extends CandidateInfo> getValues();

        CandidateInfo getCurrentItem();

        String getBaselineValue();
        
        default Status getBaselineStatus() {
            return Status.missing;
        }

        Level getCoverageLevel();

        boolean hadVotesSometimeThisRelease();
        
        CLDRLocale getLocale();

        String getXpath();
    }

    public interface CandidateInfo { // DataSection.DataRow.CandidateItem will implement
        String getValue();

        Collection<UserInfo> getUsersVotingOn();

        List<CheckStatus> getCheckStatusList();
    }

    public interface UserInfo { // UserRegistry.User will implement
        VoterInfo getVoterInfo();
    }
    // TODO merge into VoterInfo.
}
