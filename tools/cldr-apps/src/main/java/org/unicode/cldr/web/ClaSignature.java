package org.unicode.cldr.web;

import java.util.Date;
import java.util.EnumSet;
import java.util.stream.Collectors;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.util.CLDRConfig;
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
    public boolean noRights; // employer claims no rights
    public String github; // set if a Github id

    @Schema(required = false, description = "Version of CLDR signed in, or * for n/a")
    public String version; // which CLDR version was it signed in?

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
    public static final EnumSet<Organization> CLA_ORGS = getClaOrgs();

    private static final EnumSet<Organization> getClaOrgs() {
        EnumSet<Organization> orgs =
                EnumSet.of(
                        Organization.adobe,
                        Organization.airbnb,
                        Organization.apple,
                        Organization.cherokee,
                        Organization.google,
                        Organization.ibm,
                        Organization.mayan_lpp,
                        Organization.meta,
                        Organization.microsoft,
                        Organization.motorola,
                        Organization.mozilla,
                        Organization.netflix,
                        Organization.sil,
                        Organization.wikimedia,
                        Organization.surveytool);
        final String additionalOrgs = CLDRConfig.getInstance().getProperty("ADD_CLA_ORGS", "");
        if (additionalOrgs != null && !additionalOrgs.isEmpty()) {
            for (final String o : additionalOrgs.trim().split(" ")) {
                final Organization org = Organization.fromString(o);
                if (org != null) {
                    orgs.add(org);
                } else {
                    System.err.println("Bad organization in ADD_CLA_ORGS: " + o);
                }
            }
        }
        // If this is too noisy, we can move it elsewhere
        System.out.println(
                "ADD_CLA_ORGS="
                        + orgs.stream()
                                .map((Organization o) -> o.name())
                                .collect(Collectors.joining(" ")));

        return orgs;
    }

    public ClaSignature() {}

    public ClaSignature(Organization o) {
        this.email = "";
        this.name = "Corporate CLA - " + o.name();
        this.employer = o.toString();
        this.corporate = true;
        this.signed = new Date(0);
        this.readonly = true;
        this.version = "*";
        this.github = null;
    }

    public ClaSignature(String string) {
        this.email = "";
        this.name = "Testing CLA: " + string;
        this.employer = "Testing CLA";
        this.corporate = true;
        this.signed = new Date(0);
        this.readonly = true;
        this.version = "*";
    }
}
