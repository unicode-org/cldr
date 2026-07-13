package org.unicode.cldr.tool;

import com.google.common.base.Splitter;
import com.ibm.icu.text.UnicodeSet;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.FileProcessor;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.StringId;

/** implementation for option -fk */
class CLDRConfigFileFilter extends CLDRModify.CLDRFilter {

    // pattern for a hex string id
    static final UnicodeSet HEX = new UnicodeSet("[a-fA-F0-9]").freeze();

    enum ConfigKeys {
        action,
        locale,
        path,
        value,
        new_path,
        new_value,
        draft
    }

    enum ConfigAction {
        /** Remove a path */
        delete,
        /** Add a path/value */
        add,
        /** Replace a path/value. Equals 'add' but tests selected paths */
        replace,
        /** Add a a path/value. Equals 'add' but tests that path did NOT exist */
        addNew,
        /** Copy a path to new_path */
        copy,
        /** Equals 'copy' but tsts that path did NOT exist. */
        copyNew
    }

    static final class ConfigMatch {
        final String exactMatch;
        final Matcher regexMatch; // doesn't have to be thread safe
        final ConfigAction action;
        final boolean hexPath;

        static UnicodeSet SUSPICIOUS_NON_REGEX = new UnicodeSet("[*|]").freeze();

        public ConfigMatch(ConfigKeys key, String match) {
            if (key == ConfigKeys.action) {
                exactMatch = null;
                regexMatch = null;
                action = ConfigAction.valueOf(match);
                hexPath = false;
            } else if (match.length() > 1 && match.startsWith("/") && match.endsWith("/")) {
                if (key != ConfigKeys.locale && key != ConfigKeys.path && key != ConfigKeys.value) {
                    throw new IllegalArgumentException(
                            "Regex only allowed for locale=, path=, or value'.");
                }
                exactMatch = null;
                // for convenience, we automatically change [@attr="something"] to
                // \[@attr="something"]
                regexMatch =
                        PatternCache.get(
                                        match.substring(1, match.length() - 1)
                                                .replace("[@", "\\[@"))
                                .matcher("");
                action = null;
                hexPath = false;
            } else {
                exactMatch = match;
                regexMatch = null;
                action = null;
                hexPath =
                        (key == ConfigKeys.new_path || key == ConfigKeys.path)
                                && HEX.containsAll(match);
                if (key == ConfigKeys.locale || key == ConfigKeys.path) {
                    if (SUSPICIOUS_NON_REGEX.containsSome(match)) {
                        System.out.println(
                                Joiners.ES.join(
                                        "The value ",
                                        match,
                                        " is being matched literally, but contains regex charcters. Did you mean /",
                                        match,
                                        "/ ?"));
                    }
                }
            }
        }

        public boolean matches(String other) {
            if (exactMatch == null) {
                return regexMatch.reset(other).find();
            } else if (hexPath) {
                // convert path to id for comparison
                return exactMatch.equals(StringId.getHexId(other));
            } else {
                return exactMatch.equals(other);
            }
        }

        @Override
        public String toString() {
            return action != null
                    ? action.toString()
                    : exactMatch == null
                            ? regexMatch.toString()
                            : hexPath ? "*" + exactMatch + "*" : exactMatch;
        }

        public String getPath(CLDRFile cldrFileToFilter) {
            if (!hexPath) {
                return exactMatch;
            }
            // ensure that we have all the possible paths cached
            String path = StringId.getStringFromHexId(exactMatch);
            if (path == null) {
                for (String eachPath : cldrFileToFilter.fullIterable()) {
                    StringId.getHexId(eachPath);
                }
                path = StringId.getStringFromHexId(exactMatch);
                if (path == null) {
                    throw new IllegalArgumentException("No path for hex id: " + exactMatch);
                }
            }
            return path;
        }

        public static String getModified(
                ConfigMatch valueMatch, String value, ConfigMatch newValue) {
            if (valueMatch == null) { // match anything
                if (newValue != null && newValue.exactMatch != null) {
                    return newValue.exactMatch;
                }
                if (value != null) {
                    return value;
                }
                throw new IllegalArgumentException("Can't have both old and new be null.");
            } else if (valueMatch.exactMatch == null) { // regex
                if (newValue == null || newValue.exactMatch == null) {
                    throw new IllegalArgumentException("Can't have regex without replacement.");
                }
                StringBuffer buffer = new StringBuffer();
                valueMatch.regexMatch.appendReplacement(buffer, newValue.exactMatch);
                return buffer.toString();
            } else {
                return newValue.exactMatch != null ? newValue.exactMatch : value;
            }
        }
    }

