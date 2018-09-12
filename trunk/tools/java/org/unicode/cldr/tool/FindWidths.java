package org.unicode.cldr.tool;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.ApproximateWidth;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;

import com.ibm.icu.text.NumberFormat;

public class FindWidths {
    private static final CLDRConfig testInfo = ToolConfig.getToolInstance();
    private static final org.unicode.cldr.util.Factory CLDR_FACTORY = testInfo.getCldrFactory();
    private static final double MIN_TEST_INCREASE = 1.5;
    private static final int MIN_TEST_WIDTH = 2 * ApproximateWidth.getWidth("え");

    static final class Data {
        String locale;
        String value;
        int width = Integer.MIN_VALUE;
        int count = 0;
        int englishWidth;

        public void add(String locale2, String value2, int width2, int englishWidth) {
            if (width2 > width) {
                width = width2;
                locale = locale2;
                value = value2;
                this.englishWidth = englishWidth;
            }
            ++count;
        }
    }

    public static void main(String[] args) {
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMaximumFractionDigits(0);
        CLDRFile english = testInfo.getEnglish();
        Factory phf = PathHeader.getFactory(english);
        Map<PathHeader, Integer> englishWidths = new HashMap<PathHeader, Integer>();
        Map<PathHeader, Data> maxWidths = new TreeMap<PathHeader, Data>();
        Set<String> sampleLocales = testInfo.getStandardCodes().getLocaleCoverageLocales("google");

        for (String path : english) {
            PathHeader ph = phf.fromPath(path);
            PageId pageId = ph.getPageId();
            SectionId sectionId = ph.getSectionId();
            if (pageId == PageId.Alphabetic_Information
                || sectionId == SectionId.Special) {
                continue;
            }
            String value = english.getStringValue(path);
            int width = ApproximateWidth.getWidth(value);
            englishWidths.put(ph, width);
        }

        for (String locale : CLDR_FACTORY.getAvailableLanguages()) {
            if (!sampleLocales.contains(locale) || locale.equals("en")) {
                continue;
            }
            System.out.println(locale);
            CLDRFile file = CLDR_FACTORY.make(locale, false);
            for (String path : file) {
                PathHeader ph = phf.fromPath(path);
                Integer englishWidth = englishWidths.get(ph);
                if (englishWidth == null) {
                    continue;
                }
                PageId pageId = ph.getPageId();
                SectionId sectionId = ph.getSectionId();
                if (pageId == PageId.Alphabetic_Information
                    || sectionId == SectionId.Special) {
                    continue;
                }

                String value = file.getStringValue(path);
                int width = ApproximateWidth.getWidth(value);
                if (width < MIN_TEST_WIDTH) {
                    continue; // don't care about very short
                }
                double sizeIncrease = width / (double) englishWidth - 1;
                if (sizeIncrease < MIN_TEST_INCREASE) {
                    continue;
                }
                Data data = maxWidths.get(ph);
                if (data == null) {
                    maxWidths.put(ph, data = new Data());
                }
                data.add(locale, value, width, englishWidth);
            }
        }
        int count = 0;
        for (Entry<PathHeader, Data> entry : maxWidths.entrySet()) {
            PathHeader path = entry.getKey();
            Data data = entry.getValue();
            double sizeIncrease = data.width / (double) data.englishWidth - 1;
            System.out.println(++count
                + "\t" + path
                + "\t" + data.locale
                + "\t«" + english.getStringValue(path.getOriginalPath()) + "»"
                + "\t«" + data.value + "»"
                + "\t+" + nf.format(sizeIncrease)
                + "\t" + data.width
                + "\t" + data.count);
        }
    }
}
