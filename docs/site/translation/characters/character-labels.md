---
title: Character Labels
---
# Character Labels

CLDR has different types of character labels.

- **Category Labels** are used for broad categories of characters, such as “**Punctuation**”, or “**Smileys & People**”. The main usage is in “character/symbol pickers”.
- **Category Patterns** are used to compose labels. For example, the pattern “{0} — Historic” can be composed with “**Punctuation**” to produce “**Punctuation — Historic**”
- **Annotations** are used for more specific features of characters, such as “cactus”. Annotations do not need to be unique. They can be used in predictive typing, such as when typing “p i z” shows🍕 in a suggestion box.
- **TTS Labels** are used for Text-to-Speech support, where a character is read aloud. They are typically a shortened and sometimes reworded version of the formal Unicode name. They may be combined with a Category Label for disambiguation. The names may not be unique, although they should be unique within a category.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)