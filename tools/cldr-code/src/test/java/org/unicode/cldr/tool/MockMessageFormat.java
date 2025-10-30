package org.unicode.cldr.tool;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.tool.MockMessageFormat.Expression.Type;
import org.unicode.cldr.util.RegexUtilities;

/**
 * A mock implementation of a possible internal organization for MF2. Each function is represented
 * by an MfFunction. That factory can take a literal, input value or MfResolvedVariable, plus
 * options, and produce a MfResolvedVariable. For example, a NumberFactory represents the :number
 * function. It can take an input value (say a Double) and some options, and produce a
 * NumberVariable. <br>
 * A function factory may produce a different type. For example, a StringFunction (aka :string)
 * could take a NumberVariable, and produce a StringVariable. <br>
 * This is a mockup, and no attempt has been made to produce optimal code, nor is everything cleanly
 * encapsulated with getters/setters, nor marked with private/public as would be needed for a real
 * API, nor with good error checking.
 */
public class MockMessageFormat {
    static final Splitter SPACE_SPLITTER = Splitter.on(' ').omitEmptyStrings().trimResults();
    static final Splitter HYPHEN_SPLITTER = Splitter.on('-').omitEmptyStrings().trimResults();
    static final boolean DEBUG = false;

    /**
     * A factory that represents a particular function. It is used to create a FunctionVariable from
     * either an input, or another variable (
     */
    public interface MfFunction {

        public String getName();

        /** Used in checking syntax */
        public boolean canSelect();

        /** Used in checking syntax */
        public boolean canFormat();

        public MfResolvedVariable from(Expression expression, MfContext mfContext);
    }

    public enum MatchComparison {
        /**
         * special value: if one key in a variant fails to match, the variant is never considered
         */
        FAIL,
        /** worse than best variant so far */
        WORSE,
        /** same as best variant so far */
        SAME,
        /** better than as best variant so far */
        BETTER,
        /** special value: if all keys in a variant are the best possible, we don't look further */
        BEST
    }

    /**
     * An immutable variable containing information that results from applying a FunctionFactory
     * such as :number or :date. The FunctionVariable is specific to FunctionFactory.
     */
    public abstract static class MfResolvedVariable {
        private OptionsMap options;
        // The subclasses will have a value as well

        /** Return the stored options (immutable) */
        public OptionsMap getOptions() {
            return options;
        }

        /** Set the options. Only usable in the subclasses */
        protected void setOptions(OptionsMap options) {
            this.options = options;
        }

        /**
         * Matches this value against the result produced by its FunctionFactory from the literal
         * matchKey. A match value of zero is "no match"; the maximum int value is an exact match;
         * anything between implementation defined
         */
        public static final double NO_MATCH = -1d;

        public static final double EXACT_MATCH = 0d;

        public static final double FALLBACK = Double.POSITIVE_INFINITY;

        /**
         * Returns the match difference. <br>
         * 0.0 is an exact match <br>
         * -1 is a complete failure <br>
         * Anything positive is a "distance" from being exact. The meaning of that distance is up to
         * the MfFunction
         *
         * @param bestMatchKey
         */
        public abstract MatchComparison match(
                MfContext contact, String matchKey, String bestMatchKey);

        public abstract String format(MfContext contact);

        // abstract Parts formatToParts(); // not necessary for mock

        @Override
        /** Provides a debug form of the internal structure */
        public String toString() {
            return options.toString();
        }
    }

    // Once built, these are invariant
    public final Map<String, Expression> variables = new LinkedHashMap<>();
    public final List<Expression> selectors = new ArrayList<>(0);
    public final Variants variants = new Variants();

    public Expression addVariable(String variableId, Expression expression) {
        Expression old = variables.put(variableId, expression);
        if (old != null) {
            throw new IllegalArgumentException(
                    "Cannot reset variables once created: " + variableId);
        }
        return expression;
    }

    public Expression addSelector(Expression selectorVariable) {
        selectors.add(selectorVariable);
        return selectorVariable;
    }

    public int selectorSize() {
        return selectors.size();
    }

    public Expression getExpressionFromVariableId(String variableId) {
        return variables.get(variableId);
    }

    class Message {
        // An object is either a String or an Expression
        // The only restriction is that a String can't be followed by another String
        List<Object> contents = new ArrayList<>();

