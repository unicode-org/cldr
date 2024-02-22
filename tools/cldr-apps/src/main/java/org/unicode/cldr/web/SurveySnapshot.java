package org.unicode.cldr.web;

import java.time.Instant;

public interface SurveySnapshot {
    String SNAP_NONE = "NONE";
    String SNAP_CREATE = "CREATE";
    String SNAP_SHOW = "SHOW";

    String SNAPID_NOT_APPLICABLE = "NA";

    static String newId() {
        return Instant.now().toString(); // a timestamp like 2022-01-18T12:34:56.789Z
    }

    void put(String json, String snapshotId);

    String get(String snapshotId);

    String[] list();
}
