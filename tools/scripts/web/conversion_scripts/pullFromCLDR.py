import requests
from bs4 import BeautifulSoup
import markdownify 
from cleanup import fullCleanupString


#LOCAL LOCATION OF CLDR
CLDR_SITE_LOCATION = "/Users/chrispyle/Documents/GitHub/cldr/docs/site"

#fetch HTML from the website
url = input("Enter link to convert: ")
#compute path in cldr using url
directoryPath = url.replace("https://cldr.unicode.org", "")
outputMDFile = CLDR_SITE_LOCATION + directoryPath + ".md"
#compute path for text file using name of page
outputTextFile = CLDR_SITE_LOCATION + "/TEMP-TEXT-FILES/" + url.rsplit('/', 1)[-1] + ".txt"

#get html content of page
response = requests.get(url)
html_content = response.text

#extract html inside <div role="main" ... >
soup = BeautifulSoup(html_content, 'html.parser')
main_div = soup.find('div', {'role': 'main'})
html_inside_main = main_div.encode_contents().decode('utf-8')

#convert html to md with markdownify and settings from conversion doc
markdown_content = markdownify.markdownify(html_inside_main, heading_style="ATX", bullets="-") 
#clean md file using cleanup.py
cleaned_markdown = fullCleanupString(markdown_content)

#parse raw text from site
textParser = BeautifulSoup(html_inside_main, 'html.parser')

#add newlines to text content for all newline tags
for block in textParser.find_all(['p', 'div', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'li', 'br']):
    block.append('\n')

#fet text content from the parsed HTML
rawText = textParser.get_text()

#remove unnecessary newlines
rawText = '\n'.join(line.strip() for line in rawText.splitlines() if line.strip())

#write files to cldr in proper locations
with open(outputMDFile, 'w', encoding='utf-8') as f:
    f.write(cleaned_markdown)

with open(outputTextFile, 'w', encoding='utf-8') as f:
    f.write(rawText)

