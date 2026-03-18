package org.unicode.cldr.web.util;

import com.google.gson.Gson;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/* shim */
public class JSONObject implements JSONString {
    private static final Gson gson = JsonUtil.gson();
    private final Map<String, Object> m = new TreeMap<>();

    @Override
    public String toJSONString() throws JSONException {
        return gson.toJson(m);
    }

    @Override
    public String toString() {
        return toJSONString();
    }

    public JSONObject put(String k, Object v) {
        m.put(k, v);
        return this;
    }

    public Object get(String string) {
        return m.get(string);
    }

    public JSONArray getJSONArray(String string) {
        return (JSONArray) (get(string));
    }

    public String getString(String k) {
        return get(k).toString();
    }

    public int length() {
        return m.size();
    }

    public boolean has(String ov) {
        return m.containsKey(ov);
    }

    public Iterator<String> keys() {
        return m.keySet().iterator();
    }
}
