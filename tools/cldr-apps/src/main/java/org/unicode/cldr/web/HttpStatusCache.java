package org.unicode.cldr.web;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class caches the status (200, 404, etc.) of a set of pages, attempting to avoid unnecessary
 * server traffic. At present the cache is a singleton.
 *
 * @author srl
 */
public class HttpStatusCache {
    /**
     * Main entrypoint. Check a URL for its status (i.e., is the page found or not?)
     *
     * @param url
     * @return
     */
    public static final boolean check(final String url) {
        try {
            return isGoodStatus(check(new URL(url)));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static final Integer check(final URL url) {
        try {
            Integer status = urlCache.get(url, () -> internalCheck(url));
            // For now we just pass the status through.
            // A future improvement would be to retry on some types of statuses.
            return status;
        } catch (ExecutionException e) {
            System.err.println("HttpStatusCache: Err in check of URL " + url.toString());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Flush the cache of a specific URL or all of them
     *
     * @param url ('all' if null)
     */
    public static final void flush(final URL url) {
        if (url == null) {
            System.err.println("HTTP Status Cache: invalidating all " + urlCache.size());
            urlCache.invalidateAll();
        } else {
            urlCache.invalidate(url);
        }
    }

    // ----------

    private static Cache<URL, Integer> urlCache =
            CacheBuilder.newBuilder()
                    .maximumSize(8192)
                    // We would want the expiry time greater if this were used by users, not just
                    // TC.
                    .expireAfterWrite(2, TimeUnit.HOURS)
                    .concurrencyLevel(3)
                    .build();

    /**
     * Returns the status code.
     *
     * @param status
     * @return true if "good" (200-ish)
     */
    public static final boolean isGoodStatus(Integer status) {
        if (status == null) return false;
        return (status >= 200
                && // found-ish
                status < 400); // redirect-ish
    }

    /**
     * Perform the actual check. Does not attempt to limit concurrency.
     *
     * @param url
     * @return HTTP status
     */
    static final Integer internalCheck(final URL url) {
        HttpURLConnection connection = null;
        try {
            // We don't need the content, so just do a HEAD check
            connection = (HttpURLConnection) (url.openConnection());
            connection.setRequestMethod("HEAD");
            connection.connect();
            final int code = connection.getResponseCode();
            return code;
        } catch (IOException t) {
            t.printStackTrace();
            return 499; // client closed request
        } finally {
            // Cleanup.
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
