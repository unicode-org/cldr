package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.web.SurveySnapshot;
import org.unicode.cldr.web.VettingViewerQueue;
import org.unicode.cldr.web.VettingViewerQueue.LoadingPolicy;

@Schema(description = "Summary Request")
public final class SummaryRequest {
    public SummaryRequest() {}

    @Schema(implementation = VettingViewerQueue.LoadingPolicy.class, defaultValue = "NOSTART")
    public VettingViewerQueue.LoadingPolicy loadingPolicy = LoadingPolicy.NOSTART;

    public String snapshotPolicy = SurveySnapshot.SNAP_NONE;

    public String snapshotId = SurveySnapshot.SNAPID_NOT_APPLICABLE;

    public boolean summarizeAllLocales = false;
}
