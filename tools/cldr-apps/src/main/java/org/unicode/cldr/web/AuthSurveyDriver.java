package org.unicode.cldr.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.web.UserRegistry.User;

public class AuthSurveyDriver {

    /**
     * Fictitious domain identifying fictitious email addresses for simulated users like
     * "driver-123@cldr-apps-webdriver.org"
     */
    public static final String EMAIL_AT_DOMAIN = "@cldr-apps-webdriver.org";

    private static final String EMAIL_PREFIX = "driver-";

    private static final int INVALID_USER_INDEX = -1;
    private static final int FIRST_VALID_USER_INDEX = 0;

    public static final java.util.logging.Logger logger =
            SurveyLog.forClass(AuthSurveyDriver.class);

    private static boolean isInitialized = false;
    private static boolean isEnabled = false;
    private static String realPassword = null;

    /**
     * If webdriver users are enabled, and the given password and email are correct for such users,
     * create a new user.
     *
     * @param password the password
     * @param email the (pseudo) email address
     * @return the new user, or null
     */
    public static User createTestUser(String password, String email) {
        if (!isInitialized) {
            initialize();
        }
        if (isEnabled && realPassword.equals(password) && email.contains(EMAIL_AT_DOMAIN)) {
            int userIndex = getUserIndexFromEmail(email);
            if (userIndex >= FIRST_VALID_USER_INDEX) {
                return addTestUser(password, email, userIndex);
            }
        }
        return null;
    }

    private static void initialize() {
        String status;
        if (SurveyMain.isUnofficial()) {
            realPassword = CLDRConfig.getInstance().getProperty("CLDR_WEBDRIVER_PASSWORD", "");
            if (realPassword != null && !realPassword.isBlank()) {
                isEnabled = true;
                status = "enabled";
            } else {
                status = "disabled, no property";
            }
        } else {
            status = "disabled, official";
        }
        logger.info("Webdriver simulated users are " + status);
        isInitialized = true;
    }

    /**
     * Get a number like 123 given an email address like "driver-123@cldr-apps-webdriver.org"
     *
     * @param email the address
     * @return the nonnegative number if the address matches the pattern, else INVALID_USER_INDEX
     */
    private static int getUserIndexFromEmail(String email) {
        Matcher m = Pattern.compile("\\d+").matcher(email);
        if (m.find()) {
            int userIndex = Integer.parseInt(m.group());
            String properEmail = EMAIL_PREFIX + userIndex + EMAIL_AT_DOMAIN;
            if (properEmail.equals(email)) {
                return userIndex;
            }
        }
        return INVALID_USER_INDEX;
    }

    private static User addTestUser(String password, String email, int userIndex) {
        UserRegistry reg = CookieSession.sm.reg;
        User u = reg.new User(UserRegistry.NO_USER);
        u.email = email;
        // Make user level TC, since even with ALL_LOCALES (*), a VETTER might
        // not have permission to vote in all locales, depending on the organization.
        u.userlevel = UserRegistry.TC;
        u.locales = StandardCodes.ALL_LOCALES;
        u.name = EMAIL_PREFIX + userIndex;
        Organization[] orgArray = Organization.values();
        u.org = orgArray[userIndex % orgArray.length].name();
        u.setPassword(password);
        User registeredUser = reg.newUser(null, u);
        if (registeredUser == null || registeredUser.id <= 0) {
            logger.severe("Failed to add webdriver simulated user " + email);
            return null;
        }
        logger.info("Added webdriver simulated user " + email);
        return registeredUser;
    }
}
