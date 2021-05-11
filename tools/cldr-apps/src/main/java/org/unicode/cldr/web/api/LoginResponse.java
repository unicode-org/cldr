package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public final class LoginResponse {
    LoginResponse() {
        sessionId = null;
    }

    @Schema(description = "CookieSession string id")
    public String sessionId;

    // TODO: need to add User data here
    // But it stopped being serializable.
    // For now, just a boolean
    @Schema(description = "True if there is a user")
    public boolean user;
}
