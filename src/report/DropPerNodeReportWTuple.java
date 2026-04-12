package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class DropPerNodeReportWTuple extends Report implements MessageListener {
    private Map<DTNHost, List<Tuple<Double, Boolean>>> dropEvents = new HashMap<>();

    public DropPerNodeReportWTuple() {
        init();
    }

    @Override
    public void init() {
        super.init();
        dropEvents = new HashMap<>();
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) {
            return;
        }

        // Mencatat event drop atau remove untuk host tertentu
        List<Tuple<Double, Boolean>> events = dropEvents.getOrDefault(where, new ArrayList<>());

        // Menyimpan waktu dan jenis event drop/remove
        events.add(new Tuple<>(getSimTime(), dropped));

        // Update map dengan event terbaru
        dropEvents.put(where, events);
    }

    @Override
    public void done() {
        write("=== DROP PER NODE STATISTICS ===");

        for (Map.Entry<DTNHost, List<Tuple<Double, Boolean>>> entry : dropEvents.entrySet()) {
            DTNHost host = entry.getKey();
            List<Tuple<Double, Boolean>> events = entry.getValue();

            int droppedCount = 0;
            int removedCount = 0;

            for (Tuple<Double, Boolean> t : events) {
                if (t.getValue()) {
                    droppedCount++;
                } else {
                    removedCount++;
                }
            }
            write("Host " + host + " | Drops: " + droppedCount + " | Removes: " + removedCount + " | Total Events: "
                    + events.size() + "Time : " + events.get(events.size() - 1).getKey() + " sec");
        }
    }

    @Override
    public void newMessage(Message m) {
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
    }
}