package uk.num.cmd;

import lombok.Value;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import uk.num.net.NumProtocolSupport;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;

/**
 * A class to run the NUM protocol command line client.
 */
public class Main {

    /**
     * Some exit strings
     */
    private static final Collection<String> exits = Arrays.asList("q", "quit", "exit", "done", "bye", "goodbye");

    /**
     * Main entry point
     *
     * @param args Use -help or -uri &lt;num-uri&gt;
     */
    public static void main(String[] args) {
        new Main().runClient(args);
    }

    /**
     * Main entry point for instances of Main
     *
     * @param args Use -help or -uri &lt;num-uri&gt;
     */
    private void runClient(final String[] args) {
        // Initialise the NUM protocol to add support to java.net.URL
        NumProtocolSupport.init();

        // Set up and check the command line arguments.
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
            // Parse the command line arguments
            final CommandLineParser parser = new DefaultParser();
            final CommandLine cmd;
            cmd = parser.parse(options, args);

            final boolean verbose = cmd.hasOption("verbose");

            // If a URI is supplied then we just run once, otherwise we're interactive
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

    /**
     * A command loop to accept URIs and fetch NUM records.
     *
     * @param cmd a CommandLine object
     */
    private void runInteractive(final CommandLine cmd) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        boolean shouldLoop = true;

        // Pre-load the cache
        Executors.newSingleThreadExecutor()
                .submit(() -> {
                    run("num.uk:1");
                    run("num.uk:3");
                    run("num.uk:4");
                    return null;
                });

        // Repeat until the user says quit
        do {
            try {
                System.out.print("Enter URI or Q[uit]> ");
                final String line = reader.readLine();
                if (!StringUtils.isBlank(line)) {
                    if (isExit(line)) {
                        shouldLoop = false;
                    } else {
                        runOnce(cmd, line, true);
                    }
                }
            } catch (final Exception e) {
                System.err.println(e.getMessage());
            }
        } while (shouldLoop);
    }

    /**
     * Check for a quit command
     *
     * @param line the user entry
     * @return true if we should quit
     */
    private boolean isExit(final String line) {
        return line != null && exits.contains(line.toLowerCase());
    }

    /**
     * Run for a single NUM URI
     *
     * @param cmd       a CommandLine object
     * @param uriString a NUM URI String
     * @param verbose   true if verbose messages are required.
     * @throws FileNotFoundException if we can't use the '-output file'
     */
    private void runOnce(final CommandLine cmd, final String uriString, final boolean verbose) throws
                                                                                               FileNotFoundException {

        if (verbose) {
            System.out.println("loading...");
        }

        PrintStream out = System.out;// Default to sdtout

        if (cmd.hasOption("output")) {
            out = new PrintStream(new FileOutputStream(new File(cmd.getOptionValue("output"))));
        }

        // Fetch the NUM record
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
        } else if (result.error != null) {
            System.err.println("No record available.");
        }
    }

    /**
     * Fetch a NUM record from a NUM URI
     *
     * @param urlString the NUM URI String
     * @return a Result object
     */
    private Result run(final String urlString) {
        try {
            final long start = System.currentTimeMillis();

            // Convert the URI String to a URL Object
            final URL url = NumProtocolSupport.toUrl(urlString);

            // Try to fetch the NUM record
            final String json = IOUtils.toString(url, StandardCharsets.UTF_8);

            // Check how long it took.
            final long end = System.currentTimeMillis();
            double time = (end - start) / 1000.0;

            return new Result(time, json, null);
        } catch (final Exception e) {
            return new Result(0.0D, null, e);
        }
    }

    /**
     * A class for the results of fetching a NUM record.
     */
    @Value
    private static class Result {

        double time;

        String json;

        Exception error;

    }

}
