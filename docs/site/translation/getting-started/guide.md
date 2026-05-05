---
title: Survey Tool Guide
---

# Survey Tool Guide

### _News_

- 2026-05-04 Survey Tool Guide revised for clarity

The Survey Tool is a web based tool for collecting CLDR data and includes various features that the contributors (vetters) should know before getting started.

> Note: The exact appearance in screenshots may change as the tool is enhanced over time and based on your vetter level.

### 💡 Helpful Tips

- Please read the home page of the [Translation Guidelines](/translation) before starting your data contribution.
    - If you experience a **Loading...** problem with the Survey Tool, try clearing your browser cache. See [Reloading JavaScript](https://www.filecloud.com/blog/2015/03/tech-tip-how-to-do-hard-refresh-in-browsers/#.XOjGNtMzbuM).
- **Browser support** for Survey Tool includes the latest versions of Edge, Safari, Chrome, and Firefox.
- Use [Reports](#reports) at the beginning to review the data in your language in a holistic manner for Date & time, Zones, and Numbers.
    - Capitalization: Translations should be what is most appropriate for ”middle-of-sentence” use. So, for example, if your language normally doesn't spell languages with a capital letter, then you shouldn’t do that here. Please see [Capitalization](/translation/translation-guide-general/capitalization) for more details.
    - Plurals: For important information regarding the use of plural forms for your language, please read [Plurals](/translation/getting-started/plurals).

### Vetting Phase

At a point towards the end of a Survey Tool period, the Technical Committee will change the Survey Tool to "Vetting Mode". In Vetting Mode, submitting new data/translations is no longer possible, but you can still change your votes and participate in the forum. (The exception is that you can submit new data if the currently winning value has generated an error or a warning.)

---

## Login and Import of old votes

At the start of each CLDR cycle, you will need to go to <https://st.unicode.org/cldr-apps/survey/> and log in.

![Log In button in upper right hand corner of Survey Tool](../../images/gettingStartedGuideNotOpenYet.jpeg)

Your old votes will be imported automatically the first time you log in. Your previous votes will only be imported **if they match** the most recently released data.
If you have voted previously, you will see a message showing the number of your votes that matched the currently winning votes that have been auto-imported.
Import of your old votes will take some time, but this is only done at the first log-in for a new [General Submission][] cycle.

![Notification of number of automatically imported votes](../../images/gettingStartedGuideOldWinningVotes.jpg)

You can still import your old voted data that **DO NOT match** the winning value in the last released data. To do this:

1. Go to the **☰ Menu**, look under **My Votes**, and choose **Import Old Votes**.

	<img src="../../images/GettingStartedGuideImportOldVotes.png" alt="Import Old Votes option in menu" width="300">

2. You can choose to limit your import to specific items.

	![Import selected items option](../../images/gettingStartedGuideImportSelectedItems.jpeg)

3. Scroll to the bottom to see the category selection for bulk import.
4. Select the categories that you want to import and click the **Import selected items** button at the bottom.
5. Go to the data categories in the Survey Tool where you have imported your old votes, these will show up in the Others column with no votes.
6. Review and add your vote. The best practice is to create a forum entry explaining why this data should be changed and drive to gain consensus with other vetters.

---

## Picking Locales

<img src="../../images/Spanish_default_locale%20example.png" alt="Example of parent language (Spanish) with the default region Spain and other child locales" style="float: left; width: 300px; margin-right: 20px;">

In the left navigation bar, you will see the CLDR locale(s) for which you have permission marked with a _**pencil icon**_.
You can view others locales but not submit contributions.
Selecting the "Show read-only" option beneath the search bar will allow you to see the locales you don't have permissions for.
Sometimes it can be helpful to see how other locales have decided to handle a data item if your locale doesn't yet have a commonly accepted way of writing it.

Each language is followed by a list of regions that represent specific locales. The locale that is grayed out and preceded by an × is the default locale.
The others are considered “sub-locales”. If you are working on the default locale, select the language name instead of the region name.
For example, if you work on Spanish in general (default = Spain),
you will see that that Spain is grayed out in the list below: choosing Spanish means that you are working on the default (Spanish for Spain).

If you are working on a specific variant language (or "sub-locale"), you will need to pick a non-default region.
For example, if you are vetting Mexican Spanish, pick **Mexico**. (You should see a pencil icon before Mexico instead of Spanish in this situation.)

<br clear="left" />

---

## Check your coverage level

<img src="../../images/gettingStartedGuideCoverageDropDown.png" alt="Coverage dropdown menu" style="float: left; width: 300px; margin-right: 20px;">

After you've selected your locale but before you start contributing,
it is always good to check your [coverage level][] to make sure it matches the level you are planning to contribute to.
Which items are shown to you for review in the [Dashboard][], along with [how your progress is tracked](#progress-widget) is based on your target coverage level.
To check you coverage level click on the drop down to the right of the ☰ menu button.
The coverage level which is followed by "(Default)" is your current coverage level.
For example the coverage level in this screenshot is [Modern][]
If your default coverage level is not correct you can follow [Reporting Survey Tool issues][] to have it fixed.

<br clear="left" />

---

## Voting view

Once you have selected your locale, more options show up in the left left navigation bar.
It is usually hidden unless you click on the **\>** icon to access it.
Select a section, and you'll see a table to enter votes which contains seven columns with key information about that item.

| Column name | Description |
| :---------: | :---------- |
| **Code**    | The code CLDR uses to identify this data point. You can search for codes using the [Survey Tool Search functionality][]. |
| **English** | The English value of the data point (the text you are to localize). A 👁️‍🗨️ icon will be visible if there are any open posts, while a 💬 shows that there are closed forum posts for that item. Hovering over a **ⓘ** icon will display a tooltip with additional information, a **ⓔ** icon will shows a tooltips with an example. |
| **Abstain** | The default vote value until you vote on an item. You can also use this to retract a vote, or when you are unsure of your vote. |
| **A**       | The value’s current draft status. We use emoji for these icons so that you can use Find on Page in your browser to look for them. The full list of [draft status symbols][] is in a table at the end of this section. |
| **Winning** | This is the currently winning value. If the Survey Tool would close now, this is the value we would publish. If the value has a blue star next to it, that means it’s also the value that was published in the most recent CLDR release. |
| **Add**    | If the winning value is not correct and is not listed under Others, then use the plus button to enter the correct value. If you enter a new value, your vote will be applied to it automatically. |
| **Others** | Other suggested values, not currently winning, but available to vote for. |

![Image of the 7 columns in the voting view](../../images/gettingStartedGuideArabic.png)

<br>

More information that can't easily be visualized in the table can be seen in the [Info Panel][] which is described later in this guide. _If the [Info Panel][] was turned off, click on the **Open Info Panel** button near the top right of the window._

![Button to open Info Panel if the Info Panel is currently closed](../../images/gettingStartedGuideCookies.png)

### How to vote

You can vote by either clicking on the radio button for an exisiting value or clicking on the + icon to submit a new value and vote for it.
There are buttons you can click to copy over the English value or the currently winning value, so that you can have that as the base of your edit.
The [Special Characters][] section below explains how to edit values if you need to add bidirectional markers or a specific type of white space such as a no-break thin space (NBTSP).

The winning status changes in real-time as vetters vote on items.
Depending on vote requirements and existing votes for that value, your vote may move your desired value to the winning column right away.
Normally it takes at least two votes from two different organizations to change value: in some locales the bar is lower, and for some items it is higher.
It is important to vote to confirm the best item, even if it already appears in the Winning column.

* An approved value makes it clear to anyone using CLDR data — and to the CLDR-TC — that the value is correct for your locale.
* An approved value is less likely to be accidentally overturned: Suppose that 3 organizations agree that a value is right, but don't vote for it, leaving the winning value with a lower status. Later a fourth organization votes for a suboptimal alternative, which then wins, because the winning value was not approved.

If your locale is relatively new and you are working on populating the [Basic][] data items,
start working on the **Core Data** section and go through the rest of the sections one by one.
If the locale is mostly complete, then go to the [Dashboard][] section below for a guide on how to navigate the priority items for your locale in the current cycle.

### Icons

The main panel uses icons to indicate important information and possible problems. After clicking on the **Code** cell, look to the right-side info panel for more details.

![List of icons used in the voting view](../../images/gettingStartedGuideIcon.png)

(Icons for the **Dashboard** are listed in the [Dashboard Icons][] section below.)

### Special Characters

Some items may use special characters, especially invisible characters. 

These will be shown underneath the regular value in an additional box. For example:

<img width="520" height="169" alt="Text input with Show Hidden" src="https://github.com/user-attachments/assets/9a112871-1050-4073-81b8-a3581b3d5000" />

This indicates that there is a special character that was not visible in the upper line, in this case a no-break thin space, U+202F.

When you are adding a value, you can see any "Hidden" characters, and insert additional ones.
These include characters that are completely invisible, as well as variants of spaces.
For example, in the image below, someone is in the middle of adding a new item. 
There is a new "eye" icon in the bottom left that the user can toggle to see hidden characters, and they have turned it on. 
That opens a new (uneditable) box below the text entry, where they can see the NBTSP (non-breaking thin space) variant of the space character in a 'chit'.

They realize that they need to insert a hidden character, 
so they pull down the insert-character menu from the "insert-character" icon in the top left.
That lets them insert a character at the current insertion point in the text.

<img width="583" height="292" alt="Screenshot 2026-04-21 at 16 32 05" src="https://github.com/user-attachments/assets/8c77a348-6330-4f5e-a95a-913d65230776" />

Hovering over the items in that menu shows details about their usage, as you see in the image
— so it can also be used to decode the meaning of the abbreviations used in the chits.

NOTE: For alphabetic information (such as exemplar characters), an older mechanism is still in place.
We hope to update it during the submission phase. More details on how to [**input** these from the keyboard][],
and for a key to **all** the escapes, see [Key to Escapes][] for a list with some of the escape characters.

### Draft status symbols

| Symbol | Status | Description |
|:------:| ----------- | ----------- |
| ✅ | Approved | Enough votes for use in implementantations. "Approved" is the highest draft status for values. |
| ☑️ | Contributed | The value has been confirmed but not enough people have voted on it to qualify as "approved". |
| ✖️ | Provisional | The value has "Provisional" draft status. Most implementations will not use this data. |
| ❌ | Unconfirmed | The value has not been confirmed at any level. Most implementations will not use this data. |
 ⬆️ | Inherited | Used in combination with ✖️ and ❌ icons. It indicates that the value will be inherited (unless it gets stronger approval). Most likely to appear on provisional and unconfirmed items. A ✖️ or ❌  is not necessarily bad for sub-locales. If the parent locale has a good value, the sub-locale will inherit it. Check the **Winning** column. |
 |  🕳️ | Missing | The value is completely missing in your locale. |

---

## Info Panel

<img src="../../images/gettingStartedGuideInfoPanel.png" alt="Example of what the Info Panel looks like" style="float: left; width: 400px; margin-right: 20px;">

1. If you hover over an item (including the English item), a tooltip will appear showing a sample value and usage of the item in context. The item itself will have a white background; other text in context will have a gray background.
2.  When you select an item (the text, **NOT** the radio button), additional information will show in the right-hand **Info Panel**. See screenshot below.
3. Make sure you use a wide-screen monitor and enlarge your window until you can see it, something like the image below.
4. The box at the top shows information about the code you are translating. _It also has a link that you should click on the first type you encounter that kind of item that will explain any "gotchas"._
5. If there is an error or warning for the item, you see that in the middle.
6. Below that, you'll see an example. This is the same as you get by hovering over the item in the center section.
7. If there are votes, you'll see a breakdown of them; you also see the number of votes required to change the value. Unicode TC organization members usually have 6 votes; members from other [CLDR organizations][] usually have 4 votes; and _vetters who are not representing a Unicode organization usually have 1 vote_. Your vote value and the vote value required for change will show on the right navigation for the selected item as shown in this screenshot (4 and 8 respectively in this case).
8. Near the bottom, you'll see a pulldown menu that shows the values for different regional Variants. Here, you can quickly compare the values and go to different sub-locales to correct inconsistencies.
9. You'll also see the New Forum Post button (as shown in the screenshot _to the right_). This is the easiest way to post discussions for the selected item. Remember that the Forum posts are at language level and not at sub-locale level. For more information, see [Forum][].
10. **No Info Panel?**
    - If you click on the **Code** cell and there is no **Info Panel**, you may have turned it off accidentally. Click on the **Open Info Panel** button near the top right of the window.

<br clear="left" />

---

## Inheritance

There are three different ways a value can be inherited. You have the option of voting for an inherited value or entering a different one.
For items which are inherited either from a different place in the locale, such as date formats,
or from a parent locale you will see a "Jump to Original ⇒" option in the [Info Panel][] which will take you to the place the value is being inherited from.

Inherited values are also highlighted with different colors to make it easier to know where an item is being inherited from.

| Color        | Meaning |
| ------------ |-------- |
| Red          | The value is inherited from Root. Only certain types of data can be inherited from Root. |
| Light purple | The value is inherited from somewhere else in the same locale. A common place to see this is in Date & Time |
| Blue         | The value is inherited from a parent locale. See [Regional Inheritance][] for more details. |

![Image of the inheritance colors](../../images/gettingStartedGuideColorCode.png)

> Note: Voting on inheritance means "always use the inherited value, even if the value changes in the future”.
> An inheritance vote is recommended if there are no differences in spelling conventions and political relations between your locale and the parent locale.
> Voting for inheritance minimizes duplication of data.

### Regional Variants (also known as Sub-locales)

Language variants by Region are differentiated as Parent-locale and sub-locales. For example:

-   **Spanish es** is the parent (or the default) locale for all Spanish locales. Its default content is Spanish (Spain) es\_ES.

-   **Spanish (Latin America) es\_419** is one of the sub-locales for Spanish. Votes on inheritance will ensure that it will only contain content that is different than what is in Spanish.

-   **Spanish (Argentina) es\_AR** is one of the sub-locales for Spanish (Latin America).Votes on inheritance will ensure that it will only contain content that is different than what is in Spanish (Latin America)

<!--
The regional variants menu for a date value is shown on the [Info Panel][]. It will look something like the following (the exact appearance depends on the browser).

Click on the "Check Regional Variants..." button, it will show which regional variants have different values than the main language locale. The current locale will show as darker gray as shown in the screenshot on the right.

![Regional variant dropdown option](../../images/gettingStartedGuideICUSyntax.png)

![Dropdown of regional variants for Spanish](../../images/gettingStartedGuideVarientDropdown.png)

In the example in the screenshot above, Switzerland and Liechtenstein have different regional data than German (=Germany). Hover over the menu item to see the value it has. In the above diagram, you'll see "." as the decimal separator for Switzerland in the yellow hover box above as shown in the screenshot above.

If any values are incorrect, please correct them. You can do that by selecting the menu item, which will take you to the same item, but in the regional locale. For example, selecting “Belgium” above goes to the decimal separator for _German (Belgium)_. You can then correct the item.

Normally, the only values that should be different are in date and number formats. In some locales there are other variations: for example, German (Switzerland) spells words with “ss” instead of “ß”, so differences can occur on many different pages.

-->

### Regional Inheritance

If you are voting in a sub-locale such as en\_AU, es\_MX, fr\_CA etc., you can vote to use the translation from the locale you inherit from.
You can do this by voting for the translation highlighted in blue box.

An inheritance vote is useful if there are no differences in spelling conventions and political relations between your locale and the parent locale.
Abstaining from voting may appear to have the same effect, but if another vetter votes for something different, their vote may cause the winning vote to change.
By voting for the blue inheritance value you make your opinion known to other vetters and the CLDR TC.

Whenever you vote for inheritance, you are saying "**always use the inherited value, even if it changes in the parent value**".
In the screenshot below, this is the case for the vote for "inglês". If the parent value eventually changes, the value in your locale will also change.
You can vote for a different value in your locale if the value in the parent locale later changes to be something that won't work in your locale.
You can find the list of items where the parent inheritance has changed by checking the Dashboard [Inherited Changed category][].
New items may appear in the [Inherited Changed category][] throughout the [General Submission][] and [Vetting][] phases as vetters in the parent locale vote for items.

- Inheritance is important, it helps reduce CLDR's data size, making a smaller footprint for any implementation.
- Inheritance is not only limited to “sub locales”. Parent locales (or default language locales) also have inheritance from root or other fields.
- By default, all data are inherited if there are no contributions. The data are indicated as Missing or Abstain. Sub-locales have inherited values that are generally from the parent locale (e.g. de\_CH will inherit values from de\_DE).
- The inherited values appear in the **Others** column highlighted in blue box (e.g. “embu” and "inglês"). By clicking the radio button in front of those values, you are voting for inheritance.
- If the inherited value is not correct for your locale or it’s likely for your locale to change the data in the future, click the + button, and enter a new suggestion.

---

<a id='progress-widget'>
## Tracking your progress

There is a progress widget that shows your voting progress on the page in the upper right corner of the Survey Tool next to the Info Panel toggle.
You can see details of your progress when you hover over the widget, including what progress is being measured,
and the total number of items remaining for you to vote on in that category.
Your progress is measured based on the coverage level you have set, so make sure your coverage level is set correctly at the start of the new cycle.

> [!Note:] The progress widget is currently only visible when you are signed in and the dashboard is open.

### Page progress

_Progress bar shows progress of items on page for your coverage level._

<img src="../../images/gettingStartedGuidePageProgress.png" alt="Page progress widget tooltip" width="300">

### Overall progress

_Progress bar shows progress of items overall for your coverage level._

<img src="../../images/gettingStartedGuideOverallProgress.png" alt="Overall progress widget tooltip" width="300">

---

## Dashboard

The Dashboard will show you a list of data items with warnings of different kinds. Some will require action, some may be false positives.
At the top of the Dashboard is a header with a button for each section title (such as **Missing**) and the number of items. Below that header are a series of rows.
The goal is that you should work the Dashboard down to show zero items, then review the [Reports][] (below).

![Dashboard option in left navigation bar](../../images/gettingStartedGuideDashboard.png)

### Dashboard Summary

There are two ways to clear items from the **Dashboard** list:

1. Fix them (such as adding a translation for a missing item).
2. Hide them (such as for an invalid warning or when the English has changed and you've verified that the current translation is still correct).

- _**Only**_ hide items if they really are false positives, **not** because you gave up on fixing them…
- If you hide an item by mistake:
    - Unhide all the lines with the top eye button.
    - Click on the orange eye button in the line (a “Show" tooltip will appear).
    - Hide all the lines again by clicking the top eye button.

_The first priority is to fix all the_ _**Missing**_ _items by supplying the correct translations._

### Dashboard Columns

There are six columns in the Dashboard view that provide a summary of key information about that item.

| Column | Description|
|------- |----------- |
| Dashboard category | The first letter of the section name enclosed in a circle. |
| Data Type | The section that the item belongs to. |
| Code | This is a link to the field in the Survey Tool. |
| English | The English value is highlighted in blue. |
| Winning _**XX**_ |The currently winning value is highlighted in green. |
| Hide checkbox | For items that can be hidden a checkbox to hide that option appears on the far right. |

![example of the columns for a specific item in the dashboard](../../images/gettingStartedGuideDashboardCols.png)

### How to handle different categories

Following are guidelines on best practices for handling items under each category in Dashboard:

| Category | Priority | Description |
| :------: | :------: | :---------- |
| Error    | High   | The currently winning value has caused an error in validation that will have to be resolved before release. Fix the error. In some cases there is a conflict between items (such as two different items having identical translations) and you'll need to fix the conflicting item, not the one showing the error. |
| Missing  | High   | These are items where there is no localization provided by any contributor. Click on the line to be taken to the item in the Survey Tool where items are highlighted and you can add a translation. When you fix a **Missing** item it will turn to **Changed**. |
| Losing   | Medium | These are items that you have already voted on, but where your vote is not for the currently winning value. If you can live with the currently winning value — even if you don't think it is optimal, but reasonable — change your vote to be for the winning item. Otherwise, click the **Forum** button in the **Info Panel** and give reasons for why your fellow vetters should change their vote. If others change their vote, the value may still be approved before the end of the cycle. **Engage with others on the Forum discussions**. Make sure to post good and verifiable arguments as to why others should change their votes and **respond to others’ posts**. Review all of the items to see if someone else’s item is better and read the forum, and whether you want to change your vote. Discuss in the forum, then use the Hide button to hide disputes you’ve addressed in the forum. |
| Changed  | Medium | Items show up under **Changed** because the Winning translation has been changed, possibly due to a Missing item being resolved or just that the winning value changed.	|
| English Changed | High | These are items where the English source has changed *after* the last change in translation.	Items that are listed in this section indicates that you need to re-check them and assess the impact to your language and update as appropriate. (Sometimes English changes will have no impact to translations, such as changes in capitialization.) |
| Warnings | Medium | These are issues which appear after automatic checks. For example, a message could be "_The value is the same as English_", which is a quite common warning for languages that are close to English in the spelling of languages or territories. If the value is actually valid, then click on the Hide button (crossed eye). If not, then vote for a fix, or post on the Forum for discussion. Any warnings you've hidden will remain hidden during an future CLDR cycles you participate in. |
| Inherited Changed | High | The translated value is inherited from another value, and that value has changed. Ensure that the value is still suitable for your locale.			|
| Abstained | High  | These are values where you and the org you represent have not provided any vote at all. It is best practice to have no abstained values for your language and coverage level to ensure high confidence in our release. If you voted for the item in the previous cycle and the same value is still winning but your previous vote didn't import, please submit a vote and also go to [Reporting Survey Tool issues][]. |

![Example of a missing item in the dashboard](../../images/gettingStartedGuideMissing.png)

![example of a warning in the dashboard for another locale](../../images/gettingStartedGuideWarning.png)

---

## Searching in the Survey Tool

The ability to search in the Survey Tool has been added in [CLDR-18423][] and supports searching for: values, English value, and for the codes.
In the Dashboard header, each notification category (such as "Missing" or "Abstained") has a checkbox determining whether it is shown or hidden.
The symbols in the A column have been changed to be searchable in browsers (with *Find in Page*) and stand out more on the page. See below for a table. They override the symbols in [Survey Tool Guide: Icons](translation/getting-started/guide#icons).

## Reports

The reports show lists of items that need to be reviewed for consistency. Review these last, after you have voted for all the missing and provisional items this cycle, since they will not generate correctly with missing data. Reports can be found in both the dashboard as well as in the left navigation bar between the Dashboard and Forum options.

[Left navigation bar view](../../images/gettingStartedGuideKorean.png)

_Example of a section of the date time report:_

![Example of a section of the date time report](../../images/gettingStartedGuidePatterns.jpeg)

See [Review Reports][] for more details.

---

## Special cases

**You may not make changes to this locale**

You may see a message like the following when trying to modify a regional locale, like pt-BR.

![Brazil is grayed out in left navigation bar example](../../images/gettingStartedGuidePortuguese.png)

→

![Tooltip explanation of why it is grayed out](../../images/gettingStartedGuideBrazil.png)

The reason that Brazil is grayed out is that it is the default content locale for Portuguese. So to modify pt-BR, you need to simply click on Portuguese. If you do click on Brazil, you will get to a page with the following at the top. Click on [default content locale][] for more details.

![warning at on main Survey Tool page when Portuguese (Brazil) is selected](../../images/gettingStartedGuideGeneralInfo.png)

<a id='20-vote-items-may-not-be-modified'>
<a id='change-protected-items'>

### Changing Protected Items

Some items have change-protection in place that will stop the item value from changing without the CLDR Technical Commmittee's approval, but you can still advocate a change.
This is indicated by the message shown below. This warning indicates that the item has particularly broad impact and changes should only be made if absolutely necessary.

![Changing Protected Items warning](../../images/handling-protected-items-warning.png)

![Request for Review](../../images/request-review.png)

After hitting that **Request** button, you'll see the following.

![Request Justification](../../images/request-justification.png)

### Steps to flag a protected item for CLDR TC review:

1. Vote on or Add the item you want.
2. Click on the “**Flag for Review button**”.
3. On the new page, you'll see a message box.
4. Enter the change that you want to make, and add a justification for changing it.
    - Make sure that the justification is clear to people who don't speak your language.
    - Cite sources (web pages) where possible.
5. Then click **“Post**”.
6. Encourage others to support your request by voting for the item you're requesting.
7. The Technical Committee will review the change request and
    - accept it, or
    - ask for more information, or
    - reject it with comments

> [!Notes]
>	* The **Flag for Review** button will be available only when the item is under change-protection *and* there is a vote for an alternative in the **Others** column.
>	* If you change your vote, it removes the flag — so if you want for it still to be flagged, you have to redo the process above.

---

## Forum

Forum is the place to discuss and collaborate with other vetters on questions and issues in your language. The forum is at Language level and not at sub-locale level; if you are talking about a translation in a sub-locale, be sure that you are clear about that.

It's a best practice to **create a Forum post whenever you propose a change to a previously approved value**, and provide an explanation and links to references.

### Forum Etiquette

While creating New Posts on Forum or participating in discussions please follow these general etiquette guidelines for best productive outcomes:

- Be professional. Provide accurate, reasoned answers so that other participants can easily understand what you are talking about.
- Be courteous. Refrain from inappropriate language and derogatory or personal attacks.
- Don’t “SHOUT”; that is don’t use all capitals.
- In case of disagreement, focus on the data and provide evidence to support your position. Avoid challenges that may be interpreted as a personal attack.
- Be prepared to have your own opinions challenged or questioned, but don’t take answers personally.
- It’s possible that participants have different expectations on the intent of the data. Clarify what you think is the intent may help especially if disputes continue.
- Remember that open discussion is an important part of the process; abiding by these guidelines will encourage active participation by all vetters and a better end result.

### Forum post workflow

1. Create a new **Request**
2. Responses by other vetters in your language with Agree, Decline, or Comment.
3. Once resolved, the creator of the the initial Request closes the post.

> Note: Any posts where the requested value becomes the winning value by the end of the CLDR cycle will be automatically closed by the CLDR TC.

#### How to create a new forum post

A forum post can be specific to a particular data point or a general issue. In either case, create a new forum post to an item.

* A post that is specific to a particular data point.
* A general issue that impacts multiple data points. In a general case that impacts multiple data points, you do not need to post new forum posts for every item impacted. The general issue should be flagged to other vetters and once a consensus is reached, it is expected that vetters update their votes on all impacted items. New forum posts can be used to flag to other vetters if others fail to update their votes on all impacted items. ONLY request if others have missed or have not updated consistently.

##### Create forum posts from the [Info Panel][] in the voting window

1. Vote on an item (or add new +) for the item you want to suggest changing.

![Add a new item and vote for it](../../images/gettingStartedGuideVote.png)

2. In the Info Panel on the right, there are two buttons to indicate the type of forum posts:
    1. **Request** You have voted on a non-winning item, and you want to Request others to change their votes.
    2. **Discuss** - Currently only TC members can make discuss posts.
3. Click **Request** button and fill out the details of your request. (Note: The **Request** button is disabled unless you have voted)

![Request button](../../images/gettingStartedGuideRequest.png)

**Request**

1. A precomposed text is included to help start your post. **Important!: Request works WITH the item you voted for currently.**
2. _Please consider voting for “{your voted-value}”. My reasons are:_ Complete the text by filling out your reasons and links to references.

![Add your reasons to changing the value to the request post draft](../../images/gettingStartedGuideReasons.png)

3. Then **Submit.**

### Responding to Request posts

There are two ways to respond to forum posts:
- Info Panel (This is the recommended option so that you can see all the context for that item.)
- In the Forum view (See [Working in the Forum view][])

**Respond from the [Info Panel][] in the voting window.**

In the **Info Panel**, select the **Comment** button
- Each posted response is labeled in Red and its response type: Agree, Decline or Comment.

![Forum Post example from the Info Hub](../../images/gettingStartedGuideAgree.png)

**Choosing your Response:** Vote, or Comment

1. When you make a vote on an item that already has a Request post by another vetter, then an Agree or Decline post will be made for you automatically. If you agree with the reasons for change, you aren't required to add explanations for agreeing unless you believe it is important to add additional explanation for linguists or the CLDR TC so that the decision during this cycle is not accidentally overturned in a future cycle due to missing information about the original change.

2. An **Automated Agree post** will be posted if you vote for the requested value with a precomposed reply: _(Auto-generated:) I voted for “{requested-value}”_

3. If you later change your vote to something other than the requested value an automatic response will be be posted with the precomposed reply: _(Auto-generated:) I changed my vote to “{requested-value}”, which now disagrees with the request.”_

	![Example of an automatically generated response by changing votes](../../images/gettingStartedGuideAutoAgree.png)

4. **Comment:** Use this option if you do not Agree, and you have other input to bring to the discussion or if you want to ask for more information.

![View of comment button on a forum post](../../images/gettingStartedGuideComment.png)

### Responding to Discuss posts

Currently only the CLDR TC can post Discuss posts. They will post a Discuss post whenever feedback is needed from the linguists for a specific locale. Feedback is needed in two cases:

1. Resolving errors during the [Vetting][] and [Resolution][] phases
2. Resolving issues reported in the public CLDR Issue tracker in [JIRA][]

#### Forum posts for CLDR JIRA ticket feedback

CLDR users can send in data feedback using [JIRA][]. Tickets filed in JIRA will be processed as described below. Please expect to see posts by CLDR Technical Committee members (TCs) in Forums, and participate by providing your response to any tickets needing your input.

The goal is to bring it to the attention to all linguists contributing in a particular language, and gather their input, so an informed decision can be made and/or suggested.

1. CLDR TC members accept JIRA tickets.
2. For each ticket assigned to them, the TC member will post a forum topic in each language mentioned in the ticket, asking for vetters to look at the issue and either make the requested change, or explain in a forum post why changes should not be made.
3. A reason for not changing could be for example that it is a reasonable change, but doesn't exceed the 'stability' bar in the translation guidelines.
4. TC members will monitor the forum discussion/change during the Submission phase, and will close the JIRA ticket after the forum discussion is concluded.

### Working in the Forum view

In the Survey Tool [Forum view][], there are multiple filtering options available for you to work more effectively.

1. The Forum view can be accessed from the left navigation **Forum.**

![Forum option in the left Navigation bar](../../images/gettingStartedGuideForum.jpeg)

2. By default, the filter is for **Needing action**.

![Filter options drop down](../../images/gettingStartedGuideNeedingAction.png)

3. Filter options:
    - **Needing action**: Forum posts included in this filter are Requests and Discussion posts by someone in your language. You have not yet taken action on either agreed or declined or posted a discussion asking for additional information. For Discussion posts, these are where you are not the last poster.
    - **Open requests by you**: Forum posts included in this filter are Requests that you have posted that you have not closed yet.
    - **All Open topics**: All posts that are open. This includes both Request and Discussions that have not closed yet.
    - **All topics:** All topics, open or closed, including forum posts from previous releases.

#### Respond to forum posts in the Forum view

In the Forum view, you can respond to Request post by voting for the requested item. This will trigger an automatic post with the text: "(Auto-generated:) I voted for “REQUESTED ITEM”" or by clicking on the "Comment" button to explain why you disagree with the proposal.

See [Responding to Request posts in Info Panel][].

![Example forum post](../../images/gettingStartedGuideForumPosts.png)

### Forum email notifications

1. Another way to check for posts that may need your attention is to review email notifications to the e-mail account for your locale. You can delete these notifications if they are for changes initiated by you. You can open the post directly from a link in the email.
2. When you make a forum entry, it will be emailed to all other linguists working on locales with the same language, parent or sub-locale (i.e. **forum is at Language level and not at sub-locale level**). If you are talking about a translation in a sub-locale, be sure that you are clear about that.

## Reporting Survey Tool issues

If you run into a problem with the Survey Tool functionalities or if the documentation doesn't match the current Survey Tool experience, please see [FAQ & Known Bugs][] to see whether it has already been reported (and whether there is a work-around).

If there is a PM (Project Manager) managing contributions for your organization, please report the issue to your PM. To get support for DDL locales check the [DDL: Help Center][] for instructions.

---

## Advanced Survey Tool Features

1. Users familiar with CLDR XML format can upload votes (and submissions) for multiple items at once. See [Bulk Data Upload][].
2. Organization managers can manage users for their organization (add, remove, send passwords, set locales, etc.). For more information, see [Managing Users][].
    1. Some users may want to reset their Coverage Level, with the menu that looks like the image to the right.
    1. The Coverage Level determines the items that you will see for translation: the minimal level has the highest priority items. You normally start with the level marked "Default" (which will vary by your organization and locale). Each successively higher level adds more items, at successively lower priorities. You will not normally go beyond "[Modern][]", unless you have special instructions for your organization.

> Note: Some companies won't use the data for UI localization until it is complete at a certain [coverage level][], typically _**[Modern][]**_.
  
[Basic]: /index/cldr-spec/coverage-levels#basic-data
[Bulk Data Upload]: /index/survey-tool/bulk-data-upload
[CLDR Organizations]: /index/survey-tool/cldr-organization
[coverage level]: /index/cldr-spec/coverage-levels
[Dashboard]: /translation/getting-started/guide#dashboard
[Dashboard Icons]: /translation/getting-started/guide#dashboard-icons
[DDL: Help Center]: /translation/ddl#support
[default content locale]: /translation/translation-guide-general/default-content
[draft status]: https://www.unicode.org/reports/tr35/#attribute-draft
[draft status symbols]: /translation/getting-started/guide#draft-status-symbols
[FAQ & Known Bugs]: /translation#known-issues
[file a ticket]: /requesting_changes#how-to-file-a-ticket
[Forum]: /translation/getting-started/guide#forum
[Forum view]: https://st.unicode.org/cldr-apps/v#forum/USER//
[General Submission]: /translation/getting-started/survey-tool-phases#survey-tool-phase-general-submission
[Info Panel]: /translation/getting-started/guide#info-panel
[Inherited Changed category]: /translation/getting-started/guide#how-to-handle-different-categories
[**input** these from the keyboard]: /translation/core-data/exemplars#input
[JIRA]: /requesting_changes
[Key to Escapes]: /translation/core-data/exemplars#key-to-escapes
[Managing Users]: /index/survey-tool/managing-users
[Modern]: /index/cldr-spec/coverage-levels#modern-data
[Regional Inheritance]: /translation/getting-started/guide#regional-inheritance
[Reports]: /translation/getting-started/guide#reports
[Reporting Survey Tool issues]: /translation/getting-started/guide#reporting-survey-tool-issues
[Responding to Request posts in Info Panel]: /translation#responding-to-request-posts
[Resolution]: /translation/getting-started/survey-tool-phases#resolution-closed-to-vetters
[Review Reports]: /translation/getting-started/review-formats
[Special Characters]: /translation/getting-started/guide#special-characters
[Survey Tool Search functionality]: /translation/getting-started/guide#searching-in-the-survey-tool
[Vetting]: /translation/getting-started/survey-tool-phases#survey-tool-phase-vetting
[Working in the Forum view]: /translation/getting-started/guide#working-in-the-forum-view
