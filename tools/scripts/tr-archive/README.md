# Unicode TR35 archiver

## What this does

- render `../../../docs/ldml/*.md` into `.html`
- add some CSS and fixup the HTML a little bit
- The goal is to create .html suitable to end up at, for example, <https://www.unicode.org/reports/tr35/tr35-61/tr35.html>

## Prerequisites

Node.js (Tested with v12)

## How to use

- To generate the .zip file: (Unix)

```shell
$ bash make-tr-archive.sh
```

You will end up with HTML files under `dist/` and a zipped up `./tr35.zip` archive.

- To generate, and also serve locally via a little web server (Unix):

```shell
$ npm install
$ npm run serve
```

`reports.css` gets downloaded locally so that the relative link ( `../reports.css`) within the HTML will work.


## Updating ToC

```shell
$ npm install
$ npm run fix-tocs
```

This will update the tr .md files in place. Then, go ahead and check in updates to the .md fioles

## Checking internal link targets and stable anchors

```shell
$ npm install
$ npm run build
$ npm run extract-link-targets
```

1. fix any errors, such as bad links
2. there are warnings about duplicate anchors - these are OK.
3. check the git status an diff on the `docs/ldml/tr35*.anchors.json` files
  - make sure that any anchors aren't inexplicably removed

### Copyright

Copyright &copy; 1991-2021 Unicode, Inc.
All rights reserved.
[Terms of use](https://www.unicode.org/copyright.html)

See [../../../README.md](../../../README.md) for full project information.
