# CLDR Scripts

The `tools/scripts` directory contains accessory scripts of various kinds, used for CLDR process and Survey Tool deployment. These include:

- ansible: a folder containing Ansible scripts for configuring a remote server to run the Survey Tool, still in use as of 2025
- cldr-svnprops-check.py: possibly obsolete, a Python script to check and fix svn property settings for CLDR source files
- cldrres.mk: possibly obsolete, a Makefile to (re)generate ICU data out of CLDR
- CLDRWrapper: possibly obsolete, a UNIX executable file, purpose undocumented
- col2icu.py: possibly obsolete, a Python script to convert CLDR collation files from XML syntax to ICU syntax
- fixSVNProps.sh: possibly obsolete, a shell script, using svn; purpose unknown
- interimVettingTool.sh: possibly obsolete, a shell script; purpose unknown
- jira-updater: a folder containing JavaScript and json code, still in use as of 2025
- keyboard-abnf-tests: a folder containing JavaScript code, still in use as of 2025
- llm: a folder containing an AI-powered classifier that flags entries deviating from expected patterns, still in use as of 2025
- platformDiffTool.sh: possibly obsolete, a shell script to create an InterimVettingChart
- sidewaysCharts.sh: a shell script to run GenerateSidewaysView (a Java class)
- tr-archive: a folder containing JavaScript for a Unicode TR35 archiver; this is the critical production path for producing TR35
- uca: a folder containing a shell script blankweights.sed to blank out non-zero weights
- updateHash.sh: a shell script to calculate shasum of CLDR jar/zip files
- web: a folder containing a variety of shell scripts, Python scripts, and Docker files, related to the site cldr.unicode.org

For copyright, terms of use, and further details, see the top [README](../../README.md).
