package org.unicode.cldr.util;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.StandardCodes.LstrType;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public abstract class MatchValue implements Predicate<String> {
    @Override
    public abstract boolean is(String item);
    public abstract String getName();

    public static MatchValue of(String command) {
        String originalArg = command;
        int colonPos = command.indexOf('/');
        String subargument = null;
        if (colonPos >= 0) {
            subargument = command.substring(colonPos + 1);
            command = command.substring(0, colonPos);
        }
        try {
            MatchValue result = null;
            switch (command) {
            case "any":
                result = AnyMatchValue.of(subargument);
                break;
            case "set":
                result =  SetMatchValue.of(subargument);
                break;
            case "validity": 
                result =  ValidityMatchValue.of(subargument);
                break;
            case "bcp47": 
                result =  Bcp47MatchValue.of(subargument);
                break;
            case "range": 
                result =  RangeMatchValue.of(subargument);
                break;
            case "literal":
                result =  LiteralMatchValue.of(subargument);
                break;
            case "regex":
                result =  RegexMatchValue.of(subargument);
                break;
            case "metazone":
                result =  MetazoneMatchValue.of(subargument);
                break;
            case "version":
                result =  VersionMatchValue.of(subargument);
                break;
            case "time":
                result =  TimeMatchValue.of(subargument);
                break;
            default: 
                throw new IllegalArgumentException("Illegal/Unimplemented match type: " + originalArg);
            }
            if (!originalArg.equals(result.getName())) {
                System.err.println("Non-standard form or error: " + originalArg + " ==> " + result.getName());
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Problem with: " + originalArg, e);
        }
    }

    static class LocaleMatchValue extends MatchValue {
        private final Predicate<String> lang = new ValidityMatchValue(LstrType.language);
        private final Predicate<String> script = new ValidityMatchValue(LstrType.script);
        private final Predicate<String> region = new ValidityMatchValue(LstrType.region);
        private final Predicate<String> variant = new ValidityMatchValue(LstrType.variant);

        @Override
        public String getName() {
            return "validity/locale";
        }

        @Override
        public boolean is(String item) {
            if (!item.contains("_")) {
                return lang.is(item);
            }
            LanguageTagParser ltp = new LanguageTagParser().set(item);
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

    public static <T> boolean and(Predicate<T> predicate, Iterable<T> items) {
        for (T item : items) {
            if (!predicate.is(item)) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean or(Predicate<T> predicate, Iterable<T> items) {
        for (T item : items) {
            if (predicate.is(item)) {
                return true;
            }
        }
        return false;
    }

    static public class ValidityMatchValue extends MatchValue {
        private final LstrType type;

        @Override
        public String getName() {
            return "validity/" + type.toString();
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
        public synchronized boolean is(String item) {
            if (valid == null) { // must lazy-eval
                SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
                Relation<String, String> keyToSubtypes = sdi.getBcp47Keys();
                Relation<R2<String, String>, String> keySubtypeToAliases = sdi.getBcp47Aliases();
                Map<String, String> aliasesToKey = new HashMap<>();
                for (String key : keyToSubtypes.keySet()) {
                    Set<String> aliases = keySubtypeToAliases.get(Row.of(key, ""));
                    if (aliases != null) {
                        for (String alias : aliases) {
                            aliasesToKey.put(alias, key);
                        }
                    }
                }
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
                    if (subtypeList == null) {
                        String key2 = aliasesToKey.get(key);
                        if (key2 != null) {
                            subtypeList = keyToSubtypes.get(key2);
                        }
                    }
                    try {
                        valid = new TreeSet<>(subtypeList);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Illegal keyValue: " + getName());
                    }
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

    // TODO: have Range that can be ints, doubles, or versions
    static public class RangeMatchValue extends MatchValue {
        private final double start;
        private final double end;
        private final boolean isInt;

        @Override
        public String getName() {
            return "range/" + (isInt ? (long)start + "~" + (long)end : start + "~" + end);
        }

        private RangeMatchValue(String key) {
            Iterator<String> parts = RANGE.split(key).iterator();
            start = Double.parseDouble(parts.next());
            end = Double.parseDouble(parts.next());
            isInt = !key.contains(".");
            if (parts.hasNext()) {
                throw new IllegalArgumentException("Range must be of form <int>~<int>");
            }
        }

        public static RangeMatchValue of(String key) {
            return new RangeMatchValue(key);
        }

        @Override
        public boolean is(String item) {
            if (isInt && item.contains(".")) {
                return false;
            }
            double value;
            try {
                value = Double.parseDouble(item);
            } catch (NumberFormatException e) {
                return false;
            }
            return start <= value && value <= end;
        }
    }

    static final Splitter LIST = Splitter.on(',').trimResults();

    static public class LiteralMatchValue extends MatchValue {
        private final Set<String> items;

        @Override
        public String getName() {
            return "literal/" + CollectionUtilities.join(items, ", ");
        }

        private LiteralMatchValue(String key) {
            items = ImmutableSet.copyOf(LIST.splitToList(key));
        }

        public static LiteralMatchValue of(String key) {
            return new LiteralMatchValue(key);
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

    static public class VersionMatchValue extends MatchValue {

        @Override
        public String getName() {
            return "version";
        }

        private VersionMatchValue(String key) {
        }

        public static VersionMatchValue of(String key) {
            if (key != null) {
                throw new IllegalArgumentException("No parameter allowed");
            }
            return new VersionMatchValue(key);
        }

        @Override
        public boolean is(String item) {
            try {
                VersionInfo.getInstance(item);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
    }

    static public class MetazoneMatchValue extends MatchValue {
        private Set<String> valid;

        @Override
        public String getName() {
            return "metazone";
        }

        public static MetazoneMatchValue of(String key) {
            if (key != null) {
                throw new IllegalArgumentException("No parameter allowed");
            }
            return new MetazoneMatchValue();
        }

        @Override
        public synchronized boolean is(String item) {
            // must lazy-eval
            if (valid == null) {
                SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
                valid = sdi.getAllMetazones();
            }
            return valid.contains(item);
        }
    }

    static public class AnyMatchValue extends MatchValue {

        @Override
        public String getName() {
            return "any";
        }

        public static AnyMatchValue of(String key) {
            if (key != null) {
                throw new IllegalArgumentException("No parameter allowed");
            }
            return new AnyMatchValue();
        }

        @Override
        public boolean is(String item) {
            return true;
        }
    }

    static final Splitter SPACE_SPLITTER = Splitter.on(' ').omitEmptyStrings();

    static public class SetMatchValue extends MatchValue {
        final MatchValue subtest;

        public SetMatchValue(MatchValue subtest) {
            this.subtest = subtest;
        }

        @Override
        public String getName() {
            return "set/"+subtest.getName();
        }

        public static SetMatchValue of(String key) {
            return new SetMatchValue(MatchValue.of(key));
        }

        @Override
        public  boolean is(String items) {
            return and(subtest,SPACE_SPLITTER.split(items));
        }
    }

    static public class TimeMatchValue extends MatchValue {
        final SimpleDateFormat formatter;

        public TimeMatchValue(String key) {
            formatter = new SimpleDateFormat(key,ULocale.ROOT);
        }

        @Override
        public String getName() {
            return "time/" + formatter.toPattern();
        }

        public static TimeMatchValue of(String key) {
            return new TimeMatchValue(key);
        }

        @Override
        public  boolean is(String item) {
            try {
                formatter.parse(item);
                return true;
            } catch (ParseException e) {
                return false;
            }
        }
    }
}
