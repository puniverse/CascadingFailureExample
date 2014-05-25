package co.paralleluniverse.examples.comsatjetty;

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
import org.apache.http.impl.client.HttpClients;

public class RoutingServlet extends HttpServlet {
    private final CloseableHttpClient httpClient;
    private final BasicResponseHandler basicResponseHandler;

    public RoutingServlet() {
        httpClient = HttpClients.custom().
                setMaxConnPerRoute(99999).
                setMaxConnTotal(99999).
                setDefaultRequestConfig(RequestConfig.custom().
                        setConnectTimeout(7000).
                        setSocketTimeout(7000).
                        setConnectionRequestTimeout(7000).build()).
                build();
        basicResponseHandler = new BasicResponseHandler();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (PrintWriter out = resp.getWriter()) {
            final String callResponse = "true".equals(req.getParameter("callService"))
                    ? httpClient.execute(new HttpGet(CascadingFailureServer.SERVICE_URL + req.getParameter("sleep")), basicResponseHandler)
                    : "skipped";
            out.print("call response: " + callResponse);
        }
    }

}
