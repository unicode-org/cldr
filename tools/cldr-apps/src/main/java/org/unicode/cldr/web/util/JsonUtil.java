package org.unicode.cldr.web.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.unicode.cldr.util.CLDRLocale;

/** utilities for JSON */
public class JsonUtil {

    private abstract static class WriteOnlyTypeAdapter<T> extends TypeAdapter<T> {
        @Override
        public T read(JsonReader in) throws IOException {
            // write only
            throw new UnsupportedOperationException("Unimplemented method 'read' - write only");
        }
    }

    private static final class JSONStringTypeAdapter extends WriteOnlyTypeAdapter<JSONString> {
        @Override
        public void write(JsonWriter out, JSONString value) throws IOException {
            out.jsonValue(value.toJSONString());
        }
    }

    /** type adapter that uses Object.toString() rather than serializing object contents */
    private static final class ToStringTypeAdapter<T> extends WriteOnlyTypeAdapter<T> {
        @Override
        public void write(JsonWriter out, T value) throws IOException {
            out.value(value.toString());
        }
    }

    private static final Gson gson =
            new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .setPrettyPrinting()
                    // register any classes that need toString() for serialization
                    .registerTypeHierarchyAdapter(CLDRLocale.class, new ToStringTypeAdapter<>())
                    .registerTypeHierarchyAdapter(JSONString.class, new JSONStringTypeAdapter())
                    .create();

    public static Gson gson() {
        return gson;
    }
}
