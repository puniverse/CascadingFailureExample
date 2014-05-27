package co.paralleluniverse.examples.cascading;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RequestLimit;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.*;
import javax.servlet.Servlet;

public class UndertowServer extends AbstractEmbeddedServer {
    private DeploymentInfo deployment;

    private void build() {
        if (deployment != null)
            return;
        this.deployment = Servlets.deployment().setDeploymentName("")
                .setClassLoader(ClassLoader.getSystemClassLoader())
                .setContextPath("/");
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
        DeploymentManager servletsContainer = Servlets.defaultContainer().addDeployment(deployment);
        servletsContainer.deploy();
        HttpHandler handler = servletsContainer.start();
        handler = Handlers.requestLimitingHandler(new RequestLimit(maxConn), handler);
        Undertow server = Undertow.builder().setHandler(handler)
                .setIoThreads(nThreads)
                .addHttpListener(port, "localhost")
                .build();
        server.start();
    }

    private static class UndertowServletDesc implements ServletDesc {
        private final ServletInfo impl;

        public UndertowServletDesc(ServletInfo info) {
            this.impl = info;
        }

        @Override
        public ServletDesc setInitParameter(String name, String value) {
            impl.addInitParam(name, value);
            return this;
        }

        @Override
        public ServletDesc setLoadOnStartup(int load) {
            impl.setLoadOnStartup(load);
            return this;
        }
    }
}
