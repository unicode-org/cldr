---
title: Generating Charts
---

# Generating Charts

## Generate

The input for this is the cldr\-staging/production file, and the output is in github cldr\-staging. **(If for the development version, the input is main or the maint branch, but we should change that.)**

1. Make sure the settings and VM arguments are right for where you are in the release:
	1. **Start, Mid\-release, Prefinal release,** or **Final release** (see below)
2. Run GenerateAllCharts. The results for each will be in ... cldr\-staging/docs/charts/by\_type/names.currency.html and so on.
3. Spot\-check for sanity.
	1. Start from the main page (eg cldr\-staging/docs/charts/index.html), and click on each of those links.
	2. On each of the subpages, take the first chart on each page, recursively.
	3. Use the "Index" link to go back up (not the back button), and make sure it goes to the right version of the page.
4. Check into github on cldr\-staging

## Start Release

1. Make sure the version  (eg **99**) is right in ToolConstants
	1. Make sure the *last* number (eg **99\.0**) is in CLDR\_VERSIONS
	2. Set DEFAULT\_CHART\_VERSION \= "99";
2. Add an new folder with that number, such as cldr\-staging/docs/charts/**99**
3. Create the archive ([Creating the Archive](https://cldr.unicode.org/development/creating-the-archive)) with at least the last release (if you don't have it already)
4. **Use the same VM arguments as Mid\-Release**

## Mid\-release

1. Use the VM arguements
	1. \-DCHART\_VERSION\=**99**
	2. \-DCHART\_STATUS\=**beta** // \=*default*, uses trunk, calls it β

## Prefinal Release

1. VM Arguments
	1. \-DCHART\_VERSION\=**99**
	2. \-DCHART\_STATUS\=**trunk** (uses trunk, no β. Used at the end of the release, but before the final data is in cldr\-archive)
2. In the printout from delta\_summary.txt, there is a listing of the sizes at the top
	1. Something like the following:
		1. \# dir file added deleted changed total
			1. TOTAL 30,276 3,601 10,909 2,153,094
	2. Add those new figures to the release page

## Final Release

1. Make sure the settings are:
	1. \-DCHART\_VERSION\=**99**
	2. \-DCHART\_STATUS\=**release** (only uses the cldr\-archive, no β)
2. Change the page <http://cldr.unicode.org/index/charts> to add the new release
3. Check the redirection links on [test\-chart\-links](https://cldr.unicode.org/development/cldr-big-red-switch/test-chart-links).
4. On index.html; open it, and fix the version (eg to 25β \=\> 25\)

## Modifying the chart programs

The chart programs have grown over time, and need some cleanup. For example, the supplemental charts duplicate code that is now in SupplementalDataInfo. 

### ShowLanguages.

The messages that they use are in a file util/data/chart\_messages.html. The right cell contains the key, which is extracted by lines like:

&emsp;PrintWriter pw \= new PrintWriter(new FormattedFileWriter(index, "Zone \\u2192 Tzid", null));

The key will be zone\_tzid, in this case.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)