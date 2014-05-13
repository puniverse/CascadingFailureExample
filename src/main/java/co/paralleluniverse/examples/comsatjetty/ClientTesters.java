package co.paralleluniverse.examples.comsatjetty;

import co.paralleluniverse.common.benchmark.StripedLongTimeSeries;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.IOReactorException;

public class ClientTesters {
    static final int MAX_CONN = 50000;
    static final int WARMUP = 3;
    static final int DURATION = 5;

    public static void main(String[] args) throws InterruptedException, IOReactorException, IOException {
        if (args.length < 4) {
            System.out.println("args are " + Arrays.toString(args));
            System.out.println("Usage: ClientTesters baseUrl servletPath sleepTime rate");
            System.out.println("Example:\n\tClientTesters http://localhost:8080 /regular 10 500");
//            args = new String[]{"http://46.137.129.107:8080", "/fiber", "5000", "10"};
            System.exit(0);
        }
        final ConcurrentHashMap<String, AtomicInteger> errors = new ConcurrentHashMap<>();
        final String HOST = args[0];
        final String URL1 = HOST + args[1] + "?sleep=" + args[2];
        final String URL2 = HOST + "/simple";
        final int rate = Integer.parseInt(args[3]);
        System.out.println("configuration: " + HOST + " " + URL1 + " " + rate);

        final ThreadFactory deamonTF = new ThreadFactoryBuilder().setDaemon(true).build();
        CloseableHttpAsyncClient ahc = createDefaultHttpAsyncClient();

        final AtomicInteger url1Errors = new AtomicInteger();
        final AtomicInteger url2Errors = new AtomicInteger();

        final StripedLongTimeSeries opendUrl1 = new StripedLongTimeSeries(100000, false);
        final StripedLongTimeSeries latUrl2 = new StripedLongTimeSeries(100000, false);

//        try (CloseableHttpAsyncClient ahc = HttpAsyncClientBuilder.create().setConnectionManager(mngr).build()) {
//        try (CloseableHttpAsyncClient ahc = HttpAsyncClientBuilder.create().setMaxConnPerRoute(9999).setMaxConnTotal(9999).build()) {
        try (CloseableHttpClient client = new FiberHttpClient(ahc)) {

            System.out.println("warming up..");
            call(new HttpGet(URL1), 100, 3, null, null, null, MAX_CONN, client, deamonTF).await();

            System.out.println("starting..");
            final long start = System.nanoTime();
            CountDownLatch latch1 = call(new HttpGet(URL1), rate, DURATION, opendUrl1, null, errors, MAX_CONN, client, deamonTF);
            call(new HttpGet(URL2), 5, DURATION, null, latUrl2, errors, MAX_CONN, client, deamonTF).await();
            latch1.await();

            latUrl2.getRecords().forEach(rec -> System.out.println("url2_lat " + TimeUnit.NANOSECONDS.toMillis(rec.timestamp - start) + " " + rec.value));
            errors.entrySet().stream().forEach(p -> {
                System.out.println(p.getKey() + " " + p.getValue());
            });
//            opendUrl1.getRecords().forEach(rec -> System.out.println("url1_cnt " + TimeUnit.NANOSECONDS.toMillis(rec.timestamp - start) + " " + rec.value));
        }
    }

    public static CloseableHttpAsyncClient createDefaultHttpAsyncClient() throws IOReactorException {
        DefaultConnectingIOReactor ioreactor = new DefaultConnectingIOReactor(IOReactorConfig.custom().
                setConnectTimeout(7000).
                setIoThreadCount(10).
                setSoTimeout(7000).
                build());
        PoolingNHttpClientConnectionManager mngr = new PoolingNHttpClientConnectionManager(ioreactor);
        mngr.setDefaultMaxPerRoute(MAX_CONN);
        mngr.setMaxTotal(MAX_CONN);
        CloseableHttpAsyncClient ahc = HttpAsyncClientBuilder.create().setConnectionManager(mngr).build();
        return ahc;
    }

    private static CountDownLatch call(
            final HttpGet httpGet, final int rate, int duration, final StripedLongTimeSeries openedCountSeries, final StripedLongTimeSeries latancySeries,
            final ConcurrentHashMap<String, AtomicInteger> errorsCounters, final int maxOpen, final CloseableHttpClient client, final ThreadFactory deamonTF) {
        int num = duration * rate;
        int tenth = num / 10;
        CountDownLatch cdl = new CountDownLatch(num);
        Semaphore sem = new Semaphore(maxOpen);
        Thread url1Thread = deamonTF.newThread(() -> {
            final RateLimiter rl = RateLimiter.create(rate);

            for (int i = 0, j = 0; i < num; i++) {
                rl.acquire();
                if (openedCountSeries != null)
                    openedCountSeries.record(System.nanoTime(), maxOpen - sem.availablePermits());
                if (sem.availablePermits() == 0)
                    System.out.println("waiting...");
                sem.acquireUninterruptibly();
                if (openedCountSeries != null && i % tenth == 0)
                    System.out.println("sent " + i / tenth * 10 + "%...");

//                if (openedCountSeries != null)
//                    System.out.println(">" + (maxOpen - sem.availablePermits()));
                new Fiber<Void>(() -> {
                    long reqStart = System.nanoTime();
                    try (CloseableHttpResponse resp = client.execute(httpGet)) {
                        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - reqStart);
                        if (resp.getStatusLine().getStatusCode() == 200) {
//                            if (millis > 1000)
//                                System.out.println("millis " + millis);
                            if (latancySeries != null)
                                latancySeries.record(reqStart, millis);
                        }
                    } catch (IOException ex) {
                        if (errorsCounters != null) {
                            errorsCounters.putIfAbsent(ex.getClass().getName(), new AtomicInteger());
                            errorsCounters.get(ex.getClass().getName()).incrementAndGet();
                        }
                    } finally {
                        sem.release();
                        cdl.countDown();
                        if (openedCountSeries != null && (num - cdl.getCount()) % tenth == 0)
//                            System.out.println("dd "+ (num-cdl.getCount()) + " : "+ tenth + " : "+(num-cdl.getCount()% tenth));
                            System.out.println("responeded " + ((num - cdl.getCount()) / tenth * 10) + "%...");
//                        }
//                        if (openedCountSeries != null)
//                            System.out.println("<" + (maxOpen - sem.availablePermits()));
                    }
                }).start();
            }
        });
        url1Thread.start();
        return cdl;
    }
}
