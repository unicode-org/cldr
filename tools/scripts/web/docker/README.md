# Docker for local builds of the CLDR Site

See <https://cldr.unicode.org/development/updating-site> for more details about updating the site.

## Installing assets

This is a prerequisite, and `build` needs to re-run if the sitemap changes.

1. run `npm i` and `npm run build` in `docs/site`

## Previewing locally

1. install https://docker.io
2. `docker compose up`
3. visit <http://127.0.0.1:4000>
4. hit control-C to cancel the docker run.
5. (on Windows, you may need to restart the container to pickup changes)

## Building the static site

1. `docker compose run -w /src site jekyll build`
2. output is in `./_site` here in this dir.

## Production Build

Cloudflare runs `sh tools/scripts/web/build-site.sh` from the repo root.  The `wrangler.jsonc` file at the root controls the deploy.

## Link Check

You can locally run lychee to link check \- [https://github.com/lycheeverse](https://github.com/lycheeverse).

   1. `lychee --cache http://127.0.0.1:4000/`

   2. 1-liner for link checking the entire site ( from docs/site dir ):
      `for p in $(jq  -r '.usermap | keys | flatten[]' < assets/json/tree.json); do echo; echo $p; lychee --cache http://127.0.0.1:4000/${p}; done`
