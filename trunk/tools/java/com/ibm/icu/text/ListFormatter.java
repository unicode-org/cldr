package com.ibm.icu.text;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import com.ibm.icu.util.ULocale;

/**
 * Immutable class for formatting a list, using data from CLDR (or supplied
 * separately). The class is currently not subclassable.
 * 
 * @author markdavis
 */
final public class ListFormatter implements Transform<Collection<String>, String> {
    // TODO optimize this for the common case that the patterns are all of the
    // form {0}<sometext>{1}.
    // For now, we avoid MessageFormat, because there is no "sub" formatting.
    private final String two;
    private final String start;
    private final String middle;
    private final String end;

    /**
     * Create a ListFormatter from component strings, with definitions as in
     * LDML.
     * 
     * @param two
     *            string for two items, containing {0} for the first, and {1}
     *            for the second.
     * @param start
     *            string for the start of a list items, containing {0} for the
     *            first, and {1} for the rest.
     * @param middle
     *            string for the start of a list items, containing {0} for the
     *            first part of the list, and {1} for the rest of the list.
     * @param end
     *            string for the end of a list items, containing {0} for the
     *            first part of the list, and {1} for the last item.
     */
    public ListFormatter(String two, String start, String middle, String end) {
        this.two = two;
        this.start = start;
        this.middle = middle;
        this.end = end;
    }

    /**
     * Create a list formatter that is appropriate for a locale.
     * 
     * @param locale
     *            the locale in question.
     * @return ListFormatter
     */
    public static ListFormatter getInstance(ULocale locale) {
        // TODO: get ICU data from resource bundle
        return null;
    }

    /**
     * Create a list formatter that is appropriate for a locale.
     * 
     * @param locale
     *            the locale in question.
     * @return ListFormatter
     */
    public static ListFormatter getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    /**
     * Format a list of objects.
     * 
     * @param items
     *            items to format. The toString() method is called on each.
     * @return items formatted into a string
     */
    public String format(Object... items) {
        // TODO: optimize
        return format(Arrays.asList(items));
    }

    /**
     * Format a collation of objects. The toString() method is called on each.
     * 
     * @param items
     *            items to format. The toString() method is called on each.
     * @return items formatted into a string
     */
    public String format(Collection<Object> items) {
        Iterator<Object> it = items.iterator();
        int count = items.size();
        switch (count) {
        case 0:
            return "";
        case 1:
            return it.next().toString();
        case 2:
            return format2(two, it.next(), it.next());
        }
        String result = it.next().toString();
        result = format2(start, result, it.next());
        for (count -= 3; count > 0; --count) {
            result = format2(middle, result, it.next());
        }
        return format2(end, result, it.next());
    }

    private String format2(String pattern, Object a, Object b) {
        // TODO: make slightly faster by using single pass.
        return pattern.replace("{0}", a.toString()).replace("{1}", b.toString());
    }

    @Override
    public String transform(Collection<String> source) {
        return format(source);
    }
}
