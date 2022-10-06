package org.unicode.cldr.web.api;

import java.time.Instant;

/**
 * This class exists because UserRegistry.User is unserializable
 * at present.
 */
public class UserItem implements Comparable<UserItem> {
    public String level;
    public String name;
    public String org;
    public int id;
    public String email;
    public String assignedLocales[];
    public Instant lastLogin;

    /**
     * Order by org, name, id
     */
    @Override
    public int compareTo(UserItem o) {
        int rc = org.compareTo(o.org);
        if (rc == 0) {
            rc = name.compareTo(o.name);
        }
        if (rc == 0) {
            rc = Integer.compare(id, o.id);
        }
        return rc;
    }
}
