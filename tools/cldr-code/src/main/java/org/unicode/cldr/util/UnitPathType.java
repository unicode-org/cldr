package org.unicode.cldr.util;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public enum UnitPathType {
    // we may add features over time
    unit("kilometer", EnumSet.of(
        GrammarInfo.GrammaticalFeature.grammaticalNumber,
        GrammarInfo.GrammaticalFeature.grammaticalCase),
        null),
    perUnit("minute", null, null),
    times(null, null, ImmutableMultimap.<String,String>builder()
        .put("", "newton-meter")
        .put("", "kilowatt-hour")
        .build()),
    per(null, null, ImmutableMultimap.<String,String>builder()
        .put("", "meter-per-second")
        .put("", "mile-per-gallon")
        .build()),
    prefix(null, null, ImmutableMultimap.<String,String>builder()
        .put("10p2", "hectopascal")
        .put("10p3", "kilometer")
        .put("10p6", "megabyte")
        .put("10p9", "gigahertz")
        .put("10p12", "terabyte")
        .put("10p15", "petabyte")
        .put("10p-1", "deciliter")
        .put("10p-2", "centimeter")
        .put("10p-3", "milligram")
        .put("10p-6", "microsecond")
        .put("10p-9", "nanosecond")
        .put("10p-12", "picometer")
        .build()),
    power("power2",
        EnumSet.of(
            GrammarInfo.GrammaticalFeature.grammaticalNumber,
            GrammarInfo.GrammaticalFeature.grammaticalCase,
            GrammarInfo.GrammaticalFeature.grammaticalGender),
        ImmutableMultimap.<String,String>builder()
        .put("power2", "square-meter")
        .put("power2", "square-second")
        .put("power3", "cubic-meter")
        .put("power3", "cubic-second")
        .build()),
    duration(null, null, null),
    gender(null,null, null),
    coordinate(null, null, null),
    displayName(null, null, null)
    ;

    public final Set<GrammaticalFeature> features;
    public final Set<String> sampleShortUnitType;
    public final ImmutableMultimap<String,String> sampleComposedShortUnitIds;

    private UnitPathType(String sampleType, Set<GrammarInfo.GrammaticalFeature> features, ImmutableMultimap<String,String> sampleComposedLongUnits) {
        this.sampleShortUnitType = Collections.singleton(sampleType);
        this.sampleComposedShortUnitIds = sampleComposedLongUnits;
        this.features = features == null ? Collections.emptySet() : ImmutableSet.copyOf(features);
    }

    public static UnitPathType getPathType(XPathParts parts) {
        String el2 = parts.getElement(2);
        if (el2.equals("durationUnit")) {
            String lastEl = parts.getElement(-1);
            if (lastEl.equals("durationUnitPattern")) {
                return UnitPathType.duration;
            } else {
                return null;
            }
        }
        if (!el2.equals("unitLength")) {
            return null;
        }
        switch (parts.getElement(-1)) {
        case "compoundUnitPattern": return "times".equals(parts.getAttributeValue(-2, "type")) ? UnitPathType.times : UnitPathType.per;
        case "unitPrefixPattern": return UnitPathType.prefix;
        case "compoundUnitPattern1": return UnitPathType.power;
        case "unitPattern": return UnitPathType.unit;
        case "perUnitPattern": return UnitPathType.perUnit;
        case "prefix": return UnitPathType.prefix;
        case "gender": return UnitPathType.gender;
        case "coordinateUnitPattern": return UnitPathType.coordinate;
        case "durationUnit": return UnitPathType.duration;
        case "alias": return null;
        case "displayName": return UnitPathType.displayName;
        }
        throw new IllegalArgumentException("PathType: " + parts);
    }

    public String getTranslationPath(LocaleStringProvider resolvedFile, String width, String shortUnitId, String pluralCategory,
        String caseVariant, String genderVariant) {
        UnitPathType pathType = this;
        final String pathPrefix = "//ldml/units/unitLength[@type=\"" + width + "\"]/";
        String longUnitId;
        GrammarInfo grammarInfo1;
        UnitConverter uc = CLDRConfig.getInstance().getSupplementalDataInfo().getUnitConverter();

        String grammaticalAttributes;
        switch (pathType) {
        case times:
            return pathPrefix + "compoundUnit[@type=\"" + "times" + "\"]/compoundUnitPattern";
        case per:
            return pathPrefix + "compoundUnit[@type=\"" + "per" + "\"]/compoundUnitPattern";
        case prefix:
            longUnitId = CLDRConfig.getInstance().getSupplementalDataInfo().getUnitConverter().getLongId(shortUnitId);
            return pathPrefix + "compoundUnit[@type=\"" + longUnitId + "\"]/unitPrefixPattern";
        case power:
            longUnitId = uc.getLongId(shortUnitId);
            grammarInfo1 = CLDRConfig.getInstance().getSupplementalDataInfo().getGrammarInfo(resolvedFile.getLocaleID());
            grammaticalAttributes = GrammarInfo.getGrammaticalInfoAttributes(grammarInfo1, pathType, pluralCategory, genderVariant, caseVariant);
            return pathPrefix + "compoundUnit[@type=\"" + longUnitId + "\"]/compoundUnitPattern1" + grammaticalAttributes;
        case unit:
            longUnitId = uc.getLongId(shortUnitId);
            grammarInfo1 = CLDRConfig.getInstance().getSupplementalDataInfo().getGrammarInfo(resolvedFile.getLocaleID());
            grammaticalAttributes = GrammarInfo.getGrammaticalInfoAttributes(grammarInfo1, pathType, pluralCategory, genderVariant, caseVariant);
            return pathPrefix + "unit[@type=\""  + longUnitId + "\"]/unitPattern" + grammaticalAttributes;
        case displayName:
            longUnitId = uc.getLongId(shortUnitId);
            return pathPrefix + "unit[@type=\""  + longUnitId + "\"]/displayName";
        case perUnit:
            longUnitId = uc.getLongId(shortUnitId);
            return pathPrefix + "unit[@type=\"" + longUnitId + "\"]/perUnitPattern";
        case gender:
            if (!width.equals("long")) {
                throw new IllegalArgumentException("illegal width for gender: ");
            }
            longUnitId = uc.getLongId(shortUnitId);
            return pathPrefix + "unit[@type=\"" + uc.getLongId(shortUnitId) + "\"]/gender";
        case coordinate:
            return pathPrefix + "coordinateUnit/coordinateUnitPattern[@type=\"" + shortUnitId + "\"]";
        case duration:
            return "//ldml/units/durationUnit[@type=\"" + shortUnitId + "\"]/durationUnitPattern";
        }
        throw new IllegalArgumentException("PathType: " + pathType);
    }

    public  String getTrans(LocaleStringProvider resolvedFile, String width, String shortUnitId, String pluralCategory, String caseVariant, String genderVariant, Multimap<UnitPathType, String> partsUsed) {
        UnitPathType pathType = this;
        String path = pathType.getTranslationPath(resolvedFile, width, shortUnitId, pluralCategory, caseVariant, genderVariant);
        String result = resolvedFile.getStringValue(path);
        if (result == null) {
            int debug = 0;
        }

        if (partsUsed != null) {
            CLDRFile.Status status = new CLDRFile.Status();
            String foundLocale = resolvedFile.getSourceLocaleID(path, status );
            partsUsed.put(pathType,
                (result != null ? "«" + result + "»": "∅")
                + (foundLocale.equals(resolvedFile.getLocaleID()) ? "" : "\n\t\tactualLocale: " + foundLocale)
                + (status.pathWhereFound.equals(path) ? "" : "\n\t\trequestPath: " + path + "\n\t\tactualPath:  " + status.pathWhereFound)
                );
        }
        return result;
    }
}