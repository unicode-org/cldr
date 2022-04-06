package org.unicode.cldr.test;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.MatchValue.LocaleMatchValue;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.Field;
import org.unicode.cldr.util.personname.PersonNameFormatter.ModifiedField;
import org.unicode.cldr.util.personname.PersonNameFormatter.Modifier;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePattern;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.PersonNameFormatter.ParameterMatcher;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class CheckPlaceHolders extends CheckCLDR {

    private static final Pattern PLACEHOLDER_PATTERN = PatternCache.get("([0-9]|[1-9][0-9]+)");
    private static final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults();
    private static final Joiner JOIN_SPACE = Joiner.on(' ');

    private static final Pattern SKIP_PATH_LIST = Pattern
        .compile("//ldml/characters/(exemplarCharacters|parseLenient).*");

    private static final LocaleMatchValue LOCALE_MATCH_VALUE = new LocaleMatchValue(ImmutableSet.of(
        Validity.Status.regular,
        Validity.Status.special,
        Validity.Status.unknown)
        );

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
            //ldml/personNames/initialPattern[@type="initial"]
            default:
                // skip to rest of handleCheck
                break;

            case "nameOrderLocales":
                //ldml/personNames/nameOrderLocales[@order="givenFirst"]
                Set<String> orderErrors = null;
                for (String item : SPLIT_SPACE.split(value)) {
                    boolean mv = LOCALE_MATCH_VALUE.is(item);
                    if (!mv) {
                        if (orderErrors == null) {
                            orderErrors = new LinkedHashSet<>();
                        }
                        orderErrors.add(item);
                    }
                }
                if (orderErrors != null) {
                    result.add(new CheckStatus().setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Invalid locales: \"" + JOIN_SPACE.join(orderErrors) + "\""));
                }
                return this;

            case "sampleName":
                //ldml/personNames/sampleName[@item="informal"]/nameField[@type="surname"]
                if (value.equals("∅∅∅")) {
                    // check for required values

                    ModifiedField fieldType = ModifiedField.from(parts.getAttributeValue(-1, "type"));
                    Field field = fieldType.getField();

                    switch(field) {
                    case given:
                        // we must have a given
                        if (fieldType.getModifiers().isEmpty()) {
                            result.add(new CheckStatus().setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.invalidPlaceHolder)
                                .setMessage("Names must have a value for the ‘given‘ field. Mononyms (like ‘Lady Gaga’) use given, not surname"));
                        }
                        break;
                    case surname:
                        // can't have surname2 unless we have surname
                        String modPath = parts.cloneAsThawed().setAttribute(-1, "type", Field.surname2.toString()).toString();
                        String surname2Value = getCldrFileToCheck().getStringValue(modPath);
                        if (!surname2Value.equals("∅∅∅")) {
                            result.add(new CheckStatus().setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.invalidPlaceHolder)
                                .setMessage("Names must have a value for the ‘surname’ field if they have a ‘surname2’ field."));
                        }
                        break;
                    default:
                        break;
                    }
                }

                return this;
            case "personName":
                //ldml/personNames/personName[@length="long"][@usage="addressing"][@style="formal"][@order="sorting"]/namePattern

                // check that the name pattern is valid

                Pair<ParameterMatcher, NamePattern> pair = null;
                try {
                    pair = PersonNameFormatter.fromPathValue(parts, value);
                } catch (Exception e) {
                    result.add(new CheckStatus().setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Invalid placeholder in value: \"" + value + "\""));
                    return this; // fatal error, don't bother with others
                }

                final ParameterMatcher parameterMatcher = pair.getFirst();
                final NamePattern namePattern = pair.getSecond();

                // now check that the namePattern is reasonable

                Multimap<Field, Integer> fieldToPositions = namePattern.getFieldPositions();

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
                            result.add(new CheckStatus().setCause(this)
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

                if (firstGiven < Integer.MAX_VALUE && firstSurname < Integer.MAX_VALUE) {

                    Set<Order> order = parameterMatcher.getOrder();


                    // Handle 'sorting' value. Will usually be compatible with surnameFirst in foundOrder, except for known exceptions

                    ImmutableSet<Object> givenFirstSortingLocales = ImmutableSet.of("is"); // will be static
                    if (order.contains(Order.sorting)) {
                        EnumSet<Order> temp = EnumSet.noneOf(Order.class);
                        temp.addAll(order);
                        temp.remove(Order.sorting);
                        if (givenFirstSortingLocales.contains(getCldrFileToCheck().getLocaleID())) { // TODO Mark cover contains-by-inheritance also
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

                    if (true) { // TODO: clean up to avoid block

                        if(order.contains(Order.givenFirst)
                            && order.contains(Order.surnameFirst)
                            ) {
                            result.add(new CheckStatus().setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.invalidPlaceHolder)
                                .setMessage("Conflicting Order values: " + order));
                        }

                        // now check order in pattern is consistent with Order

                        Order foundOrder = firstGiven < firstSurname ? Order.givenFirst : Order.surnameFirst;
                        final Order first = order.iterator().next();

                        if (first != foundOrder) {

                            if (first == Order.givenFirst && !"en".equals(getCldrFileToCheck().getLocaleID())) { // TODO Mark Drop HACK once root is ok
                                return this;
                            }

                            result.add(new CheckStatus().setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.invalidPlaceHolder)
                                .setMessage("Pattern order {0} is inconsistent with code order {1}", foundOrder, first));
                        }
                    }
                }
                return this;
            }
            // done with person names
        }

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
        return this;
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