        public Message(String messageString) {
            Matcher matcher = EXPRESSION_PATTERN.matcher(messageString);
            int position = 0;
            while (matcher.find()) {
                if (matcher.start() > position) {
                    contents.add(messageString.substring(position, matcher.start()));
                }
                Expression expression = getInternalExpression(matcher);
                contents.add(expression);
                position = matcher.end();
            }
            String fail = RegexUtilities.showMismatch(matcher, messageString);
            if (position < messageString.length()) {
                contents.add(messageString.substring(position, messageString.length()));
            }
        }

        /** Format a variant message, once it has been chosen. */
        String format(MfContext context) {
            StringBuilder result = new StringBuilder();
            for (Object piece : contents) {
                if (piece instanceof String) {
                    result.append(piece.toString());
                } else {
                    MfResolvedVariable fv = ((Expression) piece).resolve(context);
                    result.append(fv.format(context));
                }
            }
            return result.toString();
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            for (Object piece : contents) {
                if (piece instanceof String) {
                    result.append(piece.toString());
                } else {
                    Expression expression = (Expression) piece;
                    result.append(expression);
                }
            }
            return result.toString();
        }
    }

    Message getMessage(String messageText) {
        return new Message(messageText);
    }

    /** A container for an options map. <b> Could have enum keys instead of Strings. */
    static class OptionsMap {
        final Map<String, Object> map;

        static final OptionsMap EMPTY = new OptionsMap(ImmutableMap.of());

        static class OptionBuilder {
            Map<String, Object> map = new LinkedHashMap<>();

            OptionBuilder put(String key, Object value) {
                map.put(key, value);
                return this;
            }

            OptionsMap done() {
                return new OptionsMap(map);
            }
        }

        private OptionsMap(Map<String, Object> rawMap) {
            map = ImmutableMap.copyOf(rawMap);
        }

        public Set<Entry<String, Object>> entrySet() {
            return map.entrySet();
        }

        public Set<String> keySet() {
            return map.keySet();
        }

        public Object get(String option) {
            return map.get(option);
        }

        public OptionsMap merge(OptionsMap options) {
            Map<String, Object> temp = new LinkedHashMap<>();
            temp.putAll(options.map);
            return new OptionsMap(temp);
        }

        static OptionBuilder start() {
            return new OptionBuilder();
        }

        static OptionBuilder put(String key, Object value) {
            return new OptionBuilder().put(key, value);
        }

        @Override
        public String toString() {
            return map.toString();
        }

        public static OptionsMap make(int firstPair, List<String> pairs) {
            OptionBuilder builder = OptionsMap.start();
            for (int i = 3; i < pairs.size(); ++i) {
                List<String> optionParts = Splitter.on('=').splitToList(pairs.get(i));
                builder.put(optionParts.get(0), optionParts.get(1));
            }
            return builder.done();
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public static OptionsMap make(String pairs) {
            if (pairs == null || pairs.isBlank()) {
                return EMPTY;
            }
            OptionBuilder builder = OptionsMap.start();
            SPACE_SPLITTER
                    .split(pairs)
                    .forEach(
                            x -> {
                                Iterator<String> it = Splitter.on('=').split(x).iterator();
                                builder.put(it.next(), it.next());
                            });
            return builder.done();
        }
    }

    /** Represents a mapping of key-lists to variants */
    public static class Variants {
        private Map<List<String>, Message> map = new LinkedHashMap<>();

        public Variants add(List<String> keys, Message message) {
            Message old = map.put(keys, message);
            if (old != null) {
                throw new IllegalArgumentException(
                        String.format("Duplicate variant keys %s %s %s", keys, old, message));
            }
            return this;
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }

    static final String allButSpaceAndCloseCurly = "[^\\}\\s]*";
    static final String allButCurly = "[^\\}]*";
    static final String EXPRESSION =
            // spotless:off
        "\\{(" + allButSpaceAndCloseCurly  + ")" // operand
        + "(?:"
          + "\\s+(:" + allButSpaceAndCloseCurly + ")" // function
          + "\\s*"
          + "(" + allButCurly + ")" // options
        + ")?"
        + "\\s*}";
    // spotless:on

    static final Pattern EXPRESSION_PATTERN = Pattern.compile(EXPRESSION);

