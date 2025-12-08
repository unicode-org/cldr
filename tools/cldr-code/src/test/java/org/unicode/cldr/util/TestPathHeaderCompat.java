package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

// mvn test --file tools/pom.xml -pl cldr-code -Dtest=org.unicode.cldr.util.TestPathHeaderCompat

public class TestPathHeaderCompat {

    private static final Logger logger =
            Logger.getLogger(TestPathHeaderCompat.class.getSimpleName());

    private static final boolean VERBOSE = false;

    private static final Set<String> pageIdNames = new TreeSet<>();

    /**
     * This is based on PathHeader.PageId, and includes additional items for backward compatibility,
     * to test that old Page names are still supported by PathHeader.PageId.fromStringCompatible().
     * When a page name changes or is removed from PathHeader.PageId, keep the old name here. If a
     * test fails, revision may be needed here or in PathHeader.PageId.fromStringCompatible().
     */
    private enum compatiblePage {
        Alphabetic_Information(PathHeader.SectionId.Core_Data, "Alphabetic Information"),
        Numbering_Systems(PathHeader.SectionId.Core_Data, "Numbering Systems"),
        LinguisticElements(PathHeader.SectionId.Core_Data, "Linguistic Elements"),

        Locale_Name_Patterns(PathHeader.SectionId.Locale_Display_Names, "Locale Name Patterns"),
        Languages_A_D(PathHeader.SectionId.Locale_Display_Names, "Languages (A-D)"),
        Languages_E_J(PathHeader.SectionId.Locale_Display_Names, "Languages (E-J)"),
        Languages_K_N(PathHeader.SectionId.Locale_Display_Names, "Languages (K-N)"),
        Languages_O_S(PathHeader.SectionId.Locale_Display_Names, "Languages (O-S)"),
        Languages_T_Z(PathHeader.SectionId.Locale_Display_Names, "Languages (T-Z)"),
        Scripts(PathHeader.SectionId.Locale_Display_Names),
        Territories(PathHeader.SectionId.Locale_Display_Names, "Geographic Regions"),
        T_NAmerica(PathHeader.SectionId.Locale_Display_Names, "Territories (North America)"),
        T_SAmerica(PathHeader.SectionId.Locale_Display_Names, "Territories (South America)"),
        T_Africa(PathHeader.SectionId.Locale_Display_Names, "Territories (Africa)"),
        T_Europe(PathHeader.SectionId.Locale_Display_Names, "Territories (Europe)"),
        T_Asia(PathHeader.SectionId.Locale_Display_Names, "Territories (Asia)"),
        T_Oceania(PathHeader.SectionId.Locale_Display_Names, "Territories (Oceania)"),
        Locale_Variants(PathHeader.SectionId.Locale_Display_Names, "Locale Variants"),
        Keys(PathHeader.SectionId.Locale_Display_Names),

        Fields(PathHeader.SectionId.DateTime),
        Relative(PathHeader.SectionId.DateTime),
        Gregorian(PathHeader.SectionId.DateTime),
        Gregorian_YMD(PathHeader.SectionId.DateTime, "Gregorian YMD"),

        // ISO8601 is obsolete but mapped to Gregorian_YMD for testing backward compatibility
        ISO8601(PathHeader.SectionId.DateTime),
        Generic(PathHeader.SectionId.DateTime),
        Buddhist(PathHeader.SectionId.DateTime),
        Chinese(PathHeader.SectionId.DateTime),
        Coptic(PathHeader.SectionId.DateTime),
        Dangi(PathHeader.SectionId.DateTime),
        Ethiopic(PathHeader.SectionId.DateTime),
        Ethiopic_Amete_Alem(PathHeader.SectionId.DateTime, "Ethiopic-Amete-Alem"),
        Hebrew(PathHeader.SectionId.DateTime),
        Indian(PathHeader.SectionId.DateTime),
        Islamic(PathHeader.SectionId.DateTime),
        Japanese(PathHeader.SectionId.DateTime),
        Persian(PathHeader.SectionId.DateTime),
        Minguo(PathHeader.SectionId.DateTime),

        Timezone_Display_Patterns(PathHeader.SectionId.Timezones, "Timezone Display Patterns"),
        NAmerica(PathHeader.SectionId.Timezones, "North America"),
        SAmerica(PathHeader.SectionId.Timezones, "South America"),
        Africa(PathHeader.SectionId.Timezones),
        Europe(PathHeader.SectionId.Timezones),
        Russia(PathHeader.SectionId.Timezones),
        WAsia(PathHeader.SectionId.Timezones, "Western Asia"),
        CAsia(PathHeader.SectionId.Timezones, "Central Asia"),
        EAsia(PathHeader.SectionId.Timezones, "Eastern Asia"),
        SAsia(PathHeader.SectionId.Timezones, "Southern Asia"),
        SEAsia(PathHeader.SectionId.Timezones, "Southeast Asia"),
        Australasia(PathHeader.SectionId.Timezones),
        Antarctica(PathHeader.SectionId.Timezones),
        Oceania(PathHeader.SectionId.Timezones),
        UnknownT(PathHeader.SectionId.Timezones, "Unknown Region"),
        Overrides(PathHeader.SectionId.Timezones),

