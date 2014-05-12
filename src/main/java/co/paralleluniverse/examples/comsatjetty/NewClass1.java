package co.paralleluniverse.examples.comsatjetty;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.nio.reactor.IOReactorException;

public class NewClass1 {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        try (FiberHttpClient fiberHttpClient = new FiberHttpClient(ClientTesters.createDefaultHttpAsyncClient())) {
            
            new Fiber<>(() -> {
                try {
                    int statusCode = fiberHttpClient.execute(new HttpGet("http://54.73.179.243:1234/path?par=25")).getStatusLine().getStatusCode();
                    System.out.println("sc is "+statusCode);
                } catch (IOException ex) {
                    Logger.getLogger(NewClass1.class.getName()).log(Level.SEVERE, null, ex);
                }

            }).start().join();
        } catch (IOReactorException ex) {
            Logger.getLogger(NewClass1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NewClass1.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
