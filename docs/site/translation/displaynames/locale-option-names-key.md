---
title: 'Locale Option Names (Key)'
---

# Locale Option Names (Key)

Locales can have special variants, to indicate the use of particular calendars, or other features. They be used to select among different options in menus, and also display which options are in effect for the user.

## Locale Option Names

Here are examples of names of Options to be translated.

| Option | Meaning   |
|---|---|
| Calendar | Calendar system (the European calendar is called "Gregorian"; others are the Chinese Lunar Calendar, and so on.) |
| Collation | How text is sorted (where a language has different possible ways to sort). |
| Currency | The default currency. (The value is any currency value, such as USD). |
| Numbers | The numbering system in use, such as European (0,1,2), Arabic (٠, ١, ٢ ), Devanagari ( ०,  १,  २ ). <br /> - Usually these are just derived from the name of the script.<br /> - There are some special forms, such as " Simplified Chinese Financial Numerals " or " Full Width Digits ". |
| Private Use | Used for Private-Use options (x). |

## Locale Option Value Names

There are two kinds of option value names: the Long name and the Core name. The Long name includes the name of the Option while the Core name does not. Here are examples of what that can look like:

| Code | Name |
| -- | -- |
| `calendar` | Kalender |
| `calendar-buddhist` | Buddhistischer Kalender |
| `calendar-buddhist-core` | Buddhistischer |

The core name is used in two ways: 
- In locale names with Locale Option Pattern, like "English (Calendar: Buddhist)"
- In menu listings or pull-downs, where the Option code is used as the header, such as:
    - **Calendar**
        - Buddhist
        - Chinese
        - Coptic
        - …
