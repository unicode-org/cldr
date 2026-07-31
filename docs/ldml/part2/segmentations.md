## <a name="Segmentations" id="Segmentations" href="#Segmentations">Segmentations</a>

```dtd
<!ELEMENT segmentations ( alias | segmentation*) >

<!ELEMENT segmentation ( alias | (variables?, segmentRules? , exceptions?, suppressions?) | special*) >
<!ATTLIST segmentation type NMTOKEN #REQUIRED >

<!ELEMENT variables ( alias | variable*) >

<!ELEMENT variable ( #PCDATA ) >
<!ATTLIST variable id CDATA #REQUIRED >

<!ELEMENT segmentRules ( alias | rule*) >

<!ELEMENT rule ( #PCDATA ) >
<!ATTLIST rule id NMTOKEN #REQUIRED >

<!ELEMENT suppressions ( suppression* ) >

<!ATTLIST suppressions type NMTOKEN "standard" >

<!ATTLIST suppressions draft ( approved | contributed | provisional | unconfirmed ) #IMPLIED >

<!ELEMENT suppression ( #PCDATA ) >
```

* The `segmentations` element provides for segmentation of text into words, lines, or other segments. The structure is based on [[UAX29](https://www.unicode.org/reports/tr41/#UAX29)] notation, but adapted to be machine-readable. It uses a list of variables (representing character classes) and a list of rules. Each must have an `id` attribute.


The rules in _root_ implement the segmentations found in [[UAX29](https://www.unicode.org/reports/tr41/#UAX29)] and
[[UAX14](https://www.unicode.org/reports/tr41/#UAX14)], for grapheme clusters, words, sentences, and lines. They can be
overridden by rules in child locales. In addition, there are several locale keywords that affect segmentation:

* "dx", [Unicode Dictionary Break Exclusion Identifier](tr35.md#UnicodeDictionaryBreakExclusionIdentifier)
* "lb", [Unicode Line Break Style Identifier](tr35.md#UnicodeLineBreakStyleIdentifier)
* "lw", [Unicode Line Break Word Identifier ](tr35.md#UnicodeLineBreakWordIdentifier)
* "ss", [Unicode Sentence Break Suppressions Identifier ](tr35.md#UnicodeSentenceBreakSuppressionsIdentifier)

Here is an example:

```xml
<segmentations>
  <segmentation type="GraphemeClusterBreak">
    <variables>
      <variable id="$CR">\p{Grapheme_Cluster_Break=CR}</variable>
      <variable id="$LF">\p{Grapheme_Cluster_Break=LF}</variable>
      <variable id="$Control">\p{Grapheme_Cluster_Break=Control}</variable>
      <variable id="$Extend">\p{Grapheme_Cluster_Break=Extend}</variable>
      <variable id="$L">\p{Grapheme_Cluster_Break=L}</variable>
      <variable id="$V">\p{Grapheme_Cluster_Break=V}</variable>
      <variable id="$T">\p{Grapheme_Cluster_Break=T}</variable>
      <variable id="$LV">\p{Grapheme_Cluster_Break=LV}</variable>
      <variable id="$LVT">\p{Grapheme_Cluster_Break=LVT}</variable>
    </variables>
    <segmentRules>
      <rule id="3"> $CR × $LF </rule>
      <rule id="4"> ( $Control | $CR | $LF ) ÷ </rule>
      <rule id="5"> ÷ ( $Control | $CR | $LF ) </rule>
      <rule id="6"> $L × ( $L | $V | $LV | $LVT ) </rule>
      <rule id="7"> ( $LV | $V ) × ( $V | $T ) </rule>
      <rule id="8"> ( $LVT | $T) × $T </rule>
      <rule id="9"> × $Extend </rule>
    </segmentRules>
  </segmentation>
...
```

**Variables:** All variable ids must start with a $, and otherwise be valid identifiers according to the Unicode definitions in [[UAX31](https://www.unicode.org/reports/tr41/#UAX31)]. The contents of a variable is a regular expression using variables and [UnicodeSet](tr35.md#Unicode_Sets)s. The ordering of variables is important; they are evaluated in order from first to last (see _[Segmentation Inheritance](#Segmentation_Inheritance)_). It is an error to use a variable before it is defined.

**Rules:** The contents of a rule uses the syntax of [[UAX29](https://www.unicode.org/reports/tr41/#UAX29)]. The rules are evaluated in numeric id order (which may not be the order in which they appear in the file). The first rule that matches determines the status of a boundary position, that is, whether it breaks or not. Thus ÷ means a break is allowed; × means a break is forbidden. It is an error if the rule does not contain exactly one of these characters (except where a rule has no contents at all, or if the rule uses a variable that has not been defined.

There are some implicit rules:

*   The implicit initial rules are always "start-of-text ÷" and "÷ end-of-text"; these are not to be included explicitly.
*   The implicit final rule is always "Any ÷ Any". This is not to be included explicitly.

### <a name="Segmentation_Inheritance" id="Segmentation_Inheritance" href="#Segmentation_Inheritance">Segmentation Inheritance</a>

Variables and rules both inherit from the parent.

**Variables:** The child's variable list is logically appended to the parent's, and evaluated in that order. For example:

```xml
// in parent
<variable id="$AL">[:linebreak=AL:]</variable>
<variable id="$YY">[[:linebreak=XX:]$AL]</variable> // adds $AL

// in child
<variable id="$AL">[$AL && [^a-z]]</variable> // changes $AL, does not affect $YY
<variable id="$ABC">[abc]</variable> // adds new rule
```

**Rules:** The rules are also logically appended to the parent's. Because rules are evaluated in numeric id order, to insert a rule in between others just requires using an intermediate number. For example, to insert a rule after id="10.1" and before id="10.2", just use id="10.15". To delete a rule, use empty contents, such as:

```xml
<rule id="3" /> // deletes rule 3
```

### <a name="Segmentation_Exceptions" id="Segmentation_Exceptions" href="#Segmentation_Exceptions">Segmentation Suppressions</a>

**Note:** As of CLDR 26, the `<suppressions>` data is to be considered a technology preview. Data currently in CLDR was extracted from the Unicode Localization Interoperability project, or ULI. The ULI committee has been disbanded, but historical information can be found at <https://www.unicode.org/uli/>.

The segmentation **suppressions** list provides a set of cases which, though otherwise identified as a segment by rules, should be skipped (suppressed) during segmentation.

* For example, in the English phrase "Mr. Smith", CLDR segmentation rules would normally find a Sentence Break between "Mr" and "Smith". However, typically, "Mr." is just an abbreviation for "Mister", and not actually the end of a sentence.


Each suppression has a separate `<suppression>` element, whose contents are the break to be skipped.

Example:

```xml
<segmentation type="SentenceBreak">
    <suppressions type="standard" draft="provisional">
        <suppression>Maj.</suppression>
        <suppression>Mr.</suppression>
        <suppression>Lt.Cdr.</suppression>
        . . .
    </suppressions>
</segmentation>
```

**Note:** These elements were called `<exceptions>` and `<exception>` prior to CLDR 26, but those names are now deprecated.

