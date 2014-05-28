package co.paralleluniverse.examples.cascading.internal;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.io.IOException;
import java.util.Date;
import javax.inject.Singleton;
import javax.ws.rs.*;

@Singleton
@Path("/foo")
public class SimulatedMicroservice {
    @GET
    @Produces("text/plain")
    public String get(@QueryParam("sleep") Integer sleep) throws IOException, SuspendExecution, InterruptedException {
        if (sleep == null || sleep == 0)
            sleep = 10;
        Strand.sleep(sleep);
        return "slept for " + sleep + ": " + new Date().getTime();
    }
}
