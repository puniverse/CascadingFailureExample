package co.paralleluniverse.examples.comsatjetty;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.examples.servlet.ServletServer;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RequestLimit;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import javax.servlet.ServletException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class CascadingFailureServer {
    private static final int THREAD_COUNT_DEFAULT = 200;
    static final int TIMEOUT = 30000;
    static final int MAX_CONN = 99999;
    static final String SERVICE_URL = "http://localhost:8080/target?sleep=";

    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : THREAD_COUNT_DEFAULT;
        System.out.println("Serving using " + threads + " threads....");
        System.out.println("http://localhost:8080/regular?sleep=5000&callService=true");
        System.out.println("http://localhost:8080/fiber?sleep=5000&callService=true");
        createUndertowServer(threads);
    }

    private static void createJettyServer(int threads) throws Exception {
        final Server server = new Server(new QueuedThreadPool(threads, threads));
        ServerConnector http = new ServerConnector(server);
        http.setPort(8080);
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
                .setClassLoader(ServletServer.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("")
                .addServlets(
                        Servlets.servlet("target", TargetServlet.class).addMapping("/target").setAsyncSupported(true),
                        Servlets.servlet("fiber", FiberRoutingServlet.class).addMapping("/fiber").setAsyncSupported(true),
                        Servlets.servlet("regular", RoutingServlet.class).addMapping("/regular")
                ));
        servletsContainer.deploy();
        Undertow server = Undertow.builder()
                .setIoThreads(threads)
                .addHttpListener(8080, "localhost")
                .setHandler(Handlers.requestLimitingHandler(new RequestLimit(MAX_CONN), servletsContainer.start()))
                .build();
        server.start();
    }
}
