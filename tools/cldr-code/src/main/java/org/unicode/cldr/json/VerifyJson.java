package org.unicode.cldr.json;

import static org.unicode.cldr.json.Ldml2JsonConverter.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.json.Ldml2JsonConverter.JSONSection;
import org.unicode.cldr.json.Ldml2JsonConverter.RunType;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CalculatedCoverageLevels;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.TempPrintWriter;
import org.unicode.cldr.util.Timer;

@CLDRTool(alias = "verifyjson", description = "Verify JSON matches XML")
public class VerifyJson {
    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private CLDRConfig config = CLDRConfig.getInstance();
    private CalculatedCoverageLevels coverage = CalculatedCoverageLevels.getInstance();
    private Options options;
    private final boolean VERBOSE = false;
    private final String DESTDIR;

    public VerifyJson(Options options) {
        this.options = options;
        // TODO CLDR-13978: verbose as an option
        this.DESTDIR = options.get("destdir").getValue();
    }

    public static class NestedJsonPath {
        final String leaf;
        final NestedJsonPath parent;
        final int n;

        // root
        private NestedJsonPath(final NestedJsonPath parent, final String child, final int n) {
            this.parent = parent;
            this.leaf = child;
            this.n = n;
        }

        static NestedJsonPath root() {
            return new NestedJsonPath(null, ".", 0);
        }

        NestedJsonPath of(final String child) {
            return new NestedJsonPath(this, child, n + 1);
        }

        @Override
        public final String toString() {
            if (parent == null) return leaf;
            final String parStr = parent.toString();
            if (parStr.equals(".")) return leaf;
            return parent.toString() + "." + leaf;
        }

        public NestedJsonPath of(int i) {
            return of("[" + Integer.toString(i) + "]");
        }

        /** Get leaf name of element n from the top. get(0) gets the root item, always null */
        public String get(int i) {
            if (i > n) return null; // out of range
            if (i == n) return leaf;
            return parent.get(i);
        }

        public int size() {
            return n + 1;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(GEAR_ICON + " " + Ldml2JsonConverter.class.getName() + " options:");
        final Options options =
                new Options(
                                "Usage: VerifyJson [OPTIONS]\n"
                                        + "This program validates that JSON data contains everything that is in XML.\n")
                        .add(
                                "destdir",
                                'd',
                                ".*",
                                CLDRPaths.GEN_DIRECTORY,
                                "Destination directory for json files to check, defaults to CldrUtility.GEN_DIRECTORY")
                        .add(
                                "match",
                                'm',
                                ".*",
                                ".*",
                                "Regular expression to define only specific locales or files to be generated")
                        .add(
                                "type",
                                't',
                                "(" + Ldml2JsonConverter.RunType.valueList() + ")",
                                "all",
                                "Type of CLDR data being tested, such as main, supplemental, or segments. All gets all.")
                        .add(
                                "draftstatus",
                                's',
                                "(approved|contributed|provisional|unconfirmed)",
                                "unconfirmed",
                                "The minimum draft status of the output data")
                        .add(
                                "coverage",
                                'l',
                                "(minimal|basic|moderate|modern|comprehensive|optional)",
                                "optional",
                                "The maximum coverage level of the output data")
                        .add(
                                "fullnumbers",
                                'n',
                                "(true|false)",
                                "false",
                                "Whether the output JSON should output data for all numbering systems, even those not used in the locale");

        options.parse(args, true);

        VerifyJson v = new VerifyJson(options);

        Timer overallTimer = new Timer();
        overallTimer.start();
        final String rawType = options.get("type").getValue();

        if (RunType.all.name().equals(rawType)) {
            // Running all types
            for (final RunType t : RunType.values()) {
                if (t == RunType.all) continue;
                System.out.println();
                System.out.println(
                        TYPE_ICON + "#######################  " + t + " #######################");
                Timer subTimer = new Timer();
                subTimer.start();
                v.processType(t.name());
                System.out.println(
                        TYPE_ICON + " " + t + "\tFinished in " + subTimer.toMeasureString());
                System.out.println();
            }
        } else {
            v.processType(rawType);
        }

        System.out.println(
                "\n\n###\n\n"
                        + DONE_ICON
                        + " Finished everything in "
                        + overallTimer.toMeasureString());
    }

    private void processType(String type) {
        processType(RunType.valueOf(type));
    }

    private void processType(RunType type) {
        if (type == RunType.main) {
            System.out.println("Locale: " + " - " + type);
            processMain();
        } else {
            System.out.println(NONE_ICON + " - skipping unsupported " + type);
        }
    }

