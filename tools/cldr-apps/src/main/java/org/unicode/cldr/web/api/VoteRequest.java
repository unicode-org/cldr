package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class VoteRequest {
    @Schema(description = "String value for vote")
    public String value;

    @Schema(
            description =
                    "If set/nonzero, integer level of vote for an overridden vote level, otherwise the default level for the user is used",
            nullable = true,
            defaultValue = "null")
    public Integer voteLevelChanged;
}
