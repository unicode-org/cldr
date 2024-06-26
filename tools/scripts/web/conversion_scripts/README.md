# Scripts to help with CLDR → Markdown Conversion

Part of the [CLDR to Markdown Conversion Process](https://docs.google.com/document/d/1NoQX0zqSYqU4CUuNijTWKQaphE4SCuHl6Bej2C4mb58/edit?usp=sharing), aiming to automate steps 1-3.

NOTE: does not get rid of all manual work, images, tables, and general review are still required. 

## File 1: cleanup.py

Objective: this file aims to correct some of the common mistakes that show up when using a html to markdown converter on the google sites CLDR site. It is not a comprehensive list, and there can still be mistakes, but it helps to correct some of the consistently seen errors that show up, particularly with the specific markdown converter used in pullFromCLDR.py. Most of the adjustments utilize regular expressions to find and replace specific text. The functions are as follows:

### Link Correction

- Removing redundant links, e.g. \(https://www.example.com)[https://www.example.com] → https://www.example.com
- Correcting relative links, e.g. \(index)[/index] → \(index)[https://cldr.unicode.org/index]
- Correcting google redirect links, e.g. \(people)[http://www.google.com/url?q=http%3A%2F%2Fcldr-smoke.unicode.org%2Fsmoketest%2Fv%23%2FUSER%2FPeople%2F20a49c6ad428d880&sa=D&sntz=1&usg=AOvVaw38fQLnn3h6kmmWDHk9xNEm] → \(people)[https://cldr-smoke.unicode.org/cldr-apps/v#/fr/People/20a49c6ad428d880]
- Correcting regular redirect links

### Common Formatting Issues

- Bullet points and numbered lists have extra spaces after them
- Bullet points and numbered lists have extra lines between them
- Link headings get put in with headings and need to be removed

### Project specific additions

- Every page has --- title: PAGE TITLE --- at the top of the markdown file
- Every page has the unicode copyright "\!\[Unicode copyright](https://www.unicode.org/img/hb_notice.gif)" at the bottom of the markdown file

## File 2: pullFromCLDR.py

Objective: this file is used along side cleanup.py to automate the process of pulling html and text from a given CLDR page. It uses libraries to retrieve the htmal as well as plain text from a given page, convert the html into markdown, parse the markdown using the cleanup.py file, and create the .md file and the temporary .txt file in the cldr site location. There are a couple of things to note with this:

- The nav bar header are not relevant to each page for this conversion process, so only the html within \<div role="main" ... > is pulled from the page
- To convert the html into raw text, the script parses the text, and then seperates relevant tags with newlines to appear as text does when copy/pasted from the page.
- This will only work with "https://cldr.unicode.org" pages, without modifying line 12 of the file

## Usage

### Installation

To run this code, you must have python3 installed. You need to install the following Python libraries:

- BeautifulSoup (from `bs4`)
- markdownify
- requests

You can install them using pip:

```bash
pip install beautifulsoup4 markdownify requests
```

### Constants

Line 8 of cleanup.py should contain the url that will be appended to the start of all relative links (always https://cldr.unicode.org):
```
#head to place at start of all relative links
RELATIVE_LINK_HEAD = "https://cldr.unicode.org"
```

Line 7 of pullFromCLDR.py should contain your local location of the cloned CLDR site, this is where the files will be stored:
```
#LOCAL LOCATION OF CLDR
CLDR_SITE_LOCATION = "DIRECTORY TO CLDR LOCATION/docs/site"
```

### Running

Before running, ensure that the folders associated to the directory of the page you are trying to convert are within your cldr site directory, and there is a folder named TEMP-TEXT-FILES.

Run with:
```
python3 pullFromCLDR.py
```

You will then be prompted to enter the url of the site you are trying to convert, after which the script will run.

If you would like to run unit tests on cleanup, or use any of the functions indiviually, run
```
python3 cleanup.py
```



