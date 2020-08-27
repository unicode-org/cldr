package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.CLDRFileTransformer;
import org.unicode.cldr.tool.CLDRFileTransformer.LocaleTransform;

import com.ibm.icu.util.ICUUncheckedIOException;

/**
 * List of locale IDs which are somehow 'special'. Parses SpecialLocales.txt
 *
 * @author srl
 *
 */
public class SpecialLocales {
    private static final String INCLUDE_SUBLOCALES = "*";

    public enum Type {
        /**
         * Locale may not be modified by user.
         */
        readonly,
        /**
         * Locale may be modified by user. Contents aren't part of CLDR release and may change.
         */
        scratch
    }

    /**
     * Get the type of this locale
     *
     * @param l
     * @return a Type or null
     */
    public static Type getType(CLDRLocale l) {
        return getInstance().getTypeInternal(l);
    }

    /**
     * Get all CLDRLocales matching this type. Does not include wildcard (*) sublocales.
     *
     * @param t
     * @return a set, or null if none found
     */
    public static Set<CLDRLocale> getByType(Type t) {
        return getInstance().getByTypeInternal(t);
    }

    /**
     * Get the comment on this locale. Strip out @ text.
     *
     * @param l
     * @return string or null
     */
    public static String getComment(CLDRLocale l) {
        return getCommentRaw(l).replaceAll("@", "");
    }

    /**
     * Get the comment on this locale. Include "@locale" markers.
     *
     * @param l
     * @return string or null
     */
    public static String getCommentRaw(CLDRLocale l) {
        return getInstance().getCommentInternal(l).replaceAll("@@", "@" + l.getBaseName());
    }

    /**
     * Singleton object
     */
    private static SpecialLocales singleton = null;

    /**
     * Internal accessor. All access is via the static functions.
     * @return
     */
    private static synchronized SpecialLocales getInstance() {
        if (singleton == null) {
            singleton = new SpecialLocales();
        }
        return singleton;
    }

    private Map<CLDRLocale, Type> specials = new HashMap<>();
    private Map<Type, Set<CLDRLocale>> types = new HashMap<>();
    private Map<CLDRLocale, String> comments = new HashMap<>();
    private Set<CLDRLocale> specialsWildcards = new HashSet<>();

    public Set<CLDRLocale> getByTypeInternal(Type t) {
        return types.get(t);
    }

    public Type getTypeInternal(CLDRLocale l) {
        l = findLocale(l, l);
        return specials.get(l);
    }

    public String getCommentInternal(CLDRLocale l) {
        l = findLocale(l, l);
        return comments.get(l);
    }

    public CLDRLocale findLocale(CLDRLocale fromLocale, CLDRLocale origLocale) {
        if (origLocale == fromLocale && specials.containsKey(origLocale)) {
            return origLocale; // explicit locale - no search.
        }
        if (fromLocale == null) {
            return origLocale;
        }
        if (specialsWildcards.contains(fromLocale)) {
            return fromLocale;
        }
        return findLocale(fromLocale.getParent(), origLocale);
    }

    /**
     * Internal constructor
     */
    private SpecialLocales() {
        // First, read the algorithmic locales.
        for(final LocaleTransform lt : CLDRFileTransformer.LocaleTransform.values()) {
            if(lt.getPolicyIfExisting() != CLDRFileTransformer.PolicyIfExisting.DISCARD) {
                continue;
            }
            // Add each of these as if they were in SpecialLocales.txt
            CLDRLocale inputLocale = CLDRLocale.getInstance(lt.getInputLocale());
            CLDRLocale outputLocale = CLDRLocale.getInstance(lt.getOutputLocale());

            // add as readonly
            addToType(Type.readonly, outputLocale);

            // add similar comment to SpecialLocales.txt
            comments.put(outputLocale, "@"+outputLocale.getBaseName()+" is generated from @"+inputLocale.getBaseName() +
                " via transliteration, and so @@ may not be edited directly. Edit @"+inputLocale.getBaseName()+" to make changes.");
        }

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
                String id = stuff.get(0);
                boolean includeSublocs = (id.endsWith(INCLUDE_SUBLOCALES));
                if (includeSublocs) {
                    id = id.substring(0, id.length() - INCLUDE_SUBLOCALES.length());
                }
                String type = stuff.get(1);
                String comment = stuff.get(2);
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

                addToType(t, l);
                if (includeSublocs) {
                    specialsWildcards.add(l);
                }
                if (!comment.isEmpty()) {
                    comments.put(l, comment);
                }
                if (false) {
                    System.out.println(SpecialLocales.class.getSimpleName() + ": locale " + l + ", includejSublocs=" + includeSublocs + ", type=" + t
                        + ", comment: " + comment);
                }
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException("Internal Error", e);
        }
        specials = Collections.unmodifiableMap(specials);
        specialsWildcards = Collections.unmodifiableSet(specialsWildcards);
        comments = Collections.unmodifiableMap(comments);
        types = Collections.unmodifiableMap(types);
    }

    private Set<CLDRLocale> addToType(Type t, CLDRLocale l) {
        Set<CLDRLocale> s = types.get(t);
        if (s == null) {
            s = new TreeSet<>();
            types.put(t, s);
        }
        s.add(l);
        specials.put(l, t);
        return s;
    }

}
