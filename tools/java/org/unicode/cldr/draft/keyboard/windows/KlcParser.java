package org.unicode.cldr.draft.keyboard.windows;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.keyboard.CharacterMap;
import org.unicode.cldr.draft.keyboard.IsoLayoutPosition;
import org.unicode.cldr.draft.keyboard.Keyboard;
import org.unicode.cldr.draft.keyboard.KeyboardBuilder;
import org.unicode.cldr.draft.keyboard.KeyboardId;
import org.unicode.cldr.draft.keyboard.KeyboardId.Platform;
import org.unicode.cldr.draft.keyboard.KeyboardIdMap;
import org.unicode.cldr.draft.keyboard.KeycodeMap;
import org.unicode.cldr.draft.keyboard.ModifierKey;
import org.unicode.cldr.draft.keyboard.ModifierKeyCombination;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

/**
 * This class allows the parsing of Windows source keyboard files which are created by the Windows
 * Keyboard Layout Creator application. These files are simply text files divided into sections.
 *
 * <p>
 * A typical layout file has multiple sections (always in the same order) with each section headed
 * by a title in capital letters (eg. "LAYOUT"). Each of these sections are parsed independently of
 * the others.
 */
public final class KlcParser {
    public static final KeycodeMap KEYCODE_MAP = KeycodeMap.fromResource(KlcParser.class,
        "windows-keycodes.csv");
    public static final KeyboardIdMap KEYBOARD_ID_MAP = KeyboardIdMap.fromResource(KlcParser.class,
        "windows-locales.csv", Platform.WINDOWS);

    private static final Splitter WHITESPACE_SPLITTER = Splitter.on(CharMatcher.whitespace())
        .omitEmptyStrings();
    private static final Splitter LINE_SPLITTER = Splitter.on(CharMatcher.anyOf("\n\r"))
        .omitEmptyStrings();

    private final KeyboardBuilder builder;
    private final String klcContents;
    private final Table<String, Integer, String> virtualKeyAndIndexPlaceToLigature;
    private final Multimap<Integer, ModifierKeyCombination> indexToModifierKeyCombinations;
    // Sorted list of the modifier indexes. Used when parsing the layout.
    private final List<Integer> modifierIndexes;
    private int shiftIndex;

    private KlcParser(String klcContents) {
        this.klcContents = checkNotNull(klcContents);
        builder = new KeyboardBuilder();
        virtualKeyAndIndexPlaceToLigature = HashBasedTable.create();
        indexToModifierKeyCombinations = ArrayListMultimap.create();
        modifierIndexes = Lists.newArrayList();
    }

    public static ImmutableList<Keyboard> parseLayout(String klcLayout) {
        KlcParser parser = new KlcParser(klcLayout);
        return parser.parse();
    }

    /** Parse the given klc layout contents and populate the keyboard builder. */
    private ImmutableList<Keyboard> parse() {
        parseNameAndId();
        parseModifiers();
        parseLigatures();
        // Dead keys has to come before layout since they are needed in the layout process.
        parseDeadkeys();
        parseLayout();
        return builder.build();
    }

    private void parseNameAndId() {
        // The descriptions section (containing the name) is bounded below by the languagenames section.
        int descriptionsIndex = klcContents.indexOf("\nDESCRIPTIONS");
        int languageNamesIndex = klcContents.indexOf("\nLANGUAGENAMES");
        String section = klcContents.substring(descriptionsIndex, languageNamesIndex);
        List<String> lines = LINE_SPLITTER.splitToList(section);
        checkArgument(lines.size() == 2, section);
        // The name line always starts with a 4 digit number, then a tab followed by the actual name.
        // eg. "0409   Canadian French - Custom"
        String name = lines.get(1).substring(5).replace(" - Custom", "").trim();
        checkArgument(!name.isEmpty(), section);
        builder.addName(name);
        ImmutableCollection<KeyboardId> ids = KEYBOARD_ID_MAP.getKeyboardId(name);
        builder.addKeyboardIds(ids);
    }

