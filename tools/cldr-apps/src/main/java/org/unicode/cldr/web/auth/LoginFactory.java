package org.unicode.cldr.web.auth;

/** a LoginFactory is a type of login management that can be used with the ST */
public abstract class LoginFactory {

    /** indicates a login request's intended use */
    public enum LoginIntent {
        // /** for access to the ST. Not implemented yet. */
        // sso,
        /** for CLA assertion */
        cla,
    };

    /** return true if this factory is all ready for use */
    public abstract boolean valid();

    /**
     * Get the URL for the login link.
     *
     * @param intent what type of login link to generate.
     */
    public abstract String getLoginUrl(LoginIntent intent);
}
