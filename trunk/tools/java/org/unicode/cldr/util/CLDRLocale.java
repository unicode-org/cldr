// Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved.

package org.unicode.cldr.util;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.ULocale;

/**
 * This class implements a CLDR UTS#35 compliant locale.
 * It differs from ICU and Java locales in that it is singleton based, and that it is Comparable.
 * It uses LocaleIDParser to do the heavy lifting of parsing.
 *
 * @author srl
 * @see LocaleIDParser
 * @see ULocale
 */
public final class CLDRLocale implements Comparable<CLDRLocale> {
    private static final boolean DEBUG = false;

    public interface NameFormatter {
        String getDisplayName(CLDRLocale cldrLocale);

        String getDisplayName(CLDRLocale cldrLocale, boolean onlyConstructCompound, Transform<String, String> altPicker);

        String getDisplayLanguage(CLDRLocale cldrLocale);

        String getDisplayScript(CLDRLocale cldrLocale);

        String getDisplayVariant(CLDRLocale cldrLocale);

        String getDisplayCountry(CLDRLocale cldrLocale);
    }

    public static class SimpleFormatter implements NameFormatter {
        private LocaleDisplayNames ldn;

        public SimpleFormatter(ULocale displayLocale) {
            this.ldn = LocaleDisplayNames.getInstance(displayLocale);
        }

        public LocaleDisplayNames getDisplayNames() {
            return ldn;
        }

        public LocaleDisplayNames setDisplayNames(LocaleDisplayNames ldn) {
            return this.ldn = ldn;
        }

        @Override
        public String getDisplayVariant(CLDRLocale cldrLocale) {
            return ldn.variantDisplayName(cldrLocale.getVariant());
        }

        @Override
        public String getDisplayCountry(CLDRLocale cldrLocale) {
            return ldn.regionDisplayName(cldrLocale.getCountry());
        }

        @Override
        public String getDisplayName(CLDRLocale cldrLocale) {
            StringBuffer sb = new StringBuffer();
            String l = cldrLocale.getLanguage();
            String s = cldrLocale.getScript();
            String r = cldrLocale.getCountry();
            String v = cldrLocale.getVariant();

            if (l != null && !l.isEmpty()) {
                sb.append(getDisplayLanguage(cldrLocale));
            } else {
                sb.append("?");
            }
            if ((s != null && !s.isEmpty()) ||
                (r != null && !r.isEmpty()) ||
                (v != null && !v.isEmpty())) {
                sb.append(" (");
                if (s != null && !s.isEmpty()) {
                    sb.append(getDisplayScript(cldrLocale)).append(",");
                }
                if (r != null && !r.isEmpty()) {
                    sb.append(getDisplayCountry(cldrLocale)).append(",");
                }
                if (v != null && !v.isEmpty()) {
                    sb.append(getDisplayVariant(cldrLocale)).append(",");
                }
                sb.replace(sb.length() - 1, sb.length(), ")");
            }
            return sb.toString();
        }

        @Override
        public String getDisplayScript(CLDRLocale cldrLocale) {
            return ldn.scriptDisplayName(cldrLocale.getScript());
        }

        @Override
        public String getDisplayLanguage(CLDRLocale cldrLocale) {
            return ldn.languageDisplayName(cldrLocale.getLanguage());
        }

        @Override
        public String getDisplayName(CLDRLocale cldrLocale, boolean onlyConstructCompound, Transform<String, String> altPicker) {
            return getDisplayName(cldrLocale);
        }
    }

    /**
     * @author srl
     *
     * This formatter will delegate to CLDRFile.getName if a CLDRFile is given, otherwise StandardCodes
     */
    public static class CLDRFormatter extends SimpleFormatter {
        private FormatBehavior behavior = FormatBehavior.extend;

        private CLDRFile file = null;

        public CLDRFormatter(CLDRFile fromFile) {
            super(CLDRLocale.getInstance(fromFile.getLocaleID()).toULocale());
            file = fromFile;
        }

        public CLDRFormatter(CLDRFile fromFile, FormatBehavior behavior) {
            super(CLDRLocale.getInstance(fromFile.getLocaleID()).toULocale());
            this.behavior = behavior;
            file = fromFile;
        }

        public CLDRFormatter() {
            super(ULocale.ROOT);
        }

        public CLDRFormatter(FormatBehavior behavior) {
            super(ULocale.ROOT);
            this.behavior = behavior;
        }

