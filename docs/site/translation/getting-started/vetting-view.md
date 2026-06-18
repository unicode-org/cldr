---
title: 'Survey Tool: Dashboard tips'
---

# Survey Tool: Dashboard tips

## Dashboard

Once you have finished entering data, you will need to review the items for accuracy.
To assist with this, use the Survey Tool's Dashboard.

![Dashboard option in left navigation bar](../../images/gettingStartedGuideDashboard.png)

The Dashboard usually opens automatically when you open a locale in the Survey Tool.
It can also be accessed by clicking the Open Dashboard button in the upper right corner of the Survey Tool under the log-in option
if you have closed it previously.

The Dashboard will show you a list of data items with warnings of different kinds. Some may be false positives, but some will require action.
The idea here is that you should work the Dashboard down to show zero items.

### Dashboard Categories

**Tip:** The "New" category has been added to the Dashboard to help you identify any items which do not have a prior winning value so that you can review and vote for them.

![Example of a missing item in the dashboard](../../images/gettingStartedGuideMissing.png)

The information in the Dashboard is divided into the following categories.
If a category is not visible for you, that means you have no items in that category:

| Category | Priority | Description |
| :------: | :------: | :---------- |
| Error    | High   | The currently winning value has caused an error in validation that will have to be resolved before release. Fix the error. In some cases there is a conflict between items (such as two different items having identical translations) and you may have to fix the other, conflicting item, not necessarily the one showing the error. |
| Missing  | High   | These are items where there is no localization provided by any contributor. Click on the line to be taken to the item in the Survey Tool where items are highlighted and you can add a translation. When you fix a **Missing** item it will turn to **Changed**. See [Draft status symbols][] for an explanation of the different draft statuses. |
| Provisional  | High   | These are items where an unvetted value has been added. Sometimes this was constructed based on existing data in your locale. Almost all implementations will _not use_ this data until it has a higher status. See [Draft status symbols][] for an explanation of the different draft statuses. |
| New | High | These are items that did not have a winning value in the last release and that you have not yet voted for. All missing and provisional items will show up as ‘New’ until you vote on them. |
| Losing   | Medium | These are items that you have already voted on, but where your vote is not for the currently winning value. |
| Disputed | Medium  | There are two or more conflicting values that have received votes. |
| Changed  | Medium | Items show up under **Changed** because the Winning translation has been changed.	|
| English Changed | High | These are items where the English source has changed *after* the last change in translation. |
| Warnings | Medium | These are issues which appear after automatic checks. Some warnings may become Errors during the [Vetting phase][]. |
| Inherited Changed | High | The translated value is inherited from another value, and that value has changed. |
| Abstained | High  | These are values where you and the org you represent have not provided any vote at all. |
| Reports | High  | The reports show lists of items that need to be reviewed for consistency. |
| Other | Low | All items you have voted for which are not in any other category. Usually appears as a checkbox unless selected. |

### Fixing Entries

Each entry has the following information about that item:

| Column | Description|
|------- |----------- |
| Dashboard category | The first letter of the section name enclosed in a circle. |
| Data Type | The section that the item belongs to. |
| Code | This is a link to the field in the Survey Tool. |
| English | The English value is highlighted in blue. |
| Winning _**XX**_ |The currently winning value is highlighted in green. |
| Hide checkbox | For items that can be hidden, a checkbox to hide that option appears on the far right. |

![example of a warning in the dashboard for another locale](../../images/gettingStartedGuideWarning.png)

### Clearing Items

There are two ways to clear items from the list:

1. The preferred way is to fix them (such as adding a translation for a missing item). Click on the item line to bring up that item in the Survey Tool where you can change or edit your vote, or post a forum vote to try to convince the other vetters to change their vote for losing or disputed items.
2. The other is to hide them (such as when the English has changed but the translation doesn’t need to change). Only hide an item if it really is a false positive, not because you gave up on fixing it. To hide an item from view, click on the checkbox on the far right of the item. Note that this will not make it disappear, just hide it from view. Click on the check box next to 'Hide' on the Dashboard menu to hide or unhide hidden lines. The count on each section will show how many items are showing versus the total.

More information on how to handle each of the types of issues as follows:

<a id='missing'>
#### Missing (only applicable during Submission phase)

1. Add the missing value, or vote for an "inherited" value (in a special color).
2. Unless there is some other error, you can't change these during the vetting phase, so make sure to get them done early!

<a id='provisional'>
#### Provisional (priority during Submission phase)

1. Check the current provisional value and vote to confirm it, vote for an ["inherited" value][] (in a special color), or add a new value if neither the provisional value or inherited value is correct.
2. Unless there is some other error, you can't add any new data values during the vetting phase, so make sure to get them done early!

<a id='new'>
#### New (priority during Submission phase)

1. Check the current provisional value and vote to confirm it, vote for an ["inherited" value][] (in a special color), or add a new value if neither the provisional value or inherited value is correct.
2. Unless there is some other error, you can't add any new data values during the vetting phase, so make sure to get them done early!

**Note:** Any Missing and Provisional items will also show up under the New category since they do not have winning values from the prior release.

#### Changed, English Changed, and Inheritance Changed

1. Check the currently winning value and vote to confirm it, vote for an ["inherited" value][] (in a special color), or add a new value if none of the existing options is correct.
2. Unless there is some other error, you can't add any new data values during the vetting phase, so make sure to get them done early!

<a id='error'><a id='warning'>
#### Error, Warning

1. Go to the item (by clicking the **Fix?** link).
2. Error items will be removed from the release, so they are a priority in the vetting phase.
3. Review the warning items; most of them need fixing but not all. See [Handling Errors and Warnings][].

<a id='losing'>
#### Losing

1. Decide if you can live with the currently winning value, even if you don't think it is optimal, but reasonable. If so, change your vote to be for the winning item.
2. Otherwise, click the **Forum** button in the **Info Panel** and give reasons for why your fellow vetters should change their vote to align with yours.
   1. If others change their vote, the value may still be approved before the end of the cycle. **Engage with others on the Forum discussions**.
   2. Make sure to post good and verifiable arguments as to why others should change their votes and **respond to others’ posts**.
   3. Review all of the items to see if someone else’s item is better and read the forum, and whether you want to change your vote.
   4. Discuss in the forum, then use the Hide button to hide disputes you’ve addressed. 

<a id='disputed'>
#### Disputed

1. See if the winning value is ok. If so, change your vote to it, and go to the next item.
2. Otherwise, post a message to the [Forum][].
   1. State why you think the item should be changed.
   2. If there are a number of items that have the same characteristic (such as the wrong capitalization), you can make that case in a single posting on the first item in the category rather than multiple ones.

<a id='reports'>
#### Review Reports

Once you have completed your items, review the [Reports][] again to see that all the changes are as expected.

[Handling Errors and Warnings]: /translation/getting-started/errors-and-warnings
["inherited" value]: /translation/getting-started/guide#inheritance
[Forum]: /translation/getting-started/guide#forum
[Reports]: /translation/getting-started/review-formats
[Data Resolution process]: /index/process#data-resolution
[Submission phase]: /translation/getting-started/survey-tool-phases#survey-tool-phase-general-submission
[Vetting phase]: /translation/getting-started/survey-tool-phases#survey-tool-phase-vetting

