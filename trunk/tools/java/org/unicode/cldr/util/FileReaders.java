package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

public class FileReaders {
    //
    // public static SemiFileReader fillMapFromSemi(Class classLocation, String fileName, SemiFileReader handler) {
    // return handler.process(classLocation, fileName);
    // }
    public static BufferedReader openFile(Class<?> class1, String file) {
        return openFile(class1, file, CldrUtility.UTF8);
    }

    public static BufferedReader openFile(Class<?> class1, String file, Charset charset) {
        // URL path = null;
        // String externalForm = null;
        try {
            // //System.out.println("Reading:\t" + file1.getCanonicalPath());
            // path = class1.getResource(file);
            // externalForm = path.toExternalForm();
            // if (externalForm.startsWith("file:")) {
            // externalForm = externalForm.substring(5);
            // }
            // File file1 = new File(externalForm);
            // boolean x = file1.canRead();
            // final InputStream resourceAsStream = new FileInputStream(file1);
            final InputStream resourceAsStream = class1.getResourceAsStream(file);
            // String foo = class1.getResource(".").toString();
            if (charset == null) {
                charset = CldrUtility.UTF8;
            }
            InputStreamReader reader = new InputStreamReader(resourceAsStream, charset);
            BufferedReader bufferedReader = new BufferedReader(reader, 1024 * 64);
            return bufferedReader;
        } catch (Exception e) {
            String className = class1 == null ? null : class1.getCanonicalName();
            String canonicalName = null;
            try {
                String relativeFileName = FileReaders.getRelativeFileName(class1, "../util/");
                canonicalName = new File(relativeFileName).getCanonicalPath();
            } catch (Exception e1) {
                throw new IllegalArgumentException("Couldn't open file: " + file + "; relative to class: "
                    + className, e);
            }
            throw new IllegalArgumentException("Couldn't open file " + file + "; in path " + canonicalName + "; relative to class: "
                + className, e);
        }
    }

    public static BufferedReader openFile(String directory, String file, Charset charset) {
        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(new File(directory, file)), charset));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e); // handle dang'd checked exception
        }
    }

    public static BufferedReader openFile(String directory, String file) {
        return openFile(directory, file, CldrUtility.UTF8);
    }

    public static String getRelativeFileName(Class<?> class1, String filename) {
        URL resource = class1.getResource(filename);
        String resourceString = resource.toString();
        if (resourceString.startsWith("file:")) {
            return resourceString.substring(5);
        } else if (resourceString.startsWith("jar:file:")) {
            return resourceString.substring(9);
        } else {
            throw new IllegalArgumentException("File not found: " + resourceString);
        }
    }
}