package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * List of locale IDs which are somehow 'special'. Parses SpecialLocales.txt
 * 
 * @author srl
 * 
 */
public class SpecialLocales {
    public enum Type {
        readonly, scratch
    };

    /**
     * Get the type of this locale
     * 
     * @param l
     * @return a Type or null
     */
    public static Type getType(CLDRLocale l) {
        return getInstance().specials.get(l);
    }

    /**
     * Get all CLDRLocales matching this type
     * 
     * @param t
     * @return a set, or null if none found
     */
    public static Set<CLDRLocale> getByType(Type t) {
        return getInstance().types.get(t);
    }

    /**
     * Get the comment on this locale
     * 
     * @param l
     * @return string or null
     */
    public static String getComment(CLDRLocale l) {
        return getInstance().comments.get(l);
    }

    private static SpecialLocales singleton = null;

    private static synchronized SpecialLocales getInstance() {
        if (singleton == null) {
            singleton = new SpecialLocales();
        }
        return singleton;
    }

    private Map<CLDRLocale, Type> specials = new HashMap<CLDRLocale, Type>();
    private Map<Type, Set<CLDRLocale>> types = new HashMap<Type, Set<CLDRLocale>>();
    private Map<CLDRLocale, String> comments = new HashMap<CLDRLocale, String>();

    SpecialLocales() {
        // from StandardCodes.java
        String line;
        int ln = 0;
        try {
            BufferedReader lstreg = CldrUtility.getUTF8Data("SpecialLocales.txt");
            while (true) {
                line = lstreg.readLine();
                ln++;
                if (line == null)
                    break;
                int commentPos = line.indexOf('#');
                if (commentPos >= 0) {
                    line = line.substring(0, commentPos);
                }
                line = line.trim();
                if (line.length() == 0)
                    continue;
                List<String> stuff = CldrUtility.splitList(line, ';', true);
                String id = (String) stuff.get(0);
                String type = (String) stuff.get(1);
                String comment = (String) stuff.get(2);
                Type t = null;

                // verify that the locale is valid
                CLDRLocale l = null;
                try {
                    l = CLDRLocale.getInstance(id);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid CLDRLocale in SpecialLocales.txt:" + ln + ": " + line);
                }

                // verify that the type is valid
                try {
                    t = Type.valueOf(type.toLowerCase(Locale.ENGLISH));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid SpecialLocales.Type in SpecialLocales.txt:" + ln + ": "
                        + line);
                }

                Set<CLDRLocale> s = types.get(t);
                if (s == null) {
                    s = new TreeSet<CLDRLocale>();
                    types.put(t, s);
                }
                s.add(l);
                specials.put(l, t);
                if (!comment.isEmpty()) {
                    comments.put(l, comment);
                }
            }
        } catch (IOException e) {
            throw (IllegalArgumentException) new IllegalArgumentException("Internal Error").initCause(e);
        }
    }

}
