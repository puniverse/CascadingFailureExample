package co.paralleluniverse.examples.cascading;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.RequestLimit;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import java.io.File;
import javax.servlet.ServletException;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class Main {
    private static final int THREAD_COUNT_DEFAULT = 200;
    public static final int TIMEOUT = 30000;
    public static final int MAX_CONN = 120000;
    private static final int PORT = 8080;
    private static final String PARAM_JERSEY_PACKAGES = "jersey.config.server.provider.packages";
    private static final Class JERSEY_SERVLET = org.glassfish.jersey.servlet.ServletContainer.class;
    private static final Class JERSEY_FIBER_SERVLET = co.paralleluniverse.fibers.jersey.ServletContainer.class;
    private static final String PACKAGE_NAME_PREFIX = Main.class.getPackage().getName() + ".";

    public static final String SERVICE_URL = "http://localhost:8080/internal/foo?sleep=";

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("threads", true, "number of threads");
        options.addOption("server", true, "jetty/tomcat/undertow");
        options.addOption("fibers", false, "use fibers");
        options.addOption("help", false, "print help");
        try {
            CommandLine cmd = new BasicParser().parse(options, args);
            if (cmd.hasOption("help"))
                printUsageAndExit(options);
            final String server = cmd.getOptionValue("server", "jetty").toLowerCase();
            final int threads = Integer.parseInt(cmd.getOptionValue("threads", Integer.toString(THREAD_COUNT_DEFAULT)));
            final boolean useFibers = cmd.hasOption("fibers");

            System.out.println("Serving with " + server + " with " + threads + " IO threads - " + (useFibers ? "USING" : "NOT USING") + " FIBERS");
            System.out.println("http://localhost:8080/api/service?sleep=5000");
            switch (server) {
                case "jetty":
                    createJettyServer(useFibers, threads);
                    break;
                case "tomcat":
                    createTomcatServer(useFibers, threads);
                    break;
                case "undertow":
                    createUndertowServer(useFibers, threads);
                    break;
                default:
                    System.err.println("Unknown server type '" + server + "'");
                    printUsageAndExit(options);
            }
        } catch (ParseException ex) {
            System.err.println("Parsing failed.  Reason: " + ex.getMessage());
        }
    }

    private static void createJettyServer(boolean useFibers, int threads) throws Exception {
        final Server server = new Server(new QueuedThreadPool(threads, threads));
        ServerConnector http = new ServerConnector(server);
        http.setPort(PORT);
        http.setAcceptQueueSize(MAX_CONN);
        server.addConnector(http);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        ServletHolder sh1 = new ServletHolder(JERSEY_FIBER_SERVLET);
        sh1.setInitParameter(PARAM_JERSEY_PACKAGES, PACKAGE_NAME_PREFIX + "internal");
        context.addServlet(sh1, "/internal/*");

        ServletHolder sh2 = new ServletHolder(useFibers ? JERSEY_FIBER_SERVLET : JERSEY_SERVLET);
        sh2.setInitParameter(PARAM_JERSEY_PACKAGES, PACKAGE_NAME_PREFIX + (useFibers ? "fibers" : "plain"));
        context.addServlet(sh2, "/api/*");

        server.setHandler(context);
        server.start();
        server.join();
    }

    private static void createUndertowServer(boolean useFibers, int threads) throws ServletException {
        final DeploymentManager servletsContainer = Servlets.defaultContainer().addDeployment(
                Servlets.deployment()
                .setClassLoader(ClassLoader.getSystemClassLoader())
                .setContextPath("/")
                .setDeploymentName("")
                .addServlet(Servlets.servlet("internal", JERSEY_FIBER_SERVLET)
                        .addInitParam(PARAM_JERSEY_PACKAGES, PACKAGE_NAME_PREFIX + "internal")
                        .addMapping("/internal/*").setAsyncSupported(true))
                .addServlet(Servlets.servlet("service", useFibers ? JERSEY_FIBER_SERVLET : JERSEY_SERVLET)
                        .addInitParam(PARAM_JERSEY_PACKAGES, PACKAGE_NAME_PREFIX + (useFibers ? "fibers" : "plain"))
                        .addMapping("/api/*").setAsyncSupported(useFibers)));
        servletsContainer.deploy();
        Undertow server = Undertow.builder()
                .setIoThreads(threads)
                .addHttpListener(PORT, "localhost")
                .setHandler(Handlers.requestLimitingHandler(new RequestLimit(MAX_CONN), servletsContainer.start()))
                .build();
        
        server.start();
    }

    private static void createTomcatServer(boolean useFibers, int threads) throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.getConnector().setAttribute("maxThreads", threads);
        tomcat.getConnector().setAttribute("acceptCount", MAX_CONN);
        Context rootCtx = tomcat.addContext("/", new File(System.getProperty("java.io.tmpdir")).getAbsolutePath());

        Wrapper sh1 = Tomcat.addServlet(rootCtx, "internal", JERSEY_FIBER_SERVLET.getName());
        sh1.addInitParameter(PARAM_JERSEY_PACKAGES, PACKAGE_NAME_PREFIX + "internal");
        sh1.addMapping("/internal/*");
        
        Wrapper sh2 = Tomcat.addServlet(rootCtx, "service", useFibers ? JERSEY_FIBER_SERVLET.getName() : JERSEY_SERVLET.getName());
        sh2.addInitParameter(PARAM_JERSEY_PACKAGES, PACKAGE_NAME_PREFIX + (useFibers ? "fibers" : "plain"));
        sh2.addMapping("/api/*");
        
        tomcat.start();
        tomcat.getServer().await();
    }

    private static void printUsageAndExit(Options options) {
        new HelpFormatter().printHelp("run.sh server [options]", options);
        System.exit(0);
    }
}