    private void parseModifiers() {
        // The modifiers section (shiftstate) is always bounded below by the layout section.
        int shiftStateIndex = klcContents.indexOf("\nSHIFTSTATE");
        int layoutIndex = klcContents.indexOf("\nLAYOUT");
        String section = klcContents.substring(shiftStateIndex, layoutIndex);
        List<String> lines = LINE_SPLITTER.splitToList(section);
        for (int i = 1; i < lines.size(); i++) {
            List<String> components = WHITESPACE_SPLITTER.splitToList(lines.get(i));
            Integer index = Integer.valueOf(components.get(0));
            Set<ModifierKey> modifiers = Sets.newHashSet();
            for (int j = 1; j < components.size(); j++) {
                String token = components.get(j);
                if (token.equals("Shft")) {
                    modifiers.add(ModifierKey.SHIFT);
                } else if (token.equals("Alt")) {
                    modifiers.add(ModifierKey.ALT);
                } else if (token.equals("Ctrl")) {
                    modifiers.add(ModifierKey.CONTROL);
                }
            }
            ImmutableSet<ModifierKey> dontCareKeys = ImmutableSet.of();
            // Exception: If either Ctrl or Alt or both are present in a combination, add Caps as a
            // don't care key
            if (modifiers.contains(ModifierKey.ALT) || modifiers.contains(ModifierKey.CONTROL)) {
                dontCareKeys = ImmutableSet.of(ModifierKey.CAPSLOCK);
            }
            modifierIndexes.add(index);
            ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(modifiers,
                dontCareKeys);
            indexToModifierKeyCombinations.put(index, combination);
            if (combination.equals(SHIFT)) {
                shiftIndex = index;
            }
            // Exception: If both Ctrl and Alt are present in a combination then a second equivalent
            // combination of "AltR+Caps?" is added to the same index
            if (modifiers.contains(ModifierKey.ALT) && modifiers.contains(ModifierKey.CONTROL)) {
                Set<ModifierKey> keys = Sets.newHashSet(modifiers);
                keys.remove(ModifierKey.ALT);
                keys.remove(ModifierKey.CONTROL);
                keys.add(ModifierKey.ALT_RIGHT);
                ModifierKeyCombination rAltCombination = ModifierKeyCombination.ofOnAndDontCareKeys(keys,
                    ImmutableSet.of(ModifierKey.CAPSLOCK));
                indexToModifierKeyCombinations.put(index, rAltCombination);
            }
        }
    }

    private int findLigatureEndIndex() {
        int keynameIndex = klcContents.indexOf("\nKEYNAME");
        keynameIndex = keynameIndex == -1 ? Integer.MAX_VALUE : keynameIndex;
        int deadkeyIndex = klcContents.indexOf("\nDEADKEY");
        deadkeyIndex = deadkeyIndex == -1 ? Integer.MAX_VALUE : deadkeyIndex;
        return Math.min(keynameIndex, deadkeyIndex);
    }

