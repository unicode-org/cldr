package org.unicode.cldr.tool;

import com.ibm.icu.dev.util.UOption;
import org.unicode.cldr.util.CLDRTool;

@CLDRTool(alias = "kbd", description = "Tool for working with CLDR Keyboard files")
public class KeyboardTool {

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.create("flatten", 'F', UOption.REQUIRES_ARG),
    };

    public static void help() {
        System.out.println(
                "CLDR Keyboard Tool\n"
                        + "----\n"
                        + "Usage:\n"
                        + " -h | --help | -?                          print this help\n"
                        + " -F infile.xml | --flatten infile.xml > outfile.xml      print a flattened xml to stdout, without imports\n"
                        + "");
    }

    public static void main(String args[]) throws Throwable {
        UOption.parseArgs(args, options);
        if (options[0].doesOccur || options[1].doesOccur) {
            help();
        } else if (options[2].doesOccur) {
            System.err.println("Flatten: " + options[2].value);
            KeyboardFlatten.flatten(options[2].value, System.out);
        } else {
            help();
        }
    }
}
