## <a name="Numbering_Systems" id="Numbering_Systems" href="#Numbering_Systems">Numbering Systems</a>

```dtd
<!ELEMENT numberingSystems ( numberingSystem* ) >
<!ELEMENT numberingSystem EMPTY >
<!ATTLIST numberingSystem id NMTOKEN #REQUIRED >
<!ATTLIST numberingSystem type ( numeric | algorithmic ) #REQUIRED >
<!ATTLIST numberingSystem radix NMTOKEN #IMPLIED >
<!ATTLIST numberingSystem digits CDATA #IMPLIED >
<!ATTLIST numberingSystem rules CDATA #IMPLIED >
```

* Numbering systems information is used to define different representations for numeric values to an end user. Numbering systems are defined in CLDR as one of two different types: algorithmic and numeric. Numeric systems are simply a decimal based system that uses a predefined set of digits to represent numbers. Examples are Western digits (ASCII digits), Thai digits, Devanagari digits. Algorithmic systems are more complex in nature, since the proper formatting and presentation of a numeric quantity is based on some algorithm or set of rules. Examples are Chinese numerals, Hebrew numerals, or Roman numerals. In CLDR, the rules for presentation of numbers in an algorithmic system are defined using the RBNF syntax described in _[Section 6: Rule-Based Number Formatting](#Rule-Based_Number_Formatting)_.


Attributes for the `<numberingSystem>` element are as follows:

- `id` - Specifies the name of the numbering system that can be used to designate its use in formatting.
- `type` - Specifies whether the numbering system is algorithmic or numeric.
- `digits` - For numeric systems, specifies the digits used to represent numbers, in order, starting from zero.
- `rules` - Specifies the RBNF ruleset to be used for formatting numbers from this numbering system. The rules specifier can contain simply a ruleset name, in which case the ruleset is assumed to be found in the rule set grouping "NumberingSystemRules". Alternatively, the specifier can denote a specific locale, ruleset grouping, and ruleset name, separated by slashes.

Examples:

```xml
<!-- ASCII digits - A numeric system -->
<numberingSystem id="latn" type="numeric" digits="0123456789"/>

<!-- A numeric system using Thai digits -->
<numberingSystem id="thai" type="numeric" digits="๐๑๒๓๔๕๖๗๘๙"/>

<!-- An algorithmic system - Georgian numerals, rules found in NumberingSystemRules -->
<numberingSystem id="geor" type="algorithmic" rules="georgian"/>

<!-- An algorithmic system. Traditional Chinese numerals -->
<numberingSystem id="hant" type="algorithmic" rules="zh_Hant/SpelloutRules/spellout-cardinal"/>
```

For general information about the numbering system data, including the BCP47 identifiers, see the main document _Section Q.1.1 [Numbering System Data](tr35.md#Numbering%20System%20Data)._

