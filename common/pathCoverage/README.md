# Path Coverage Data
These are **draft** data files that can be used to find the coverage levels for each path in each locale file.
The format is a series of lines, where blank lines and lines starting with '#' are ignored, and lines are trimmed.

The lines have the following format:

## Variables
Variables can be assigned for use in later rules.
```
variableAssignment := variable '=' values
variable := '%' [a-zA-Z0-9]+
values := value (',' value)*
value := [\-_A-Za-z0-9] // a list of cldr attribute values
```

## Rules
Rules are used to find the coverage for a path. 
The structure is the following.

A _chassis_ is an XPath in CLDR, where the attribute values have been removed, along with the preceding '='.
A rule for a chassis is of the following form:

```
rule := 'path=' chassis levelTest* \n finalLevel
levelTest := 'level=' level \n attributesMatches*
level := 'core'|'basic'|'moderate'|'modern'|'comprehensive'
attributesMatches := attributesMatch ('or' \n attributesMatch)*
attributesMatch := attribute '=' variable | values \n
attribute := 'attr' attributeNumber
attributeNumber := \d
finalLevel := 'finalLevel=' level \n
```

Example:
```
path=//ldml/dates/calendars/calendar[@type]/dateTimeFormats/intervalFormats/intervalFormatItem[@id]/greatestDifference[@id]
 level=moderate
  attr0=gregorian
  attr1=%intervalFormatItem31
or
  attr0=generic
  attr1=%intervalFormatItem23
 level=modern
  attr0=generic,gregorian
  attr1=Bh,Bhm
 finalLevel=comprehensive
```

Logically, this is read into a main map from chassises to a submap from attributeMatchers to levels.
An 'or' value logically just copies the previous level. Thus the above corresponds to:

```
chassis → 
   attr0 ∈ {gregorian} AND attr1 ∈ {value(%intervalFormatItem31)}
     → moderate
   attr0 ∈ {generic} AND attr1 ∈ {value(%intervalFormatItem23)}
     → moderate
   attr0 ∈ {generic,gregorian} AND attr1 ∈ {Bh,Bhm}
     → modern
   ELSE --> comprehensive
```

To use that information to get a level from a path, 
that path is first converted to a chassis plus a map from attributeNumber to attributeValue.

1. The chassis is looked up in the main map to get the submap. 
2. If there is none, the resulting level is `comprehensive`.
3. Then the path's map from attributeNumber to attributeValue is checked against the attributeMatchers, until a match is found.
4. If there is none, the finalLevel is returned
