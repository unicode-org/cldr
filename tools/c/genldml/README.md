# CLDR `tools/c/genldml` directory

This directory contains source code for an internal tool for processing CLDR data, circa 2003.

The only C language tool, this was used to convert ICU format data into LDML.

## Pre-requisities

- ICU 2.4 build
- VC++ 6 with Service Pack 2

## Building gendlml

- Open a command window

- Enter the following commands

  `c:\> set ICU_ROOT=<absolute path to ICU directory>`

  `c:\> set PATH=%PATH%;<absolute path to ICU directory>\bin`

- Launch Microsoft Development studio from the command window so that it inherits the environment

  `c:\> msdev`

- Open genldml.dsw

- Select `Build > Rebuild All`

### Copyright

For copyright, terms of use, and further details, see the top [README](../../../README.md).
