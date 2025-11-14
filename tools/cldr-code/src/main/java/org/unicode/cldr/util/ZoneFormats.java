package org.unicode.cldr.util;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;
import java.util.Locale;

public class ZoneFormats {
    private final String gmtFormat;
    private final String hourFormat;
    private final String[] hourFormatPlusMinus;
    private final ICUServiceBuilder icuServiceBuilder;
    CLDRFile cldrFile;

    public ZoneFormats(CLDRFile cldrFile) {
        this.cldrFile = cldrFile;
        this.gmtFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/gmtFormat");
        this.hourFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/hourFormat");
        this.hourFormatPlusMinus = hourFormat.split(";");
        this.icuServiceBuilder = new ICUServiceBuilder(cldrFile);
    }

    public enum Length {
        LONG,
        SHORT;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    public enum Type {
        generic,
        standard,
        daylight,
        genericOrStandard
    }

    public String formatGMT(TimeZone currentZone) {
        int tzOffset = currentZone.getRawOffset();
        SimpleDateFormat dateFormat =
                icuServiceBuilder.getDateFormat(
                        "gregorian", hourFormatPlusMinus[tzOffset >= 0 ? 0 : 1]);
        String hoursMinutes = dateFormat.format(tzOffset >= 0 ? tzOffset : -tzOffset);
        return MessageFormat.format(gmtFormat, hoursMinutes);
    }

    public String getExemplarCity(String timezoneString) {
        String exemplarCity =
                cldrFile.getWinningValue(
                        "//ldml/dates/timeZoneNames/zone[@type=\""
                                + timezoneString
                                + "\"]/exemplarCity");
        if (exemplarCity == null) {
            exemplarCity =
                    timezoneString.substring(timezoneString.lastIndexOf('/') + 1).replace('_', ' ');
        }
        return exemplarCity;
    }

    public String getMetazoneName(
            String metazone, ZoneFormats.Length length, ZoneFormats.Type typeIn) {
        ZoneFormats.Type type = typeIn == Type.genericOrStandard ? Type.generic : typeIn;
        String name =
                cldrFile.getWinningValue(
                        "//ldml/dates/timeZoneNames/metazone[@type=\""
                                + metazone
                                + "\"]/"
                                + length
                                + "/"
                                + type);

        return name != null
                ? name
                : typeIn != Type.genericOrStandard
                        ? "n/a"
                        : getMetazoneName(metazone, length, Type.standard);
    }
}
