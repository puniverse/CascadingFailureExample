package co.paralleluniverse.examples.comsatjetty;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TargetServlet extends FiberHttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SuspendExecution {
        try (PrintWriter out = resp.getWriter()) {
            int sleeptime = parseInt(req.getParameter("sleep"), 10);
            Fiber.sleep(sleeptime);
            out.println("sleeping " + sleeptime + "ms starting now: " + new Date().getTime() + " \n");
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    static int parseInt(String str, int defaultVal) {
        int val;
        try {
            val = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            val = defaultVal;
        }
        return val;
    }

}
