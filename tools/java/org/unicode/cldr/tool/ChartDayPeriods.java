package org.unicode.cldr.tool;

import java.io.PrintWriter;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DayPeriodData;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.DayPeriodInfo.Type;

import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class ChartDayPeriods extends Chart {

    public static void main(String[] args) {
        new ChartDayPeriods().writeChart(null);
    }

    public String getName() {
        return "Day Periods";
    }

    public void writeContents(PrintWriter pw) {
        TablePrinter tablePrinter = new TablePrinter()
        .addColumn("Locale Name", "class='source'", null, "class='source'", true)
        .setSortPriority(1)
        .addColumn("Code", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
        .setSortPriority(2)
        .setBreakSpans(true)
        //.addColumn("Type", "class='source'", null, "class='source'", true)
        //.setSortPriority(3)
        .addColumn("Start Time", "class='target'", null, "class='target'", true)
        .addColumn("Day Period Code", "class='target'", null, "class='target'", true)
        .addColumn("Example", "class='target'", null, "class='target'", true)
        ;
        
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
                DayPeriodInfo dayPeriodInfo = SDI.getDayPeriods(Type.selection, locale);
                for (int i = 0; i < dayPeriodInfo.getPeriodCount(); ++i) {
                    R3<Integer, Boolean, DayPeriod> data = dayPeriodInfo.getPeriod(i);
                    Integer time = data.get0();
                    
                    String name = DayPeriodData.getName(locale, data.get2());
                    if (name == null) {
                        name = "missing";
                    }
                    tablePrinter.addRow()
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
        pw.println(tablePrinter.toTable());
    }
}
