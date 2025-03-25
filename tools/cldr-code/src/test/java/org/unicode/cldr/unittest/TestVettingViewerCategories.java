package org.unicode.cldr.unittest;

import java.util.*;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.*;

public class TestVettingViewerCategories extends TestFmwk {

    public void TestNotificationCategories() {
        final Organization org = VettingViewer.getNeutralOrgForSummary();
        final EnumSet<NotificationCategory> set1 =
                VettingViewer.getPriorityItemsSummaryCategories(org);
        final EnumSet<NotificationCategory> set2 = VettingViewer.getLocaleCompletionCategories();
        if (set1.contains(NotificationCategory.abstained)) {
            errln("getPriorityItemsSummaryCategories should not contain abstained");
        }
        if (!set1.contains(NotificationCategory.warning)) {
            errln("getPriorityItemsSummaryCategories should contain warning");
        }
        if (set2.contains(NotificationCategory.warning)) {
            errln("getLocaleCompletionCategories should not contain warning");
        }
        if (!set1.containsAll(set2)) {
            // This assumption is implicit in the way the Progress column of Priority Items Summary
            // is
            // calculated in the same pass as the other Priority Items Summary columns
            errln("getLocaleCompletionCategories be a subset of getPriorityItemsSummaryCategories");
        }
    }
}
