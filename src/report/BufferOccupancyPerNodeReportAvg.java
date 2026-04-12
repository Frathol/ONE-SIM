package report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Settings;
import core.SimClock;
import core.UpdateListener;

public class BufferOccupancyPerNodeReportAvg extends Report implements UpdateListener {

    public static final String INTERVAL_SETTING = "occupancyInterval";
    public static final int DEFAULT_INTERVAL = 5;

    private double lastRecord;
    private int interval;

    // untuk ambil rata-rata per node
    private Map<DTNHost, Double> sumPerNode;
    private Map<DTNHost, Integer> countPerNode;

    public BufferOccupancyPerNodeReportAvg() {
        super();

        Settings s = getSettings();

        if (s.contains(INTERVAL_SETTING)) {
            interval = s.getInt(INTERVAL_SETTING);
        } else {
            interval = DEFAULT_INTERVAL;
        }

        init();
    }

    @Override
    public void init() {
        super.init();

        lastRecord = Double.MIN_VALUE;

        sumPerNode = new HashMap<>();
        countPerNode = new HashMap<>();
    }

    @Override
    public void updated(List<DTNHost> hosts) {

        if (SimClock.getTime() - lastRecord >= interval) {

            lastRecord = SimClock.getTime();

            // update sum dan count pernode untuk rata-ratanya
            for (DTNHost h : hosts) {

                double bo = h.getBufferOccupancy();

                if (bo > 100.0) {
                    bo = 100.0;
                }

                sumPerNode.put(h, sumPerNode.getOrDefault(h, 0.0) + bo);

                countPerNode.put(h, countPerNode.getOrDefault(h, 0) + 1);
            }
            printLine(hosts);
        }
    }

    private void printLine(List<DTNHost> hosts) {

        write(String.format("Time %.0f\n", SimClock.getTime()));

        for (DTNHost h : hosts) {

            double bo = h.getBufferOccupancy();

            if (bo > 100.0) {
                bo = 100.0;
            }
            write(String.format("%-15s : %6.2f %%\n", h.toString(), bo));
        }
        write("\n");
    }

    @Override
    public void done() {

        write("\n=== AVERAGE BUFFER OCCUPANCY PER NODE ===");

        for (DTNHost h : sumPerNode.keySet()) {

            double sum = sumPerNode.get(h);
            int count = countPerNode.get(h);

            double avg = sum / count;

            write(String.format("%-15s : %6.2f %%\n", h.toString(), avg));
        }
        super.done();
    }
}