    private void processMain() {
        final Pattern p = Pattern.compile(options.get("match").getValue());
        for (final CLDRLocale l : config.getCldrFactory().getAvailableCLDRLocales()) {
            if (!coverage.isLocaleReallyAtLeastBasic(l.getBaseName())) {
                continue; // skip sub-basic
            }
            if (!p.matcher(l.getBaseName()).matches()) {
                continue;
            }
            try {
                processMainLocale(l);
            } catch (Throwable t) {
                System.err.println("FAIL: " + t + " for " + l);
            }
        }
    }

    // TODO CLDR-13978: print these on exit.
    private Set<String> missingDirs = new HashSet<>();
    // TODO CLDR-13978: fail if this is an empty set (i.e. nothing read)
    private Set<String> foundDirs = new HashSet<>();

    private void processMainLocale(CLDRLocale l) throws Throwable {
        // TODO CLDR-13978:type could be a param
        final RunType type = RunType.main;
        final LdmlConfigFileReader configFile = LdmlConfigFileReader.getInstance(type);
        final Collection<String> packages = configFile.getPackages();
        final Collection<JSONSection> sections = configFile.getSections();
        System.out.println(" " + l);
        final CLDRFile f = config.getCldrFactory().make(l.getBaseName(), true);
        // values we've already seen
        final Set<String> seenValues = new HashSet<>();
        final Set<String> readFiles = new TreeSet<>();
        for (final String p : packages) {
            String n = p;
            if (RunType.main.tiered()) {
                n = CLDR_PKG_PREFIX + n + FULL_TIER_SUFFIX;
            }
            final String dirName =
                    DESTDIR + "/cldr-json/" + n + "/" + type.name() + "/" + l.toLanguageTag() + "/";
            if (!Files.isDirectory(Path.of(dirName))) {
                missingDirs.add(dirName);
                continue;
            } else {
                foundDirs.add(dirName);
            }
            for (final JSONSection s : sections) {
                // not every section is in every package. so just try them all
                // TODO CLDR-13978: try s.package as a filter
                final String jsonName = dirName + s.section + ".json";
                try (JsonReader json = new JsonReader(new FileReader(jsonName)); ) {
                    readFiles.add(jsonName);
                    extractJsonDocument(json, seenValues);
                } catch (FileNotFoundException fnf) {
                    // ... ignored, just skip a missing file
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.err.println("Some other error on " + jsonName);
                }
            }
        }

        if (foundDirs.isEmpty() || readFiles.isEmpty()) {
            System.err.println("Did not read any files. Check paths: " + missingDirs.toString());
            System.exit(1); // TODO CLDR-13978: improve failure
        }

        System.err.println(
                "# for "
                        + type
                        + " / "
                        + l
                        + " Read dirs: "
                        + foundDirs.size()
                        + ", Read files: "
                        + readFiles.size());

        Set<String> missingValues = new HashSet<>();
        Set<String> activeNumberingSystems = Ldml2JsonConverter.getActiveNumberingSystems(f);
        // TODO CLDR-13978: COPYPASTA from Ldml2JsonConverter
        Matcher noNumberingSystemMatcher = LdmlConvertRules.NO_NUMBERING_SYSTEM_PATTERN.matcher("");
        Matcher numberingSystemMatcher = LdmlConvertRules.NUMBERING_SYSTEM_PATTERN.matcher("");
        Matcher rootIdentityMatcher = LdmlConvertRules.ROOT_IDENTITY_PATTERN.matcher("");
        Matcher versionMatcher = LdmlConvertRules.VERSION_PATTERN.matcher("");

        // collect missing paths in order, so we can report them.
        Set<String> missingXpaths = new TreeSet<>();

        for (final String xpath : f.iterableWithoutExtras()) {
            final String fullPath = f.getFullXPath(xpath);

            final CLDRFile file = f;
            final boolean resolve = true;
            final boolean fullNumbers = false;

            // TODO CLDR-13978: COPYPASTA from Ldml2JsonConverter

            // TODO: CLDR-17790 known issue
            // ldml/identity inherits when it shouldn't.
            rootIdentityMatcher.reset(fullPath);
            if (rootIdentityMatcher.matches() /* && !file.isHere(fullPath) */) {
                continue;
            }

            // discard version stuff
            versionMatcher.reset(fullPath);
            if (versionMatcher.matches()) {
                // drop //ldml/identity/version entirely.
                continue;
            }

            // automatically filter out number symbols and formats without a numbering system
            noNumberingSystemMatcher.reset(fullPath);
            if (noNumberingSystemMatcher.matches()) {
                continue;
            }

            // Filter out non-active numbering systems data unless fullNumbers is specified.
            numberingSystemMatcher.reset(fullPath);
            if (numberingSystemMatcher.matches() && !fullNumbers) {
                final String currentNS = getNumberingSystem(fullPath);
                if (currentNS != null && !activeNumberingSystems.contains(currentNS)) {
                    continue;
                }
            }

            if (xpath.endsWith("/alias")) continue;
            if (Ldml2JsonConverter.isFallbackValue(f, xpath)) continue;
            final String v = f.getStringValue(xpath);

            // Handle the no inheritance marker.
            if (resolve && CldrUtility.NO_INHERITANCE_MARKER.equals(v)) {
                continue;
            }
            if (seenValues.contains(v)) continue; // already processed
            if (missingValues.contains(v)) continue; // already complained
            if (VERBOSE) System.err.println(xpath + " = " + v + " - missing");
            missingValues.add(v);
            missingXpaths.add(xpath);
        }
        if (!missingValues.isEmpty()) {
            System.err.println(" Missing value count: " + missingValues.size());
        } else {
            System.out.println(" - no missing values");
        }
        writeMissingReport(type, l, f, missingXpaths, missingValues);
    }

