package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Report khusus untuk membedakan statistik pesan berdasarkan Prefix.
 * Untuk membandingkan ExternalEvents vs MessageGenerator.
 */
public class MessageEventSplitReport extends Report implements MessageListener {
    private Map<String, Integer> nrofCreated = new HashMap<>();
    private Map<String, Integer> nrofStarted = new HashMap<>();
    private Map<String, Integer> nrofRelayed = new HashMap<>();
    private Map<String, Integer> nrofAborted = new HashMap<>();
    private Map<String, Integer> nrofDropped = new HashMap<>();
    private Map<String, Integer> nrofDelivered = new HashMap<>();
    
    private Map<String, List<Double>> latencies = new HashMap<>();
    private Map<String, List<Integer>> hopCounts = new HashMap<>();
    private Map<String, List<Double>> msgBufferTime = new HashMap<>();
    private Map<String, Double> creationTimes = new HashMap<>();

    public MessageEventSplitReport() {
        init();
    }

    private String getPrefix(String id) {
        // Mengambil karakter sebelum angka (Contoh: M1 -> M, GEN_AUTO12 -> GEN_AUTO)
        return id.replaceAll("\\d.*", "");
    }

    @Override
    public void newMessage(Message m) {
        if (isWarmup()) return;
        String p = getPrefix(m.getId());
        
        nrofCreated.put(p, nrofCreated.getOrDefault(p, 0) + 1);
        creationTimes.put(m.getId(), getSimTime());
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        if (isWarmup()) return;
        String p = getPrefix(m.getId());
        nrofStarted.put(p, nrofStarted.getOrDefault(p, 0) + 1);
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
        if (isWarmup()) return;
        String p = getPrefix(m.getId());

        nrofRelayed.put(p, nrofRelayed.getOrDefault(p, 0) + 1);

        if (finalTarget) {
            nrofDelivered.put(p, nrofDelivered.getOrDefault(p, 0) + 1);
            if (creationTimes.containsKey(m.getId())) {
                // if(!latencies.containsKey(p)){
                //   latencies.put(p, new ArrayList<Double>());
                // }
                // latencies.get(p).add(getSimTime() - creationTimes.get(m.getId()));
                latencies.computeIfAbsent(p, k -> new ArrayList<>()).add(getSimTime() - creationTimes.get(m.getId()));
            }
            hopCounts.computeIfAbsent(p, k -> new ArrayList<>()).add(m.getHops().size() - 1);
        }
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmup()) return;
        String p = getPrefix(m.getId());

        if (dropped) {
            nrofDropped.put(p, nrofDropped.getOrDefault(p, 0) + 1);
        }
        msgBufferTime.computeIfAbsent(p, k -> new ArrayList<>()).add(getSimTime() - m.getReceiveTime());
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        if (isWarmup()) return;
        String p = getPrefix(m.getId());
        nrofAborted.put(p, nrofAborted.getOrDefault(p, 0) + 1);
    }

    @Override
    public void done() {
        write("Detailed Message Stats by Prefix");
        write("sim_time: " + format(getSimTime()));

        for (String p : nrofCreated.keySet()) {
            int created = nrofCreated.get(p);
            int delivered = nrofDelivered.getOrDefault(p, 0);
            int relayed = nrofRelayed.getOrDefault(p, 0);
            
            double deliveryProb = (created > 0) ? (double) delivered / created : 0;
            double overhead = (delivered > 0) ? (double) (relayed - delivered) / delivered : 0;

            write("\n--- Statistics for Prefix: [" + p + "] ---");
            write("created       : " + created);
            write("started       : " + nrofStarted.getOrDefault(p, 0));
            write("relayed       : " + relayed);
            write("aborted       : " + nrofAborted.getOrDefault(p, 0));
            write("dropped       : " + nrofDropped.getOrDefault(p, 0));
            write("delivered     : " + delivered);
            write("delivery_prob : " + format(deliveryProb));
            write("overhead_ratio: " + format(overhead));
            write("latency_avg   : " + getAverage(latencies.getOrDefault(p, new ArrayList<>())));
            write("hopcount_avg  : " + getIntAverage(hopCounts.getOrDefault(p, new ArrayList<>())));
            write("buffertime_avg: " + getAverage(msgBufferTime.getOrDefault(p, new ArrayList<>())));
        }
        super.done();
    }
}