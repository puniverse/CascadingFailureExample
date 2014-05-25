package co.paralleluniverse.examples.comsatjetty;

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
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        Server server = createJettyServer(threads, context);
        context.addServlet(new ServletHolder(RoutingServlet.class), "/regular");
        context.addServlet(new ServletHolder(FiberRoutingServlet.class), "/fiber");
        context.addServlet(new ServletHolder(TagetServlet.class), "/target");

        server.start();
        System.out.println("http://localhost:8080/regular?sleep=5000&callService=true");
        System.out.println("http://localhost:8080/fiber?sleep=5000&callService=true");
        server.join();
    }

    private static Server createJettyServer(int threads, ServletContextHandler context) {
        final Server server = new Server(new QueuedThreadPool(threads, threads));
        ServerConnector http = new ServerConnector(server);
        http.setPort(8080);
        http.setAcceptQueueSize(MAX_CONN);
        server.addConnector(http);
        server.setHandler(context);
        return server;
    }
}
