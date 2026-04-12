package report;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Settings;
import core.SimClock;
import core.Tuple;
import core.UpdateListener;

public class AvgBufferOccupancyPerNodePerInterval extends Report implements UpdateListener {

    public static final String INTERVAL_SETTING = "interval";
    public static final int DEFAULT_INTERVAL = 100;

    private int interval;
    private double lastRecord = Double.MIN_VALUE;

    private List<Integer> timeSeries;
    private List<DTNHost> hostList;

    // moving average series
    private Map<DTNHost, List<Tuple<Integer, Double>>> movingAvgSeries;

    // Untuk menghitung rata-rata per node dalam interval
    private Map<DTNHost, Double> sumPerNode;
    private Map<DTNHost, Integer> countPerNode;

    public AvgBufferOccupancyPerNodePerInterval() {

        super();

        Settings s = getSettings();

        if (s.contains(INTERVAL_SETTING))
            interval = s.getInt(INTERVAL_SETTING);
        else
            interval = DEFAULT_INTERVAL;

        init();
    }

    @Override
    protected void init() {

        super.init();

        timeSeries = new ArrayList<>();

        hostList = new ArrayList<>();

        movingAvgSeries = new LinkedHashMap<>();

        sumPerNode = new HashMap<>();

        countPerNode = new HashMap<>();
    }

    @Override
    public void updated(List<DTNHost> hosts) {

        if (isWarmup()) {
            return;
        }

        if (hostList.isEmpty()) {

            hostList.addAll(hosts);

            // Inisialisasi masing-masing host di Map untuk ma, sum, dan count
            for (DTNHost h : hosts) {

                movingAvgSeries.put(h, new ArrayList<>());

                sumPerNode.put(h, 0.0);

                countPerNode.put(h, 0);
            }
        }

        if (SimClock.getTime() - lastRecord >= interval) {

            lastRecord = SimClock.getTime();

            int time = (int) SimClock.getTime();

            timeSeries.add(time);

            for (DTNHost h : hostList) {

                double bo = h.getBufferOccupancy();

                if (bo > 100.0) {
                    bo = 100.0;
                }

                // update accumulator
                sumPerNode.put(h, sumPerNode.get(h) + bo);

                // update count
                countPerNode.put(h, countPerNode.get(h) + 1);

                // Hitung moving average
                double movingAvg = sumPerNode.get(h) / countPerNode.get(h);

                // Simpan moving average di series
                movingAvgSeries.get(h).add(new Tuple<>(time, movingAvg));
            }
        }
    }

    private void printLine() {

        write("=== MOVING AVG BUFFER OCCUPANCY PER NODE ===");
        write("");

        // Header
        String header = String.format("%-8s", "Time");

        for (DTNHost h : hostList)
            header += String.format("%-10s", h);

        write(header);

        // Rows
        for (int i = 0; i < timeSeries.size(); i++) {

            String row = String.format("%-8d", timeSeries.get(i));

            for (DTNHost h : hostList) {

                double avg = movingAvgSeries.get(h).get(i).getValue();

                row += String.format("%-10.2f", avg);
            }
            write(row);
        }
    }

    @Override
    public void done() {

        printLine();

        super.done();
    }
}