package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class XPathRequest {

    @Schema(required = true, description = "XPath string") public String str;

    public XPathRequest() {

    }
}
