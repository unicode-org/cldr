# CLDR SurveyTool Client Test

These tests are run against a live SurveyTool.

They run in a Node.js environment.

## Running Locally

Configuration is through environment variables. Give the tests your SurveyTool admin password and URL as shown.

```shell
CLDR_VAP=some-password
SURVEYTOOL_URL=http://localhost:9080
```

You launch these tests with `npm i && npm run client-test` from the `js` dir.

## Run in CI

Tests are automatically run in CI.

## License and Copyright

See the main [README.md](../../../../../README.md).
