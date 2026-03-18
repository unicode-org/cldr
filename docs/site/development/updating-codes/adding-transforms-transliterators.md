---
title: Adding Transforms / Transliterators
---

# Adding Transforms / Transliterators

## Adding new Transliterators

There is a gotcha when adding transforms. If transform A-B depends on transform X (eg it uses ::A-C; ::C-B), then ICU has to register B before A. A table for this is built for testing CLDR transforms, and for the tool that converts to ICU: ConvertTransforms. When you add a new transform, you may need to add to that table. (If you are lucky, and X occurs alphabetically before A-B, then you don't need to do this.)

How to do it:

1. Open CLDRTransforms
2. Goto class DependencyOrder
3. There is a static list at the top of that class, with lines like:
   * addDependency("es-zh", "es-es_FONIPA", "es_FONIPA-zh");
1. Add a new line of that form

Make sure you run the tests to verify that the new transliterators are correct. 

## Testing Transliterators

run org.unicode.cldr.unittest.TestTransforms - _does a basic test of transforms_.


The following need to be merged into the unittest above. For now they are standalone.

1. org.unicode.cldr.test.TestTransformsSimple - _runs a few other tests_.
2. org.unicode.cldr.icu.ConvertTransforms - _generates the ICU-style transforms, in a folder of your choice. Do this before running TestTransforms._
3. org.unicode.cldr.test.TestTransforms - _runs the ICU4J transliteration tests. Set -Dfiles to the folder you used for #1, like:_
   * -Dfiles=${workspace_loc}/Generated/cldr/icu-transforms/

## Adding test files

You can add plaintext test files to the following folder. Any files there are run as a part of the unittest.

*  ${workspace_loc}/cldr/tools/java/org/unicode/cldr/util/data/test

Each such test file should have the name of the transliterator + ".txt". The format is:

`{source_string}{tab}{expected_result}`

_For example, for cs-ja.txt_

Achijáš Šíloský     アヒヤーシュ・シーロスキー
achnatonova アフナトノヴァ
...
