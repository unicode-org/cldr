package org.unicode.cldr.test;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.Field;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.ModifiedField;
import org.unicode.cldr.util.personname.PersonNameFormatter.Modifier;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePattern;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.PersonNameFormatter.Usage;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class CheckPlaceHolders extends CheckCLDR {

    private static final Pattern PLACEHOLDER_PATTERN = PatternCache.get("([0-9]|[1-9][0-9]+)");
    private static final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults();
    private static final Joiner JOIN_SPACE = Joiner.on(' ');

    private static final Pattern SKIP_PATH_LIST = Pattern
        .compile("//ldml/characters/(exemplarCharacters|parseLenient).*");

//    private static final LocaleMatchValue LOCALE_MATCH_VALUE = new LocaleMatchValue(ImmutableSet.of(
//        Validity.Status.regular,
//        Validity.Status.special,
//        Validity.Status.unknown)
//        );

    /**
     * Contains all CLDR locales, plus some special cases
     */
    private static final Set<String> CLDR_LOCALES_FOR_NAME_ORDER;
    static {
        Set<String> valid = new HashSet<>();
        valid.addAll(CLDRConfig.getInstance().getCldrFactory().getAvailable());
        valid.add("zxx");
        valid.add("und");
        CLDR_LOCALES_FOR_NAME_ORDER = ImmutableSet.copyOf(valid);
    }

    private static final ImmutableSet<Modifier> SINGLE_CORE = ImmutableSet.of(Modifier.core);
    private static final ImmutableSet<Modifier> SINGLE_PREFIX = ImmutableSet.of(Modifier.prefix);
    private static final ImmutableSet<Modifier> CORE_AND_PREFIX = ImmutableSet.of(Modifier.prefix, Modifier.core);

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (value == null
            || path.endsWith("/alias")
            || SKIP_PATH_LIST.matcher(path).matches()) {
            return this;
        }

        if (path.contains("/personNames")) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            switch(parts.getElement(2)) {
            default:
                break;// skip to rest of handleCheck
            case "initialPattern":
                checkInitialPattern(this, path, value, result);
                break;// skip to rest of handleCheck
            case "foreignSpaceReplacement":
                checkForeignSpaceReplacement(this, value, result);
                return this;
            case "nameOrderLocales":
                checkNameOrder(this, path, value, result);
                return this;
            case "sampleName":
                checkSampleNames(this, parts, value, result);
                return this;
            case "personName":
                checkPersonNamePatterns(this, path, parts, value, result);
                return this;
            }
            // done with person names
            // note: depending on the switch value, may fall through
        }

        checkBasicPlaceholders(value, result);
        checkListPatterns(path, value, result);
        return this;
    }


    /**
     * Verify the that nameOrder items are clean.
     */
    public static void checkNameOrder(CheckAccessor checkAccessor, String path, String value, List<CheckStatus> result) {
        //ldml/personNames/nameOrderLocales[@order="givenFirst"]
        final String localeID = checkAccessor.getLocaleID();
        Set<String> items = new TreeSet<>();
        Set<String> orderErrors = checkForErrorsAndGetLocales(localeID, value, items);
        if (orderErrors != null) {
            result.add(new CheckStatus().setCause(checkAccessor)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.invalidLocale)
                .setMessage("Invalid locales: " + JOIN_SPACE.join(orderErrors)));
            return;
        }
        // Check to see that user's language and und are explicitly mentioned.
        // but only if the value is not inherited.
        String unresolvedValue = checkAccessor.getUnresolvedStringValue(path);
        if (unresolvedValue != null) {
            // And the other value is not inherited.
            String otherPath = path.contains("givenFirst")
                ? path.replace("givenFirst", "surnameFirst")
                    : path.replace("surnameFirst", "givenFirst");
            String otherValue = checkAccessor.getStringValue(otherPath);
            if (otherValue != null) {
                String myLanguage = localeID;
                if (!myLanguage.equals("root")) { // skip root

                    Set<String> items2 = new TreeSet<>();
                    orderErrors = checkForErrorsAndGetLocales(localeID, otherValue, items2); // adds locales from other path. We don't check for errors there.
                    if (!Collections.disjoint(items, items2)) {
                        result.add(new CheckStatus().setCause(checkAccessor)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.invalidLocale)
                            .setMessage("Locale codes can occur only once: " + JOIN_SPACE.join(Sets.intersection(items, items2))));
                    }

                    items.addAll(items2); // get the union for checking below
                    myLanguage = new LocaleIDParser().set(myLanguage).getLanguage();

                    if (!items.contains(myLanguage)) {
                        result.add(new CheckStatus().setCause(checkAccessor)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.missingLanguage)
                            .setMessage("Your locale code (" + myLanguage
                                + ") must be explicitly listed in one of the nameOrderLocales:"
                                + " either in givenFirst or in surnameFirst."));
                    }

                    if (!items.contains("und")) {
                        result.add(new CheckStatus().setCause(checkAccessor)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.missingLanguage)
                            .setMessage("The special code ‘und’ must be explicitly listed in one of the nameOrderLocales: either givenFirst or surnameFirst."));
                    }
                }
            }
        }
    }

    /**
     * Verify the that sampleName items are clean.
     * @param checkAccessor
     */
    public static void checkSampleNames(CheckAccessor checkAccessor, XPathParts pathParts, String value, List<CheckStatus> result) {
        //ldml/personNames/sampleName[@item="informal"]/nameField[@type="surname"]

        // check basic consistency of modifier set
        ModifiedField fieldType = ModifiedField.from(pathParts.getAttributeValue(-1, "type"));
        Field field = fieldType.getField();
        Set<Modifier> modifiers = fieldType.getModifiers();
        Output<String> errorMessage = new Output<>();
        Modifier.getCleanSet(modifiers, errorMessage);
        if (errorMessage.value != null) {
            result.add(new CheckStatus().setCause(checkAccessor)
                .setMainType(CheckStatus.warningType)
                .setSubtype(Subtype.invalidPlaceHolder)
                .setMessage(errorMessage.value));
            return;
        }

        if (value.equals("∅∅∅")) {
            // check for required values

            switch(field) {
            case given:
                // we must have a given
                if (fieldType.getModifiers().isEmpty()) {
                    result.add(new CheckStatus().setCause(checkAccessor)
                        .setMainType(CheckStatus.warningType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Names must have a value for the ‘given‘ field. Mononyms (like ‘Lady Gaga’) use given, not surname"));
                }
                break;
            case surname:
                // can't have surname2 unless we have surname
                final XPathParts thawedPathParts = pathParts.cloneAsThawed();
                String modPath = thawedPathParts.setAttribute(-1, "type", Field.surname2.toString()).toString();
                String surname2Value = checkAccessor.getStringValue(modPath);
                String modPathcore = thawedPathParts.setAttribute(-1, "type", "surname-core").toString();
                String surnameCoreValue = checkAccessor.getStringValue(modPathcore);
                if (surname2Value != null
                    && !surname2Value.equals("∅∅∅")
                    && (surnameCoreValue == null || surnameCoreValue.equals("∅∅∅"))) {
                    result.add(new CheckStatus().setCause(checkAccessor)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Names must have a value for the ‘surname’ field if they have a ‘surname2’ field."));
                }
                break;
            default:
                break;
            }
        } else if (value.equals("zxx")) { // mistaken "we don't use this"
            result.add(new CheckStatus().setCause(checkAccessor)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.invalidPlaceHolder)
                .setMessage("Illegal name field; zxx is only appropriate for NameOrder locales"));
        } else { // real value
            // special checks for prefix/core
            final boolean hasPrefix = modifiers.contains(Modifier.prefix);
            final boolean hasCore = modifiers.contains(Modifier.core);
            if (hasPrefix || hasCore) {
                // We need consistency among the 3 values if we have either prefix or core

                String coreValue = hasCore ? value : modifiedFieldValue(checkAccessor, pathParts, field, modifiers, Modifier.core);
                String prefixValue = hasPrefix ? value : modifiedFieldValue(checkAccessor, pathParts, field, modifiers, Modifier.prefix);
                String plainValue = modifiedFieldValue(checkAccessor, pathParts, field, modifiers, null);

                String errorMessage2 = Modifier.inconsistentPrefixCorePlainValues(prefixValue, coreValue, plainValue);
                if (errorMessage2 != null) {
                    result.add(new CheckStatus().setCause(checkAccessor)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage(errorMessage2));
                }
            }
        }
    }

    static final ImmutableSet<Object> givenFirstSortingLocales = ImmutableSet.of("is", "ta", "si"); // TODO should be data-driven

    /**
     * Verify the that personName patterns are clean.
     * @param path TODO
     */
    public static void checkPersonNamePatterns(CheckAccessor checkAccessor, String path, XPathParts pathParts, String value, List<CheckStatus> result) {
        //ldml/personNames/personName[@order="sorting"][@length="long"][@usage="addressing"][@style="formal"]/namePattern

        // check that the name pattern is valid

        Pair<FormatParameters, NamePattern> pair = null;
        try {
            pair = PersonNameFormatter.fromPathValue(pathParts, value);
        } catch (Exception e) {
            result.add(new CheckStatus().setCause(checkAccessor)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.invalidPlaceHolder)
                .setMessage("Invalid placeholder in value: \"" + value + "\""));
            return; // fatal error, don't bother with others
        }

        final FormatParameters parameterMatcher = pair.getFirst();
        final NamePattern namePattern = pair.getSecond();

        // now check that the namePattern is reasonable

        Multimap<Field, Integer> fieldToPositions = namePattern.getFieldPositions();

        // Check for special cases: https://unicode-org.atlassian.net/browse/CLDR-15782

        boolean usageIsMonogram = parameterMatcher.matches(new FormatParameters(null, null, Usage.monogram, null));

        ModifiedField lastModifiedField = null;
        for (int i = 0; i < namePattern.getElementCount(); ++i) {
            ModifiedField modifiedField = namePattern.getModifiedField(i);
            if (modifiedField == null) { // literal
                String literal = namePattern.getLiteral(i);
                if (literal.contains(".")) {
                    if (lastModifiedField != null) {
                        Set<Modifier> lastModifiers = lastModifiedField.getModifiers();
                        if (lastModifiers.contains(Modifier.initial) && lastModifiers.contains(Modifier.initialCap)) {
                            result.add(new CheckStatus().setCause(checkAccessor)
                                .setMainType(CheckStatus.warningType)
                                .setSubtype(Subtype.namePlaceholderProblem)
                                .setMessage("“.” is strongly discouraged after an -initial or -initialCap placeholder in {" + lastModifiedField + "}"));
                            continue;
                        }
                    }
                    if (usageIsMonogram) {
                        result.add(new CheckStatus().setCause(checkAccessor)
                            .setMainType(CheckStatus.warningType)
                            .setSubtype(Subtype.namePlaceholderProblem)
                            .setMessage("“.” is discouraged when usage=monogram, in " + namePattern));
                    }
                }
            } else {
                lastModifiedField = modifiedField;
                Set<Modifier> modifiers = modifiedField.getModifiers();
                Field field = modifiedField.getField();
                switch (field) {
                case title:
                case credentials:
                case generation:
                    if (usageIsMonogram) {
                        result.add(new CheckStatus().setCause(checkAccessor)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.invalidPlaceHolder)
                            .setMessage("Disallowed when usage=monogram: {" + field + "…}"));
                    }
                    break;
                default:
                    final boolean monogramModifier = modifiers.contains(Modifier.monogram);
                    final boolean allCapsModifier = modifiers.contains(Modifier.allCaps);
                    if (!usageIsMonogram) {
                        if (monogramModifier) {
                            result.add(new CheckStatus().setCause(checkAccessor)
                                .setMainType(CheckStatus.warningType)
                                .setSubtype(Subtype.invalidPlaceHolder)
                                .setMessage("-monogram is strongly discouraged when usage≠monogram, in {" + modifiedField + "}"));
                        }
                    } else if (usageIsMonogram) {
                        if (!monogramModifier) {
                            result.add(new CheckStatus().setCause(checkAccessor)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.invalidPlaceHolder)
                                .setMessage("-monogram is required when usage=monogram, in {" + modifiedField + "}"));
                        } else if (!allCapsModifier) {
                            result.add(new CheckStatus().setCause(checkAccessor)
                                .setMainType(CheckStatus.warningType)
                                .setSubtype(Subtype.invalidPlaceHolder)
                                .setMessage("-allCaps is strongly encouraged with -monogram, in {" + modifiedField + "}"));
                        }
                    }
                }
            }
            lastModifiedField = modifiedField;
        }

        // gather information about the fields
        int firstSurname = Integer.MAX_VALUE;
        int firstGiven = Integer.MAX_VALUE;

        // TODO ALL check for combinations we should enforce; eg, only have given2 if there is a given; only have surname2 if there is a surname; others?

        for (Entry<Field, Collection<Integer>> entry : fieldToPositions.asMap().entrySet()) {

            // If a field occurs twice, probably an error. Could relax this upon feedback

            Collection<Integer> positions = entry.getValue();
            if (positions.size() > 1) {

                // However, do allow prefix&core together

                boolean skip = false;
                if (entry.getKey() == Field.surname) {
                    Iterator<Integer> it = positions.iterator();
                    Set<Modifier> m1 = namePattern.getModifiedField(it.next()).getModifiers();
                    Set<Modifier> m2 = namePattern.getModifiedField(it.next()).getModifiers();
                    skip = m1.contains(Modifier.core) && m2.contains(Modifier.prefix)
                        || m1.contains(Modifier.prefix) && m2.contains(Modifier.core);
                }

                if (!skip) {
                    result.add(new CheckStatus().setCause(checkAccessor)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Duplicate fields: " + entry));
                }
            }

            // gather some info for later

            Integer leastPosition = positions.iterator().next();
            switch (entry.getKey()) {
            case given: case given2:
                firstGiven = Math.min(leastPosition, firstGiven);
                break;
            case surname: case surname2:
                firstSurname = Math.min(leastPosition, firstSurname);
                break;
            default: // ignore
            }
        }

        // the rest of the tests are of the pattern, and only apply when we have both given and surname
        // and not inheriting

        if (firstGiven < Integer.MAX_VALUE && firstSurname < Integer.MAX_VALUE
            && checkAccessor.getUnresolvedStringValue(path) != null) {

            Order orderRaw = parameterMatcher.getOrder();
            Set<Order> order = orderRaw == null ? Order.ALL : ImmutableSet.of(orderRaw);
            // TODO, fix to avoid set (a holdover from using PatternMatcher)

            // Handle 'sorting' value. Will usually be compatible with surnameFirst in foundOrder, except for known exceptions

            if (order.contains(Order.sorting)) {
                EnumSet<Order> temp = EnumSet.noneOf(Order.class);
                temp.addAll(order);
                temp.remove(Order.sorting);
                if (givenFirstSortingLocales.contains(checkAccessor.getLocaleID())) { // TODO Mark cover contains-by-inheritance also
                    temp.add(Order.givenFirst);
                } else {
                    temp.add(Order.surnameFirst);
                }
                order = temp;
            }

            if (order.isEmpty()) {
                order = Order.ALL;
            }

            // check that we don't have a difference in the order AND there is a surname or surname2
            // that is, it is ok to coalesce patterns of different orders where the order doesn't make a difference

            { // TODO: clean up to avoid block

                if(order.contains(Order.givenFirst)
                    && order.contains(Order.surnameFirst)
                    ) {
                    result.add(new CheckStatus().setCause(checkAccessor)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Conflicting Order values: " + order));
                }

                // now check order in pattern is consistent with Order

                Order foundOrder = firstGiven < firstSurname ? Order.givenFirst : Order.surnameFirst;
                final Order first = order.iterator().next();

                if (first != foundOrder) {

//                    if (first == Order.givenFirst && !"en".equals(checkAccessor.getLocaleID())) { // TODO Mark Drop HACK once root is ok
//                        return;
//                    }

                    result.add(new CheckStatus().setCause(checkAccessor)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Pattern order {0} is inconsistent with code order {1}", foundOrder, first));
                }
            }
        }
    }

    /**
     * Check that {\d+} placeholders are ok; no unterminated, only digits
     */
    private void checkBasicPlaceholders(String value, List<CheckStatus> result) {
        int startPlaceHolder = 0;
        int endPlaceHolder;
        while (startPlaceHolder != -1 && startPlaceHolder < value.length()) {
            startPlaceHolder = value.indexOf('{', startPlaceHolder + 1);
            if (startPlaceHolder != -1) {
                endPlaceHolder = value.indexOf('}', startPlaceHolder + 1);
                if (endPlaceHolder == -1) {
                    result.add(new CheckStatus().setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Invalid placeholder (missing terminator) in value \"" + value + "\""));
                } else {
                    String placeHolderString = value.substring(startPlaceHolder + 1, endPlaceHolder);
                    Matcher matcher = PLACEHOLDER_PATTERN.matcher(placeHolderString);
                    if (!matcher.matches()) {
                        result.add(new CheckStatus().setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.invalidPlaceHolder)
                            .setMessage("Invalid placeholder (contents \"" + placeHolderString + "\") in value \"" + value + "\""));
                    }
                    startPlaceHolder = endPlaceHolder;
                }
            }
        }
    }

    /**
     * Check that list patterns are "ordered" so that they only compose from the right.
     */

    private void checkListPatterns(String path, String value, List<CheckStatus> result) {
        // eg
        //ldml/listPatterns/listPattern/listPatternPart[@type="start"]
        //ldml/listPatterns/listPattern[@type="standard-short"]/listPatternPart[@type="2"]
        if (path.startsWith("//ldml/listPatterns/listPattern")) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            // check order, {0} must be before {1}

            switch(parts.getAttributeValue(-1, "type")) {
            case "start":
                checkNothingAfter1(value, result);
                break;
            case "middle":
                checkNothingBefore0(value, result);
                checkNothingAfter1(value, result);
                break;
            case "end":
                checkNothingBefore0(value, result);
                break;
            case "2": {
                int pos1 = value.indexOf("{0}");
                int pos2 = value.indexOf("{1}");
                if (pos1 > pos2) {
                    result.add(new CheckStatus().setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Invalid list pattern «" + value + "»: the placeholder {0} must be before {1}."));
                }}
            break;
            case "3": {
                int pos1 = value.indexOf("{0}");
                int pos2 = value.indexOf("{1}");
                int pos3 = value.indexOf("{2}");
                if (pos1 > pos2 || pos2 > pos3) {
                    result.add(new CheckStatus().setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Invalid list pattern «" + value + "»: the placeholders {0}, {1}, {2} must appear in that order."));
                }}
            break;
            }
        }
    }

    /**
     * Check that both patterns don't have the same literals.
     */
    public static void checkInitialPattern(CheckAccessor checkAccessor, String path, String value, List<CheckStatus> result) {
        if (path.contains("initialSequence")) {
            String valueLiterals = value.replace("{0}", "").replace("{1}", "");
            if (!valueLiterals.isBlank()) {
                String otherPath = path.replace("initialSequence", "initial");
                String otherValue = checkAccessor.getStringValue(otherPath);
                if (otherValue != null) {
                    String literals = otherValue.replace("{0}", "");
                    if (!literals.isBlank() && value.contains(literals)) {
                        result.add(new CheckStatus().setCause(checkAccessor)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.namePlaceholderProblem)
                            .setMessage("The initialSequence pattern must not contain initial pattern literals: «" + literals + "»"));
                        return;
                    }
                }
                result.add(new CheckStatus().setCause(checkAccessor)
                    .setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.namePlaceholderProblem)
                    .setMessage("Non-space characters are discouraged in the initialSequence pattern: «" + valueLiterals.replace(" ", "") + "»"));
            }
        }
        // no current check for the type="initial"
    }

    static final UnicodeSet allowedForeignSpaceReplacements = new UnicodeSet("[[:whitespace:][:punctuation:]]");

    /**
     * Check that the value is limited to punctuation or space, or inherits
     */
    public static void checkForeignSpaceReplacement(CheckAccessor checkAccessor, String value, List<CheckStatus> result) {
        if (!allowedForeignSpaceReplacements.containsAll(value) && !value.equals("↑↑↑")) {
            result.add(new CheckStatus().setCause(checkAccessor)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.invalidLocale)
                .setMessage("Invalid choice, must be punctuation or a space: «" + value + "»"));
        }
    }


    /**
     * Gets a string value for a modified path
     */
    private static String modifiedFieldValue(CheckAccessor checkAccessor, XPathParts parts, Field field, Set<Modifier> modifiers, Modifier toAdd) {
        Set<Modifier> adjustedModifiers = Sets.difference(modifiers, CORE_AND_PREFIX);
        if (toAdd != null) {
            switch (toAdd) {
            case core:
                adjustedModifiers = Sets.union(adjustedModifiers, SINGLE_CORE);
                break;
            case prefix:
                adjustedModifiers = Sets.union(adjustedModifiers, SINGLE_PREFIX);
                break;
            default:
                break;
            }
        }
        String modPath  = parts.cloneAsThawed().setAttribute(-1, "type", new ModifiedField(field, adjustedModifiers).toString()).toString();
        String value = checkAccessor.getStringValue(modPath);
        return "∅∅∅".equals(value) ? null : value;
    }

    public static Set<String> checkForErrorsAndGetLocales(String locale, String value, Set<String> items) {
        if (value.isEmpty()) {
            return null;
        }
        Set<String> orderErrors = null;
        for (String item : SPLIT_SPACE.split(value)) {
            boolean mv = (item.equals(locale))
                || CLDR_LOCALES_FOR_NAME_ORDER.contains(item);
            if (!mv) {
                if (orderErrors == null) {
                    orderErrors = new LinkedHashSet<>();
                }
                orderErrors.add(item);
            } else {
                items.add(item);
            }
        }
        return orderErrors;
    }

    private void checkNothingAfter1(String value, List<CheckStatus> result) {
        if (!value.endsWith("{1}")) {
            result.add(new CheckStatus().setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.invalidPlaceHolder)
                .setMessage("Invalid list pattern «" + value + "», no text can come after {1}."));
        }

    }

    private void checkNothingBefore0(String value, List<CheckStatus> result) {
        if (!value.startsWith("{0}")) {
            result.add(new CheckStatus().setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.invalidPlaceHolder)
                .setMessage("Invalid list pattern «" + value + "», no text can come before {0}."));
        }
    }
}
