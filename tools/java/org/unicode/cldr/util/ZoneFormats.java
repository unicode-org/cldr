package org.unicode.cldr.util;

import java.util.Locale;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;

public class ZoneFormats {
    private String gmtFormat;
    private String hourFormat;
    private String[] hourFormatPlusMinus;
    private ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
    CLDRFile cldrFile;

    public enum Length {
        LONG, SHORT;
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    public enum Type {
        generic, standard, daylight, genericOrStandard
    }

    public ZoneFormats set(CLDRFile cldrFile) {
        this.cldrFile = cldrFile;

        gmtFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/gmtFormat");
        hourFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/hourFormat");
        hourFormatPlusMinus = hourFormat.split(";");
        icuServiceBuilder.setCldrFile(cldrFile);
        return this;
    }

    public String formatGMT(TimeZone currentZone) {
        int tzOffset = currentZone.getRawOffset();
        SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian",
            hourFormatPlusMinus[tzOffset >= 0 ? 0 : 1]);
        String hoursMinutes = dateFormat.format(tzOffset >= 0 ? tzOffset : -tzOffset);
        return MessageFormat.format(gmtFormat, hoursMinutes);
    }

    public String getExemplarCity(String timezoneString) {
        String exemplarCity = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/zone[@type=\"" + timezoneString
            + "\"]/exemplarCity");
        if (exemplarCity == null) {
            exemplarCity = timezoneString.substring(timezoneString.lastIndexOf('/') + 1).replace('_', ' ');
        }
        return exemplarCity;
    }

    public String getMetazoneName(String metazone, ZoneFormats.Length length, ZoneFormats.Type typeIn) {
        ZoneFormats.Type type = typeIn == Type.genericOrStandard ? Type.generic : typeIn;
        String name = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/metazone[@type=\""
            + metazone + "\"]/" + length + "/" + type);

        return name != null ? name : typeIn != Type.genericOrStandard ? "n/a" : getMetazoneName(metazone, length,
            Type.standard);
    }
}