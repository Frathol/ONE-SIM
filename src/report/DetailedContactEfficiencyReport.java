package report;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;

public class DetailedContactEfficiencyReport extends Report implements ConnectionListener, MessageListener {

    private Map<DTNHost, Integer> totalContactMap;

    private Map<DTNHost, Integer> successContactMap;

    private Set<String> activeSuccessPairs;

    public DetailedContactEfficiencyReport() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        totalContactMap = new HashMap<>();
        successContactMap = new HashMap<>();
        activeSuccessPairs = new HashSet<>();
    }

    private String getPairId(DTNHost h1, DTNHost h2) {
        if (h1.getAddress() < h2.getAddress()) {
            return h1.getAddress() + "-" + h2.getAddress();
        } else {
            return h2.getAddress() + "-" + h1.getAddress();
        }
    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        totalContactMap.put(host1, totalContactMap.getOrDefault(host1, 0) + 1);
        totalContactMap.put(host2, totalContactMap.getOrDefault(host2, 0) + 1);
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        String pairId = getPairId(from, to);

        if (!activeSuccessPairs.contains(pairId)) {
            activeSuccessPairs.add(pairId);

            successContactMap.put(from, successContactMap.getOrDefault(from, 0) + 1);
            successContactMap.put(to, successContactMap.getOrDefault(to, 0) + 1);
        }
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        String pairId = getPairId(host1, host2);
        activeSuccessPairs.remove(pairId);
    }

    @Override
    public void done() {
        write("=== DETAILED CONTACT EFFICIENCY REPORT ===");
        write(String.format("%-10s %-15s %-15s %-15s %-15s",
                "Node", "Total_Contact", "Success", "Zonk", "Zonk_Ratio(%)"));

        List<DTNHost> allHosts = new ArrayList<>(totalContactMap.keySet());
        Collections.sort(allHosts);

        for (DTNHost host : allHosts) {
            int total = totalContactMap.getOrDefault(host, 0);
            int sukses = successContactMap.getOrDefault(host, 0);
            int zonk = total - sukses;

            double zonkRatio = (total == 0) ? 0.0 : ((double) zonk / total) * 100.0;

            write(String.format("%-10s %-15d %-15d %-15d %-15.2f",
                    host.toString(), total, sukses, zonk, zonkRatio));
        }

        super.done();
    }

    @Override
    public void newMessage(Message m) {
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
    }
}