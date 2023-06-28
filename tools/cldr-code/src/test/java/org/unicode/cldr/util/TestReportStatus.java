package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.VoteResolver.VoterInfo;
import org.unicode.cldr.util.VoterReportStatus.ReportAcceptability;
import org.unicode.cldr.util.VoterReportStatus.ReportId;
import org.unicode.cldr.util.VoterReportStatus.ReportStatus;

public class TestReportStatus {
    @Test
    void testGetSet() {
        ReportStatus status = new ReportStatus();

        assertEquals(
                EnumSet.noneOf(ReportId.class),
                status.completed,
                "newly minted settings should be empty");

        // punch some buttons
        status.mark(ReportId.compact, true, false);
        assertEquals(EnumSet.of(ReportId.compact), status.completed, "after marking 'compact' set");
        assertEquals(
                EnumSet.noneOf(ReportId.class), status.acceptable, "after marking 'compact' set");

        // punch all buttons
        for (ReportId r : ReportId.values()) {
            status.mark(r, true, true);
        }
        assertEquals(
                EnumSet.allOf(ReportId.class), status.completed, "after marking 'compact' set");
        assertEquals(
                EnumSet.allOf(ReportId.class), status.acceptable, "after marking 'compact' set");

        // unset one
        status.mark(ReportId.compact, false, false);
        {
            EnumSet<ReportId> s = status.completed;
            assertNotEquals(EnumSet.allOf(ReportId.class), s, "after unsetting 'compact'");
            assertEquals(
                    s.size(),
                    ReportId.values().length - 1,
                    () -> "Expected all but one was set after unsetting compact: " + s.toString());
        }
        {
            EnumSet<ReportId> s = status.acceptable;
            assertNotEquals(EnumSet.allOf(ReportId.class), s, "after unsetting 'compact'");
            assertEquals(
                    s.size(),
                    ReportId.values().length - 1,
                    () -> "Expected all but one was set after unsetting compact: " + s.toString());
        }

        status.mark(ReportId.compact, true, false); // complete but not acceptable
        {
            EnumSet<ReportId> s = status.completed;
            assertEquals(EnumSet.allOf(ReportId.class), s, "after unsetting 'compact'");
        }
        {
            EnumSet<ReportId> s = status.acceptable;
            assertNotEquals(EnumSet.allOf(ReportId.class), s, "after unsetting 'compact'");
            assertEquals(
                    s.size(),
                    ReportId.values().length - 1,
                    () -> "Expected all but one was set after unsetting compact: " + s.toString());
        }
    }

    @Test
    @Disabled("until CLDR-15765 merged, not reliable due to static init")
    void testResolver() {
        Map<Integer, VoterInfo> m = new HashMap<>();
        final int VETTER1 = 1;
        final int GUEST2 = 2;
        final int TC3 = 3;
        final int VETTER4 = 4;
        m.put(
                VETTER1,
                new VoterInfo(
                        Organization.meta,
                        VoteResolver.Level.vetter,
                        "1-meta-vetter",
                        new LocaleSet(true)));
        m.put(
                GUEST2,
                new VoterInfo(
                        Organization.unaffiliated,
                        VoteResolver.Level.guest,
                        "2-guest-guest",
                        new LocaleSet(true)));
        m.put(
                TC3,
                new VoterInfo(
                        Organization.surveytool,
                        VoteResolver.Level.tc,
                        "3-st-tc",
                        new LocaleSet(true)));
        m.put(
                VETTER4,
                new VoterInfo(
                        Organization.wikimedia,
                        VoteResolver.Level.vetter,
                        "1-wiki-vetter",
                        new LocaleSet(true)));
        VoteResolver<ReportAcceptability> res =
                new VoteResolver<>(
                        new VoterInfoList().setVoterToInfo(m) // NEW - UNCOMMENT after CLDR-15765
                        );

        MemVoterReportStatus<Integer> mrv = new MemVoterReportStatus<>();

        final CLDRLocale french = CLDRLocale.getInstance("fr");
        final CLDRLocale assamese = CLDRLocale.getInstance("ssy");

        // update resolver
        Map<Integer, ReportAcceptability> votes = new TreeMap<>();
        mrv.updateResolver(french, ReportId.compact, m.keySet(), res, votes);

        // should be missing
        assertAll("fr: no votes yet", () -> assertEquals(Status.missing, res.getWinningStatus()));

        mrv.markReportComplete(VETTER1, french, ReportId.compact, true, true);
        mrv.updateResolver(french, ReportId.compact, m.keySet(), res, votes);
        assertAll(
                "fr: vetter1 voted",
                () -> assertEquals(Status.approved, res.getWinningStatus()),
                () -> assertEquals(ReportAcceptability.acceptable, res.getWinningValue()));

        // now dispute it as a guest (mark as not acceptable)
        mrv.markReportComplete(GUEST2, french, ReportId.compact, true, false);
        mrv.updateResolver(french, ReportId.compact, m.keySet(), res, votes);
        assertAll(
                "fr: guest2 unacceptable",
                () -> assertEquals(Status.approved, res.getWinningStatus()),
                () -> assertEquals(ReportAcceptability.acceptable, res.getWinningValue()));
        // more support for unacceptability
        mrv.markReportComplete(VETTER4, french, ReportId.compact, true, false);
        mrv.updateResolver(french, ReportId.compact, m.keySet(), res, votes);
        assertAll(
                "fr: vetter4 also unacceptable",
                () -> assertEquals(Status.approved, res.getWinningStatus()),
                () -> assertEquals(ReportAcceptability.notAcceptable, res.getWinningValue()));
        // guest drops out (abstention). Now it is a dispute.
        mrv.markReportComplete(GUEST2, french, ReportId.compact, false, false);
        mrv.updateResolver(french, ReportId.compact, m.keySet(), res, votes);
        assertAll(
                "fr: guest2 unvotes",
                // now provisional
                () -> assertEquals(Status.provisional, res.getWinningStatus())
                // Winning value is acceptable - is that due to comparison  (a… < n…)?
                // () -> assertEquals(ReportAcceptability.acceptable, res.getWinningValue())
                );

        // Now in Assamese.
        mrv.markReportComplete(GUEST2, assamese, ReportId.zones, true, false);
        mrv.updateResolver(assamese, ReportId.zones, m.keySet(), res, votes);
        assertAll(
                "ssy: guest2 votes notAcceptable",
                // now unconfirmed
                () -> assertEquals(Status.unconfirmed, res.getWinningStatus()),
                // Winning value is acceptable - is that due to comparison  (a… < n…)?
                () -> assertEquals(ReportAcceptability.notAcceptable, res.getWinningValue()));
    }
}
