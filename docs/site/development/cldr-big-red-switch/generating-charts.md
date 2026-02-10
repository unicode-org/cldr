---
title: Generating Charts
---

## Generate

The input for this process is the `../cldr-staging/production` file, and the output is in the github repo [`cldr-staging`][cldr-staging]. **(If for the development version, the input is `main` or the `maint` branch, but we should change that.)**

1. Switch to the correct branch in the `cldr-staging` repo, such as `charts/49`.
	- `cldr-staging` is expected to be checked out as a _sibling_ directory to your CLDR source repository, hence `../cldr-staging`
	- If that branch doesn't exist, create it (by copying from the next lowest numbered branch).
	- You will then need to git delete any existing subdirectories in`docs/charts/` in `cldr-staging`.
	- To summarize, every chart branch in cldr-staging, such as `charts/49` should only have ONE charts directory, that is `docs/charts/49`.
2. Make sure the settings and VM arguments are right for where you are in the release:
	1. **Start, Mid\-release, Prefinal release,** or **Final release** (see below)
3. Build the keyboard charts.
	```shell
	mvn --file=tools/pom.xml -pl :cldr-keyboard-charts integration-test
	```
	For more details and options, see [keyboard-charts].
4. Run the Java tool `GenerateAllCharts`. The results for each will be in `../cldr-staging/docs/charts/<version>/by_type/names.currency.html` and so on.
5. Spot\-check for sanity.
	1. Start from the main page (eg `cldr-staging/docs/charts/<version>/index.html`)
	2. Check the DTD deltas (`cldr-staging/docs/charts/<version>/supplemental/dtd_deltas.html`. 
	    * All major versions should have at least one row for that version. 
	    * Dot versions may or may not: diff the /dtd/ files to check for whether differences should show up.
	3. Spot check the Delta Charts (`cldr-staging/docs/charts/<version>/delta/index.html`)
	    * ¤¤BCP47 Delta
	    * ¤¤Supplemental Delta
	    * ¤¤Transforms Delta
	    * a locale in a language you know.
	 4.  For other links on the main page, click on each of those links.
         * On each of the subpages, take the first chart on each page, recursively.
         * Use the "Index" link to go back up (not the back button), and make sure it goes to the right version of the page.
6. Push the updated branch to the `cldr-staging` repo. It can be done with a PR, but a PR is not necessary (and nearly impossible to review).

## Deployment

- As noted above, charts should be pushed to a branch such as `charts/49` in the [cldr-staging] repository.
- Each branch, for example, `charts/49`, should only have ONE release's charts, in the `docs/charts/49` directory.  The `charts/50` branch would have `docs/charts/50` and so on.
- It is not necessary to use pull requests to update these branches.
- Every time content is pushed to a `charts/` branch, two deployments are started, one to 'preview' and one to 'production'. (see previous section)
- To approve deployments, go to [Waiting Deployments][waiting-deployments]

### Preview Deployment

- Begin by approving the _preview_ deployment. Click on Preview and then Review Pending Deployments.

![Screenshot: Review Requested](/images/development/charts-approve.png)

- Check the Preview deployment and approve it. A comment is optional.

![Screenshot: Approving](/images/development/charts-approve2.png)

- The deployment will start. Click on the Summary button.

![Screenshot: Summary](/images/development/charts-summary.png)

- Once the preview is deployed, it will display the preview link, such as:

> - Updated: <https://cldr-smoke.unicode.org/cldr/charts/49>

![Screenshot: Preview](/images/development/charts-preview.png)

- Click on the link and validate that the charts are correct.

### Production Deployment

- Go to the review section above, and approve the production deployment.

- The process is identical, except that it updates the production URL:

> - Updated: https://www.unicode.org/cldr/charts/49

## Chart versions throughout the release cycle

### Start Release

1. Make sure the version  (eg **`99`**) is right in `ToolConstants.java`
	1. Make sure the *last* number (eg **99\.0**) is in CLDR\_VERSIONS
	2. Set DEFAULT\_CHART\_VERSION \= "99";
2. Add an new folder with that number, such as `cldr-staging/docs/charts/`**99**
3. Create the archive ([Creating the Archive](/development/creating-the-archive)) with at least the last release (if you don't have it already)
4. **Use the same VM arguments as Mid\-Release**

### Mid\-release

1. Use the VM arguements
	1. \-DCHART\_VERSION\=**99**
	2. \-DCHART\_STATUS\=**beta** // \=*default*, uses trunk, calls it β

### Prefinal Release

1. VM Arguments
	1. \-DCHART\_VERSION\=**99**
	2. \-DCHART\_STATUS\=**trunk** (uses trunk, no β. Used at the end of the release, but before the final data is in cldr\-archive)
2. In the printout from `tsv/delta_summary.tsv`, there is a listing of the sizes at the top
	1. Something like the following:
		1. \# dir file added deleted changed total
			1. TOTAL 30,276 3,601 10,909 2,153,094
	2. Add those new figures to the release page _(TODO: where?)_

### Final Release

1. Make sure the settings are:
	1. \-DCHART\_VERSION\=**99**
	2. \-DCHART\_STATUS\=**release** (only uses the `cldr-archive`, no β)
2. Check the redirection links on [test\-chart\-links](/development/cldr-big-red-switch/test-chart-links).
3. On `index.html`; open it, and fix the version (eg to 25β \=\> 25\)

## Modifying the chart programs

The chart programs have grown over time, and need some cleanup. For example, the supplemental charts duplicate code that is now in `SupplementalDataInfo.java`

### `ShowLanguages.java`

The messages that they use are in a file `util/data/chart_messages.html`. The right cell contains the key, which is extracted by lines like:

```java
PrintWriter pw = new PrintWriter(new FormattedFileWriter(index, "Zone \u2192 Tzid", null));
```

The key will be `zone_tzid`, in this case.


[keyboard-charts]: https://github.com/unicode-org/cldr/blob/main/docs/charts/keyboards/README.md
[cldr-staging]: https://github.com/unicode-org/cldr-staging
[waiting-deployments]: https://github.com/unicode-org/cldr-staging/actions/workflows/post-smoke.yml?query=is%3Awaiting
