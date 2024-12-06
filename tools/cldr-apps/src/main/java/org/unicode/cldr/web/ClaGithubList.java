package org.unicode.cldr.web;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileReader;
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

public class ClaGithubList {
    public enum SignStatus {
        missing,
        signed,
        revoked,
    };

    public static final class SignEntry {
        public String user_name;
        public String signed_at;
        public String revoked_at;
        public String name;
        public String category;
        public String employer;
        public String email;

        // etc - more fields we don't need.

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

    public static final ClaGithubList getInstance() {
        return Helper.INSTANCE;
    }

    private static final class Helper {

        static final ClaGithubList INSTANCE = new ClaGithubList();
    }

    static final Gson gson = new Gson();

    private final CLDRConfig instance = CLDRConfig.getInstance();

    private final String CLA_FILE = instance.getProperty("CLA_FILE", "./signatures.json");
    private final Path claFilePath;

    ClaGithubList() {
        final File homeFile = ((CLDRConfigImpl) instance).getHomeFile();
        claFilePath = new File(homeFile, CLA_FILE).toPath();
        logger.info("CLA_FILE=" + claFilePath.toString());
    }

    public SignEntry getSignEntry(final String id) {
        // Impl:  reread each time
        Map<String, SignEntry> allSigners = new HashMap<>();
        try (final Reader r = new FileReader(claFilePath.toFile(), StandardCharsets.UTF_8);
                final JsonReader jr = gson.newJsonReader(r); ) {
            jr.beginArray();
            while (jr.hasNext()) {
                SignEntry e = new SignEntry();
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

                // the list is in REVERSE order (latest first). So we only track the first
                // appearance.
                if (!allSigners.containsKey(e.user_name)) {
                    allSigners.put(e.user_name, e);
                    // TODO: we could break here.

                } // else: ignore, only count first entry
            }
            jr.endArray();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Trying to read signatures for " + id, t);
        }
        logger.info("Read " + allSigners.size() + " signatures");
        // Get response
        final SignEntry requestedEntry = allSigners.get(id);
        return requestedEntry;
    }

    public SignStatus getSignStatus(final String id) {
        final SignEntry e = getSignEntry(id);
        if (e == null) {
            return SignStatus.missing;
        } else {
            return e.getSignStatus();
        }
    }
}
