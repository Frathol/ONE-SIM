package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;

import java.util.HashMap;
import java.util.Map;

public class DropStatsIntervalReport extends Report
        implements MessageListener {

    private double interval;
    private int totalDropped = 0;
    private Map<Integer, Integer> dropPerInterval = new HashMap<>();

    public DropStatsIntervalReport() {
        Settings s = getSettings();
        this.interval = s.getDouble("interval", 120.0);
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (!dropped) {
            return;
        }

        totalDropped++;

        double time = SimClock.getTime();
        int slot = (int) (time / interval);

        dropPerInterval.put(
                slot,
                dropPerInterval.getOrDefault(slot, 0) + 1);
    }

    @Override
    public void done() {
        write("=== DROP STATISTICS ===");
        write("Interval = " + interval + " seconds");
        write("Total dropped messages = " + totalDropped);
        write("");

        for (Integer slot : dropPerInterval.keySet()) {
            double start = slot * interval;
            double end = start + interval;

            write(String.format(
                    "Time %.0f - %.0f sec : %d drops",
                    start, end, dropPerInterval.get(slot)));
        }

        super.done();
    }

    // (MessageListener) =====
    public void newMessage(Message m) {
    }

    public void messageTransferStarted(Message m,
            DTNHost from,
            DTNHost to) {
    }

    @Override
    public void messageTransferred(Message m,
            DTNHost from,
            DTNHost to,
            boolean isFirstDelivery) {
    }

    public void messageTransferAborted(Message m,
            DTNHost from,
            DTNHost to) {
    }
}