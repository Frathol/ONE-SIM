package report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Settings;
import core.SimClock;
import core.UpdateListener;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.decisionengine.PeopleRank;

public class PeopleRankStatReport extends Report implements UpdateListener {
    public static final String BUFFER_REPORT_INTERVAL = "timeInterval";

    /** Default value for the snapshot interval */
    public static final int DEFAULT_RANK_REPORT_INTERVAL = 500;

    private String prefix = "";
    private double lastRecord = Double.MIN_VALUE;
    private int interval;
    private boolean headerPrinted;

    private Map<DTNHost, double[]> peopleRanks;

    public PeopleRankStatReport() {
        super();
        Settings settings = getSettings();

        if (settings.contains(BUFFER_REPORT_INTERVAL)) {
            interval = settings.getInt(BUFFER_REPORT_INTERVAL);
        } else {
            interval = -1;
        }

        if (interval < 0) {
            interval = DEFAULT_RANK_REPORT_INTERVAL;
        }

        this.peopleRanks = new HashMap<>();
        this.headerPrinted = false;
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        recordRanks(hosts);
        if (SimClock.getTime() - this.lastRecord >= (double) this.interval) {
            if (!this.headerPrinted) {
                this.write(String.format("%-13s", "time/host"));
                hosts.forEach((host) -> this.write(String.format("%-8s", host.toString())));
                this.write("\n");
                this.headerPrinted = true;
            }

            this.lastRecord = SimClock.getTime();
            this.printLine(this.lastRecord);
        }

    }

    private void recordRanks(List<DTNHost> hosts) {
        for (DTNHost host : hosts) {
            peopleRanks.computeIfAbsent(host, k -> new double[1])[0] = getDecisionEngine(host).getRank();
        }
    }

    private PeopleRank getDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " +
                " with other routers of same type";

        return (PeopleRank) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    private void printLine(double time) {
        write(String.format("%-13.1f", time));
        peopleRanks.forEach((host, values) -> {
            write(String.format("%-8.2f", values[0]));
            values[0] = 0;
        });
        write("\n");
    }

    @Override
    protected void write(String txt) {
        if (out == null) {
            init();
        }
        out.print(prefix + txt);
    }

}