package org.unicode.cldr.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

// TODO needs some cleanup to make it work right with comments.
// Should incorporate structure from RegexModify

public class XMLModify {
    enum MyOptions {
        sourceDirectory(new Params()
            .setHelp("sourceDirectory")
            .setDefault(CLDRPaths.COMMON_DIRECTORY)
            .setMatch(".+")), targetDirectory(new Params()
                .setHelp("targetDirectory")
                .setDefault(CLDRPaths.GEN_DIRECTORY + "xmlModify")
                .setMatch(".+")), fileRegex(new Params().setHelp("filename regex")
                    .setMatch(".*")
                    .setDefault(".*")), pathRegex(new Params().setHelp("path regex")
                        .setMatch(".*")),
//        PathReplacement(new Params().setHelp("path replacement")
//            .setMatch(".*")),
//        valueRegex(new Params().setHelp("path regex")
//            .setMatch(".*")),
//        ValueReplacement(new Params().setHelp("path replacement")
//            .setMatch(".*")),
        verbose(new Params().setHelp("verbose debugging messages")),
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

    public static void main(String[] args) {
        MyOptions.parse(args, true);
        File sourceDirectory = new File(MyOptions.sourceDirectory.option.getValue());
        Matcher fileMatcher = Pattern.compile(MyOptions.fileRegex.option.getValue()).matcher("");
        Matcher pathMatcher = Pattern.compile(MyOptions.pathRegex.option.getValue()).matcher("");
        List<Pair<String, String>> data = new ArrayList<>();
        List<Pair<String, String>> output = new ArrayList<>();
        try (PrintWriter out = new PrintWriter(System.out)) {
            for (String file : sourceDirectory.list()) {
                if (!fileMatcher.reset(file).matches()) {
                    continue;
                }
                data.clear();
                out.println(file);
                XPathParts lastParts = new XPathParts();
                for (Pair<String, String> pathValue : XMLFileReader.loadPathValues(
                    sourceDirectory.toString() + "/" + file, data, true, true)) {
                    String value = pathValue.getSecond();
                    String path = pathValue.getFirst();
                    if (path.equals("!")) {
                        out.println("<!--" + value + " -->");
                        continue;
                    }
                    XPathParts parts = XPathParts.getInstance(path);
                    if (pathMatcher.reset(path).matches()) {
                        String type = parts.getAttributeValue(-1, "type");
                        parts.setAttribute(-1, "type", type.toLowerCase(Locale.ROOT).replaceAll("-", ""));
                    }
                    parts.writeDifference(out, parts, lastParts, lastParts, value, null);
                    out.flush();
                    lastParts = parts;
                }
            }
        }
    }
}
