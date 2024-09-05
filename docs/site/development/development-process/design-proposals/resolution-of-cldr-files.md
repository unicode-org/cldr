---
title: Resolution of CLDR files
---

# Resolution of CLDR files

These are some notes on how CLDR files are resolved. It is a basic description of the process to help people to understand how the code works. Some of the details may be a bit off if the code changes. The code could undoubtedly be improved both for efficiency and maintainability – this is just an attempt to document what happens now.

If significant changes are made, please update this document.

## XMLSource

Behind each CLDR file is an XMLSource, which manages access to the data. It provides for iteration through all of the distinguished paths in the file, and getting the values associated with each such path. While there is more than one item associated with the path (the value, comments, and the full path), we'll focus on the element value.

There are three main implementations:

1. **simple file access** - uncomplicated, just reads an XML file and produces a map from paths to values.
2. **survey tool access** - accesses a database for use in the survey tool
3. **resolving access** - produces a resolving XMLSource, based on one of the first two. That is, it has a main file which is one of the first two, and can also create any needed other one as necessary: a parent locale, or a locale pointed to by an alias.

The resolution process is fairly complicated. The main issues are lookup and iteration. Of them, iteration is somewhat harder. However, due to aliases being restricted to the root locale since CLDR 2.0, the process has been made a lot easier.

## Lookup

Lookup would be easy if it weren't for aliases. Here's how it works.

**Start with the main file.**

1. Look in the file. If found, return the value.
2. If the value is not found, look in the parent recursively.
3. *If not found when root has been reached and checked, see if the path has an alias in root.*
	1. Example:
		1. looking up
	2. //ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="stand-alone"]/dayPeriodWidth[@type="narrow"]/dayPeriod[@type="am"]
		1. will match
	3. //ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="stand-alone"]/**alias**[@source="**locale**"][@path="**../dayPeriodContext[@type='format']**"]
	4. If so, construct two items:
		1. sourceLocale
		2. resolvedPath
	5. Example from above:
	6. //ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="**format**"]/dayPeriodWidth[@type="narrow"]/dayPeriod[@type="am"]
	7. Recursively lookup the path in the source locale, and return the value.
		1. Note that a locale of "locale" means to lookup in the *original* locale (of the main file).
4. Repeat from step 1 using the resolvedPath until a value is reached or no more aliases are found.
5. If not found, there is a special file that is algorithmically constructed called CODE-FALLBACK, so look there.
6. If not found there, return null (fail).

This process can get complicated. If we look in the sr-YU locale for \<ethiopic calendar dayPeriod, stand-alone, narrow, am>

- it looks first in sr-YU
- then got to the parent, sr
- then root. Finds an alias as above redirecting to format
- look back at sr-YU, now for dayPeriod, format, narrow, am
- look in the parent sr
- and so on.
- Eventually we find the value in gregorian calendar, dayPeriod, format, wide, am

Internally, the code caches the location (targetLocale and resolved path) for each path, so that a second lookup is fast. Note that the target locale "locale" needs to bump all the way up to the top each time, so that the appropriate localized resources are found if they are there.

## Iteration

Iteration is more complicated. We have to figure out whether *any possible* path would return a value in lookup. Again, this would be very simple if it weren't for aliases. Here's how it works.

**Start with the main file.**

1. Find the set of all non-aliased paths in the file and each of its parents, and sort it by path.
2. Collect all the aliases in root and obtain a reverse mapping of aliases, i.e. destinationPath to sourcePath. Sort it by destinationPath.
3. Working backwards, use each reverse alias on the path set to get a set of new paths that would use the alias to map to one of the paths in the original set.
4. Add the new set of paths to the original set of paths, and use the new set as input into step 3. Repeat until there are no more new paths found.

A set of all the paths is cached on the first access for iteration, using the above process. For lookup, the incoming path is checked against the cached set of paths, and the lookup process takes place only if the value is not already cached. The iteration and lookup processes are performed separately because they are both optimized for their individual use cases. Iteration would slow down significantly if value storage was performed at the same time.


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)