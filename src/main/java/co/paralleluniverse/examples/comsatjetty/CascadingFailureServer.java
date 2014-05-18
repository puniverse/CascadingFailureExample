package co.paralleluniverse.examples.comsatjetty;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class CascadingFailureServer {
    private static final int THREAD_COUNT = 200;
    public static final MetricRegistry metrics = new MetricRegistry();
    public static AtomicInteger ai = new AtomicInteger();

    public static void main(String[] args) throws Exception {
//        ConsoleReporter.forRegistry(metrics).build().start(2, TimeUnit.SECONDS);
        int threads = args.length > 0 ? parseInt(args[0], THREAD_COUNT) : THREAD_COUNT;
        System.out.println("Serving using " + threads + " threads....");
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        Server server = createServer(threads, context);
        FiberHttpClient fiberHttpClient = new FiberHttpClient(ClientTesters.createDefaultHttpAsyncClient());

        CloseableHttpClient httpClient = HttpClients.custom().
                setMaxConnPerRoute(99999).
                setMaxConnTotal(99999).
                setDefaultRequestConfig(RequestConfig.custom().
                        setConnectTimeout(7000).
                        setSocketTimeout(7000).
                        setConnectionRequestTimeout(7000).build()).
                build();

        ResponseHandler<String> handler = new BasicResponseHandler();
        final String TARGET_URL = "http://localhost:8080/target?sleep=";

        context.addServlet(new ServletHolder(new HttpServlet() {
//            final Client httpClient = ClientBuilder.newClient();

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                try (PrintWriter out = resp.getWriter()) {

                    out.print("call: " + httpClient.execute(new HttpGet(TARGET_URL + req.getParameter("sleep")), handler));
//                }
//                try (PrintWriter out = resp.getWriter()) {
//                    Thread.sleep(parseInt(req.getParameter("sleep"), 10));
//                    out.println("answer: ok " + new Date().getTime());
//                } catch (InterruptedException ex) {
//                    throw new RuntimeException(ex);
                }

            }
        }), "/regular");
        context.addServlet(new ServletHolder(new FiberHttpServlet() {
//            final Client httpClient = AsyncClientBuilder.newClient();

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
                try (PrintWriter out = resp.getWriter()) {
                    out.print("call: " + fiberHttpClient.execute(new HttpGet(TARGET_URL + req.getParameter("sleep")), handler));
////                try {
//                    Fiber.sleep(parseInt(req.getParameter("sleep"), 10));
//                    out.println("answer: ok " + new Date().getTime());
//                    out.println("call: " + httpClient.target(TARGET_URL + req.getParameter("sleep")).request().get().readEntity(String.class));
//                } catch (InterruptedException ex) {
//                    throw new RuntimeException(ex);
                }
            }
        }), "/fiber");
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                try (PrintWriter out = resp.getWriter()) {
                    out.println("do nothing. " + new Date().getTime());
                }
            }
        }), "/simple");
        context.addServlet(new ServletHolder(new FiberHttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
                int get = ai.incrementAndGet();
                if (get%100==0)
                    System.out.println(new Date()+": "+get);
                try {//(PrintWriter out = resp.getWriter()) {
                    int sleeptime = parseInt(req.getParameter("sleep"), 10);
                    Fiber.sleep(sleeptime);
//                    out.println("sleeping " + sleeptime + "ms starting now: " + new Date().getTime() + " \n");
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

    private static Server createServer(int threads, ServletContextHandler context) {
//        final BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(10, 100, threads);
//        QueuedThreadPool queuedThreadPool = new QueuedThreadPool(threads, 10, 60000, queue);
        final Timer execute = metrics.timer("execute");
//        final Timer jobrun = metrics.timer("jobrun");

        final QueuedThreadPool queuedThreadPool = new QueuedThreadPool(threads) {
            AtomicInteger ai2 = new AtomicInteger();

            @Override
            public void execute(Runnable job) {
//                StringBuilder sb = new StringBuilder();
//                sb.append("exec:").append('\n');                
//                Arrays.asList(Thread.currentThread().getStackTrace()).stream().forEach(st->sb.append("\t").append(st.toString()).append('\n'));
//                final long start = System.nanoTime();
//                System.out.println(job.getClass().getName());
//                new UnsupportedOperationException().printStackTrace();
                final long start = System.nanoTime();
//                int get = ai2.incrementAndGet();
//                if (get%2000==0)
//                    System.out.println("2: "+new Date()+": "+get);
                super.execute(() -> {
                    if (ai2.incrementAndGet() % 1000 == 0)
                        System.out.println("2: "+new Date()+" "+ai2+" "+TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
                    job.run();
                });
            }

        };

//        metrics.register("waiting jobs", (Gauge<Integer>) () -> queuedThreadPool.getQueueSize());
//        metrics.register("threads num", (Gauge<Integer>) () -> queuedThreadPool.getThreads());
        final Server server = new Server(queuedThreadPool);
        ServerConnector http = new ServerConnector(server);
        http.setPort(8080);
        http.setIdleTimeout(30000);
        http.setAcceptQueueSize(99999);
        server.addConnector(http);
        server.setHandler(context);
//        
//        // Setup JMX
//        MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
//        server.addEventListener(mbContainer);
////        server.getContainer().addEventListener(mbContainer);
//        server.addBean(mbContainer);
        return server;
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
