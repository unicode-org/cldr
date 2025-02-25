package org.unicode.cldr.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import com.vdurmont.semver4j.SemverException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity.Status;

public abstract class MatchValue implements Predicate<String> {
    public static final String DEFAULT_SAMPLE = "❓";

    @Override
    public abstract boolean is(String item);

    public abstract String getName();

    public String getSample() {
        return DEFAULT_SAMPLE;
    }

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
                    result = SetMatchValue.of(subargument);
                    break;
                case "validity":
                    result = ValidityMatchValue.of(subargument);
                    break;
                case "bcp47":
                    result = Bcp47MatchValue.of(subargument);
                    break;
                case "range":
                    result = RangeMatchValue.of(subargument);
                    break;
                case "literal":
                    result = LiteralMatchValue.of(subargument);
                    break;
                case "regex":
                    result = RegexMatchValue.of(subargument);
                    break;
                case "semver":
                    result = SemverMatchValue.of(subargument);
                    break;
                case "metazone":
                    result = MetazoneMatchValue.of(subargument);
                    break;
                case "version":
                    result = VersionMatchValue.of(subargument);
                    break;
                case "time":
                    result = TimeMatchValue.of(subargument);
                    break;
                case "or":
                    result = OrMatchValue.of(subargument);
                    break;
                case "unicodeset":
                    result = UnicodeSpanMatchValue.of(subargument);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Illegal/Unimplemented match type: " + originalArg);
            }
            // check for errors in the MatchValue functions
            if (!originalArg.equals(result.getName())) {
                throw new IllegalArgumentException(
                        "Non-standard form or error: " + originalArg + " ==> " + result.getName());
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Problem with: " + originalArg, e);
        }
    }

    /** Check that a bcp47 locale ID is well-formed. Does not check validity. */
    public static class BCP47LocaleWellFormedMatchValue extends MatchValue {
        static final UnicodeSet basechars = new UnicodeSet("[A-Za-z0-9_]");

        public BCP47LocaleWellFormedMatchValue() {}

        @Override
        public String getName() {
            return "validity/bcp47-wellformed";
        }

        @Override
        public boolean is(String item) {
            if (item.equals("und")) return true; // special case because of the matcher
            if (item.contains("_")) return false; // reject any underscores
            try {
                ULocale l = ULocale.forLanguageTag(item);
                if (l == null || l.getBaseName().isEmpty()) {
                    return false; // failed to parse
                }

                // check with lstr parser
                LanguageTagParser ltp = new LanguageTagParser();
                ltp.set(item);
            } catch (Throwable t) {
                return false; // string failed
            }
            return true;
        }

        @Override
        public String getSample() {
            return "de-u-nu-ethi";
        }
    }

    public static class LocaleMatchValue extends MatchValue {
        private final Predicate<String> lang;
        private final Predicate<String> script;
        private final Predicate<String> region;
        private final Predicate<String> variant;

        public LocaleMatchValue() {
            this(null, null, null, null); // use default status
        }

        public LocaleMatchValue(Set<Status> statuses) {
            this(statuses, statuses, statuses, statuses);
        }

        public LocaleMatchValue(
                Set<Status> langStatus,
                Set<Status> scriptStatus,
                Set<Status> regionStatus,
                Set<Status> variantStatus) {
            lang = new ValidityMatchValue(LstrType.language, langStatus, false);
            script = new ValidityMatchValue(LstrType.script, scriptStatus, false);
            region = new ValidityMatchValue(LstrType.region, regionStatus, false);
            variant = new ValidityMatchValue(LstrType.variant, variantStatus, false);
        }

        @Override
        public String getName() {
            return "validity/locale";
        }

        @Override
        public boolean is(String item) {
            if (item.equals("root")) {
                item = "und";
            }
            if (!item.contains("_")) {
                return checkLang(item);
            }
            LanguageTagParser ltp;
            try {
                ltp = new LanguageTagParser().set(item);
            } catch (Exception e) {
                return false;
            }
            return checkLang(ltp.getLanguage())
                    && (ltp.getScript().isEmpty() || script.is(ltp.getScript()))
                    && (ltp.getRegion().isEmpty() || region.is(ltp.getRegion()))
                    && (ltp.getVariants().isEmpty() || and(variant, ltp.getVariants()))
                    && ltp.getExtensions().isEmpty()
                    && ltp.getLocaleExtensions().isEmpty();
        }

        public boolean checkLang(String language) {
            return lang.is(language);
        }

        @Override
        public String getSample() {
            return "de";
        }
    }

    /**
     * Check for the language OR certain backwards-compatible exceptions for data to support
     * retaining variants, namely likelySubtags: "in","iw","ji","jw","mo","tl"
     */
    public static class XLocaleMatchValue extends LocaleMatchValue {
        static final Set<String> exceptions = Set.of("in", "iw", "ji", "jw", "mo", "tl");

        @Override
        public boolean checkLang(String language) {
            return super.checkLang(language) // first check normal
                    || exceptions.contains(language);
        }

        @Override
        public String getName() {
            return "validity/locale-for-likely";
        }
    }

    /**
     * Check for the language OR certain backwards-compatible exceptions for language names: "fat",
     * "sh", "tl", "tw"
     */
    public static class NLocaleMatchValue extends LocaleMatchValue {
        static final Set<String> exceptions = Set.of("fat", "sh", "tl", "tw");

        @Override
        public boolean checkLang(String language) {
            return super.checkLang(language) // first check normal
                    || exceptions.contains(language);
        }

        @Override
        public String getName() {
            return "validity/locale-for-names";
        }
    }

    // TODO remove these if possible — ticket/10120
    static final Set<String> SCRIPT_HACK =
            ImmutableSet.of(
                    "Afak", "Blis", "Cirt", "Cyrs", "Egyd", "Egyh", "Geok", "Inds", "Jurc", "Kpel",
                    "Latf", "Latg", "Loma", "Maya", "Moon", "Nkgb", "Phlv", "Roro", "Sara", "Syre",
                    "Syrj", "Syrn", "Teng", "Visp", "Wole");
    static final Set<String> VARIANT_HACK = ImmutableSet.of("POSIX", "REVISED", "SAAHO");

    /**
     * Returns true if ALL items match the predicate
     *
     * @param <T>
     * @param predicate predicate to check
     * @param items items to be tested with the predicate
     * @return
     */
    public static <T> boolean and(Predicate<T> predicate, Iterable<T> items) {
        for (T item : items) {
            if (!predicate.is(item)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if ANY items match the predicate
     *
     * @param <T>
     * @param predicate predicate to check
     * @param items items to be tested with the predicate
     * @return
     */
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
            if (text == null) {
                return null;
            }
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
            if (set.size() > all.size() / 2) {
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

    public static class ValidityMatchValue extends MatchValue {
        private static final Validity VALIDITY = Validity.getInstance();
        public static final Multimap<LstrType, Status> DEFAULT_STATUS;

        static {
            Multimap<LstrType, Status> DEFAULT_STATUS_ = TreeMultimap.create();
            for (LstrType lstrType : LstrType.values()) {
                switch (lstrType) {
                    case region:
                        DEFAULT_STATUS_.putAll(
                                lstrType,
                                Set.of(
                                        Status.regular,
                                        Status.unknown,
                                        Status.macroregion,
                                        Status.special));
                        break;
                    case language:
                    case script:
                        DEFAULT_STATUS_.putAll(
                                lstrType, Set.of(Status.regular, Status.unknown, Status.special));
                        break;
                    case subdivision:
                    case currency:
                        DEFAULT_STATUS_.putAll(
                                lstrType,
                                Set.of(Status.regular, Status.unknown, Status.deprecated));
                        break;
                    default:
                        DEFAULT_STATUS_.putAll(lstrType, Set.of(Status.regular, Status.unknown));
                        break;
                }
            }
            DEFAULT_STATUS = ImmutableMultimap.copyOf(DEFAULT_STATUS_);
        }

        private static Map<String, Status> shortCodeToStatus;
        private static final EnumParser<Status> validityStatusParser = EnumParser.of(Status.class);

        private final LstrType type;
        private final boolean shortId;
        private final Set<Status> statuses;

        @Override
        public String getName() {
            Collections a;
            return "validity/"
                    + (shortId ? "short-" : "")
                    + type.toString()
                    + (statuses.equals(Set.copyOf(DEFAULT_STATUS.get(type)))
                            ? ""
                            : statuses.equals(VALIDITY.getStatusToCodes(type).keySet())
                                    ? "/all"
                                    : "/" + validityStatusParser.format(statuses));
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
            // validForType = Validity.getInstance().getStatusToCodes(type).keySet();
            this.statuses =
                    ImmutableSet.copyOf(statuses == null ? DEFAULT_STATUS.get(type) : statuses);
        }

        public static MatchValue of(String typeName) {
            if (typeName.equals("locale")) {
                return new LocaleMatchValue();
            }
            if (typeName.equals("locale-for-likely")) {
                return new XLocaleMatchValue();
            }
            if (typeName.equals("locale-for-names")) {
                return new NLocaleMatchValue();
            }
            if (typeName.equals("bcp47-wellformed")) {
                return new BCP47LocaleWellFormedMatchValue();
            }
            String statusPart = null;
            int slashPos = typeName.indexOf('/');
            if (slashPos > 0) {
                statusPart = typeName.substring(slashPos + 1);
                typeName = typeName.substring(0, slashPos);
            }
            boolean shortId = typeName.startsWith("short-");
            if (shortId) {
                typeName = typeName.substring(6);
            }
            LstrType type = LstrType.fromString(typeName);
            Set<Status> statuses =
                    "all".equals(statusPart)
                            ? VALIDITY.getStatusToCodes(type).keySet()
                            : validityStatusParser.parse(statusPart);
            return new ValidityMatchValue(type, statuses, shortId);
        }

        @Override
        public boolean is(String item) {
            // TODO handle deprecated
            switch (type) {
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
                        if (shortCodeToStatus
                                == null) { // lazy evaluation to avoid circular dependencies
                            Map<String, Status> _shortCodeToStatus = new TreeMap<>();
                            for (Entry<String, Status> entry :
                                    VALIDITY.getCodeToStatus(LstrType.unit).entrySet()) {
                                String key = entry.getKey();
                                Status status = entry.getValue();
                                final String shortKey = key.substring(key.indexOf('-') + 1);
                                Status old = _shortCodeToStatus.get(shortKey);
                                if (old == null) {
                                    _shortCodeToStatus.put(shortKey, status);
                                    //                            } else {
                                    //                                System.out.println("Skipping
                                    // duplicate status: " + key + " old: " + old + " new: " +
                                    // status);
                                }
                            }
                            shortCodeToStatus = ImmutableMap.copyOf(_shortCodeToStatus);
                        }
                        final Status status = shortCodeToStatus.get(item);
                        return status != null && statuses.contains(status);
                    }
                default:
                    break;
            }
            final Status status = VALIDITY.getCodeToStatus(type).get(item);
            return status != null && statuses.contains(status);
        }

        @Override
        public String getSample() {
            return VALIDITY.getCodeToStatus(type).keySet().iterator().next();
        }
    }

    public static class Bcp47MatchValue extends MatchValue {
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
                switch (key) {
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
                        switch (key) {
                            case "ca":
                                valid.add(
                                        "generic"); // TODO: investigate adding to bcp47 data files
                                break;
                        }
                        break;
                }
                valid = ImmutableSet.copyOf(valid);
            }
            // <key name="tz" description="Time zone key" alias="timezone">
            //  <type name="adalv" description="Andorra" alias="Europe/Andorra"/>
            // <key name="nu" description="Numbering system type key" alias="numbers">
            //  <type name="adlm" description="Adlam digits" since="30"/>
            return valid.contains(item);
        }

        private void addAliases(
                Relation<R2<String, String>, String> keySubtypeToAliases,
                String keyItem,
                String subtype) {
            Set<String> aliases = keySubtypeToAliases.get(Row.of(keyItem, subtype));
            if (aliases != null && !aliases.isEmpty()) {
                valid.addAll(aliases);
            }
        }

        @Override
        public String getSample() {
            is("X"); // force load data
            return valid == null ? "XX" : valid.iterator().next();
        }
    }

    static final Splitter RANGE = Splitter.on('~').trimResults();

    // TODO: have Range that can be ints, doubles, or versions
    public static class RangeMatchValue extends MatchValue {
        private final double start;
        private final double end;
        private final boolean isInt;

        @Override
        public String getName() {
            return "range/" + (isInt ? (long) start + "~" + (long) end : start + "~" + end);
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

        @Override
        public String getSample() {
            return String.valueOf((int) (start + end) / 2);
        }
    }

    static final Splitter LIST = Splitter.on(", ").trimResults();
    static final Splitter SPLIT_SPACE_OR_COMMA =
            Splitter.on(Pattern.compile("[, ]")).omitEmptyStrings().trimResults();

    public static class LiteralMatchValue extends MatchValue {
        private final Set<String> items;

        @Override
        public String getName() {
            return "literal/" + Joiner.on(", ").join(items);
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

        @Override
        public String getSample() {
            return items.iterator().next();
        }

        /**
         * Return immutable set of items
         *
         * @return
         */
        public Set<String> getItems() {
            return items;
        }
    }

    public static class RegexMatchValue extends MatchValue {
        private final Pattern pattern;

        @Override
        public String getName() {
            return "regex/" + pattern;
        }

        protected RegexMatchValue(String key) {
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

    public static class SemverMatchValue extends MatchValue {
        @Override
        public String getName() {
            return "semver";
        }

        protected SemverMatchValue(String key) {
            super();
        }

        public static SemverMatchValue of(String key) {
            if (key != null) {
                throw new IllegalArgumentException("No parameter allowed");
            }
            return new SemverMatchValue(key);
        }

        @Override
        public boolean is(String item) {
            try {
                new Semver(item, SemverType.STRICT);
                return true;
            } catch (SemverException e) {
                return false;
            }
        }
    }

    public static class VersionMatchValue extends MatchValue {

        @Override
        public String getName() {
            return "version";
        }

        private VersionMatchValue(String key) {}

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

    public static class MetazoneMatchValue extends MatchValue {
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

    public static class AnyMatchValue extends MatchValue {
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

    public static class SetMatchValue extends MatchValue {
        final MatchValue subtest;

        public SetMatchValue(MatchValue subtest) {
            this.subtest = subtest;
        }

        @Override
        public String getName() {
            return "set/" + subtest.getName();
        }

        public static SetMatchValue of(String key) {
            return new SetMatchValue(MatchValue.of(key));
        }

        @Override
        public boolean is(String items) {
            List<String> splitItems = SPACE_SPLITTER.splitToList(items);
            if ((new HashSet<>(splitItems)).size() != splitItems.size()) {
                throw new IllegalArgumentException("Set contains duplicates: " + items);
            }
            return and(subtest, splitItems);
        }

        @Override
        public String getSample() {
            return subtest.getSample();
        }
    }

    static final Splitter BARS_SPLITTER = Splitter.on("||").omitEmptyStrings();

    public static class OrMatchValue extends MatchValue {
        final List<MatchValue> subtests;

        private OrMatchValue(Iterator<MatchValue> iterator) {
            this.subtests = ImmutableList.copyOf(iterator);
        }

        @Override
        public String getName() {
            return "or/" + Joiner.on("||").join(subtests);
        }

        public static OrMatchValue of(String key) {
            return new OrMatchValue(
                    BARS_SPLITTER.splitToList(key).stream().map(k -> MatchValue.of(k)).iterator());
        }

        @Override
        public boolean is(String item) {
            for (MatchValue subtest : subtests) {
                if (subtest.is(item)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getSample() {
            for (MatchValue subtest : subtests) {
                String result = subtest.getSample();
                if (!result.equals(DEFAULT_SAMPLE)) {
                    return result;
                }
            }
            return DEFAULT_SAMPLE;
        }
    }

    public static class TimeMatchValue extends MatchValue {
        final String sample;
        final SimpleDateFormat formatter;

        public TimeMatchValue(String key) {
            formatter = new SimpleDateFormat(key, ULocale.ROOT);
            sample = formatter.format(new Date());
        }

        @Override
        public String getName() {
            return "time/" + formatter.toPattern();
        }

        public static TimeMatchValue of(String key) {
            return new TimeMatchValue(key);
        }

        @Override
        public boolean is(String item) {
            try {
                formatter.parse(item);
                return true;
            } catch (ParseException e) {
                return false;
            }
        }

        @Override
        public String getSample() {
            return sample;
        }
    }

    public static class UnicodeSpanMatchValue extends MatchValue {
        final String sample;
        final UnicodeSet uset;

        public UnicodeSpanMatchValue(String key) {
            UnicodeSet temp;
            try {
                temp = new UnicodeSet(key);
            } catch (Exception e) {
                temp = UnicodeSet.EMPTY;
                int debug = 0;
            }
            uset = temp.freeze();
            sample = new StringBuilder().appendCodePoint(uset.getRangeStart(0)).toString();
        }

        @Override
        public String getName() {
            return "unicodeset/" + uset;
        }

        public static UnicodeSpanMatchValue of(String key) {
            return new UnicodeSpanMatchValue(key);
        }

        @Override
        public boolean is(String item) {
            return uset.span(item, SpanCondition.CONTAINED) == item.length();
        }

        @Override
        public String getSample() {
            return sample;
        }
    }
}
