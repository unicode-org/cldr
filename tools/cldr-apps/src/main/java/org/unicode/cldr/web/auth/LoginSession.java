package org.unicode.cldr.web.auth;

/** Abstraction for a validated user login. Generated by a LoginFactory. */
public abstract class LoginSession implements Comparable<LoginSession> {
    protected LoginSession(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        // by default, the display name is the ID
        return getId();
    }

    /** is the session valid? i.e. not expired, etc */
    public boolean isValid() {
        return true;
    }

    /**
     * What is the type of the session? Often the class name of the provider, example: "SomeClass"
     */
    public String getType() {
        return getClass().getSimpleName();
    }

    /**
     * What is the unique (untyped) identifier of the session? Example: "someuser" or
     * "user@example.com"
     */
    public String getId() {
        return id;
    }

    /**
     * Combine the type and ID into a single string. This is considered a unique identifier for this
     * session.
     */
    public String getKey() {
        return String.format("%s:%s", getType(), getId());
    }

    @Override
    public String toString() {
        return getKey();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || !(other instanceof LoginSession)) {
            return false;
        } else {
            return getKey().equals(((LoginSession) other).getKey());
        }
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    @Override
    public int compareTo(LoginSession other) {
        return getKey().compareTo(other.getKey());
    }

    private final String id;
}
