package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.StandardCodes.LstrType;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;

public abstract class MatchValue implements Predicate<String> {
    @Override
    public abstract boolean is(String item);
    public abstract String getName();

    public static MatchValue of(String command) {
        int colonPos = command.indexOf('/');
        String subargument = null;
        if (colonPos >= 0) {
            subargument = command.substring(colonPos + 1);
            command = command.substring(0, colonPos);
        }
        switch (command) {
        case "validity": 
            return ValidityMatchValue.of(subargument);
        case "bcp47": 
            return Bcp47MatchValue.of(subargument);
        case "range": 
            return RangeMatchValue.of(subargument);
        case "list":
            return ListMatchValue.of(subargument);
        case "regex":
            return RegexMatchValue.of(subargument);
        case "metazone":
            return MetazoneMatchValue.of(subargument);
        default: 
            throw new IllegalArgumentException("Illegal/Unimplemented match type: " + subargument);
        }
    }

    static class LocaleMatchValue extends MatchValue {
        private LanguageTagParser ltp;
        private Predicate<String> lang = new ValidityMatchValue(LstrType.language);
        private Predicate<String> script = new ValidityMatchValue(LstrType.script);
        private Predicate<String> region = new ValidityMatchValue(LstrType.region);
        private Predicate<String> variant = new ValidityMatchValue(LstrType.variant);

        @Override
        public String getName() {
            return "validity/locale";
        }

        @Override
        public boolean is(String item) {
            if (!item.contains("_")) {
                return lang.is(item);
            }
            if (ltp == null) {
                ltp = new LanguageTagParser();
            }
            ltp.set(item);
            return lang.is(ltp.getLanguage())
                && (ltp.getScript().isEmpty() 
                    || script.is(ltp.getScript()))
                && (ltp.getRegion().isEmpty() 
                    || region.is(ltp.getRegion()))
                && (ltp.getVariants().isEmpty() 
                    || and(variant,ltp.getVariants()))
                && ltp.getExtensions().isEmpty()
                && ltp.getLocaleExtensions().isEmpty()
                ;
        }        
    }

    // TODO remove these if possible — ticket/10120
    static final Set<String> SCRIPT_HACK = ImmutableSet.of(
        "Afak", "Blis", "Cirt", "Cyrs", "Egyd", "Egyh", "Geok", "Inds", "Jurc", "Kpel", "Latf", "Latg",
        "Loma", "Maya", "Moon", "Nkgb", "Phlv", "Roro", "Sara", "Syre", "Syrj", "Syrn", "Teng", "Visp", "Wole");
    static final Set<String> VARIANT_HACK = ImmutableSet.of("POSIX", "REVISED", "SAAHO");

    public static <T> boolean and(Predicate<T> predicate, Collection<T> items) {
        for (T item : items) {
            if (!predicate.is(item)) {
                return false;
            }
        }
        return true;
    }

    static public class ValidityMatchValue extends MatchValue {
        private final LstrType type;

        @Override
        public String getName() {
            return type.toString();
        }

        private ValidityMatchValue(LstrType type) {
            this.type = type;
        }

        public static MatchValue of(String typeName) {
            if (typeName.equals("locale")) {
                return new LocaleMatchValue();
            }
            LstrType type = LstrType.valueOf(typeName);
            return new ValidityMatchValue(type);
        }

        @Override
        public boolean is(String item) {
            // TODO handle deprecated
            switch(type) {
            case script: 
                if (SCRIPT_HACK.contains(item)) {
                    return true; 
                }
                break;
            case variant: 
                if (VARIANT_HACK.contains(item)) {
                    return true; 
                }
                item = item.toLowerCase(Locale.ROOT); 
                break;
            case language: 
                item = item.equals("root") ? "und" : item; 
                break;
            default: break;
            }
            return Validity.getInstance().getCodeToStatus(type).get(item) != null;
        }
    }

    static public class Bcp47MatchValue extends MatchValue {
        private final String key;
        private Set<String> valid;

        @Override
        public String getName() {
            return "bcp47/" + key;
        }

        private Bcp47MatchValue(String key) {
            this.key = key;
        }

        public static Bcp47MatchValue of(String key) {
            return new Bcp47MatchValue(key);
        }

