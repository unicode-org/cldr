/**
 * 
 */
package org.unicode.cldr.web;

/**
 * @author srl
 * 
 */
public class BallotBoxVoteNotAcceptedException extends Exception {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1462132656348262950L;

    enum Reason {
        NO_AUTHORIZATION, READONLY_LOCALE, INTERNAL_ERROR, VALUE_ERROR
    };

    public BallotBoxVoteNotAcceptedException(Reason r, String message) {
        super(message);
    }

    public BallotBoxVoteNotAcceptedException(Reason r, String message, Throwable t) {
        super(message, t);
    }
}
