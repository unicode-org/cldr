---
title: List Formatting
---

# List Formatting

We add a set of patterns used for formatting variable-length lists, such as "A, B, C, and D" as follows:

\<listPatterns>

&emsp;\<listPattern>

&emsp;&emsp;\<listPatternPart type="2">{0}, {0}\</listPatternPart>

&emsp;&emsp;\<listPatternPart type="end">{0}, {1}\</listPatternPart>

&emsp;&emsp;\<listPatternPart type="middle">{0}, {1}\</listPatternPart>

&emsp;&emsp;\<listPatternPart type="start">{0}, {1}\</listPatternPart>

&emsp;\</listPattern>

\</listPatterns>

The way this works is that you format with type = exact number if there is one (eg type="2"). If not:

1. Format the last two elements with the "end" format.
2. Then use middle format to add on subsequent elements working towards the front, all but the very first element. That is, {1} is what you've already done, and {0} is the previous element.
3. Then use "start" to add the front element, again with {1} as what you've done so far, and {0} is the first element.

Thus a list (a,b,c,...m, n) is formatted as

start(a,middle(b,middle(c,middle(...end(m, n))...)))

By using start, middle, and end, we have the possibility of doing something special between the first two and last two elements. So here's how it would work for English.

\<listPatterns>

&emsp;\<listPattern>

&emsp;&emsp;\<listPatternPart type="2">{0} and {1}\</listPatternPart>

&emsp;&emsp;\<listPatternPart type="end">{0}, and {1}\</listPatternPart>

&emsp;&emsp;\<listPatternPart type="middle">{0}, {1}\</listPatternPart>

&emsp;&emsp;\<listPatternPart type="start">{0}, {1}\</listPatternPart>

&emsp;\</listPattern>

\</listPatterns>

Thus a list (a,b,c,d) is formatted as "a, b, c, and d" using this.

Note that a higher level needs to handle the cases of zero and one element. Typically one element would just be that element; for zero elements a different structure might be substituted. Example:

- zero: There are no meetings scheduled.
- one: There is a meeting scheduled on Wednesday.
- other: There are meetings scheduled on Wednesday, Friday, and Saturday.

(The grammar of rest of these sentences aside from the list can be handled with plural formatting.)

To account for the issue Philip raises, we might want to have alt values for a semi-colon (like) variant.


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)