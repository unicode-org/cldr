package org.unicode.cldr.tool;

import com.ibm.icu.text.Transliterator;
import java.util.Set;

public enum CldrConfigOption {
    delete,
    add,
    addNew,
    replace;

    public String show(
            CldrConfigOption configOption,
            String fileParent,
            String localeOrFile,
            String match_path,
            String match_value,
            String new_path,
            Set<String> new_values,
            Set<String> otherValues) {
        // locale= sv ; action=delete; value= YER ; path=
        // //ldml/numbers/currencies/currency[@type="YER"]/symbol ;

        // locale=en ; action=delete ; path=/.*short.*/

        // locale=en ; action=add ;
        // new_path=//ldml/localeDisplayNames/territories/territory[@type="PS"][@alt="short"] ;
        // new_value=Palestine
        // locale=  af     ; action=add ; new_path=
        // //ldml/dates/fields/field[@type="second"]/relative[@type="0"]    ; new_value=    nou

        int extensionPos = localeOrFile.lastIndexOf('.');
        String fileWithoutSuffix =
                extensionPos >= 0 ? localeOrFile.substring(0, extensionPos) : localeOrFile;

        String values2 =
                new_values == null
                        ? null
                        : new_values.size() != 1
                                ? new_values.toString()
                                : new_values.iterator().next();

        return fileParent
                + ";\tlocale="
                + fileWithoutSuffix
                + ";\taction="
                + configOption
                + (match_value == null ? "" : ";\tvalue=" + escape(match_value))
                + (match_path == null ? "" : ";\tpath=" + match_path)
                + (values2 == null ? "" : ";\tnew_value=" + escape(values2))
                + (new_path == null ? "" : ";\tnew_path=" + new_path)
                + (otherValues == null ? "" : ";\tother_value=" + otherValues);
    }

    static final Transliterator showInvisibles =
            Transliterator.getInstance("[[:whitespace:][:cf:]-[\\u0020]]hex/perl");

    private static String escape(String source) {
        return showInvisibles.transform(source);
    }
}
