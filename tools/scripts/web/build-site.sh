#!/bin/sh

set -x

bash tools/scripts/web/check-site-diffs.sh ${WORKERS_CI_COMMIT_SHA:-HEAD}

gem install bundler jekyll kramdown-parser-gfm webrick && \
cd docs/site/assets && \
PUPPETEER_SKIP_DOWNLOAD=true npm ci && \
npm run build && \
cd .. &&  \
jekyll build || exit 1

# TEMPORARY (CLDR-19852): see debug-mermaid.sh
bash ../../tools/scripts/web/debug-mermaid.sh

echo 'Output in ./_site'
