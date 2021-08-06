# Prom exporter for SurveyTool

What is this? An exporter for <https://prometheus.io> that reads from the
Survey Tool.

## Planned Obsolescence

As part of [https://unicode-org.atlassian.net/browse/CLDR-14768](CLDR-14768),
the plan is to move the exporter itself into the SurveyTool.  This will remove the need for
a separate exporter instance, and will change the scrape URL to something such as
`/cldr-apps/metrics`.

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

### As a service

One possible way to keep the exporter running is to create the file:

`/lib/systemd/system/prometheus-cldr-surveytool-exporter.service`

with the following contents:

```
[Unit]
Description=Prometheus exporter for CLDR SurveyTool
Documentation=https://github.com/unicode-org/cldr/tree/master/tools/st-prom-exporter

[Service]
Restart=always
User=prometheus
ExecStart=/usr/bin/node /usr/local/src/cldr-st-prom-exporter/tools/st-prom-exporter/index.js $ARGS
ExecReload=/bin/kill -HUP $MAINPID
TimeoutStopSec=20s
SendSIGKILL=no

[Install]
WantedBy=multi-user.target
```

Note this assumes a CLDR checkout in `/usr/local/src/cldr-st-prom-exporter` with `config.json` in that directory.

Then, as root run:

```shell
# systemctl enable prometheus-cldr-surveytool-exporter.service
# systemctl start prometheus-cldr-surveytool-exporter.service
# systemctl status prometheus-cldr-surveytool-exporter.service
```

## Integration

Here is an example prometheus.yml scrape config:

```yaml
  - job_name: 'st'
    scrape_interval: 2m
    scrape_timeout: 2m
    static_configs:
      - targets: ['localhost:9909']
    metric_relabel_configs:
      - source_labels: [exported_instance]
        target_label: instance
```

Here is an alert rule to see that the surveytool is not busted:

```yaml
  - name: surveytool
    rules:
      - alert: surveytool_busted
        expr: surveytool_busted == 1
        for: 5m
        annotations:
          summary: "Survey Tool Busted on {{ $labels.exported_instance }}"
          description: "Survey Tool Busted: {{ $labels.exported_instance }}, Err: {{ $labels.err}}"
```

And here is an alert rule to make sure the exporter itself is up.

```yaml
  - name: instances
    rules:
      - alert: ScraperDown
        expr: up{job="st"} == 0
        for: 5m
        labels:
          severity: page
        annotations:
          summary: "Scraper for{{ $labels.job }} down"
          description: "{{ $labels.instance }} of job {{ $labels.job }} has been down for more than 30 minutes."
```

## License and Copyright

©2020 Unicode, Inc. All Rights Reserved.

For license and copyright see
https://www.unicode.org/copyright.html
or [../../unicode-license.txt](../../unicode-license.txt)