# New CLDR Developers

Here is a quick overview of what you need to know to do development
work on CLDR.

First, you need to have accounts set up for you on:


1. **Jira** -- for getting and handling bug reports.
2. **GitHub** -- for submitting code / data.
3. **Git LFS (Large File Support)** -- in particular, this is used for jar files
   in `tools/java/libs`.
    * Install `git lfs` support.
    * It might be enough to simply install `git-lfs` before cloning the CLDR
      repository.
    * For repositories that have already been cloned without LFS, you could try
      `git lfs fetch && git lfs checkout` to grab the `.jar` files.
4. **cldr-dev** -- for discussions of issues, questions, etc. -- if you will be
   joining the TC.
5. **Google Docs** -- to view/edit the CLDR agenda and internal documents -- if
   you will be joining the TC.
6. **Google Sites** -- only if you are going to edit the CLDR website.

If you don't get emails about these, contact Rick or other CLDR contacts. It is
handy, though not necessary, for you to use a gmail account for the last two of
these. Many people use a different account than their internal company email
address; you just have to link them with https://accounts.google.com/SignUp.

*Warning: some of these pages get stale. Ask questions on cldr-dev if you run
into problems; you or the responder should also fix the stale page.*

Next, get your Eclipse environment set up properly.

1. http://cldr.unicode.org/development/eclipse-setup
2. http://cldr.unicode.org/development/running-survey-tool/eclipse


## Run the CLDR tests to be sure they pass before beginning work:

Command line:

1. Be at the root of your CLDR git repository
2. cd tools/java
3. ant -version  # at least version 1.10.7
4. ant all
5. cd ../cldr-unittest
6. ant check
7. If you see test errors, for instance TestBasic/TestDtdComparison fails, run
   only the failing test like so:

       ant -Druncheck.arg="-v TestBasic/TestDtdComparison" check

   The -v tells test script to show stack trace at the test failure for
   debugging.

8. To get all parameters that could be passed at runcheck.arg, run

        ant -Druncheck.arg="-?" check

Via eclipse:

1. Go [here](http://cldr.unicode.org/development/eclipse-setup#TOC-Test).

Once you are all set up, be sure to read the development process, for how to
handle tickets, when you can't make changes, etc. at:
http://cldr.unicode.org/development/development-process (TBD update for
migration to Jira/Github)

---

The table below points to documentation for various tasks.

| Task to complete | Link to documentation |
| ---------------- | --------------------- |
| moving new CLDR data over to ICU by editing ldml2icu_locale.txt | http://cldr.unicode.org/development/coding-cldr-tools/newldml2icuconverter |
| performance work          | http://cldr.unicode.org/development/perf-testing |
| survey tool database work | http://cldr.unicode.org/development/running-survey-tool/cldr-properties/db |

Other useful pages are under [CLDR Development
Site](http://cldr.unicode.org/development); you can also use the search box.

[UTS #35: Unicode Locale Data Markup Language
(LDML)](http://www.unicode.org/reports/tr35/) is the specification of the XML
format used for CLDR data, including the interpretation of the CLDR data.
