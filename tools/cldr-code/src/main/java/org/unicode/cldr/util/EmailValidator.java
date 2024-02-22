package org.unicode.cldr.util;

public class EmailValidator {

    /**
     * Is the given string at least approximately a valid email address?
     *
     * <p>This is not a thorough test, and is only meant to detect the crudest mistakes/bugs, such
     * in https://unicode-org.atlassian.net/browse/CLDR-13329
     *
     * <p>Note that the front end does its own more thorough validation with the help of the browser
     *
     * <p>Supposedly this regex could be used:
     * /^([!#-\'*+\/-9=?A-Z^-~\\\\-]{1,64}(\.[!#-\'*+\/-9=?A-Z^-~\\\\-]{1,64})*|"([\]!#-[^-~\
     * \t\@\\\\]|(\\[\t\
     * -~]))+")@([0-9A-Z]([0-9A-Z-]{0,61}[0-9A-Za-z])?(\.[0-9A-Z]([0-9A-Z-]{0,61}[0-9A-Za-z])?))+$/i
     *
     * @param email the string
     * @return true if we don't find a problem with it
     */
    public static boolean passes(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}
