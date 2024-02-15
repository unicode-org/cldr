package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.SupplementalDataInfo.ApprovalRequirementMatcher;
import org.unicode.cldr.util.SupplementalDataInfo.ParentLocaleComponent;

public class TestSupplementalDataInfo {
    @Test
    void TestApprovalRequirementMatcher() {
        {
            ApprovalRequirementMatcher arm =
                    new ApprovalRequirementMatcher(
                            "//supplementalData/coverageLevels/approvalRequirements/approvalRequirement[@votes=\"=HIGH_BAR\"][@locales=\"ssy hy\"][@paths=\"//ldml/identity\"]");
            assertNotNull(arm);
            assertEquals(VoteResolver.HIGH_BAR, arm.getRequiredVotes());
        }
        {
            ApprovalRequirementMatcher arm =
                    new ApprovalRequirementMatcher(
                            "//supplementalData/coverageLevels/approvalRequirements/approvalRequirement[@votes=\"=LOWER_BAR\"][@locales=\"ssy hy\"][@paths=\"//ldml/identity\"]");
            assertNotNull(arm);
            assertEquals(VoteResolver.LOWER_BAR, arm.getRequiredVotes());
        }
        {
            ApprovalRequirementMatcher arm =
                    new ApprovalRequirementMatcher(
                            "//supplementalData/coverageLevels/approvalRequirements/approvalRequirement[@votes=\"=tc\"][@locales=\"ssy hy\"][@paths=\"//ldml/identity\"]");
            assertNotNull(arm);
            assertEquals(
                    VoteResolver.Level.tc.getVotes(Organization.unaffiliated),
                    arm.getRequiredVotes());
        }
        {
            ApprovalRequirementMatcher arm =
                    new ApprovalRequirementMatcher(
                            "//supplementalData/coverageLevels/approvalRequirements/approvalRequirement[@votes=\"3\"][@locales=\"ssy hy\"][@paths=\"//ldml/identity\"]");
            assertNotNull(arm);
            assertEquals(3, arm.getRequiredVotes());
        }
    }

    @Test
    void TestArabicPlurals() {
        final SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        PluralRules arrules =
                sdi.getPluralRules(ULocale.forLanguageTag("ar"), PluralRules.PluralType.CARDINAL);
        assertNotNull(arrules, "ar rules");
        assertTrue(arrules.getKeywords().contains("two"), "ar did not have two");
        assertEquals(2.0, arrules.getUniqueKeywordValue("two"), "ar unique value for 'two'");
    }

    @Test
    void TestMaltesePlurals() {
        final SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        PluralRules mtrules =
                sdi.getPluralRules(ULocale.forLanguageTag("mt"), PluralRules.PluralType.CARDINAL);
        assertNotNull(mtrules, "mt rules");
        assertTrue(mtrules.getKeywords().contains("two"), "mt did not have two");
        assertEquals(2.0, mtrules.getUniqueKeywordValue("two"), "mt unique value for 'two'");
    }

    @Test
    void TestParentLocales() {
        final SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        String[][] tests = {
            {"main", "zh_Hant_MO", "zh_Hant_HK", "zh_Hant", "root"},
            {"main", "ru_Cyrl_RU", "ru_Cyrl", "ru", "root"},
            {"main", "ru_Latn_RU", "ru_Latn", "root"},
            {"main", "sr_Cyrl_ME", "sr_Cyrl", "sr", "root"},
            {"main", "hi_Latn", "en_IN", "en_001", "en", "root"},
            {"plurals", "ru_Cyrl_RU", "ru_Cyrl", "ru", "root"},
            {"plurals", "ru_Latn_RU", "ru_Latn", "ru", "root"},
            {"plurals", "zh_Hant_MO", "zh_Hant", "zh", "root"},
            {"collations", "sr_Cyrl_ME", "sr_ME", "sr", "root"},
        };
        for (String[] test : tests) {
            ParentLocaleComponent component = ParentLocaleComponent.fromString(test[0]);
            String child = test[1];
            for (int i = 2; i < test.length; ++i) {
                final String expected = test[i];
                String parent = LocaleIDParser.getParent(child, component);
                assertEquals(expected, parent, component + "/" + child);
                child = parent;
            }
        }
    }
}
