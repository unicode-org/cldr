Update emoji translations & ordering
====================================

SBRS (at the start of the release):
-----

Where the current version is VV:

1.  Run unicodetools GenerateEmoji with specific version number, like 14.0

1.  If you get an error like

    * Exception in thread "main" java.lang.IllegalArgumentException: no name for 🫱‍🫲 1FAF1 200D 1FAF2
    * at org.unicode.tools.emoji.EmojiData.\_getName(EmojiData.java:1230)
    * at org.unicode.tools.emoji.EmojiData.getName(EmojiData.java:1194)
    * at org.unicode.tools.emoji.EmojiDataSourceCombined.getName(EmojiDataSourceCombined.java:156)
    * at org.unicode.tools.emoji.GenerateEmoji.showCandidateStyle(GenerateEmoji.java:3600)
    * at org.unicode.tools.emoji.GenerateEmoji.main(GenerateEmoji.java:641)

2.  Then change the name composition algorithm if necessary (for new emoji zwj sequences)

    1.  It may have also been modified during the emoji development. Typically the code that needs changing will be in Annotations.synthesize, to capture yet another special skintone instance
    2.  Ensure that the documentation of composition of names (for new components like hair styles) in LDML is updated to match what is in org.unicode.cldr.util.Annotations.
    3.  Make sure that org.unicode.tools.emoji.unittest.TestAll runs successfully, with -Demoji-beta.

2.  Copy

    * /emoji/docs/Public/emoji/14.0/emoji-test.txt<br>
	to
    * /cldr-code/src/main/resources/org/unicode/cldr/util/data/emoji/emoji-test.txt

3.  Run unicode tools: org.unicode.tools.emoji.GenerateCldrData

    1.  Copy each list of data from the console into (respectively) as per instructions
    
        1.  annotations/root.xml
        2.  annotations/en.xml

    2.  Copy emoji-test.txt into org.unicode.cldr.util.data.emoji

4.  Run org.unicode.tools.emoji.CopyImagesToCldr.java to add images to ... /cldr/tools/cldr-apps/src/main/webapp/images/emoji

    * These are the ones that show up in the info panel of the survey tool.
    * Update the collation/root.xml using unicode/draft/emoji/charts-VV/emoji-ordering-rules.txt

5.  Run tests

    1.  You may get an error in testAnnotationPaths.

        1.  May need to change org.unicode.cldr.util.Emoji.SPECIALS to have TestAnnotations pass. These are zwj sequences whose names cannot be composed.
        2.  eg "\[{🏳‍🌈}{👁‍🗨}{🏴‍☠}\]"

    2.  You may also get an error in TestNames. Check the names to see what is happening, and whether to change the test or the data.

TODO: test that derived names are complete

BRS (if the UCD files are adjusted after the start of the release):
----

As above, except that you only need to

1.  Run unicodetools GenerateEmoji with the beta options
2.  Copy emoji-test.txt into org.unicode.cldr.util.data.emoji
3.  update collation/root.xml using unicode/draft/emoji/charts-XX/emoji-ordering-rules.txt