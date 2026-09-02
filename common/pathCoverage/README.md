# Path Coverage Data
These are **draft** data files that can be used to find the coverage levels for each path in each locale file.
The format is a series of lines, where blank lines and lines starting with '#' are ignored.
The lines have the following format:

## Variables
Variables can be assigned for use in later rules.
```
variableAssignment := variable '=' value
variable := '%' [a-zA-Z0-9]+
regex := [\p{ascii}-\p{S}-\p{C}] // a valid regex expression. Currently only alternations of ascii attribute values
```

## Rules
Rules are used to find the coverage for a path. 
The structure is the following.

A _chassis_ is an XPath in CLDR, where the attribute values have been removed, along with the preceding '='.
A rule for a chassis is of the following form:

rule := 'path=' chassis levelTest* \n finalLevel
levelTest := 'level=' level \n attributesMatch*
level := 'core'|'basic'|'moderate'|'modern'|'comprehensive'
attributesMatch := attribute '=' variable | regex \n
attribute := 'attr' attributeNumber
attributeNumber := \d
finalLevel := 'finalLevel=' level \n

Example:
```
path=//ldml/dates/calendars/calendar[@type]/dateTimeFormats/intervalFormats/intervalFormatItem[@id]/greatestDifference[@id]
level=moderate
attr0=gregorian
attr1=%intervalFormatItem31
attr0=generic
attr1=%intervalFormatItem23
level=modern
attr0=generic|gregorian
attr1=Bh|Bhm
```

The following describes the lookup process in pseudocode.
It assumes that the data in the file has been read into a Map, and as usual, can be optimized.

1. Let chassis = chassis(path)
2. Let attributes = a map from integers to attribute values in the path
3. Let levelTests = Map(chassis)
4. For each level test
   1. Record the level.
   2. Set result = true
   3. Set group = {}
   4. For each attributesMatch
      1. If the group contains attributeNumber
          1. If result = true, return the level
          2. else set result = true, group = {}
      2. Add attributeNumber to group.
      3. Let attributeValue = attributes(attributeNumber)
      4. Let result = result & regex.matches(attributeValue)
 5. If this point is reached, then there has not been a match, and the level in finalLevel is returned