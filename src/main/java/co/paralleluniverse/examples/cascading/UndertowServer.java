package co.paralleluniverse.examples.cascading;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.RequestLimit;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import javax.servlet.Servlet;

public class UndertowServer extends AbstractEmbeddedServer {
    private DeploymentInfo deployment;

    private void build() {
        if (deployment != null)
            return;
        this.deployment = Servlets.deployment()
                .setClassLoader(ClassLoader.getSystemClassLoader())
                .setContextPath("/")
                .setDeploymentName("");
    }

    @Override
    public ServletDesc addServlet(String name, Class<? extends Servlet> servletClass, String mapping) {
        build();
        ServletInfo info = Servlets.servlet(name, servletClass).addMapping(mapping).setAsyncSupported(true);
        deployment.addServlet(info);
        return new UndertowServletDesc(info);
    }

    @Override
    public void run() throws Exception {
        final DeploymentManager servletsContainer = Servlets.defaultContainer().addDeployment(deployment);
        servletsContainer.deploy();
        Undertow server = Undertow.builder()
                .setIoThreads(nThreads)
                .addHttpListener(port, "localhost")
                .setHandler(Handlers.requestLimitingHandler(new RequestLimit(maxConn), servletsContainer.start()))
                .build();
        
        server.start();
    }

    private static class UndertowServletDesc implements ServletDesc {
        private final ServletInfo info;

        public UndertowServletDesc(ServletInfo info) {
            this.info = info;
        }

        @Override
        public ServletDesc setInitParameter(String name, String value) {
            info.addInitParam(name, value);
            return this;
        }
    }
}
