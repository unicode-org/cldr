package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRTool;

import com.ibm.icu.dev.util.CollectionUtilities;

/**
 * Simpler mechanism for handling options, where everything can be defined in one place.
 * For an example, see {@link org.unicode.cldr.tool.DiffCldr.java}
 * Note that before any enums are used, the main has to have MyOptions.parse(args, true);
 * <ul>
 * <li>The options and help message are defined in one place, for easier maintenance.</li>
 * <li>The options are represented by enums, for better type & syntax checking for problems.</li>
 * <li>The arguments can be checked against a regular expression.</li>
 * <li>The flag is defaulted to the first letter.</li>
 * <li>The options are printed at the top of the console output to document the exact input.</li>
 * <li>The callsite is slightly more verbose, but safer:
 *    <table>
 *    <tr><th>old</th><td>options[FILE_FILTER].value</td></tr>
 *    <tr><th>new</th><td>MyOptions.file_filter.option.getValue();</td></tr>
 *    </table>
 * </ul>
 * @author markdavis
 */
public class Option {
    private final String tag;
    private final Character flag;
    private final Pattern match;
    private final String defaultArgument;
    private final String helpString;
    //private final Enum<?> optionEnumValue;
    private boolean doesOccur;
    private String value;

    /** Arguments for setting up options.
     * Migration
     * from UOption.create("generate_html", 'g', UOption.OPTIONAL_ARG).setDefault(CLDRPaths.CHART_DIRECTORY + "/errors/"),
     * to: generate_html(new Params().setHelp"
     *  • UOption.NO_ARG: must have neither .setMatch nor .setDefault
     *  • UOption.REQUIRES_ARG: must have .setMatch but not setDefault
     *  • UOption.OPTIONAL_ARG: must have .setMatch and .setDefault (usually just copy over the .setDefault from the UOption)
     *  • Supply a meaningful .setHelp message
     *  • If the flag (the 'g' above) is different than the first letter of the enum, have a .setFlag
     */
    public static class Params {
        private Object match = null;
        private String defaultArgument = null;
        private String helpString = null;
        private char flag = 0;

        /**
         * @param match the match to set
         */
        public Params setMatch(Object match) {
            this.match = match;
            return this;
        }

        /**
         * @param defaultArgument the defaultArgument to set
         */
        public Params setDefault(String defaultArgument) {
            this.defaultArgument = defaultArgument;
            return this;
        }

        /**
         * @param helpString the helpString to set
         */
        public Params setHelp(String helpString) {
            this.helpString = helpString;
            return this;
        }

        public Params setFlag(char c) {
            flag = c;
            return this;
        }
    }

    // private boolean implicitValue;

    public void clear() {
        doesOccur = false;
        // implicitValue = false;
        value = null;
    }

    public String getTag() {
        return tag;
    }

    public Pattern getMatch() {
        return match;
    }

    public String getHelpString() {
        return helpString;
    }

    public String getValue() {
        return value;
    }

    public String getExplicitValue() {
        return doesOccur ? value : null;
    }

    // public boolean getUsingImplicitValue() {
    // return false;
    // }

    public boolean doesOccur() {
        return doesOccur;
    }

    public Option(Enum<?> optionEnumValue, String argumentPattern, String defaultArgument, String helpText) {
        this(optionEnumValue, optionEnumValue.name(), (Character) (optionEnumValue.name().charAt(0)), Pattern.compile(argumentPattern), defaultArgument,
            helpText);
    }

    public Option(Enum<?> enumOption, String tag, Character flag, Object argumentPatternIn, String defaultArgument, String helpString) {
        Pattern argumentPattern = getPattern(argumentPatternIn);

        if (defaultArgument != null && argumentPattern != null) {
            if (!argumentPattern.matcher(defaultArgument).matches()) {
                throw new IllegalArgumentException("Default argument doesn't match pattern: " + defaultArgument + ", "
                    + argumentPattern);
            }
        }
        this.match = argumentPattern;
        this.helpString = helpString;
        this.tag = tag;
        this.flag = flag;
        this.defaultArgument = defaultArgument;
    }

    public Option(Enum<?> optionEnumValue, Params optionList) {
        this(optionEnumValue,
            optionEnumValue.name(),
            optionList.flag != 0 ? optionList.flag : optionEnumValue.name().charAt(0),
            optionList.match,
            optionList.defaultArgument,
            optionList.helpString);
    }

