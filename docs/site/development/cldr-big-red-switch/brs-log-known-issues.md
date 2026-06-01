---
title: 'BRS: Handling Known Issues'
---

Sometimes, we skip tests during development “temporarily” rather than comment them out or remove them, expecting to reinstate them once the relevant bug is fixed.  To achieve this end, the "logKnownIssues" mechanism has been used, shared with the [ICU project](https://icu.unicode.org/setup/eclipse/time). For JUnit tests, a related mechanism is used, see the TestWithKnownIssues.java class.

See [Testing!](/development/development-process/#testing) for more details on how this is used during development.

For this BRS task there are two goals. FIRST, make sure that any log known issue targets that are remaining issues, point to a currently open ticket.  SECOND, make sure that any issues that actually were fixed, have the "logKnownIssue" call removed to make sure the regression tests are actually run.

Steps:

1. Create a ticket to track your work. You can use [CLDR-18267](https://unicode-org.atlassian.net/browse/CLDR-18267) as an example: "v47 BRS: Known Issues".

2. View a recent run of CLDR tests. The recommended way is to view a [recent run of cldr-mvn against main](https://github.com/unicode-org/cldr/actions/workflows/maven.yml?query=branch%3Amain)
(A local run of the tests can also be used, see [testing](/development/development-process/#testing). The remainder of this page will discuss using a build on GitHub.)

3. In the ci Summary, scroll down to the "build summary" or search for "Known Issues".  This is on the summary page for the build, NOT the detailed build logs.
"

4. Expand each of the sections labelled "Known Issues". There may be multiple sections, each with one or more known issues (due to how the tests run). 
Each known issue looks something like this:

 * [CLDR-7075](https://unicode.org/cldr/trac/ticket/7075)
 
```
CLDR/TestSupplementalInfo/TestPluralCompleteness (Missing ordinal minimal pairs)
CLDR/TestSupplementalInfo/TestPluralSamples2 (Missing ordinal minimal pairs)
```

All of the failures _under that ticket_ are grouped together. There will only be one major heading for each CLDR (or ICU) ticket.
Note: An entry here means that a test was _skipped_, not that the test failed. We need to find out whether the test still fails.

5. First, check to see if any of these are declared to be fixed. Click on each one and read its Jira status.

Also, make sure each ticket has the `LogKnownIssues` label set.

6. _For any tickets that are listed as "DONE" or "REVIEWING"_ see if the test still fails as below.  These are command line instructions, they can be converted into `-D` properties for eclipse, etc.

    ```shell
    mvn test --file tools/pom.xml -pl cldr-code -Dtest=org.unicode.cldr.unittest.TestShim '-Dorg.unicode.cldr.unittest.testArgs=-prop:logKnownIssue=no -filter:TestChinese'
    ```

    If the tests _still failed_, then the issue isn't fixed!  Open up the referenced CLDR ticket (the URL was given in the test results above) and see what is happening.  If the ticket was closed a while ago, you may need to clone the referenced ticket (here CLDR-14166) and create a new ticket so that the bug actually gets fixed.  Change the referenced ticket in the `logKnownIssue()` function call from the old number to the new number. [Here is an example of such a change.](https://github.com/unicode-org/cldr/pull/4322/files#diff-f086d09aeea63b3da66518165de7b62e9de9c00477d237293f53b9e56399cd48)

    If the tests _passed_, then the issue seems to be fixed, however, we've failed to re-enable the test case.
    You must REMOVE the call to `logKnownIssue()` as well as the logic that skips the test. [Here is an example of such a change.](https://github.com/unicode-org/cldr/pull/4322/files#diff-62a59f099d43391ba60f3cdde57c4cbdd6f9adee4200e8730200b9707f4992bcL141-L144)  Generally the code to remove looks like the following (note the `if` and `continue;`).

    ```java
    if (logKnownIssue("CLDR-14166", "We have to fix this and that before we can fix this")) {
        continue;
    }
    ```

7. Tests should pass again (without special options), with updated logKnownIssues updates, after you make your change.  Check your changes into a PR and send it for review, etc.

8. Update your ticket with status - i.e. which known issues are remaining, which are already done.
