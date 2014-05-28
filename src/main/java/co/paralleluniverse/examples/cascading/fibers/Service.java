package co.paralleluniverse.examples.cascading.fibers;

import co.paralleluniverse.examples.cascading.*;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.httpclient.FiberHttpClientBuilder;
import java.io.IOException;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.*;

@Singleton
@Path("/service")
public class Service extends HttpServlet {
    private final CloseableHttpClient httpClient;
    private static final BasicResponseHandler basicResponseHandler = new BasicResponseHandler();

    public Service() {
        httpClient = FiberHttpClientBuilder.create() // <---------- FIBER
                .setMaxConnPerRoute(Main.MAX_CONN)
                .setMaxConnTotal(Main.MAX_CONN)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Main.TIMEOUT)
                        .setSocketTimeout(Main.TIMEOUT)
                        .setConnectionRequestTimeout(Main.TIMEOUT).build()).build();
                    }

    @GET
    @Produces("text/plain")
    @Suspendable  // <------------- FIBER
    public String get(@QueryParam("sleep") int sleep) throws IOException {
        // simulate a call to a service that always completes in 10 ms
        String res1 = httpClient.execute(new HttpGet(Main.SERVICE_URL + 10), basicResponseHandler);

        // simulate a call to a service that might fail and cause a delay
        String res2 = sleep > 0 ? httpClient.execute(new HttpGet(Main.SERVICE_URL + sleep), basicResponseHandler) : "skipped";

        return "call response res1: " + res1 + " res2: " + res2;
    }
}
