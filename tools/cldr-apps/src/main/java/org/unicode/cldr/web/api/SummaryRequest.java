package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.web.VettingViewerQueue;
import org.unicode.cldr.web.VettingViewerQueue.LoadingPolicy;


@Schema(description = "Summary Request")
public final class SummaryRequest {
    public SummaryRequest() {

    }
    @Schema(implementation = VettingViewerQueue.LoadingPolicy.class, defaultValue = "NOSTART")
    public VettingViewerQueue.LoadingPolicy loadingPolicy = LoadingPolicy.NOSTART;
}
