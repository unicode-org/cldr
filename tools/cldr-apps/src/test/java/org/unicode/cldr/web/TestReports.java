package org.unicode.cldr.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.VoterReportStatus.ReportId;

public class TestReports {
    @Test
    void testGetSet() {
        EphemeralSettings settings = new EphemeralSettings();

        assertEquals(EnumSet.noneOf(ReportId.class),
            Reports.getCompletion(settings), "newly minted settings should be empty");

        // punch some buttons
        Reports.markReportComplete(settings, ReportId.compact, true, false);
        assertEquals(EnumSet.of(ReportId.compact),
            Reports.getCompletion(settings), "after marking 'compact' set");
        assertEquals(EnumSet.noneOf(ReportId.class),
            Reports.getAcceptability(settings), "after marking 'compact' set");

        // punch all buttons
        for (ReportId r : ReportId.values()) {
            Reports.markReportComplete(settings, r, true, true);
        }
        assertEquals(EnumSet.allOf(ReportId.class),
            Reports.getCompletion(settings), "after marking 'compact' set");
        assertEquals(EnumSet.allOf(ReportId.class),
            Reports.getAcceptability(settings), "after marking 'compact' set");

        // unset one
        Reports.markReportComplete(settings, ReportId.compact, false, false);
        {
            EnumSet<ReportId> s = Reports.getCompletion(settings);
            assertNotEquals(EnumSet.allOf(ReportId.class),
                s, "after unsetting 'compact'");
            assertEquals(s.size(), ReportId.values().length - 1,
                () -> "Expected all but one was set after unsetting compact: " + s.toString());
        }
        {
            EnumSet<ReportId> s = Reports.getAcceptability(settings);
            assertNotEquals(EnumSet.allOf(ReportId.class),
                s, "after unsetting 'compact'");
            assertEquals(s.size(), ReportId.values().length - 1,
                () -> "Expected all but one was set after unsetting compact: " + s.toString());
        }

        Reports.markReportComplete(settings, ReportId.compact, true, false); // complete but not acceptable
        {
            EnumSet<ReportId> s = Reports.getCompletion(settings);
            assertEquals(EnumSet.allOf(ReportId.class),
                s, "after unsetting 'compact'");
        }
        {
            EnumSet<ReportId> s = Reports.getAcceptability(settings);
            assertNotEquals(EnumSet.allOf(ReportId.class),
                s, "after unsetting 'compact'");
            assertEquals(s.size(), ReportId.values().length - 1,
                () -> "Expected all but one was set after unsetting compact: " + s.toString());
        }
    }
}
