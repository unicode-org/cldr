import re
import requests
import urllib.parse
import unittest
from unittest.mock import patch

#head to place at start of all relative links
RELATIVE_LINK_HEAD = "https://cldr.unicode.org"

#sometimes the html --> md conversion puts extra spaces between bullets
def fixBullets(content):
    #remove extra spaces after dash in bullet points
    content = re.sub(r'-\s{3}', '- ', content)
    #remove extra space after numbered bullet points
    content = re.sub(r'(\d+\.)\s{2}', r'\1 ', content)
    #process lines for list handling
    processed_lines = []
    in_list = False
    for line in content.splitlines():
        if re.match(r'^\s*[-\d]', line):
            #check if the current line is part of a list
            in_list = True
        elif in_list and not line.strip():
            #skip empty lines within lists
            continue
        else:
            in_list = False
        processed_lines.append(line)
    processed_content = '\n'.join(processed_lines)
    
    return processed_content

#html-->md conversion puts link headings into md and messes up titles
def fixTitles(content):
    #link headings regex
    pattern = re.compile(r'(#+)\s*\n*\[\n*\]\(#.*\)\n(.*)\n*')

    #replace matched groups
    def replaceUnwanted(match):
        heading_level = match.group(1)  #heading level (ex. ##)
        title_text = match.group(2).strip()  #capture and strip the title text
        return f"{heading_level} {title_text}"  #return the formatted heading and title on the same line

    # Replace the unwanted text using the defined pattern and function
    processed_content = re.sub(pattern, replaceUnwanted, content)
    return processed_content

# add title at top and unicode copyright at bottom
def addHeaderAndFooter(content):
    #get title from top of md file
    title_match = re.search(r'(?<=#\s).*', content)
    if title_match:
        title = title_match.group(0).strip()
    else:
        title = "Default Title"  #default if couldnt find
    
    #header
    header = f"---\ntitle: {title}\n---\n"
    #footer
    footer = "\n![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)\n"

    #look for existing title and copywrite in the YAML front matter
    title_exists = re.search(r'^---\n.*title:.*\n---', content, re.MULTILINE)
    footer_exists = footer.strip() in content

    #add header
    if not title_exists:
        content = header + content
    
    #add footer
    if not footer_exists:
        content = content + footer
    
    return content

#html-->md sometimes produces double bullets on indented lists
def fixIndentedBullets(content):
    #regex pattern to match the double hyphen bullets
    pattern = re.compile(r'^-\s-\s(.*)', re.MULTILINE)

    #split into lines
    lines = content.split('\n')

    #normalize bullets
    normalized_lines = []
    in_list = False
    
    for line in lines:
        #lines with double hyphens
        match = pattern.match(line)
        if match:
            #normalize the double hyphen bullet
            bullet_point = match.group(1)
            normalized_lines.append(f'- {bullet_point.strip()}')
            in_list = True
        elif in_list and re.match(r'^\s*-\s', line):
            #remove indentation from following bullets in the same list
            normalized_lines.append(line.strip())
        else:
            normalized_lines.append(line)
            in_list = False

    #join back into a single string
    processed_content = '\n'.join(normalized_lines)
    return processed_content

#links on text that is already a link
def removeRedundantLinks(content):
    #(link)[link] regex pattern
    link_pattern = re.compile(r'\((https?:\/\/[^\s\)]+)\)\[\1\]')

    #function to process unwanted links
    def replace_link(match):
        return match.group(1)  #return only the first URL

    #replace the links
    processed_content = re.sub(link_pattern, replace_link, content)
    return processed_content

#process links, google redirects, normal redirects, and relative links (takes in a url)
def convertLink(url):
    #relative links
    if url.startswith("/"):
        return RELATIVE_LINK_HEAD + url
    #google redirect links
    elif "www.google.com/url" in url:
        parsed_url = urllib.parse.urlparse(url)
        query_params = urllib.parse.parse_qs(parsed_url.query)
        if 'q' in query_params:
            return query_params['q'][0]
        return url
    #redirects
    else:
        try:
            response = requests.get(url)
            return response.url
        except requests.RequestException as e:
            print(f"Error following redirects for {url}: {e}")
            return url