    void writeMissingReport(
            RunType type,
            CLDRLocale locale,
            CLDRFile file,
            Collection<String> missingXpaths,
            Collection<String> missingValues) {
        File chartDir = new File(DESTDIR + "/missing");
        chartDir.mkdirs();

        File outFile =
                new File(
                        chartDir,
                        String.format("missing-%s-%s.md", type.name(), locale.getBaseName()));

        try (TempPrintWriter out = new TempPrintWriter(outFile)) {
            out.println(
                    String.format(
                            "# Missing XPath report — %s / %s (%s)",
                            type.name(), locale.getBaseName(), locale.getDisplayName()));
            out.println("");
            out.println("## Statistics");
            out.println("");
            out.println(String.format("- Missing values: %d", missingValues.size()));
            // TODO CLDR-13978: include other params such as draftStatus
            out.println("");
            out.println("## Missing values");
            out.println("");
            out.println("_Sorted by example XPath. Only one XPath is shown per value._");
            out.println("");
            for (final String xpath : missingXpaths) {
                final String v = file.getStringValue(xpath);
                out.println(String.format(" - **%s**", v));
                out.println(String.format("   `%s`", xpath));
                out.println();
            }
        }
        System.out.println("# Wrote: " + outFile);
    }

    void extractJsonDocument(JsonReader json, Set<String> seenValues) throws IOException {
        extractJsonObject(json, seenValues, NestedJsonPath.root());
    }

    void extractJsonObject(JsonReader json, Set<String> seenValues, final NestedJsonPath parentKey)
            throws IOException {
        json.beginObject();

        String key = null;
        while (json.hasNext()) {

            switch (json.peek()) {
                case JsonToken.NAME:
                    key = json.nextName();
                    break;

                case JsonToken.END_OBJECT:
                    json.endObject();
                    key = null;
                    /* NOTREACHED */
                    return;

                case JsonToken.BEGIN_OBJECT:
                    // it's a sub object.
                    extractJsonObject(json, seenValues, parentKey.of(key));
                    key = null;
                    break;
                case JsonToken.BEGIN_ARRAY:
                    // it's a sub array.
                    extractJsonArray(json, seenValues, parentKey.of(key));
                    key = null;
                    break;

                case JsonToken.STRING:
                    extractJsonString(json, seenValues, parentKey.of(key));
                    key = null;
                    break;

                default:
                case JsonToken.NULL:
                case JsonToken.NUMBER:
                case JsonToken.BOOLEAN:
                    // skip all of these.
                    json.skipValue();
                    key = null;
            }
        }
        json.endObject();
    }

    void extractJsonArray(JsonReader json, Set<String> seenValues, NestedJsonPath parentKey)
            throws IOException {
        json.beginArray();
        int n = 0;
        while (json.hasNext()) {
            switch (json.peek()) {
                case JsonToken.NAME:
                    // shouldn't happen, this is an array
                    json.nextName();
                    break;

                case JsonToken.END_ARRAY:
                    json.endArray();
                    /* NOTREACHED */
                    return;

                case JsonToken.BEGIN_OBJECT:
                    // it's a sub object.
                    extractJsonObject(json, seenValues, parentKey.of(n++));
                    break;
                case JsonToken.BEGIN_ARRAY:
                    // it's a sub array.
                    extractJsonArray(json, seenValues, parentKey.of(n++));
                    break;

                case JsonToken.STRING:
                    extractJsonString(json, seenValues, parentKey.of(n++));
                    break;

                default:
                case JsonToken.NULL:
                case JsonToken.NUMBER:
                case JsonToken.BOOLEAN:
                    n++;
                    // skip all of these.
                    json.skipValue();
            }
        }
        json.endArray();
    }

    void extractJsonString(JsonReader json, Set<String> seenValues, NestedJsonPath parentKey)
            throws IOException {
        // read nextstr
        final String str = json.nextString();

        // skip if in identity
        if ("identity".equals(parentKey.get(3))) return; // main.mt.identity = skip.

        seenValues.add(str);
    }
}
