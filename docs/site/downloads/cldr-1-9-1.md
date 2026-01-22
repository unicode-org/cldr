---
title: CLDR 1.9.1 Release Note
---

# CLDR 1.9.1 Release Note

The following are the files for this release. For a description of their purpose and format, see [CLDR Releases (Downloads)].

| No. | Date | Rel. Note | Data | Spec | Delta Tickets | GitHub Tag |
|:---:|:----------:|:---------:|:--------:|:------------:|:---:|:----------:|
| 1.9.1 | 2011-03-11 | [v1.9.1][] | [CLDR1.9.1][] | [LDML1.9][] | [~~Δ1.9.1~~][] | release-1-9-1 |

Unicode CLDR 1.9.1 is an update release, with no new translations. Changes include the following:

## Data

- Based on various corrections and improvements to the source data, updated the Chinese pinyin and stroke collations, updated the Han-Latin and Han-Latin/Names transforms, and updated the index exemplar characters for zh_Hant.
- Added search collators for Czech and Slovak.
- Updated time zone support (through Olson 2011c).
- Fixed the Thai grapheme break iterator to remove '+' from the Extend class.

## Specification

- There were no specification updates for this release, please see the LDML 1.9 specification.

Note: The POSIX data files were not updated for this release; the latest version of CLDR POSIX data is 1.9.

See the Delta link above for a full list of changes.  For information on changes in the 1.9 release, see the [CLDR 1.9 Release Note].

## Key

- The Release Note contains a general description of the contents of the release, and any relevant notes about the release.
- The Data link points to a set of zip files containing the contents of the release (the files are complete in themselves, and do not require files from earlier releases -- for the structure of the zip file, see Repository Organization).
- The Spec is the version of UTS #35: LDML that corresponds to the release.
- The Delta document points to a list of all the bug fixes and features in the release, which be used to get the precise corresponding file changes using BugDiffs.)
- The SVN Tag can be used to get the files via Repository Access.

[CLDR Releases (Downloads)]: /index/downloads
[CLDR 1.9 Release Note]: /index/downloads/cldr-1-9
<!-- 1.9.1 release: 2011-03-11 -->
[v1.9.1]: /index/downloads/cldr-1-9-1
[CLDR1.9.1]: https://unicode.org/Public/cldr/1.9.1/
[LDML1.9]: https://www.unicode.org/reports/tr35/tr35-17.html
[~~Δ1.9.1~~]: https://unicode.org/cldr/trac/query?status=closed&milestone=1.9.1
