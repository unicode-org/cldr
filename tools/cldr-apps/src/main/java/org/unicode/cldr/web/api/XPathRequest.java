package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class XPathRequest {

    @Schema(
            required = true,
            description = "XPath string",
            example = "//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator")
    public String str;

    public XPathRequest() {}
}
