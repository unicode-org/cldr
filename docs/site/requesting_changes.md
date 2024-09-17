# Requesting Changes

There are two ways to request changes (for bug fixes or new features). Filing a ticket and fixing in the Survey Tool.

## When to file a ticket

* Additions of new locales (see Adding New Locales, below)
* Defects in the survey tool
* Adding or changing non-language data (such as currency usage)
* Additions or changes to data that is not yet handled by the survey tool (collation, segmentation, and transliteration)
* Feature requests in CLDR or [UTS #35: Locale Data Markup Language (LDML)](https://www.unicode.org/reports/tr35/).

In CLDR Bug Reports, please try to give as much information as possible to help address the issue, and please group related bugs
(such as a list of problems with the LDML specification) into a single bug report.

A few areas are particularly tricky.

* For the sort order of a language, see [Collation Guidelines](https://cldr.unicode.org/index/cldr-spec/collation-guidelines)
* For plural rules (ordinals and cardinals), see [Plural Rules](https://cldr.unicode.org/index/cldr-spec/plural-rules)

### Requesting updates to locale data through a ticket

If you are requesting a change to language data with a ticket, please provide appropriate documentation for the language specialists for
that market to consider during their next review (usually annually and sometimes twice during the year).

Please note that for all formats, CLDR data aligns to the most common usage in the market. That is, decisions are based on the market and
usage cases, as well as language, so that if a prescriptive language body and/or other authority's recommended use conflicts with the predominant use described in the points below, then the predominant use should still be the one reflected in CLDR. The more important the data, we look for more evidence and discussions among the language specialists.

Helpful information include:
* Examples of the most prominent usage(s)
* Evidence of usage in the mainstream media (for example, a newspaper or magazine)
* If there are alternatives (e.g. different currency symbols), we need to see examples of those (from the most prominent sources). Please note that failure to supply alternatives if they exist significantly reduces credibility.

Please file all tickets in English since the people who are reviewing your ticket are likely to not speak your language. You can use automated translation software if you don't speak English well enough, and include the same text in your native language as well.

Note: If you are a regular user of that language you can also contribute directly through the Survey Tool. [More information about the process including opening an account.](https://cldr.unicode.org/index/survey-tool)

### How to File a Ticket

The CLDR tickets are located at: https://unicode-org.atlassian.net/projects/CLDR/.

To file a ticket, click the red "Create" button on the top navigation bar. See [Jira documentation](https://support.atlassian.com/jira-work-management/docs/create-issues-and-subtasks/)
for additional details on how to create an issue.

You must have an account in order to file tickets. If you do not have an account, you can [request a Jira account](https://id.atlassian.com/signup?continue=https%3A%2F%2Funicode-org.atlassian.net%2Flogin%3FredirectCount%3D1%26dest-url%3Dhttps%253A%252F%252Funicode-org.atlassian.net%252Fprojects%252FCLDR%252Fissues&application=jira). 

## When to fix in survey tool

If you regularly use software in a language and would like to contribute and help fix/add data for that language, please use [the Survey Tool]( https://cldr.unicode.org/index/survey-tool)
during the regular CLDR development cycle.

## Adding New Locales

If you would like to add data for a new locale: 

* Make sure that you pick the right locale code for the new data. See [Picking the Right Language Code](https://cldr.unicode.org/index/cldr-spec/picking-the-right-language-code)
* Gather the [Core Data for New Locales](https://cldr.unicode.org/index/cldr-spec/core-data-for-new-locales). A new locale is only added if someone commits to supplying/maintaining the data.
* Follow the instructions above to file a ticket, requesting the addition.  Add the language code (#1) and core data (#2) in the ticket)
