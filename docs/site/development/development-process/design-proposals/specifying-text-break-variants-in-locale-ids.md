---
title: Specifying text break variants in locale IDs
---

# Specifying text break variants in locale IDs

|   |   |
|---|---|
| Author | Peter Edberg |
| Date | 2014-11-11, last update 2016-10-20 |
| Status | Proposal |
| Feedback to | pedberg (at) apple (dot) com |
| Bugs | See list below |

This proposal discusses options for extending Unicode locale identifiers to specify text break variants with a locale. It was prompted by CLDR and ICU bugs including the following, as well as by other requests:

- CLDR #[2142](http://unicode.org/cldr/trac/ticket/2142), Alternate Grapheme Clusters
- CLDR #[2161](http://unicode.org/cldr/trac/ticket/2161), Grapheme break iterator with legacy behavior
- CLDR #[2825](http://unicode.org/cldr/trac/ticket/2825), Add aksha grapheme break
- CLDR #[2975](http://unicode.org/cldr/trac/ticket/2975), Support legacy grapheme break
- CLDR #[4931](http://unicode.org/cldr/trac/ticket/4931), Provide mechanism for parameterizing linebreak, etc.
- CLDR #[7032](http://unicode.org/cldr/trac/ticket/7032), BCP47 for break exceptions
- CLDR #[8204](http://unicode.org/cldr/trac/ticket/8204), Other line break parameterization to support CSS word-break, etc.
- ICU #[9379](http://bugs.icu-project.org/trac/ticket/9379), Request to add Japanese linebreak tailoring selectable as variations
- ICU #[11248](http://bugs.icu-project.org/trac/ticket/11248), Improve C/J FilteredBreakIterator, move to draft
- ICU #[11530](http://bugs.icu-project.org/trac/ticket/11530), More efficient representation for multiple line break rule sets
- ICU #[11531](http://bugs.icu-project.org/trac/ticket/11531), Update RBBI TestMonkey to test line break variants
- ICU #[11770](http://bugs.icu-project.org/trac/ticket/11770), BreakIterator should support new locale key "ss"
- ICU #[11771](http://bugs.icu-project.org/trac/ticket/11771), FilteredBreakIterator should move from i18n to common

## I. Options needed (as known so far)

### A. Grapheme cluster break

Need to choose one of the following (current CLDR/ICU implementation uses extended grapheme clusters):

- Legacy UAX #29 grapheme clusters (also called spacing units).
- Extended UAX #29 grapheme clusters: legacy clusters plus also include spacing combining marks in Indic scripts, and Thai SARA AM and Lao AM (but not other spacing vowels in SE Asian scripts).
- Aksaras (Indic & SE Asian consonant/vowel clusters or syllables): extended clusters plus also include consonant-virama sequences, and spacing vowels in SE Asian scripts.

### B. Word break

