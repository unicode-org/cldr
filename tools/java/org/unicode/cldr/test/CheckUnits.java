package org.unicode.cldr.test;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.UnitId;
import org.unicode.cldr.util.UnitPathType;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.UnicodeSet;

public class CheckUnits extends CheckCLDR {
    private static final Pattern HOUR_SYMBOL = PatternCache.get("h{1,2}");
    private static final Pattern MINUTE_SYMBOL = PatternCache.get("m{1,2}");
    private static final Pattern SECONDS_SYMBOL = PatternCache.get("ss");
    private static final UnicodeSet DISALLOW_LONG_POWER = new UnicodeSet("[²³]").freeze();

    static final UnitConverter unitConverter = CLDRConfig.getInstance().getSupplementalDataInfo().getUnitConverter();

    private Collection<String> genders = null;

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);

        GrammarInfo grammarInfo = CLDRConfig.getInstance().getSupplementalDataInfo().getGrammarInfo(cldrFileToCheck.getLocaleID());
        genders = grammarInfo == null ? null : grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalGender, GrammaticalScope.units);

        return this;
    }

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {

        if (value == null || !path.startsWith("//ldml/units")) {
            return this;
        }
        final XPathParts parts = XPathParts.getFrozenInstance(path);
        String finalElement = parts.getElement(-1);

        if (genders != null && !genders.isEmpty() && finalElement.equals("gender")) {
            if (!genders.contains(value)) {
                result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.invalidGenderCode)
                    .setMessage("The gender value for this locale must be one of: {0}", genders));
            }
        }

        // Note, the following test has some overlaps with the checkAndReplacePlaceholders
        // test in CheckForExamplars (why there?). That is probably OK, they check in
        // different ways, but some errors will produce two somewhat different error messages.
        UnitPathType pathType = UnitPathType.getPathType(parts);
        if (pathType != null) {
            int min = 0;
            int max = 0;
            switch(pathType) {
            case power: case prefix:
                min = 1;
                max = 1;
                break;
            case times: case per:
                min = 2;
                max = 2;
                break;
            case perUnit: case coordinate: // coordinateUnitPattern
                min = 1;
                max = 1;
                break;
            case unit:
                min = 0;
                max = 1;
                break;
            default: // 0, 0
            }
            if (max > 0) {
                try {
                    SimpleFormatter sf = SimpleFormatter.compileMinMaxArguments(value, min, max);
                } catch (Exception e) {
                    result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.invalidPlaceHolder)
                        .setMessage("Invalid unit pattern, must have min " + min + " and max " + max + " distinct placeholders of the form {n}"));
                }
            }
            String idType;
            switch(pathType) {
            case power: {
                final String width = parts.getAttributeValue(-3, "type");
                if (value != null && "long".contentEquals(width)) {
                    if (DISALLOW_LONG_POWER.containsSome(fixedValueIfInherited(value, path))) {
                        String unresolvedValue = getCldrFileToCheck().getUnresolved().getStringValue(path);
                        if (unresolvedValue != null) {
                            final String message = genders == null
                                ? "Long value for power can’t use superscripts; it must be spelled out."
                                    : "Long value for power can’t use superscripts; it must be spelled out. [NOTE: values can vary by gender.]";
                            result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.longPowerWithSubscripts)
                                .setMessage(message));
                        }
                    }
                }
            }
            // fall through
            case prefix:
                idType = parts.getAttributeValue(-2, "type");
                for (String shortUnitId : pathType.sampleComposedShortUnitIds.get(idType)) {
                    final UnitId unitId = unitConverter.createUnitId(shortUnitId);
                    final String width = parts.getAttributeValue(-3, "type");
                    String count = parts.getAttributeValue(-1, "count");
                    String caseVariant = parts.getAttributeValue(-1, "case");
                    final CLDRFile cldrFile = getCldrFileToCheck();
                    String explicitPattern = UnitPathType.unit.getTrans(cldrFile, width, shortUnitId, count, caseVariant, null, null);
                    if (explicitPattern != null) {
                        String composedPattern = unitId.toString(cldrFile, width, count, caseVariant, null, false);
                        if (composedPattern != null && !explicitPattern.equals(composedPattern)) {
                            unitId.toString(cldrFile, width, count, caseVariant, null, false); // for debugging
                            result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType)
                                .setSubtype(Subtype.mismatchedUnitComponent)
                                .setMessage("Mismatched component: «{0}» produces «{1}», but the explicit translation is «{2}». See http://cldr.unicode.org/translation/units-1/units#TOC-Compound-Units", value, composedPattern, explicitPattern));
                        }
                    }
                }
                break;
            default:
                break;
            }
        }

        if (pathType == UnitPathType.duration) {
            XPathParts xpp = parts;
            String durationUnitType = xpp.findAttributeValue("durationUnit", "type");
            boolean hasHourSymbol = HOUR_SYMBOL.matcher(value).find();
            boolean hasMinuteSymbol = MINUTE_SYMBOL.matcher(value).find();
            boolean hasSecondsSymbol = SECONDS_SYMBOL.matcher(value).find();

            if (durationUnitType.contains("h") && !hasHourSymbol) {
                /* Changed message from "The hour symbol (h or hh) is missing"
                 *  to "The hour indicator should be either h or hh for duration"
                 *  per http://unicode.org/cldr/trac/ticket/10999
                 */
                result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.invalidDurationUnitPattern)
                    .setMessage("The hour indicator should be either h or hh for duration."));
            } else if (durationUnitType.contains("m") && !hasMinuteSymbol) {
                result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.invalidDurationUnitPattern)
                    .setMessage("The minutes symbol (m or mm) is missing."));
            } else if (durationUnitType.contains("s") && !hasSecondsSymbol) {
                result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.invalidDurationUnitPattern)
                    .setMessage("The seconds symbol (ss) is missing."));
            }
        }
        return this;
    }
}
