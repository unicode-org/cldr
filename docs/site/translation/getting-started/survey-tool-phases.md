---
title: Survey Tool stages
---

# Survey Tool stages

## Stages

Data collection in the Survey tool has 4 stages:

1. Shakedown
2. General submission
3. Vetting
4. Resolution

## Full vs. Limited Submission 

There are two types of releases: full, and limited-submission.

- Full-submission:
    - Typically with the even versions (e.g. Version 36).
    - All languages and data areas are open for contributions. 
- Limited-submission
    - Typically with odd versions (e.g. version 37).
    - Selected fields are open for all locales; the voting options will be grayed out for those data points that are not in scope.
        - Proceed with Submission (General), but start with the [Dashboard](https://cldr.unicode.org/translation/getting-started/guide#h.bmzr9ejnlv1u) and focus on Errors\*, Missing†, Provisional
    - Selected locales are open for votes on all fields.
        - Proceed with Submission (General). 

### Survey Tool phase: Shakedown

_Make sure your coverage level is set correctly at the top of the page._

Shakedown is on an invitation basis. If you have not received an invitation to participate in shakedown, your participation is discouraged.

You should know in this stage:

- The survey tool is **live and all data that you enter will be saved and used.**   
- You can start work  
- Expect a churn: there may be additional Tooling fixes and Data additions during this period.  
- Tool may be taken down for updates more frequently during general submission
- You are expected to look for issues with the Survey tool and any other problems you encounter as a vetter. Please [file a ticket](https://cldr.unicode.org/index/bug-reports).

### Survey Tool phase: General Submission

_Make sure your coverage level is set correctly at the top of the page._

For new locales or ones where the goal is to increase the level, it is best to proceed page-by-page starting with the **Core Data** section. At the top of each page you can see the number of items open on the page. Then scan down the page to see all the places where you need to vote (including adding items). Some 

Then please focus on the [Dashboard](https://cldr.unicode.org/translation/getting-started/guide#h.bmzr9ejnlv1u) view,

1. Get all **Missing**† items entered 
2. Vote for all **Provisional** items (where you haven't already voted) 
3. Address any remaining **Errors\***  
4. Review the **English Changed** (where the English value changed, but your locale's value didn't. These may need adjustment.)
    - \* Note that if the committee finds systematic errors in data, new tests can be added during the submission period, resulting in new **Errors**.
    - † Among the _**Missing**_ are are new items for translation. (On the [Dashboard](https://cldr.unicode.org/translation/getting-started/guide#h.bmzr9ejnlv1u), **New** means winning values that have changed since the last release.)

If you are working in a sub-locales (such as fr\_CA), coordinate with others on the Forum to work on each section after it is are done in the main locale (fr). That way you avoid additional work and gratuitous differences. See voting for inheritance vs. hard votes in [Survey Tool guide](https://cldr.unicode.org/translation/getting-started/guide). 

### Survey Tool phase: Vetting

All contributors are encourage to move their focus to the [Dashboard](https://cldr.unicode.org/translation/getting-started/guide#h.bmzr9ejnlv1u) view. Also see [Dashboard tip](https://cldr.unicode.org/translation/getting-started/vetting-view)s to use during Vetting stage.
1. Any [Flagged entries](https://st.unicode.org/cldr-apps/v#flagged///) in your locale may have questions in the Forum from TC members; please add additional information to help the TC resolve them.
    1. To see the Flagged items, click on the **Menu** icon in the upper left corner, under the **Forum** header see **Flagged items**:
2. Open the Dashboard, and resolve all of the Errors, Provisional Items, Disputed items, and finish Reports
    1. Consider other's opinions, by reviewing the **Disputed** and the **Losing**. See guidelines for handling [Disputed](http://cldr.unicode.org/translation/getting-started/guide#TOC-Disputed) and [Losing](http://cldr.unicode.org/translation/getting-started/guide#TOC-Losing).
3. Review all open Requests and Discussions in the [Forums](https://cldr.unicode.org/translation/getting-started/guide#h.fx4wl2fl31az), and respond.
        

### Resolution (Closed to vetters)

The vetting is done, and further work is being done by the CLDR committee to resolve problems. You should periodically take a couple of minutes to check your [Forums](https://cldr.unicode.org/translation/getting-started/guide#h.fx4wl2fl31az) to see if there are any questions about language-specific items that came up.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)