package co.paralleluniverse.examples.comsatjetty;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class CascadingFailureServer {
    private static final int THREAD_COUNT = 200;

    public static void main(String[] args) throws Exception {
        int threads = args.length>0 ? parseInt(args[0], THREAD_COUNT):THREAD_COUNT;
        final BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(threads, threads, 9999);
        QueuedThreadPool queuedThreadPool = new QueuedThreadPool(threads, threads, 60000, queue);

        final Server server = new Server(queuedThreadPool);
//        final Server server = new Server(8080);
        ServerConnector http = new ServerConnector(server);
        http.setPort(8080);
        http.setIdleTimeout(30000);
        http.setAcceptQueueSize(9999);
        server.addConnector(http);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        server.setHandler(context);
        final String TARGET_URL = "http://localhost:8080/target?sleep=";

        context.addServlet(new ServletHolder(new HttpServlet() {
//            final Client httpClient = ClientBuilder.newClient();

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//                try (PrintWriter out = resp.getWriter()) {
//                    out.println("call: " + httpClient.target(TARGET_URL + req.getParameter("sleep")).request().get().readEntity(String.class));
//                }
                try (PrintWriter out = resp.getWriter()) {
                    Thread.sleep(parseInt(req.getParameter("sleep"), 10));
                    out.println("answer: ok " + new Date().getTime());
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }

            }
        }), "/regular");
        context.addServlet(new ServletHolder(new FiberHttpServlet() {
//            final Client httpClient = AsyncClientBuilder.newClient();

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
                try (PrintWriter out = resp.getWriter()) {
//                try {
                    Fiber.sleep(parseInt(req.getParameter("sleep"), 10));
                    out.println("answer: ok " + new Date().getTime());
//                    out.println("call: " + httpClient.target(TARGET_URL + req.getParameter("sleep")).request().get().readEntity(String.class));
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }), "/fiber");
        context.addServlet(new ServletHolder(new FiberHttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
                try (PrintWriter out = resp.getWriter()) {
                    out.println("do nothing. " + new Date().getTime());
                }
            }
        }), "/simple");
        context.addServlet(new ServletHolder(new FiberHttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
                try (PrintWriter out = resp.getWriter()) {
                    int sleeptime = parseInt(req.getParameter("sleep"), 10);
                    out.println("sleeping " + sleeptime + "ms starting now: " + new Date().getTime() + " \n");
                    Fiber.sleep(sleeptime);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }), "/target");
        context.addServlet(new ServletHolder(new FiberHttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
                try {
                    server.stop();
                    System.exit(0);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }), "/shutdown");

        server.start();
        System.out.println("http://localhost:8080/regular");
        System.out.println("http://localhost:8080/fiber");
        System.out.println("http://localhost:8080/simple");
        System.out.println("http://localhost:8080/shutdown");
        server.join();
    }

    static int parseInt(String str, int defaultVal) {
        int val;
        try {
            val = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            val = defaultVal;
        }
        return val;
    }
}
