---
title: CLDR Process
---

# CLDR Process

## Introduction

This document describes the Unicode CLDR Technical Committee's process for data collection, resolution, public feedback and release.

- The process is designed to be light-weight; in particular, the meetings are frequent, short, and informal. Most of the work is by email or phone, with a database recording requested changes (See [change request](http://cldr.unicode.org/index/bug-reports)).    
- When gathering data for a region and language, it is important to have multiple sources for that data to produce the most commonly used data. The initial versions of the data were based on best available sources, and updates with new and improvements are released twice a year with work by contributors inside and outside of the Unicode Consortium.    
- It is important to note that CLDR is a Repository, not a Registration. That is, contributors should NOT expect that their suggestions will simply be adopted into the repository; instead, it will be vetted by other contributors.    
- The [CLDR Survey Tool](http://www.unicode.org/cldr/survey_tool.html) is the main channel for collecting data, and bug/feature request are tracked in a database ([CLDR Bug Reports](http://www.unicode.org/cldr/filing_bug_reports.html)).    
- The final approval of the release of any version of CLDR is up to the decision of the CLDR Technical Committee.

## Formal Technical Committee Procedures

For more information on the formal procedures for the Unicode CLDR Technical Committee, see the [Technical Committee Procedures for the Unicode Consortium](http://www.unicode.org/consortium/tc-procedures.html).

## Specification Changes

The [UTS #35: Locale Data Markup Language (LDML)](http://www.unicode.org/reports/tr35/) specification are kept up to date with each release with change/added structure for new data types or other features.

- Requests for changes are entered in the bug/feature request database ([CLDR Bug Reports](http://www.unicode.org/cldr/filing_bug_reports.html)).    
- Structural changes are always backwards-compatible. That is, previous files will continue to work. Deprecated elements remain, although their usage is strongly discouraged.    
- There is a standing policy for structural changes that require non-trivial code for proper implementation, such as time zone fallback or alias mechanisms. These require design discussions in the Technical Committee that demonstrates correct function according to the proposed specification.

## Data- Submission and Vetting

The contributors of locale data are expected to be language speakers residing in the country/region. In particular, national standards organizations are encouraged to be involved in the data vetting process.

There are two types of data in the repository:

- **Core data** (See [Core data for new locales](http://cldr.unicode.org/index/cldr-spec/minimaldata)): The content is collected from language experts typically with a CLDR Technical Committee member involvement, and is reviewed by the committee. This is required for a new language to be added in CLDR. See also [Exemplar Character Sources](http://www.unicode.org/cldr/filing_bug_reports.html#Exemplar_Characters).    
- **Common locale data**: This is the bulk of the CLDR data and data collection occurs twice a year using the Survey tool. (See [How to Contribute](http://cldr.unicode.org/#TOC-How-to-Contribute-).)
    

The following 4 states are used to differentiate the data contribution levels. The initial data contributions are normally marked as draft; this may be changed once the data is vetted.

- Level 1: **unconfirmed**    
- Level 2: **provisional**    
- Level 3: **contributed (= minimally approved)**    
- Level 4: **approved** (equivalent to an absent draft attribute)

Implementations may choose the level at which they wish to accept data. They may choose to accept even **unconfirmed** data if having some data is better than no data for their purpose. Approved data are vetted by language speakers; however, this does not mean that the data is guaranteed to be error-free -- this is simply the best judgment of the vetters and the committee according to the process.

### Survey Tool User Levels

There are multiple levels of access and control:

| **Vetter Level** | **Number of Votes** | **Description** |  |
|---|---|---|:---:|
| *TC Member* | 50 / 6 or 4 | - Manage users in their organization <br>- Can vet and submit data for all locales (However, their vetting work is only done to correct issues.) <br>- Can see the email addresses for all vetters in their organization <br>- Only uses a 50 vote for items agreed to by the CLDR technical Committee <br>- TC members may have a 6 or 4 regular vote depending on how actively their organization participates in the TC |  |
| *TC Organization Managers* | 6 | - Manage users in their organization <br>- Can vet and submit data for all locales (However, their vetting work is only done to correct issues.) <br>- Can see the email addresses for all vetters in their organization |  |
| *Organization Managers* | 4 | -Manage users in their organization <br>- Can vet and submit data for all locales (However, their vetting work is only done to correct issues.) <br>- Can see the email addresses for all vetters in their organization |  |
| *TC Organization  Vetter* | 6 | - Can vet and submit data for a particular set of locales. <br>- Can see the email addresses for submitted data in their locales. <br>- Cannot manage other users. |  |
| *Organization Vetter* | 4 | - Can vet and submit data for a particular set of locales <br>- Can see the email addresses for submitted data in their locales. <br>- Cannot manage other users. |  |
| *Guest Vetter* | 1 | - Can vet and submit data for a particular set of locales <br>- Cannot see email addresses. <br>- Cannot manage other users.  |  |
| *Locked Vetter* | 0 | - If a user is locked or removed, then their vote is considered a zero weight. |  |

These levels are decided by the technical committee and the TC representative for the respective organizations.

- Unicode TC members (full/institutional/supporting) can assign its users to Regular or Guest level, and with approval of the TC, users at the Expert level.    
- TC Organizations that are fully engaged in the CLDR Technical Committee are given a higher vote level of 6 votes to reflect their level of expertise and coordination in the working of CLDR and the survey tool as compared to the normal organization vote level of 4 votes    
- Liaison or associate members can assign to Guest, or to other levels with approval of the TC.    
    - The liaison/associate member him/herself gets TC status in order to manage users, but gets a Guest status in terms of voting, unless the committee approves a higher level.        
- Users assigned to "[unicode.org](http://unicode.org/)" are normally assigned as Guest, but the committee can assign a different level.
    
### Voting Process

- Each user gets a vote on each value, but the strength of the vote varies according to the user level (see table above).    
- For each value, each organization gets a vote based on the maximum (not cumulative) strength of the votes of its users who voted on that item.    
- For example, if an organization has 10 Vetters for one locale, if the highest user level who voted has user level of 4 votes, then the vote count attributed to the organization as a whole is 4 for that item.

### Optimal Field Value

For each release, there is one optimal field value determined by the following:

- Add up the votes for each value from each organization.    
- Sort the possible alternative values for a given field    
    - by the most votes (descending)       
    - then by UCA order of the values (ascending)        
- The first value is the optimal value (**O**).    
- The second value (if any) is the next best value (**N**).

### Draft Status of Optimal Field Value

1. Let **O** be the optimal value's vote, **N** be the vote of the next best value (or zero if there is none), and G be the number of organizations that voted for the optimal value. Let **oldStatus** be the draft status of the previously released value.
        
2. Assign the draft status according to the first of the conditions below that applies:

| **Resulting Draft Status** | **Condition** |
|---|---|
| *approved* | - O &gt; N and O ≥ 8, for *established* locales* <br>- O &gt; N and O ≥ 4, for other locales |
| *contributed* | - O &gt; N and O ≥ 4 and oldstatus &lt; contributed <br>- O &gt; N and O ≥ 2 and G ≥ 2 |
| *provisional* | O ≥ N and O  ≥  2  |
| *unconfirmed* | *otherwise* |
    

1. *Established* locales are currently found in [coverageLevels.xml](https://github.com/unicode-org/cldr/blob/master/common/supplemental/coverageLevels.xml), with approvalRequirement\[@votes="8"\]        
    - Some specific items have an even higher threshold. See approvalRequirement elements in [coverageLevels.xml](http://unicode.org/repos/cldr/trunk/common/supplemental/coverageLevels.xml) for details.
2. If the oldStatus is better than the new draft status, then no change is made. Otherwise, the optimal value and its draft status are made part of the new release.    
    - For example, if the new optimal value does not have the status of **approved**, and the previous release had an **approved** value (one that does not have an error and is not a fallback), then that previously-released value stays **approved** and replaces the optimal value in the following steps.
        
It is difficult to develop a formulation that provides for stability, yet allows people to make needed changes. The CLDR committee welcomes suggestions for tuning this mechanism. Such suggestions can be made by filing a [new ticket](https://cldr.unicode.org/index/bug-reports#TOC-Filing-a-Ticket).

## Data- Resolution

After the contribution of collecting and vetting data, the data needs to be refined free of errors for the release:

- Collisions errors are resolved by retaining one of the values and removing the other(s).    
- The resolution choice is based on the judgment of the committee, typically according to which field is most commonly used.    
    - When an item is removed, an alternate may then become the new optimal value.        
    - All values with errors are removed.
- Non-optimal values are handled as follows    
    - Those with no votes are removed.        
    - Those with votes are marked with *alt=proposed* and given the draft status: **unconfirmed**
        
If a locale does not have minimal data (at least at a provisional level), then it may be excluded from the release. Where this is done, it may be restored to the repository for the next submission cycle.

This process can be fine-tuned by the Technical Committee as needed, to resolve any problems that turn up. A committee decision can also override any of the above process for any specific values.

For more information see the key links in [CLDR Survey Tool](http://www.unicode.org/cldr/survey_tool.html) (especially the Vetting Phase).

**Notes:**
- If data has a formal problem, it can be fixed directly (in CVS) without going through the above process. Examples include:    
    - syntactic problems in pattern, extra trailing spaces, inconsistent decimals, mechanical sweeps to change attributes, translatable characters not quoted in patterns, changing ' (punctuation mark) to curly apostrophe or s-cedilla to s-comma-below, removing disallowed exemplar characters (non-letter, number, mark, uppercase when there is a lowercase).        
    - These are changed in-place, without changing the draft status.        
- Linguistically-sensitive data should always go through the survey tool. Examples include:    
    - names of months, territories, number formats, changing ASCII apostrophe to U+02BC modifier letter apostrophe or U+02BB modifier letter turned comma, or U+02BD modifier letter reversed comma, adding/removing normal exemplar characters.        
- The TC committee can authorize bulk submissions of new data directly (CVS), with all new data marked draft="unconfirmed" (or other status decided by the committee), but only where the data passes the CheckCLDR console tests.    
- The survey tool does not currently handle all CLDR data. For data it doesn't cover, the regular bug system is used to submit new data or ask for revisions of this data. In particular:    
    - Collation, transforms, or text segmentation, which are more complex.        
        - For collation data, see the comparison charts at [http://www.unicode.org/cldr/comparison\_charts.html](http://www.unicode.org/cldr/comparison_charts.html) or the XML data at [http://unicode.org/cldr/data/common/collation/](http://unicode.org/cldr/data/common/collation/)            
        - For transforms, see the XML data at [http://unicode.org/cldr/data/common/transforms/](http://unicode.org/cldr/data/common/transforms/)            
    - Non-linguistic locale data:        
        - XML data: [http://unicode.org/cldr/data/common/supplemental/](http://unicode.org/cldr/data/common/supplemental/)            
        - HTML view: [http://www.unicode.org/cldr/data/diff/supplemental/supplemental.html](http://www.unicode.org/cldr/data/diff/supplemental/supplemental.html)
            

### Prioritization

There may be conflicting common practices or standards for a given country and language. Thus LDML provides keyword variants to reflect the different practices (for example, for German it allows the distinction between PHONEBOOK and DICTIONARY collation.).

When there is an existing national standard for a country that is widely accepted in practice, the goal is to follow that standard as much as possible. Where the common practice in the country deviates from the national standard, or if there are multiple conflicting common practices, or options in conforming to the national standard, or conflicting national standards, multiple variants may be entered into the CLDR, distinguished by keyword variants or variant locale identifiers.

Where a data value is identified as following a particular national standard (or other reference), the goal is to keep that data aligned with that standard. There is, however, no guarantee that data will be tagged with any or all of the national standards that it follows.

### Maintenance Releases

Maintenance releases, such as 26.1, are issued whenever the standard identifiers change (that is, BCP 47 identifiers, Time zone identifiers, or ISO 4217 Currency identifiers). Updates to identifiers will also mean updating the English names for those identifiers.

Corrigenda may also be included in maintenance releases. Maintenance releases may also be issued if there are substantive changes to supplemental data (non-language such as script info, transforms) data or other critical data changes that impact the CLDR data users community.

The structure and DTD may change, but except for additions or for small bug fixes, data will not be changed in a way that would affect the content of resolved data.

[**Data Retention Policy**](/index/process/cldr-data-retention-policy)

## Public Feedback Process

The public can supply formal feedback into CLDR via the [Survey Tool](http://unicode.org/cldr/apps/survey/) or by filing a [Bug Report or Feature Request](http://www.unicode.org/cldr/filing_bug_reports.html). There is also a public forum for questions at [CLDRMailing List](https://www.unicode.org/consortium/distlist.html#cldr_list) (details on archives are found there).

There is also a members-only [CLDRmailing list](https://www.unicode.org/members/index.html#cldr) for members of the CLDR Technical Committee.

[Public Review Issues](http://www.unicode.org/review/) may be posted in cases where broader public feedback is desired on a particular issue.

Be aware that changes and updates to CLDR will only be taken in response to information entered in the [Survey Tool](http://unicode.org/cldr/apps/survey/) or by filing a [Bug Report or Feature Request](http://www.unicode.org/cldr/filing_bug_reports.html). Discussion on public mailing lists is not monitored; no actions will be taken in response to such discussion -- only in response to filed bugs. The process of checking and entering data takes time and effort; so even when bugs/feature requests are accepted, it may take some time before they are in a release of CLDR.

## Data Release Process

### Version Numbering

The locale data is frozen per version. Once a version is released, it is never modified. Any changes, however minor, will mean a newer version of the locale data being released. The version numbering scheme is "xy.z", where z is incremented for maintenance releases, and xy is incremented for regular semi-annual releases as defined by the [regular semi-annual schedule](http://cldr.unicode.org/index#TOC-General-Schedule-)

### Release Schedule

Early releases of a version of the common locale data will be issued as either alpha or beta releases, available for public feedback. The dates for the next scheduled release will be on [CLDR Project](http://www.unicode.org/cldr/index.html).

The schedule milestones are listed below.

| **Milestone** | **JiraPhase** | **Description** |
|---|---|---|
| **Survey Tool Shakedown** |   | Selected survey tool users try out the survey tool and supply feedback. The contributed data will be considered as real data. |
| **Data Submission** | dsub | All survey tool registered u sers can add data and vet (vote for) for data |
| **Data Vetting** | dvet | The survey tool users focus shifts to resolving data differences/disputes, and resolve errors.  |
| **Data Resolution** |   | T he data contribution is closed for general contributors. The Technical Committee will close remaining errors and issues found during the release process . |
| **Alpha and Beta releases** | rc | The release candidates are available for testing. Only showstoppers will be triage and fixed at this point. |
| **Release**  | final | Release completed with referenceable release notes and links.  |

Labels in the **Jira** column correspond to the **phase** field in Jira. Phase field in Jira is used to identify tickets that need to be completed ***before*** the start of each milestone (table above).

## Meetings and Communication

The currently-scheduled meetings are listed on the [Unicode Calendar](http://www.unicode.org/timesens/calendar.html). Meetings are held by phone, every week at 8:00 AM Pacific Time (-08:00 GMT in winter, -07:00 GMT in summer). Additional meeting is scheduled every other Mondays depending on the need and people's availability.

There is an internal email list for the Unicode CLDR Technical Committee, open to Unicode members and invited experts. All national standards bodies who are interested in locale data are also invited to become involved by establishing a [Liaison membership](http://www.unicode.org/consortium/join.html) in the Unicode Consortium, to gain access to this list.

## Officers

The current Technical Committee Officers are:

- Chair: Mark Davis (Google)    
- Vice-Chair: Annemarie Apple (Google)

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)