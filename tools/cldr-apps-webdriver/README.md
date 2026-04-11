# CLDR Survey Tool `tools/cldr-apps-webdriver` directory

Survey Tool WebDriver Test Framework: automated tests for the Survey Tool to ensure that it remains operational.

> “[WebDriver] is a remote control interface that enables introspection and control of user agents”

See [cldr-apps/README.md#docker-testing](../cldr-apps/README.md#docker-testing) for details on how to use this.

## Configuration

Configuration is via the `surveydriver.properties` file. You can see the [`surveydriver-docker.properties`](./surveydriver-docker.properties) file, which is used in the automated build for an example.

```properties
# This must be identical to the cldr.properties property CLDR_WEBDRIVER_PASSWORD=SomeRandomId
# and it grants special access to the test. As you might expect, this property is ignored
# in production.
WEBDRIVER_PASSWORD=SomeRandomId
# This tells the webdriver which instance to connect to.
SURVEYTOOL_URL=http://localhost:9080
# This is the URL of the selenium grid.
WEBDRIVER_URL=http://localhost:4444
# This is an optional parameter for detailed test output. See Output below
WEBDRIVER_OUTPUT=/tmp/webdriver-output/
# General timeout in seconds for Survey Tool startup. Bump this up if failures
# happen before the tool starts. However, if it's too long, it will delay the test
# when there's a problem.
TIME_OUT_SECONDS=120
```

### Output

If `WEBDRIVER_OUTPUT` is set and a directory, its contents will be cleared at the beginning of the test, and the following will be added (assuming test success). Not all components will be present depending on test status.

- `summary.md` - this summarizes the output and is suitable for appending to [`GITHUB_STEP_SUMMARY`](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-commands#adding-a-job-summary)
- `screenshots/` - this may contain screenshots at various steps.
- `data/` - this may contain any other downloadable content.
- `logs/` - additional logs, including those from the server

In CI, the entire `WEBDRIVER_OUTPUT` is also attached to the job.


## More details

For copyright, terms of use, and further details, see the top [README](../../README.md).

[WebDriver]: https://www.w3.org/TR/webdriver/
