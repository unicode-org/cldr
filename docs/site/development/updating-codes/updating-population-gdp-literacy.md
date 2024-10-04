---
title: Updating Population, GDP, Literacy
---

# Updating Population, GDP, Literacy

**Updated 2021\-02\-10 by Yoshito**

Instructions are based on Chrome browser.

## Load the World DataBank

**The World DataBank is at (http://databank.worldbank.org/data/views/variableselection/selectvariables.aspx?source=world-development-indicators). Unfortunately, they keep changing the link. If the page has been moved, try to get to it by doing the following. Each of the links are what currently works, but that again may change.**

1. Go to http://worldbank.org
2. Click "View More Data" in the Data section (http://data.worldbank.org/)
3. Click "Data Catalog" (http://datacatalog.worldbank.org/)
4. Search "World Development Indicators" (http://data.worldbank.org/data-catalog/world-development-indicators)
5. In "Data \& Resources" tab, click on the blue "Databank" link. It should open a new Window \- https://databank.worldbank.org/reports.aspx?source\=world\-development\-indicators

Once you are there, generate a file by using the following steps. There are 3 collapsible sections, "Country", "Series", and "Time"

- Countries
	- Expand the "Country" section, click the "Countries" tab, and then click the "Select All" button on the left. You do NOT want the aggregates here, just the countries. There were 217 countries on the list when these instructions were written; if substantially more than that, you may have mistakenly included aggregates.
- Series
	- Expand the "Series" section.
	- Select "Population, total"
	- Select "GNI, PPP (current international $)"
- Time
	- Select all years starting at 2000 up to the latest available year. The latest as of this writing was "2021". Be careful here, because sometimes it will list a year as being available, but there will be no real data there, which messes up our tooling.
	- The tooling will automatically handle new years.
- Click the "Download Options" link in the upper right.
	- A small "Download options" box will appear.
	- Select "CSV"
	- Instruct your browser to the save the file.
- You will receive a ZIP file named "**Data\_Extract\_From\_World\_Development\_Indicators.zip**".
	- Unpack this zip file. It will contain two files.
		- (From a unix command line, you can unpack it with
		- "unzip \-j \-a \-a **Data\_Extract\_From\_World\_Development\_Indicators.zip"**
			- to junk subdirectories and force the file to LF line endings.)
	- The larger file (126kb as of 2021\-02\-10\) contains the actual data we are interested in. The file name should be something like f17e18f5\-e161\-45a9\-b357\-cba778a279fd\_Data.csv
	- The smaller file is just a field definitions file that we don't care about.
- Verify that the data file is of the form:
	- Country Name,Country Code,Series Name,Series Code,2000 \[YR2000],2001 \[YR2001],2004 \[YR2004],...
	- Afghanistan,AFG,"Population, total",SP.POP.TOTL,19701940,20531160,23499850,24399948,25183615,...
	- Afghanistan,AFG,"GNI, PPP (current international $)",NY.GNP.MKTP.PP.CD,..,..,22134851020\.6294,25406550418\.3726,27761871367\.4836,32316545463\.8146,...
	- Albania,ALB,"Population, total",SP.POP.TOTL,3089027,3060173,3026939,3011487,2992547,2970017,...
	- ...
- Rename it to **world\_bank\_data.csv** and and save in {**cldr}/tools/cldr\-code/src/main/resources/org/****unicode****/cldr/util/data/external/**
- Diff the old version vs. the current.
- If the format changes, you'll have to modify WBLine in AddPopulationData.java to have the right order and contents.

## Load UN Literacy Data

1. Goto http://unstats.un.org/unsd/demographic/products/socind/default.htm
2. Click on "Education"
3. Click in "Table 4a \- Literacy"
4. Download data \- save as temporary file
5. Open in Excel, OpenOffice, or Numbers \- save as cldr/tools/java/org/unicode/cldr/util/data/external/un\_literacy.csv (Windows Comma Separated)
	1. If it has multiple sheets, you want the one that says "Data", and looks like:
6. Table 4a. Literacy
7. Last update: December 2012
8. Country or area Year Adult (15\+) literacy rate Youth (15\-24\) literacy rate
9. Total Men Women Total Men Women
10. Albania 2008 96 97 95 99 99 99
11. Diff the old version vs. the current.
12. If the format changes, you'll have to modify the loadUnLiteracy() method in **org/unicode/cldr/tool/AddPopulationData.java**
13. Note that the content does not seem to have changed since 2012, but the page says "*Please note this page is currently under revision*."
	1. If there is no change to the data (still no change 10 years later), there is no reason to commit a new version of the file.
	2. See also [CLDR\-15923](https://unicode-org.atlassian.net/browse/CLDR-15923)

## Load CIA Factbook

**Note:** Pages in original instruction were moved to below. These pages no longer provide text version compatible with files in CLDR. ([CLDR\-14470](https://unicode-org.atlassian.net/browse/CLDR-14470))

- Population: https://www.cia.gov/the-world-factbook/field/population
- Real GDP (purchasing power parity): https://www.cia.gov/the-world-factbook/field/real-gdp-purchasing-power-parity
1. All files are saved in **cldr/tools/java/org/unicode/cldr/util/data/external/**
2. Goto: https://www.cia.gov/library/publications/the-world-factbook/index.html
3. Goto the "References" tab, and click on "Guide to Country Comparisons"
4. Expand "People and Society" and click on "Population" \-
	1. There's a "download" icon in the right side of the header. Right click it, Save Link As... call it
	2. **factbook\_population.txt**
	3. **You may need to delete header lines. The first line should begin with "1 China … " or similar.**
5. Back up a page, then Expand "Economy" and click on "GDP (purchasing power parity)"
	1. Right Click on DownloadData, Save Link As... call it
	2. **factbook\_gdp\_ppp.txt**
	3. **You may need to delete header lines. The first line should begin with "1 China … " or similar.**
6. Literacy \- **No longer works, so we need to revise program \- They are still publishing updates to the data at this page, we just need to write some code to put the data into a form we can use, see** [**CLDR\-9756 (comment 4\)**](https://unicode-org.atlassian.net/browse/CLDR-9756?focusedCommentId=118608)
	1. ~~https://www.cia.gov/library/publications/the-world-factbook/fields/2103.html~~ maybe https://www.cia.gov/library/publications/the-world-factbook/fields/370.html ?
	2. ~~Right Click on "Download Data", Save Link As... Call it~~
	3. ~~**factbook\_literacy.txt**~~
7. Diff the old version vs. the current.
8. If the format changes, you'll have to modify the loadFactbookLiteracy()) method in **org/unicode/cldr/tool/AddPopulationData.java**

## Convert the data

1. If you saw any different country names above, you'll need to edit external/alternate\_country\_names.txt to add them.
	1. For example, we needed to add Czechia in 2016\.
2. Q: How would I know?
	1. If two\-letter non\-countries are added, then you'll need to adjust StandardCodes.isCountry.
3. Q: How would I know?
	1. Run "AddPopulationData *\-DADD\_POP*\=**true"** and look for errors.
4. **java \-jar \-DADD\_POP\=true \-DCLDR\_DIR\=${HOME}/src/cldr cldr.jar org.unicode.cldr.tool.AddPopulationData**
5. Once everything looks ok, check everything in to git.
6. Once done, then run the ConvertLanguageData tool as on [Update Language Script Info](/development/updating-codes/update-language-script-info)

