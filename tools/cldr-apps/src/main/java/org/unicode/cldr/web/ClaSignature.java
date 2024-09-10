package org.unicode.cldr.web;

import java.util.Date;
import java.util.EnumSet;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.util.Organization;

public final class ClaSignature {
    /**
     * If TRUE, then CLAs are NOT required by default unless the REQUIRE_CLA property is set. At
     * present (CLDR-16499) this facility is not activated by default.
     */
    public static final boolean DO_NOT_REQURE_CLA = true;

    public static final String CLA_KEY = "SignedCla";
    public String email;
    public String name;
    public String employer; // May be different than org!
    public boolean corporate; // signed as corporate

    @Schema(required = false)
    public Date signed;

    @Schema(description = "CLA is fixed by organization and cannot be changed", required = false)
    public boolean readonly;

    public boolean valid() {
        if (email.isBlank()) return false;
        if (name.isBlank()) return false;
        if (employer.isBlank()) return false;
        if (corporate && employer.equals("none")) return false;
        return true;
    }

    /**
     * Organizations which are known to have signed the CLA. Update this from
     * https://www.unicode.org/policies/corporate-cla-list/
     */
    public static final EnumSet<Organization> CLA_ORGS =
            EnumSet.of(
                    Organization.adobe,
                    Organization.apple,
                    Organization.cherokee,
                    Organization.google,
                    Organization.ibm,
                    Organization.meta,
                    Organization.microsoft,
                    Organization.mozilla,
                    Organization.sil,
                    Organization.wikimedia,
                    Organization.surveytool);

    public ClaSignature() {}

    public ClaSignature(Organization o) {
        this.email = "";
        this.name = "Corporate CLA - " + o.name();
        this.employer = o.toString();
        this.corporate = true;
        this.signed = new Date(0);
        this.readonly = true;
    }

    public ClaSignature(String string) {
        this.email = "";
        this.name = "Testing CLA: " + string;
        this.employer = "Testing CLA";
        this.corporate = true;
        this.signed = new Date(0);
        this.readonly = true;
    }
}
