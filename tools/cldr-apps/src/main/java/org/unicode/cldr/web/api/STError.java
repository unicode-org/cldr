package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name="STError", description="Error return object")
public class STError {
    public STError(String desc) {
        this.message = desc;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
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
    public String code;
}
