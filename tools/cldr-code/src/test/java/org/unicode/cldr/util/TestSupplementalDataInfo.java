package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;

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

    @Test
    void TestArabicPlurals() {
        final SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        PluralRules arrules = sdi.getPluralRules(ULocale.forLanguageTag("ar"), PluralRules.PluralType.CARDINAL);
        assertNotNull(arrules, "ar rules");
        assertTrue(arrules.getKeywords().contains("two"), "ar did not have two");
        assertEquals(2.0, arrules.getUniqueKeywordValue("two"), "ar unique value for 'two'");
    }

    @Test
    void TestMaltesePlurals() {
        final SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        PluralRules mtrules = sdi.getPluralRules(ULocale.forLanguageTag("mt"), PluralRules.PluralType.CARDINAL);
        assertNotNull(mtrules, "mt rules");
        assertTrue(mtrules.getKeywords().contains("two"), "mt did not have two");
        assertEquals(2.0, mtrules.getUniqueKeywordValue("two"), "mt unique value for 'two'");
    }
}
