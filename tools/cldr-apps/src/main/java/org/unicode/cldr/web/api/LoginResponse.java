package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public final class LoginResponse {
    LoginResponse() {
        sessionId = null;
    }

    @Schema(description = "CookieSession string id")
    public String sessionId;

    @Schema(description = "True if there is a user")
    public boolean user;

    @Schema(description = "User name")
    public String name;

    @Schema(description = "User email")
    public String email;

    @Schema(description = "User id")
    public int id;
}
