---
title: Transform Fallback
---

# Transform Fallback

We need to more clearly describe the presumed lookup fallback for transforms:

## Code equivalence

- A lone script code or long script name is equivalent to the BCP 47 syntax: Latn = Latin = und-Latn.
- "und" from BCP 47 is treated the same as the special code "any" in transform IDs
- In the unlikely event that we have a collision between a special transform code (any, hex, fullwidth, etc) and a BCP 47 language code, we have to figure out what to do. Initial suggestion: add "\_ZZ" to language code.
- For the special codes, we should probably switch to aliases that have a low probability of collision, eg > 3 letters always.

## Language tag fallback

If the source or target is a Unicode language ID, then a fallback is followed, with some additions.

1. az\_Arab\_IR
2. az\_Arab
3. az\_IR
4. az
5. Arab
6. Cyrl

The fallback additions are:

- We fallback also through the country (03). This is along the lines we've otherwise discussed for BCP47 support, and that we should clarify in the spec.
- Once the language is reached, we fall back to script; first the specified script if there is one (05), then the likely script for lang (06 - if different than 05)

## Laddered fallback

The source, target, and varient use "laddered" fallback. That is, in pseudo code:

a. for variant in variant-chain

b. for target in target-chain

c. for source in source-chain

&emsp;transform = lookup source-target/variant

&emsp;if transform != null return transform

..

For example, here is the chain for ru\_RU-el\_GR/BGN. I'm spacing out the source, target, and variant for clarity.

1. ru\_RU - el\_GR /BGN
2. ru - el\_GR /BGN
3. Cyrl - el\_GR /BGN
4. ru\_RU - el /BGN
5. ru - el /BGN
6. Cyrl - el /BGN
7. ru\_RU - Grek /BGN
8. ru - Grek /BGN
9. Cyrl - Grek /BGN
10. ru\_RU - el\_GR
11. ru - el\_GR
12. Cyrl - el\_GR
13. ru\_RU - el
14. ru - el
15. Cyrl - el
16. ru\_RU - Grek
17. ru - Grek
18. Cyrl - Grek

**Comments:**

1. The above is not how ICU code works. That code actually discards the variant if the exact match is not found, so lines 02-09 are not queried at all. I think that is definitely a mistake.
2. Personally, I think the above chain might not be optimal; that it would be better to have BGN be stronger than country difference, but not as strong as Script. However, in conversations with Markus, I was convinced that a simple story for how it works is probably the best, and the above is simpler to explain and easier to implement.

## Model Requirements

We have the implicit requirement that no variant is populated unless there is a no-variant version. We need to make sure that that is maintained by the build tools and/or tests. That is, if we have fa-Latn/BGN, we should have fa-Latn as well. The other piece of this is that we should name all the no-variant versions, so that people can be explicit about the variant even in case we change the default later on. The upshot is that the no-variant version should always just be aliases to one of the variant versions. Operationally, that means the following actions:

Case 1. only fa-Latn/BGN. Add an alias from fa-Latn to fa-Latn/BGN

Case 2. only foo-Latn. Rename to foo-Latn/SOMETHING, and then do Case 1. 

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)