package org.unicode.cldr.web.auth;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfigImpl;
import org.unicode.cldr.web.SurveyLog;
import org.unicode.cldr.web.util.STRestClient;

public class GithubLoginFactory extends OAuthLoginFactory {

    static final Logger logger = SurveyLog.forClass(GithubLoginFactory.class);

    public final class GithubSession extends LoginSession {

        private final GithubUserResponse resp;

        public GithubSession(GithubUserResponse resp) {
            super(resp.login);
            this.resp = resp; // in case we need to deserialize any other fields
        }

        @Override
        public String getDisplayName() {
            return resp.name;
        }
    }

    public static final GithubLoginFactory getInstance() {
        return Helper.INSTANCE;
    }

    private static final class Helper {

        static final GithubLoginFactory INSTANCE = new GithubLoginFactory();
    }

    /**
     * file in CLDR_HOME dir containing App private key in DER format Note, GitHub provides it in
     * .pem format So, run: openssl pkcs8 -topk8 -inform PEM -outform DER -in github-app-private.pem
     * -out github-app-private.der -nocrypt openssl rsa -in github-app-private.pem -pubout -outform
     * DER -out github-app-public.der
     */
    public static final String PRIVATE_FILE_NAME = "github-app-private.der";

    /** public key file, see above */
    public static final String PUBLIC_FILE_NAME = "github-app-public.der";

    /** cldr.properties variable: App ID */
    public final String GITHUB_APP_ID;

    /** cldr.properties variable: Client ID */
    public final String GITHUB_CLIENT_ID;

    /** cldr.properties variable: Client Secret */
    public final String GITHUB_CLIENT_SECRET;

    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;

    /** construct a new login factory */
    public GithubLoginFactory() {
        CLDRConfig instance = CLDRConfig.getInstance();

        GITHUB_APP_ID = instance.getProperty("GITHUB_APP_ID");
        GITHUB_CLIENT_ID = instance.getProperty("GITHUB_CLIENT_ID");
        GITHUB_CLIENT_SECRET = instance.getProperty("GITHUB_CLIENT_SECRET");
        final File homeFile = ((CLDRConfigImpl) instance).getHomeFile();
        final Path derPrivateFile = new File(homeFile, PRIVATE_FILE_NAME).toPath();
        final Path derPublicFile = new File(homeFile, PUBLIC_FILE_NAME).toPath();
        byte[] derPublic = null;
        byte[] derPrivate = null;
        try {
            derPrivate = Files.readAllBytes(derPrivateFile);
        } catch (java.io.IOException e) {
            logger.log(
                    java.util.logging.Level.SEVERE,
                    "Could not load " + derPrivateFile + ": " + e.getMessage(),
                    e);
        }
        try {
            derPublic = Files.readAllBytes(derPublicFile);
        } catch (java.io.IOException e) {
            logger.log(
                    java.util.logging.Level.SEVERE,
                    "Could not load " + derPublicFile + ": " + e.getMessage(),
                    e);
        }

        if (derPublic != null && derPrivate != null) {
            try {
                final KeyFactory rsaFactory = KeyFactory.getInstance("RSA");
                publicKey = rsaFactory.generatePublic(new X509EncodedKeySpec(derPublic));
                privateKey = rsaFactory.generatePrivate(new PKCS8EncodedKeySpec(derPrivate));
            } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
                logger.log(
                        java.util.logging.Level.SEVERE,
                        "Could not read private/public keys: " + ex.getMessage(),
                        ex);
            }
        }
    }

    @Override
    public boolean valid() {
        if (GITHUB_APP_ID == null
                || GITHUB_APP_ID.isBlank()
                || GITHUB_CLIENT_ID == null
                || GITHUB_CLIENT_ID.isBlank()
                || GITHUB_CLIENT_SECRET == null
                || GITHUB_CLIENT_SECRET.isBlank()
                || privateKey == null
                || publicKey == null) {
            return false;
        }

        return true;
    }

    @Override
    public String getLoginUrl(LoginIntent intent) {
        if (intent != LoginIntent.cla) {
            throw new IllegalArgumentException("LoginIntent must be cla but got " + intent);
        }
        // Note: does not set redirect URI, that can be done on the front end.
        // TODO CLDR-18165: add 'state=…' to a temporary token.
        return String.format(
                "https://github.com/login/oauth/authorize?client_id=%s", GITHUB_CLIENT_ID);
    }

    public static final class GithubAccessTokenResponse {
        public String access_token;
        public String refresh_token;
        public String refresh_token_expires_in;
        public String token_type;
    }

    public class GithubAccessTokenRequest {
        public String client_id = GITHUB_CLIENT_ID;
        public String client_secret = GITHUB_CLIENT_SECRET;
        public final String code;

        public GithubAccessTokenRequest(String code) {
            this.code = code;
        }
    }

    public static final class GithubUserResponse {
        public String login;
        public String name;
        // others…
    }

    /** mint a LoginSession */
    public LoginSession forCode(String code) {
        try {
            final URL accessTokenUrl = new URL("https://github.com/login/oauth/access_token");
            logger.finest("=> req code " + code);
            final GithubAccessTokenRequest body = new GithubAccessTokenRequest(code);
            STRestClient client = new STRestClient();
            final GithubAccessTokenResponse resp =
                    client.post(accessTokenUrl, GithubAccessTokenResponse.class, body);
            final String access_token = resp.access_token;

            logger.finest("=> req user w/ token " + access_token);
            final URL userRequest = new URL("https://api.github.com/user");
            client.setAuthBearer(access_token);
            final GithubUserResponse user = client.get(userRequest, GithubUserResponse.class);

            logger.fine("Got user: @" + user.login + " " + user.name);
            final LoginSession ls = new GithubSession(user);
            logger.fine("Got login session: " + ls.getKey());
            return ls;
        } catch (Throwable t) {
            logger.log(java.util.logging.Level.SEVERE, "Problem processing GitHub code", t);
            return null;
        }
    }
}