    private static Pattern getPattern(Object match) {
        if (match == null) {
            return null;
        } else if (match instanceof Pattern) {
            return (Pattern) match;
        } else if (match instanceof String) {
            return Pattern.compile((String) match);
        } else if (match instanceof Class) {
            try {
                Enum[] valuesMethod = (Enum[]) ((Class) match).getMethod("values").invoke(null);
                return Pattern.compile(CollectionUtilities.join(valuesMethod, "|"));
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        throw new IllegalArgumentException(match.toString());
    }

    static final String PAD = "                    ";

    public String toString() {
        return "-" + flag
            + " (" + tag + ")"
            + PAD.substring(Math.min(tag.length(), PAD.length()))
            + (match == null ? "no-arg" : "match: " + match.pattern())
            + (defaultArgument == null ? "" : " \tdefault=" + defaultArgument)
            + " \t" + helpString;
    }

    enum MatchResult {
        noValueError, noValue, valueError, value
    }

    public MatchResult matches(String inputValue) {
        if (doesOccur) {
            System.err.println("#Duplicate argument: '" + tag);
            return match == null ? MatchResult.noValueError : MatchResult.valueError;
        }
        doesOccur = true;
        if (inputValue == null) {
            inputValue = defaultArgument;
        }

        if (match == null) {
            return MatchResult.noValue;
        } else if (inputValue != null && match.matcher(inputValue).matches()) {
            this.value = inputValue;
            return MatchResult.value;
        } else {
            System.err.println("#The flag '" + tag + "' has the parameter '" + inputValue + "', which must match "
                + match.pattern());
            return MatchResult.valueError;
        }
    }

    public static class Options implements Iterable<Option> {

        private String mainMessage;
        final Map<String, Option> stringToValues = new LinkedHashMap<String, Option>();
        final Map<Enum<?>, Option> enumToValues = new LinkedHashMap<Enum<?>, Option>();
        final Map<Character, Option> charToValues = new LinkedHashMap<Character, Option>();
        final Set<String> results = new LinkedHashSet<String>();
        {
            add("help", null, "Provide the list of possible options");
        }
        final Option help = charToValues.values().iterator().next();

        public Options(String mainMessage) {
            this.mainMessage = (mainMessage.isEmpty() ? "" : mainMessage + "\n") + "Here are the options:\n";
        }

        public Options() {
            this("");
        }

        /**
         * Generate based on class and, optionally, CLDRTool annotation
         * @param forClass
         */
        public Options(Class<?> forClass) {
            this(forClass.getSimpleName() + ": " + getCLDRToolDescription(forClass));
        }

        public Options add(String string, String helpText) {
            return add(string, string.charAt(0), null, null, helpText);
        }

        public Options add(String string, String argumentPattern, String helpText) {
            return add(string, string.charAt(0), argumentPattern, null, helpText);
        }

        public Options add(String string, Object argumentPattern, String defaultArgument, String helpText) {
            return add(string, string.charAt(0), argumentPattern, defaultArgument, helpText);
        }

        public Option add(Enum<?> optionEnumValue, Object argumentPattern, String defaultArgument, String helpText) {
            add(optionEnumValue, optionEnumValue.name(), optionEnumValue.name().charAt(0), argumentPattern,
                defaultArgument, helpText);
            return get(optionEnumValue.name());
            // TODO cleanup
        }

        public Options add(String string, Character flag, Object argumentPattern, String defaultArgument,
            String helpText) {
            return add(null, string, flag, argumentPattern, defaultArgument, helpText);
        }

        public Options add(Enum<?> optionEnumValue, String string, Character flag, Object argumentPattern,
            String defaultArgument, String helpText) {
            Option option = new Option(optionEnumValue, string, flag, argumentPattern, defaultArgument, helpText);
            return add(optionEnumValue, option);
        }

        public Options add(Enum<?> optionEnumValue, Option option) {
            if (stringToValues.containsKey(option.tag)) {
                throw new IllegalArgumentException("Duplicate tag <" + option.tag + "> with " + stringToValues.get(option.tag));
            }
            if (charToValues.containsKey(option.flag)) {
                throw new IllegalArgumentException("Duplicate tag <" + option.tag + ", " + option.flag + "> with "
                    + charToValues.get(option.flag));
            }
            stringToValues.put(option.tag, option);
            charToValues.put(option.flag, option);
            if (optionEnumValue != null) {
                enumToValues.put(optionEnumValue, option);
            }
            return this;
        }

        public Set<String> parse(Enum<?> enumOption, String[] args, boolean showArguments) {
            return parse(args, showArguments);
        }

        public Set<String> parse(String[] args, boolean showArguments) {
            results.clear();
            for (Option option : charToValues.values()) {
                option.clear();
            }
            int errorCount = 0;
            boolean needHelp = false;
            for (int i = 0; i < args.length; ++i) {
                String arg = args[i];
                if (!arg.startsWith("-")) {
                    results.add(arg);
                    continue;
                }
                // can be of the form -fparam or -f param or --file param
                boolean isStringOption = arg.startsWith("--");
                String value = null;
                Option option;
                if (isStringOption) {
                    arg = arg.substring(2);
                    int equalsPos = arg.indexOf('=');
                    if (equalsPos > -1) {
                        value = arg.substring(equalsPos + 1);
                        arg = arg.substring(0, equalsPos);
                    }
                    option = stringToValues.get(arg);
                } else { // starts with single -
                    if (arg.length() > 2) {
                        value = arg.substring(2);
                    }
                    arg = arg.substring(1);
                    option = charToValues.get(arg.charAt(0));
                }
                boolean tookExtraArgument = false;
                if (value == null) {
                    value = i < args.length - 1 ? args[i + 1] : null;
                    if (value != null && value.startsWith("-")) {
                        value = null;
                    }
                    if (value != null) {
                        ++i;
                        tookExtraArgument = true;
                    }
                }
                if (option == null) {
                    ++errorCount;
                    System.out.println("#Unknown flag: " + arg);
                } else {
                    MatchResult matches = option.matches(value);
                    if (tookExtraArgument && (matches == MatchResult.noValue || matches == MatchResult.noValueError)) {
                        --i;
                    }
                    if (option == help) {
                        needHelp = true;
                    }
                }
            }
            // clean up defaults
            for (Option option : stringToValues.values()) {
                if (!option.doesOccur && option.defaultArgument != null) {
                    option.value = option.defaultArgument;
                    // option.implicitValue = true;
                }
            }

            if (errorCount > 0) {
                System.err.println("Invalid Option - Choices are:");
                System.err.println(getHelp());
                System.exit(1);
            } else if (needHelp) {
                System.err.println(getHelp());
                System.exit(1);
            } else if (showArguments) {
                System.out.println(Arrays.asList(args));
                for (Option option : stringToValues.values()) {
                    if (!option.doesOccur && option.value == null) {
                        continue;
                    }
                    System.out.println("#-" + option.flag
                        + "\t" + option.tag
                        + (option.doesOccur ? "\t≔\t" : "\t≝\t") + option.value);
                }
            }
            return results;
        }

        private String getHelp() {
            StringBuilder buffer = new StringBuilder(mainMessage);
            boolean first = true;
            for (Option option : stringToValues.values()) {
                if (first) {
                    first = false;
                } else {
                    buffer.append('\n');
                }
                buffer.append(option);
            }
            return buffer.toString();
        }

        @Override
        public Iterator<Option> iterator() {
            return stringToValues.values().iterator();
        }

        public Option get(String string) {
            Option result = stringToValues.get(string);
            if (result == null) {
                throw new IllegalArgumentException("Unknown option: " + string);
            }
            return result;
        }

        public Option get(Enum<?> enumOption) {
            Option result = enumToValues.get(enumOption);
            if (result == null) {
                throw new IllegalArgumentException("Unknown option: " + enumOption);
            }
            return result;
        }

    }

    private enum Test {
        A, B, C
    }

    final static Options myOptions = new Options()
        .add("file", ".*", "Filter the information based on file name, using a regex argument")
        .add("path", ".*", "default-path", "Filter the information based on path name, using a regex argument")
        .add("content", ".*", "Filter the information based on content name, using a regex argument")
        .add("gorp", null, null, "Gorp")
        .add("enum", Test.class, null, "enum check")
        .add("regex", "a*", null, "Gorp");

    public static void main(String[] args) {
        if (args.length == 0) {
            args = "foo -fen.xml -c a* --path bar -g b -r aaa -e B".split("\\s+");
        }
        myOptions.parse(args, true);

        for (Option option : myOptions) {
            System.out.println("#" + option.getTag() + "\t" + option.doesOccur() + "\t" + option.getValue() + "\t"
                + option.getHelpString());
        }
        Option option = myOptions.get("file");
        System.out.println("\n#" + option.doesOccur() + "\t" + option.getValue() + "\t" + option);
    }

    /**
     * Helper function
     * @param forClass
     * @return
     */
    private static String getCLDRToolDescription(Class<?> forClass) {
        CLDRTool cldrTool = forClass.getAnnotation(CLDRTool.class);
        if (cldrTool != null) {
            return cldrTool.description();
        } else {
            return "(no @CLDRTool annotation)";
        }
    }

    public String getDefaultArgument() {
        return defaultArgument;
    }

}