        Symbols(PathHeader.SectionId.Numbers),
        Number_Formatting_Patterns(PathHeader.SectionId.Numbers, "Number Formatting Patterns"),
        Compact_Decimal_Formatting(PathHeader.SectionId.Numbers, "Compact Decimal Formatting"),
        Compact_Decimal_Formatting_Other(
                PathHeader.SectionId.Numbers,
                "Compact Decimal Formatting (Other Numbering Systems)"),

        Measurement_Systems(PathHeader.SectionId.Units, "Measurement Systems"),
        Duration(PathHeader.SectionId.Units),
        Graphics(PathHeader.SectionId.Units),
        Length_Metric(PathHeader.SectionId.Units, "Length Metric"),
        Length_Other(PathHeader.SectionId.Units, "Length Other"),
        Area(PathHeader.SectionId.Units),
        Volume_Metric(PathHeader.SectionId.Units, "Volume Metric"),
        Volume_US(PathHeader.SectionId.Units, "Volume US"),
        Volume_Other(PathHeader.SectionId.Units, "Volume Other"),
        SpeedAcceleration(PathHeader.SectionId.Units, "Speed and Acceleration"),
        MassWeight(PathHeader.SectionId.Units, "Mass and Weight"),
        EnergyPower(PathHeader.SectionId.Units, "Energy and Power"),
        ElectricalFrequency(PathHeader.SectionId.Units, "Electrical and Frequency"),
        Weather(PathHeader.SectionId.Units),
        Digital(PathHeader.SectionId.Units),
        Coordinates(PathHeader.SectionId.Units),
        OtherUnitsMetric(PathHeader.SectionId.Units, "Other Units Metric"),
        OtherUnitsMetricPer(PathHeader.SectionId.Units, "Other Units Metric Per"),
        OtherUnitsUS(PathHeader.SectionId.Units, "Other Units US"),
        OtherUnits(PathHeader.SectionId.Units, "Other Units"),
        CompoundUnits(PathHeader.SectionId.Units, "Compound Units"),

        Displaying_Lists(PathHeader.SectionId.Misc, "Displaying Lists"),
        MinimalPairs(PathHeader.SectionId.Misc, "Minimal Pairs"),
        PersonNameFormats(PathHeader.SectionId.Misc, "Person Name Formats"),
        Transforms(PathHeader.SectionId.Misc),

        Identity(PathHeader.SectionId.Special),
        Version(PathHeader.SectionId.Special),
        Suppress(PathHeader.SectionId.Special),
        Deprecated(PathHeader.SectionId.Special),
        Unknown(PathHeader.SectionId.Special),

        C_NAmerica(PathHeader.SectionId.Currencies, "North America (C)"),
        // need to add (C) to differentiate from Timezone territories
        C_SAmerica(PathHeader.SectionId.Currencies, "South America (C)"),
        C_NWEurope(PathHeader.SectionId.Currencies, "Northern/Western Europe"),
        C_SEEurope(PathHeader.SectionId.Currencies, "Southern/Eastern Europe"),
        C_NAfrica(PathHeader.SectionId.Currencies, "Northern Africa"),
        C_WAfrica(PathHeader.SectionId.Currencies, "Western Africa"),
        C_MAfrica(PathHeader.SectionId.Currencies, "Middle Africa"),
        C_EAfrica(PathHeader.SectionId.Currencies, "Eastern Africa"),
        C_SAfrica(PathHeader.SectionId.Currencies, "Southern Africa"),
        C_WAsia(PathHeader.SectionId.Currencies, "Western Asia (C)"),
        C_CAsia(PathHeader.SectionId.Currencies, "Central Asia (C)"),
        C_EAsia(PathHeader.SectionId.Currencies, "Eastern Asia (C)"),
        C_SAsia(PathHeader.SectionId.Currencies, "Southern Asia (C)"),
        C_SEAsia(PathHeader.SectionId.Currencies, "Southeast Asia (C)"),
        C_Oceania(PathHeader.SectionId.Currencies, "Oceania (C)"),
        C_Unknown(PathHeader.SectionId.Currencies, "Unknown Region (C)"),

        // BCP47
        u_Extension(PathHeader.SectionId.BCP47),
        t_Extension(PathHeader.SectionId.BCP47),

