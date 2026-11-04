CLDR properties directory

Copyright © 1991-2020 Unicode, Inc.
For terms of use, see https://www.unicode.org/copyright.html
Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the U.S. and other countries.
CLDR data files are interpreted according to the LDML specification (https://www.unicode.org/reports/tr35/)

This directory contains additional information for CLDR. Currently, this includes:

* segments/ — segmentation customization for the UCD
* ExtendedPictographic.txt — a file that has been deprecated, since the data is now the UCD.
* external_data_versions.tsv — a tsv file with information about external data used in CLDR
* labels.txt — a file with label information for emoji (not up to date)
* scriptMetadata.txt — a file with additional data about Unicode scripts
* unit_categories.tsv: unit categories with stable numeric IDs and version history
* unit_quantities.tsv: unit quantities (what a unit measures, such as length) with stable numeric IDs, base units, and version history
* units_info.tsv: units with stable numeric IDs, complexity (simple, prefixed, single, core, or mixed), conversion factors, base units, measurement systems, and version history; format_only marks units that can't be converted, and reciprocal_quantity names the quantity in the same category reached with 1/x
  * The rows are the units with display names of their own, plus the units that CLDR data uses without them: the units in the unit preferences (such as foot-and-inch and millimeter-per-hour) and the base unit of each quantity (such as kilogram-square-meter-per-square-second for energy).
  * display_name says where a unit's display names come from: own (a `<unit type>` in the locale data), composed (built from its parts with the locale's patterns), or none (CLDR can't build them, as for a power above 3). A unit without names of its own gets the category of the named units with its quantity, or no category if there is none.

In unit_categories.tsv, unit_quantities.tsv, and units_info.tsv, an ID never changes and is never reused. A unit_id names one unit, whatever its category or quantity: no two rows share a unit_id or a unit, and a unit keeps its unit_id if it moves to another category (karat moved from proportion to concentr in CLDR 29).