Currently uses dictionary-based break for sequences in CJK scripts (Han/Kana/Hangul) or SE Asian scripts (LineBreak property value SA/Complex\_Context: Thai, Lao, Khmer, Myanmar, etc.); we need a locale keyword that can turn this on or off (i.e. off to use basic UAX #29 word break), at least for CJK.

### C. Sentence break

We need a locale keyword to control use of ULI suppressions data (i.e. to determine whether we should wrap the UAX29-based break iterator in a FilteredBreakIterator instance for the locale, and to determine which suppressions set to use).

### D. Line break (highest priority)

Currently ICU uses dictionary-based break for text in SE Asian scripts only. The two most important needs for line break control are:

- For Japanese text, control whether line breaks are allowed before small kana and before the prolonged sound mark 30FC; this corresponds to (most of) the distinction between CSS level 3 strict and normal line break (see below), and is implemented by treating LineBreak property value CJ as either NS (strict) or ID (normal).
- For Korean text, control whether the line break style is E. Asian style (breaks can occur in the middle of words) or “Western” style (breaks are space based), as described in UAX 14. 

Other desirable capabilities include:

- In a CJK-language context, control over whether breaks are allowed in the middle of words in alphabetic scripts that normally use a space-based approach (e.g. Latin, Greek, Cyrillic). Currently fullwidth Latin letters have LineBreak property value ID and do allow such breaks, but normal Latin letters are AL and do not.
- In a CJK-language context, explicit control over whether characters with LineBreak property value AI resolve to ID or AL (UAX 14 recommends using resolved East Asian Width to do this, but in the absence of that or any other higher-level mechanism they default to AL). This is somewhat related to the previous bullet. Note that characters with value AI include some symbols, punctuation, superscript digits, modifier letters, etc.
- Full control over CSS line break styles, see below (these can be used to control most of the above line break features)

## II. Notes on CSS level 3 line break

(from draft of Jun 2015, [http://dev.w3.org/csswg/css-text/#line-breaking](http://dev.w3.org/csswg/css-text/#line-breaking))

CSS has two independent properties for controlling line break behavior:

### A. The line-break property

This is mainly about break behavior for punctuation and symbols, though it does affect small kana. The rules are intended to specify behavior that may be language-specific, but explicit rules are provided for CJK. Besides the “auto” value, there are three specific values for this property.

- **strict:** The most restrictive rules, for longer lines and/or ragged margins. Prevents break before small kana and before prolonged sound mark 30FC (this is the set of characters with LineBreak property value CJ, which have general category Lo or Lm).
- **normal:** Allows break before small kana and before prolonged sound mark 30FC. If the content language is Chinese or Japanese, also allows breaks before hyphen like characters: ‐ U+2010, – U+2013, ～ U+301C, ゠ U+30A0 (LineBreak property value BA for the first two, NS for the second two; general category Pd for all four).
- **loose:** The least restrictive, used for short lines as in newspapers. In addition to breaks allowed for normal, allows breaks before iteration marks (々 U+3005, 〻 U+303B, ゝ U+309D, ゞ U+309E, ヽ U+30FD, ヾ U+30FE, all with LineBreak property value NS and general category Lm) and breaks between characters with LineBreak property value IN (inseparable). If the content language is Chinese or Japanese, also allows breaks before certain centered punctuation marks, before suffixes and after prefixes.

### B. The word-break property

This only controls break opportunities between letter-like characters (including ideographs), and has 3 possible values. Symbols that break in the same way as letters are affected in the same way by these options.

- **normal:** Words break according to their customary rules. For Korean this specifies E. Asian style break behavior.
- **break-all:** Allow breaks within words (between any two “typographic letter units” of general category L or N) unless forbidden by a line-break setting. This is mainly intended for a primarily-CJK context to allow breaks in the middle of normal Latin, Cyrillic, and Greek words.
- **keep-all:** Prohibit breaks between letters regardless of line-break options, except where opportunities exists due to dictionary-based break. For Korean this option specifies “western”-style line break. This is also useful when short CJK snippets are included in text that is primarily in a language using space-based breaking.

## III. Proposed -u extension keys

### A. For control of grapheme cluster break

For gb, current default is extended.

```
<key name="gb" description="Grapheme cluster break type key">

	<type name="legacy" description=“Grapheme break using UAX #29 legacy grapheme clusters"/>

	<type name="extended" description="Grapheme break using UAX #29 extended grapheme clusters"/>

	<type name="aksara" description="Grapheme break adding aksaras to extended grapheme clusters”/>
```

### B. For control of word break

(Type key not needed yet and values undetermined, just reserve it)

```<key name="wb" description="Word break type key">```

Will also need a word break parameter key to control whether dictionary-based work break is used, probably need separate control for at least for CJ, Korean, and SEAsian scripts; no key proposed yet.

### C. For control of sentence break

(Type key not needed yet and values undetermined, just reserve it)

```<key name="sb" description="Sentence break type key">```

For ss, current default is none.

```
<key name=“ss” description=“Sentence break parameter key to control use of suppressions data”>

	<type name=“none” description="Don’t use segmentation suppressions data"/>

	<type name=“standard” description="Use segmentation suppressions data of type standard"/>
```

### D. For control of line break

The current proposal is to use the *type* to specify the CSS line-break property; this can be used in older implementations as e.g. “@lb=strict”. One or more additional parameter keywords are provided to permit control of the CSS word-break property and to permit control of whether AI is treated as AL or ID.

D1. Supporting CSS line-break

For lb, the current default is normal for the "ja" locale, but strict for others; it should probably be normal for all since the distinction is mainly relevant for Japanese), and the discussion below assumes that change.

```
<key name="lb" description="Line break type key">

	<type name="strict" description="CSS lev 3 line-break=strict, e.g. treat CJ as NS"/>

	<type name="normal" description="CSS lev 3 line-break=normal, e.g. treat CJ as ID, break before hyphens for ja,zh"/>

	<type name="loose" description="CSS lev 3 line-break=loose"/>
```

D2. Supporting other controls including CSS word-break (for line break), first idea (2014-11)

For the other controls, including support of the CSS word-break property, I think it is best to have separate control over how certain sets of characters are treated:

- Treat Hangul (characters with LineBreak property value H2, H3, JL, JV, JT) per UAX 14 (default, for E. Asian break style), or as AL (for space-based break style, part of CSS word-break=keep-all).
- Treat characters with LineBreak property value ID per UAX #14 (default, for E. Asian break style) or as AL (for space-based break style, part of CSS word-break=keep-all). Is this correct or is the real goal just to eliminate breaks between ID?
- Treat alphabetic and numeric characters (General Category L and N) per UAX #14 (default), or as ID (to get behavior like CSS word-break=break-all).
- Treat characters with LineBreak property value AI as AL (default per UAX #14) or as ID.

```
<key name="lc” description="Line break class remapping“>

	<type name=“LB_CLASS_MAP_CODE” description=“One or more linebreak class mapping codes, see xxx”/>
```

where LB\_CLASS\_MAP\_CODE is a sequence of one or more of the following codes (separated by - or \_):

- hang2al (treat Hangul as AL)
- id2al (treat ID as AL)
- alnum2id (treat normal alphabetic/numeric as ID)
- ai2id (treat AI as ID)

Then for example CSS lev 3 word-break=keep-all could be indicated as “-u-lc-hang2al-id2al”.

D3. Supporting CSS word-break (for line break), second idea (2015-07)

I now think explicit remapping of certain classes is the wrong approach for supporting the CSS word-break options for line break control:

- These options are not defined in terms of UAX #14 LineBreak property values, but rather in terms of general categories L and N.
- The specific definition of the CSS word-break options (and line-break options) may change somewhat over time; we need locale tags that map to the current CSS definition.
- We may *also* want other kinds of line-break controls whose behavior does *not* change, and whose behavior *may* be defined in terms of LineBreak property values (as with the proposal in section D2 above), but that is a separate consideration.

Thus I propose the following .

```
<key name="lw" description="Line break key for CSS lev 3 word-break options">

	<type name="normal" description="CSS lev 3 word-break=normal"/>

	<type name="breakall" description="CSS lev 3 word-break=break-all, allow breaks in words unless forbidden by lb setting"/>

	<type name="keepall" description="CSS lev 3 word-break=keep-all, prohibit breaks in words except for dictionary breaks"/>
```

### E. Other ideas

For linebreak control:

- CLDR #4391 proposed using “-u-lb-strictja” to specify CSS line-break=strict.
	- Mark Davis suggested that the -lb- keyword could take multiple values including all of those proposed for the separate -lc- keyword, thus eliminating the need for the -lc- keyword; for example, “-u-lb-strict-hang2al-id2al”.

Overall: Another suggestion goes further than the second bullt above: Have just a single keyword to specify all break variants; it would be followed by a list of attributes that would all share a single namespace, and whose names would need to identify which type of break they affected. Examples might include gblegacy, gbextend, gbaksara (use one to specify grapheme break); ssnone, ssstd (use one to specify sentence break suppressions); etc. While this consumes less of the -u keyword namespace, it is less flexible at mapping to values specified in resource attributes, such as different types of sentence break suppression data, unless significant restrictions are placed on those attribute values.

### F. Current status

F1. keyword -lb-

In the CLDR meeting of 2014-Nov-19, it was agreed to add the -lb- keyword with at least the values "strict", "normal" and "loose" for support of the corresponding CSS level 3 behavior; for legacy-style Unicode locale IDs using '@', "lb=" should be used. The implementation details are not yet determined or specified, nor are the details of any locale-specific override behavior.

Current (2015-02-18) work under CLDR #[4931](http://unicode.org/cldr/trac/ticket/4931) and ICU #[9379](http://bugs.icu-project.org/trac/ticket/9379) includes the following, approved in CLDR and ICU meetings:

1. Add new CLDR file common/bcp47/segmentation.xml (name OK?) with the following:

```
<key name="lb" description="Line break type key" since="27">

	<type name="strict" description="CSS level 3 line-break=strict, e.g. treat CJ as NS"/>

	<type name="normal" description="CSS level 3 line-break=normal, e.g. treat CJ as ID, break before hyphens for ja,zh"/>

	<type name="loose" description="CSS lev 3 line-break=loose"/>

</key>
```

2. In CLDR file common/dtd/ldmlICU.dtd, add "alt" as an attribute for the \<icu:line ...> element (and allow multiple \<icu:line ...> elements).
3. In ICU icu/trunk/source/data/xml/brkitr/ files such as root.xml, fi.xml, and ja.xml, add lines mapping the line break types to corresponding rule files, e.g. in root:

```
<icu:line alt="loose" icu:dependency="line_loose.brk"/>

<icu:line alt="normal" icu:dependency="line_normal.brk"/>

<icu:line alt="strict" icu:dependency="line.brk"/>
```

(Note that we need to add brkitr locales for zh and zh\_Hant since they have non-standard CSS line break types, like ja)

4. In CLDR, update tools/java/org/unicode/cldr/icu/BreakIteratorMapper.java to handle the alts (3 added lines).
5. In ICU, add 6 new line break rule files in source/data/brkitr/ (and delete line\_ja.txt):

```
line_loose.txt

line_loose_cj.txt

line_loose_fi.txt

line_normal.txt

line_normal_cj.txt

line_normal_fi.txt
```

These result in an increase of about 630K bytes (2.5%) in the data file. These can be tailored out in cases for which it is a problem, either by deleting lines from the ICUdata/xml/brkitr/ files if building from CLDR data, or by deleting corresponding lines in the data/brkitr/\<locale>.txt files and deleting the unused files from BRK\_SOURCE in data/brkitr/brkfiles.mk. #[11530](http://bugs.icu-project.org/trac/ticket/11530) is to investigate a more efficient way of representing the line break rule variants.

Note that the CLDR representation of the line break rules have not yet been updated to match (they are currently ignored when generating ICU data).

6. In ICU4C, update BreakIterator::makeInstance to map locale to the correct ruleset (about 10 lines, not yet committed)... similar change in ICU4J.
7. Update testate/rbbitst.txt to test the variants. More extensive monkey tests for the variants are covered by #[11531](http://bugs.icu-project.org/trac/ticket/11531).

F2. keywords -ss-, -lw-

Proposal for CLDR & ICU meetings 2015-Jul-08:

1. In CLDR file common/bcp47/segmentation.xml add the following (approved in CLDR meeting 2015-Jul-08):

```
<key name="lw" description="Line break key for CSS lev 3 word-break options" since="28">

	<type name="normal" description="CSS lev 3 word-break=normal"/>

	<type name="breakall" description="CSS lev 3 word-break=break-all, allow breaks in words unless forbidden by lb setting"/>

	<type name="keepall" description="CSS lev 3 word-break=keep-all, prohibit breaks in words except for dictionary breaks"/>

</key>
```

Default value is "normal". English names for the values are:

• normal: "Normal line breaks for words"

• breakall: "Allow line breaks in all words"

• keepall: "Prevent line breaks in all words"

```
<key name="ss" description="Sentence break parameter key to control use of suppressions data" since="28">

<type name="none" description="Don’t use segmentation suppressions data"/>

<type name="standard" description="Use segmentation suppressions data of type standard"/>

</key>
```

Current default value is "none". In the future we hope to make the default "standard". English names for the values are:

- normal: "Normal sentence breaks per Unicode specification"
- standard: "Prevent sentence breaks after standard abbreviations"

2. In ICU BreakIterator, initial support will be incomplete (details for ICU4C below, similar approach in ICU4J):

a) In ICU4C BreakIterator::makeInstance, for kind = UBRK\_SENTENCE, if locale has key "ss" with value "standard", then call FilteredBreakIteratorBuilder on the result of BreakIterator::buildInstance to produce a new BreakIterator\* which supports the sentence break exceptions. Notes:

- Currently FilteredBreakIteratorBuilder does not have a way to support different segmentation suppression sets, it only supports the "standard" set.
- A BreakIterator produced in this way currently supports the next() method but not the other BreakIterator methods for moving through text (see [class details](http://icu-project.org/apiref/icu4c/classicu_1_1FilteredBreakIteratorBuilder.html#details)). This should be fixed fairly soon.

b) In ICU4C RuleBasedBreakIterator::handleNext and handlePrevious, for now we can implement an approximation of support for the key "lw" values by alteriing the character classes as follows (similar to the behavior in section D2 above):

- For "keepall", if the class is Hangul (H2, H3, JL, JV, JT) or ID, remap to AL
- For "breakall", if the class is AL, HL, AI, or NU, remap to ID.

More complete support is dependent on a mechanism for turning on and off certain rules, see ICU #[11530](http://bugs.icu-project.org/trac/ticket/11530).

## IV. Implementation notes

What I had in mind was that the break type selection (gb, lb) would be implemented by selection of different break table resources, while the parameter keywords (ss, lc) would be implemented in code (changing line break classes, perhaps with an annotation in the tables along the lines suggested in http://unicode.org/cldr/trac/ticket/4931). However, it is not clear how to implement selection of different tables given the current resource structure in ICU (which does not exactly mirror the CLDR structure).

### A. CLDR XML structure

Currently in CLDR we can have a structure locale-specific break iterator data icu/trunk/source/data/xml/brkitr/xx.xml as follows; except for the suppressions data, this is otherwise ignored for building ICU data (segmentation type is GraphemeClusterBreak, WordBreak, LineBreak, SentenceBreak):

```
<ldml>

	<segmentations>

		<segmentation type="LineBreak">

			<variables>

				….

			</variables>

			<segmentRules>

				….

			</segmentRules>

		<segmentation type="SentenceBreak">

			<suppressions type="standard">

				…

			</suppressions
```

We could an attribute "alt" for \<segmentation> to specify the specific variant (corresponds to the value for the -gb or -lb keyword, for example), though this would currently be ignored for LDML to ICU conversion:

```<segmentation type="LineBreak" alt="strict">```

Handling of default values and elements without "alt" is discussed in section E below.

### B. ICU XML source structure

In ICU we have XML source data and generated txt data. The XML source structure is specified by

http://www.unicode.org/repos/cldr/trunk/common/dtd/ldmlICU.dtd

and currently looks like this for root (any locale-specific data uses a subset of this):

```
<special xmlns:icu="http://www.icu-project.org/">

	<icu:breakIteratorData>

		<icu:boundaries>

			<icu:grapheme icu:dependency="char.brk"/>

			<icu:word icu:dependency="word.brk"/>

			<icu:line icu:dependency="line.brk"/> or e.g. "line_xx.brk" in locale-specific data

			…

		</icu:boundaries>

		<icu:dictionaries>

			<icu:dictionary type="Hani" icu:dependency="cjdict.dict"/>

			<icu:dictionary type="Hira" icu:dependency="cjdict.dict"/>

			…

			<icu:dictionary type="Thai" icu:dependency="thaidict.dict"/>

		</icu:dictionaries>

	</icu:breakIteratorData>

</special>
```

Note that the following attributes for the boundaries subelements (icu:word etc.) are defined in CLDR’s ICU DTD but currently unused:

```icu:class NMTOKEN #IMPLIED```

```icu:append NMTOKEN #IMPLIED```

```icu:import NMTOKEN #IMPLIED```

We could define an additional attribute "alt" and then use that to match the CLDR \<segmentations> alt attribute:

```
<icu:boundaries>

	<icu:grapheme icu:dependency="char.brk"/>

	<icu:grapheme alt="extended" icu:dependency="char.brk"/>

	<icu:grapheme alt="legacy" icu:dependency="char_legacy.brk"/>

	…

	<icu:line icu:dependency="line.brk"/>

	<icu:line alt="normal" icu:dependency="line.brk"/>

	<icu:line alt="strict" icu:dependency="line_strict.brk"/>

	…

</icu:boundaries>
```

### C. ICU txt resource structure

The ICU xml files (and the CLDR xml files, for suppressions data) are processed by CLDR tools such as cldr/trunk/tools/java/org/unicode/cldr/icu/BreakIteratorMapper.java to generate the text resources, for example:

```
root{

	boundaries{

		grapheme:process(dependency){"char.brk"}

		line:process(dependency){"line.brk"}

		…

		word:process(dependency){"word.brk"}

	}

	dictionaries{

		Hani:process(dependency){"cjdict.dict"}

		Hira:process(dependency){"cjdict.dict"}

		...

		Thai:process(dependency){"thaidict.dict"}

	}

}

xx{

	boundaries{

		line:process(dependency){"line_xx.brk"}

	}

	exceptions{

		SentenceBreak:array{

			"Mr.",

			"Etc.",

			…

		}

	}

}
```

These files are read by BreakIterator::buildInstance(...) in ICU4C, with a type parameter that maps directly to the key in the boundaries resource: "grapheme", "line", etc. Currently there is not a way to add attributes for the boundaries subelements such as line or word. However, we could map the icu:alt values proposed in section C to resource keys with extensions where appropriate:

```
boundaries{

	grapheme:process(dependency){"char.brk"}

	grapheme_extended:process(dependency){"char.brk"}

	grapheme_legacy:process(dependency){"char_legacy.brk"}

	…

	line:process(dependency){"line.brk"}

	line_normal:process(dependency){"line.brk"}

	line_strict:process(dependency){"line_strict.brk"}

	…

}
```

BreakIterator::buildInstance is called by BreakIterator::makeInstance, which provides the type keys "grapheme", "line", etc. It could use the locale to construct the resource keys with extensions. 

### D. Current dictionary break implementation

(See also the [relevant section of the ICU User Guide](http://userguide.icu-project.org/boundaryanalysis#TOC-Details-about-Dictionary-Based-Break-Iteration))

The use of dictionary break depends on the existence in the rules of a variable "$dictionary" which defines the UnicodeSet of characters for which dictionary break should be used.

For line break, this is defined as “```$dictionary = [:LineBreak = Complex_Context:];```” where the Line\_Break property value Complex\_Context is equivalent to SA and applies to most letters, marks, and some other signs in Southeast Asian scripts: Thai, Lao, Myanmar, Khmer, Tai Le, New Tai Lue, Tai Tham, Tai Viet, etc. For word break, in addition to characters with Line\_Break property value SA, the $dictionary set includes characters with script Han, Hiragana, Katakana, as well as composed Hangul syllables in the range \uAC00-\uD7A3 (not sure why the latter are included, since we do not have dictionary support for them).

In both cases, the rules are defined to disallow breaks between characters in the $dictionary set. When determine the next or previous break, the iterator first determines the break using the normal rules (which will not break between characters in the $dictionary set); in the process it marks which characters are handled by a dictionary break engine (For each script that has a break dictionary, the associated break engine defines a more specific set of characters to which it applies). If characters handled by a dictionary break engine were encountered, the break iterator then invokes the dictionary break engines to determine breaks within the $dictionary-set span.

### E. Multiple rule sets that depend on break type

It would be nice for a given locale to be able to specify, for each break type, which variant is the default for that locale. In root this can just be done by using the resource key without any extension. In other locales, we could do something like this in the CLDR XML:

```
<segmentations>

	<default type="LineBreak" alt="strict">

	<default type="GraphemeClusterBreak" alt="legacy">
```

## V. Acknowledgments

Thanks to Koji Ishii and the CLDR team for feedback on this document.


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)