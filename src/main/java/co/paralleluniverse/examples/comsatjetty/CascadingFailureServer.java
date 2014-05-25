package co.paralleluniverse.examples.comsatjetty;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class CascadingFailureServer {
    private static final int THREAD_COUNT = 200;
    public static final MetricRegistry metrics = new MetricRegistry();
    public static AtomicInteger ai = new AtomicInteger();
    final static String SERVICE_URL = "http://localhost:8080/target?sleep=";

    public static void main(String[] args) throws Exception {
        System.out.println("Selector: " + SelectorProvider.provider());
        ConsoleReporter.forRegistry(metrics).build().start(2, TimeUnit.SECONDS);
        int threads = args.length > 0 ? Integer.parseInt(args[0], THREAD_COUNT) : THREAD_COUNT;
        System.out.println("Serving using " + threads + " threads....");
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        Server server = createJettyServer(threads, context);
        FiberHttpClient fiberHttpClient = new FiberHttpClient(ClientTesters.createDefaultHttpAsyncClient(null));

        ResponseHandler<String> handler = new BasicResponseHandler();

        context.addServlet(new ServletHolder(RoutingServlet.class), "/regular");
        context.addServlet(new ServletHolder(FiberRoutingServlet.class), "/fiber");
        context.addServlet(new ServletHolder(new TagetServlet()), "/target");

        server.start();
        System.out.println("http://localhost:8080/regular");
        System.out.println("http://localhost:8080/fiber");
        System.out.println("http://localhost:8080/simple");
        System.out.println("http://localhost:8080/shutdown");
        server.join();
    }

    private static Server createJettyServer(int threads, ServletContextHandler context) {
//        final BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(10, 100, threads);
//        QueuedThreadPool queuedThreadPool = new QueuedThreadPool(threads, 10, 60000, queue);
        final Timer execute = metrics.timer("execute");
        final Timer jobrun = metrics.timer("jobrun");

        // Use this in order to measure delay times in the queue
        final QueuedThreadPool queuedThreadPool = new QueuedThreadPool(threads) {
//            AtomicInteger ai2 = new AtomicInteger();
//
//            @Override
//            public void execute(Runnable job) {
//                Timer.Context executeCtx = execute.time();
//                super.execute(() -> {
//                    Timer.Context runCtx = jobrun.time();
//                    job.run();
//                    try {
//                        if (job.getClass().getName().equals("org.eclipse.jetty.io.AbstractConnection$2")) {
//                            Field f = job.getClass().getDeclaredField("this$0");
//                            if (f != null && f.getType().isAssignableFrom(HttpConnection.class)) {
//                                f.setAccessible(true);
//                                HttpConnection conn = (HttpConnection) f.get(job);
//                                String pathInfo = conn.getHttpChannel().getRequest().getPathInfo();
//                                if ("/target".equals(pathInfo)) {
//                                    executeCtx.stop();
//                                    runCtx.stop();
//                                }
//                            }
//                        }
//                    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
//                        Logger.getLogger(CascadingFailureServer.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                });
//            }
        };

        metrics.register("waiting jobs", (Gauge<Integer>) () -> queuedThreadPool.getQueueSize());
        metrics.register("threads num", (Gauge<Integer>) () -> queuedThreadPool.getThreads());
        final Server server = new Server(queuedThreadPool);
        ServerConnector http = new ServerConnector(server);
        http.setPort(8080);
        http.setIdleTimeout(30000);
        http.setAcceptQueueSize(99999);
        server.addConnector(http);
        server.setHandler(context);
        return server;
    }

}
