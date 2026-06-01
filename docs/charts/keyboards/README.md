# Keyboard Charts

## What are these

The Keyboard Charts are now built as client-side JavaScript tables.

## To Build from the command line

- install <https://nodejs.org> current LTS version, then in this directory:
- `npm i`
- `npm run build`

## To build from Maven

Run this from the command line in the top level directory:

- `mvn --file=tools/pom.xml -pl :cldr-keyboard-charts integration-test`

## To build from Eclipse

- Right-click on the `pom.xml` in `docs/charts/keyboards` (it will show as "cldr-keyboard-charts")
- Choose **Run As... - Maven Build**
- Change the **goal** to `integration-test`
- Choose Run

## Trying them out

- `npm run serve` will serve the charts locally on <http://localhost:3000>
- Or view the [`index.html`](./index.html) file located in the same directory as this README

