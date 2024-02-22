package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;

@CLDRTool(
        alias = "checkout-archive",
        description = "Checkout CLDR archive to $ARCHIVE (usually ../cldr-archive)",
        url = "https://cldr.unicode.org/development/creating-the-archive")
public class CheckoutArchive {
    enum MyOptions {
        prune("Perform a 'git prune' first"),
        echo("Only show commands, don't run them. (Dry run)"),
        ;

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = new Option(this, argumentPattern, defaultArgument, helpText);
        }

        private MyOptions(String helpText) {
            option = new Option(this, helpText);
        }

        static Options myOptions = new Options();

        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args, boolean showArguments) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        MyOptions.parse(args, true);

        Path archiveDir = new File(CLDRPaths.ARCHIVE_DIRECTORY).toPath();
        if (!archiveDir.toFile().isDirectory()) {
            throw new FileNotFoundException(
                    "Archive directory "
                            + archiveDir.toString()
                            + " does not exist, please create it or change the value of -DARCHIVE=");
        }
        System.out.println("Setting up in $ARCHIVE " + archiveDir.toString() + " …");
        int skip = 0;
        int created = 0;
        int err = 0;

        if (MyOptions.prune.option.doesOccur()) {
            final String cmd[] = {
                "git", "worktree", "prune",
            };
            if (runCommand(cmd)) {
                err++;
            }
        }

        for (final String ver : ToolConstants.CLDR_VERSIONS) {
            final Path dirName = archiveDir.resolve("cldr-" + ver);
            if (dirName.toFile().isDirectory()) {
                skip++;
                System.out.println("# Skipping existing \t" + dirName.toString());
            } else {
                final String tag = "release-" + ver.replaceAll("\\.", "-").replaceAll("-0$", "");
                final String cmd[] = {"git", "worktree", "add", dirName.toString(), tag};
                if (runCommand(cmd)) {
                    err++;
                }
            }
        }
        System.out.println(String.format("Created %d and skipped %d version(s)", created, skip));
        if (err != 0) {
            throw new RuntimeException("Total errors: " + err);
        }
    }

    /**
     * Run a command
     *
     * @param cmd
     * @return true on err
     * @throws InterruptedException
     * @throws IOException
     */
    private static boolean runCommand(final String[] cmd) throws InterruptedException, IOException {
        System.out.println("# " + String.join(" ", cmd));
        if (!MyOptions.echo.option.doesOccur()) {
            int ev =
                    new ProcessBuilder(cmd)
                            .directory(new File(CLDRPaths.BASE_DIRECTORY))
                            .inheritIO()
                            .start()
                            .waitFor();
            if (ev != 0) {
                System.err.println("Error: exit value " + ev);
                return true;
            }
        }
        return false;
    }
}