    static final String DEBUG_PATHS = null; // ".*currency.*";

    private Map<ConfigMatch, LinkedHashSet<Map<ConfigKeys, ConfigMatch>>> locale2keyValues;
    private LinkedHashSet<Map<ConfigKeys, ConfigMatch>> keyValues = new LinkedHashSet<>();
    final String DEBUG_PATH = null;
    private final Supplier<Boolean> configOptionChosen;
    private final Supplier<String> configFileName;

    public CLDRConfigFileFilter(
            Supplier<Boolean> configOptionChosen, Supplier<String> configFileName) {
        // Note, Supplier must be used because this is constructed at static init time.
        this.configOptionChosen = configOptionChosen;
        this.configFileName = configFileName;
    }

    @Override
    public void handleStart() {
        super.handleStart();
        if (!configOptionChosen.get()) {
            return;
        }
        if (locale2keyValues == null) {
            fillCache();
        }
        // set up for the specific locale we are dealing with.
        // a small optimization
        String localeId = getLocaleID();
        keyValues.clear();
        for (Entry<ConfigMatch, LinkedHashSet<Map<ConfigKeys, ConfigMatch>>> localeMatcher :
                locale2keyValues.entrySet()) {
            if (localeMatcher.getKey().matches(localeId)) {
                keyValues.addAll(localeMatcher.getValue());
            }
        }
        // System.out.println("# Checking entries & changing:\t" +
        // keyValues.size());
        for (Map<ConfigKeys, ConfigMatch> entry : keyValues) {
            ConfigMatch action = entry.get(ConfigKeys.action);
            ConfigMatch pathMatch = entry.get(ConfigKeys.path);
            ConfigMatch valueMatch = entry.get(ConfigKeys.value);
            ConfigMatch newPath = entry.get(ConfigKeys.new_path);
            ConfigMatch newValue = entry.get(ConfigKeys.new_value);

            CLDRModify.verboseln(
                    "Action=%s, pathMatch=%s, newPath=%s", action.action, pathMatch, newPath);

            switch (action.action) {
                // we add all the values up front
                case addNew:
                case add:
                    {
                        if (pathMatch != null
                                || valueMatch != null
                                || newPath == null
                                || newValue == null) {
                            throw new IllegalArgumentException(
                                    action.action
                                            + ": must have no path nor value = null AND new_path or new_value:\n\t"
                                            + entry);
                        }
                        String newPathString = newPath.getPath(getResolved());
                        if (action.action == ConfigAction.add
                                || cldrFileToFilter.getStringValue(newPathString) == null) {
                            replace(newPathString, newPathString, newValue.exactMatch, "config");
                        }
                    }
                    break;
                case copy:
                case copyNew:
                    {
                        // just check
                        if (pathMatch == null || newPath == null) {
                            throw new IllegalArgumentException(
                                    String.format(
                                            "%s: must have path and new_path", action.action));
                        }
                    }
                    break;
                // we just check
                case replace:
                    if ((pathMatch == null && valueMatch == null)
                            || (newPath == null && newValue == null)) {
                        throw new IllegalArgumentException(
                                action.action
                                        + ": must have (path or value) AND (new_path or new_value):\n\t"
                                        + entry);
                    }
                    break;
                // For delete, we just check; we'll remove later
                case delete:
                    if (newPath != null || newValue != null) {
                        throw new IllegalArgumentException(
                                action.action
                                        + ": must have no new_path nor new_value:\n\t"
                                        + entry);
                    }
                    break;
                default: // fall through
                    throw new IllegalArgumentException("Internal Error");
            }
        }
    }

    private static final Splitter SPLIT_ON_SEMI = Splitter.onPattern("\\s*;\\s+");

