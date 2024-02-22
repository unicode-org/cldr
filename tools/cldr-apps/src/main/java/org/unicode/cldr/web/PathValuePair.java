package org.unicode.cldr.web;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Path, Value Pair")
public class PathValuePair {
    @Schema(description = "Path")
    public String xpstrid;

    @Schema(description = "Value")
    public String value;

    public PathValuePair(String xpstrid, String value) {
        this.xpstrid = xpstrid;
        this.value = value;
    }
}
