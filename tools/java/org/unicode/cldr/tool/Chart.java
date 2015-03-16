package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;

import org.unicode.cldr.tool.ShowLanguages.FormattedFileWriter;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.SupplementalDataInfo;

public abstract class Chart {
    public static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    public static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    public static final CLDRFile ENGLISH = CONFIG.getEnglish();

    public abstract String getName();
    public abstract void writeContents(PrintWriter pw);

    public final void writeChart(PrintWriter index) {
        writeChart(index, null);
    }
    
    public void writeChart(PrintWriter index, String directory) {
        try (
            FormattedFileWriter x = new FormattedFileWriter(index, getName(), null, false);
            PrintWriter pw = new PrintWriter(x)) {
            if (directory != null) {
                x.setDirectory(directory);
            }
            writeContents(pw);
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
    }
}
