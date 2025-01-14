package org.unicode.cldr.web.util;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/** Generic REST API client. */
public class STRestClient {

    @FunctionalInterface
    public static interface CheckedIOFunction<T, R> {
       R apply(T t) throws IOException;
    }

    public <T> T get(final URL url, Class<T> classOfT) throws IOException {
        return request(url, classOfT, null);
    }

    public <T> T post(final URL url, Class<T> classOfT, Object payload) throws IOException {
        if (payload == null) {
            throw new NullPointerException("payload cannot be null, use get()");
        }
        return request(url, classOfT, payload);
    }

    public Long postResultToFile(final URL url, File resultFile, Object payload) throws IOException {
        if (payload == null) {
            throw new NullPointerException("payload cannot be null, use get()");
        }
        return request(url, (InputStream in) -> {
            try (final FileOutputStream out = new FileOutputStream(resultFile)) {
                return in.transferTo(out);
            }
        }, payload);
    }

    /** request, consuming an InputStream  */
    private <T> T request(final URL url, CheckedIOFunction<InputStream, T> processor, Object payload) throws IOException {
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

            if (xtoken != null) {
                connection.setRequestProperty("x-token", xtoken);
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
            return processor.apply(in);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** request yielding a JSON-deserialized class */
    private <T> T request(final URL url, Class<T> classOfT, Object payload) throws IOException {
        return request(url, (InputStream in) -> gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), classOfT), payload);
    }

    static final Gson gson = new Gson();

    String bearer = null;

    String xtoken = null;

    public void setAuthBearer(String bearer) {
        this.bearer = bearer;
    }

    public void setXToken(String xtoken) {
        this.xtoken = xtoken;
    }
}
