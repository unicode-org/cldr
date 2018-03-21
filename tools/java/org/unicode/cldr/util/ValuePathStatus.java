package org.unicode.cldr.util;

import java.util.List;

import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class ValuePathStatus {

    public enum MissingOK {
        ok, latin, alias, compact
    }

    public static final UnicodeSet LATIN = new UnicodeSet("[:sc=Latn:]").freeze();

    public static boolean isLatinScriptLocale(CLDRFile sourceFile) {
        UnicodeSet main = sourceFile.getExemplarSet("", WinningChoice.WINNING);
        return LATIN.containsSome(main);
    }

    public static Transform<String, ValuePathStatus.MissingOK> MISSING_STATUS_TRANSFORM = new Transform<String, ValuePathStatus.MissingOK>() {
        public ValuePathStatus.MissingOK transform(String source) {
            return ValuePathStatus.MissingOK.valueOf(source);
        }
    };

    static final RegexLookup<ValuePathStatus.MissingOK> missingOk = new RegexLookup<ValuePathStatus.MissingOK>()
        .setPatternTransform(RegexLookup.RegexFinderTransformPath)
        .setValueTransform(MISSING_STATUS_TRANSFORM)
        .loadFromFile(ValuePathStatus.class, "data/paths/missingOk.txt");

    static int countZeros(String otherValue) {
        int result = 0;
        for (int i = 0; i < otherValue.length(); ++i) {
            if (otherValue.charAt(i) == '0') {
                ++result;
            }
        }
        return result;
    }

    public static boolean isMissingOk(CLDRFile sourceFile, String path, boolean latin, boolean aliased) {
        Output<String[]> arguments = new Output<String[]>();
        List<String> failures = null;
//        if (path.startsWith("//ldml/characters/parseLenients")) {
//            int debug = 0;
//            failures = new ArrayList<>();
//        }
        ValuePathStatus.MissingOK value = missingOk.get(path, null, arguments, null, failures);
        if (value == null) {
            return false;
        }
        switch (value) {
        case ok:
            return true;
        case latin:
            return latin;
        case alias:
            return aliased;
        case compact:
            // special processing for compact numbers
            if (path.contains("[@count=\"other\"]")) {
                return false; // the 'other' class always counts as missing
            }
            String otherPath = "//ldml/numbers/decimalFormats[@numberSystem=\"" + arguments.value[1]
                + "\"]/decimalFormatLength[@type=\"" + arguments.value[2]
                + "\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"" + arguments.value[3]
                + "\"][@count=\"other\"]";
            String otherValue = sourceFile.getWinningValue(otherPath);
            if (otherValue == null) {
                return false; // something's wrong, bail
            }
            int digits = countZeros(otherValue);
            if (digits > 4) { // we can only handle to 4 digits
                return false;
            }
            // if there are no possible Count values for this many digits, then it is ok to be missing.
            Count c = Count.valueOf(arguments.value[4]);
            SupplementalDataInfo supplementalDataInfo2 = CLDRConfig.getInstance().getSupplementalDataInfo();
            // SupplementalDataInfo.getInstance(sourceFile.getSupplementalDirectory());
            PluralInfo plurals = supplementalDataInfo2.getPlurals(sourceFile.getLocaleID());
            return plurals == null || !plurals.hasSamples(c, digits); // ok if no samples
        // TODO: handle fractions
        default:
            throw new IllegalArgumentException();
        }
    }

}
