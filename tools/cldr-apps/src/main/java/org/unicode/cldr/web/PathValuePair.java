package org.unicode.cldr.web;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Path, Value Pair")
public class PathValuePair {
    @Schema(description = "Path")
    public String xpath;

    @Schema(description = "Value")
    public String value;

    public PathValuePair(String xpath, String value) {
        this.xpath = xpath;
        this.value = value;
    }
}
