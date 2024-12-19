package org.unicode.cldr.web.auth;

import java.util.logging.Logger;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.SurveyLog;

public class LoginManager {

    static final Logger logger = SurveyLog.forClass(LoginManager.class);

    public static final LoginManager getInstance() {
        return Helper.INSTANCE;
    }

    private static final class Helper {

        static final LoginManager INSTANCE = new LoginManager();
    }

    // Simple implementation for now.
    public LoginManager() {
        try {
            github = GithubLoginFactory.getInstance();
            if (!github.valid()) {
                logger.info("Github login not valid");
                github = null;
            }
            logger.info("Github login available");
        } catch (Throwable t) {
            SurveyLog.logException(t, "trying to setup GitHub");
            github = null;
        }
    }

    GithubLoginFactory github = null;

    public String getLoginString(LoginFactory.LoginIntent intent) {
        if (github == null) return null;
        return github.getLoginUrl(intent);
    }

    /** Get the login session (if any) in this session */
    public LoginSession getLoginSession(CookieSession cs) {
        if (cs == null) return null;
        return (LoginSession) cs.get(SESSION_KEY);
    }

    /**
     * Update the Login Session
     *
     * @returns the previous login session, if any
     */
    public LoginSession setLoginSession(CookieSession cs, LoginSession l) {
        final LoginSession old = getLoginSession(cs);
        cs.put(SESSION_KEY, l);
        return old;
    }

    /**
     * Remove the login session if any
     *
     * @returns the previous login session, if any
     */
    public LoginSession clearLoginSession(CookieSession cs) {
        return (LoginSession) cs.clear(SESSION_KEY);
    }

    // Object identifier in the CookieSession hash
    private static final String SESSION_KEY = LoginManager.class.getSimpleName();
}
