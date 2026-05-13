---
title: Survey Tool phases
---

# Survey Tool phases

This page explains the status entry in the top left corner of the [top of the Survey Tool page](https://st.unicode.org)
<!-- TODO: Add image of the different status messages in ST -->
For the latest news about the Survey Tool, see the [Information Hub for Linguists: News][] section.

## Phases

Data collection in the Survey tool for a regular submission cycle has 4 phases:

1. Shakedown
2. General submission
3. Vetting
4. Resolution

## Regular Submission phases

Locales which are directly managed by the [CLDR TC][] will follow the regular submission process in order to facilitate discussions
in the [Forum][] and allow resolution of any disputed items.

### Survey Tool phase: Shakedown

_Make sure your coverage level is set correctly at the top of the page._

Shakedown is on an invitation basis. If you have not received an invitation to participate in shakedown, your participation is discouraged.

You should know in this stage:

- The survey tool is **live and all data that you enter will be saved and used.**
- You can start work
- Expect a churn: there may be additional Tooling fixes and Data additions during this period. 
- Tool may be taken down for updates more frequently during general submission
- You are expected to look for issues with the Survey tool and any other problems you encounter as a vetter. Please see instructions for [Reporting Survey Tool issues][] for how to report any issues you encounter.

### Survey Tool phase: General Submission

_Make sure your coverage level is set correctly at the top of the page._

For new locales or ones where the goal is to increase the level, it is best to proceed page-by-page starting with the **Core Data** section. At the top of each page you can see the number of items open on the page. Then scan down the page to see all the places where you need to vote (including adding items). Some 

Then please focus on the [Dashboard][] view,

1. Get all **Missing**† items entered
2. Vote for all **Provisional** items (where you haven't already voted)
3. Address any remaining **Errors\***
4. Review the **English Changed** (where the English value changed, but your locale's value didn't. These may need adjustment.)
    - \* Note that if the committee finds systematic errors in data, new tests can be added during the submission period, resulting in new **Errors**.
    - † Among the _**Missing**_ are are new items for translation. (On the [Dashboard][], **New** means winning values that have changed since the last release.)

If you are working in a sub-locale (such as fr\_CA), coordinate with others on the Forum to work on each section after it is are done in the main locale (fr). That way you avoid additional work and gratuitous differences. See voting for inheritance vs. hard votes in [Survey Tool Guide][]. 

### Survey Tool phase: Vetting

All contributors are encouraged to move their focus to the [Dashboard][] view. Also see [Dashboard tip](/translation/getting-started/vetting-view)s to use during Vetting stage.
1. Any [Flagged entries](https://st.unicode.org/cldr-apps/v#flagged///) in your locale may have questions in the Forum from TC members; please add additional information to help the TC resolve them.
    1. To see the Flagged items, click on the **Menu** icon in the upper left corner, under the **Forum** header see **Flagged items**:
2. Open the Dashboard, and resolve all of the Errors, Provisional Items, Disputed items, and finish Reports
    1. Consider other's opinions by reviewing the **Disputed** and the **Losing**. See guidelines for handling Disputed and Losing in the [How to handle different Dashboard categories][] section
3. Review all open Requests and Discussions in the [Forum][], and respond.

### Resolution (Closed to vetters)

The vetting is done and further work is being done by the CLDR committee to resolve problems. You should periodically take a couple of minutes to check your [Forum][] to see if there are any questions about language-specific items that come up.

## Special Survey Tool phases

It is possible for the Survey Tool to be open for some locales and not others. See below for more information.

### Mixed Phases

It is possible for the Survey Tool to be open for some locales and not others. In this case, you may see a mixed status such as `VETTING_CLOSED/SUBMIT` if you have not selected a locale yet. Log in and choose a specific locale to see the status for that locale.

### Survey Tool phase: Extended Submission

The Extended Submission phase was created to support the [non-TC locales][] that are managed by the [DDL WG][].
The longer submission period allows smaller organizations to submit data on a more flexible timeline.
Locales in Extended Submission often begins earlier than [General Submission][] and may stay in a SUBMIT type phase
while TC-locales move on to the [Vetting][] and [Resolution][] phases. The Extended Submission for a major release may even be open before the previous version of CLDR has been released.

The locales that are eligible for Extended Submission may change in the future. 
It may be that multiple organizations start contributing to a locale and it becomes important for them to collaborate in the [Forum][] to resolve Disputed items.

## Regular vs. Limited-submission releases

There are two types of releases: regular-submission and limited-submission.

- Regular-submission release
    - Typically with the even versions (e.g. Version 36).
    - All languages and data areas are open for contributions. 
- Limited-submission release
    - Typically with odd versions (e.g. version 37).
    - Selected fields are open for all locales; the voting options will be grayed out for the data points not in scope.
        - Proceed with Submission (General), but start with the [Dashboard][] and focus on Errors\*, Missing†, Provisional
    - Selected locales are open for votes on all fields.
        - Proceed with Submission (General).
- Extended-submission
    - Similar to Limited-submission, only some locales are open for submission.
    - Submission often opens earlier and is often extended to encompass the vetting (and sometimes resolution) period.
    - Extended submision may occur as part of a Regular or Limited submission release.

[CLDR TC]: /cldr-tc
[Dashboard]: /translation/getting-started/guide#dashboard
[DDL: Helpl Center]: /translation/ddl
[DDL locales list]: /ddl#list
[DDL WG]: /cldr-tc#ddl-working-group
[Forum]: /translation/getting-started/guide#forum
[General Submission]: /translation/getting-started/survey-tool-phases#survey-tool-phase-general-submission
[How to handle different Dashboard categories]: /translation/getting-started/guide#how-to-handle-different-categories
[Information Hub for Linguists: News]: /translation/getting-started/guide#news
[non-TC locales]: /ddl#list
[Reporting Survey Tool issues]: /translation/getting-started/guide#reporting-survey-tool-issues
[Resolution]: /translation/getting-started/survey-tool-phases#resolution-closed-to-vetters
[Survey Tool guide]: /translation/getting-started/guide
[Vetting]: /translation/getting-started/survey-tool-phases#survey-tool-phase-vetting
