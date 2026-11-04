/**
 * Generates {@code common/properties/units_info.tsv}, {@code unit_categories.tsv}, and {@code
 * unit_quantities.tsv}: the list of units that CLDR data names (units with display names, plus the
 * units it uses without them, such as foot-and-inch), which ICU4C, ICU4J, and ICU4X generate their
 * built-in units from.
 *
 * <p>Start with {@link org.unicode.cldr.tool.unitsinfo.GenerateUnitsInfo}. Each other class does
 * one job:
 *
 * <ul>
 *   <li>{@link org.unicode.cldr.tool.unitsinfo.UnitsInfo}: the three files, their columns, and
 *       rows.
 *   <li>{@code CldrUnits}: which units CLDR names and uses in its unit preferences, in this
 *       checkout and at each release tag.
 *   <li>{@link org.unicode.cldr.tool.unitsinfo.UnitHistory}: when each unit and category was added
 *       and deprecated.
 *   <li>{@link org.unicode.cldr.tool.unitsinfo.StableIds}: keeps every ID, and gives new units and
 *       categories the next free ID.
 *   <li>{@link org.unicode.cldr.tool.unitsinfo.UnitConversions}: each unit's complexity, display
 *       names, and conversion to its base unit, from CLDR's converter.
 *   <li>{@link org.unicode.cldr.tool.unitsinfo.Conversion} and {@code ScientificRational}: that
 *       conversion as signed integers.
 * </ul>
 */
package org.unicode.cldr.tool.unitsinfo;
