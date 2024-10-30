---
title: FAQ and Known Bugs
---

# FAQ and Known Bugs

[**Survey Tool**](http://st.unicode.org/cldr-apps/survey) **\|** [**Accounts**](/index/survey-tool/survey-tool-accounts) **\|** [**Guide**](/translation/getting-started/guide) **\|** [**FAQ and Known Bugs**](/index/survey-tool/faq-and-known-bugs)

## FAQ (Frequently Asked Questions)

***Q. Should I preserve the case of English words, like names of languages?***

A. Beginning with CLDR 22, the new guidance is that names of items such as languages, regions, calendar and collation types, as well as names of months and weekdays in calendar data, should be capitalized as appropriate for the middle of body text. For more information, see the [Capitalization](/translation/translation-guide-general/capitalization) section in the [Translation Guidelines](/translation/).

***Q. What about the warning about parentheses being discouraged in cases such as "(other)"***

A. You need to remove "(other)" or the equivalent from language names. In general, you should avoid using parentheses in the names of languages, scripts, or regions if at all possible. There is more information about this in the zoomed view.

***Q. Why is the tool slow?***

A. The performance of the Survey Tool has been greatly improved compared to previous versions. However, we are constantly striving to improve performance and our ability to accommodate a larger user base.

If you feel a task is taking an unusual amount of time, and it is a consistent problem, please [file a ticket](/requesting_changes#how-to-file-a-ticket). In the ticket, describe exactly what operation is being attempted and approximately how long it is taking to receive a response.

***Q. How are votes weighted and the "best" item picked?***

A. You basically want to get multiple organizations to agree on the best value. For details on the voting process, see [Resolution Procedure](/index/process).

***Q. In the key, it says that the red box is a fallback. What does that mean?***

A. The Unicode CLDR data uses inheritance. That means that if you are looking at *English (United Kingdom)* (a "sublocale") most of the data is inherited from *English* (which contains data for the US), called the "parent locale". Such data will show up as red. You only need to have different data in the sublocale where there are important differences in usage from the parent locale.

Data in a sublocale may be *spuriously different*; that is, the parent's data may be perfectly acceptable in the sublocale, but somehow a difference has crept in. In that case, you should vote for the parent's data to reduce the gratuitous differences.

***Q. But what I see is a funny symbol like Zxxx?***

A. If there is no other translation available, what you will see is a "neutral" code, typically an ISO code. In cases where there is no such code available, such as for labels like "Month", then you may see English \-\- which needs to be translated.

***Q. How do I delete an item?***

A. You can only delete an item if you yourself have entered it, and there are no other votes. Click on the "Abstain" button for that row.

To remove a spurious difference in a sublocale, vote for the red fallback item.

***Q. What if I can't delete it?***

A. It doesn't really matter much. What is really important is to make sure the the *right* item is voted for; so try to get consensus as described above. If all the alternatives are really wrong, and you really don't know what the right item would be, vote for the red fallback item.

***Q. What if I want to just try out some changes, but don't want to affect the data?***

A. Everyone can add data to "**Unknown or Invalid Language**" (und), so you can try out the Survey tool there without worry.

**Q. What if I have questions?**

A. You should click on the items you have questions about, and read the information in the right\-hand information panel.

*In many cases, even seemingly straightforward translations like the language, script, and territory names have issues.*

You can also go directly to the [Translation Guidelines](/translation).

If you have further questions, or problems with the Survey Tool, send a message to [cldr\-users@unicode.org](mailto:cldr-users@unicode.org).

## Known Bugs, Issues, Restrictions

The following are general known bugs and issues. For known issues in the current release, see [Translation Guidelines](/translation).

1. The description of bulk uploading (http://cldr.unicode.org/index/survey-tool/upload) has not yet been updated for the new UI.
2. The description of managing users (http://cldr.unicode.org/index/survey-tool/managing-users) has not yet been updated for the new UI.

If you find additional problems, please [file a ticket](/requesting_changes#how-to-file-a-ticket).

