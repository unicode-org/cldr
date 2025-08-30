---
title: Copy en_GB to en_001
---


# BRS: Copy en\_GB to en\_001

This task is required to maintain en_001 following the addition of new items or deeper review of existing data.
Many English locales inherit locale data from [en_001][]

The program **CompareEn.java** can be used to copy data from en_GB up to en_001.

### Tool Options

-   \-u (uplevel) — move elements from en_GB into en_001. By default, the output directory is common/main and common/annotations in trunk
    -   If not present, just write a comparison to Generated/cldr/comparison/en.txt
-   \-v (verbose) — provide verbose output

## Process

1.  Run with no options first.
    1.  That generates a file that indicates what changes would be made.
    2.  Put that file in a spreadsheet
    3.  Post to the CLDR TC for review.
    4.  You'll then want to retract any items that shouldn't be copied.
    5.  Change CompareEn.java if there are paths that should be skipped in the future.
2.  Once you agree on the results, you'll run -u.
    1.  That will modify your local copy of en\_oo1.xml
    2.  Then do a diff with HEAD to make sure it matches expectations
    3.  Then check in en\_oo1.xml and CompareEn.java

## Review guidelines for inclusion in en_001

World English i.e. en_001 is intended to be as neutral as possible. Only data for items which differ from en need to be upleveled.

This would mean making en_001 have the following characteristics:
- Date formats
    - For full, long, medium formats use European style day, month, year. Numeric dates are also day, month, year order where both day and month are zero padded.
    - en_001 includes a comma following the day name while en_GB does not.
- Time formats - Use 12 hour clock, as this is the predominant format for English speaking locales ( ref : https://en.wikipedia.org/wiki/Date_and_time_representation_by_country)
- Spelling of unit names - (metre vs. meter) - Since the BIPM spelling is “metre”, “centimetre”, etc., The “en_001” locale should prefer “metre” over the American spelling “meter”.
- Compact decimal number formats currently inherit from en until there is evidence that the en_GB abbreviations are the most common globally.
- For units such as gallon that have both a “US” version and an “imperial” version: Both versions should have the qualifier (“US” or “imperial”), should not remove the “imperial” as in the GB versions.
- Time zone names: en_001 should use “∅∅∅” to cancel the short metazone names inherited from en since US short metazone names are not relevant for most of the world.
- Emoji 'names' are determined by the Technical Committee depending on whether the en_GB or en term is more common, while emoji annotation keywords are intended to be a combination of the both locales.

Specific items not in this list may need research to determine common usage.

More about the history of [en_001][]

[en_001]: https://cldr.unicode.org/development/development-process/design-proposals/english-inheritance
