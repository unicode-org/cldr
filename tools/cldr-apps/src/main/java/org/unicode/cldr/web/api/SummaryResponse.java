package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.json.JSONObject;
import org.unicode.cldr.web.VettingViewerQueue;


@Schema(description = "Summary Response")
public final class SummaryResponse {
    public SummaryResponse() {

    }
    @Schema(description = "Java status object")
    public JSONObject jStatus;
    @Schema(description = "vetting viewer status enum")
    public VettingViewerQueue.Status status;
    @Schema(description = "HTML current status")
    public String ret;
    @Schema(description = "HTML output on success")
    public String output;
}
