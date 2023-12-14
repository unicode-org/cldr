package org.unicode.cldr.web.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.web.SurveyException;
import org.unicode.cldr.web.SurveyException.ErrorCode;

@Schema(name = "STError", description = "Error return object")
public class STError {
    public STError(String desc) {
        this.message = desc;
    }

    public STError(Throwable t) {
        this(t, null);
    }

    public STError(Throwable t, String preMessage) {
        if (preMessage == null) {
            preMessage = "Exception:";
        }
        setMessage(preMessage + " " + t.toString());
        if (t instanceof SurveyException) {
            SurveyException se = (SurveyException) t;
            setCode(se.getErrCode());
        } else {
            setCode(ErrorCode.E_INTERNAL);
        }
    }

    public STError(ErrorCode code, String string) {
        this.code = code;
        this.message = string;
    }

    public STError(ErrorCode code) {
        this.code = code;
        this.message = "Error: " + code;
    }

    private void setCode(ErrorCode code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    /** Description of the error */
    @Schema(description = "Error message")
    public String message;
    /** identifies this as an error */
    @Schema(description = "Always set to true, identifies this as an error.")
    public final boolean err = true;
    /** Optional error code */
    @Schema(description = "Error code if present")
    public ErrorCode code;
    /**
     * Convenience function: return STError("something").build() => 500
     *
     * @return
     */
    public Response build() {
        return Response.status(getStatus()).entity(this).build();
    }

    /** the error as a Status */
    public Status getStatus() {
        switch (code) {
            case E_BAD_LOCALE:
            case E_BAD_SECTION:
            case E_BAD_XPATH:
                return Response.Status.NOT_FOUND;
            case E_NO_PERMISSION:
                return Response.Status.FORBIDDEN;
            case E_BAD_VALUE:
            case E_VOTE_NOT_ACCEPTED:
                return Response.Status.NOT_ACCEPTABLE;
            case E_SESSION_DISCONNECTED:
            case E_NOT_LOGGED_IN:
                return Response.Status.UNAUTHORIZED;
            default:
                return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }

    public static Response surveyNotQuiteReady() {
        return Response.status(503, "Try again later")
                .entity(
                        "{\"error\": \"SurveyTool is not ready to handle API requests, try again later\"}")
                .build();
    }

    public static Response badLocale(String localeId) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new STError("Locale ID " + localeId + " not found"))
                .build();
    }

    public static Response badPath(String hexId) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new STError("XPath Hex ID " + hexId + " not found"))
                .build();
    }
}
