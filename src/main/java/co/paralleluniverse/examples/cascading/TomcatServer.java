package co.paralleluniverse.examples.cascading;

import java.io.File;
import javax.servlet.Servlet;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;

public class TomcatServer extends AbstractEmbeddedServer {
    private Tomcat tomcat;
    private Context context;

    private void build() {
        if (tomcat != null)
            return;
        this.tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector().setAttribute("maxThreads", nThreads);
        tomcat.getConnector().setAttribute("acceptCount", maxConn);
        this.context = tomcat.addContext("/", new File(System.getProperty("java.io.tmpdir")).getAbsolutePath());
    }

    @Override
    public ServletDesc addServlet(String name, Class<? extends Servlet> servletClass, String mapping) {
        build();
        Wrapper w = Tomcat.addServlet(context, name, servletClass.getName());
        w.addMapping(mapping);
        return new TomcatServletDesc(w);
    }
    
    @Override
    public void run() throws Exception {
        tomcat.start();
        tomcat.getServer().await();
    }
    
    private static class TomcatServletDesc implements ServletDesc {
        private final Wrapper w;

        public TomcatServletDesc(Wrapper w) {
            this.w = w;
        }

        @Override
        public ServletDesc setInitParameter(String name, String value) {
            w.addInitParameter(name, value);
            return this;
        }
    }
}