        @Override
        public String getDisplayVariant(CLDRLocale cldrLocale) {
            if (file != null) return file.getName("variant", cldrLocale.getVariant());
            return tryForBetter(super.getDisplayVariant(cldrLocale),
                cldrLocale.getVariant(),
                "variant");
        }

        @Override
        public String getDisplayName(CLDRLocale cldrLocale) {
            if (file != null) return file.getName(cldrLocale.toDisplayLanguageTag(), true, null);
            return super.getDisplayName(cldrLocale);
        }

        @Override
        public String getDisplayName(CLDRLocale cldrLocale, boolean onlyConstructCompound, Transform<String, String> altPicker) {
            if (file != null) return file.getName(cldrLocale.toDisplayLanguageTag(), onlyConstructCompound, altPicker);
            return super.getDisplayName(cldrLocale);
        }

        @Override
        public String getDisplayScript(CLDRLocale cldrLocale) {
            if (file != null) return file.getName("script", cldrLocale.getScript());
            return tryForBetter(super.getDisplayScript(cldrLocale),
                cldrLocale.getScript(),
                "language");
        }

        @Override
        public String getDisplayLanguage(CLDRLocale cldrLocale) {
            if (file != null) return file.getName("language", cldrLocale.getLanguage());
            return tryForBetter(super.getDisplayLanguage(cldrLocale),
                cldrLocale.getLanguage(),
                "language");
        }

        @Override
        public String getDisplayCountry(CLDRLocale cldrLocale) {
            if (file != null) return file.getName("territory", cldrLocale.getCountry());
            return tryForBetter(super.getDisplayLanguage(cldrLocale),
                cldrLocale.getLanguage(),
                "territory");
        }

        private String tryForBetter(String superString, String code, String type) {
            if (superString.equals(code)) {
                String fromLst = StandardCodes.make().getData("language", code);
                if (fromLst != null && !fromLst.equals(code)) {
                    switch (behavior) {
                    case replace:
                        return fromLst;
                    case extend:
                        return superString + " [" + fromLst + "]";
                    case extendHtml:
                        return superString + " [<i>" + fromLst + "</i>]";
                    }
                }
            }
            return superString;
        }
    }

    public enum FormatBehavior {
        replace, extend, extendHtml
    };

    /**
     * Reference to the parent CLDRLocale
     */
    private CLDRLocale parent = null;
    /**
     * Cached ICU format locale
     */
    private ULocale ulocale;
    /**
     * base name, 'without parameters'. Currently same as fullname.
     */
    private String basename;
    /**
     * Full name
     */
    private String fullname;
    /**
     * The LocaleIDParser interprets the various parts (language, country, script, etc).
     */
    private LocaleIDParser parts = null;

    /**
     * Construct a CLDRLocale from an ICU ULocale.
     * Internal, called by the factory function.
     */
    private CLDRLocale(ULocale loc) {
        init(loc);
    }

    /**
     * Returns the BCP47 langauge tag for all except root. For root, returns "root".
     * @return
     */
    private String toDisplayLanguageTag() {
        if (getBaseName().equals("root")) {
            return "root";
        } else {
            return toLanguageTag();
        }
    }

    /**
     * Return BCP47 language tag
     * @return
     */
    public String toLanguageTag() {
        return ulocale.toLanguageTag();
    }

    /**
     * Construct a CLDRLocale from a string with the full locale ID.
     * Internal, called by the factory function.
     *
     * @param str
     */
    private CLDRLocale(String str) {
        init(str);
    }

    /**
     * Initialize a CLDRLocale from a ULocale
     *
     * @param loc
     */
    private void init(ULocale loc) {
        ulocale = loc;
        init(loc.getBaseName());
    }

    /**
     * Initialize a CLDRLocale from a string.
     *
     * @param str
     */
    private void init(String str) {
        // if(str.length()==0) {
        // str = "root";
        // }
        str = process(str);
        // System.err.println("bn: " + str);
        if (str.equals(ULocale.ROOT.getBaseName()) || str.equalsIgnoreCase("root")) {
            fullname = "root";
            parent = null;
        } else {
            parts = new LocaleIDParser();
            parts.set(str);
            fullname = parts.toString();
            String parentId = LocaleIDParser.getParent(str);
            if (DEBUG) System.out.println(str + " par = " + parentId);
            if (parentId != null) {
                parent = CLDRLocale.getInstance(parentId);
            } else {
                parent = null; // probably, we are root or we are supplemental
            }
        }
        basename = fullname;
        if (ulocale == null) {
            ulocale = new ULocale(fullname);
        }
    }

