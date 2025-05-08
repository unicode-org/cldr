---
title: Update emoji translations & ordering
---

SBRS (at the start of the release):
-----

## Do in UnicodeTools

1.  Run unicodetools GenerateEmoji with the latest or dev version number. The 14.0 is used in some examples below.

1.  If you get an error like

    * Exception in thread "main" java.lang.IllegalArgumentException: no name for 🫱‍🫲 1FAF1 200D 1FAF2
    * at org.unicode.tools.emoji.EmojiData.\_getName(EmojiData.java:1230)
    * at org.unicode.tools.emoji.EmojiData.getName(EmojiData.java:1194)
    * at org.unicode.tools.emoji.EmojiDataSourceCombined.getName(EmojiDataSourceCombined.java:156)
    * at org.unicode.tools.emoji.GenerateEmoji.showCandidateStyle(GenerateEmoji.java:3600)
    * at org.unicode.tools.emoji.GenerateEmoji.main(GenerateEmoji.java:641)

2.  Then change the name composition algorithm if necessary (for new emoji zwj sequences)

    1.  It may have also been modified during the emoji development. Typically the code that needs changing will be in Annotations.synthesize, to capture yet another special skintone instance
    2.  ~~Ensure that the documentation of composition of names (for new components like hair styles) in LDML is updated to match what is in org.unicode.cldr.util.Annotations.~~
    3.  Make sure that org.unicode.tools.emoji.unittest.TestAll runs successfully, with -Demoji-beta.

~~3.  Copy~~
    * ~~/emoji/docs/Public/emoji/14.0/emoji-test.txt~~
	~~to~~
    * ~~/cldr-code/src/main/resources/org/unicode/cldr/util/data/emoji/emoji-test.txt~~

4.  Run unicode tools: org.unicode.tools.emoji.GenerateCldrData

    1.  Copy data from the console into (respectively) as per instructions, into 
        1.  **GenerateCldrData-console.txt**
    2.  ~~Copy emoji-test.txt into org.unicode.cldr.util.data.emoji~~

5.  Get the emoji images for the info panel of the survey tool
    *  This used to be done by running `org.unicode.tools.emoji.CopyImagesToCldr.java` to add images to ... /cldr/tools/cldr-apps/src/main/webapp/images/emoji
    *  They might be delivered from the ESR via a zip file.
7. ~~Update the collation/root.xml using unicode/draft/emoji/charts-VV/emoji-ordering-rules.txt~~
    * ~~Note: emoji-ordering-rules.txt should be moved into the repo (there's an issue for that)~~


# Do in CLDR
**For a sample, see [CLDR-17582 BRS Emoji for v46](https://github.com/unicode-org/cldr/pull/3667/files)**
- Adds into annotations: en.xml and root.xml
- Adds to images: emoji_1fa89.png, ...
- Adds emoji-test.txt
- Adds into collation/root.xml (for this, the [15.1 emoji PR](https://github.com/unicode-org/cldr/pull/2950/files#diff-79cd8667f61d7f7fa462effe4382a87581f1918af50e7c951bc56b2e81267cce) is a better example)


1. Copy **from**
    - https://github.com/unicode-org/unicodetools/blob/main/unicodetools/data/emoji/dev/emoji-test.txt
        - **to**
    - https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/emoji/emoji-test.txt
2. Copy **from**
    - https://github.com/unicode-org/unicodetools/blob/main/unicodetools/data/emoji/dev/internal/emoji-ordering-rules.txt
        - **into the following, replacing** `<!-- Machine-readable version of the emoji ordering rules for v16.0β (corresponding to CLDR). -->... ]]></cr></collation>`
    - https://github.com/unicode-org/cldr/blob/main/common/collation/root.xml
3. Copy emoji images from a .zip file supplied by the ESR folks
    - **First make sure that the images have the right file name, eg emoji_0023_20e3.png**
        - Make sure that there are no emoji variation selectors in the titles: no `_FE0F_`.
        - Ideally, they would include all and only the new emoji — that reduces the checking we need to do.
    - **into**
        - https://github.com/unicode-org/cldr/tree/main/tools/cldr-apps/src/main/webapp/images/emoji
4. Copy
    - From GenerateCldrData-console.txt
    - **into the ends of (respectively)**
        - [annotations/root.xml](https://github.com/unicode-org/cldr/blob/main/common/annotations/root.xml)
        - [annotations/en.xml](https://github.com/unicode-org/cldr/blob/main/common/annotations/en.xml)
6. Composition of names (for new components like hair styles)
    - For new composed names, modify org.unicode.cldr.util.Annotations
    - Ensure that the LDML document of composition of names in LDML is updated to match what is in org.unicode.cldr.util.Annotations.
7.  Run tests
    -  You may get an error in testAnnotationPaths.
          - May need to change org.unicode.cldr.util.Emoji.SPECIALS to have TestAnnotations pass. These are zwj sequences whose names cannot be composed.
          - eg "\[{🏳‍🌈}{👁‍🗨}{🏴‍☠}\]"
    -  You may also get an error in TestNames. Check the names to see what is happening, and whether to change the test or the data.
  
**TODO: test that derived names are complete
**

## BRS (if the UCD files are adjusted after the start of the release):

As above, except that you only need to

1.  Have the ESR run unicodetools GenerateEmoji with the beta options
2.  Copy emoji-test.txt (#1 above)
3.  Update collation/root.xml (#2 above)
