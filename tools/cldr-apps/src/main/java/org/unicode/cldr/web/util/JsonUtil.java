package org.unicode.cldr.web.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

/** utilities for JSON */
public class JsonUtil {

    private static final Gson gson =
            new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .setPrettyPrinting()
                    .registerTypeHierarchyAdapter(
                            JSONString.class,
                            new TypeAdapter<JSONString>() {

                                @Override
                                public void write(JsonWriter out, JSONString value)
                                        throws IOException {
                                    out.jsonValue(value.toJSONString());
                                }

                                @Override
                                public JSONString read(JsonReader in) throws IOException {
                                    // write only
                                    // TODO Auto-generated method stub
                                    throw new UnsupportedOperationException(
                                            "Unimplemented method 'read' - write only");
                                }
                            })
                    .create();

    public static Gson gson() {
        return gson;
    }
}
