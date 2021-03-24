package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.web.UserRegistry.User;

public final class LoginResponse {
    LoginResponse() {
        sessionId = null;
        user = null;
        newlyLoggedIn = false;
    }
    @Schema(description = "CookieSession string id")
    public String sessionId;
    @Schema(description = "If set, contains the current user’s information")
    public User user;
    @Schema(description = "True if the session was created by this API call")
    public boolean newlyLoggedIn;
}