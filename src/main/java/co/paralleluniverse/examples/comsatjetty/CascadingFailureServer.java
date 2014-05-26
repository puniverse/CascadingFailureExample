package co.paralleluniverse.examples.comsatjetty;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.RequestLimit;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import java.io.File;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
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

public class CascadingFailureServer {
    private static final int THREAD_COUNT_DEFAULT = 200;
    static final int TIMEOUT = 30000;
    static final int MAX_CONN = 120000;
    private static final int PORT = 8080;

    static final String SERVICE_URL = "http://localhost:8080/target?sleep=";

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("threads", true, "number of threads");
        options.addOption("type", true, "jetty/tomcat/undertow");
        options.addOption("help", false, "print help");
        try {
            CommandLine cmd = new BasicParser().parse(options, args);
            if (cmd.hasOption("help"))
                printUsageAndExit(options);
            String type = cmd.getOptionValue("type", "jetty");
            int threads = Integer.parseInt(cmd.getOptionValue("threads", Integer.toString(THREAD_COUNT_DEFAULT)));
            System.out.println("Serving with " + type + " using " + threads + " threads....");
            System.out.println("http://localhost:8080/regular?sleep=5000&callService=true");
            System.out.println("http://localhost:8080/fiber?sleep=5000&callService=true");
            switch (type) {
                case "jetty":
                    createJettyServer(threads);
                    break;
                case "tomcat":
                    createTomcatServer(threads);
                    break;
                case "undertow":
                    createUndertowServer(threads);
                    break;
                    default:
                        System.err.println("Unknown server type '"+type+"'");
                        printUsageAndExit(options);
            }
        } catch (ParseException ex) {
            System.err.println("Parsing failed.  Reason: " + ex.getMessage());
        }
    }

    private static void createJettyServer(int threads) throws Exception {
        final Server server = new Server(new QueuedThreadPool(threads, threads));
        ServerConnector http = new ServerConnector(server);
        http.setPort(PORT);
        http.setAcceptQueueSize(MAX_CONN);
        server.addConnector(http);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        context.addServlet(new ServletHolder(RoutingServlet.class), "/regular");
        context.addServlet(new ServletHolder(FiberRoutingServlet.class), "/fiber");
        context.addServlet(new ServletHolder(TargetServlet.class), "/target");

        server.setHandler(context);
        server.start();
        server.join();
    }

    private static void createUndertowServer(int threads) throws ServletException {
        final DeploymentManager servletsContainer = Servlets.defaultContainer().addDeployment(
                Servlets.deployment()
                .setClassLoader(ClassLoader.getSystemClassLoader())
                .setContextPath("/")
                .setDeploymentName("")
                .addServlets(
                        Servlets.servlet("target", TargetServlet.class).addMapping("/target").setAsyncSupported(true),
                        Servlets.servlet("fiber", FiberRoutingServlet.class).addMapping("/fiber").setAsyncSupported(true),
                        Servlets.servlet("regular", RoutingServlet.class).addMapping("/regular")));
        servletsContainer.deploy();
        Undertow server = Undertow.builder()
                .setIoThreads(threads)
                .addHttpListener(PORT, "localhost")
                .setHandler(Handlers.requestLimitingHandler(new RequestLimit(MAX_CONN), servletsContainer.start()))
                .build();
        server.start();
    }

    private static void createTomcatServer(int threads) throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.getConnector().setAttribute("maxThreads", threads);
        tomcat.getConnector().setAttribute("acceptCount", MAX_CONN);
        Context rootCtx = tomcat.addContext("/", new File(System.getProperty("java.io.tmpdir")).getAbsolutePath());
        tomcatAddServlet(rootCtx, TargetServlet.class, "/target");
        tomcatAddServlet(rootCtx, RoutingServlet.class, "/regular");
        tomcatAddServlet(rootCtx, FiberRoutingServlet.class, "/fiber");
        tomcat.start();
        tomcat.getServer().await();
    }

    private static void tomcatAddServlet(Context rootCtx, Class<? extends Servlet> cls, String mapping) {
        Tomcat.addServlet(rootCtx, cls.getSimpleName(), cls.getName());
        rootCtx.addServletMapping(mapping, cls.getSimpleName());
    }

    private static void printUsageAndExit(Options options) {
        new HelpFormatter().printHelp("run.sh server [options]", options);
        System.exit(0);
    }
}
