package co.paralleluniverse.examples.comsatjetty;

import co.paralleluniverse.common.benchmark.StripedLongTimeSeries;
import co.paralleluniverse.concurrent.util.ThreadUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.ws.rs.client.AsyncClientBuilder;
import co.paralleluniverse.jersey.connector.JettyConnector;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.spi.RequestExecutorsProvider;
//import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;

public class TKBProblemWithAsyncJersy {

    public static void main(String[] args) throws InterruptedException, Exception {
        if (args.length < 4) {
            args = new String[]{"http://localhost:8080", "/regular", "2000", "1"};
        }
        final String HOST = args[0];
        final String URL1 = HOST + args[1] + "?sleep=" + args[2];
        final String URL2 = HOST + "/simple";
        final int REQ_PER_SEC = Integer.parseInt(args[3]);
        final int WARMUP = 0;//3;
        final int DURATION = 10 + WARMUP;
        final int MAX_URL1_OPEN_CONNECTIONS = 4096;
        System.out.println("configuration: " + HOST + " " + URL1 + " " + REQ_PER_SEC);
        final ThreadFactory deamonTF = new ThreadFactoryBuilder().setDaemon(true).build();
        final RequestExecutorsProvider singleThreadPoolReqExecProvider = new RequestExecutorsProvider() {
//        final RequestExecutorProvider singleThreadPoolReqExecProvider = new RequestExecutorProvider() {
            private ExecutorService tp = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("jersey-puniverse-single-worker-%d").build());

            @Override
            public ExecutorService getRequestingExecutor() {
                return tp;
            }
//
//            @Override
//            public void releaseRequestingExecutor(ExecutorService es) {
//            }
        };
//        final Client newClient = ClientBuilder.newClient();
        ClientConfig configuration = new ClientConfig();
//        configuration.register(singleThreadPoolReqExecProvider, RequestExecutorProvider.class);
//        configuration.property(ClientProperties.ASYNC_THREADPOOL_SIZE, 20);
        configuration.property(ClientProperties.CONNECT_TIMEOUT, 7000);
        configuration.connector(new JettyConnector(configuration));
        configuration.register(singleThreadPoolReqExecProvider, RequestExecutorsProvider.class);
//        configuration.property(ClientProperties.READ_TIMEOUT, 7000);
//        configuration.connectorProvider(new JettyConnectorProvider());
//        HttpClient jettyClient = new HttpClient();
//        jettyClient.setConnectTimeout(7000);
////        jettyClient.setMaxConnectionsPerDestination(5000);
//        jettyClient.setExecutor(new QueuedThreadPool(5));
////        jettyClient.setScheduler(new ScheduledExecutorScheduler("sched", true));
//        jettyClient.setMaxConnectionsPerDestination(1000);
//        System.out.println("cc1: "+jettyClient.getMaxConnectionsPerDestination());
//        System.out.println("cc2: "+jettyClient.getMaxRequestsQueuedPerDestination());
//        jettyClient.start();

        final Client newClient = ClientBuilder.newClient(configuration);
//        JettyConnectorProvider.getHttpClient(newClient).setMaxConnectionsPerDestination(1024);
        final AtomicInteger url1Counter = new AtomicInteger();
        final Semaphore sem = new Semaphore(MAX_URL1_OPEN_CONNECTIONS);
        final StripedLongTimeSeries stsOenURL1 = new StripedLongTimeSeries(100000, false);
        final StripedLongTimeSeries stsLatancyURL2 = new StripedLongTimeSeries(100000, false);

        System.out.println("starting");
        final long start = System.nanoTime();
        final RateLimiter rl = WARMUP > 0 ? RateLimiter.create(REQ_PER_SEC, WARMUP, TimeUnit.SECONDS)
                : RateLimiter.create(REQ_PER_SEC);
        int i = 0;
        for (long ct = start; ct < start + TimeUnit.SECONDS.toNanos(DURATION); ct = System.nanoTime()) {
            rl.acquire();
            stsOenURL1.record(ct, MAX_URL1_OPEN_CONNECTIONS - sem.availablePermits());
            if (sem.availablePermits() == 0)
                System.out.println("waiting...");
            sem.acquireUninterruptibly();
//            jettyClient.newRequest(URL1).send((Result result) -> {
//                sem.release();
//                if (result.isFailed())
//                    System.out.println(result.getFailure());
//                else {
//                    int counter = url1Counter.incrementAndGet();
////                    System.out.println("resp "+counter+": " + result.getResponse().getStatus());
//                }
//            });
            System.out.println("submitted "+(i++));

//            /*
//            new Fiber<Void>(() -> {
//                try {
//                    Response resp = newClient.target(URL1).request().buildGet().submit().get(7, TimeUnit.SECONDS);
//                    if (resp.getStatus() == 200)
//                        url1Counter.incrementAndGet();
//                } catch (ExecutionException | TimeoutException ex) {
//                    System.out.println(ex);
//                } finally {
//                    sem.release();
//                }
//            }).start();
            newClient.target(URL1).request().buildGet().submit(new InvocationCallback<Response>() {
                @Override
                public void completed(Response resp) {
                    if (resp.getStatus() == 200)
                        url1Counter.incrementAndGet();
                    else
                        System.out.println("res code "+resp.getStatus());
                    System.out.println("resp: " + resp.getStatus()+" "+System.nanoTime());
                    sem.release();
                }

                @Override
                public void failed(Throwable ex) {
                    ex.printStackTrace();
                    //System.out.println("url1exp: " + ex);
                    sem.release();
                }
            });
//            System.out.println("submitted " + System.nanoTime());
//             */
//                        Response resp = getInvokation.submit().get(7, TimeUnit.SECONDS);
        }
        sem.acquire(MAX_URL1_OPEN_CONNECTIONS);

        System.out.println(
                "finished " + url1Counter);
        ThreadUtil.dumpThreads();
//        stsLatancyURL2.getRecords().forEach(rec->System.out.println("url2_lat "+TimeUnit.NANOSECONDS.toMillis(rec.timestamp-start)+" "+rec.value));
//        stsOenURL1.getRecords().forEach(rec->System.out.println("url1_cnt "+TimeUnit.NANOSECONDS.toMillis(rec.timestamp-start)+" "+ rec.value));
    }

}
