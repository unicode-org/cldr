# Gandhari (Phonetic) Keyboard

A QWERTY-based phonetic keyboard layout for typing Gandhari using the Kharoshthi script.

## Overview

| Property | Value |
|----------|-------|
| **Locale** | `pgd-Khar` (Gandhari language, Kharoshthi script) |
| **Layout** | QWERTY Phonetic |
| **Author** | Andrew Glass |
| **Version** | 1.0.2 |
| **Keyboard Indicator** | 𐨒𐨎 (U+10A12 U+10A0E) |
| **CLDR Conformance** | CLDR 47 |
| **Draft Status** | Contributed |
| **Form Factors** | US hardware keyboard |

## About Gandhari and Kharoshthi

Gandhari (also known as Gāndhārī Prakrit) was a Middle Indo-Aryan language used in the region of Gandhara (modern-day northwestern Pakistan and eastern Afghanistan) from approximately the 3rd century BCE to the 4th century CE. The Kharoshthi script was the primary writing system used to record Gandhari texts, particularly Buddhist manuscripts.

The Kharoshthi script (Unicode range U+10A00–U+10A5F) is an ancient Indic script that was used in the northwestern regions of the Indian subcontinent. It is written from right to left, unlike most other Indic scripts.

## Keyboard Layers

This keyboard defines six layers based on modifier key combinations:

| Layer | Modifiers | Description |
|-------|-----------|-------------|
| Base | None | Primary consonants, vowels, numbers, punctuation |
| Shift | Shift | Aspirated consonants, additional punctuation |
| Caps | Caps Lock | Same as Shift layer (aspirates and long vowels) |
| Shift+Caps | Shift + Caps Lock | Inverted caps behavior |
| AltGr | Right Alt | Vowel modifiers, retroflex consonants, special characters |
| Shift+AltGr | Shift + Right Alt | Additional modifiers |

## Character Mapping

### Numbers (Row 1)

Kharoshthi numbers use a additive-multiplicative system. This keyboard implements number composition through transform rules so that input can be done using key sequences based on the decimal place value.

| Keypress | Result | Unicode | Notes |
|----------|--------|---------|-------------|
| 0        |        | N/A     | Invokes transform to decade / hundred / thousand |
| 1 | 𐩀 | U+10A40 | Kharoshthi Digit One |
| 2 | 𐩁 | U+10A41 | Kharoshthi Digit Two |
| 3 | 𐩂 | U+10A42 | Kharoshthi Digit Three |
| 4 | 𐩃 | U+10A43 | Kharoshthi Digit Four |
| 5 | 𐩃𐩀 | U+10A40 U+10A43 | Composite output via transform |
| 6 | 𐩃𐩁 | U+10A41 U+10A43 | Composite output via transform |
| 7 | 𐩃𐩂 | U+10A42 U+10A43 | Composite output via transform |
| 8 | 𐩃𐩃 | U+10A43 U+10A43 | Composite output via transform |
| 9 | 𐩃𐩃𐩀 | U+10A40 U+10A43 U+10A43 | Composite output via transform |
| 10 | 𐩄 | U+10A44 | Kharoshthi Numeral Ten |
| 20 | 𐩄 | U+10A45 | Kharoshthi Numeral Twenty |
| 30 | 𐩅𐩄 | U+10A45 U+10A44 | Composite output via transform |
| 100 | 𐩆 | U+10A46 | Kharoshthi Numeral Hundred |
| 200 | 𐩁𐩆 | U+10A41 U+10A46 | Composite output via transform |
| 201 | 𐩁𐩆𐩀 | U+10A41 U+10A46 U+10A40 | Composite output via transform |
| 1000 | 𐩇 | U+10A47 | Kharoshthi Numeral Thousand |
| 2000 | 𐩁𐩇 | U+10A41 U+10A47 | Composite output via transform |
| 2001 | 𐩁𐩇𐩀 | U+10A41 U+10A47 U+10A40 | Composite output via transform |

### Vowels
Full vowels and dependent vowels are entered using the
same keys. Context determines whether the full or dependent
form is entered. Dependent forms are entered immediately
after a consonant. Full forms are entered otherwise.

