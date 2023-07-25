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
import java.io.File;
import java.io.IOException;
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
        // Step 1: trang
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
    }
}
