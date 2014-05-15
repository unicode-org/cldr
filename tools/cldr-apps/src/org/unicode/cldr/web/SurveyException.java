package org.unicode.cldr.web;

/**
 * An exception class that has an err_code that the ST client understands.
 * @author srl
 *
 */
public class SurveyException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = -3666193665068094456L;

    /**
     * Keep in sync with stui.js
     * @author srl
     *
     */
    enum ErrorCode {
        E_UNKNOWN, 
        E_INTERNAL, 
        E_BAD_SECTION,
        E_BAD_LOCALE,
        E_NOT_STARTED,
        E_SPECIAL_SECTION, 
        E_SESSION_DISCONNECTED, 
        E_DISCONNECTED, 
        E_NO_PERMISSION,
        E_NOT_LOGGED_IN, E_BAD_VALUE, E_BAD_XPATH;
    }

    private final ErrorCode err_code;;
    
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
}
