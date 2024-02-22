package org.unicode.cldr.util;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;

public class LocaleValidator {
    static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();

    static final Validity VALIDITY = Validity.getInstance();
    static final Set<LstrType> FIELD_ALLOWS_EMPTY = Set.of(LstrType.script, LstrType.region);
    // Map<LstrType, Map<String, Map<LstrField, String>>>
    static final Map<String, Validity.Status> VALID_VARIANTS =
            ImmutableMap.copyOf(
                    StandardCodes.getEnumLstreg().get(LstrType.variant).entrySet().stream()
                            .collect(
                                    Collectors.toMap(
                                            x -> x.getKey(),
                                            y ->
                                                    y.getValue().get(LstrField.Deprecated) == null
                                                            ? Validity.Status.regular
                                                            : Validity.Status.deprecated)));

    private static final Map<String, Validity.Status> KR_REORDER =
            SupplementalDataInfo.getInstance().getBcp47Keys().get("kr").stream()
                    .filter(x -> !x.equals("REORDER_CODE"))
                    .collect(
                            Collectors.toMap(
                                    Function.identity(),
                                    y -> {
                                        String temp =
                                                SupplementalDataInfo.getInstance()
                                                        .getBcp47Deprecated()
                                                        .get(Row.of("kr", y));
                                        return "false".equals(temp)
                                                ? Validity.Status.regular
                                                : Validity.Status.deprecated;
                                    }));
    private static final Map<String, Validity.Status> LOWERCASE_SCRIPT =
            VALIDITY.getCodeToStatus(LstrType.script).entrySet().stream()
                    .collect(
                            Collectors.toMap(
                                    x -> UCharacter.toLowerCase(x.getKey()), x -> x.getValue()));

    private static final Map<String, Validity.Status> LOWERCASE_REGION =
            VALIDITY.getCodeToStatus(LstrType.script).entrySet().stream()
                    .collect(
                            Collectors.toMap(
                                    x -> UCharacter.toLowerCase(x.getKey()), x -> x.getValue()));

    public static class AllowedMatch {
        final Pattern key;
        final Pattern value;
        final Validity.Status status;

        public AllowedMatch(String code) {
            this(code, null, null);
        }

        public AllowedMatch(String code, String value) {
            this(code, value, null);
        }

        public AllowedMatch(String code, String value, Validity.Status status) {
            this.key = code == null ? null : Pattern.compile(code);
            this.value = value == null ? null : Pattern.compile(value);
            this.status = status;
        }

        public boolean matches(String key0, String value0, Validity.Status status) {
            return (key == null || key.matcher(key0).matches())
                    && (value == null
                            || value.matcher(value0).matches()
                                    && (status == null || status == status));
        }

        @Override
        public String toString() {
            return key + "→" + value;
        }
    }

    public static class AllowedValid {

        private final Set<Validity.Status> allowedStatus; // allowed without exception
        private final Multimap<LstrType, AllowedMatch> allowedExceptions;

        boolean isAllowed(Validity.Status status) {
            return allowedStatus.contains(status);
        }

        /** Only called if isAllowed is not true */
        boolean isAllowed(LstrType lstrType, String key, String value, Validity.Status status) {
            Collection<AllowedMatch> allowedMatches = allowedExceptions.get(lstrType);
            if (allowedMatches == null) {
                return false;
            }
            for (AllowedMatch allowedMatch : allowedMatches) {
                if (allowedMatch.matches(key, value, status)) return true;
            }
            return false;
        }

        public AllowedValid(Set<Validity.Status> allowedStatus, Object... allowedExceptions) {
            this.allowedStatus =
                    allowedStatus == null
                            ? Set.of(Validity.Status.regular)
                            : Set.copyOf(allowedStatus);
            Multimap<LstrType, AllowedMatch> allowed = HashMultimap.create();
            if (allowedExceptions != null) {
                for (int i = 0; i < allowedExceptions.length; i += 2) {
                    allowed.put(
                            (LstrType) allowedExceptions[i],
                            (AllowedMatch) allowedExceptions[i + 1]);
                }
            }
            this.allowedExceptions = ImmutableMultimap.copyOf(allowed);
        }

        @Override
        public String toString() {
            return allowedStatus + " " + allowedExceptions;
        }
    }