#finds all links and runs them through converLink
def process_links(content):
    #regex pattern for md links
    pattern = re.compile(r'\[(.*?)\]\((.*?)\)')
    
    #replace each link
    def replace_link(match):
        text = match.group(1)
        url = match.group(2)
        new_url = convertLink(url)
        return f'[{text}]({new_url})'
    
    return pattern.sub(replace_link, content)

#given a file path to an md file, run it through every cleanup function and write inot samle.md
def fullCleanup(file_path):
    with open(file_path, 'r') as file:
        content = file.read()  # Read entire file as a string
    content = addHeaderAndFooter(content)
    content = fixTitles(content)
    content = fixBullets(content)
    content = removeRedundantLinks(content)
    content = fixIndentedBullets(content)
    content = process_links(content)
    with open("sample.md", 'w') as file:
        file.write(content)

#given a md string, run through every cleanup function and return result
def fullCleanupString(str):
    content = addHeaderAndFooter(str)
    content = fixTitles(content)
    content = fixBullets(content)
    content = removeRedundantLinks(content)
    content = fixIndentedBullets(content)
    content = process_links(content)
    return content


#TESTS
class TestMarkdownLinkProcessing(unittest.TestCase):
    def test_remove_redundant_links(self):
        #standard use cases
        markdown_content1 = '''
        redundant link (https://mail.google.com/mail/u/1/#inbox)[https://mail.google.com/mail/u/1/#inbox].
        not redundant link [example](https://www.example.com).
        '''
        expected_output1 = '''
        redundant link https://mail.google.com/mail/u/1/#inbox.
        not redundant link [example](https://www.example.com).
        '''
        self.assertEqual(removeRedundantLinks(markdown_content1), expected_output1)

        #edge cases:
        #If the link does not start with http:// or https:// it will not be picked up as a link
        #if the two links are different, it does not get corrected
        markdown_content2 = '''
        not link [www.example.com](www.example.com).
        Different links (https://mail.google.com/mail/u/1/#inbox)[https://emojipedia.org/japanese-symbol-for-beginner].
        '''
        expected_output2 = '''
        not link [www.example.com](www.example.com).
        Different links (https://mail.google.com/mail/u/1/#inbox)[https://emojipedia.org/japanese-symbol-for-beginner].
        '''
        self.assertEqual(removeRedundantLinks(markdown_content2), expected_output2)

    @patch('requests.get')
    def test_replace_links(self, mock_get):
        #mock responses for follow_redirects function
        def mock_get_response(url):
            class MockResponse:
                def __init__(self, url):
                    self.url = url
            if url == 'http://www.google.com/url?q=http%3A%2F%2Fwww.typolexikon.de%2F&sa=D&sntz=1&usg=AOvVaw3SSbqyjrSIq8enzBt6Gltw':
                return MockResponse('http://www.typolexikon.de/')
            elif url == 'http://www.example.com/':
                return MockResponse('http://www.example.com/')
            return MockResponse(url)

        mock_get.side_effect = mock_get_response

        #standard use cases
        markdown_content1 = '''
        relative link [page](/relative-page).
        Google redirect link [typolexikon.de](http://www.google.com/url?q=http%3A%2F%2Fwww.typolexikon.de%2F&sa=D&sntz=1&usg=AOvVaw3SSbqyjrSIq8enzBt6Gltw).
        normal link [example.com](http://www.example.com/).
        '''
        expected_output1 = '''
        relative link [page](https://cldr.unicode.org/relative-page).
        Google redirect link [typolexikon.de](http://www.typolexikon.de/).
        normal link [example.com](http://www.example.com/).
        '''
        cleaned_content = removeRedundantLinks(markdown_content1)
        self.assertEqual(process_links(cleaned_content), expected_output1)

if __name__ == '__main__':
    fullCleanup("testing.md")
    unittest.main()
