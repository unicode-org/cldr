---
title: CLDR BRS Post Items
---

# CLDR BRS Post Items

Title was: CLDR BRS Post Items (Section E) for Rick

**Actions for Rick, prior to Alpha**

**A24-1**

1. [http://cldr.unicode.org/index/downloads/cldr-XX](http://cldr.unicode.org/index/downloads/cldr-XX) — In the header, put the SVN Tag link on the Data item On the release page, e.g. http://cldr.unicode.org/index/downloads/cldr-35  
   2. set the string and link for "SVN Tag" field to "release-35-alpha" and a SVN link such as http://www.unicode.org/repos/cldr/tags/release-35-alpha/ and add that link also to the "Data" field.  
   3. [http://cldr.unicode.org/](http://cldr.unicode.org/) — In the "News" section at the top of the page, add a new yellow top row for the alpha (for the beta/release, reuse that row). Remove old releases from the table (major ≤ alpha-2 \- leaving only 2 recent major releases), and de-yellow the remainder except for the top row.  
   4. [http://cldr.unicode.org/index/downloads](http://cldr.unicode.org/index/downloads) — Add a new row (below "Latest" and "Dev") for the new alpha. Copy various links as needed. (It will stay the same for the beta, then change to be the release row).

**A26**

**Updating "news" items on main page. Check for the background color on *row*, not cell. E.g.**
**_NOTE: the following is propbably outdated._***

```<tr style="background-color:rgb(255,255,0)"\>
<td>2021-03-01</td>
<td><a href="[https://sites.google.com/site/cldr/index/downloads/cldr-39](https://sites.google.com/unicode.org/cldr/index/downloads/cldr-39)" target="_blank">CLDR v39</a> Alpha available</td>
</tr>
<tr>
<td>2020-10-28</td\>
<td><a href="[https://sites.google.com/site/cldr/index/downloads/cldr-38](https://sites.google.com/unicode.org/cldr/index/downloads/cldr-38)" target="_blank">CLDR v38.1</a> Released</td>
</tr>
```
**Actions for Rick, gathering participants**

**A26-1**

\* Get contributors for [http://cldr.unicode.org/index/acknowledgments](http://cldr.unicode.org/index/acknowledgments)

\* List of those who contributed through Survey tool: Login as TC or Admin, under "List Survey Tool Users, find 'Manage Users' then click 'Email Address of Users Who Participated' (shows all users, not just your org).

\* Bug reporters and bug committers in Jira. Search in CLDR project for the full reporter list. Include fixversion \= the release you want, status \= done or reviewing. After searching, save as CSV and import into Excel as UTF-8. In Jira, gather unique Reporter list via sorting. Example: [https://unicode-org.atlassian.net/issues/?jql=project%20%3D%20CLDR%20AND%20status%20in%20%28Done%2C%20Reviewing%29%20AND%20fixVersion%20%3D%20%2239%22%20order%20by%20created%20DESC](https://unicode-org.atlassian.net/issues/?jql=project%20%3D%20CLDR%20AND%20status%20in%20%28Done%2C%20Reviewing%29%20AND%20fixVersion%20%3D%20%2239%22%20order%20by%20created%20DESC)

\* To find all Atlassian/Jira users, see this report and fish out addresses for the unique reporters who aren't already in the Acks page, if available: [https://admin.atlassian.com/s/a116048c-e2e1-4d0c-8ec4-ab9cf3ad6cb5/users](https://admin.atlassian.com/s/a116048c-e2e1-4d0c-8ec4-ab9cf3ad6cb5/users) Or try the “Gear” menu in JIRA (top right corner) then the User admin panel, then use "download users".

\* People who submitted pull requests within the release time-frame:

[https://github.com/unicode-org/cldr/graphs/contributors?from=2020-11-01\&to=2021-03-22\&type=c](https://github.com/unicode-org/cldr/graphs/contributors?from=2020-11-01&to=2021-03-22&type=c)

See also: [https://github.com/orgs/unicode-org/people](https://github.com/orgs/unicode-org/people)

But there's apparently no way to find email addresses, and no way to send a broadcast message.

\* Once everything is gathered as in steps above, send a message out to those people who contributed at least one item, (see the [http://cldr.unicode.org/development/cldr-big-red-switch](http://cldr.unicode.org/development/cldr-big-red-switch)).

\* This is NOT a blocker for the release; we can update that page at any time.

\* Note: To import UTF-8 into Excel, make a blank document. Then use Data-\>FromText and import the CSV file. Choose UTF-8 as the encoding and "New worksheet" rather than importing over existing. Email addresses are currently in column C, and there are 1,610 of them as of 2021-03-30.

**Actions for Rick, section "E" of the CLDR Release BRS**

**D10**

Remove the yellow from UTS\#35 draft, and clean up everything for posting. Run W3C validations and link checks on all parts. Make sure links to images are correct before posting, and create a symlink to the image directory within the reports/tr35/tr35NN directory. Make sure that the revision is headed with "updated for" rather than "proposed update for". Double check the text and links for the DTD and versions in the header. Check into SVN. Then post as final UTS \#35 on unicode site. Double-check the DTD and header pointers again when finished.

### **E02**

Creating Artifacts (Example: for **release-40**)

* Data zips:  
  * Automated Way  
    1. go to cldr-staging's workflow, [https://github.com/unicode-org/cldr-staging/actions/workflows/maven.ym](https://github.com/unicode-org/cldr-staging/actions/workflows/maven.ym)l for the corresponding tag (such as release-40)  
    2. Click on the Workflow Run corresponding to the specific tag  
    3. An artifact will appear, **cldr-staging**. Download and unpack this zipfile.  
    4. cldr-staging.zip will contain **core.zip** and **keyboards.zip**  
  * Manual Way:   "**`mvn package`**" within cldr-staging, yields **target/\*.zip**  
* Jarfile  
  * Automated Way  
    1. Go to cldr's  cldr-mvn workflow, [https://github.com/unicode-org/cldr/actions/workflows/maven.yml](https://github.com/unicode-org/cldr/actions/workflows/maven.yml)  
    2. If there isn't a run for the release tag, you can start one with the "run workflow" button.  
    3. Click on the run. There will be an artifact named **cldr-code** which will have a zipfile containing **cldr-code.jar**  
  * Manual way  
    1. **`mvn --file=tools/pom.xml -pl cldr-code package`** , Yields: **tools/cldr-code/target/cldr-code.jar**  
* Rename "core.zip" to "**cldr-common-40.0.zip**", and rename "keyboards.zip" to "**cldr-keyboards-40.0.zip**"  
   Create symlinks from core.zip to cldr-common-40.0.zip and from keyboards.zip to cldr-keyboards-40.0.zip  
* Rename "cldr-code.jar" to "cldr-tools-40.0.0.jar"  
* Create the hashes directory:  
  * **`mkdir hashes ; shasum -a 512 *40.0* | tee hashes/SHASUM512.txt`**  
* Create the GPG hashes and move them to the subdirectory  
  * **`gpg --detach-sign cldr-common-40.0.zip ; gpg --detach-sign cldr-keyboards-40.0.zip ; gpg --detach-sign cldr-tools-40.0.jar`**   
  * **`mv *.sig hashes/`**  
* You should now have this structure, which is ready (except for the README.html) for uploading  
  * cldr-common-40.0.zip  
  * cldr-keyboards-40.0.zip  
  * cldr-tools-40.0.jar  
  * core.zip \-\> cldr-common-40.0.zip  
  * keyboards.zip \-\> cldr-keyboards-40.0.zip  
  * hashes/SHASUM512.txt  
  * hashes/cldr-tools-40.0.jar.sig  
  * hashes/cldr-common-40.0.zip.sig  
  * hashes/cldr-keyboards-40.0.zip.sig  
* Upload these same files ( excepting the symlinks ) to **https://github.com/unicode-org/cldr/releases/tag/release-40**

**E03**

Create a new directory, e.g. [http://unicode.org/Public/cldr39/](http://unicode.org/Public/cldr/39/) and populate with the zip files from \[Steven/Peter or whoever is making them for the release\]. Double check filenames core.zip, tools.zip, keyboards.zip. (Add json.zip and json\_full.zip or other files, if they've been made.) For each of the zip files, create a *symlink* that is the **versioned name** of the file. E.g. make cldr-tools-30.0.3.zip a symbolic link to tools.zip. Etc. Example:

   \# ln \-s cldr-common-38.0.zip core.zip

   \# ln \-s cldr-keyboards-38.0.zip keyboards.zip

which results in "ls \-la" showing them like this:

   core.zip \-\> cldr-common-38.0.zip

   keyboards.zip \-\> cldr-keyboards-38.0.zip

**NOTE**: This step should be updated again when we know which files precisely are supposed to be in the relase going forward.

**E04**

Update link to "latest" by modifying /home/httpd/htdocs/.htaccess to change the redirect of "/Public/cldr/latest" and "/Public/cldr/latest/". Two lines near the top of the file. Just update the number at the end of each.

**E05**

Change the chart redirects to point to the new versions. For testing, change the links on the page to the right to the new version, eg /22.1/ \=\> /23/. Then spot check. Make the beta-charts links point to 'not found' page (until we regenerate beta-charts). See the "**RewriteRule**" section in **/etc/apache2/httpd-rewrite.conf** where it points cldr/charts to the "latest" and "dev" release explicitly (around line 39ff of the special conf file). There are 2 redirects, one for dev and one for the new version that's being released. (Currently near lines 54 and 59 of the file.) See also the "stable links info" on Sites: [http://cldr.unicode.org/stable-links-info](http://cldr.unicode.org/stable-links-info). This must be updated and the server gracefully restarted. (Command "apachectl graceful" works.)

*Bug-How-To-Links:*

[https://sites.google.com/site/cldr/development/cldr-big-red-switch/test-chart-links](https://sites.google.com/unicode.org/cldr/development/cldr-big-red-switch/test-chart-links) (Copy out the text of the test links on that page; Change the {latest} to be current release, and any prev release numbers to the current one, the test them all.) See also info here and update DEV and LATEST as needed: [https://sites.google.com/site/cldr/stable-links-info](https://sites.google.com/unicode.org/cldr/stable-links-info) Verify links on [https://sites.google.com/site/cldr/development/cldr-big-red-switch/test-chart-links](https://sites.google.com/unicode.org/cldr/development/cldr-big-red-switch/test-chart-links) work.

**E06**

Verify that the links on the release page (e.g. [http://sites.google.com/site/cldr/index/downloads/cldr-28](https://sites.google.com/unicode.org/cldr/index/downloads/cldr-28) ) are correct, and Draft is removed from the text. At this time also make sure the "news" item on [http://cldr.unicode.org](http://cldr.unicode.org) is updated, at the top of the page \-- for all releases, alpha, beta, and final.

**E07**

Modify [http://sites.google.com/site/cldr/index/downloads](https://sites.google.com/unicode.org/cldr/index/downloads) so that it includes a new row on the top (Latest:), with the information from the current release (eg [http://sites.google.com/site/cldr/index/downloads/cldr-27](https://sites.google.com/unicode.org/cldr/index/downloads/cldr-27) ). Might be easiest to do in HTML mode. Most of the ***latest*** row should remain standard, but some of the *targets* of those anchors need to be updated for each release. Click through all and test them.

**E08**

Change the dtd redirection, in **/home/httpd/htdocs/cldr/dtd/.htaccess** so that https://www.unicode.org/cldr/dtd/(X+1)/ points to [https://www.unicode.org/cldr/data/common/dtd/](https://www.unicode.org/cldr/data/common/dtd/) http://www.unicode.org/cldr/dtd/X/ is new. Upload the tagged dtd files there. Edit /home/httpd/htdocs/cldr/dtd/.htaccess and bump the version number to "X+1". To verify that these are properly accessible, type the new dtd (X+1) URL into a browser, and if you get to the head of CVS, it's correct. (The tagged DTD files are in core.zip, in case they aren't supplied separately; get them from the zip file under common/dtd directory.)

**E09**

Update https://sites.google.com/site/cldr/index/charts and other pages that refer to version files directly. List here: https://sites.google.com/site/cldr/index/downloads/dev \[Not clear if there are other pages that should be included here??\] Here is a list of cells/fields on the [http://sites.google.com/site/cldr/index/downloads](https://sites.google.com/unicode.org/cldr/index/downloads) page that need to be checked and updated for reach release:

* [dev-version](http://cldr.unicode.org/index/downloads/dev)  
* [**dev-charts**](http://unicode.org/cldr/charts/dev)  
* [**dev-delta-dtd**](https://www.unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html)  
* [**dev-tickets**](https://unicode-org.atlassian.net/issues/?jql=project%20%3D%20CLDR%20AND%20status%20%3D%20Done%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%20%2239%22%20ORDER%20BY%20created%20ASC)  
* [**latest-version**](http://cldr.unicode.org/index/downloads/latest) // contents at [http://cldr.unicode.org/index/downloads/dev](http://cldr.unicode.org/index/downloads/dev) needs update  
* [**latest-charts**](https://unicode-org.github.io/cldr-staging/charts/latest/index.html) // we try to have all of these (this and below) have stable links with /latest/ and depend on either link redirects or copies, but some we have to hardcode.  
* [**latest-ldml**](http://www.unicode.org/reports/tr35/)  
* [**latest-tickets**](https://unicode-org.atlassian.net/issues/?jql=project%20%3D%20CLDR%20AND%20status%20%3D%20Done%20AND%20resolution%20%3D%20Fixed%20AND%20fixVersion%20%3D%20%2238%2E1%22%20ORDER%20BY%20created%20ASC)  
* [`release-39`](https://github.com/unicode-org/cldr/tree/release-39)  
* [**latest-delta-dtd**](https://unicode-org.github.io/cldr-staging/charts/38.1/supplemental/dtd_deltas.html)

*Bug-How-To-Links:*

In the charts page, add a row to the top of the table for the new release.(Recently Peter has been doing the new rel and download pages.)

**E13**

Update the sidebar for the release, eg: CLDR 1.8 Schedule: 2009-NN

\=\>

Latest Release: CLDR 1.8

and gray out the schedule. NOTE: Only the "site owner" can change the sidebar. Go to "edit site layout" in the "More" pulldown, then click on the Milestones header to edit that section.

NEW: Go to the bottom of the cldr.unicode.org home page and find the table for "Regular Semi-Annual Schedule". In HTML mode, copy which ever half of the table is for the upcoming release (spring or fall), and update the sidebar with that section of the table by pasting in the different HTML blob of the table.

**E14**

Create a new blog page from based on [Google doc of announcement drafts](https://docs.google.com/document/d/1_o-mGZkUgt68KqOJVV4OOWDUUzuRjp5AgwoMIKnWQ0M/edit#); publish the blog. (Note: these days home site gets update automatically with "news" based on the blog.)Tweet a little announcement pointing to the blog as needed. (Note: Mark and others on the ed committee can update and edit the blog/twitter as l2doc@unicode.org.)

**E15**

As an announcement, send contents of [http://sites.google.com/site/cldr/development/cldr-big-red-switch/draft-press](https://sites.google.com/unicode.org/cldr/development/cldr-big-red-switch/draft-press-release) to all the Unicode mailing lists \-- Usually first make sure the list is up to date by logging in as root and running "make-announcement-list.sh". Mark, Steven, Rick, and John are all able to send announcements to "announcements@unicode.org". (NOTE: best to double-buffer and check the HTML before sending to the list; HTML pasted in from sites and other places often has quirks.)

*Bug-How-To-Links:*

Normal announcement mechanism is described in: [http://www.unicode.org/\~book/announce-brs.html](http://www.unicode.org/~book/announce-brs.html) (for Rick or others to follow)

**E16**

Verify that these are empty before trunk opens: [http://unicode.org/cldr/trac/report/22](http://unicode.org/cldr/trac/report/22) [http://unicode.org/cldr/trac/report/51](http://unicode.org/cldr/trac/report/51) [http://unicode.org/cldr/trac/report/52](http://unicode.org/cldr/trac/report/52) \-- and then Notify cldr@unicode.org that the trunk is open: e.g., say "Now that CLDR X.Y has been released, the SVN trunk ( http://unicode.org/repos/cldr/trunk ) is open for commits towards the next release." Use a subject line something like: "CLDR X.Y \- trunk now open", where X.Y is the next release. If report 51 is not empty, verify that anything remaining is either the result of a misticketed item, or that any commits had net zero effect on trunk ( for example, a change that was later backed out ).

F02

Create a new stub release page for the next release by copying the previous page (...downloads/cldr-31 or whatever) and state that it's a stub. Retain the top-level headers from the previous release page. Yellow some sections for the update.

Fix the links as followed (to **Link Nonfinal**). (When the release is done, fix to **Link Final**).

*Note: replace 99 by target release number, 999 by target release number minus 1, and 88 by final tr35 revision.*  
Near the top, add this line:

**This version is currently in development. See the [latest release](http://cldr.unicode.org/index/downloads/latest).**

Update these two pages as required with the dev version pointer to the new page:

[http://cldr.unicode.org/index/downloads/latest](http://cldr.unicode.org/index/downloads/latest)

[http://cldr.unicode.org/index/downloads/beta](http://cldr.unicode.org/index/downloads/beta)

*Also create the Data folder so that the link doesn't fail, and have the charts made so that link doesn't fail.*

**F04**

Update all of the "CURRENT" reports in trac to point to the new current milestones. Make sure $user isn't replaced by your name. The affected reports are (52, 61, 66, 76, 77, 80\) and (62, 63).

**ALPHA PAGE TABLE HEADING**

When making an "alpha" release page, check the header info in the table. Before the release, some links should go to "dev" version rather than a non-existent final version.

Rel note \= dev version

Data \= no link

Charts \= dev version

Spec \= proposed

Delta \= link to the query for milestone

SVN Tag \= dev version

DTD delta \= "trunk" version
