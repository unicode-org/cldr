package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.VoterReportStatus.ReportId;
import org.unicode.cldr.util.VoterReportStatus.ReportStatus;

public class TestReportStatus {
    @Test
    void testGetSet() {
        ReportStatus status = new ReportStatus();

        assertEquals(EnumSet.noneOf(ReportId.class),
            status.completed, "newly minted settings should be empty");

        // punch some buttons
        status.mark(ReportId.compact, true, false);
        assertEquals(EnumSet.of(ReportId.compact),
        status.completed, "after marking 'compact' set");
        assertEquals(EnumSet.noneOf(ReportId.class),
            status.acceptable, "after marking 'compact' set");

        // punch all buttons
        for (ReportId r : ReportId.values()) {
            status.mark(r, true, true);
        }
        assertEquals(EnumSet.allOf(ReportId.class),
            status.completed, "after marking 'compact' set");
        assertEquals(EnumSet.allOf(ReportId.class),
            status.acceptable, "after marking 'compact' set");

        // unset one
        status.mark(ReportId.compact, false, false);
        {
            EnumSet<ReportId> s = status.completed;
            assertNotEquals(EnumSet.allOf(ReportId.class),
                s, "after unsetting 'compact'");
            assertEquals(s.size(), ReportId.values().length - 1,
                () -> "Expected all but one was set after unsetting compact: " + s.toString());
        }
        {
            EnumSet<ReportId> s = status.acceptable;
            assertNotEquals(EnumSet.allOf(ReportId.class),
                s, "after unsetting 'compact'");
            assertEquals(s.size(), ReportId.values().length - 1,
                () -> "Expected all but one was set after unsetting compact: " + s.toString());
        }

        status.mark(ReportId.compact, true, false); // complete but not acceptable
        {
            EnumSet<ReportId> s = status.completed;
            assertEquals(EnumSet.allOf(ReportId.class),
                s, "after unsetting 'compact'");
        }
        {
            EnumSet<ReportId> s = status.acceptable;
            assertNotEquals(EnumSet.allOf(ReportId.class),
                s, "after unsetting 'compact'");
            assertEquals(s.size(), ReportId.values().length - 1,
                () -> "Expected all but one was set after unsetting compact: " + s.toString());
        }
    }
}
