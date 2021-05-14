# CLDR Survey Tool

For general information, see the main [README.md](../../README.md).

The `cldr-apps` subproject builds `cldr-apps.war` which contains the Survey Tool
packaged for deployment. The Survey Tool is used to collect and confirm translations
for CLDR.

## Building and Running the Survey Tool

Please use the parent [CLDR/tools/pom.xml](../pom.xml) with maven to build and run.

- Copy `src/main/liberty/config/server.env.sample` to `src/main/liberty/config/server.env`
- Edit that `server.env` file to contain the MySQL credentials for the ST database
- Use `mvn --file=tools/pom.xml -pl cldr-apps liberty:dev` to run a development
web server, listening on port 9080
- Navigate to http://localhost:9080/cldr-apps to view the app

See <http://cldr.unicode.org/development/running-survey-tool> for further information
about the Survey Tool.

## Using Logging

This is interim guidance, to be expanded upon as part of [CLDR-8581](https://unicode-org.atlassian.net/browse/CLDR-8581)

### Getting a logger

Example:

```java
class MyClass {
    static final java.util.logging.Logger logger = SurveyLog.forClass(MyClass.class);
    // …
    MyClass() {
        logger.finer("A finer point.");  // see java.util.logging.Logger docs
        logger.info("Something informative!");
        logger.warning("Something bad happened!");
    }
}
```

### Configuring log

In `bootstrap.properties` _or other mechanism for setting system properties_, set the following to bump the log level:

```properties
com.ibm.ws.logging.trace.specification=org.unicode.cldr.web.MyClass.level=finest
```

You can also set in the `server.xml`:

```xml
<logging traceSpecification="org.unicode.cldr.web.MyClass.level=finest"/>
```

See <https://openliberty.io/docs/21.0.0.4/log-trace-configuration.html>

## Testing SMTP

I use [mailhog](https://github.com/mailhog/MailHog). From docker:

```shell
docker run --rm -p 8025:8025 -p 1025:1025 mailhog/mailhog
```

Then browse to http://localhost:8025 to watch mail flow in.

Then, setup SurveyTool with these `cldr.properties`:

```properties
CLDR_SENDMAIL=true
mail.host=127.0.0.1
mail.smtp.port=1025

## if needed
## etc see
#CLDR_SMTP_USER=authuser
#CLDR_SMTP_PASSWORD=authpassword
#mail.smtp.auth=true
#mail.smtp.starttls.enable=true
# see <https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html>

## other properties
## how long after ST startup before we start trying to send out mail
#CLDR_MAIL_DELAY_FIRST=55
## how many seconds to wait between each "batch"
#CLDR_MAIL_DELAY_EACH=15
## How many mails to send in each "batch"
#CLDR_MAIL_BATCHSIZE=0
## How long to wait between each mail in the batch.
CLDR_MAIL_DELAY_BATCH_ITEM=0

```

### Licenses

- Usage of CLDR data and software is governed by the [Unicode Terms of Use](http://www.unicode.org/copyright.html)
a copy of which is included as [unicode-license.txt](../../unicode-license.txt).

For more details, see the main [README.md](../../README.md).

### Copyright

Copyright &copy; 1991-2021 Unicode, Inc.
All rights reserved.
[Terms of use](http://www.unicode.org/copyright.html)
