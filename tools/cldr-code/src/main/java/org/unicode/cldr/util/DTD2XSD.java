package org.unicode.cldr.util;

import com.thaiopensource.relaxng.edit.SchemaCollection;
import com.thaiopensource.relaxng.input.InputFailedException;
import com.thaiopensource.relaxng.input.InputFormat;
import com.thaiopensource.relaxng.input.dtd.DtdInputFormat;
import com.thaiopensource.relaxng.output.LocalOutputDirectory;
import com.thaiopensource.relaxng.output.OutputFailedException;
import com.thaiopensource.relaxng.output.OutputFormat;
import com.thaiopensource.relaxng.output.xsd.XsdOutputFormat;
import com.thaiopensource.relaxng.translate.util.InvalidParamsException;
import com.thaiopensource.xml.sax.ErrorHandlerImpl;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Called by GenerateDtd to convert DTD to XSD */
public class DTD2XSD {

    public static void write(DtdData data, DtdType type) {
        final String dtdPath = CLDRPaths.BASE_DIRECTORY + type.dtdPath;
        final String xsdPath = CLDRPaths.BASE_DIRECTORY + type.getXsdPath();
        writeXsd(type, data, dtdPath, xsdPath);
        System.err.println("Wrote XSD: " + xsdPath);
    }

    private static void writeXsd(
            DtdType type, DtdData data, final String dtdPath, final String xsdPath) {
        // Step 1: fire up Trang to do the basic conversion
        InputFormat inFormat = new DtdInputFormat();
        OutputFormat outputFormat = new XsdOutputFormat();
        ErrorHandlerImpl eh = new ErrorHandlerImpl();
        final String inputUri = new File(dtdPath).toURI().toString();
        String params[] = {};
        try {
            SchemaCollection sc = inFormat.load(inputUri, params, "xsd", eh, null);
            outputFormat.output(
                    sc,
                    new LocalOutputDirectory(inputUri, new File(xsdPath), ".xsd", "UTF-8", 72, 2),
                    params,
                    "dtd",
                    eh);
        } catch (InputFailedException
                | InvalidParamsException
                | IOException
                | SAXException
                | OutputFailedException e) {
            e.printStackTrace();
            System.err.println("Error generating XSD from " + inputUri);
        }

        // Step 2: re-parse
        final Document d = LDMLUtilities.parse(xsdPath, false, false);

        // Step 3: remove all <!-- @ comments
        removeAnnotationComments(null, d);

        // Step 4: write .xsd out
        try (OutputStream file =
                        new BufferedOutputStream(new FileOutputStream(xsdPath, false)); // Append
                PrintWriter pw = new PrintWriter(file, false, StandardCharsets.UTF_8); ) {
            LDMLUtilities.printDOMTree(
                    d,
                    pw,
                    null,
                    "<!--\n"
                            + "Note: The .xsd files are a Technology Preview. They are subject to change or removal in future CLDR versions.\n"
                            + "Note: DTD @-annotations are not currently converted to .xsd. For full CLDR file validation, use the DTD and CLDR tools.\n"
                            + "-->\n\n" /* copyright is passthrough from .dtd */);
        } catch (Throwable t) {
            throw new RuntimeException("Generating .xsd for " + type, t);
        }
    }

    static void removeAnnotationComments(Node parent, Node n) {
        if (parent != null && n.getNodeType() == Node.COMMENT_NODE) {
            if (n.getTextContent().trim().startsWith("@")) {
                parent.removeChild(n);
            }
        } else {
            NodeList nl = n.getChildNodes();
            if (nl != null) {
                // walk backwards because of deletion
                for (int i = nl.getLength() - 1; i > 0; i--) {
                    // recurse
                    removeAnnotationComments(n, nl.item(i));
                }
            }
        }
    }
}
