package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Enum which represents the corresponding position of a key using the ISO layout convention where
 * rows are identified by letters and columns are identified by numbers. For example, "D01"
 * corresponds to the “Q” key on a US keyboard. For the purposes of this enum, we depict an ISO
 * layout position by a one-letter row identifier followed by a two digit column number (like "B03",
 * "E12" or "C00").
 *
 * <p>
 * It is important to note that the physical placement of the keys is not encoded in this enum,
 * rather what is important is their logical placement using the ISO convention.
 *
 * <p>
 * We can also extend the ISO Layout convention by adding rows as we please (such as adding an F
 * row) or adding columns (going beyond 13 or before 0, in which case we would introduce negative
 * column numbers). This extension can be used to map almost any key to the convention for our
 * purposes.
 *
 * <p>
 * More information about the ISO layout positions can be found in the <a
 * href="https://docs.google.com/document/d/1XSFyUKFGJr3lTv1mmoL4Pmk3RTJJapwYZ63pAkiIYvg/edit">LDML
 * XML Keyboard Specification</a>
 */
public enum IsoLayoutPosition {
    /* Row 1 */
    E00('E', 0, "`"), E01('E', 1, "1"), E02('E', 2, "2"), E03('E', 3, "3"), E04('E', 4, "4"), E05('E', 5, "5"), E06('E', 6, "6"), E07('E', 7, "7"), E08('E', 8,
        "8"), E09('E', 9, "9"), E10('E', 10, "0"), E11('E', 11, "-"), E12('E', 12, "="), E13('E', 13, "(key to right of =)"), // Additional key in 106 keyboards (like Japanese
    // keyboards)

    /* Row 2 */
    D01('D', 1, "Q"), D02('D', 2, "W"), D03('D', 3, "E"), D04('D', 4, "R"), D05('D', 5, "T"), D06('D', 6, "Y"), D07('D', 7, "U"), D08('D', 8, "I"), D09('D', 9,
        "O"), D10('D', 10, "P"), D11('D', 11, "["), D12('D', 12, "]"), D13('D', 13, "\\"),

    /* Row 3 */
    C01('C', 1, "A"), C02('C', 2, "S"), C03('C', 3, "D"), C04('C', 4, "F"), C05('C', 5, "G"), C06('C', 6, "H"), C07('C', 7, "J"), C08('C', 8, "K"), C09('C', 9,
        "L"), C10('C', 10, ";"), C11('C', 11, "'"), C12('C', 12, "(key to right of ')"), // Additional key in 102+ layouts, typically is present
    // when D13 is not

    /* Row 4 */
    B00('B', 0, "(key to left of Z)"), // Additional key in 102 and 103 keyboards (like European
    // keyboards)
    B01('B', 1, "Z"), B02('B', 2, "X"), B03('B', 3, "C"), B04('B', 4, "V"), B05('B', 5, "B"), B06('B', 6, "N"), B07('B', 7, "M"), B08('B', 8, ","), B09('B', 9,
        "."), B10('B', 10, "/"), B11('B', 11, "(key to right of /)"), B12('B', 12, "(2 keys to right of /)"), // Additional key for Android

    /* Row 5 */
    A01('A', 1, "(2 keys to left of space)"), // Additional key for Android
    A02('A', 2, "(key to left of space)"), // Additional key for Android
    A03('A', 3, "space"), A04('A', 4, "(key to right of space)"), // Additional key for Android
    A05('A', 5, "(2 keys to right of space)"), // Additional key for Android
    A06('A', 6, "(3 keys to right of space)"), // Additional key for Android
    A07('A', 7, "(4 keys to right of space)"); // Additional key for Android

    private final char row;
    private final int column;
    private final String englishKeyName;

    private IsoLayoutPosition(char row, int column, String englishKeyName) {
        this.row = row;
        this.column = column;
        this.englishKeyName = checkNotNull(englishKeyName);
    }

    public char row() {
        return row;
    }

    public int column() {
        return column;
    }

    /**
     * Get the label that would be on the key on a US keyboard. This is for convenience and
     * readability purposes only. If the key does not appear on a US keyboard, it returns a
     * description of the position relative to the closest US keyboard key.
     */
    public String englishKeyName() {
        return englishKeyName;
    }

    /**
     * Returns the enum member for a given row and column. Throws an illegal argument exception if
     * the element does not exist.
     *
     * @param row the layout row, is an upper-case character between A and E (inclusive)
     * @param column the layout column, is an integer between 0 and 13 (inclusive), not all rows
     *        contain elements for all 14 columns
     */
    public static IsoLayoutPosition forPosition(char row, int column) {
        for (IsoLayoutPosition position : values()) {
            if (position.row == row && position.column == column) {
                return position;
            }
        }
        throw new IllegalArgumentException("Missing ISO Position for " + row + ":" + column);
    }
}