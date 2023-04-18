package org.unicode.cldr.web.api;

public final class LockRequest {
    /**
     * The E-mail is included so that the server can require it to match the E-mail implicit in the
     * session ID, to double-check that the client knows whom it's locking
     */
    public String email;

    public String session;
    public String reason;

    /**
     * @return true if input incomplete
     */
    protected boolean isEmpty() {
        return email == null
                || email.isEmpty()
                || reason == null
                || reason.isEmpty()
                || session == null
                || session.isEmpty();
    }
}
