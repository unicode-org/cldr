package org.unicode.cldr.web.util;

import java.util.LinkedList;
import java.util.List;

public class JSONArray implements JSONString {

    private final List<Object> l = new LinkedList<>();

    public JSONArray(Object[] value) {
        for (final Object v : value) {
            l.add(v);
        }
    }

    public JSONArray() {}

    public JSONArray put(Object o) {
        l.add(o);
        return this;
    }

    public int length() {
        return l.size();
    }

    public JSONArray getJSONArray(int i) {
        return (JSONArray) (l.get(i));
    }

    public int getInt(int i) {
        return (Integer) (l.get(i));
    }

    @Override
    public String toString() {
        return toJSONString();
    }

    @Override
    public String toJSONString() throws JSONException {
        return JsonUtil.gson().toJson(l);
    }
}