    /**
     * @return true iff the component validates
     */
    public static boolean isValid(
            LanguageTagParser ltp, LocaleValidator.AllowedValid allowed, Set<String> errors) {
        if (errors != null) {
            errors.clear();
        }
        if (allowed == null) {
            allowed = new AllowedValid(null, null);
        }
        if (ltp.isLegacy() && allowed.isAllowed(Validity.Status.deprecated)) {
            return true; // don't need further checking, since we already did so when parsing
        }
        if (Validation.abort
                == validates(LstrType.language, ltp.getLanguage(), null, allowed, errors)) {
            return false;
        }
        if (Validation.abort
                == validates(LstrType.script, ltp.getScript(), null, allowed, errors)) {
            return false;
        }
        if (Validation.abort
                == validates(LstrType.region, ltp.getRegion(), null, allowed, errors)) {
            return false;
        }
        for (String variant : ltp.getVariants()) {
            if (Validation.abort == validates(LstrType.variant, variant, null, allowed, errors)) {
                return false;
            }
        }
        for (Entry<String, List<String>> entry : ltp.getLocaleExtensionsDetailed().entrySet()) {
            if (Validation.abort
                    == validates(
                            LstrType.extension,
                            entry.getKey(),
                            entry.getValue(),
                            allowed,
                            errors)) {
                return false;
            }
        }
        for (Entry<String, List<String>> entry : ltp.getExtensionsDetailed().entrySet()) {
            if (Validation.abort
                    == validates(
                            LstrType.extension,
                            entry.getKey(),
                            entry.getValue(),
                            allowed,
                            errors)) {
                return false;
            }
        }
        return errors.isEmpty(); // if we didn't abort, then we recorded errors in the set
    }