    /**
     * Return the full locale name, in CLDR format.
     */
    public String toString() {
        return fullname;
    }

    /**
     * Return the base locale name, in CLDR format, without any @keywords
     *
     * @return
     */
    public String getBaseName() {
        return basename;
    }

    /**
     * internal: process a string from ICU to CLDR form. For now, just collapse double underscores.
     *
     * @param baseName
     * @return
     * @internal
     */
    private String process(String baseName) {
        return baseName.replaceAll("__", "_");
    }

    /**
     * Compare to another CLDRLocale. Uses string order of toString().
     */
    public int compareTo(CLDRLocale o) {
        if (o == this) return 0;
        return fullname.compareTo(o.fullname);
    }

    /**
     * Hashcode - is the hashcode of the full string
     */
    public int hashCode() {
        return fullname.hashCode();
    }

    /**
     * Convert to an ICU compatible ULocale.
     *
     * @return
     */
    public ULocale toULocale() {
        return ulocale;
    }

    /**
     * Allocate a CLDRLocale (could be a singleton). If null is passed in, null will be returned.
     *
     * @param s
     * @return
     */
    public static CLDRLocale getInstance(String s) {
        if (s == null) return null;
        CLDRLocale loc = stringToLoc.get(s);
        if (loc == null) {
            loc = new CLDRLocale(s);
            loc.register();
        }
        return loc;
    }

    /**
     * Public factory function. Allocate a CLDRLocale (could be a singleton). If null is passed in, null will be
     * returned.
     *
     * @param u
     * @return
     */
    public static CLDRLocale getInstance(ULocale u) {
        if (u == null) return null;
        CLDRLocale loc = ulocToLoc.get(u);
        if (loc == null) {
            loc = new CLDRLocale(u);
            loc.register();
        }
        return loc;
    }

    /**
     * Register the singleton instance.
     */
    private void register() {
        stringToLoc.put(this.toString(), this);
        ulocToLoc.put(this.toULocale(), this);
    }

    private static Hashtable<String, CLDRLocale> stringToLoc = new Hashtable<String, CLDRLocale>();
    private static Hashtable<ULocale, CLDRLocale> ulocToLoc = new Hashtable<ULocale, CLDRLocale>();

    /**
     * Return the parent locale of this item. Null if no parent (root has no parent)
     *
     * @return
     */
    public CLDRLocale getParent() {
        return parent;
    }

    /**
     * Returns true if other is equal to or is an ancestor of this, false otherwise
     */
    public boolean childOf(CLDRLocale other) {
        if (other == null) return false;
        if (other == this) return true;
        if (parent == null) return false; // end
        return parent.childOf(other);
    }

    /**
     * Return an iterator that will iterate over locale, parent, parent etc, finally reaching root.
     *
     * @return
     */
    public Iterable<CLDRLocale> getParentIterator() {
        final CLDRLocale newThis = this;
        return new Iterable<CLDRLocale>() {
            public Iterator<CLDRLocale> iterator() {
                return new Iterator<CLDRLocale>() {
                    CLDRLocale what = newThis;

                    public boolean hasNext() {
                        // TODO Auto-generated method stub
                        return what.getParent() != null;
                    }

                    public CLDRLocale next() {
                        // TODO Auto-generated method stub
                        CLDRLocale curr = what;
                        if (what != null) {
                            what = what.getParent();
                        }
                        return curr;
                    }

                    public void remove() {
                        throw new InternalError("unmodifiable iterator");
                    }

                };
            }
        };
    }

    /**
     * Get the 'language' locale, as an object. Might be 'this'.
     * @return
     */
    public CLDRLocale getLanguageLocale() {
        return getInstance(getLanguage());
    }

    public String getLanguage() {
        return parts == null ? fullname : parts.getLanguage();
    }

    public String getScript() {
        return parts == null ? null : parts.getScript();
    }

    public boolean isLanguageLocale() {
        return this.equals(getLanguageLocale());
    }

    /**
     * Return the region
     *
     * @return
     */
    public String getCountry() {
        return parts == null ? null : parts.getRegion();
    }

    /**
     * Return "the" variant.
     *
     * @return
     */
    public String getVariant() {
        return toULocale().getVariant(); // TODO: replace with parts?
    }

