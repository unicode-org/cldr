The 1.3 version of CLDR is at alpha status.

The alpha is *not* complete: it does not contain changes for all the
additions to the specification and additions/fixes to the data slated for
CLDR 1.3, and should not be used in implementations. We do, however,
encourage people to look at the data and provide feedback on the progress
towards CLDR 1.3, especially on the new POSIX-format data and the changes
in the specification.

The main features are:
- The addition of new data to support localization of timezones
- The addition of data for UN M.49 regions, including continents and regions
- A complete set of POSIX-format data generated from the XML format, plus a
tool to generate versions for different platforms.
- The canonicalization of the data files, including the consolidation of
inherited data
- The restriction of currency codes to ISO 4217 codes (past and present)
- The addition of number and data tests, for verifying that implementations
correctly implement the LDML specification using CLDR data.
- Various other fixes and additions of data, and extensions to the
specification.

In the specification, please look at the changed areas (in yellow), especially
those starting with "Issues:"

NOTE: Feedback should not be provided via the online Unicode forms; instead,
use http://unicode.org/cldr/filing_bug_reports.html
