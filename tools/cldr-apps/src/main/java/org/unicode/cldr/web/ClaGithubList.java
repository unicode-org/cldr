package org.unicode.cldr.web;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfigImpl;
import org.unicode.cldr.web.util.JsonUtil;

/**
 * Utility class for reading a signatures.json file This file can be downloaded from
 * cla-assistant.io and placed in the cldr/ server directory. location can be overridden with the
 * CLA_FILE configuration preference. Currently, the file is re-read every time a query occurs, to
 * allow the file to be updated.
 *
 * <p>The simplest way to use this is as follows (given the id “github”): <code>
 * ClaGithubList.getInstance().getSignStatus("github")</code>
 */
public class ClaGithubList {
    /** Only 'signed' indicates a valid CLA. */
    public enum SignStatus {
        /** The signature was not found. Used by higher level APIs. */
        missing,
        /** A good CLA was found. */
        signed,
        /** The CLA was found, but had been revoked. */
        revoked,
    };

    /** One entry in the CLA file. */
    public static final class SignEntry {
        /** The GitHub user ID */
        public String user_name;

        /** Signing date, if any. */
        public String signed_at;

        /** Revocation date, if any. */
        public String revoked_at;

        /** User's real name. */
        public String name;

        /** Type of signature */
        public String category;

        /** User's employer */
        public String employer;

        /** User's email */
        public String email;

        // Note: There are additional fields in the JSON file which
        // are not currently read, and these are ignored.

        public SignStatus getSignStatus() {
            if (revoked_at != null && !revoked_at.isBlank()) {
                return SignStatus.revoked;
            } else if (signed_at != null && !signed_at.isBlank()) {
                return SignStatus.signed;
            } else {
                return SignStatus.missing; // Shouldn't happen, but could be a data error.
            }
        }

        @Override
        public String toString() {
            return String.format(
                    "Sign status: @%s <%s>: %s %s, revoked %s, emp %s, cat %s",
                    user_name,
                    name,
                    getSignStatus().name(),
                    signed_at,
                    revoked_at,
                    employer,
                    category);
        }

        /**
         * @return signed_at as a date, or null
         */
        public Date getSignedAt() {
            if (signed_at == null || signed_at.isEmpty()) return null;
            return Date.from(Instant.parse(signed_at));
        }
    }

    static final Logger logger = SurveyLog.forClass(ClaGithubList.class);

    /** get the singleton instance */
    public static final ClaGithubList getInstance() {
        return Helper.INSTANCE;
    }

    private static final class Helper {

        static final ClaGithubList INSTANCE = new ClaGithubList();
    }

    /** default file path, from configuration */
    private final String CLA_FILE;

    /** full path to .json file */
    private final Path claFilePath;

    ClaGithubList() {
        final CLDRConfig instance = CLDRConfig.getInstance();
        CLA_FILE = instance.getProperty("CLA_FILE", "./signatures.json");
        if (instance instanceof CLDRConfigImpl) {
            final File homeFile = ((CLDRConfigImpl) instance).getHomeFile();
            claFilePath = new File(homeFile, CLA_FILE).toPath();
            logger.fine("CLA_FILE=" + claFilePath.toString());
        } else {
            claFilePath = null;
            logger.fine("claFile path not set, could not get CLDRConfigImpl");
        }
    }

    /**
     * @returns the SignEntry for an id, or null if not found
     */
    public SignEntry getSignEntry(final String id) {
        Map<String, SignEntry> allSigners;
        try {
            allSigners = readSigners();
        } catch (IOException t) {
            logger.log(Level.SEVERE, "Trying to read signatures", t);
            return null;
        }
        // Get response
        final SignEntry requestedEntry = allSigners.get(id);
        return requestedEntry;
    }

    /**
     * This is the simplest and most recommended API for most use cases.
     *
     * @return the signing status of a GitHub ID, or missing.
     */
    public SignStatus getSignStatus(final String id) {
        final SignEntry e = getSignEntry(id);
        if (e == null) {
            return SignStatus.missing;
        } else {
            return e.getSignStatus();
        }
    }

    /** read the default .json file */
    Map<String, SignEntry> readSigners() throws IOException {
        if (claFilePath == null) {
            throw new NullPointerException(
                    "CLA_FILE=" + CLA_FILE + " but could not find file path.");
        }
        return readSigners(claFilePath);
    }

    /** read a specific path */
    Map<String, SignEntry> readSigners(final Path path) throws IOException {
        try (final Reader r = new FileReader(path.toFile(), StandardCharsets.UTF_8); ) {
            return readSigners(r);
        }
    }

    /** read from a Reader */
    Map<String, SignEntry> readSigners(final Reader r) throws IOException {
        final Gson gson = JsonUtil.gson();
        Map<String, SignEntry> allSigners = new HashMap<>();
        try (final JsonReader jr = gson.newJsonReader(r); ) {
            jr.beginArray();
            while (jr.hasNext()) {
                SignEntry e;
                try {
                    e = parseSignEntry(jr);
                    // the list is in REVERSE order (latest first). So we only track the first
                    // appearance.
                    if (!allSigners.containsKey(e.user_name)) {
                        allSigners.put(e.user_name, e);
                    }
                    // else: ignore, we only count the first (most recent entry).

                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "reading SignEntry - will skip", t);
                }
            }
            jr.endArray();
        }
        logger.info("Read " + allSigners.size() + " signatures");
        return allSigners;
    }

    private SignEntry parseSignEntry(final JsonReader jr) throws IOException {
        final SignEntry e = new SignEntry();
        jr.beginObject();
        while (jr.hasNext()) {
            switch (jr.nextName()) {
                case "user_name":
                    e.user_name = jr.nextString();
                    break;
                case "signed_at":
                    e.signed_at = jr.nextString();
                    break;
                case "revoked_at":
                    e.revoked_at = jr.nextString();
                    break;
                case "name":
                    e.name = jr.nextString();
                    break;
                case "category":
                    e.category = jr.nextString();
                    break;
                case "employer":
                    e.employer = jr.nextString();
                    break;
                case "email":
                    e.email = jr.nextString();
                    break;
                default:
                    jr.skipValue();
                    break; // ignore
            }
        }
        jr.endObject();
        return e;
    }
}