    /**
     * Most objects should be singletons, and so equality/inequality comparison is done first.
     */
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CLDRLocale)) return false;
        return (0 == compareTo((CLDRLocale) o));
    }

    /**
     * The root locale, a singleton.
     */
    public static final CLDRLocale ROOT = getInstance(ULocale.ROOT);

    public String getDisplayName() {
        return getDisplayName(getDefaultFormatter());
    }

    public String getDisplayRegion() {
        return getDisplayCountry(getDefaultFormatter());
    }

    public String getDisplayVariant() {
        return getDisplayVariant(getDefaultFormatter());
    }

    public String getDisplayName(boolean combined, Transform<String, String> picker) {
        return getDisplayName(getDefaultFormatter(), combined, picker);
    }

    /**
     * These functions wrap calls to the displayLocale, but are provided to supply an interface that looks similar to
     * ULocale.getDisplay___(displayLocale)
     *
     * @param displayLocale
     * @return
     */
    public String getDisplayName(NameFormatter displayLocale) {
        if (displayLocale == null) displayLocale = getDefaultFormatter();
        return displayLocale.getDisplayName(this);
    }

//    private static LruMap<ULocale, NameFormatter> defaultFormatters = new LruMap<ULocale, NameFormatter>(1);
    private static Cache<ULocale, NameFormatter> defaultFormatters = CacheBuilder.newBuilder().initialCapacity(1).build();
    private static NameFormatter gDefaultFormatter = getSimpleFormatterFor(ULocale.getDefault());

    public static NameFormatter getSimpleFormatterFor(ULocale loc) {
//        NameFormatter nf = defaultFormatters.get(loc);
//        if (nf == null) {
//            nf = new SimpleFormatter(loc);
//            defaultFormatters.put(loc, nf);
//        }
//        return nf;
//        return defaultFormatters.getIfPresent(loc);
        final ULocale uLocFinal = loc;
        try {
            return defaultFormatters.get(loc, new Callable<NameFormatter>() {

                @Override
                public NameFormatter call() throws Exception {
                    return new SimpleFormatter(uLocFinal);
                }
            });
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public String getDisplayName(ULocale displayLocale) {
        return getSimpleFormatterFor(displayLocale).getDisplayName(this);
    }

    public static NameFormatter getDefaultFormatter() {
        return gDefaultFormatter;
    }

    public static NameFormatter setDefaultFormatter(NameFormatter nf) {
        return gDefaultFormatter = nf;
    }

    /**
     * These functions wrap calls to the displayLocale, but are provided to supply an interface that looks similar to
     * ULocale.getDisplay___(displayLocale)
     *
     * @param displayLocale
     * @return
     */
    public String getDisplayCountry(NameFormatter displayLocale) {
        if (displayLocale == null) displayLocale = getDefaultFormatter();
        return displayLocale.getDisplayCountry(this);
    }

    /**
     * These functions wrap calls to the displayLocale, but are provided to supply an interface that looks similar to
     * ULocale.getDisplay___(displayLocale)
     *
     * @param displayLocale
     * @return
     */
    public String getDisplayVariant(NameFormatter displayLocale) {
        if (displayLocale == null) displayLocale = getDefaultFormatter();
        return displayLocale.getDisplayVariant(this);
    }

    /**
     * Construct an instance from an array
     *
     * @param available
     * @return
     */
    public static Set<CLDRLocale> getInstance(Iterable<String> available) {
        Set<CLDRLocale> s = new TreeSet<CLDRLocale>();
        for (String str : available) {
            s.add(CLDRLocale.getInstance(str));
        }
        return s;
    }

    public interface SublocaleProvider {
        public Set<CLDRLocale> subLocalesOf(CLDRLocale forLocale);
    }

    public String getDisplayName(NameFormatter engFormat, boolean combined, Transform<String, String> picker) {
        return engFormat.getDisplayName(this, combined, picker);
    }

    /**
     * Return the highest parent that is a child of root, or null.
     * @return highest parent, or null.  ROOT.getHighestNonrootParent() also returns null.
     */
    public CLDRLocale getHighestNonrootParent() {
        CLDRLocale res;
        if (this == ROOT) {
            res = null;
            ;
        } else if (this.parent == ROOT) {
            res = this;
        } else if (this.parent == null) {
            res = this;
        } else {
            res = parent.getHighestNonrootParent();
        }
        if (DEBUG) System.out.println(this + ".HNRP=" + res);
        return res;
    }
}
