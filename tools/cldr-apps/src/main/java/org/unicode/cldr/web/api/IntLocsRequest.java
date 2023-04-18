package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class IntLocsRequest {
    @Schema(required = true, description = "Session string")
    public String sessionString;

    @Schema(required = true, description = "Email address")
    public String email;

    @Schema(required = true, description = "Interest locales")
    public String intlocs;

    public IntLocsRequest() {}
}