| Keypress | Transliteration | Full | Unicode                 | Dependent | Unicode         |
|---------|-----------------|------|-------------------------|-----------|-----------------|
| a       | a               | 𐨀    | U+10A00                 |           |                 |
| A       | ā               | 𐨀𐨌    | U+10A00 U+10A0C         |  𐨌        | U+10A0C         |
| i       | i               | 𐨀𐨁    | U+10A00 U+10A01         |  𐨁        | U+10A01         |
| I       | ī               | 𐨀𐨁𐨌    | U+10A00 U+10A01 U+10A0C |  𐨁𐨌        | U+10A01 U+10A0C |
| u       | u               | 𐨀𐨂    | U+10A00 U+10A02         |  𐨂        | U+10A02         |
| U       | ū               | 𐨀𐨂𐨌    | U+10A00 U+10A02 U+10A0C |  𐨂𐨌        | U+10A02 U+10A0C |
| AltGr+r | r̥               | 𐨀𐨃    | U+10A00 U+10A03         |  𐨃        | U+10A03         |
| AltGr+R | r̥̄               | 𐨀𐨃𐨌    | U+10A00 U+10A03 U+10A0C |  𐨃𐨌        | U+10A03 U+10A0C |
| e       | e               | 𐨀𐨅    | U+10A00 U+10A05         |  𐨅        | U+10A05         |
| E       | ai              | 𐨀𐨅𐨌    | U+10A00 U+10A05 U+10A0C |  𐨅𐨌        | U+10A05 U+10A0C |
| o       | o               | 𐨀𐨆    | U+10A00 U+10A06         |  𐨆        | U+10A06         |
| O       | au              | 𐨀𐨆𐨌    | U+10A00 U+10A06 U+10A0C |  𐨆𐨌        | U+10A06 U+10A0C |

#### Dependent Vowels (AltGr Layer)
Direct entry of dependent vowel forms may be done to bypass
contextual transformation rules.

| Keypress | Transliteration | Result | Unicode |
|----------|---|--------|---------|
| AltGr+a  | &#x25CC; ̄  | 𐨌      | U+10A0C |
| AltGr+i  | i | 𐨁      | U+10A01 |
| AltGr+u  | u | 𐨂      | U+10A02 |
| AltGr+r  | r̥ | 𐨃      | U+10A03 |
| AltGr+e  | e | 𐨅      | U+10A05 |
| AltGr+o  | o | 𐨆      | U+10A06 |

#### Vowel Modifiers

| Keypress    | Transliteration | Result | Unicode | Description |
|-------------|---|--------|---------|-------------|
| M | ṃ | 𐨎 | U+10A0E | Anusvara |
| H | ḥ | 𐨏 | U+10A0F | Visarga |
| AltGr+8     | &#x25CC; ͚  | 𐨍 | U+10A0D | Double ring below |

### Consonants

#### Basic Consonants
Consonants are entered with the inherant vowel invisibly suppressed. Pressing the key |a| provides the inherent vowel. If a consonant is pressed directly after another consontant, a conjuct form may result, unless the combination matches a diagraph form, see below.

| Keypress | Transliteration |Result | Unicode |
|-----|--------|---------|-----------------|
| k | k | 𐨐 | U+10A10 |
| K | kh | 𐨑 | U+10A11 |
| g | g | 𐨒 | U+10A12 |
| G | gh | 𐨓 | U+10A13 |
| c | c | 𐨕 | U+10A15 |
| C | ch | 𐨖 | U+10A16 |
| j | j | 𐨗 | U+10A17 |
| N | ṇ | 𐨞 | U+10A1E |
| t | t | 𐨟 | U+10A1F |
| T | th | 𐨠 | U+10A20 |
| d | d | 𐨡 | U+10A21 |
| D | dh | 𐨢 | U+10A22 |
| n | n | 𐨣 | U+10A23 |
| p | p | 𐨤 | U+10A24 |
| f | ph | 𐨥 | U+10A25 |
| b | b | 𐨦 | U+10A26 |
| B | bh | 𐨧 | U+10A27 |
| m | m | 𐨨 | U+10A28 |
| y | y | 𐨩 | U+10A29 |
| r | r | 𐨪 | U+10A2A |
| l | l | 𐨫 | U+10A2B |
| v | v | 𐨬 | U+10A2C |
| w | ś | 𐨭 | U+10A2D |
| x | ṣ | 𐨮 | U+10A2E |
| s | s | 𐨯 | U+10A2F |
| z | z | 𐨰 | U+10A30 |
| h | h | 𐨱 | U+10A31 |

#### Digraphs
Consonants normally transcribed with digraphs can
be entered by typing the usual diagraph or an approximation.

