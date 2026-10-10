#!/bin/sh

# generate a data readme

cat <<EOF
# CLDR Data

This zipfile contains [CLDR](http://cldr.unicode.org) Data.

## LICENSE

See [LICENSE.](./LICENSE)

EOF

NOW=$(date '+%Y')

echo ">Copyright © 2019-$NOW Unicode, Inc. All rights reserved."
echo ">Distributed under the Terms of Use in https://www.unicode.org/copyright.html"
