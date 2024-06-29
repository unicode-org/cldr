---
title: Persian
---

# Persian

## Persian style guide and Common issues

### Orthography

Please follow the orthography published by the Persian Academy (دستور خط فارسی). Since the rules are sometimes complicated and hard to decipher, refer to فرهنگ املایی خط فارسی, by Ali-Ashraf Sadeghi and Zahra Zandi-Moghaddam. (We have PDF versions [here](https://drive.google.com/file/d/1R2_7PMMxNzu_rYZvUQgWEGsFBA549Z1K/view?usp=sharing) and [here](https://drive.google.com/file/d/1bDIQ2XWGsahQbg9yZ3DqLKaFuh41RBxx/view?usp=sharing), but we don’t know if these PDFs are the latest editions or not. Refer to the latest printed versions, if you can.)

- Always write the *ezafe* over *he*, if it’s pronounced. For example, use مقدونیهٔ شمالی for North Macedonia.
- For names of continents and their derived forms that could start with either *aa-ye baa-kolaah* (آ) or *alef* (ا), use *alef*: Africa should be افریقا and North America should be امریکای شمالی.

### Characters to use

It may appear that there is a choice among which characters to use for certain Persian letters, but the Unicode Standard and the Iranian National Standard ISIRI 6219, are strict about what to use for different letters or marks:

- For *kaaf*, use U+06A9 ک (and not U+0643 ك).
- For *ye*, use U+06CC ی (and not U+0649 ي or U+064A ى)
- For digits, use U+06F0..U+06F9 ۰۱۲۳۴۵۶۷۸۹ (and not U+0660..U+0669)
- For decimal separator, use U+066B ٫ (and not /)
- For thousands separator, use U+066C ٬ (and not any of ,،`’ etc.)
- For *ezafe* over *he*, use \<U+0647, U+0654> هٔ (and not U+06C0)

Locale patterns: Most of existing CLDR locale data for Persian is based on the [FarsiWeb publication “نیازهای شرایط محلی برای فارسی ایران”](https://drive.google.com/file/d/1yDoUbXnV_q6mrzzaRZK_AvsOLaU-O9Qy/view?usp=sharing), which is in turn based on extensive research in Persian standards and reference material. Follow that document where it covers an issue, and try to remain consistent with it if it doesn’t.

### Language, script, region, and location names

Please do not rely on the Persian Wikipedia for translation of these. You can consult the Persian Wikipedia as a start, but never use it as a primary reference; instead look at its references.

Or find a good Persian reference book about languages and scripts (such as Razi Hirmandi’s translation of Kenneth Katzner’s *The Languages of the World*, published as زبانهای جهان by Markaz-e Nashr-e Daneshgahi, which is the source of the names of most Persian language names in CLDR), or a good atlas, and use names from those instead.

Even better, find multiple references and compare. If there exists a consensus Persian name, it will become clear after consulting multiple references.

Try to use references published before the Persian Wikipedia started in 2003, to minimize potential influences. If in doubt, or can’t find a reference, it may be better to avoid voting for a value instead of using something potentially made up by a Persian Wikipedia editor.

- For names that start with Southern, Western, etc, use the pattern where the compass point comes before the region name. For example, Southern Africa would be جنوب افریقا, while South Africa would be افریقای جنوبی.

### Currencies

The pattern we follow is name of currency, followed by an *ezafe* (written if the Persian name of the currency ends in most vowels), followed by the name of the region. For example, Canadian dollar is دلار کانادا, while Indian rupee is روپیهٔ هند.

### Dates and time

For date formats when a year follows a month, in some calendar systems such as Gregorian and Islamic, the *ezafe* form of month names should be used. For example, while January 12 would be ‏۱۲ ژانویه, January 2019 would be ژانویهٔ ۲۰۱۹. To make this distinction, stand-alone patterns (LLLL etc) are localized without *ezafe*, while formatting patterns (MMMM etc) are localized with *ezafe*. When localizing patterns, pay attention to this distinction and use the correct pattern. For example, “MMMM d, y” should be translated as “d MMMM y” (since the Persian version would need the *ezafe*), while “MMMM d” should be translated as “d LLLL” (since the Persian version doesn’t use the *ezafe*).

### Units

TBD

### Time zones

TBD

### Characters

TBD


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)