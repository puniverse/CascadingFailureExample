package co.paralleluniverse.examples.comsatjetty;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactorException;

public class NewClass {
    public static void main(String[] args) throws IOReactorException, IOException, InterruptedException {
        Registry<SchemeIOSessionStrategy> reg = RegistryBuilder.<SchemeIOSessionStrategy>create()
                .register("http", NoopIOSessionStrategy.INSTANCE).build();
        PoolingNHttpClientConnectionManager manager = new PoolingNHttpClientConnectionManager(
                new DefaultConnectingIOReactor(IOReactorConfig.DEFAULT
        ),reg);
        manager.setDefaultMaxPerRoute(9999);
        manager.setMaxTotal(9999);
        CloseableHttpAsyncClient ahc = HttpAsyncClientBuilder.create().
                setMaxConnPerRoute(9999).
                setMaxConnTotal(9999).
                setConnectionManager(manager).
//                                        setIoThreadCount(50).
//                                        setConnectTimeout(7000).
//                                        setSoTimeout(7000).
////                                        setTcpNoDelay(true).
//                                        build()))).
                build();
        ahc.start();
        final int num = 4;
        CountDownLatch cdl = new CountDownLatch(num);
        long last, now;
        last = System.nanoTime();
        HttpGet httpGet = new HttpGet("http://localhost:8080/fiber?sleep=2000");
        for (int i = 0; i < num; i++) {

            ahc.execute(httpGet, new FutureCallback<HttpResponse>() {

                @Override
                public void completed(HttpResponse t) {
                    cdl.countDown();
                }

                @Override
                public void failed(Exception excptn) {
                    cdl.countDown();
                }

                @Override
                public void cancelled() {
                    cdl.countDown();
                }
            });

        }
        now = System.nanoTime();
        System.out.println(TimeUnit.NANOSECONDS.toMillis(now - last));
        last = now;

        cdl.await(30, TimeUnit.SECONDS);
        System.out.println("cdl " + cdl.getCount());
        now = System.nanoTime();
        System.out.println(TimeUnit.NANOSECONDS.toMillis(now - last));
        ahc.close();
    }

}
