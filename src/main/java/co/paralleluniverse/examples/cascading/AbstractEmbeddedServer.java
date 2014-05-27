package co.paralleluniverse.examples.cascading;


public abstract class AbstractEmbeddedServer implements EmbeddedServer {
    protected int port;
    protected int nThreads;
    protected int maxConn;
    
    @Override
    public EmbeddedServer setPort(int port) {
        this.port = port;
        return this;
    }

    @Override
    public EmbeddedServer setNumThreads(int nThreads) {
        this.nThreads = nThreads;
        return this;
    }

    @Override
    public EmbeddedServer setMaxConnections(int maxConn) {
        this.maxConn = maxConn;
        return this;
    }
}