    private void fillCache() {
        locale2keyValues = new LinkedHashMap<>();
        FileProcessor myReader =
                new FileProcessor() {
                    {
                        doHash = false;
                    }

                    @Override
                    protected boolean handleLine(int lineCount, String line) {
                        line = line.trim();
                        Iterable<String> lineParts = SPLIT_ON_SEMI.split(line);
                        Map<ConfigKeys, ConfigMatch> keyValue = new EnumMap<>(ConfigKeys.class);
                        for (String linePart : lineParts) {
                            int pos = linePart.indexOf('=');
                            if (pos < 0) {
                                // WARNING; the code doesn't allow for ; within
                                // values; need to restructure for that.
                                throw new IllegalArgumentException(
                                        lineCount
                                                + ":\t No = in command: «"
                                                + linePart
                                                + "» in "
                                                + line);
                            }
                            ConfigKeys key = ConfigKeys.valueOf(linePart.substring(0, pos).trim());
                            if (keyValue.containsKey(key)) {
                                throw new IllegalArgumentException(
                                        "Must not have multiple keys: " + key);
                            }
                            String match = linePart.substring(pos + 1).trim();
                            keyValue.put(key, new ConfigMatch(key, match));
                        }
                        final ConfigMatch locale = keyValue.get(ConfigKeys.locale);
                        if (locale == null || keyValue.get(ConfigKeys.action) == null) {
                            throw new IllegalArgumentException();
                        }

                        // validate new path
                        LinkedHashSet<Map<ConfigKeys, ConfigMatch>> keyValues =
                                locale2keyValues.get(locale);
                        if (keyValues == null) {
                            locale2keyValues.put(locale, keyValues = new LinkedHashSet<>());
                        }
                        keyValues.add(keyValue);
                        return true;
                    }
                };
        myReader.process(CLDRModify.class, configFileName.get());
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    public void handlePath(String xpath) {
        // slow method; could optimize
        if (DEBUG_PATH != null && DEBUG_PATH.equals(xpath)) {
            System.out.println(xpath);
        }
        for (Map<ConfigKeys, ConfigMatch> entry : keyValues) {
            ConfigMatch pathMatch = entry.get(ConfigKeys.path);
            if (pathMatch != null && !pathMatch.matches(xpath)) {
                if (DEBUG_PATH != null && pathMatch != null && pathMatch.regexMatch != null) {
                    System.out.println(RegexUtilities.showMismatch(pathMatch.regexMatch, xpath));
                }
                continue;
            }
            ConfigMatch valueMatch = entry.get(ConfigKeys.value);
            final String value = cldrFileToFilter.getStringValue(xpath);
            if (valueMatch != null && !valueMatch.matches(value)) {
                continue;
            }
            ConfigMatch action = entry.get(ConfigKeys.action);
            switch (action.action) {
                case delete:
                    {
                        remove(xpath, "config");
                    }
                    break;
                case replace:
                    {
                        ConfigMatch newPath = entry.get(ConfigKeys.new_path);
                        ConfigMatch newValue = entry.get(ConfigKeys.new_value);

                        String fullpath = cldrFileToFilter.getFullXPath(xpath);
                        String draft = "";
                        int loc = fullpath.indexOf("[@draft=");
                        if (loc >= 0) {
                            int loc2 = fullpath.indexOf(']', loc + 7);
                            draft = fullpath.substring(loc, loc2 + 1);
                        }

                        String modPath = ConfigMatch.getModified(pathMatch, xpath, newPath) + draft;
                        String modValue = ConfigMatch.getModified(valueMatch, value, newValue);
                        replace(xpath, modPath, modValue, "config");
                    }
                    break;
                case copy:
                case copyNew:
                    {
                        // get out if there's no existing value
                        if (value == null) break;
                        ConfigMatch draft = entry.get(ConfigKeys.draft);
                        ConfigMatch newPath = entry.get(ConfigKeys.new_path);
                        final String oldNewPathString =
                                ConfigMatch.getModified(pathMatch, xpath, newPath);
                        String newPathString;
                        if (draft != null && !oldNewPathString.contains("[@draft=")) {
                            newPathString = oldNewPathString + "[@draft=\"" + draft + "\"]";
                        } else {
                            newPathString = oldNewPathString;
                        }
                        if (action.action == ConfigAction.copyNew
                                && cldrFileToFilter.isHere(oldNewPathString)) {
                            // the copyNew action skips if it's already here
                            break;
                        }
                        // TOOD: Allow skipping inheritance marker?
                        replace(
                                oldNewPathString,
                                newPathString,
                                value, // TODO: allow new_value to override with
                                // match
                                "config");
                    }
                    break;
            }
        }
    }
}
