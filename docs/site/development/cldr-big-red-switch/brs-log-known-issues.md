---
title: 'BRS: Handling Known Issues'
---

Sometimes, we skip tests during development “temporarily” rather than comment them out or remove them, expecting to reinstate them once the relevant bug is fixed.  To achieve this end, the "logKnownIssues" mechanism has been used, shared with the [ICU project](https://icu.unicode.org/setup/eclipse/time).

See [Testing!](/development/development-process/#testing) for more details on how this is used during development.

For this BRS task there are two goals. FIRST, make sure that any log known issue targets that are remaining issues, point to a currently open ticket.  SECOND, make sure that any issues that actually were fixed, have the "logKnownIssue" call removed to make sure the regression tests are actually run.

Steps:

1. Create a ticket. You can use [CLDR-18267](https://unicode-org.atlassian.net/browse/CLDR-18267) as an example: "v47 BRS: Known Issues".

2. Run CLDR tests (See [Testing!](/development/development-process/#testing))

3. At the end of the test run,  you should see some output similar to this (even though the tests passed):

    ```md
    1 Known Issues:
    CLDR-14166 <https://unicode-org.atlassian.net/browse/CLDR-14166>
    - CLDR/LanguageInfoTest/TestChinese (Skip until CLDR updated for new ICU4J LocaleMatcher)
    - CLDR/LocaleMatcherTest/testChinese (Skip until CLDR updated for new ICU4J LocaleMatcher)

    << ALL TESTS PASSED >>
    ```

    There will only be one major heading for each CLDR (or ICU) ticket.
    Note: An entry here means that a test was _skipped_, not that the test failed.

4. Let's see if any of these are declared to be fixed.

    Copy and paste that entire "known issues" section (between the `1 Known Issues` and `<< ALL TESTS PASSED >>`) into the new Jira ticket created above.  It might be best to create these into an "Expand" section, so that it doesn't take up as much screen space.

    Jira will linkify the tickets, making it easy to see what each one's status is.

5. _For any tickets that are listed as "DONE" or "REVIEWING"_ see if the test still fails as below.  These are command line instructions, they can be converted into `-D` properties for eclipse, etc.

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

6. Tests should pass again (without special options), with updated logKnownIssues updates, after you make your change.  Check your changes into a PR and send it for review, etc.

7. Finally, click through all of the Jira tickets (either from the ticket you created, or from the command line).  Make sure each one has the `LogKnownIssues` label set.
