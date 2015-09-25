package org.unicode.cldr.tool;

import java.io.IOException;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DayPeriodData;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.DayPeriodInfo.Type;
import org.unicode.cldr.util.LanguageGroup;

import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class ChartDayPeriods extends Chart {

    public static void main(String[] args) {
        new ChartDayPeriods().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return FormattedFileWriter.CHART_TARGET_DIR;
    }

    @Override
    public String getTitle() {
        return "Day Periods";
    }

    @Override
    public String getExplanation() {
        return "<p>Day Periods indicate roughly how the day is broken up in different languages. "
            + "The following shows the ones that can be used as selectors in messages. "
            + "The first column has a language group to collect languages together that are more likely to have similar day periods. "
            + "For more information, see: "
            + "<a href='http://unicode.org/repos/cldr/trunk/specs/ldml/tr35-dates.html#Day_Period_Rule_Sets'>Day Period Rules</a>. "
            + "The latest release data for this chart is in "
            + "<a href='http://unicode.org/cldr/latest/common/supplemental/dayPeriods.xml'>dayPeriods.xml</a>.<p>";
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {

        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Locale Group", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Locale Name", "class='source'", null, "class='source'", true)
            .setSortPriority(1)
            .addColumn("Code", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .setSortPriority(2)
            .setBreakSpans(true)
            //.addColumn("Type", "class='source'", null, "class='source'", true)
            //.setSortPriority(3)
            .addColumn("Start Time", "class='target'", null, "class='target'", true)
            .addColumn("Day Period Code", "class='target'", null, "class='target'", true)
            .addColumn("Example", "class='target'", null, "class='target'", true);

        DateFormat df = DateFormat.getPatternInstance("HH:mm", ULocale.ENGLISH);
        df.setTimeZone(TimeZone.GMT_ZONE);

        for (Type type : Type.values()) {
            if (type != Type.selection) { // skip until in better shape
                continue;
            }
            for (String locale : SDI.getDayPeriodLocales(type)) {
                if (locale.equals("root")) {
                    continue;
                }
                LanguageGroup group = LanguageGroup.get(new ULocale(locale));

                DayPeriodInfo dayPeriodInfo = SDI.getDayPeriods(Type.selection, locale);
                for (int i = 0; i < dayPeriodInfo.getPeriodCount(); ++i) {
                    R3<Integer, Boolean, DayPeriod> data = dayPeriodInfo.getPeriod(i);
                    Integer time = data.get0();

                    String name = DayPeriodData.getName(locale, data.get2());
                    if (name == null) {
                        name = "missing";
                    }
                    tablePrinter.addRow()
                        .addCell(group)
                        .addCell(ENGLISH.getName(locale))
                        .addCell(locale)
                        //.addCell(type)
                        .addCell(df.format(time))
                        .addCell(data.get2())
                        .addCell(name)
                        .finishRow();
                }
            }
        }
        pw.write(tablePrinter.toTable());
    }

}
