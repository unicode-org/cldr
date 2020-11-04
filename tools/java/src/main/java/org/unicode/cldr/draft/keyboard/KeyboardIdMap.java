package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.unicode.cldr.draft.keyboard.KeyboardId.Platform;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.ibm.icu.util.ULocale;

public final class KeyboardIdMap {
    private final ImmutableMultimap<String, KeyboardId> nameToKeyboardId;
    // Internal only.
    private final Set<String> coveredNames;

    private KeyboardIdMap(ImmutableMultimap<String, KeyboardId> nameToKeyboardId) {
        this.nameToKeyboardId = checkNotNull(nameToKeyboardId);
        coveredNames = Sets.newHashSet();
    }

    private static final Splitter LINE_SPLITTER = Splitter.on("\n").omitEmptyStrings();
    private static final Splitter COMMA_SPLITTER = Splitter.on(",");
    private static final Splitter DASH_SPLITTER = Splitter.on("-").omitEmptyStrings();

    /**
     * Creates the mapping from csv contents. The first line must contain the column headers
     * "name,locale,attributes".
     */
    public static KeyboardIdMap fromCsv(String csv, Platform platform) {
        checkArgument(!csv.isEmpty());
        List<String> lines = LINE_SPLITTER.splitToList(csv);
        checkArgument(lines.get(0).equals("name,locale,attributes"), "Missing csv headers");
        ImmutableMultimap.Builder<String, KeyboardId> builder = ImmutableMultimap.builder();
        for (String line : Iterables.skip(lines, 1)) {
            // The first element may be between quotes (if it includes a comma), if so parse it manually.
            String name;
            int closingQuote = line.startsWith("\"") ? line.indexOf("\"", 1) : 0;
            List<String> components = COMMA_SPLITTER.splitToList(line.substring(closingQuote));
            if (closingQuote != 0) {
                name = line.substring(1, closingQuote);
            } else {
                name = components.get(0);
            }
            ULocale locale = ULocale.forLanguageTag(components.get(1));
            ImmutableList<String> attributes = ImmutableList.copyOf(DASH_SPLITTER.splitToList(components.get(2)));
            builder.put(name, KeyboardId.of(locale, platform, attributes));
        }
        return new KeyboardIdMap(builder.build());
    }

    /** Retrieves the csv file relative to the class given. */
    public static KeyboardIdMap fromResource(Class<?> clazz, String fileName, Platform platform) {
        try {
            String csv = Resources.toString(Resources.getResource(clazz, fileName),
                Charsets.UTF_8);
            return fromCsv(csv, platform);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public ImmutableCollection<KeyboardId> getKeyboardId(String name) {
        coveredNames.add(name);
        ImmutableCollection<KeyboardId> ids = nameToKeyboardId.get(name);
        checkArgument(ids.size() > 0, "No keyboard id for %s [%s]", name, nameToKeyboardId);
        return ids;
    }

    public Set<String> unmatchedIds() {
        return Sets.difference(nameToKeyboardId.keySet(), coveredNames);
    }
}