        @Override
        public boolean is(String item) {
            if (valid == null) { // must lazy-eval
                SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
                Relation<String, String> keyToSubtypes = sdi.getBcp47Keys();
                Relation<R2<String, String>, String> keySubtypeToAliases = sdi.getBcp47Aliases();
                Set<String> keyList;
                Set<String> subtypeList;
                // TODO handle deprecated
                // fix data to remove aliases, then narrow this
                switch(key) {
                case "anykey":
                    keyList = keyToSubtypes.keySet();
                    valid = new TreeSet<>(keyList);
                    for (String keyItem : keyList) {
                        addAliases(keySubtypeToAliases, keyItem, "");
                    }
                    valid.add("x"); // TODO: investigate adding to bcp47 data files
                    break;
                case "anyvalue":
                    valid = new TreeSet<>(keyToSubtypes.values());
                    for (String keyItem : keyToSubtypes.keySet()) {
                        subtypeList = keyToSubtypes.get(keyItem);
//                        if (subtypeList == null) {
//                            continue;
//                        }
                        for (String subtypeItem : subtypeList) {
                            addAliases(keySubtypeToAliases, keyItem, subtypeItem);
                        }
                    }
                    valid.add("generic"); // TODO: investigate adding to bcp47 data files
                    break;
                default:
                    subtypeList = keyToSubtypes.get(key);
                    valid = new TreeSet<>(subtypeList);
                    for (String subtypeItem : subtypeList) {
                        addAliases(keySubtypeToAliases, key, subtypeItem);
                    }
                    switch(key) {
                    case "ca":
                        valid.add("generic"); // TODO: investigate adding to bcp47 data files
                        break;
                    }
                    break;
                }
                valid = ImmutableSet.copyOf(valid);
            }
            //<key name="tz" description="Time zone key" alias="timezone">
            //  <type name="adalv" description="Andorra" alias="Europe/Andorra"/>
            // <key name="nu" description="Numbering system type key" alias="numbers">
            //  <type name="adlm" description="Adlam digits" since="30"/>
            return valid.contains(item);
        }

        private void addAliases(Relation<R2<String, String>, String> keySubtypeToAliases, String keyItem, String subtype) {
            Set<String> aliases = keySubtypeToAliases.get(Row.of(keyItem, subtype));
            if (aliases != null && !aliases.isEmpty()) {
                valid.addAll(aliases);
            }
        }
    }

    static final Splitter RANGE = Splitter.on('~').trimResults();

    static public class RangeMatchValue extends MatchValue {
        private final int start;
        private final int end;

        @Override
        public String getName() {
            return "range/" + start + "-" + end;
        }

        private RangeMatchValue(String key) {
            Iterator<String> parts = RANGE.split(key).iterator();
            start = Integer.parseInt(parts.next());
            end = Integer.parseInt(parts.next());
            if (parts.hasNext()) {
                throw new IllegalArgumentException("Range must be of form <int>~<int>");
            }
        }

        public static RangeMatchValue of(String key) {
            return new RangeMatchValue(key);
        }

        @Override
        public boolean is(String item) {
            int value;
            try {
                value = Integer.parseInt(item);
            } catch (NumberFormatException e) {
                return false;
            }
            return start <= value && value <= end;
        }
    }

    static final Splitter LIST = Splitter.on(',').trimResults();

    static public class ListMatchValue extends MatchValue {
        private final Set<String> items;

        @Override
        public String getName() {
            return "list/" + CollectionUtilities.join(items, ", ");
        }

        private ListMatchValue(String key) {
            items = ImmutableSet.copyOf(LIST.splitToList(key));
        }

        public static ListMatchValue of(String key) {
            return new ListMatchValue(key);
        }

        @Override
        public boolean is(String item) {
            return items.contains(item);
        }
    }

    static public class RegexMatchValue extends MatchValue {
        private final Pattern pattern;

        @Override
        public String getName() {
            return "regex/" + pattern;
        }

        private RegexMatchValue(String key) {
            pattern = Pattern.compile(key);
        }

        public static RegexMatchValue of(String key) {
            return new RegexMatchValue(key);
        }

        @Override
        public boolean is(String item) {
            return pattern.matcher(item).matches();
        }
    }

    static public class MetazoneMatchValue extends MatchValue {

        @Override
        public String getName() {
            return "metazone";
        }

        public static MetazoneMatchValue of(String key) {
            return new MetazoneMatchValue();
        }

        @Override
        public boolean is(String item) {
            // must lazy-eval
            SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
            return sdi.getAllMetazones().contains(item);
        }
    }

}
