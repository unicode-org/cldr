---
title: Unit Names and Patterns
---

# Unit Names and Patterns

## Display Names

These are intended for stand-alone use such as the label of a field where values may be entered, for example Minutes and Seconds in the example below.

Enter desired time to liftoff:

|   |   |   |   |
|---|---|---|---|
| Minutes : |   |   Seconds : |   |

The expectation is that the display names would be in nominative form for languages in which it makes a difference. There may be cases in which a display name could be used in contexts for which nominative would be inappropriate, but the hope is that CLDR clients would arrange the use of display names in user interfaces so that nominative form is not inappropriate.

The unit data is provided in three widths:

- long: typically a fully spelled-out name
- short: a distinctive abbreviated form, not easily confused with another unit
- narrow: as short as possible for situations with limited display space. Typically this form eliminates spaces and punctuation to the extent possible, and uses abbreviations that might only be clear in context.

Note that if in your locale certain abbreviations are always understood to mean particular units, then different abbreviations should generally be used even in the narrow form for other units. For example, if “m” is always understood to mean “meters”, then even in the narrow form a different abbreviation should be used for “minutes”—perhaps “min”, “mn”, or even “m.”.

## Simple Units

Simple units are cases like "{0} mile" or "{0} miles". These have from 1 to 6 [plural forms](https://cldr.unicode.org/translation/getting-started/plurals), depending on the language. Units also have a display name, such as "miles", used as labels in interfaces.

## Compound Units

Some units are compound units, like "miles per hour". When formatting, software will first look for a specialized pattern (with the right plural form), then if that is not available, look for a compound unit and compose a fallback format. Hover over the English column and the Winning column to see examples. The following patterns are available:

|   | Pattern Examples | Formatted Examples | Description |
|---|---|---|---|
| **per** | {0} per {1} | **cm** / **s** , **centimeters** per **second** | Used to compose units consisting of a division of two source units when there is no specialized pattern available. See [perUnitPatterns](https://www.unicode.org/reports/tr35/tr35-general.html#perUnitPatterns). |
| **times** | {0}-{1} | kW⋅h, kilowatt-hour | Used to compose units consisting of a multiplication of two source units when there is no specialized pattern available. See [kilowatt hour](https://en.wikipedia.org/wiki/Kilowatt-hour) . |
| **power** (square, cubic) | square {0} | square **meters** | - Used to compose area or volume measures when there is no specialized pattern available. The width should correspond to the base unit's width, as in [{0} square meter](https://cldr-smoke.unicode.org/cldr-apps/v#/fr/Area/5888c3421f06626c) or [{0} m²](https://cldr-smoke.unicode.org/cldr-apps/v#/fr/Area/6b88ccc5db865200).<br /> - The short form will typically be superscripts {0}² or {0}³; the long form may be the same or may be spelled out (as in English).<br /> - The long form should be spelled out. See Long Power.<br /> - Note that the base unit will be lowercased when there is no spacing around the {0}. Thus {0} Meter will be lowercased when composed with Quadrat{0}. |
| **prefix** (milli-, kilo-,…) | milli{0} | milli **meter** | - Used to compose **metric** units when there is no specialized pattern available.<br /> - The width should correspond to the base unit's width, as in [{0} decimeter](https://cldr-smoke.unicode.org/cldr-apps/v#/fr/Length/ce4591152ece6f2) or [{0} dm](https://cldr-smoke.unicode.org/cldr-apps/v#/fr/Length/1f1b7ef47cfaee78).<br /> - Note that the base unit will be lowercased when there is no spacing around the {0}. Thus {0} Meter will be lowercased when composed with Milli{0}.<br /> - There are two different sets of prefixes:<br />&nbsp;&nbsp; - The standard prefixes are base 10, such as [mega{0}](https://st.unicode.org/cldr-apps/v#/USER/CompoundUnits/2ac0c0f7d5fff965) = ×10⁶ = ×1,000,000.<br />&nbsp;&nbsp; - The  binary  prefixes are base 1024, such as [mebi{0}](https://st.unicode.org/cldr-apps/v#/fr/CompoundUnits/61ea5a846717f829) = ×1024² = ×1,048,576. There are only a few of these, and they are only used with digital units such as byte or bit. See [Mebibyte](https://en.wikipedia.org/wiki/Byte#Multiple-byte_units) for more information. |

Using these patterns, names for complex units can be formed, such as *gigawatt hour per square second*. In common cases, these complex units will have an explicit translation that you supply such as [kilowatt-hour](https://st.unicode.org/cldr-apps/v#/USER/EnergyPower/49796c300ae2d104). But for less common cases, the components will be used to form units, and you need to make sure that they are consistent.

### Common Problems with Compound Units

Make sure that the prefix patterns are consistent with the explicit translations. Here are some common problems to watch for.

| Descriptions | Examples |
|---|---|
| Prefix is the wrong format, such as having a hyphen when the explicit compound doesn't | «γιγα-{0}» produces «{0} γιγα-χερτζ», but the explicit translation is «{0} γιγαχέρτζ» |
| The spacing for the simple unit pattern differs from the spacing for the explicit compound (including non-breaking vs breaking space) | «hecto{0}» produces «{0} hectopascals», but the explicit translation is «{0} hectopascals» |
| The translation of the core unit is different than the translation of the explicit compound | «M{0}» produces «{0} M byte », but the explicit translation is «{0} MB» «n{0}» produces «{0} nsec», but the explicit translation is «{0} ns» «{0}²» produces «{0} μ.²», but the explicit translation is «{0} m²» |
| The translation is incomplete, for example: the "one" form «{0} carré» was translated, but not the plural form, which inherits {0}² from root | «{0}²» produces «{0} mètres²», but the explicit translation is «{0} mètres carrés» |

Important: languages are complicated, and sometimes the composition of complex units cannot match what is grammatically correct for your language. Sometimes there are no fixes that you can make to fix the issue completely. So any indications of problems are warnings, not errors. For example, here is an issue that the translator can't fix:

| Descriptions | Examples |
|---|---|
| The accent needs to shift when a prefix is added | «δεκατο{0}» produces «{0} δεκατολίτρα», but the explicit translation is «{0} δεκατόλιτρα» |
| The internal algorithm for when to combine spacing needs adjustment. | M{0}» produces «{0}M o», but the explicit translation is «{0} Mo» |

Your goal is to make the composition work as well as you can, even it if is not perfect.

### Specialized Pattern

The specialized patterns are needed where there is a special abbreviation, like "mph" instead of "m/hr".

In addition, there are some very common combinations that are translated as a whole, such as "{0} kilometro par hora". In that case, the number is substituted directly for the {0} placeholder.

### Per Unit Pattern

In many languages, the "per Y" part is inflected, and the dividing unit can't be simply substituted for {1}. Because of that, there are "per unit" patterns that are of the form "{0} per hour". For that case, the process is simpler. The first two steps are the same, but then the result is substituted into the "{0} per hour" pattern directly.

### Long Power

If your language is inflected for case or gender: 

- **No inflected alternatives.** If it doesn't list inflected alternatives for square or cubic yet, choose the most neutral form inflection. For many locales, an abbreviated form may work the best, so that there is no visible inflection.
- **Inflected alternatives.** If it does list inflected alternatives, you should look at some of the compound units with "square" and "cubic" that are already translated, to see how to translate power2 and power3. For example, for English, we see
    - Length / Kilometer / long-other => {0} kilometers 
    - Area / Square-Kilometer / long-other => {0} square kilometers
- The pattern for power2 should be constructed so that if you take the word for "kilometers" and substituted it into the pattern, you get "square kilometers". So let's take an example from French:

    1. [https://st.unicode.org/cldr-apps/v#/fr/Length/52e5f7e1046696e8](https://st.unicode.org/cldr-apps/v#/fr/Length/52e5f7e1046696e8) => **{0} kilomètres**
    2. [https://st.unicode.org/cldr-apps/v#/fr/Area/18a3be1b1257d916](https://st.unicode.org/cldr-apps/v#/fr/Area/18a3be1b1257d916) => **{0} kilomètres carrés**
- So the appropriate pattern for power2 would be:

    3. [https://st.unicode.org/cldr-apps/v#/fr/CompoundUnits/15b049cba8052719](https://st.unicode.org/cldr-apps/v#/fr/CompoundUnits/15b049cba8052719) => **{0} carrés**
- If we were to substitute "kilomètres" from the pattern in #1 into the pattern in #3, we would get "kilomètres carrés", which appears in pattern #2. 

### Fallback Format: two units

Some units are formed by combining other units. The most common of this is X per Y, such as "miles per hour". There is a "per" pattern that is used for this. For example, "{0} per {1}" might get replaced by "*10 meters* **per** *second*". 

Given an amount, and two units, the process uses the available patterns to put together a result, as described on [perUnitPatterns](http://www.unicode.org/reports/tr35/tr35-general.html#perUnitPatterns). (e.g. "3 kilograms" + "{0} per second" → "3 kilograms per second")

## Special Units

### Points, dots, and pixels

The measurements for *points, dots,* and *pixels* may be confusing. A *point* is a unit of length, and completely different than *pixel* or *dot*. A pixel and dot are closely related, and are not units of length. This table provides more information.

| Category | Name | ST Link | Description |
|---|---|---|---|
| Units &gt; Length | point | https://st.unicode.org/cldr-apps/v#/USER/Length/45da5a8ddcacb08f   | Display name (long form) for “length-point”, referring to a typographic point , 1⁄72 inch or ≈ 0.353 mm. See [wikipedia](https://en.wikipedia.org/wiki/Point_(typography)) |
| Units &gt; Graphics | dot | https://st.unicode.org/cldr-apps/v#/USER/Graphics/6f52d9bd7df8af32   | Display name (long form) for “graphics-dot”, used in measures of print or screen resolution, such as dots per inch or dots per centimeter. See [wikipedia.org](https://en.wikipedia.org/wiki/Dots_per_inch) |
| Units &gt; Graphics | pixel | https://st.unicode.org/cldr-apps/v#/USER/Graphics/33686b8e75b9afad   | Display name (long form) for “graphics-pixel”, used for counting the individual elements on a screen image. See [wikipedia](https://en.wikipedia.org/wiki/Pixel). |

**💡 Translation Tips**

- You can use the same name for **dot** and **pixel**.
- You **cannot** use the same name for **point** (a unit of length) and **dot**
- You **cannot** use the same name for **point** (a unit of length) and **pixel**.

If the natural word for both "point" and "dot" is the same, such as *punkt*, then there are a few different options to solve the conflict. Italic will be used for native words.

**Changing the name for *point*.** 

1. Use the equivalent of “*punkt length*” in your language for **point**.
2. Use the equivalent of “*typographic punkt*” in your language for **point**.
3. Use the English word “point” (or the transliteration in your script) for **point**.

**Changing the name for *dot*.**

1. Use the equivalent of “*pixel*” in your language for **dot**.

### Year-Person, Month-Person, Week-Person, Day-Person

A few languages have special words for **year, month, week,** or **day** when they are used in context of a person's age. Other languages may simply use the same terms for each one, and do not require separate translation.


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)