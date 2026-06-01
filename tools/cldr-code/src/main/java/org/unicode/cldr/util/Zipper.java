package org.unicode.cldr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipper {

    private static final int BUFFER_SIZE = 4096;

    /**
     * Create a zip file from the given directory.
     *
     * <p>If the directory is /foo/bar, then the zip file will be /foo/bar.zip
     *
     * @param dir the given directory
     * @return the zip file
     * @throws IOException if unable to read/write files
     */
    public static File zipDirectory(File dir) throws IOException {
        String zipFileName = dir.getAbsolutePath() + ".zip";
        FileOutputStream fos = new FileOutputStream(zipFileName);
        ZipOutputStream zos = new ZipOutputStream(fos);
        zipFile(dir, dir.getName(), zos);
        zos.close();
        fos.close();
        return new File(zipFileName);
    }

    /**
     * Create a zip file from the given file or directory
     *
     * <p>Note: this function calls itself recursively
     *
     * @param file the file or directory to zip
     * @param name the name of the file, including its slash-separated path when called recursively
     * @param zos the ZipOutputStream
     * @throws IOException if unable to read/write files
     */
    private static void zipFile(File file, String name, ZipOutputStream zos) throws IOException {
        if (file.isDirectory()) {
            zos.putNextEntry(new ZipEntry(name.endsWith("/") ? name : name + "/"));
            zos.closeEntry();
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!child.isHidden()) {
                        zipFile(child, name + "/" + child.getName(), zos);
                    }
                }
            }
        } else {
            FileInputStream fis = new FileInputStream(file);
            zos.putNextEntry(new ZipEntry(name));
            byte[] bytes = new byte[BUFFER_SIZE];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            fis.close();
        }
    }
}
