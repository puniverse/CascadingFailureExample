package co.paralleluniverse.examples.cascading;

import javax.servlet.Servlet;


public interface EmbeddedServer {
    EmbeddedServer setPort(int port);
    EmbeddedServer setNumThreads(int nThreads);
    EmbeddedServer setMaxConnections(int maxConn);
    ServletDesc addServlet(String name, Class<? extends Servlet> servletClass, String mapping);
    
    void run() throws Exception;
    
    interface ServletDesc {
        ServletDesc setInitParameter(String name, String value);
        ServletDesc setLoadOnStartup(int load);
    }
}
