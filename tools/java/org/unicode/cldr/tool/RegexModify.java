package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.RegexUtilities;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

public class RegexModify {

    enum MyOptions {
        verbose(new Params()
            .setHelp("verbose debugging messages")), sourceDirectory(new Params()
                .setHelp("sourceDirectory")
                .setDefault(CLDRPaths.COMMON_DIRECTORY)
                .setMatch(".+")), targetDirectory(new Params()
                    .setHelp("targetDirectory")
                    .setDefault(CLDRPaths.GEN_DIRECTORY + "xmlModify")
                    .setMatch(".+")), fileRegex(new Params()
                        .setHelp("filename regex")
                        .setMatch(".*")
                        .setDefault(".*\\.xml")), lineRegex(new Params()
                            .setHelp("line regex")
                            .setMatch(".*")), applyFunction(new Params()
                                .setHelp("function name to apply")
                                .setMatch(".*")),
                                ;

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(Params params) {
            option = new Option(this, params);
        }

        private static Options myOptions = new Options();
        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args, boolean showArguments) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) throws Exception {
        MyOptions.parse(args, true);
        File sourceDirectory = new File(MyOptions.sourceDirectory.option.getValue());
        File targetDirectory = new File(MyOptions.targetDirectory.option.getValue());
        Matcher fileMatcher = Pattern.compile(MyOptions.fileRegex.option.getValue()).matcher("");
        String functionName = MyOptions.applyFunction.option.getValue();
        RegexFunction f = getFunction(RegexModify.class, functionName);

        for (String file : sourceDirectory.list()) {
            if (!fileMatcher.reset(file).matches()) {
                continue;
            }
            try (
                BufferedReader in = FileUtilities.openUTF8Reader(sourceDirectory.toString(), file);
                PrintWriter out = FileUtilities.openUTF8Writer(targetDirectory.toString(), file)) {
                f.clear();
                for (String line : FileUtilities.in(in)) {
                    String newLine = f.apply(line);
                    if (newLine != null) {
                        out.println(newLine);
                    }
                }
                System.out.println(f.getChangedCount() + " changed lines in " + file);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T> T getFunction(Class class1, String applyFunction) {
        Map<String, Class<Function>> methods = getMethods(class1);
        Class result = methods.get(applyFunction);
        try {
            return (T) result.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("-a value must be in " + methods.keySet()
                + " but is “" + applyFunction + "”");
        }
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, Class<Function>> getMethods(Class class1) {
        ImmutableMap.Builder<String, Class<Function>> result = ImmutableMap.builder();
        //Set<Class<Function>> skipSet = new HashSet<>(Arrays.asList(skip));
        for (Class classMember : class1.getClasses()) {
            if ((Modifier.ABSTRACT & classMember.getModifiers()) != 0
                || !Function.class.isAssignableFrom(classMember)) {
                continue;
            }

            String name = classMember.getName();
            result.put(name.substring(name.indexOf('$') + 1), classMember);
        }
        return result.build();
    }

    public static abstract class RegexFunction implements Function<String, String> {
        protected Matcher lineMatcher;
        private int count;

        public RegexFunction() {
            lineMatcher = Pattern.compile(getPattern()).matcher("");
        }

        public void clear() {
            count = 0;
        }

        public int getChangedCount() {
            return count;
        }

        public String apply(String line) {
            if (lineMatcher.reset(line).matches()) {
                String oldLine = line;
                line = fixLine();
                if (!line.equals(oldLine)) {
                    ++count;
                }
            } else if (getCheckOnPattern() != null && line.contains(getCheckOnPattern())) {
                System.out.println(RegexUtilities.showMismatch(lineMatcher, line));
            }
            return line;
        }

        public abstract String getPattern();

        public abstract String getCheckOnPattern(); // for debugging the regex

        public abstract String fixLine();
    }

    public static class Subdivision extends RegexFunction {
        @Override
        public String getCheckOnPattern() {
            return "subdivision";
        }

        @Override
        public String getPattern() {
            //return "(.*<subdivision(?:Alias)? type=\")([^\"]+)(\".*)";
            return "(.*<subdivision(?:Alias)? type=\")([^\"]+)(\" replacement=\")([^\"]+)(\".*)";
        }

        @Override
        public String fixLine() {
            String value = convertToCldr(lineMatcher.group(2));
            String value2 = convertToCldr(lineMatcher.group(4));
            //return lineMatcher.replaceAll("$1"+value+"$3"); // TODO modify to be cleaner
            return lineMatcher.replaceAll("$1" + value + "$3" + value2 + "$5"); // TODO modify to be cleaner
        }

        private static boolean isRegionCode(String s) {
            return s.length() == 2 || (s.length() == 3 && s.compareTo("A") < 0);
        }

        private static String convertToCldr(String regionOrSubdivision) {
            return isRegionCode(regionOrSubdivision) ? regionOrSubdivision.toUpperCase(Locale.ROOT)
                : regionOrSubdivision.replace("-", "").toLowerCase(Locale.ROOT);
        }

    }
}
