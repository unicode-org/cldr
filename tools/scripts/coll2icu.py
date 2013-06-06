#!/usr/bin/python
# -*- coding: utf-8 -*-
#
# created on: 2013jun05
# created by: Markus W. Scherer

"""Converts CLDR collation files from XML syntax to ICU syntax.

Handles the CLDR collation data in the post-CLDR 23 trunk in 2013 June.
Preserves indentation (except where it joins lines) and text vs. NCR etc.
Does not handle arbitrary LDML XML collation syntax."""

# Invoke with two arguments:
# - the source folder path
# - the destination folder path
# For example:
# ~/svn.cldr$ collicu/tools/scripts/coll2icu.py trunk/common/collation collicu/common/collation

import codecs
import glob
import os.path
import sys

def GetIndent(s):
  for i in xrange(len(s)):
    if s[i] not in " \t": return s[:i]
  return s


# substring replacements
replacements = (
  # White space and syntax characters must be quoted.
  ("<reset> </reset>", "&' '"),  # can't just replace all "> <"
  (">!<", ">'!'<"),
  ('>"<', ">'\\\"'<"),
  (">&quot;<", ">'\\\"'<"),
  (">#<", ">'\\u0023'<"),
  (">$<", ">'$'<"),
  (">%<", ">'%'<"),
  (">&<", ">'&'<"),
  (">&amp;<", ">'&'<"),
  (">'<", ">''<"),
  (">&apos;<", ">''<"),
  (">(<", ">'('<"),
  (">)<", ">')'<"),
  (">*<", ">'*'<"),
  (">+<", ">'+'<"),
  (">,<", ">','<"),
  (">-<", ">'-'<"),
  (">.<", ">'.'<"),
  (">/<", ">'/'<"),
  (">:<", ">':'<"),
  (">;<", ">';'<"),
  (">&lt;<", ">'<'<"),
  (">=<", ">'='<"),
  (">&gt;<", ">'>'<"),
  (">?<", ">'?'<"),
  (">@<", ">'@'<"),
  (">[<", ">'['<"),
  (">\\<", ">'\\\\'<"),
  (">]<", ">']'<"),
  (">^<", ">'^'<"),
  (">_<", ">'_'<"),
  (">`<", ">'`'<"),
  (">{<", ">'{'<"),
  (">|<", ">'|'<"),
  (">}<", ">'}'<"),
  (">~<", ">'~'<"),
  # ha.xml has the following
  ("'y", "''y"),
  ("'Y", "''Y"),
  # kl.xml has the following
  ("K'", "K''"),
  # not Pattern_White_Space, just obscure
  (u"\u00A0", u"\\u00A0"),
  (u"\u3000", u"\\u3000"),
  # obscure, and some tools do not handle noncharacters well
  (u"\uFDD0", u"\\uFDD0"),
  # Convert XML elements into ICU syntax.
  ("><!--", "> #"),  # add a space before an inline comment
  ("<!--", "#"),
  (" -->", ""),
  ("-->", ""),
  ("<reset>", "&"),
  ('<reset before="primary">', "&[before 1]"),
  ('<reset before="secondary">', "&[before 2]"),
  ('<reset before="tertiary">', "&[before 3]"),
  ("</reset>", ""),
  ("<p>", "<"),
  ("</p>", ""),
  ("<s>", "<<"),
  ("</s>", ""),
  ("<t>", "<<<"),
  ("</t>", ""),
  ("<i>", "="),
  ("</i>", ""),
  ("<pc>", "<*"),
  ("</pc>", ""),
  ("<sc>", "<<*"),
  ("</sc>", ""),
  ("<tc>", "<<<*"),
  ("</tc>", ""),
  ("<ic>", "=*"),
  ("</ic>", ""),
  ("<x>", ""),
  ("</x>", ""),
  ("<extend>", "/"),
  ("</extend>", ""),
  ("</context>", "|"),
  ("<first_tertiary_ignorable/>", "[first tertiary ignorable]"),
  ("<last_tertiary_ignorable/>", "[last tertiary ignorable]"),
  ("<first_secondary_ignorable/>", "[first secondary ignorable]"),
  ("<last_secondary_ignorable/>", "[last secondary ignorable]"),
  ("<first_primary_ignorable/>", "[first primary ignorable]"),
  ("<last_primary_ignorable/>", "[last primary ignorable]"),
  ("<first_variable/>", "[first variable]"),
  ("<last_variable/>", "[last variable]"),
  ("<first_non_ignorable/>", "[first regular]"),
  ("<last_non_ignorable/>", "[last regular]"),
  ("<first_trailing/>", "[first trailing]"),
  ("<last_trailing/>", "[last trailing]")
)


