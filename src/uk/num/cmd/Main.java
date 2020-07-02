package uk.num.cmd;

import lombok.Value;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import uk.num.net.NumProtocolSupport;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

public class Main {

    private static final Collection<String> exits = Arrays.asList("q", "quit", "exit", "done", "bye", "goodbye");

    public static void main(String[] args) {
        new Main().runClient(args);
    }

    private void runClient(final String[] args) {
        NumProtocolSupport.init();

        final HelpFormatter formatter = new HelpFormatter();
        final Options options = new Options();

        final Option uri = new Option("uri", true, "a NUM URI, e.g. num.uk:1");
        uri.setRequired(false);
        options.addOption(uri);

        final Option verboseOption = new Option("verbose", "verbose messages");
        verboseOption.setRequired(false);
        options.addOption(verboseOption);

        final Option outputFileOption = new Option("output", true, "output file");
        outputFileOption.setRequired(false);
        options.addOption(outputFileOption);

        final Option help = new Option("help", "A valid NUM URI is of the form\n\n" +
                "- num://numexample.com:1\n" +
                "- num://jo.smith@numexample.com:1\n" +
                "- num://jo.smith@numexample.com:1/work\n" +
                "- num://jo.smith@numexample.com:1/personal\n" +
                "- num://jo.smith@numexample.com:1/hobbies\n" +
                "- num://numexample.com:1/support\n" +
                "- num://numexample.com:1/support/website\n" +
                "- num://numexample.com:1/support/delivery\n" +
                "- num://numexample.com:1/enquiries\n" +
                "- num://numexample.com:1/sales\n" +
                "- num://numexample.com:1\n" +
                "\n" +
                "The protocol can be omitted, and the module defaults to 0 if not specified.");
        help.setRequired(false);
        options.addOption(help);

        try {
            final CommandLineParser parser = new DefaultParser();
            final CommandLine cmd;

            cmd = parser.parse(options, args);

            final boolean verbose = cmd.hasOption("verbose");
            if (cmd.hasOption("uri")) {
                final String uriString = cmd.getOptionValue("uri");
                runOnce(cmd, uriString, verbose);
            } else {
                runInteractive(cmd);
            }
        } catch (ParseException | FileNotFoundException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("java -jar num-cmd-line-client.jar -uri <NUM URI>", options);

            System.exit(1);
        }
    }

    private void runInteractive(final CommandLine cmd) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        boolean shouldLoop = true;
        do {
            try {
                System.out.print("Enter URI or Q[uit]> ");
                final String line = reader.readLine();
                System.out.println();
                if (isExit(line)) {
                    shouldLoop = false;
                } else {
                    runOnce(cmd, line, true);
                }
            } catch (final Exception e) {
                System.err.println(e.getMessage());
                shouldLoop = false;
            }
        } while (shouldLoop);
    }

    private boolean isExit(final String line) {
        return line != null && exits.contains(line.toLowerCase());
    }

    private void runOnce(final CommandLine cmd, final String uriString, final boolean verbose) throws
                                                                                               FileNotFoundException {

        if (verbose) {
            System.out.println("loading...");
        }

        PrintStream out = System.out;
        if (cmd.hasOption("output")) {
            out = new PrintStream(new FileOutputStream(new File(cmd.getOptionValue("output"))));
        }

        final Result result = run(uriString);
        if (result.json != null) {
            out.println(result.json);

            if (cmd.hasOption("output")) {
                out.close();
            }

            if (verbose) {
                System.out.printf("Took  : %.3fs%n", result.time);
                System.out.println("Done.");
            }
        }
    }

    private Result run(final String urlString) {
        try {
            final long start = System.currentTimeMillis();
            final URL url = NumProtocolSupport.toUrl(urlString);
            final String json = IOUtils.toString(url, StandardCharsets.UTF_8);
            final long end = System.currentTimeMillis();
            double time = (end - start) / 1000.0;

            return new Result(time, json, null);
        } catch (final Exception e) {
            return new Result(0.0D, null, e);
        }
    }

    @Value
    private static class Result {

        double time;

        String json;

        Exception error;

    }

}
