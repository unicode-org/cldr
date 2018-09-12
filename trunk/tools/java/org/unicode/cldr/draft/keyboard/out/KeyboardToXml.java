package org.unicode.cldr.draft.keyboard.out;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Writer;

import org.unicode.cldr.draft.keyboard.CharacterMap;
import org.unicode.cldr.draft.keyboard.IsoLayoutPosition;
import org.unicode.cldr.draft.keyboard.KeyMap;
import org.unicode.cldr.draft.keyboard.Keyboard;
import org.unicode.cldr.draft.keyboard.KeyboardId.Platform;
import org.unicode.cldr.draft.keyboard.KeyboardSettings.FallbackSetting;
import org.unicode.cldr.draft.keyboard.KeyboardSettings.TransformFailureSetting;
import org.unicode.cldr.draft.keyboard.KeyboardSettings.TransformPartialSetting;
import org.unicode.cldr.draft.keyboard.ModifierKeyCombinationSet;
import org.unicode.cldr.draft.keyboard.Transform;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.text.UnicodeSet;

public final class KeyboardToXml {
    private final Keyboard keyboard;
    private final XmlWriter xmlWriter;

    private KeyboardToXml(Keyboard keyboard, XmlWriter xmlWriter) {
        this.keyboard = checkNotNull(keyboard);
        this.xmlWriter = checkNotNull(xmlWriter);
    }

    /** Writes the given keyboard map in XML format to the provided writer. */
    public static void writeToXml(Keyboard keyboard, Writer writer) {
        XmlWriter xmlWriter = XmlWriter.newXmlWriter(writer);
        KeyboardToXml keyboardToXml = new KeyboardToXml(keyboard, xmlWriter);
        keyboardToXml.toXml();
    }

    private void toXml() {
        xmlWriter.startDocument("keyboard", "../dtd/ldmlKeyboard.dtd");
        xmlWriter.startElement("keyboard", ImmutableMap.of("locale", keyboard.keyboardId()));
        addMetadata();
        addKeyMaps();
        addTransforms();
        xmlWriter.endElement();
        xmlWriter.endDocument();
    }

    private static DecimalFormat VERSION_FORMAT = new DecimalFormat("#.#");

    private void addMetadata() {
        Platform platform = keyboard.keyboardId().platform();
        ImmutableMap<String, String> versionAttributes = ImmutableMap.of(
            "platform", VERSION_FORMAT.format(platform.version()),
            "number", "$Revision$");
        xmlWriter.addElement("version", versionAttributes);
        // xmlWriter.addElement("generation", ImmutableMap.of("date", "$Date$"));
        xmlWriter.startElement("names");
        for (String name : keyboard.names()) {
            xmlWriter.addElement("name", ImmutableMap.of("value", name));
        }
        xmlWriter.endElement();
        // Settings.
        ImmutableMap.Builder<String, String> settingsBuilder = ImmutableMap.builder();
        if (platform.settings().fallbackSetting() == FallbackSetting.OMIT) {
            FallbackSetting fallbackSetting = platform.settings().fallbackSetting();
            settingsBuilder.put("fallback", fallbackSetting.toString().toLowerCase());
        }
        boolean hasTransform = keyboard.transforms().size() > 0;
        if (hasTransform
            && platform.settings().transformFailureSetting() == TransformFailureSetting.OMIT) {
            TransformFailureSetting transformFailure = platform.settings().transformFailureSetting();
            settingsBuilder.put("transformFailure", transformFailure.toString());
        }
        if (hasTransform
            && platform.settings().transformPartialSetting() == TransformPartialSetting.HIDE) {
            TransformPartialSetting transformPartial = platform.settings().transformPartialSetting();
            settingsBuilder.put("transformPartial", transformPartial.toString());
        }
        ImmutableMap<String, String> settingsAttributes = settingsBuilder.build();
        if (!settingsAttributes.isEmpty()) {
            xmlWriter.addElement("settings", settingsAttributes);
        }
    }

    private static final Joiner COMMA_JOINER = Joiner.on(",");

