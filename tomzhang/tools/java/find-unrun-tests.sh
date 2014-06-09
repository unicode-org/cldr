#!/bin/sh
# by srl 2013 sep 4
grep 'extends.*TestFmwk' org/unicode/cldr/unittest/*.java | cut -d/ -f5 | cut -d. -f1 | sort | uniq > all_test_classes.txt
ant check -Druncheck.arg='-l' | fgrep -- '--'  | fgrep -v -- '-- CLDR' | cut -d'-' -f3 | tr -d ' ' | sort > currently_running.txt
echo "---- Unit Test Report ----"
echo "Lines ending in '<' are tests that exist but are not run."
echo "Lines ending in '>' are tests that are run but don't exist (!)"
diff --side-by-side --suppress-common-lines all_test_classes.txt currently_running.txt
echo
echo "And the following are just wrong- should be under unittest."
find org com -name '*.java'   | xargs grep 'extends.*TestFmwk' | fgrep -v org/unicode/cldr/unittest

