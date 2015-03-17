package org.unicode.cldr.tool;

import java.io.IOException;

import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.SupplementalDataInfo;

public abstract class Chart {
    public static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    public static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    public static final CLDRFile ENGLISH = CONFIG.getEnglish();

    /**
     * null means a string will be constructed from the title. Otherwise a real file name (no html extension).
     * @return
     */
    public String getFileName() {
        return null; 
    };
    /**
     * Short explanation that will go just after the title/dates.
     * @return
     */
    public String getExplanation() {
        return null;
    }
    /**
     * Directory for the file to go into.
     * @return
     */
    public abstract String getDirectory();
    /**
     * Short title for page. Will appear at the top, and in the window title, and in the index.
     * @return
     */
    public abstract String getTitle();
    /**
     * Work
     * @param pw
     * @throws IOException
     */
    public abstract void writeContents(FormattedFileWriter pw) throws IOException;

    public final void writeChart(Anchors anchors) {
        try (
            FormattedFileWriter x = new FormattedFileWriter(getFileName(), getTitle(), getExplanation(), anchors);) {
            x.setDirectory(getDirectory());
            writeContents(x);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
