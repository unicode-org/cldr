package org.unicode.cldr.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Hidden notifications")
public class HiddenNotifications {

    private final HashMap<String, List<PathValuePair>> map;

    public HiddenNotifications() {
        map = new HashMap<>();
    }

    public boolean needsData() {
        return map.isEmpty();
    }

    /*
     * For serialization, convert lists to arrays
     */
    public HashMap<String, PathValuePair[]> getHidden() {
        HashMap<String, PathValuePair[]> m = new HashMap<>();
        map.forEach((subtype, pairList) -> m.put(subtype, pairList.toArray(new PathValuePair[0])));
        return m;
    }

    public void put(String subtype, String xpstrid, String value) {
        List<PathValuePair> pvList = map.get(subtype);
        if (pvList == null) {
            pvList = new ArrayList<>();
        }
        pvList.add(new PathValuePair(xpstrid, value));
        map.put(subtype, pvList);
    }
}
