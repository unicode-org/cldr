package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.web.SurveySnapshot;
import org.unicode.cldr.web.VettingViewerQueue;

@Schema(description = "Summary Response")
public final class SummaryResponse {
    public SummaryResponse() {

    }

    @Schema(description = "vetting viewer status enum")
    public VettingViewerQueue.Status status;

    @Schema(description = "HTML current status")
    public String ret;

    @Schema(description = "HTML output on success")
    public String output;

    @Schema(description = "Snapshot ID, or NA for Not Applicable")
    public String snapshotId = SurveySnapshot.SNAPID_NOT_APPLICABLE;
}
