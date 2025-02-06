---
title: Running Tests
---

# Running Tests

You will always need to run tests when you do a check\-in.

1. Preconditions
	- If you change the DTD, be sure to read and follow [Updating DTDs](/development/updating-dtds) first.
	- If you added a new feature or fixed a significant bug, add a unit test for it.
		- See unittest/NumberingSystemsTest as an example.
		- Remember to add to unittest/TestAll
2. Run **TestAll \-e**
	- These are the unit tests in exhaustive mode
	- If you are doing something you know to be simple, you could do the shorter run of just **TestAll**
3. Run **ConsoleCheckCLDR \-e \-z final\_testing \-S common,seed**
	- This runs the same set of test that the Survey Tool does.
	- If you know what you are doing, you can run a set of filtered tests.
4. Other tests
	1. The unit tests are not complete, so you get a better workout if you are doing anything fancy by running:
	2. [**NewLdml2IcuConverter**](/development/coding-cldr-tools/newldml2icuconverter)
	3. [**Generating Charts**](/development/cldr-big-red-switch/generating-charts)
		1. If you have interesting new data, write a chart for it. See subclasses of Chart.java for examples.

## Running tests on the command line

```bash
$ export CLDR_DIR=/path/to/svn/root/for/cldr

$ cd $CLDR_DIR/tools/java && ant all

$ cd $CLDR_DIR/tools/cldr-unittest && ant unittestExhaustive datacheck
```

\[TODO: add more commands here; can't we automate all this into a single build rule for ant?] TODO: [TODOL ticket:8864](http://unicode.org/cldr/trac/ticket/8864)

## Debugging

\[TODO: add more tips here]

### Regexes

We use a lot of regexes!

1. There is org.unicode.cldr.util.RegexUtilities.showMismatch (and related methods) that are really useful in debugging cases where regexes fail. You hand it a pattern or matcher and a string, and it shows how far the regex got before it failed.
2. To debug RegexLookup, there is a special call you can make where you pass in a set. On return, that set is filled with a set of strings showing how far each of the regex patterns progressed. You can thus see why a string didn't match as expected.

