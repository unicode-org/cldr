---
title: Locale Format
---

# Locale Format

**Problem:**

Currently, we can get formats like the following:

Chinese (Simplified Han)

Simplified Chinese (Singapore)

Chinese (Simplified Han, Singapore)

English (United States)

American English

English (United States, Variant)

American English (Variant)

But we want to be able to have formats like:

**Chinese (Simplified, Singapore)**

**Chinese (Simplified)**

**English (US), or English (American)**

**English (UK), or English (British)**

**English (US, Variant)**

Here is a proposal for how to do this:

Our current data looks like this (English):

\<localeDisplayPattern>

\<localePattern>{0} ({1})\</localePattern>

\<localeSeparator>, \</localeSeparator>

\</localeDisplayPattern>

1. \<language type="zh\_Hans">Simplified Chinese\</language>
2. \<language type="zh\_Hant">Traditional Chinese\</language>
3. \<language type="en\_US">U.S. English\</language>
4. \<script type="Hans">Simplified Han\</script>
5. \<script type="Hant">Traditional Han\</script>

What happens is that in formatting, the fields that are not present in the type are put into {1} in the localePattern, separated by the localeSeparator (if there is more than one).

We would change it slightly so that we could have patterns like:

1. \<language type="zh\_Hans">Chinese (Simplified{SEP\_LEFT})\</language>
2. \<language type="en\_US">English (US{SEP\_LEFT})\</language>

{SEP\_LEFT} is whatever is left: separated by localeSeparator, and with localeSeparator in front

{LEFT} is whatever is left: separated by localeSeparator, but with no initial localeSeparator

Then we get:

en\_US\_VARIANT => English (US, Variant)

If there is no placeholder in the pattern, it works the old way.

### Issue:

1. Add context="", "standalone", "short", "short standalone"
2. If you have type="en\_US", then it will get one of the following:
	1. "": English (American) *or* English (United States)
	2. "short": English (US)
	3. "standalone": American English
	4. "short standalone": US English
3. We would also add context="short" on Regions, to get "US", and use it if there wasn't a short form of en\_US context="short" or "short standalone"

Fallbacks: 

- short standalone => standalone => ""
- short => ""

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)