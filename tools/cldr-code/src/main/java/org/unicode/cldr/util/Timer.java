package org.unicode.cldr.util;

import java.util.Locale;

import com.ibm.icu.impl.duration.DurationFormatter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DurationFormat;
import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RelativeDateTimeFormatter;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

public final class Timer {
    private static final double NANOS_PER_SECOND = 1000000000.0d;

    private long startTime;
    private long duration;
    {
        start();
    }

    public void start() {
        startTime = System.nanoTime();
        duration = Long.MIN_VALUE;
    }

    public long getDuration() {
        if (duration == Long.MIN_VALUE) {
            duration = System.nanoTime() - startTime;
        }
        return duration;
    }

    public long getNanoseconds() {
        return getDuration();
    }

    public double getSeconds() {
        return getDuration() / NANOS_PER_SECOND;
    }

    /**
     * Return nanos
     * @return
     */
    public long stop() {
        return getDuration();
    }

    @Override
    public String toString() {
        return nf.format(getDuration() / NANOS_PER_SECOND) + "s";
    }

    /**
     * @return the duration as a measureformat string
     */
    public String toMeasureString() {
        double seconds = getSeconds();
        double minutes = Math.floorDiv((int) seconds, 60);
        seconds = seconds - (minutes * 60.0);

        return MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT)
            .formatMeasures(new Measure(seconds, MeasureUnit.SECOND),
                new Measure(minutes, MeasureUnit.MINUTE));
    }

    public String toString(Timer other) {
        return toString(1L, other.getDuration());
    }

    public String toString(long iterations) {
        return nf.format(getDuration() / (NANOS_PER_SECOND * iterations)) + "s";
    }

    public String toString(long iterations, long other) {
        return toString(iterations) + "\t(" + pf.format((double) getDuration() / other - 1D)
            + ")";
    }

    private static DecimalFormat nf = (DecimalFormat) NumberFormat.getNumberInstance(ULocale.ENGLISH);
    private static DecimalFormat pf = (DecimalFormat) NumberFormat.getPercentInstance(ULocale.ENGLISH);
    static {
        nf.setMaximumSignificantDigits(3);
        pf.setMaximumFractionDigits(1);
        pf.setPositivePrefix("+");
    }
}