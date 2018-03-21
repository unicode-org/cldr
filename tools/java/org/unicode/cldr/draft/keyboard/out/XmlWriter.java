package org.unicode.cldr.draft.keyboard.out;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.google.common.collect.ImmutableMap;

/** 
 * Object which wraps around an XML stream writer. Automatically adds proper formatting
 * (indentation and line breaks) to the written XML elements.
 */
final class XmlWriter {
    private final XMLStreamWriter writer;
    private int depth = 0;

    private XmlWriter(XMLStreamWriter writer) {
        this.writer = checkNotNull(writer);
    }

    static XmlWriter newXmlWriter(Writer writer) {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlStreamWriter;
        try {
            xmlStreamWriter = outputFactory.createXMLStreamWriter(writer);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return new XmlWriter(xmlStreamWriter);
    }

    XmlWriter startDocument(String doctype, String dtdLocation) {
        try {
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeCharacters("\n");
            writer.writeDTD("<!DOCTYPE " + doctype + " SYSTEM \"" + dtdLocation + "\">");
            writer.writeCharacters("\n");
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    XmlWriter endDocument() {
        checkState(depth == 0, "Cannot close document with unclosed elements");
        try {
            writer.writeEndDocument();
            writer.close();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    XmlWriter startElement(String name) {
        return startElement(name, ImmutableMap.<String, Object> of());
    }

    XmlWriter startElement(String name, Map<String, ?> attributeToValue) {
        addIndent();
        try {
            writer.writeStartElement(name);
            for (Entry<String, ?> entry : attributeToValue.entrySet()) {
                writer.writeAttribute(entry.getKey(), "" + entry.getValue());
            }
            writer.writeCharacters("\n");
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        depth++;
        return this;
    }

    XmlWriter endElement() {
        depth--;
        addIndent();
        try {
            writer.writeEndElement();
            writer.writeCharacters("\n");
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    XmlWriter addElement(String name, Map<String, ?> attributeToValue) {
        return addElement(name, attributeToValue, "");
    }

    XmlWriter addElement(String name, Map<String, ?> attributeToValue, String comment) {
        addIndent();
        try {
            writer.writeEmptyElement(name);
            for (Entry<String, ?> entry : attributeToValue.entrySet()) {
                writer.writeAttribute(entry.getKey(), "" + entry.getValue());
            }
            if (!comment.isEmpty()) {
                writer.writeCharacters(" ");
                writer.writeComment(" " + comment + " ");
            }
            writer.writeCharacters("\n");
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    private void addIndent() {
        for (int i = 0; i < depth; i++) {
            try {
                writer.writeCharacters("\t");
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
