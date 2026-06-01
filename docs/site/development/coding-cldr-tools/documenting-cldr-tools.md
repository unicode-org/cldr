---
title: Documenting CLDR Tools
---

# Documenting CLDR Tools

*Developers: Make sure your tool is easily accessible from the command line.*

You can add the @CLDRTool annotation to any class in cldr\-code that has a main() function, and it will be documented as part of the JAR cldr\-code.jar is used.

See [CLDR Tools](/development/cldr-tools) for general information about obtaining and using CLDR tools.

## Coding it

An example from ConsoleCheckCLDR.java will start us out here

&emsp;&emsp;@CLDRTool(alias \= "check",

&emsp;&emsp;description \= "Run CheckCLDR against CLDR data")

&emsp;&emsp;public class ConsoleCheckCLDR { …

Then, calling ```java -jar cldr-tools.jar -l``` produces:

&emsp;&emsp;*check \- Run CheckCLDR against CLDR data*

&emsp;&emsp;*\<http://cldr.unicode.org/tools/check\>*

&emsp;&emsp;*\= org.unicode.cldr.test.ConsoleCheckCLDR*

And then ```java -jar cldr-tools.jar check``` can be used to run this tool. All additional arguments after "check" are passed to **ConsoleCheckCLDR.main()** as arguments.

Note these annotation parameters. Only "alias" is required.

- **alias** \- used from the command line instead of the full class name. Also forms part of the default URL for documentation.
- **description** \- a short description of the tool.

Additional parameters:

- **url** \- you can specify a custom URL for the tool. This is displayed with the listing.
- **hidden** \- if non\-empty, this specifies a reason to *not* show the tool when running "java \-jar" without "\-l". For example, the main() function may be a less\-useful internal tool, or a test.
## Documenting it

Assuming your tools’s alias is *myalias,* create a new subpage with the URL http://cldr.unicode.org/tools/myalias (a subpage of [CLDR Tools](/development/cldr-tools)). Fill this page out with information about how to use your tool.

