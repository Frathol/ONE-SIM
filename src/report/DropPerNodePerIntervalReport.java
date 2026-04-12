package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.SimScenario;
import core.Message;
import core.MessageListener;
import core.SimClock;
// import core.SimClock;
import core.Tuple;
import core.UpdateListener;

public class DropPerNodePerIntervalReport extends Report implements MessageListener, UpdateListener {
    private int interval = 5;
    private int lastSave = 0;
    // Map untuk menyimpan jumlah drop per node per interval, dengan key DTNHost dan
    // value List of Tuple<Time, DropCount>
    private Map<DTNHost, List<Tuple<Integer, Integer>>> dropCount = new HashMap<>();
    // Untuk menyimpan jumlah drop sementara dalam interval saat ini, yang akan
    // dipindahkan ke dropCount saat interval berakhir
    private Map<DTNHost, Integer> dropBuf = new HashMap<>();
    private List<Integer> timeSeries;
    private List<DTNHost> hostList;

    public DropPerNodePerIntervalReport() {
        init();
    }

    @Override
    public void init() {
        super.init();

        dropCount = new HashMap<>();
        dropBuf = new HashMap<>();

        timeSeries = new ArrayList<>();
        hostList = new ArrayList<>();

        interval = 5;
        lastSave = 0;
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        if (hostList.isEmpty()) {
            hostList.addAll(hosts);
        }
        if (SimClock.getTime() - lastSave >= interval) {
            lastSave = (int) SimClock.getTime();
            timeSeries.add(lastSave);
            for (DTNHost h : hosts) {
                List<Tuple<Integer, Integer>> events = dropCount.getOrDefault(h, new ArrayList<>());
                int dropped = dropBuf.getOrDefault(h, 0);
                events.add(new Tuple<>(lastSave, dropped));
                dropCount.put(h, events);
                dropBuf.put(h, 0);
            }
        }
    }

    @Override
    public void newMessage(Message m) {
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) {
            return;
        }

        // if (dropped) {
        // this.dropBuf.compute(
        // where,
        // (host, nrofDropped) -> (nrofDropped == null || nrofDropped == 0) ? 1 :
        // nrofDropped + 1);
        // }

        if (dropped) {
            dropBuf.put(where, dropBuf.getOrDefault(where, 0) + 1);
        } else {
            dropBuf.put(where, dropBuf.getOrDefault(where, 0));
        }
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
    }

    @Override
    public void done() {
        write("=== DROP PER NODE PER INTERVAL ===");
        write("");

        String header = String.format("%-8s", "Time");

        // Header
        for (DTNHost h : hostList) {
            header += String.format("%-10s", h);
        }

        write(header);

        // Row
        for (int i = 0; i < timeSeries.size(); i++) {

            int time = timeSeries.get(i);

            String row = String.format("%-8d", time);

            for (DTNHost h : hostList) {

                List<Tuple<Integer, Integer>> events = dropCount.get(h);

                int drops = 0;

                if (events != null && i < events.size()) {
                    drops = events.get(i).getValue();
                }

                row += String.format("%-10d", drops);
            }

            write(String.format("%-8s", row));
        }
        super.done();
    }
}