def ConvertFile(src, dest):
  in_rules = False
  partial = ""
  in_ml_comment = False
  for line in src:
    if "<rules>" in line:
      indent = GetIndent(line)
      stripped = line.strip()
      # Replace import-only rules with import elements.
      if stripped == '<rules><import source="sr"/></rules>':
        dest.write(indent + '<import source="sr"/>\n')
      elif stripped == '<rules><import source="hr" type="search"/></rules>':
        dest.write(indent + '<import source="hr" type="search"/>\n')
      elif stripped == '<rules><import source="hr"/></rules>':
        dest.write(indent + '<import source="hr"/>\n')
      elif stripped == '<rules><import source="ps"/></rules>':
        dest.write(indent + '<import source="ps"/>\n')
      else:
        # Replace the XML <rules> section with ICU syntax rules in <cr>.
        assert stripped == "<rules>"
        dest.write(indent + "<cr><![CDATA[\n")
        in_rules = True
    elif "</rules>" in line:
      # Flush, and go back to just copying lines until the next <rules>.
      if partial:
        dest.write(partial + "\n")
        partial = ""
      in_ml_comment = False
      dest.write(GetIndent(line) + "]]></cr>\n")
      in_rules = False
    else:
      if in_rules:
        # Find out whether we want to concatenate the current line
        # with the previous and/or next one.
        finish_partial = False  # Finish collected, partial input.
        start_ml_comment = False  # Start of a multi-line comment.
        stop_comment = False  # End of a comment, must terminate the line.
        if ("<reset" in line) or line.lstrip().startswith("<!--"):
          finish_partial = True
        if partial and len(partial.strip()) > 80:
          finish_partial = True
        if "<!--" in line and "-->" not in line:
          start_ml_comment = True
        if "-->" in line:
          assert line.rstrip().endswith("-->")
          stop_comment = True

        # Convert XML syntax to ICU syntax.
        while True:
          # Convert a Numeric Character Reference to \\uhhhh.
          i = line.find("&#x")
          if i < 0: break
          limit = line.find(";", i + 3)
          cp = line[i + 3:limit]
          while len(cp) < 4: cp = "0" + cp
          assert len(cp) == 4  # not handling supplementary code points
          line = line[:i] + "\\u" + cp + line[limit + 1:]

        if "<context>" in line:
          # Swap context & relation:
          #   <x><context>カ</context><i>ー</i></x>
          # turns into
          #   =カ|ー
          if "<i>" in line:
            line = line.replace("<i>", "").replace("<context>", "<i>")
          elif "<t>" in line:
            line = line.replace("<t>", "").replace("<context>", "<t>")

        for (xml, icu) in replacements:
          line = line.replace(xml, icu)

        # Start/continue/finish concatenation, and output.
        if partial and finish_partial:
          # Write collected input.
          dest.write(partial + "\n")
          partial = ""

        if start_ml_comment:
          # Start a multi-line comment.
          assert not partial
          comment_indent = GetIndent(line)  # can be the empty string
          in_ml_comment = True
        elif in_ml_comment:
          # Continue a multi-line comment.
          assert not partial
          if line.startswith(comment_indent):
            if line[len(comment_indent)] in " \t":
              # Preserve further indentation.
              line = comment_indent + "#" + line[len(comment_indent):]
            else:
              # Add a space after the #.
              line = comment_indent + "# " + line[len(comment_indent):]
          else:
            # Indent at least as much as the first line.
            line = line.lstrip()
            if line:
              line = comment_indent + "# " + line
            else:
              line = comment_indent + "#\n"
        elif stop_comment:
          # Just output the line, do not start collecting input.
          # ICU-syntax comments end with the end of the line,
          # do not append rules to them.
          if partial:
            line = partial + line.lstrip() + "\n"
            partial = ""
        elif not partial:
          # Start collecting input.
          partial = line.rstrip()
        elif partial:
          # Continue collecting input.
          partial += line.strip()

        if stop_comment:
          in_ml_comment = False
      if not partial: dest.write(line)


def main():
  (src_root, dest_root) = sys.argv[1:3]
  src_pattern = os.path.join(src_root, "*.xml")
  for src_path in glob.iglob(src_pattern):
    basename = os.path.basename(src_path)
    dest_path = os.path.join(dest_root, basename)
    with codecs.open(src_path, "r", "UTF-8") as src:
      with codecs.open(dest_path, "w", "UTF-8") as dest:
        ConvertFile(src, dest)


if __name__ == "__main__":
  main()
