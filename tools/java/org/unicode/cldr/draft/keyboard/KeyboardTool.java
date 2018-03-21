package org.unicode.cldr.draft.keyboard;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;

import org.unicode.cldr.draft.keyboard.KeyboardId.Platform;
import org.unicode.cldr.draft.keyboard.out.KeyboardToXml;
import org.unicode.cldr.draft.keyboard.out.KeycodeMapToXml;
// import org.unicode.cldr.draft.keyboard.windows.KlcParser;
import org.unicode.cldr.draft.keyboard.windows.KlcParser;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

public final class KeyboardTool {

    /**
     * First argument is output folder, second is the location of the windows keyboard source files.
     */
    public static void main(String[] args) throws IOException {
        System.out.println(Arrays.toString(args));
        long timestamp = Instant.now().getEpochSecond();
        String output = args[0] + "/" + timestamp;
        File outputFolder = new File(output);
        parseWindowsKeyboards(args[1], outputFolder);
        // parseOsxKeyboards("/Users/rwainman/Downloads/osx", outputFolder);
    }

    /*
    private static void parseOsxKeyboards(String inputFolder, File outputFolder) throws IOException {
    File macosKeyboardsDirectory = new File(inputFolder);
    File macosOutputFolder = new File(outputFolder + "/osx");
    macosOutputFolder.mkdirs();
    for (File keyboardLayout : macosKeyboardsDirectory.listFiles(KeylayoutFilenameFilter.INSTANCE)) {
      System.out.println("Parsing " + keyboardLayout);
      String contents = Files.toString(keyboardLayout, Charsets.UTF_8);
      KeylayoutParser.parseLayout(contents);
      for (Document document : KeyboardToXml.writeToXml(keyboard)) {
        Element keyboardElement = (Element) document.getFirstChild=();
        String locale = keyboardElement.getAttribute("locale");
        File outputFile = new File(macosOutputFolder + "/" + locale + ".xml");
        Files.write(documentToString(document), outputFile, Charsets.UTF_8);
        System.out.println("   Writing to " + outputFile);
      }
      return;
    }
    System.out.println(KeylayoutParser.KEYBOARD_ID_MAP.unmatchedIds());
    }
    
    private enum KeylayoutFilenameFilter implements FilenameFilter {
    INSTANCE;
    @Override public boolean accept(File dir, String name) {
      return name.endsWith(".keylayout");
    }
    }
    */

    private static void parseWindowsKeyboards(String inputFolder, File outputFolder)
        throws IOException {
        File windowsKeyboardsDirectory = new File(inputFolder);
        File windowsOutputFolder = new File(outputFolder + "/windows");
        windowsOutputFolder.mkdirs();
        for (File keyboardLayout : windowsKeyboardsDirectory.listFiles(KlcFilenameFilter.INSTANCE)) {
            System.out.println("Parsing " + keyboardLayout);
            String contents = Files.toString(keyboardLayout, Charsets.UTF_16);
            ImmutableList<Keyboard> keyboards = KlcParser.parseLayout(contents);
            for (Keyboard keyboard : keyboards) {
                String id = keyboard.keyboardId().toString();
                File outputFile = new File(windowsOutputFolder + "/" + id + ".xml");
                StringWriter keyboardStringWriter = new StringWriter();
                KeyboardToXml.writeToXml(keyboard, keyboardStringWriter);
                FileWriter keyboardFileWriter = new FileWriter(outputFile);
                keyboardFileWriter.write(doLastMinuteFixesToXml(keyboardStringWriter.toString()));
                keyboardFileWriter.close();
                System.out.println("   Writing to " + outputFile);
            }
        }
        System.out.println("Writing _platform.xml");
        FileWriter platformFileWriter = new FileWriter(windowsOutputFolder + "/_platform.xml");
        KeycodeMapToXml.writeToXml(KlcParser.KEYCODE_MAP, Platform.WINDOWS, platformFileWriter);
        if (KlcParser.KEYBOARD_ID_MAP.unmatchedIds().size() != 0) {
            System.out.println("Found the following keyboards with no id (add them to windows-locales.csv file):");
            System.out.println(KlcParser.KEYBOARD_ID_MAP.unmatchedIds());
        }
    }

    private enum KlcFilenameFilter implements FilenameFilter {
        INSTANCE;
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".klc");
        }
    }

    private static String doLastMinuteFixesToXml(String ouputString) {
        String cleansedString = ouputString
            // The regular XML output does not escape the apostrophes.
            .replace("\"'\"", "\"&apos;\"");
        return cleansedString;
    }
}
