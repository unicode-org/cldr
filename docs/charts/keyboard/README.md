# Keyboard Charts

## What are these

The Keyboard Charts are now built as client-side JavaScript tables.

## To Build from the command line

- install <https://nodejs.org> current LTS version, then in this directory:
- `npm i`
- `npm run build`

## To build from Maven

Run this from the command line in the top level directory:

- `mvn --file=tools/pom.xml -pl :cldr-keyboard-charts com.github.eirslett:frontend-maven-plugin:npm`

## Trying them out

- `npm run serve` will serve the charts locally on <http://localhost:3000>

