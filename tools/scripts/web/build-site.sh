#!/bin/sh

set -x

bash tools/scripts/web/check-site-diffs.sh ${WORKERS_CI_COMMIT_SHA:-HEAD}

gem install bundler jekyll kramdown-parser-gfm webrick && \
cd docs/site/assets && \
npm ci && \
npm run build && \
cd .. &&  \
rougify style github > assets/css/syntax.css && \
jekyll build || exit 1

echo 'Output in ./_site'
