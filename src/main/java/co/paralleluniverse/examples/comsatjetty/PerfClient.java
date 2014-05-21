package co.paralleluniverse.examples.comsatjetty;

import co.paralleluniverse.common.monitoring.SimpleReservoir;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.RateLimiter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PerfClient {
    static String message = "Sending a test message";

    public static void main(String[] args) throws IOException, InterruptedException {
        final MetricRegistry metrics = new MetricRegistry();
        ConsoleReporter.forRegistry(metrics).build().start(1, TimeUnit.SECONDS);
        Counter connectedCounter = metrics.counter("connected");
        Counter c1 = metrics.counter("c1");
//        Counter c2 = metrics.counter("c2");
        Counter c3 = metrics.counter("c3");
        Timer timer = metrics.register("con1", new Timer(new SimpleReservoir()));
        // Timer timer = metrics.timer("con1");
        
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        double rate = args.length > 1 ? Double.parseDouble(args[1]) : 2;
//        int micros = (int) (1e6 / rate);
        System.out.println("CONF: " + host + " " + rate);

        try (Selector selector = Selector.open()) {
            new Thread(() -> {
                RateLimiter rl = RateLimiter.create(rate);
                for (;;) {
                    try  {
                        rl.acquire();
                        SocketChannel channel = SocketChannel.open();
                        channel.configureBlocking(false);
                        
                        long start = System.nanoTime();
                        channel.register(selector, SelectionKey.OP_CONNECT);
//                        long middle = System.nanoTime();
                        long end = System.nanoTime();
                        channel.connect(new InetSocketAddress(host, 8080));
                        if (TimeUnit.NANOSECONDS.toMillis(end-start)>500)
                            c1.inc();
                    } catch (IOException e) {
                        e.printStackTrace();
//                    throw new RuntimeException(e);
                    }
                }

            }).start();
//            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
//                try (Timer.Context time = timer.time()) {
//
//                    SocketChannel channel = SocketChannel.open();
//                    channel.configureBlocking(false);
//                    channel.register(selector, SelectionKey.OP_CONNECT);
//                    channel.connect(new InetSocketAddress(host, 8080));
//                } catch (IOException e) {
//                    e.printStackTrace();
////                    throw new RuntimeException(e);
//                }
//            }, 0, micros, TimeUnit.MICROSECONDS);
            Thread.sleep(100);
            while (!Thread.interrupted()) {

                selector.select(1000);

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid())
                        continue;

                    if (key.isConnectable()) {
                        c3.inc();
//                            System.out.println("I am connected to the server");
//                            key.cancel();
                        SocketChannel channel = (SocketChannel) key.channel();
                        if (channel.isConnectionPending()) {
                            channel.finishConnect();
                        }
                        channel.configureBlocking(false);
                        connectedCounter.inc();
//            channel.register(selector, SelectionKey.OP_WRITE);
                    }
//                    if (key.isWritable()) {
//                        write(key);
//                    }
//                    if (key.isReadable()) {
//                        read(key);
//                    }
                }
            }
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer readBuffer = ByteBuffer.allocate(1000);
        readBuffer.clear();
        int length;
        try {
            length = channel.read(readBuffer);
        } catch (IOException e) {
            System.out.println("Reading problem, closing connection");
            key.cancel();
            channel.close();
            return;
        }
        if (length == -1) {
            System.out.println("Nothing was read from server");
            channel.close();
            key.cancel();
            return;
        }
        readBuffer.flip();
        byte[] buff = new byte[1024];
        readBuffer.get(buff, 0, length);
        System.out.println("Server said: " + new String(buff));
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.write(ByteBuffer.wrap(message.getBytes()));

        // lets get ready to read.
        key.interestOps(SelectionKey.OP_READ);
    }
}