        // Supplemental
        Alias(PathHeader.SectionId.Supplemental),
        IdValidity(PathHeader.SectionId.Supplemental),
        Locale(PathHeader.SectionId.Supplemental),
        RegionMapping(PathHeader.SectionId.Supplemental),
        WZoneMapping(PathHeader.SectionId.Supplemental),
        Transform(PathHeader.SectionId.Supplemental),
        Units(PathHeader.SectionId.Supplemental),
        Likely(PathHeader.SectionId.Supplemental),
        LanguageMatch(PathHeader.SectionId.Supplemental),
        TerritoryInfo(PathHeader.SectionId.Supplemental),
        LanguageInfo(PathHeader.SectionId.Supplemental),
        LanguageGroup(PathHeader.SectionId.Supplemental),
        Fallback(PathHeader.SectionId.Supplemental),
        Gender(PathHeader.SectionId.Supplemental),
        Grammar(PathHeader.SectionId.Supplemental),
        Metazone(PathHeader.SectionId.Supplemental),
        NumberSystem(PathHeader.SectionId.Supplemental),
        Plural(PathHeader.SectionId.Supplemental),
        PluralRange(PathHeader.SectionId.Supplemental),
        Containment(PathHeader.SectionId.Supplemental),
        Currency(PathHeader.SectionId.Supplemental),
        Calendar(PathHeader.SectionId.Supplemental),
        WeekData(PathHeader.SectionId.Supplemental),
        Measurement(PathHeader.SectionId.Supplemental),
        Language(PathHeader.SectionId.Supplemental),
        Script(PathHeader.SectionId.Supplemental),
        RBNF(PathHeader.SectionId.Supplemental),
        Segmentation(PathHeader.SectionId.Supplemental),
        DayPeriod(PathHeader.SectionId.Supplemental),

        Category(PathHeader.SectionId.Characters),

        // [Smileys, People, Animals & Nature, Food & Drink, Travel & Places, Activities, Objects,
        // Symbols, Flags]
        Smileys(PathHeader.SectionId.Characters, "Smileys & Emotion"),
        People(PathHeader.SectionId.Characters, "People & Body"),
        People2(PathHeader.SectionId.Characters, "People & Body 2"),
        Animals_Nature(PathHeader.SectionId.Characters, "Animals & Nature"),
        Food_Drink(PathHeader.SectionId.Characters, "Food & Drink"),
        Travel_Places(PathHeader.SectionId.Characters, "Travel & Places"),
        Travel_Places2(PathHeader.SectionId.Characters, "Travel & Places 2"),
        Activities(PathHeader.SectionId.Characters),
        Objects(PathHeader.SectionId.Characters),
        Objects2(PathHeader.SectionId.Characters),
        EmojiSymbols(PathHeader.SectionId.Characters, "Emoji Symbols"),
        Punctuation(PathHeader.SectionId.Characters),
        MathSymbols(PathHeader.SectionId.Characters, "Math Symbols"),
        OtherSymbols(PathHeader.SectionId.Characters, "Other Symbols"),
        Flags(PathHeader.SectionId.Characters),
        Component(PathHeader.SectionId.Characters),
        Typography(PathHeader.SectionId.Characters),
        ;

        compatiblePage(PathHeader.SectionId ignoredSectionId, String... alternateNames) {
            // ignoredSectionId is present in the constructor for convenience only, since
            // the enum is originally copied from PathHeader.PageId
            pageIdNames.add(this.name());
            pageIdNames.addAll(List.of(alternateNames));
        }
    }

    /** Confirm that every item in PathHeader.PageId is present in compatiblePage */
    @Test
    public void testCompatiblePage() {
        assertEquals(compatiblePage.Alphabetic_Information.name(), "Alphabetic_Information");
        // Due to lazy loading, pageIdNames.size() may be zero unless a member of compatiblePage
        // has been referenced previously, as in the line above for Alphabetic_Information
        assertNotEquals(pageIdNames.size(), 0, "pageIdNames should be non-empty");
        log(
                "In testCompatiblePage; pageIdNames.size() = "
                        + pageIdNames.size()
                        + " and pageIdNames.toString() = "
                        + pageIdNames);
        for (PathHeader.PageId pageId : PathHeader.PageId.values()) {
            try {
                String pageString = pageId.toString(), pageName = pageId.name();
                assertTrue(
                        pageIdNames.contains(pageString),
                        "Expected pageIdNames to contain string " + pageString);
                if (!pageName.equals(pageString)) {
                    assertTrue(
                            pageIdNames.contains(pageName),
                            "Expected pageIdNames to contain name " + pageName);
                }
                log("testCompatiblePage OK for " + pageId);
            } catch (Throwable t) {
                fail(t);
            }
        }
    }

    /**
     * Confirm that every item in compatiblePage works for PathHeader.PageId.fromStringCompatible
     */
    @Test
    public void testPageId() {
        assertEquals(compatiblePage.Alphabetic_Information.name(), "Alphabetic_Information");
        // Due to lazy loading, pageIdNames.size() may be zero unless a member of compatiblePage
        // has been referenced previously, as in the line above for Alphabetic_Information
        assertNotEquals(pageIdNames.size(), 0, "pageIdNames should be non-empty");
        for (String pageName : pageIdNames) {
            try {
                // fromStringCompatible throws an exception if pageName isn't found
                PathHeader.PageId pageId = PathHeader.PageId.fromStringCompatible(pageName);
                log("testPageId OK for " + pageId);
            } catch (Throwable t) {
                fail(t);
            }
        }
    }

    private void log(String s) {
        if (VERBOSE) {
            logger.warning(s);
        } else {
            logger.fine(s);
        }
    }
}
