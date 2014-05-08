package co.paralleluniverse.examples.comsatjetty;

import co.paralleluniverse.common.benchmark.StripedLongTimeSeries;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.util.Arrays;
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
import org.apache.http.nio.reactor.IOReactorExceptionHandler;

public class ClientTesters {
    static final int MAX_CONN = 9000;
    static final int WARMUP = 3;
    static final int DURATION = 10;

    public static void main(String[] args) throws InterruptedException, IOReactorException, IOException {
        if (args.length < 4) {
            System.out.println("args are " + Arrays.toString(args));
            System.out.println("Usage: ClientTesters baseUrl servletPath sleepTime rate");
            System.out.println("Example:\n\tClientTesters http://localhost:8080 /regular 10 500");
            System.exit(0);
        }
        final String HOST = args[0];
        final String URL1 = HOST + args[1] + "?sleep=" + args[2];
        final String URL2 = HOST + "/simple";
        final int rate = Integer.parseInt(args[3]);
        System.out.println("configuration: " + HOST + " " + URL1 + " " + rate);

        final ThreadFactory deamonTF = new ThreadFactoryBuilder().setDaemon(true).build();
        DefaultConnectingIOReactor ioreactor = new DefaultConnectingIOReactor(IOReactorConfig.custom().
                setConnectTimeout(7000).
                setIoThreadCount(10).
                setSoTimeout(7000).
                build()){
                    
                    @Override
                    public void shutdown() throws IOException {
                        System.out.println("Hiiiiii");
                        Thread.currentThread().dumpStack();
                        super.shutdown(); //To change body of generated methods, choose Tools | Templates.
                    }
                    
                };
        ioreactor.setExceptionHandler(new IOReactorExceptionHandler() {

            @Override
            public boolean handle(IOException ex) {
                ex.printStackTrace();
                return true;
            }

            @Override
            public boolean handle(RuntimeException ex) {
                ex.printStackTrace();
                return true;
            }
        });
        PoolingNHttpClientConnectionManager mngr = new PoolingNHttpClientConnectionManager(ioreactor);
        mngr.setDefaultMaxPerRoute(9999);
        mngr.setMaxTotal(9999);

        final AtomicInteger url1Errors = new AtomicInteger();
        final AtomicInteger url2Errors = new AtomicInteger();

        final StripedLongTimeSeries opendUrl1 = new StripedLongTimeSeries(100000, false);
        final StripedLongTimeSeries latUrl2 = new StripedLongTimeSeries(100000, false);

        System.out.println("starting");
        final long start = System.nanoTime();
        try (CloseableHttpAsyncClient ahc = HttpAsyncClientBuilder.create().setConnectionManager(mngr).build()) {
//        try (CloseableHttpAsyncClient ahc = HttpAsyncClientBuilder.create().setMaxConnPerRoute(9999).setMaxConnTotal(9999).build()) {
            CloseableHttpClient client = new FiberHttpClient(ahc);
            
            //WARMUP
            call(new HttpGet(URL1), 100, 3, null, null, new AtomicInteger(), MAX_CONN, client, deamonTF).await();
            
            CountDownLatch latch1 = call(new HttpGet(URL1), rate, DURATION, opendUrl1, null, url1Errors, MAX_CONN, client, deamonTF);
            call(new HttpGet(URL2), 5, DURATION, null, latUrl2, url1Errors, MAX_CONN, client, deamonTF).await();
            latch1.await();
            
            System.out.println("url1Errors " + url1Errors);
            System.out.println("url2Errors " + url2Errors);
            latUrl2.getRecords().forEach(rec -> System.out.println("url2_lat " + TimeUnit.NANOSECONDS.toMillis(rec.timestamp - start) + " " + rec.value));
//            opendUrl1.getRecords().forEach(rec -> System.out.println("url1_cnt " + TimeUnit.NANOSECONDS.toMillis(rec.timestamp - start) + " " + rec.value));
        }
    }

    private static CountDownLatch call(
            final HttpGet httpGet, final int rate, int duration, final StripedLongTimeSeries openedCountSeries, final StripedLongTimeSeries latancySeries, final AtomicInteger errorsCounter, final int maxOpen, final CloseableHttpClient client, final ThreadFactory deamonTF) {
        int num = duration * rate;
        CountDownLatch cdl = new CountDownLatch(num);
        Semaphore sem = new Semaphore(maxOpen);
        Thread url1Thread = deamonTF.newThread(() -> {
            final RateLimiter rl = RateLimiter.create(rate);
            for (int i = 0; i < num; i++) {
                rl.acquire();
                if (openedCountSeries != null)
                    openedCountSeries.record(System.nanoTime(), maxOpen - sem.availablePermits());
                if (sem.availablePermits() == 0)
                    System.out.println("waiting...");
                sem.acquireUninterruptibly();
                new Fiber<Void>(() -> {
                    long reqStart = System.nanoTime();
                    try (CloseableHttpResponse resp = client.execute(httpGet)) {
                        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - reqStart);
                        if (resp.getStatusLine().getStatusCode() == 200) {
                            if (latancySeries != null)
                                latancySeries.record(reqStart, millis);
                        }
                    } catch (IOException ex) {
                        errorsCounter.incrementAndGet();
                        ex.printStackTrace();
                    } finally {
                        sem.release();
                        cdl.countDown();
                    }
                }).start();
            }
        });
        url1Thread.start();
        return cdl;
    }
}