    private enum Validation {
        abort,
        keepOn
    }
    /**
     * Returns true if it doesn't validate and errors == null (allows for fast rejection)
     *
     * @param type
     * @param values TODO
     * @param subtag
     * @return true if the subtag is empty, or it is an allowed status
     */
    private static LocaleValidator.Validation validates(
            LstrType type,
            String field,
            List<String> values,
            LocaleValidator.AllowedValid allowed,
            Set<String> errors) {
        Validity.Status status;
        switch (type) {
            case language:
            case script:
            case region:
                status = VALIDITY.getCodeToStatus(type).get(field);
                if (status == null) {
                    status = Validity.Status.invalid;
                }
                if (allowed.isAllowed(status)
                        || allowed.isAllowed(type, field, null, null)
                        || field.length() == 0) {
                    return Validation.keepOn;
                }
                break;
            case variant:
                status = VALID_VARIANTS.get(field);
                if (status == null) {
                    status = Validity.Status.invalid;
                }
                if (allowed.isAllowed(status)) {
                    return Validation.keepOn;
                }
                break;
            case extension:
                switch (field.length()) {
                    case 1:
                        switch (field) {
                            case "t": // value is an LSRV
                                String lsvr = Joiner.on("-").join(values);
                                status = Validity.Status.invalid;
                                try {
                                    LanguageTagParser ltp2 = new LanguageTagParser().set(lsvr);
                                    if (isValid(ltp2, allowed, errors)) {
                                        return Validation.keepOn;
                                    }
                                } catch (Exception e) {
                                    if (errors != null) {
                                        errors.add(
                                                String.format(
                                                        "Disallowed %s=%s, tlang=%s, status=%s",
                                                        type, lsvr, field, status));
                                        return Validation.keepOn;
                                    }
                                }
                                return Validation.abort;
                            case "x": // private use, everything is valid
                                status = Validity.Status.private_use;
                                break;
                            case "u": // value is an attribute, none currently valid
                                status = Validity.Status.invalid;
                                break;
                            default:
                                status = Validity.Status.invalid;
                                break;
                        }
                        break;
                    case 2:
                        // field is a tkey or a ukey, based on last char
                        String tOrU = field.charAt(1) < 'A' ? "t" : "u";
                        Set<String> subtypes = SDI.getBcp47Keys().get(field);
                        if (subtypes == null) {
                            status = Validity.Status.invalid;
                        } else {
                            String subtype = Joiner.on("-").join(values);
                            final Map<R2<String, String>, String> bcp47Deprecated =
                                    SDI.getBcp47Deprecated();
                            if ("true".equals(bcp47Deprecated.get(Row.of(field, subtype)))) {
                                status = Validity.Status.deprecated;
                            } else {
                                if (subtypes.contains(subtype)) {
                                    status = Validity.Status.regular;
                                } else {
                                    boolean mapUnknownToRegular = false;
                                    fieldSwitch:
                                    switch (field) {
                                        case "x0":
                                            status = Validity.Status.deprecated;
                                            break;
                                        case "dx":
                                            status =
                                                    checkSpecials(
                                                            type,
                                                            field,
                                                            values,
                                                            allowed,
                                                            LOWERCASE_SCRIPT);
                                            break;
                                        case "kr":
                                            status =
                                                    checkSpecials(
                                                            type,
                                                            field,
                                                            values,
                                                            allowed,
                                                            LOWERCASE_SCRIPT,
                                                            KR_REORDER);
                                            break;
                                        case "rg":
                                            mapUnknownToRegular = true;
                                        case "sd":
                                            status =
                                                    checkSpecials(
                                                            type,
                                                            field,
                                                            values,
                                                            allowed,
                                                            VALIDITY.getCodeToStatus(
                                                                    LstrType.subdivision));
                                            break;
                                        case "vt":
                                            status = Validity.Status.invalid;
                                            if (values.isEmpty()) {
                                                break fieldSwitch;
                                            }
                                            for (String value : values) {
                                                try {
                                                    int intValue = Integer.parseInt(value, 16);
                                                    if (intValue < 0
                                                            || intValue > 0x10FFFF
                                                            || (Character.MIN_SURROGATE <= intValue
                                                                    && intValue
                                                                            <= Character
                                                                                    .MAX_SURROGATE)) {
                                                        break fieldSwitch;
                                                    }
                                                } catch (NumberFormatException e) {
                                                    break fieldSwitch;
                                                }
                                            }
                                            status = Validity.Status.regular;
                                            break;
                                        default:
                                            status = Validity.Status.invalid;
                                            break;
                                    }
                                    if (mapUnknownToRegular == true
                                            && status == Validity.Status.unknown) {
                                        status = Validity.Status.regular;
                                    }
                                }
                            }
                            if (allowed.isAllowed(status)
                                    || allowed.isAllowed(
                                            LstrType.extension, field, subtype, status)) {
                                return Validation.keepOn;
                            } else if (errors == null) {
                                return Validation.abort;
                            }
                            errors.add(
                                    String.format(
                                            "Disallowed %s=%s=%s, status=%s",
                                            type, field, subtype, status));
                            return Validation.keepOn;
                        }
                        break;
                    default:
                        status = Validity.Status.invalid;
                        break;
                }
                break;
            default:
                status = null;
                break;
        }
        if (errors == null) {
            return Validation.abort;
        }
        errors.add(String.format("Disallowed %s=%s, status=%s", type, field, status));
        return Validation.keepOn;
    }

    public static Validity.Status checkSpecials(
            LstrType type,
            String field,
            List<String> values,
            LocaleValidator.AllowedValid allowed,
            Map<String, Validity.Status>... validityMaps) {
        if (values.size() > 1
                && (field.equals("sd") || field.equals("rg"))) { // TODO generalize this
            return Validity.Status.invalid;
        }
        Validity.Status best = null;
        for (String value : values) {
            Validity.Status status = null;
            for (Map<String, Validity.Status> validityMap : validityMaps) {
                status = validityMap.get(value);
                if (status != null) {
                    break;
                }
            }
            if (status == null) {
                return Validity.Status.invalid;
            }
            if (allowed.isAllowed(status) || allowed.isAllowed(type, field, value, status)) {
                if (best == null) {
                    best = status;
                }
            } else {
                return status;
            }
        }
        return best == null ? Validity.Status.invalid : best;
    }

    public Validity.Status checkRegion(
            LstrType type,
            String field,
            List<String> values,
            LocaleValidator.AllowedValid allowed) {
        Validity.Status best = null;
        for (String value : values) {
            String value2 = UCharacter.toTitleCase(value, null);
            Validity.Status status = VALIDITY.getCodeToStatus(LstrType.script).get(value2);
            if (status == null) {
                return Validity.Status.invalid;
            }
            if (allowed.isAllowed(status) || allowed.isAllowed(type, field, value, null)) {
                if (best == null) {
                    best = status;
                }
            } else {
                return status;
            }
        }
        return best == null ? Validity.Status.invalid : best;
    }
}
