package report;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import core.DTNHost;
import core.Settings;
import core.SimClock;
import core.Tuple;
import core.UpdateListener;

public class CopyBufferOccupancyPerNodePerIntervalReportAvg extends Report implements UpdateListener {

    public static final String INTERVAL_SETTING = "occupancyInterval";
    public static final int DEFAULT_INTERVAL = 5;

    private int interval;
    private double lastRecord;

    private List<Integer> timeSeries;
    private List<DTNHost> hostList;

    private Map<DTNHost, List<Tuple<Integer, Double>>> occupancySeries;
    private Map<DTNHost, Double> sumPerNode;
    private Map<DTNHost, Integer> countPerNode;

    public CopyBufferOccupancyPerNodePerIntervalReportAvg() {
        init();

        Settings s = getSettings();

        if (s.contains(INTERVAL_SETTING)) {
            interval = s.getInt(INTERVAL_SETTING);
        } else {
            interval = DEFAULT_INTERVAL;
        }
    }

    @Override
    public void init() {
        super.init();

        lastRecord = 0;

        timeSeries = new ArrayList<>();

        sumPerNode = new HashMap<>();
        countPerNode = new HashMap<>();

        hostList = new ArrayList<>();

        occupancySeries = new LinkedHashMap<>();
    }

    @Override
    public void updated(List<DTNHost> hosts) {

        if (isWarmup()) {
            return;
        }

        if (hostList.isEmpty()) {
            hostList.addAll(hosts);
        }

        if (SimClock.getTime() - lastRecord >= interval) {

            lastRecord += interval;

            int time = (int) lastRecord;

            timeSeries.add(time);

            for (DTNHost h : hosts) {

                double occ = h.getBufferOccupancy();

                if (occ > 100.0) {
                    occ = 100.0;
                }

                // update sum dan count pernode untuk rata-ratanya
                List<Tuple<Integer, Double>> list = occupancySeries.getOrDefault(h, new ArrayList<>());

                list.add(new Tuple<>(time, occ));

                occupancySeries.put(h, list);

                sumPerNode.put(h, sumPerNode.getOrDefault(h, 0.0) + occ);

                countPerNode.put(h, countPerNode.getOrDefault(h, 0) + 1);
            }
        }
    }

    @Override
    public void done() {

        write("=== BUFFER OCCUPANCY PER NODE PER INTERVAL ===");
        write("");

        // Header
        String header = String.format("%-8s", "Time");

        for (DTNHost h : hostList) {
            header += String.format("%-10s", h);
        }

        header += String.format("%-10s", "AVG");

        write(header);

        // Rows 
        for (int i = 0; i < timeSeries.size(); i++) {

            int time = timeSeries.get(i);

            String row = String.format("%-8d", time);

            double sumInterval = 0;

            for (DTNHost h : hostList) {

                // ambil occupancy per node per interval
                double occ = occupancySeries.get(h).get(i).getValue();

                // update sum untuk rata-rata per interval
                sumInterval += occ;

                row += String.format("%-10.2f", occ);
            }

            // hitung rata-rata per interval
            double avgInterval = sumInterval / hostList.size();

            row += String.format("%-10.2f", avgInterval);

            write(row);
        }

        write("");
        write("=== AVERAGE BUFFER OCCUPANCY PER NODE ===");

        for (DTNHost h : hostList) {

            // hitung rata-rata per node
            double avg = sumPerNode.get(h) / countPerNode.get(h);

            write(String.format("%-10s %.2f", h, avg));
        }
        super.done();
    }
}