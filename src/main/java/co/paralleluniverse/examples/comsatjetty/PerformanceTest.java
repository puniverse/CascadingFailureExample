package co.paralleluniverse.examples.comsatjetty;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PerformanceTest {

    public static void main(String[] args) throws IOException {
        final MetricRegistry metrics = new MetricRegistry();
        ConsoleReporter.forRegistry(metrics).build().start(1, TimeUnit.SECONDS);
        Counter acceptCounter = metrics.counter("accept");
        Counter readCounter = metrics.counter("READ");
        Counter dataCounter = metrics.counter("DATA");
//        Timer timer = metrics.timer("select");

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        Selector selector = Selector.open();

        ServerSocketChannel server1 = ServerSocketChannel.open();
        server1.configureBlocking(false);
        server1.socket().bind(new InetSocketAddress(8080));
        server1.register(selector, OP_ACCEPT);

//        ServerSocketChannel server2 = ServerSocketChannel.open();
//        server2.configureBlocking(false);
//        server2.socket().bind(new InetSocketAddress(8081));
//        server2.register(selector, OP_ACCEPT);
        int i = 0, j = 0;
        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
                SocketChannel client;
                SelectionKey key = iter.next();
                iter.remove();
                switch (key.readyOps()) {
                    case OP_ACCEPT:
                        acceptCounter.inc();
                        client = ((ServerSocketChannel) key.channel()).accept();
                        client.configureBlocking(false);
                        client.register(selector, OP_READ);
                        break;
                    case OP_READ:
                        readCounter.inc();
                        client = (SocketChannel) key.channel();
                        buffer.clear();
                        if (client.read(buffer) != -1) {
                            dataCounter.inc();
                            buffer.flip();
                            String line = new String(buffer.array(), buffer.position(), buffer.remaining());
//                            System.out.println(line);
                            client.close();
//                            if (line.startsWith("CLOSE")) {
//                                client.close();
//                            } else if (line.startsWith("QUIT")) {
//                                for (SelectionKey k : selector.keys()) {
//                                    k.cancel();
//                                    k.channel().close();
//                                }
//                                selector.close();
//                                return;
//                            }
                        } else {
                            key.cancel();
                        }
                        break;
                    default:
                        System.out.println("unhandled " + key.readyOps());
                        break;
                }
            }
        }
    }
}
