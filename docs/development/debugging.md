---
title: Debugging Structure
---

# Debugging Structure
> _DRAFT!!_

Updating structure in CLDR can be challenging. 
Often that takes many steps, as documented on [Updating DTDs](docs/site/development/updating-dtds.md): 
DTDs, path headers, path descriptions, coverage, examples, and so on.
The tests help to catch problems, but it is often tricky to find out why a test is failing.

There are some tools that can help.
_The examples are from working on https://github.com/unicode-org/cldr/pull/5619_

## SearchXML.java

This tool searches the xml files, treating them as a list of <xpath,value> pairs.
Here is an example:

Running with options:
```
-pgregorian.*appendItems.*(Era|Timezone|Day-Of-Week) -f^(en|root|fr)$ -P
```
It lists the results on the console:
```
#-s	source	≝	/Users/markdavis/github/cldr/common/main/
#-f	file	≔	^(en|root|fr)$
#-p	path	≔	gregorian.*appendItems.*(Era|Timezone|Day-Of-Week)
#-P	PathHeader	≔	null

#?en.xml	{1}, {0}	//ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/appendItems/appendItem[@request="Day-Of-Week"]
	Date & Time	Gregorian	Formats - Flexible - Append	Day-Of-Week
	https://st.unicode.org/cldr-apps/v#/en//107872cff5810356
...
```
In the above, the path and the file are specified with [regex](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html); -v can also be used for values. 
The regular expressions do a "find", so if you want to force a match for the whole string, use ^ and $.
Missing values are assumed to be .*.
The -P option can be supplied to get the PathHeader as well (the = null just means that there was no parameter value)
You can get a list of the options with -?

This is very useful for finding particular paths and or values across all the locale files.

## SearchCLDR 

This tool is similar, except that knows about locale structure.
So, for example, you can use it to find and display information about resolved cldr files.
It is especially useful for for checking examples, coverage, root aliases, and so forth.

Running with options:
```
-f^(en|root)$ -p(gregorian|generic).*append.*(Era|Day-Of-Week|Time-Day-Of-Week|Timezone|Date-Timezone) -r -P
```
It lists the results on the console:
```
[-f^(en|root)$, -p(gregorian|generic).*append.*(Era|Day-Of-Week|Time-Day-Of-Week|Timezone|Date-Timezone), -r, -P]
#-s	source	≝	/Users/markdavis/github/cldr/common/main/
#-f	file	≔	^(en|root)$
#-p	path	≔	(gregorian|generic).*append.*(Era|Day-Of-Week|Time-Day-Of-Week|Timezone|Date-Timezone)
#-r	resolved	≔	null
#-P	PathStyle	≔	path

#	en	⟪Value⟫	Path	Source-Locale	Source-Path	Org-Level
#	en	⟪{0} {1}⟫	//ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/appendItems/appendItem[@request="Date-Timezone"]	en	≣	comprehensive
#	en	⟪{1}, {0}⟫	//ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/appendItems/appendItem[@request="Day-Of-Week"]	en	≣	moderate
```
_NOTE: The options are somewhat different for these tools (would be handing to unify them!)_

This also shows the coverage value, and will show where a path value comes from (if inherited).

## ConsoleCheckCLDR

This tool calls various Survey Tool infrastructure, and can show paths, values, examples, and other information.
It can also show which items are missing with the -m option, shown on the last line below.
That is handy for helping to figure out which paths are at the wrong coverage.

Running with options:
```
-f^(en|fr)$  -e  -m  -x  -pappendItems
```
It lists the results on the console:
```
#-c	coverage	≝	comprehensive
#-x	examples	≔	null
#-f	file_filter	≔	^(en|fr)$
#-p	path_filter	≔	appendItems
#-e	errors_only	≔	null
#-s	source_directory	≝	/Users/markdavis/github/cldr/common/main/,/Users/markdavis/github/cldr/common/annotations/,/Users/markdavis/github/cldr/seed/main/
#-S	source_all	≝	common,seed,exemplars
#-m	missingPaths	≔	null

# Locale	Status	▸PPath◂	〈Eng.Value〉	【Eng.Ex.】	〈Loc.Value〉	«fill-in»	【Loc.Ex】	⁅error/warning type⁆	❮Error/Warning Msg❯	Full Path	AliasedSource/Path?

en [English]	ok	▸Date_&_Time|Gregorian|Formats_-_Flexible_-_Append|Date-Timezone◂	〈{0} {1}〉	【〖❬Sep 5, 1999❭ ❬GMT❭〗】	〈{0} {1}〉	«=»	【〖❬Sep 5, 1999❭ ❬GMT❭〗】	⁅none⁆	❮ok❯	https://st.unicode.org/cldr-apps/v#/en//7fa71371abb195ab															
en [English]	ok	▸Date_&_Time|Gregorian|Formats_-_Flexible_-_Append|Day-Of-Week◂	〈{1}, {0}〉	【〖❬Sun❭, ❬Sep 5, 1999❭〗】	〈{1}, {0}〉	«=»	【〖❬Sun❭, ❬Sep 5, 1999❭〗】	⁅none⁆	❮ok❯	https://st.unicode.org/cldr-apps/v#/en//107872cff5810356															
...
fr [French]	Raw missing	Date & Time	Gregorian	Formats - Flexible - Append	Date-Timezone	{0} {1}	//ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/appendItems/appendItem[@request="Date-Timezone"]																		
```

[Updating DTDs]: development/updating-dtds
