package org.unicode.cldr.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public abstract class MatchValue implements Predicate<String> {
    @Override
    public abstract boolean is(String item);
    public abstract String getName();

    @Override
    public String toString() {
        return getName();
    }

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
            case "or":
                result =  OrMatchValue.of(subargument);
                break;
            case "unicodeset":
                result =  UnicodeSpanMatchValue.of(subargument);
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
            LanguageTagParser ltp;
            try {
                ltp = new LanguageTagParser().set(item);
            } catch (Exception e) {
                return false;
            }
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

    public static class EnumParser<T extends Enum> {
        private final Class<T> aClass;
        private final Set<T> all;

        private EnumParser(Class<T> aClass) {
            this.aClass = aClass;
            all = ImmutableSet.copyOf(EnumSet.allOf(aClass));
        }

        public static <T> EnumParser of(Class<T> aClass) {
            return new EnumParser(aClass);
        }

        public Set<T> parse(String text) {
            Set<T> statuses = EnumSet.noneOf(aClass);
            boolean negative = text.startsWith("!");
            if (negative) {
                text = text.substring(1);
            }
            for (String item : SPLIT_SPACE_OR_COMMA.split(text)) {
                statuses.add(getItem(item));
            }
            if (negative) {
                TreeSet<T> temp = new TreeSet<>(all);
                temp.removeAll(statuses);
                statuses = temp;
            }
            return ImmutableSet.copyOf(statuses);
        }
        private T getItem(String text) {
            try {
                return (T) aClass.getMethod("valueOf", String.class).invoke(null, text);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        public String format(Set<?> set) {
            if (set.size() > all.size()/2) {
                TreeSet<T> temp = new TreeSet<>(all);
                temp.removeAll(set);
                return "!" + Joiner.on(' ').join(temp);
            } else {
                return Joiner.on(' ').join(set);
            }
        }

        public boolean isAll(Set<Status> statuses) {
            return statuses.equals(all);
        }
    }

    static public class ValidityMatchValue extends MatchValue {
        private final LstrType type;
        private final boolean shortId;
        private final Set<Status> statuses;
        private static Map<String, Status> shortCodeToStatus;
        private static final EnumParser<Status> enumParser = EnumParser.of(Status.class);

        @Override
        public String getName() {
            return "validity/" 
                + (shortId ? "short-" : "") + type.toString() 
                + (enumParser.isAll(statuses) ? "" : "/" + enumParser.format(statuses));
        }

        private ValidityMatchValue(LstrType type) {
            this(type, null, false);
        }

        private ValidityMatchValue(LstrType type, Set<Status> statuses, boolean shortId) {
            this.type = type;
            if (type != LstrType.unit && shortId) {
                throw new IllegalArgumentException("short- not supported except for units");
            }
            this.shortId = shortId;
            this.statuses = statuses == null ? EnumSet.allOf(Status.class) : ImmutableSet.copyOf(statuses);
        }

        public static MatchValue of(String typeName) {
            if (typeName.equals("locale")) {
                return new LocaleMatchValue();
            }
            int slashPos = typeName.indexOf('/');
            Set<Status> statuses = null;
            if (slashPos > 0) {
                statuses = enumParser.parse(typeName.substring(slashPos+1));
                typeName = typeName.substring(0, slashPos);
            }
            boolean shortId = typeName.startsWith("short-");
            if (shortId) {
                typeName = typeName.substring(6);
            }
            LstrType type = LstrType.valueOf(typeName);
            return new ValidityMatchValue(type, statuses, shortId);
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
            case unit: 
                if (shortId) {
                    if (shortCodeToStatus == null) { // lazy evaluation to avoid circular dependencies
                        Map<String, Status> _shortCodeToStatus = new TreeMap<>();
                        for (Entry<String, Status> entry : Validity.getInstance().getCodeToStatus(LstrType.unit).entrySet()) {
                            String key = entry.getKey();
                            Status status = entry.getValue();
                            final String shortKey = key.substring(key.indexOf('-')+1);
                            Status old = _shortCodeToStatus.get(shortKey);
                            if (old == null) {
                                _shortCodeToStatus.put(shortKey, status);
//                            } else {
//                                System.out.println("Skipping duplicate status: " + key + " old: " + old + " new: " + status);
                            }
                        }
                        shortCodeToStatus = ImmutableMap.copyOf(_shortCodeToStatus);
                    }
                    final Status status = shortCodeToStatus.get(item);
                    return status != null && statuses.contains(status);
                }
            default: break;
            }
            final Status status = Validity.getInstance().getCodeToStatus(type).get(item);
            return status != null && statuses.contains(status);
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

    static final Splitter LIST = Splitter.on(", ").trimResults();
    static final Splitter SPLIT_SPACE_OR_COMMA = Splitter.on(Pattern.compile("[, ]")).omitEmptyStrings().trimResults();

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
        final String key;

        public AnyMatchValue(String key) {
            this.key = key;
        }

        @Override
        public String getName() {
            return "any" + (key == null ? "" : "/" + key);
        }

        public static AnyMatchValue of(String key) {
            return new AnyMatchValue(key);
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

    static final Splitter BARS_SPLITTER = Splitter.on("||").omitEmptyStrings();

    static public class OrMatchValue extends MatchValue {
        final List<MatchValue> subtests;

        private OrMatchValue(Iterator<MatchValue> iterator) {
            this.subtests = ImmutableList.copyOf(CollectionUtilities.addAll(iterator, new ArrayList<>()));
        }

        @Override
        public String getName() {
            return "or/"+ CollectionUtilities.join(subtests, "||");
        }

        public static OrMatchValue of(String key) {
            IntFunction<MatchValue[]> generator = null;
            return new OrMatchValue(BARS_SPLITTER.splitToList(key)
                .stream()
                .map(k -> MatchValue.of(k))
                .iterator());
        }

        @Override
        public  boolean is(String item) {
            for (MatchValue subtest : subtests) {
                if (subtest.is(item)) {
                    return true;
                }
            }
            return false;
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

    static public class UnicodeSpanMatchValue extends MatchValue {
        final UnicodeSet uset;

        public UnicodeSpanMatchValue(String key) {
            uset = new UnicodeSet(key);
        }

        @Override
        public String getName() {
            return "unicodeset/" + uset;
        }

        public static UnicodeSpanMatchValue of(String key) {
            return new UnicodeSpanMatchValue(key);
        }

        @Override
        public  boolean is(String item) {
            return uset.span(item, SpanCondition.CONTAINED) == item.length();
        }
    }

}
