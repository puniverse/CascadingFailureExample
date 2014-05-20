package co.paralleluniverse.examples.comsatjetty;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
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

    public static void main(String[] args) throws IOException {
                final MetricRegistry metrics = new MetricRegistry();
        ConsoleReporter.forRegistry(metrics).build().start(1, TimeUnit.SECONDS);
        Counter connectedCounter = metrics.counter("connected");

        String host = args.length>0 ? args[0] : "127.0.0.1";
        int rate = args.length>1 ? Integer.parseInt(args[1]) : 10;
        int micros = (int) (1e6 / rate);
        
        try (Selector selector = Selector.open()) {
            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                try {
                    SocketChannel channel;
                    channel = SocketChannel.open();
                    channel.configureBlocking(false);

                    channel.register(selector, SelectionKey.OP_CONNECT);
                    channel.connect(new InetSocketAddress(host , 8080));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 0, micros, TimeUnit.MICROSECONDS);

            while (!Thread.interrupted()) {

                selector.select();

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid())
                        continue;

                    if (key.isConnectable()) {
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

