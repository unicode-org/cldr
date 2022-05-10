package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.SupplementalDataInfo.ApprovalRequirementMatcher;

public class TestSupplementalDataInfo {
    @Test
    void TestApprovalRequirementMatcher() {
        {
            ApprovalRequirementMatcher arm = new ApprovalRequirementMatcher("//supplementalData/coverageLevels/approvalRequirements/approvalRequirement[@votes=\"=HIGH_BAR\"][@locales=\"ssy hy\"][@paths=\"//ldml/identity\"]");
            assertNotNull(arm);
            assertEquals(VoteResolver.HIGH_BAR, arm.getRequiredVotes());
        }
        {
            ApprovalRequirementMatcher arm = new ApprovalRequirementMatcher("//supplementalData/coverageLevels/approvalRequirements/approvalRequirement[@votes=\"=LOWER_BAR\"][@locales=\"ssy hy\"][@paths=\"//ldml/identity\"]");
            assertNotNull(arm);
            assertEquals(VoteResolver.LOWER_BAR, arm.getRequiredVotes());
        }
        {
            ApprovalRequirementMatcher arm = new ApprovalRequirementMatcher("//supplementalData/coverageLevels/approvalRequirements/approvalRequirement[@votes=\"=tc\"][@locales=\"ssy hy\"][@paths=\"//ldml/identity\"]");
            assertNotNull(arm);
            assertEquals(VoteResolver.Level.tc.getVotes(Organization.guest), arm.getRequiredVotes());
        }
        {
            ApprovalRequirementMatcher arm = new ApprovalRequirementMatcher("//supplementalData/coverageLevels/approvalRequirements/approvalRequirement[@votes=\"3\"][@locales=\"ssy hy\"][@paths=\"//ldml/identity\"]");
            assertNotNull(arm);
            assertEquals(3, arm.getRequiredVotes());
        }
    }
}
