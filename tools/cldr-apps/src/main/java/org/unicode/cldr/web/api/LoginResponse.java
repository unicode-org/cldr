package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public final class LoginResponse {
    LoginResponse() {
        sessionId = null;
    }

    @Schema(description = "CookieSession string id")
    public String sessionId;
}
