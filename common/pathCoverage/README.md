# Path Coverage Data
These are **draft** data files that can be used to find the coverage levels for each path in each locale file.
The format is a series of lines, where blank lines and lines starting with '#' are ignored, and lines are trimmed.

The lines have the following format:

## Variables
Variables can be assigned for use in later rules.

```
variableAssignment := variable '=' values
variable := '%' cp+
cp := [\-_A-Za-z0-9]
values := value (',' value)*
value := cp+ // a list of cldr attribute values
```

## Rules
Rules are used to find the coverage for a path. 
The structure is the following.

A _chassis_ is an XPath in CLDR, where the attribute values have been removed, along with the preceding '='.
A rule for a chassis is of the following form:

```
rule := 'path=' chassis levelTest* \n elseLevel

levelTest := (attributesMatch* 'level=' level)* \n
attributesMatch := attribute '=' variable | values \n
attribute := 'attr' attributeNumber
attributeNumber := \d

level := 'core'|'basic'|'moderate'|'modern'|'comprehensive'
elseLevel := 'elseLevel=' level \n
```

[ wfc: The chassis within an attributesMatch must be unique and in ascending order, and must be valid according to CLDR ]
[ wfc: The attributeNumbers within an attributesMatch must be unique and in ascending order ]
[ wfc: The levels within a level test must be in non-descending order (eg, moderate..moderate is ok, but not moderate..basic ]

Example:

```
path=//ldml/dates/calendars/calendar[@type]/dateTimeFormats/intervalFormats/intervalFormatItem[@id]/greatestDifference[@id]
 attr0=gregorian
 attr1=%intervalFormatItem31
  level=moderate
 attr0=generic
 attr1=%intervalFormatItem23
  level=moderate
 attr0=generic,gregorian
 attr1=Bh,Bhm
  level=modern
  elseLevel=comprehensive
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
that path is first converted to a chassis plus an attribute map from attributeNumber to a set of attributeValues 
(or a variable that resolves to a set of attributeValues).

1. The chassis is looked up in the main map to get the submap. 
2. If there is none, the resulting level is `comprehensive`.
3. Then the path's attributeMap is checked against the attributeMatchers, until a match is found.
4. When a match is found, the previous level is returned
4. If there no match, the elseLevel is returned

Note: while the value associated with an attrN is logically a set,
it could be transformed by an implementation into another format, such as a regex.