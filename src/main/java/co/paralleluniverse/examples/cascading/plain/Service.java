package co.paralleluniverse.examples.cascading.plain;

import co.paralleluniverse.examples.cascading.*;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.*;



@Path("/service")
public class Service extends HttpServlet {
    private final CloseableHttpClient httpClient;
    private final BasicResponseHandler basicResponseHandler;

    public Service() {
        httpClient = HttpClientBuilder.create()
                .setMaxConnPerRoute(Main.MAX_CONN)
                .setMaxConnTotal(Main.MAX_CONN)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Main.TIMEOUT)
                        .setSocketTimeout(Main.TIMEOUT)
                        .setConnectionRequestTimeout(Main.TIMEOUT).build()).build();
        basicResponseHandler = new BasicResponseHandler();
    }

    
    @GET
    @Produces("text/plain")
    public String get(@QueryParam("sleep") int sleep) throws IOException {
        // simulate a call to a service that always completes in 10 ms
        String res1 = httpClient.execute(new HttpGet(Main.SERVICE_URL + 10), basicResponseHandler);
        
        // simulate a call to a service that might fail and cause a delay
        String res2 = sleep > 0 ? httpClient.execute(new HttpGet(Main.SERVICE_URL + sleep), basicResponseHandler) : "skipped";
        
        return "call response res1: " + res1 + " res2: " + res2;
    }
}
