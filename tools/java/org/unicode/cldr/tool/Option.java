package org.unicode.cldr.tool;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Simpler mechanism for handling options, where everything can be defined in one place.
 * @author markdavis
 */
public class Option {
    private final String tag;
    private final Character flag;
    private final Pattern match;
    private final String defaultArgument;
    private final String helpString;
    private final Enum<?> optionEnumValue;
    private boolean doesOccur;
    private String value;
    //private boolean implicitValue;

    public void clear() {
        doesOccur = false;
        //implicitValue = false;
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

    //    public boolean getUsingImplicitValue() {
    //        return false;
    //    }

    public boolean doesOccur() {
        return doesOccur;
    }

    private Option(Enum<?> enumOption, String tag, Character flag, Pattern argumentPattern, String defaultArgument, String helpString) {
        if (defaultArgument != null && argumentPattern != null) {
            if (!argumentPattern.matcher(defaultArgument).matches()) {
                throw new IllegalArgumentException("Default argument doesn't match pattern: " + defaultArgument + ", " + argumentPattern);
            }
        }
        this.match = argumentPattern;
        this.helpString = helpString;
        this.tag = tag;
        this.flag = flag;
        this.defaultArgument = defaultArgument;
        optionEnumValue = null;
    }

    public String toString() {
        return "-" + flag + " (" + tag + ") \t" 
        + (match == null ? "no-arg" : match.pattern()) + " \t" + helpString;
    }

    enum MatchResult {noValueError, noValue, valueError, value}

    public MatchResult matches(String inputValue) {
        if (doesOccur) {
            System.err.println("Duplicate argument: '" + tag);
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
            System.err.println("The flag '" + tag + "' has the parameter '" + inputValue + "', which must match " + match.pattern());
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

        public Options add(String string, String helpText) {
            return add(string, string.charAt(0), null, null, helpText);
        }

        public Options add(String string, String argumentPattern, String helpText) {
            return add(string, string.charAt(0), argumentPattern, null, helpText);
        }

        public Options add(String string, String argumentPattern, String defaultArgument, String helpText) {
            return add(string, string.charAt(0), argumentPattern, defaultArgument, helpText);
        }

        public Option add(Enum<?> optionEnumValue, String argumentPattern,String defaultArgument, String helpText) {
            add(optionEnumValue, optionEnumValue.name(), optionEnumValue.name().charAt(0), argumentPattern, defaultArgument, helpText);
            return get(optionEnumValue.name());
            // TODO cleanup
        }
        
        public Options add(String string, Character flag, String argumentPattern, String defaultArgument, String helpText) {
            return add(null, string, flag, argumentPattern, defaultArgument, helpText);
        }

        public Options add(Enum<?> optionEnumValue, String string, Character flag, String argumentPattern, String defaultArgument, String helpText) {
            if (stringToValues.containsKey(string)) {
                throw new IllegalArgumentException("Duplicate tag <" + string + "> with " + stringToValues.get(string));
            }
            if (charToValues.containsKey(flag)) {
                throw new IllegalArgumentException("Duplicate tag <" + string + ", " + flag + "> with " + charToValues.get(flag));
            }
            Option option = new Option(optionEnumValue, string, flag, 
                            argumentPattern == null ? null : Pattern.compile(argumentPattern, Pattern.COMMENTS), 
                                    defaultArgument, helpText);
            stringToValues.put(string, option);
            enumToValues.put(optionEnumValue, option);
            charToValues.put(flag, option);
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
                    value = i < args.length - 1 ? args[i+1] : null;
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
                    System.out.println("Unknown flag: " + arg);
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
                    //option.implicitValue = true;
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
                for (Option option : stringToValues.values()) {
                    if (!option.doesOccur && option.value == null) {
                        continue;
                    }
                    System.out.println(option.tag + "\t=\t" + option.value);
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

    final static Options myOptions = new Options()
    .add("file", ".*", "Filter the information based on file name, using a regex argument")
    .add("path", ".*", "default-path", "Filter the information based on path name, using a regex argument")
    .add("content", ".*", "Filter the information based on content name, using a regex argument")
    .add("gorp", null, null, "Gorp")
    .add("regex", "a*", null, "Gorp")
    ;

    public static void main(String[] args) {
        if (args.length == 0) {
            args = "foo -fen.xml -c a* --path bar -g b -r aaa".split("\\s+");
        }
        myOptions.parse(args, true);

        for (Option option : myOptions) {
            System.out.println(option.getTag() + "\t" + option.doesOccur() + "\t" + option.getValue() + "\t" + option.getHelpString());
        }
        Option option = myOptions.get("file");
        System.out.println("\n" + option.doesOccur() + "\t" + option.getValue() + "\t" + option);
    }

    public String getDefaultArgument() {
        return defaultArgument;
    }

}
