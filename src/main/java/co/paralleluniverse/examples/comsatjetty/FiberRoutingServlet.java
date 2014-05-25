package co.paralleluniverse.examples.comsatjetty;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;

public class FiberRoutingServlet extends FiberHttpServlet {
    private final CloseableHttpClient httpClient;
    private final BasicResponseHandler basicResponseHandler;

    public FiberRoutingServlet() {
        httpClient = new FiberHttpClient(ClientTesters.createDefaultHttpAsyncClient(null));

        basicResponseHandler = new BasicResponseHandler();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws SuspendExecution, ServletException, IOException {
        try (PrintWriter out = resp.getWriter()) {
            final String callResponse = "true".equals(req.getParameter("callService"))
                    ? httpClient.execute(new HttpGet(CascadingFailureServer.SERVICE_URL + req.getParameter("sleep")), basicResponseHandler)
                    : "skipped";
            out.print("call response: " + callResponse);
        }
    }

}
