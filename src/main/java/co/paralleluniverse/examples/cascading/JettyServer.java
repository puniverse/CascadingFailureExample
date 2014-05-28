package co.paralleluniverse.examples.cascading;

import javax.servlet.Servlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class JettyServer extends AbstractEmbeddedServer {
    private Server server;
    private ServerConnector http;
    private ServletContextHandler context;

    private void build() {
        if (server != null)
            return;
        this.server = new Server(new QueuedThreadPool(nThreads, nThreads));
        this.http = new ServerConnector(server);
        http.setPort(port);
        http.setAcceptQueueSize(maxConn);
        server.addConnector(http);
        this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    }

    @Override
    public ServletDesc addServlet(String name, Class<? extends Servlet> servletClass, String mapping) {
        build();
        ServletHolder sh = new ServletHolder(servletClass);
        context.addServlet(sh, mapping);
        return new JettyServletDesc(sh);
    }

    @Override
    public void run() throws Exception {
        server.setHandler(context);
        server.start();
        server.join();
    }

    private static class JettyServletDesc implements ServletDesc {
        private final ServletHolder impl;

        public JettyServletDesc(ServletHolder sh) {
            this.impl = sh;
        }

        @Override
        public ServletDesc setInitParameter(String name, String value) {
            impl.setInitParameter(name, value);
            return this;
        }

        @Override
        public ServletDesc setLoadOnStartup(int load) {
            impl.setInitOrder(load);
            return this;
        }
    }
}