    static final String EXPRESSION_TRIMMING = "\\s*" + EXPRESSION + "\\s*";
    static final Pattern EXPRESSION_TRIMMING_PATTERN = Pattern.compile(EXPRESSION_TRIMMING);
    static final Pattern INPUT_PATTERN = Pattern.compile("\\.input" + EXPRESSION_TRIMMING);
    static final Pattern LOCAL_PATTERN =
            Pattern.compile("\\.local\\s+(\\$\\S*)\\s*=" + EXPRESSION_TRIMMING);
    static final Pattern VARIANT_PATTERN = Pattern.compile("(.*?)\\{\\{(.*)");

    /**
     * very simple; depends on the message being in 'normal form and not too complicated
     *
     * @return
     */
    Object dumbParseInput(String line) {
        // eg .input {$var :number maxFractionDigits=2 minFractionDigits=1}
        if (line.startsWith(".input")) {
            return handleInput(line);
        } else if (line.startsWith(".local")) {
            return handleLocal(line);
        } else if (line.startsWith(".match")) {
            return handleMatch(line);
        } else { // variant: keys & message
            return handleVariant(line);
        }
    }

    private Object handleVariant(String line) {
        Matcher matcher = VARIANT_PATTERN.matcher(line); // doesn't catch final }}
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Malformed line: " + RegexUtilities.showMismatch(VARIANT_PATTERN, line));
        }
        String keys = matcher.group(1);
        List<String> keyList = SPACE_SPLITTER.splitToList(keys);
        if (keyList.size() != selectorSize()) {
            throw new IllegalArgumentException(
                    String.format(
                            "The number of keys (%s:%s) must be identical to the number of selectors (%s:%s): %s",
                            keyList.size(), keyList, selectors.size(), selectors, line));
        }
        String messageString = matcher.group(2).trim();
        if (messageString.endsWith("}}")) {
            messageString = messageString.substring(0, messageString.length() - 2);
        } else {
            throw new IllegalArgumentException("Malformed line, must end with }}: " + line);
        }
        Message message = new Message(messageString);
        variants.add(keyList, message);
        return message;
    }

    static class Expression {
        Type type;
        String operandId;
        MfFunction functionFactory;
        OptionsMap map;

        @Override
        public String toString() {
            return String.format(
                    "[type: %s, operandId: %s%s%s]",
                    type,
                    operandId,
                    functionFactory == null ? "" : ", function: " + functionFactory.getName(),
                    map.isEmpty() ? "" : ", options: " + map);
        }

        public enum Type {
            literal,
            input,
            variable
        }

        public Expression(Type type, String operandId, MfFunction function, OptionsMap optionsMap) {
            this.type = type;
            this.operandId = operandId;
            this.functionFactory = function == null ? MockFunctions.get(":string") : function;
            this.map = optionsMap == null ? OptionsMap.EMPTY : optionsMap;
            if (this.functionFactory == null) {
                throw new IllegalArgumentException("Arrrg");
            }
        }

        public MfResolvedVariable resolve(MfContext context) {
            return functionFactory.from(this, context);
        }
    }

    /**
     * Only call inside match or message
     *
     * @param matcher2
     */
    public Expression getInternalExpression(Matcher matcher) {
        // The matcher was made from EXPRESSION_PATTERN, and matched
        String operand = matcher.group(1);
        // optional
        String functionName = matcher.group(2);
        // optional, but if it occurs then the function has to be there
        String options = matcher.group(3);

        Expression existingVariable = getExpressionFromVariableId(operand);
        if (functionName == null && existingVariable != null) {
            return existingVariable;
        } else {
            MfFunction functionFactory =
                    MockFunctions.get(functionName == null ? ":string" : functionName);
            OptionsMap optionsMap = OptionsMap.make(options);
            if (existingVariable != null) {
                return new Expression(Type.variable, operand, functionFactory, optionsMap);
            } else {
                if (operand.startsWith("$")) {
                    return new Expression(Type.input, operand, functionFactory, optionsMap);
                } else {
                    return new Expression(Type.literal, operand, functionFactory, optionsMap);
                }
            }
        }
    }

