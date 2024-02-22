package org.unicode.cldr.web;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An exception class that has an err_code that the ST client understands.
 *
 * @author srl
 */
public class SurveyException extends Exception {
    /** */
    private static final long serialVersionUID = -3666193665068094456L;

    /**
     * Keep in sync with cldrText.js
     *
     * @author srl
     */
    public enum ErrorCode {
        E_UNKNOWN,
        E_INTERNAL,
        E_BAD_SECTION,
        E_BAD_LOCALE,
        E_NOT_STARTED,
        E_SPECIAL_SECTION,
        E_SESSION_DISCONNECTED,
        E_DISCONNECTED,
        E_NO_PERMISSION,
        E_NOT_LOGGED_IN,
        E_BAD_VALUE,
        E_BAD_XPATH,
        E_NO_OLD_VOTES,
        E_PERMANENT_VOTE_NO_FORUM,
        E_VOTE_NOT_ACCEPTED;
    }

    private final ErrorCode err_code;

    public void addDataTo(SurveyJSONWrapper r) throws JSONException {
        r.put(
                "err_data",
                new JSONObject()
                        .put("message", getMessage())
                        .put("class", this.getClass().getSimpleName())
                        .put("cause", this.getCause()));
    }

    public ErrorCode getErrCode() {
        return err_code;
    }

    public SurveyException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        err_code = code;
    }

    public SurveyException(ErrorCode code, String message) {
        super(message);
        err_code = code;
    }

    public SurveyException(ErrorCode code) {
        super(code.toString());
        err_code = code;
    }

    public SurveyException(ErrorCode err_code, String string, JSONObject err_data) {
        super(string);
        this.err_code = err_code;
    }
}
