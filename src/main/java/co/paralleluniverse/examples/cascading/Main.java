package co.paralleluniverse.examples.cascading;

import org.apache.commons.cli.*;

public class Main {
    public static final int TIMEOUT = 30000;
    public static final int MAX_CONN = 120000;
    public static final int PORT = 8080;
    public static final String SERVICE_URL = "http://localhost:8080/internal/foo?sleep=";

    private static final String PARAM_JERSEY_PACKAGES = "jersey.config.server.provider.packages";
    private static final Class JERSEY_SERVLET = org.glassfish.jersey.servlet.ServletContainer.class;
    private static final Class JERSEY_FIBER_SERVLET = co.paralleluniverse.fibers.jersey.ServletContainer.class;
    private static final String PACKAGE_NAME_PREFIX = Main.class.getPackage().getName() + ".";

    public static void main(String[] args) throws Exception {
        final Options options = new Options()
                .addOption("threads", true, "number of threads")
                .addOption("server", true, "jetty/tomcat/undertow")
                .addOption("fibers", false, "use fibers")
                .addOption("help", false, "print help");
        final CommandLine cmd = new BasicParser().parse(options, args);

        if (cmd.hasOption("help")) {
            new HelpFormatter().printHelp("run.sh server [options]", options);
            return;
        }

        final String serverType = cmd.getOptionValue("server", "jetty").toLowerCase();
        final int threads = Integer.parseInt(cmd.getOptionValue("threads", Integer.toString(200)));
        final boolean useFibers = cmd.hasOption("fibers");

        System.out.println("Serving with " + serverType + " with " + threads + " IO threads - " + (useFibers ? "USING" : "NOT USING") + " FIBERS");
        System.out.println("http://localhost:8080/api/service?sleep=5000");

        final EmbeddedServer server;
        switch (serverType) {
            case "jetty":
                server = new JettyServer();
                break;
            case "tomcat":
                server = new TomcatServer();
                break;
            case "undertow":
                server = new UndertowServer();
                break;
            default:
                System.err.println("Unknown server type '" + serverType + "'");
                new HelpFormatter().printHelp("run.sh server [options]", options);
                return;
        }

        server.setPort(PORT).setMaxConnections(MAX_CONN).setNumThreads(threads);

        // install the Jersey services as our servlets
        server.addServlet("internal", JERSEY_FIBER_SERVLET, "/internal/*")
                .setInitParameter(PARAM_JERSEY_PACKAGES, PACKAGE_NAME_PREFIX + "internal")
                .setLoadOnStartup(1);
        server.addServlet("api", useFibers ? JERSEY_FIBER_SERVLET : JERSEY_SERVLET, "/api/*") // <-------------
                .setInitParameter(PARAM_JERSEY_PACKAGES, PACKAGE_NAME_PREFIX + (useFibers ? "fibers" : "plain"))
                .setLoadOnStartup(1);

        server.run();
    }
}
