#!/bin/sh

set -x

gem install bundler jekyll kramdown-parser-gfm webrick && \
cd docs/site/assets && \
npm ci && \
npm run build && \
cd .. &&  \
jekyll build || exit 1

echo 'Output in ./_site'
