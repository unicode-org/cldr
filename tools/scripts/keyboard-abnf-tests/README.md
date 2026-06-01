# Keyboard-abnf-tests

Tests for and against the ABNF files, written in Node.js

## To use

- `npm ci`
- `npm t`

## To update

Note there are four files. There's a `.d` directory for each ABNF file in keyboards/abnf/.  The "pass" files are expected to pass the ABNF and the "fail" to fail it.  Lines beginning with # are comments and skipped.

- transform-from-required.d/from-match.pass.txt
- transform-from-required.d/from-match.fail.txt
- transform-to-required.d/to-replacement.pass.txt
- transform-to-required.d/to-replacement.fail.txt

## Copyright

Copyright &copy; 1991-2025 Unicode, Inc.
All rights reserved.
[Terms of use](https://www.unicode.org/copyright.html)
