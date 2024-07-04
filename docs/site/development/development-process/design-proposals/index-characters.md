---
title: Index Characters
---

# Index Characters

|  |  |
|---|---|
| Author | Mark Davis |
| Date | 2009-06-23 |
| Status | Accepted |
| Bugs | [2224](http://www.unicode.org/cldr/bugs-private/locale-bugs-private/data?id=2224) |

This is a proposal for structure to allow for index characters for UIs, on a per-language basis.

## Goal

Index characters are an ordered list characters for use as a UI "index", that is, a list of clickable characters (or character sequences) that allow the user to see a segment of a larger "target" list. That is, each character corresponds to a bucket in the target list. One may have different kinds of index lists; one that produces an index list that is relatively static, and the other is a list that produces roughly equally-sized buckets. While we are mostly focused on the first, there is provision for supporting the second as well.

The static list would be presented as something like the following (either vertically or horizontally):

… A B C D E F G H CH I J K L M N O P Q R S T U V W X Y Z …

Under "A" you would find all items that are greater than or equal to "A" in collation order, and less than any other item that is greater than "A". The use of the list requires that the target list be sorted according to the locale that is used to create that list. The … items are special, and is a bucket for everything else, either less or greater. Although we say "character" above, the index character could be a sequence, like "CH" above.

In the UI, an index character could also be omitted or grayed out if its bucket is empty. For example, if there is nothing in the bucket for Q, then Q could be omitted. That would be up to the implementation. Additional buckets could be added if other characters are present. For example, we might see something like the following:

| Sample Greek Index | Contents |
|:---:|---|
|  Α Β Γ Δ Ε Ζ Η Θ Ι Κ Λ Μ Ν Ξ Ο Π Ρ Σ Τ Υ Φ Χ Ψ Ω | With only content beginning with Greek letters |
|  … Α Β Γ Δ Ε Ζ Η Θ Ι Κ Λ Μ Ν Ξ Ο Π Ρ Σ Τ Υ Φ Χ Ψ Ω … | With some content before or after |
|  … 9 Α Β Γ Δ Ε Ζ Η Θ Ι Κ Λ Μ Ν Ξ Ο Π Ρ Σ Τ Υ Φ Χ Ψ Ω … | With numbers, and nothing between 9 and Alpha |
|   … 9 A-Z Α Β Γ Δ Ε Ζ Η Θ Ι Κ Λ Μ Ν Ξ Ο Π Ρ Σ Τ Υ Φ Χ Ψ Ω … | With numbers, some Latin |

## Proposal

Because these have to be in collation order, the specification of the list can be an unordered set. So the proposal is to add a new kind of exemplar set to CLDR. Unlike other exemplar sets, the case is significant (we may have to change some of the spec language), since it is the preferred case for the language (although it can be changed, according to the implementation). For example, the above could be represented as:

&emsp;\<characters>

&emsp;&emsp;\<exemplarCharacters **type="index"**>[A-Z {CH}]\</exemplarCharacters>

&emsp;\</characters>

Note that these are *not* simply uppercase versions of the exemplar characters, such as for Greek:

&emsp;\<characters>

&emsp;&emsp;\<exemplarCharacters>[α ά β-ε έ ζ η ή θ ι ί ϊ ΐ κ-ο ό π ρ σ ς τ υ ύ ϋ ΰ φ-ω ώ]\</exemplarCharacters>

&emsp;\</characters>

There is an optional data structure that can be used in some locales. All of these items are optional.

\<indexLabels>

&emsp;\<indexSeparator>\&nbsp;\</indexSeparator>

&emsp;\<compressedIndexSeparator>\&nbsp;•\&nbsp;\</compressedIndexSeparator>

&emsp;\<indexRangePattern>{0}-{1}\</indexRangePattern>

&emsp;\<indexLabelBefore>\&lt;1劃\</indexLabelBefore>

&emsp;\<indexLabelAfter>\&gt;24劃\</indexLabelAfter>

&emsp;\<indexLabel indexSource="一" priority="1">1劃\</indexLabel>

&emsp;\<indexLabel indexSource="二">2劃\</indexLabel>

&emsp;\<indexLabel indexSource="口">3劃\</indexLabel>

...

...

\</indexLabels>

The index Separator can used to separate the index characters if they occur in free flowing text (instead of, say, on buttons or in cells). The default (root) is a space. Where the index is compressed (by omitting values -- see the priority attribute below), the compressedIndexSeparator can be used instead.

The indexRangePattern is used for dynamic configuration. That is, if there are few items in X, Y, and Z, they can be grouped into a single bucket with \<indexRangePattern>{0}-{1}\</separator>, giving "X-Z". The indexLabel and either be applied to a single string from the exemplars, or to the result of an indexRangePattern; so the localizer can turn "X-Z" into "XYZ" if desired.

The indexLabelBefore and After are used before and after a list. The default (root) value is an elipsis, as in the example at the top. When displaying index characters with multiple scripts, the main language can be used for all characters from the main script. For other scripts there are two possibilities:

1. Use the primary characters from the UCA. This has the disadvantage that many very uncommon characters show up.
2. Use the likely-subtags language for each scripts. For example, if the main language is French, and Cyrillic characters are present, then the likely subtags language for Cyrillic is "ru" (derived by looking up "und-Cyrl").

The indexLabel is used to display characters (if it is available). That is, when displaying index characters, if there is an indexLabel, use it instead. For example, for Hungarian, we could have A => "**A, Á**". The priority is used where not all of the index characters can be displayed. In that case, only the higher priorities (lower numbers) would be displayed.

Note that the indexLabels can be used both with contiguous ranges and non-contiguous ranges. For German we might have [A-S Sch Sci St Su T-Z] as the index characters, and the following labels:

 

\<indexLabel item="Sci">S\</indexLabel>

\<indexLabel item="Su">S\</indexLabel>

What that means is that the "S" bucket will include anything [S,Sch), [Sci,St), and [Su,T). That is, items are put into the first display bucket that contains them. That allows for the desired behavior in German (and other languages) of:

- S
	- Satt
	- Semel
	- Szent
- Sch
	- Scherer
	- Schoen
- St
	- Stumpf
	- Sturr

The indexLabel elements would be added by the TC, not localizers, since they are more complex.

### Sorting variants

Note that the choice of exemplars may vary with the sorting sequence used. So there is an extra attribute for use in those languages where a non-standard sorting can be used, and the index characters need to be different.

\<exemplarCharacters **type="index" collation="pinyin"**>...\</exemplarCharacters>

## Automatic Generation

For CLDR 1.8, an initial set of index characters has been automatically generated. Translators can tune as necessary.

The automatic generation takes each of the exemplar characters from CLDR. It then sorts them, putting characters that are the same at a primary level into the same bucket. It then picks one item from each bucket as the representative. Combining sequences are dropped. Korean is handled specially. The large exemplar sets (Japanese, Chinese) need to be done with consultation with translators.

### Representatives

Where multiple character sequences sort the same at a primary level, the automatic generation tries to pick the "best" in the following way:

- Prefer the titlcase versions (A, Dz,...)
- If the NFKD form is shorter, use it.
- If the NFKD form is less (according to the collator but with strength = 3), use it.
- If the binary comparison is less, use it.

*WARNING: the automatic generation would only be a draft, for translators to tune, so any shortcomings could be fixed.*


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)