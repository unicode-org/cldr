package org.unicode.cldr.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Factory class to easily create (buffered) InputStreams
 * @author ribnitz
 *
 */
public class InputStreamFactory {

    /**
     * Create a Stream to read from the given fˇile
     * @param f - the file to read from
     * @return
     * @throws FileNotFoundException - if the File does not exist
     * @throws SecurityException - if a security manager exists and its checkRead method denies read access to the file
     */
    public static InputStream createInputStream(File f) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(f);
        return InputStreamFactory.buffer(fis);
    }

    /**
     * Decorate another InputStream to create a Buffering version
     * @param in -the Stream to decorate
     * @return a buffered version of the stream
     */
    public static InputStream buffer(InputStream in) {
        if (in instanceof BufferedInputStream) {
            return in;
        }
        return new BufferedInputStream(in);
    }

}
