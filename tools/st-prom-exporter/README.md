# Prom exporter for SurveyTool

What is this? An exporter for <https://prometheus.io> that reads from the
Survey Tool.

## Config/Installation

1. `npm i`

2. setup `config.json` as below:

```json
{
    "instances": {
        "cldr-smoke.unicode.org": "https://cldr-smoke.unicode.org/cldr-apps/SurveyAjax?what=status",
        "st.unicode.org": "https://st.unicode.org/cldr-apps/SurveyAjax?what=status"
    },
    "port": 9099
}
```

3. `node index.js`

Now, the exporter is listening on port 9099 and re-exporting ST metrics as Prometheus metrics.

## License and Copyright

©2020 Unicode, Inc. All Rights Reserved.

For license and copyright see
https://www.unicode.org/copyright.html
or [../../unicode-license.txt](../../unicode-license.txt)