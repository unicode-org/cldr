package org.unicode.cldr.web.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class AddUserRequest {
    @Schema(description = "New user name")
    public String name;

    @Schema(description = "New user e-mail")
    public String email;

    @Schema(description = "New user organization")
    public String org;

    @Schema(description = "New user level")
    public Integer level;

    @Schema(description = "New user locales")
    public String locales;
}
