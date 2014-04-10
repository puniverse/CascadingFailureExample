package co.paralleluniverse.examples.comsatjetty;

import co.paralleluniverse.common.benchmark.StripedLongTimeSeries;
import co.paralleluniverse.concurrent.util.ThreadUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.ws.rs.client.AsyncClientBuilder;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class ClientTesters {

    public static void main(String[] args) throws InterruptedException {
        if (args.length<3){
            System.out.println("Usage: ClientTesters http://localhost:8080 /regular?sleep=10 500");
            System.exit(0);
        }
        final String HOST = args[0];
        final String URL1 = HOST+args[1];
        final String URL2 = HOST+"/simple";
        final int REQ_PER_SEC = Integer.parseInt(args[2]);
        final int DURATION = 10;
        final int MAX_URL1_OPEN_CONNECTIONS = 500;
        final ThreadFactory deamonTF = new ThreadFactoryBuilder().setDaemon(true).build();

        final Client newClient = AsyncClientBuilder.newClient();
        final AtomicInteger url1Counter = new AtomicInteger();
        final AtomicInteger url2Counter = new AtomicInteger();
        final Semaphore sem = new Semaphore(MAX_URL1_OPEN_CONNECTIONS);
        final StripedLongTimeSeries stsOenURL1 = new StripedLongTimeSeries(100000, false);
        final StripedLongTimeSeries stsLatancyURL2 = new StripedLongTimeSeries(100000, false);

        System.out.println("starting");
        final long start = System.nanoTime();
        Thread url1Thread = deamonTF.newThread(() -> {
            final RateLimiter rl = RateLimiter.create(REQ_PER_SEC, 2, TimeUnit.SECONDS);
            for (long ct = start; ct < start + TimeUnit.SECONDS.toNanos(DURATION); ct = System.nanoTime()) {
                rl.acquire();
                stsOenURL1.record(ct, MAX_URL1_OPEN_CONNECTIONS - sem.availablePermits());
                if (sem.availablePermits() == 0)
                    System.out.println("waiting...");
                sem.acquireUninterruptibly();
                new Fiber<Void>(() -> {
                    try {
                        Response resp = newClient.target(URL1).request().buildGet().submit().get(5, TimeUnit.SECONDS);
                        if (resp.getStatus() == 200)
                            url1Counter.incrementAndGet();
                    } catch (ExecutionException | TimeoutException ex) {
                    } finally {
                        sem.release();
                    }
                }).start();
            }
        });
        Thread url2Thread = deamonTF.newThread(() -> {
            final RateLimiter rl = RateLimiter.create(5);
            for (long ct = start; ct < start + TimeUnit.SECONDS.toNanos(DURATION); ct = System.nanoTime()) {
                rl.acquire();
                new Fiber<Void>(() -> {
                    long reqStart = System.nanoTime();
                    try {
                        Response resp = newClient.target(URL2).request().buildGet().submit().get(5, TimeUnit.SECONDS);
                        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - reqStart);
                        if (resp.getStatus() == 200) 
                            url2Counter.incrementAndGet();
                        stsLatancyURL2.record(reqStart, millis);
                    } catch (ExecutionException | TimeoutException ex) {
                        stsLatancyURL2.record(reqStart, 5000);
                    }
                }).start();
            }
        });

        url1Thread.start();
        url2Thread.start();

        url1Thread.join();
        url2Thread.join();

        sem.acquire(MAX_URL1_OPEN_CONNECTIONS);

        System.out.println("finished " + url1Counter);
        System.out.println("finished " + url2Counter);
        stsLatancyURL2.getRecords().forEach(rec->System.out.println("url2_lat "+TimeUnit.NANOSECONDS.toMillis(rec.timestamp-start)+" "+rec.value));
        stsOenURL1.getRecords().forEach(rec->System.out.println("url1_cnt "+TimeUnit.NANOSECONDS.toMillis(rec.timestamp-start)+" "+ rec.value));
        ThreadUtil.dumpThreads();
    }
}