    private void addKeyMaps() {
        for (KeyMap keyMap : keyboard.keyMaps()) {
            ImmutableMap.Builder<String, String> keyMapAttributes = ImmutableMap.builder();
            ModifierKeyCombinationSet modifiers = keyMap.modifierKeyCombinationSet();
            if (!modifiers.isBase()) {
                keyMapAttributes.put("modifiers", modifiers.toString());
            }
            xmlWriter.startElement("keyMap", keyMapAttributes.build());
            for (CharacterMap characterMap : keyMap.isoLayoutToCharacterMap().values()) {
                String output = characterMap.output();
                ImmutableMap.Builder<String, String> mapAttributes = ImmutableMap.builder();
                mapAttributes.put("iso", "" + characterMap.position());
                String escapedOutput = escapeOutput(output);
                mapAttributes.put("to", escapedOutput);
                if (!characterMap.longPressKeys().isEmpty()) {
                    mapAttributes.put("longPress", COMMA_JOINER.join(characterMap.longPressKeys()));
                }
                if (characterMap.isTransformNo()) {
                    mapAttributes.put("transform", "no");
                }
                String comment = buildReadabilityComment(characterMap, escapedOutput);
                xmlWriter.addElement("map", mapAttributes.build(), comment);
            }
            xmlWriter.endElement();
        }
    }

    private static final UnicodeSet ESCAPED_CHARACTERS_NO_SPACE = new UnicodeSet("[[:di:][:c:][:M:][:whitespace:][\"]-[\\u0020]]").freeze();

    private String escapeOutput(String output) {
        StringBuilder stringBuilder = new StringBuilder();
        UCharacterIterator it = UCharacterIterator.getInstance(output);
        int character;
        while ((character = it.nextCodePoint()) != UCharacterIterator.DONE) {
            if (ESCAPED_CHARACTERS_NO_SPACE.contains(character)) {
                stringBuilder.append(String.format("\\u{%X}", character));
            } else {
                stringBuilder.append(UCharacter.toString(character));
            }
        }
        return stringBuilder.toString();
    }

    private static final UnicodeSet ILLEGAL_COMMENT_CHARACTERS = new UnicodeSet("[[:di:][:c:][:whitespace:]]").freeze();

    private String buildReadabilityComment(CharacterMap characterMap, String escapedOutput) {
        StringBuilder comment = new StringBuilder();
        String output = characterMap.output();
        IsoLayoutPosition position = characterMap.position();
        // English Key Name (Only if it is non-trivial)
        if (!output.toUpperCase().equals(position.englishKeyName())) {
            comment.append(position.englishKeyName());
        }
        // Base (Only if it is different than the english key and non-trivial).
        KeyMap baseMap = keyboard.baseMap();
        CharacterMap baseKey = baseMap.isoLayoutToCharacterMap().get(position);
        if (baseKey != null && !baseKey.output().toUpperCase().equals(output.toUpperCase())
            && !baseKey.output().toUpperCase().equals(position.englishKeyName())) {
            comment.append("  base=");
            comment.append(baseKey.output());
        }
        // Output (Only if the output is safe for comment)
        if (escapedOutput.contains("\\u{") && !ILLEGAL_COMMENT_CHARACTERS.contains(output)) {
            comment.append("  to= " + output + " ");
        }
        /*
        // Long press
        if (longPressString.contains("\\u{")) {
          StringBuilder longPressBuilder = new StringBuilder();
          for (String longPress : longPressKeys) {
        if (!CommonUtil.ILLEGAL_COMMENT_CHARACTERS.contains(longPress)) {
          longPressBuilder.append(longPress);
          longPressBuilder.append(" ");
        }
          }
        
          if (longPressBuilder.length() > 0) {
        comment.append("  long=");
        comment.append(longPressBuilder.toString());
          }
        }
        */
        return comment.toString();
    }

    private void addTransforms() {
        if (keyboard.transforms().isEmpty()) {
            return;
        }
        xmlWriter.startElement("transforms", ImmutableMap.of("type", "simple"));
        for (Transform transform : keyboard.transforms()) {
            String escapedSequence = escapeOutput(transform.sequence());
            String escapedOutput = escapeOutput(transform.output());
            String comment = buildTransformReadabilityComment(transform, escapedSequence, escapedOutput);
            xmlWriter.addElement("transform",
                ImmutableMap.of("from", escapedSequence, "to", escapedOutput),
                comment);
        }
        xmlWriter.endElement();

    }

    private String buildTransformReadabilityComment(Transform transform, String escapedSequence,
        String escapedOutput) {
        if ((escapedSequence.contains("\\u{") || escapedOutput.contains("\\u{"))
            && !ILLEGAL_COMMENT_CHARACTERS.containsSome(transform.sequence())
            && !ILLEGAL_COMMENT_CHARACTERS.contains(transform.output())) {
            return transform.sequence() + " → " + transform.output();
        }
        return "";
    }
}