    public List<Expression> handleMatch(String line) {
        int selector = 0;
        int position = 6;
        Matcher matcher = EXPRESSION_TRIMMING_PATTERN.matcher(line);
        while (true) {
            matcher.region(position, line.length());
            if (!matcher.lookingAt()) {
                // later, check for cruft at end
                return selectors;
            }
            String operand = matcher.group(1);
            if (operand.contains("}")) {
                throw new IllegalArgumentException();
            }
            // optional
            String functionName = matcher.group(2);
            // optional, but if it occurs then the function has to be there
            String options = matcher.group(3);

            Expression existingVariable = variables.get(operand);
            Expression result = null;

            if (functionName == null && existingVariable != null) {
                result = existingVariable;
            } else {
                MfFunction functionFactory =
                        functionName == null ? null : MockFunctions.get(functionName);
                OptionsMap optionsMap =
                        options == null || options.isBlank()
                                ? null
                                : OptionsMap.make(options); // optional
                String newVariable = "$selector__" + selector++;
                if (existingVariable != null) {
                    addVariable(
                            newVariable,
                            result =
                                    new Expression(
                                            Type.variable, operand, functionFactory, optionsMap));
                } else {
                    if (operand.startsWith("$")) {
                        addVariable(
                                newVariable,
                                result =
                                        new Expression(
                                                Type.input, operand, functionFactory, optionsMap));
                    } else {
                        addVariable(
                                newVariable,
                                result =
                                        new Expression(
                                                Type.literal,
                                                operand,
                                                functionFactory,
                                                optionsMap));
                    }
                }
            }
            addSelector(result);
            position = matcher.end();
        }
    }

    public Object handleLocal(String line) {
        if (!selectors.isEmpty()) {
            throw new IllegalArgumentException(".local cannot occur after .match");
        }
        Matcher matcher = LOCAL_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Malformed line: " + RegexUtilities.showMismatch(INPUT_PATTERN, line));
        }
        String targetVariable = matcher.group(1);
        if (variables.containsKey(targetVariable)) {
            throw new IllegalArgumentException("Can't redefine variable in: " + line);
        }
        String operand = matcher.group(2);
        String functionName = matcher.group(3); // optional
        String options =
                matcher.group(4); // optional, but if it occurs then the function has to be there

        MfFunction functionFactory = functionName == null ? null : MockFunctions.get(functionName);
        OptionsMap optionsMap = options.isBlank() ? null : OptionsMap.make(options); // optional
        Expression existingVariable = variables.get(operand);
        Expression result;
        if (existingVariable != null) {
            result = new Expression(Type.variable, operand, functionFactory, optionsMap);
        } else {
            if (operand.startsWith("$")) {
                result = new Expression(Type.variable, operand, functionFactory, optionsMap);
            } else {
                result = new Expression(Type.variable, operand, functionFactory, optionsMap);
            }
        }
        variables.put(targetVariable, result);
        return result;
    }

    public Object handleInput(String line) {
        if (!selectors.isEmpty()) {
            throw new IllegalArgumentException(".input cannot occur after .match");
        }
        Matcher matcher = INPUT_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Malformed line: " + RegexUtilities.showMismatch(INPUT_PATTERN, line));
        }
        String operand = matcher.group(1);
        String functionName = matcher.group(2); // not optional
        String options = matcher.group(3); // optional

