# Docker for CLDR Site

## Installing assets

1. run `npm i` and `npm run build` in `docs/site`

## Previewing locally

1. install https://docker.io
2. `docker compose up`
3. visit <http://127.0.0.1:4000>
4. hit control-C to cancel the docker run.

## Building

1. `docker compose run -w /src site jekyll build`
2. output is in `./_site` here in this dir.
