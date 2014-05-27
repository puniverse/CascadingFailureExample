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
        httpClient = HttpClientBuilder.create().
                setMaxConnPerRoute(Main.MAX_CONN).
                setMaxConnTotal(Main.MAX_CONN).
                setDefaultRequestConfig(RequestConfig.custom().
                        setConnectTimeout(Main.TIMEOUT).
                        setSocketTimeout(Main.TIMEOUT).
                        setConnectionRequestTimeout(Main.TIMEOUT).build()).build();
        basicResponseHandler = new BasicResponseHandler();
    }

    @GET
    @Produces("text/plain")
    public String get(@QueryParam("sleep") int sleep) throws IOException {
        String response = httpClient.execute(new HttpGet(Main.SERVICE_URL + sleep), basicResponseHandler);
        return "call response: " + response;
    }
}
