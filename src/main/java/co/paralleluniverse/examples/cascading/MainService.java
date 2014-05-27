package co.paralleluniverse.examples.cascading;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class MainService extends HttpServlet {
    private final CloseableHttpClient httpClient;
    private final BasicResponseHandler basicResponseHandler;

    public MainService() {
        httpClient = HttpClientBuilder.create().
                setMaxConnPerRoute(Main.MAX_CONN).
                setMaxConnTotal(Main.MAX_CONN).
                setDefaultRequestConfig(RequestConfig.custom().
                        setConnectTimeout(Main.TIMEOUT).
                        setSocketTimeout(Main.TIMEOUT).
                        setConnectionRequestTimeout(Main.TIMEOUT).build()).build();
        basicResponseHandler = new BasicResponseHandler();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (PrintWriter out = resp.getWriter()) {
            
            final String callResponse = "true".equals(req.getParameter("callService"))
                    ? httpClient.execute(new HttpGet(Main.SERVICE_URL + req.getParameter("sleep")), basicResponseHandler)
                    : "skipped";
            out.print("call response: " + callResponse);
        }
    }
}
