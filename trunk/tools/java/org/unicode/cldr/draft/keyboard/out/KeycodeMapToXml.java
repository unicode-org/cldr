package org.unicode.cldr.draft.keyboard.out;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Writer;
import java.util.Map.Entry;

import org.unicode.cldr.draft.keyboard.IsoLayoutPosition;
import org.unicode.cldr.draft.keyboard.KeyboardId.Platform;
import org.unicode.cldr.draft.keyboard.KeycodeMap;

import com.google.common.collect.ImmutableMap;

/** Utility class that writes the given key code map into the LDML XML format. */
public final class KeycodeMapToXml {
    private final KeycodeMap keycodeMap;
    private final Platform platform;
    private final XmlWriter xmlWriter;

    private KeycodeMapToXml(KeycodeMap keycodeMap, Platform platform, XmlWriter xmlWriter) {
        this.keycodeMap = checkNotNull(keycodeMap);
        this.platform = checkNotNull(platform);
        this.xmlWriter = checkNotNull(xmlWriter);
    }

    /** Writes the given key code map in XML format to the provided writer. */
    public static void writeToXml(KeycodeMap keycodeMap, Platform platform, Writer writer) {
        XmlWriter xmlWriter = XmlWriter.newXmlWriter(writer);
        KeycodeMapToXml keycodeMapToXml = new KeycodeMapToXml(keycodeMap, platform, xmlWriter);
        keycodeMapToXml.toXml();
    }

    private void toXml() {
        xmlWriter.startDocument("platform", "../dtd/ldmlPlatform.dtd");
        xmlWriter.startElement("platform", ImmutableMap.of("id", platform));
        xmlWriter.startElement("hardwareMap");
        for (Entry<Integer, IsoLayoutPosition> entry : keycodeMap.keycodeToIsoLayout().entrySet()) {
            xmlWriter.addElement("map", ImmutableMap.of("keycode", entry.getKey(), "iso", entry.getValue()));
        }
        xmlWriter.endElement();
        xmlWriter.endElement();
        xmlWriter.endDocument();
    }
}
