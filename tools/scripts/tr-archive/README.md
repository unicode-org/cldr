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

This will update the tr .md files in place.
### Copyright

Copyright &copy; 1991-2021 Unicode, Inc.
All rights reserved.
[Terms of use](https://www.unicode.org/copyright.html)

See [../../../README.md](../../../README.md) for full project information.

