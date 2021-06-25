package org.unicode.cldr.tool;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;

public class ListSurveyToolStatus {
    private enum MyOptions {
        locale(new Params().setHelp("Regex for locales").setMatch(".*").setDefault("^root$")),
        paths(new Params().setHelp("Regex for paths").setMatch(".+").setDefault("/unit.*day")),
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

        private static Set<String> parse(String[] args) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) {
        MyOptions.parse(args);
        Matcher locales = Pattern.compile(MyOptions.locale.option.getValue()).matcher("");
        Matcher paths = Pattern.compile(MyOptions.paths.option.getValue()).matcher("");
        Factory factory = CLDRConfig.getInstance().getCldrFactory();
        Status status = new Status();

        for (String locale : factory.getAvailable()) {
            if (locales.reset(locale).find()) {
                CLDRFile cldrFile = factory.make(locale, true);
                String testValue = cldrFile.getStringValue("//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-day-person\"]/displayName");
                Set<PathHeader> sorted = new TreeSet<>();
                for (String path : cldrFile.fullIterable()) {
                    sorted.add(PathHeader.getFactory().fromPath(path));
                }
                for (PathHeader pathHeader : sorted) {
                    // eg //ldml/units/unitLength[@type="long"]/unit[@type="duration-day-person"]/unitPattern[@count="one"][@case="accusative"]
                    final String path = pathHeader.getOriginalPath();
                    if (paths.reset(path).find()) {
                        String value = cldrFile.getStringValue(path);
                        String localeFound = cldrFile.getSourceLocaleID(path, status);
                        System.out.println(locale
                            + (locale.equals(localeFound) ? "" : "/" + localeFound)
                            + "\t" + pathHeader
                            + "\n\tvalue:\t" + value
                            + "\n\tpath:\t" + path
                            + (path.equals(status.pathWhereFound) ? "" : "\n\toPath:\t" + status.pathWhereFound)
                            + "\n");
                    }
                }
            }
        }
        System.out.println("DONE");
    }
}
