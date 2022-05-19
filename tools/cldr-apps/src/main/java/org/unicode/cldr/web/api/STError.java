package org.unicode.cldr.web.api;

import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.web.SurveyException;
import org.unicode.cldr.web.SurveyException.ErrorCode;

@Schema(name="STError", description="Error return object")
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
    /**
     * Description of the error
     */
    @Schema(description = "Error message")
    public String message;
    /**
     * identifies this as an error
     */
    @Schema(description = "Always set to true, identifies this as an error.")
    public final boolean err = true;
    /**
     * Optional error code
     */
    @Schema(description = "Error code if present")
    public ErrorCode code;
    /**
     * Convenience function:  return STError("something").build() => 500
     * @return
     */
    public Response build() {
        return Response.serverError().entity(this).build();
    }

    public static Response surveyNotQuiteReady() {
        return Response.status(503, "Try again later")
            .entity("{\"error\": \"SurveyTool is not ready to handle API requests, try again later\"}")
            .build();
    }
}
