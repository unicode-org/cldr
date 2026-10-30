/**
 * Generates {@code common/properties/units_info.tsv} and {@code unit_categories.tsv}: the list of
 * formattable units that ICU4C, ICU4J, and ICU4X generate their built-in units from.
 *
 * <p>Start with {@link org.unicode.cldr.tool.unitsinfo.GenerateUnitsInfo}. Each other class does
 * one job:
 *
 * <ul>
 *   <li>{@link org.unicode.cldr.tool.unitsinfo.UnitsInfo}: the two files, their columns, and rows.
 *   <li>{@code CldrUnits}: which units CLDR names, in this checkout and at each release tag.
 *   <li>{@link org.unicode.cldr.tool.unitsinfo.UnitHistory}: when each unit and category was added
 *       and deprecated.
 *   <li>{@link org.unicode.cldr.tool.unitsinfo.StableIds}: keeps every ID, and gives new units and
 *       categories the next free ID.
 *   <li>{@code UnitConversions}: each unit's conversion to its base unit, from CLDR's converter.
 *   <li>{@link org.unicode.cldr.tool.unitsinfo.Conversion} and {@code ScientificRational}: that
 *       conversion as signed integers.
 * </ul>
 */
package org.unicode.cldr.tool.unitsinfo;