    private void parseLigatures() {
        // The ligature section is always bounded below by the keyname or deadkey sections.
        int ligatureIndex = klcContents.indexOf("\nLIGATURE\r");
        if (ligatureIndex == -1) {
            return;
        }
        int endIndex = findLigatureEndIndex();
        String section = klcContents.substring(ligatureIndex, endIndex);
        List<String> lines = LINE_SPLITTER.splitToList(section);
        // Ignore the first line as it only contains the title.
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).startsWith("//")) {
                continue;
            }
            List<String> components = WHITESPACE_SPLITTER.splitToList(lines.get(i));
            String virtualKey = components.get(0);
            Integer modifierIndexPlace = Integer.valueOf(components.get(1));
            StringBuilder ligature = new StringBuilder();
            for (int j = 2; j < components.size(); j++) {
                String value = components.get(j);
                if (value.startsWith("//")) {
                    break;
                }
                ligature.append(convertHexValueToString(value));
            }
            String ligatureValue = ligature.toString();
            checkArgument(!ligatureValue.isEmpty(), section);
            virtualKeyAndIndexPlaceToLigature.put(virtualKey, modifierIndexPlace, ligature.toString());
        }
    }

    private int findLayoutEndIndex() {
        int deadKeyIndex = klcContents.indexOf("\nDEADKEY");
        deadKeyIndex = deadKeyIndex == -1 ? Integer.MAX_VALUE : deadKeyIndex;
        int keynameIndex = klcContents.indexOf("\nKEYNAME");
        keynameIndex = keynameIndex == -1 ? Integer.MAX_VALUE : keynameIndex;
        int ligatureIndex = klcContents.indexOf("\nLIGATURE");
        ligatureIndex = ligatureIndex == -1 ? Integer.MAX_VALUE : ligatureIndex;
        return Math.min(Math.min(deadKeyIndex, keynameIndex), ligatureIndex);
    }

    private static final ModifierKeyCombination SHIFT = ModifierKeyCombination.ofOnKeys(
        ImmutableSet.of(ModifierKey.SHIFT));

    private static final ModifierKeyCombination CAPSLOCK = ModifierKeyCombination.ofOnKeys(
        ImmutableSet.of(ModifierKey.CAPSLOCK));

    private static final ModifierKeyCombination SHIFT_CAPSLOCK = ModifierKeyCombination.ofOnKeys(
        ImmutableSet.of(ModifierKey.CAPSLOCK, ModifierKey.SHIFT));

    private void parseLayout() {
        // The layout section is bounded from below by the deadkey, keyname or ligature section.
        int layoutIndex = klcContents.indexOf("\nLAYOUT");
        int endIndex = findLayoutEndIndex();
        String section = klcContents.substring(layoutIndex, endIndex);
        List<String> lines = LINE_SPLITTER.splitToList(section);
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).startsWith("//")) {
                continue;
            }
            List<String> components = WHITESPACE_SPLITTER.splitToList(lines.get(i));
            Integer scanCode = Integer.valueOf(components.get(0), 16);
            // Delete key.
            if (scanCode == 0x53 || scanCode == 0x7E) {
                continue;
            }
            IsoLayoutPosition position = KEYCODE_MAP.getIsoLayoutPosition(scanCode);
            Map<Integer, CharacterMap> modifierIndexToCharacterMap = Maps.newHashMap();
            int index = 0;
            for (String output : Iterables.skip(components, 3)) {
                if (output.equals("//")) {
                    break;
                }
                Integer modifierIndex = modifierIndexes.get(index++);
                Collection<ModifierKeyCombination> combinations = indexToModifierKeyCombinations.get(modifierIndex);
                if (output.equals("-1")) {
                    continue;
                }
                String character;
                boolean deadKey = false;
                if (output.equals("%%")) {
                    String virtualKey = components.get(1);
                    // Getting ligatures is always based on the index of the modifier index. (The position of
                    // the index in order).
                    Integer modifierIndexPlace = index - 1;
                    character = checkNotNull(
                        virtualKeyAndIndexPlaceToLigature.get(virtualKey, modifierIndexPlace),
                        "[Modifier: %s, VirtualKey: %s]", modifierIndexPlace, virtualKey,
                        virtualKeyAndIndexPlaceToLigature);
                } else {
                    // An @ appended to the output value indicates that this is a deadkey.
                    deadKey = output.endsWith("@");
                    output = deadKey ? output.substring(0, output.length() - 1) : output;
                    character = convertHexValueToString(output);
                }
                CharacterMap characterMap = CharacterMap.of(position, character);
                if (!deadKey && startsTransformSequence(character)) {
                    characterMap = characterMap.markAsTransformNo();
                }
                builder.addCharacterMap(combinations, characterMap);
                modifierIndexToCharacterMap.put(modifierIndex, characterMap);
            }
            String capsLock = components.get(2);
            if (capsLock.equals("1") || capsLock.equals("4") || capsLock.equals("5")) {
                // Integer capsIndex = Integer.valueOf(components.get(2));
                // Add the base output to the caps+shift output
                if (modifierIndexToCharacterMap.containsKey(0)) {
                    builder.addCharacterMap(SHIFT_CAPSLOCK, modifierIndexToCharacterMap.get(0));
                }
                if (modifierIndexToCharacterMap.containsKey(shiftIndex)) {
                    builder.addCharacterMap(CAPSLOCK, modifierIndexToCharacterMap.get(shiftIndex));
                }
            } else if (capsLock.equals("SGCap")) {
                // Deal with the caps lock case.
                // The next line describes the outputs of the modifiers with the extra caps key.
                String nextLine = lines.get(++i);
                List<String> nextLineComponents = WHITESPACE_SPLITTER.splitToList(nextLine);
                int nextLineIndex = 0;
                for (String output : Iterables.skip(nextLineComponents, 3)) {
                    if (output.equals("//")) {
                        break;
                    }
                    Integer modifierIndex = modifierIndexes.get(nextLineIndex++);
                    ModifierKeyCombination combination = Iterables.getOnlyElement(indexToModifierKeyCombinations.get(modifierIndex));
                    ModifierKeyCombination capsCombination = ModifierKeyCombination.ofOnKeys(
                        ImmutableSet.copyOf(Iterables.concat(
                            combination.onKeys(), ImmutableSet.of(ModifierKey.CAPSLOCK))));
                    if (output.equals("-1")) {
                        continue;
                    }
                    String character = convertHexValueToString(output);
                    CharacterMap characterMap = CharacterMap.of(position, character);
                    builder.addCharacterMap(capsCombination, characterMap);
                }
            } else if (capsLock.equals("0")) {
                if (modifierIndexToCharacterMap.containsKey(0)) {
                    builder.addCharacterMap(CAPSLOCK, modifierIndexToCharacterMap.get(0));
                }
                if (modifierIndexToCharacterMap.containsKey(shiftIndex)) {
                    builder.addCharacterMap(SHIFT_CAPSLOCK, modifierIndexToCharacterMap.get(shiftIndex));
                }
            } else {
                throw new IllegalArgumentException(capsLock);
            }
        }
    }

    private void parseDeadkeys() {
        // Each deadkey section is bounded below by another deadkey section or the keyname section.
        int deadkeyIndex = klcContents.indexOf("\nDEADKEY");
        while (deadkeyIndex != -1) {
            int nextDeadkeySection = klcContents.indexOf("\nDEADKEY", deadkeyIndex + 1);
            int endIndex = nextDeadkeySection == -1 ? klcContents.indexOf("\nKEYNAME") : nextDeadkeySection;
            String section = klcContents.substring(deadkeyIndex, endIndex);
            checkArgument(!section.isEmpty(), klcContents);
            parseDeadkeySection(klcContents.substring(deadkeyIndex, endIndex));
            deadkeyIndex = nextDeadkeySection;
        }
    }

    private void parseDeadkeySection(String section) {
        List<String> lines = LINE_SPLITTER.splitToList(section);
        // The first line contains the actual deadkey. Eg. "DEADKEY 0060"
        String deadkey = convertHexValueToString(WHITESPACE_SPLITTER.splitToList(lines.get(0)).get(1));
        // The remaining lines include all possible combinations.
        for (int i = 1; i < lines.size(); i++) {
            List<String> components = WHITESPACE_SPLITTER.splitToList(lines.get(i));
            String combinationKey = convertHexValueToString(components.get(0));
            String output = convertHexValueToString(components.get(1));
            builder.addTransform(deadkey + combinationKey, output);
        }
    }

    private static final Pattern LETTER_OR_DIGIT = Pattern.compile("[0-9]|[a-z]|[A-Z]");

    private String convertHexValueToString(String hexValue) {
        // Some keyboards encode digits and letters as the character itself.
        if (LETTER_OR_DIGIT.matcher(hexValue).matches()) {
            return hexValue;
        }
        return Character.toString((char) Integer.valueOf(hexValue, 16).intValue());
    }

    private boolean startsTransformSequence(String string) {
        for (String sequence : builder.transformSequences()) {
            if (sequence.startsWith(string)) {
                return true;
            }
        }
        return false;
    }
}
