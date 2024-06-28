package org.unicode.cldr.web.api;

public final class LoginRequest {
    public String email;
    public String password;
    public String jwt;

    /**
     * @return true if input incomplete
     */
    protected boolean isEmpty() {
        if (jwt != null) return false;
        return email == null || password == null || email.isEmpty() || password.isEmpty();
    }
}
