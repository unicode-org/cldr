package org.unicode.cldr.draft.keyboard.osx;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.unicode.cldr.draft.keyboard.ModifierKeyCombinationSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public final class KeylayoutParser {
    private static final Pattern INVALID_UNICODE_XML = Pattern.compile("&#x00([01][0-9a-fA-F]);");
    private static final KeycodeMap KEYCODE_MAP = KeycodeMap.fromResource(KeylayoutParser.class,
        "osx-keycodes.csv");
    public static final KeyboardIdMap KEYBOARD_ID_MAP = KeyboardIdMap.fromResource(KeylayoutParser.class,
        "osx-locales.csv", Platform.OSX);

    private final KeyboardBuilder builder;
    private final Element keyboardElement;
    private final Map<String, ModifierKeyCombinationSet> mapIndexToModifier = Maps.newHashMap();

    private KeylayoutParser(Element keyboardElement) {
        this.keyboardElement = checkNotNull(keyboardElement);
        builder = new KeyboardBuilder();
    }

    public static ImmutableList<Keyboard> parseLayout(String keylayoutContents) {
        String cleansedString = keylayoutContents
            .replace("\u007f", "")
            .replace("&#x007f;", "")
            .replace("&#x007F;", "")
            .replace("\"<\"", "\"&lt;\"")
            .replace("\">\"", "\"&gt;\"")
            .replace("\"\"\"", "\"&quot;\"")
            .replace("\"&\"", "\"&amp;\"")
            .replace("\"'\"", "\"&apos;\"");
        cleansedString = INVALID_UNICODE_XML.matcher(cleansedString).replaceAll("");
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new ByteArrayInputStream(
                cleansedString.toString().getBytes(Charsets.US_ASCII))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
        KeylayoutParser parser = new KeylayoutParser(document.getDocumentElement());
        return parser.parse();
    }

    /** Parse the given keylayout contents and populate the keyboard builder. */
    private ImmutableList<Keyboard> parse() {
        parseId();
        Element layout = findLayout();
        String modifierId = layout.getAttribute("modifiers");
        parseModifiers(modifierId);
        String mapSet = layout.getAttribute("mapSet");
        parseKeyMaps(mapSet);
        return builder.build();
    }

    private void parseId() {
        String name = keyboardElement.getAttribute("name");
        checkArgument(!name.isEmpty());
        builder.addName(name);
        ImmutableCollection<KeyboardId> ids = KEYBOARD_ID_MAP.getKeyboardId(name);
        builder.addKeyboardIds(ids);
    }

    private Element findLayout() {
        NodeList layouts = keyboardElement.getElementsByTagName("layout");
        for (int i = 0; i < layouts.getLength(); i++) {
            Element layout = (Element) layouts.item(i);
            if (layout.getAttribute("first").equals("0") || layout.getAttribute("first").equals("1")) {
                return layout;
            }
        }
        throw new IllegalArgumentException("No layout element containing first='0'");
    }

    private void parseModifiers(String modifierId) {
        checkArgument(!modifierId.isEmpty());
        NodeList modifierMaps = keyboardElement.getElementsByTagName("modifierMap");
        Element modifierMap = null;
        for (int i = 0; i < modifierMaps.getLength(); i++) {
            Element modifierMapElement = (Element) modifierMaps.item(i);
            if (modifierMapElement.getAttribute("id").equals(modifierId)) {
                modifierMap = modifierMapElement;
            }
        }
        checkNotNull(modifierMap);
        NodeList keyMapSelects = modifierMap.getElementsByTagName("keyMapSelect");
        for (int i = 0; i < keyMapSelects.getLength(); i++) {
            Element keyMapSelect = (Element) keyMapSelects.item(i);
            NodeList modifiers = keyMapSelect.getElementsByTagName("modifier");
            ImmutableSet.Builder<ModifierKeyCombination> combinations = ImmutableSet.builder();
            for (int j = 0; j < modifiers.getLength(); j++) {
                Element modifier = (Element) modifiers.item(j);
                String keys = modifier.getAttribute("keys");
                combinations.add(parseKeys(keys));
            }
            String mapIndex = keyMapSelect.getAttribute("mapIndex");
            checkArgument(!mapIndex.isEmpty());
            mapIndexToModifier.put(mapIndex, ModifierKeyCombinationSet.of(combinations.build()));
        }
    }

    private static final Splitter WHITESPACE_SPLITTER = Splitter.on(CharMatcher.whitespace())
        .omitEmptyStrings();

    private static ModifierKeyCombination parseKeys(String keys) {
        ImmutableSet.Builder<ModifierKey> onKeys = ImmutableSet.builder();
        ImmutableSet.Builder<ModifierKey> dontCareKeys = ImmutableSet.builder();
        for (String key : WHITESPACE_SPLITTER.splitToList(keys)) {
            boolean maybe = key.contains("?");
            key = maybe ? key.substring(0, key.length() - 1) : key;
            ImmutableSet.Builder<ModifierKey> builder = maybe ? dontCareKeys : onKeys;
            if (key.equals("command")) {
                builder.add(ModifierKey.COMMAND);
            } else if (key.equals("caps")) {
                builder.add(ModifierKey.CAPSLOCK);
            } else if (key.equals("anyShift")) {
                builder.add(ModifierKey.SHIFT);
            } else if (key.equals("shift")) {
                builder.add(ModifierKey.SHIFT_LEFT);
            } else if (key.equals("rightShift")) {
                builder.add(ModifierKey.SHIFT_RIGHT);
            } else if (key.equals("anyOption")) {
                builder.add(ModifierKey.OPTION);
            } else if (key.equals("option")) {
                builder.add(ModifierKey.OPTION_LEFT);
            } else if (key.equals("rightOption")) {
                builder.add(ModifierKey.OPTION_RIGHT);
            } else if (key.equals("anyControl")) {
                builder.add(ModifierKey.CONTROL);
            } else if (key.equals("control")) {
                builder.add(ModifierKey.CONTROL_LEFT);
            } else if (key.equals("rightControl")) {
                builder.add(ModifierKey.CONTROL_RIGHT);
            } else {
                throw new IllegalArgumentException(key);
            }
        }
        return ModifierKeyCombination.ofOnAndDontCareKeys(onKeys.build(), dontCareKeys.build());
    }

    private void parseKeyMaps(String mapSet) {
        NodeList keyMapSets = keyboardElement.getElementsByTagName("keyMapSet");
        Element keyMapSet = null;
        for (int i = 0; i < keyMapSets.getLength(); i++) {
            Element keyMapSetElement = (Element) keyMapSets.item(i);
            if (keyMapSetElement.getAttribute("id").equals(mapSet)) {
                keyMapSet = keyMapSetElement;
            }
        }
        checkNotNull(keyMapSet);
        NodeList keyMaps = keyMapSet.getElementsByTagName("keyMap");
        for (int i = 0; i < keyMaps.getLength(); i++) {
            Element keyMap = (Element) keyMaps.item(i);
            String modifierIndex = keyMap.getAttribute("index");
            ModifierKeyCombinationSet combinationSet = mapIndexToModifier.get(modifierIndex);
            checkNotNull(combinationSet, modifierIndex);
            NodeList keys = keyMap.getElementsByTagName("key");
            for (int j = 0; j < keys.getLength(); j++) {
                Element key = (Element) keys.item(j);
                if (!key.getAttribute("code").matches("[0-9]+")) {
                    throw new IllegalArgumentException(key.getAttribute("code"));
                }
                Integer code = Integer.valueOf(key.getAttribute("code"));
                if (!KEYCODE_MAP.hasIsoLayoutPosition(code)) {
                    continue;
                }
                IsoLayoutPosition position = KEYCODE_MAP.getIsoLayoutPosition(code);
                CharacterMap characterMap = CharacterMap.of(position, key.getAttribute("output"));
                for (ModifierKeyCombination combination : combinationSet.combinations()) {
                    builder.addCharacterMap(combination, characterMap);
                }
            }
        }
    }
}
