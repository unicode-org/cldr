# Path Coverage Data
These are **Tech Preview** data files that can be used to find the coverage levels for each path in each locale file.
The format of these files is provisional; the final format might be different.
For example, the syntax could be changed, or it could be recast into JSON or XML.

## File Format

The Tech Preview format is a series of lines, where blank lines and lines starting with '#' are ignored, and lines are trimmed.

fileLines := import | variableAssignment | rule

Each type of `fileLines` is in one of the categories defined below. The `\n` notion indicates where newlines must appear.

## Terminology

A _chassis_ is an XPath where the attribute values have been removed, along with the preceding '='.
A _rule_ is an multi-line expression that maps a given chassis and sets of possible attribute values to a coverage level.
A file is ill-formed if it has two rules with the same chassis or two variableAssignments that have the same variable.

## Imports

Imports are used to reduce repetition of rules and variables in the files.

```
import := 'import=' locale
```

Logically, an import statement adds all imports, rules, and variables from the `locale` into the file at that point.

However, any conflicting rules or variables in the file override those imported rules and variables.
    * Two rules conflict if they have the same `chassis`.
    * Two variableAssignments conflict if they have the same `variable`

Imports are recursive. Suppose `en_CA` imports `en`, and `en` imports `root`.
Then the interpretation of the `en_CA` file logically consists of
* the rules in the `root` file, 
* plus new and overriding rules and variables from the `en` file,
* plus new and overriding rules and variables from the `en_CA`file.

## Variables

Variables can be assigned for use in later rules.

```
variableAssignment := variable '=' values
variable := '%' cp+
cp := [\-_A-Za-z0-9]
values := value (',' value)*
value := cp+ // a list of cldr attribute values
```

### Example

```
%var13=Hm,Hms,Hmsv,hm,hms,hmsv,yMMMd,yMd
```

## Rules
Rules are used to find the coverage for a path. 
The structure is the following.


```
rule := 'path=' chassis \n levelTest* elseLevel

levelTest := (attributesMatch* 'level=' level)* \n
attributesMatch := attribute ('='|'≠') attributeValue (',' attributeValue)* \n
attributeValue := variable | value
attribute := 'attr' attributeNumber
attributeNumber := \d

level := 'core'|'basic'|'moderate'|'modern'|'comprehensive'
elseLevel := 'elseLevel=' level \n
```

[ wfc: The attributeNumbers within an attributesMatch must be unique and in ascending order ]
[ wfc: The levels within a level test must be in non-descending order (eg, moderate..moderate is ok, but not moderate..basic ]

### Example:

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
Notice that levels can occur multiple times with different conditions, as with level=moderate above.

Logically, this is read into a main map from chassis to a submap from attributesMatches to levels,
where any variable is replaced by its value. Thus the above corresponds to:

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

The expression attrN=!… is equivalent to attrN ∉ {…}

To use that information to get a level from a path, 
that path is first converted to a pair <chassis, attributeMap>, where attributeMap maps from attributeNumber to a set of attributeValues
(In that conversion, any variables amon the  are resolved to a set of values: ).

1. The chassis is looked up in the main map to get the submap. 
2. If there is none, the resulting level is `comprehensive`.
3. Then the path's attributeMap is checked against the attributeMatchers, until a match is found.
4. When a match is found, the previous level is returned
4. If there no match, the elseLevel is returned

Notes: 
- While the value associated with an attrN is logically a set,
it could be transformed by an implementation into another format, such as a regex.
- No attributesMatch need have all of the possible attributes from the original path.
For the second attributesMatch in the following example, the attr1 is missing: that means that _any_ attr1 matches.

```
 attr0=gregorian
 attr1=%intervalFormatItem31
  level=moderate
 attr0=generic
  level=moderate
```
