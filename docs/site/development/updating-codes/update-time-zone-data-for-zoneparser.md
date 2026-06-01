---
title: Update Time Zone Data for ZoneParser
---

# Update Time Zone Data for ZoneParser

Note: This is usually done as a part of full time zone data update process.

1. Download and unpack the latest version of the TZDB from the [IANA Time Zone Database page](https://www.iana.org/time-zones)
    - `mkdir tzdata && wget -qO- https://data.iana.org/time-zones/tzdata-latest.tar.gz | tar -xzf - -C tzdata`
2. Generate the "rearguard" version of the TZDB
    - CLDR display names assume positive daylight saving offsets. The "rearguard" version is designed for tools without negative daylight saving time support.
    - `make -C tzdata rearguard.zi`
3. Copy `rearguard.zi`, `version` and `zone.tab` to `tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/tzdb`
    - `cp tzdata/{rearguard.zi,version,zone.tab} tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/tzdb`
4. Clean up the working directory
    - `rm -r tzdata`
5. **Record the version: See** [**Updating External Metadata**](/development/updating-codes/external-version-metadata)
