package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestOrganization {
    @Test
    void testStOrgs() {
        // Static test, for now
        assertEquals(3, Organization.getTCOrgs().size());

        assertTrue(Organization.getTCOrgs().contains(Organization.apple));
        assertTrue(Organization.getTCOrgs().contains(Organization.google));
        assertTrue(Organization.getTCOrgs().contains(Organization.microsoft));

        assertTrue(Organization.apple.isTCOrg());
        assertTrue(Organization.google.isTCOrg());
        assertTrue(Organization.microsoft.isTCOrg());

        assertFalse(Organization.surveytool.isTCOrg());
        assertFalse(Organization.adlam.isTCOrg());
    }
}
