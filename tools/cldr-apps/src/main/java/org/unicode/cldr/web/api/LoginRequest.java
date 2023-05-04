package org.unicode.cldr.web.api;

public final class LoginRequest {
    public String email;
    public String password;

    /**
     * @return true if input incomplete
     */
    protected boolean isEmpty() {
        return email == null || password == null || email.isEmpty() || password.isEmpty();
    }
}
