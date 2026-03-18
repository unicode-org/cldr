package org.unicode.cldr.web.util;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Generic REST API client. */
public class STRestClient {

    public <T> T get(final URL url, Class<T> classOfT) throws IOException {
        return request(url, classOfT, null);
    }

    public <T> T post(final URL url, Class<T> classOfT, Object payload) throws IOException {
        if (payload == null) {
            throw new NullPointerException("payload cannot be null, use get()");
        }
        return request(url, classOfT, payload);
    }

    private <T> T request(final URL url, Class<T> classOfT, Object payload) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) (url.openConnection());
            if (payload == null) {
                connection.setRequestMethod("GET");
            } else {
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoInput(true);
            }
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", "application/json");

            // set other headers
            if (bearer != null) {
                connection.setRequestProperty("Authorization", "Bearer " + bearer);
            }

            connection.connect();

            if (payload != null) {
                // POST
                // build up the request payload
                final String body = gson.toJson(payload);
                final OutputStream out = connection.getOutputStream();
                out.write(body.getBytes(StandardCharsets.UTF_8));
                out.close();
            }

            // get the response
            final InputStream in = connection.getInputStream();
            final T resp =
                    gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), classOfT);
            return resp;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static final Gson gson = JsonUtil.gson();

    String bearer = null;

    public void setAuthBearer(String bearer) {
        this.bearer = bearer;
    }
}
