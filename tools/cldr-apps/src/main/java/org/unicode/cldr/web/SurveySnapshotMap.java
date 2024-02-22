package org.unicode.cldr.web;

import java.util.TreeMap;

public class SurveySnapshotMap implements SurveySnapshot {

    private final TreeMap<String, String> snapMap = new TreeMap<>();

    @Override
    public void put(String snapshotId, String json) {
        snapMap.put(snapshotId, json);
    }

    @Override
    public String get(String snapshotId) {
        return snapMap.get(snapshotId);
    }

    @Override
    public String[] list() {
        String[] keys = new String[snapMap.size()];
        return snapMap.keySet().toArray(keys);
    }
}
