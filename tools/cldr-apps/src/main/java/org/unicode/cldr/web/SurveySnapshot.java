package org.unicode.cldr.web;

import java.time.Instant;

public interface SurveySnapshot {
    final public static String SNAP_NONE = "NONE";
    final public static String SNAP_CREATE = "CREATE";
    final public static String SNAP_SHOW = "SHOW";

    final public static String SNAPID_NOT_APPLICABLE = "NA";

    public static String newId() {
        return Instant.now().toString(); // a timestamp like 2022-01-18T12:34:56.789Z
    }

    public void put(String json, String snapshotId);

    public String get(String snapshotId);

    public String[] list();
}
