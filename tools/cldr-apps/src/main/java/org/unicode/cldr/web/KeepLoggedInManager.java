package org.unicode.cldr.web;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import org.apache.commons.io.FileUtils;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfigImpl;

/** Manages the JWT used for staying logged in */
public class KeepLoggedInManager {
    private static Logger logger = SurveyLog.forClass(KeepLoggedInManager.class);

    /** Algorithm to use */
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    /** Keyfile, just base64 encoded binary data */
    private static final String KEYFILE_NAME = KeepLoggedInManager.class.getName() + ".key";

    private final File keyFile;
    private SecretKey key;

    public static File getDefaultParent() {
        final CLDRConfig config = CLDRConfig.getInstance();
        if (config instanceof CLDRConfigImpl) {
            File homeFile = CLDRConfigImpl.getInstance().getHomeFile();
            if (homeFile == null) {
                throw new IllegalArgumentException(
                        "CLDRConfigImpl present but getHomeFile() returned null.");
            } else {
                return homeFile;
            }
        } else {
            return null;
        }
    }

    /**
     * Construct a KLM from the specified dir
     *
     * @param parentDir dir or null or the CLDRConfig default
     */
    public KeepLoggedInManager(File parentDir) throws IOException {
        if (parentDir == null) {
            parentDir = getDefaultParent();
        }
        if (parentDir == null) {
            throw new NullPointerException("Can't find the parent dir for storing stuff.");
        }

        if (!parentDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "parent dir " + parentDir.getAbsolutePath() + " not a dir");
        }

        keyFile = new File(parentDir, KEYFILE_NAME);
        logger.info("Setup with keyfile " + keyFile.getAbsolutePath());

        if (keyFile.exists()) {
            key = readKey(keyFile);
        } else {
            resetKey();
        }

        logger.info("Key read, fingerprint " + key.getAlgorithm() + "/" + key.getFormat());
    }

    synchronized void resetKey() throws IOException {
        if (keyFile.exists()) {
            keyFile.delete();
        }
        key = newSecretKey();
        writeKey(keyFile, key);
    }

    private void writeKey(File f, SecretKey k) throws IOException {
        FileUtils.write(f, Encoders.BASE64.encode(k.getEncoded()), StandardCharsets.US_ASCII);
        f.setReadable(false, false); // nobody can read it...
        f.setReadable(true, true); // ...but the owner
    }

    private SecretKey readKey(File f) throws IOException {
        final String s = FileUtils.readFileToString(f, StandardCharsets.US_ASCII);
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(s));
    }

    /** Request that a new key be installed. */
    private SecretKey newSecretKey() {
        return Keys.secretKeyFor(SIGNATURE_ALGORITHM);
    }

    /**
     * Testing only
     *
     * @return
     */
    SecretKey getKey() {
        return key;
    }

    /**
     * Create a JSON Web Token for the 'subject' (user ID #) specified.
     *
     * @param subject a user id, in this case "1234"
     * @return JWT, see <https://jwt.io> for decoder
     */
    public String createJwtForSubject(String subject) {
        final long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .signWith(key)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + (SurveyMain.TWELVE_WEEKS * 1000L)))
                .compact();
    }

    /**
     * Testing only. Generates a JWT that already expired.
     *
     * @param subject
     * @return
     */
    String createExpiredJwtForSubject(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .signWith(key)
                .setIssuedAt(new Date())
                .setExpiration(new Date(0))
                .compact();
    }

    /**
     * Parse a JWT <https://jwt.io> string and return the subject. This is a simplified API, and
     * returns null if ANY error occurred, such as malformed or expired jwt.
     *
     * @param jwt the jwt string
     * @return the subject, such as "1234" or null if some error occurred.
     */
    public String getSubject(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return null;
        }
        Jws<Claims> c = getClaims(jwt);
        if (c == null) {
            // failed
            return null;
        }
        return c.getBody().getSubject();
    }

    /**
     * Advanced API, returns the underlying claims from the JWT.
     *
     * @param jwt the jwt
     * @return the claims object
     */
    public Jws<Claims> getClaims(String jwt) {
        try {
            Jws<Claims> claims =
                    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt);
            return claims;
        } catch (JwtException e) {
            logger.log(Level.FINE, "Error parsing JWT", e);
            return null;
        }
    }
}
