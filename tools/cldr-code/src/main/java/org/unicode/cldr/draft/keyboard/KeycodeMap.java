package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;

/**
 * An class which maps a hardware key code (the value that is sent from keyboard driver to the
 * application) to its actual iso layout position.
 */
public final class KeycodeMap {
    private final ImmutableSortedMap<Integer, IsoLayoutPosition> keycodeToIsoLayout;

    private KeycodeMap(ImmutableSortedMap<Integer, IsoLayoutPosition> keycodeToIsoLayout) {
        this.keycodeToIsoLayout = checkNotNull(keycodeToIsoLayout);
    }

    private static final Splitter LINE_SPLITTER = Splitter.on("\n").omitEmptyStrings();
    private static final Splitter COMMA_SPLITTER = Splitter.on(",");

    /**
     * Creates the mapping from csv contents. The first line must contain the column headers
     * "keycode,iso".
     */
    public static KeycodeMap fromCsv(String csv) {
        checkArgument(!csv.isEmpty());
        List<String> lines = LINE_SPLITTER.splitToList(csv);
        checkArgument(lines.get(0).equals("keycode,iso"), "Missing csv headers");
        ImmutableSortedMap.Builder<Integer, IsoLayoutPosition> builder = ImmutableSortedMap.naturalOrder();
        for (String line : Iterables.skip(lines, 1)) {
            // No fancy CSV parsing required since there are no strings.
            List<String> components = COMMA_SPLITTER.splitToList(line);
            builder.put(Integer.valueOf(components.get(0)), IsoLayoutPosition.valueOf(components.get(1)));
        }
        return new KeycodeMap(builder.build());
    }

    /** Retrieves the csv file relative to the class given. */
    public static KeycodeMap fromResource(Class<?> clazz, String fileName) {
        try {
            String csv = Resources.toString(Resources.getResource(clazz, fileName),
                Charsets.UTF_8);
            return fromCsv(csv);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public boolean hasIsoLayoutPosition(Integer keycode) {
        return keycodeToIsoLayout.containsKey(keycode);
    }

    public IsoLayoutPosition getIsoLayoutPosition(Integer keycode) {
        return checkNotNull(keycodeToIsoLayout.get(keycode), "No keycode for %s [%s]", keycode,
            keycodeToIsoLayout);
    }

    public ImmutableSortedMap<Integer, IsoLayoutPosition> keycodeToIsoLayout() {
        return keycodeToIsoLayout;
    }
}
