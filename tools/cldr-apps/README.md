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

### Licenses
 
- Usage of CLDR data and software is governed by the [Unicode Terms of Use](http://www.unicode.org/copyright.html)
a copy of which is included as [unicode-license.txt](../../unicode-license.txt).

For more details, see the main [README.md](../../README.md).

### Copyright

Copyright &copy; 1991-2021 Unicode, Inc.
All rights reserved.
[Terms of use](http://www.unicode.org/copyright.html)