        // must be an input parameter
        Expression var = variables.get(operand);
        if (var != null) {
            throw new IllegalArgumentException(
                    String.format("Input parameter %s must not be a variable: %s", operand, line));
        }
        MfFunction functionFactory = MockFunctions.get(functionName);
        if (functionFactory == null) {
            throw new IllegalArgumentException(
                    String.format("Function %s not registered: %s", functionName, line));
        }
        OptionsMap optionsMap = OptionsMap.make(options);
        return addVariable(
                operand, new Expression(Type.input, operand, functionFactory, optionsMap));
    }

    Set<String> requiredVariables = null;
    Multimap<String, Expression> requiredInput = null;
    Set<String> unused = null;

    public MockMessageFormat freeze() {
        if (requiredVariables != null) {
            return this;
        }
        for (Entry<List<String>, Message> entry : variants.map.entrySet()) {
            if (selectors.size() != entry.getKey().size()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Number of keys (%s, %s) doesn't match number of selectors (%s):\n\t%s",
                                entry.getKey().size(),
                                entry.getKey(),
                                selectors.size(),
                                selectors));
            }
        }
        requiredVariables = new LinkedHashSet<>();
        requiredInput = LinkedHashMultimap.create();
        // we first find out what variables and input parameters are used in the match and
        // submessages
        // we check for selectors and messages
        for (Expression variant : selectors) {
            extractVariablesAndInput(variant, requiredVariables, requiredInput);
        }
        // we also check for messages
        for (Entry<List<String>, Message> keysAndVariants : variants.map.entrySet()) {
            for (Object obj : keysAndVariants.getValue().contents) {
                if (obj instanceof Expression) {
                    extractVariablesAndInput((Expression) obj, requiredVariables, requiredInput);
                }
            }
        }
        unused = new LinkedHashSet<>(variables.keySet());
        unused.removeAll(requiredVariables);
        return this;
    }

    public void extractVariablesAndInput(
            Expression expression,
            Set<String> requiredVariables,
            Multimap<String, Expression> requiredInput) {
        switch (expression.type) {
            case input:
                requiredInput.put(expression.operandId, expression);
                break;
            case variable:
                Expression parentExpression = variables.get(expression.operandId);
                if (parentExpression == null) { // this is an input and we didn't know it
                    requiredInput.put(expression.operandId, expression);
                } else {
                    requiredVariables.add(expression.operandId);
                    extractVariablesAndInput(parentExpression, requiredVariables, requiredInput);
                }
                break;
            default:
                break;
        }
    }

    /**
     * A container for context used to format a particular set of input parameters. It contains
     * structures that are used in intermediate processing.
     */
    static class MfContext {
        public final Map<String, Object> inputParameters =
                new LinkedHashMap<>(
                        Map.of("$user:locale", Locale.ENGLISH, "$user:gender", "other"));
        public final Map<String, MfResolvedVariable> boundVariables = new LinkedHashMap<>();

        @Override
        public String toString() {
            return String.format(
                    "inputParameters:\t%s\nboundVariables:\t%s", inputParameters, boundVariables);
        }

        public Locale getUserLocale() {
            return (Locale) inputParameters.get("$user:locale");
        }

        public String getUserGender() {
            return (String) inputParameters.get("$user:gender");
        }

        public MfResolvedVariable get(String name) {
            return boundVariables.get(name);
        }

        public Object getInputParameter(String id) {
            return inputParameters.get(id);
        }

        public MfContext addInput(String id, Object parameter) {
            inputParameters.put(id, parameter);
            return this;
        }

        public MfContext addInput(Map<String, Object> parameters) {
            inputParameters.putAll(parameters);
            return this;
        }

        public void bindVariables(MockMessageFormat messageFormat) {
            // the variables are in order of entry, so we should never be left hanging
            for (Entry<String, Expression> entry : messageFormat.variables.entrySet()) {
                if (messageFormat.requiredVariables.contains(entry.getKey())) {
                    Expression expression = entry.getValue();
                    boundVariables.put(entry.getKey(), expression.resolve(this));
                }
            }
        }

        public MfResolvedVariable addVariable(String name, MfResolvedVariable numberVariable) {
            if (boundVariables.containsKey(name)) {
                throw new IllegalArgumentException("Can't reassign variable");
            }
            boundVariables.put(name, numberVariable);
            return numberVariable;
        }

        public Object getInput(String operandId) {
            return inputParameters.get(operandId);
        }
    }

    public String format(MfContext context) {
        freeze();
        if (!context.inputParameters.keySet().containsAll(requiredInput.keySet())) {
            throw new IllegalArgumentException(
                    "Missing input parameters: "
                            + Sets.difference(
                                    requiredInput.keySet(), context.inputParameters.keySet()));
        }
        context.bindVariables(this);

        List<MfResolvedVariable> boundSelectors = new ArrayList<>();
        for (Expression expression : selectors) {
            boundSelectors.add(expression.resolve(context));
        }

        // all of the variables are now bound, except in variant messages
        Entry<List<String>, Message> bestVariantTilNow = null;

        int counter = 0;
        main:
        for (Entry<List<String>, Message> currentVariant : variants.map.entrySet()) {
            if (DEBUG)
                System.out.println(" " + ++counter + ") Current keys: " + currentVariant.getKey());
            MatchComparison matchResult =
                    isBetter(context, boundSelectors, bestVariantTilNow, currentVariant);
            switch (matchResult) {
                case FAIL:
                case WORSE:
                case SAME:
                    break;
                case BETTER:
                    bestVariantTilNow = currentVariant;
                    break;
                case BEST:
                    bestVariantTilNow = currentVariant;
                    break main; // when the match result is perfect, there is no need to continue
            }
        }
        if (bestVariantTilNow == null) {
            throw new IllegalArgumentException("No match in selectors");
        }
        if (DEBUG) System.out.println("Best Keys: " + bestVariantTilNow.getKey());
        return bestVariantTilNow.getValue().format(context);
    }

    /**
     * The bestEntry starts out as null; it is replaced when there as a BETTER match. An entry is
     * BEST when all the match results are BEST. An entry is NO_MATCH when at least one match result
     * is NO_MATCH Otherwise, the first different match among the keys determines the result: WORSE
     * or BETTER
     */
    private MatchComparison isBetter(
            MfContext context,
            List<MfResolvedVariable> boundSelectors,
            Entry<List<String>, Message> bestVariantTilNow,
            Entry<List<String>, Message> currentVariant) {
        Iterator<String> variantKeys = currentVariant.getKey().iterator();
        Iterator<String> bestVariantKeysTilNow =
                bestVariantTilNow == null ? null : bestVariantTilNow.getKey().iterator();
        MatchComparison firstDifference = null;
        boolean allBest = true;
        // Note: we have already verified that the number of keys is the same as the number of
        // selectors
        // We walk along the keys; we have to hit all of them.
        // If any result is FAIL we return FAIL
        // If all results are BEST we return BEST
        // Otherwise the first time there is a difference we return the better one.
        // When bestVariantTilNow == null, then we return BETTER unless there is a FAIL.
        // A * is never a fail
        for (MfResolvedVariable selector : boundSelectors) {
            final String variantKey = variantKeys.next();
            final String bestVariantKeyTilNow =
                    bestVariantKeysTilNow == null ? null : bestVariantKeysTilNow.next();
            MatchComparison matchResult;
            // handle * at this point, so that we don't depend on the functions getting it right
            if (variantKey.equals("*")) {
                matchResult =
                        bestVariantKeyTilNow == null
                                ? MatchComparison.BETTER
                                : bestVariantKeyTilNow.equals("*")
                                        ? MatchComparison.SAME
                                        : MatchComparison.WORSE;
            } else {
                matchResult = selector.match(context, variantKey, bestVariantKeyTilNow);
            }
            if (DEBUG)
                System.out.println(
                        String.format(
                                "\tselector: %s, varientKey: %s, bestTilNow: %s, match: %s",
                                selector, variantKey, bestVariantKeyTilNow, matchResult));
            switch (matchResult) {
                case FAIL:
                    return MatchComparison.FAIL; // fail
                case WORSE:
                    // although we have a match, it isn't any better, so no change
                    if (firstDifference == null) {
                        firstDifference = matchResult;
                    }
                    allBest = false;
                    break;
                case SAME:
                    // although we have a match, it isn't any better, so no change
                    allBest = false;
                    break;
                case BETTER:
                    if (firstDifference == null) {
                        firstDifference = matchResult;
                    }
                    allBest = false;
                    break;
                case BEST:
                    if (firstDifference == null) {
                        firstDifference = matchResult;
                    }
                    break;
            }
        }
        firstDifference = allBest ? MatchComparison.BEST : firstDifference;
        if (DEBUG) System.out.println("  row result: " + firstDifference);
        return firstDifference;
    }

    public MockMessageFormat add(String... messageFormatLines) {
        for (String line : messageFormatLines) {
            dumbParseInput(line);
        }
        return this;
    }

    public MockMessageFormat add(List<String> messageFormatLines) {
        for (String line : messageFormatLines) {
            dumbParseInput(line);
        }
        return this;
    }

    @Override
    public String toString() {
        return String.format(
                "requiredInput:\t%s\n"
                        + "requiredVariables:\t%s\n"
                        + "selectors:\t%s\n"
                        + "unused:\t%s\n"
                        + "variables:\t%s\n"
                        + "variants:\t%s\n",
                requiredInput, requiredVariables, selectors, unused, variables, variants);
    }
}