| Keypresses | Transliteration | Result | Unicode |
|------------|-----------------|--------|---------|
| k h | kh | &#x10A11; | U+10A11 |
| k ' | ḱ | &#x10A32; | U+10A32 |
| g h | gh | &#x10A13; | U+10A13 |
| c h | ch | &#x10A16; | U+10A16 |
| AltGr+t h | ṭh | &#x10A1B; | U+10A1B |
| AltGr+t h h | ṭ́h | &#x10A33; | U+10A33 |
| AltGr+t h' | ṭ́h | &#x10A33; | U+10A33 |
| AltGr+d h | ḍh | &#x10A1D; | U+10A1D |
| t h | th | &#x10A20; | U+10A20 |
| d h | dh | &#x10A22; | U+10A22 |
| p h | ph | &#x10A25; | U+10A25 |
| b h | bh | &#x10A27; | U+10A27 |
| s h | ś | &#x10A2D; | U+10A2D |
| AltGr+t ' | ṭ́ | &#x10A34; | U+10A34 |
| v h | vh | &#x10A35; | U+10A35 |

#### Consonants via AltGr
Consonants normally transcribed with diacritics can
be accessed using AltGr with a related key.

| Keypress | Transliteration | Result | Unicode |
|-----|--------|---------|-----------------|
| AltGr+y | ñ | 𐨙 | U+10A19 |
| AltGr+t | ṭ | 𐨚 | U+10A1A |
| AltGr+T | ṭh | 𐨛 | U+10A1B |
| AltGr+d | ḍ | 𐨜 | U+10A1C |
| AltGr+D | ḍh | 𐨝 | U+10A1D |
| AltGr+n | ṇ | 𐨞 | U+10A1E |
| AltGr+S | ś | 𐨭 | U+10A2D |
| AltGr+k | ḱ | 𐨲 | U+10A32 |

#### Special Consonants

| Key | Transliteration | Result | Unicode |
|-----|--------|---------|-----------------|
| X | kṣ | 𐨐𐨿𐨮 | U+10A10 U+10A3F U+10A2E |
| V | vh | 𐨵 | U+10A35 |

### Consonant Modifiers

| Keypress | Result | Unicode | Description |
|-----|--------|---------|-------------|
| - | 𐨸 | U+10A38 | Hyphen enters bar above a preceding consonant |
| _ | 𐨹 | U+10A39 | Underscore enters cauda below a preceding consonant |
| AltGr+. | 𐨺 | U+10A3A | Dot below |
| AltGr+/ | 𐨿 | U+10A3F | Enter the explicit virama and  render the preceding consonant in subscript form |

### Punctuation

| Keypress | Result | Unicode | Description |
|-----|--------|---------|-------------|
| . | 𐩐 | U+10A50 | Punctuation dot |
| Shift+. (>) | 𐩑 | U+10A51 | Punctuation small circle |
| ; | 𐩒 | U+10A52 | Punctuation circle |
| Shift+; (:) | 𐩓 | U+10A53 | Punctuation crescent bar |
| , | 𐩔 | U+10A54 | Punctuation mangalam |
| Shift+, (<) | 𐩕 | U+10A55 | Punctuation lotus |
| \ | 𐩖 | U+10A56 | Punctuation danda |
| Shift+\ (\|) | 𐩗 | U+10A57 | Punctuation double danda |
| = | 𐩘 | U+10A58 | Punctuation lines |
| ½ (half) | 𐩈 | U+10A48 | Fraction half |
| \| \| | 𐩗 | U+10A57 | Two single dandas transform to a double danda |

## Backspace Behavior

The keyboard implements intelligent backspace handling:

1. **Full vowel deletion**: Deleting a full vowel (base + vowel sign) removes both characters
2. **Vowel sign restoration**: Deleting a vowel sign after a consonant restores the inherent 'a'
3. **Vowel modifier handling**: Deleting a vowel modifier after a consonant restores the consonant state
4. **Conjunct dissolution**: Deleting a virama+consonant sequence removes the entire conjunct portion

## Technical Notes

- **LDML Version**: This keyboard conforms to CLDR Keyboard 3.0 specification (CLDR 47)
- **DTD**: `ldmlKeyboard3.dtd`
- **Form Factor**: Designed for US ANSI 101-key hardware keyboard layout
- **Imports**: Uses standard CLDR punctuation and currency key definitions

## References

- [Unicode Kharoshthi Block](https://www.unicode.org/charts/PDF/U10A00.pdf) (U+10A00–U+10A5F)
- [CLDR Keyboard Specification](https://www.unicode.org/reports/tr35/tr35-keyboards.html) (UTS #35 Part 7)
- [Kharoshthi Script in Unicode](https://www.unicode.org/versions/Unicode15.0.0/ch14.pdf) (Chapter 14, The Unicode Standard)

## License

This keyboard layout is part of the Unicode CLDR project and is subject to the [Unicode License Agreement](https://www.unicode.org/copyright.html).
