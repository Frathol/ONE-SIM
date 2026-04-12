package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;
import core.Tuple;
import core.UpdateListener;

/**
 * Report untuk mencatat jumlah transfer (pengiriman sukses) per node 
 * dalam interval waktu tertentu. Format output berupa matrix (Time vs Nodes).
 */
public class MessageTransferPerNodePerIntervalReport extends Report implements MessageListener, UpdateListener {
    private int interval = 100;
    private int lastSave = 0;
    
    // Simpan riwayat: Node -> List of Tuple<Waktu, JumlahTransfer>
    private Map<DTNHost, List<Tuple<Integer, Integer>>> transferCount = new HashMap<>();
    // Buffer sementara untuk interval berjalan
    private Map<DTNHost, Integer> transferBuf = new HashMap<>();
    
    private List<Integer> timeSeries = new ArrayList<>();
    private List<DTNHost> hostList = new ArrayList<>();

    public MessageTransferPerNodePerIntervalReport() {
        init();
    }

    @Override
    public void init() {
        super.init();
        transferCount = new HashMap<>();
        transferBuf = new HashMap<>();
        timeSeries = new ArrayList<>();
        hostList = new ArrayList<>();
        lastSave = 0;
        interval = 100; 
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        // Daftarkan semua host di awal simulasi untuk header table
        if (hostList.isEmpty()) {
            hostList.addAll(hosts);
        }

        // Cek apakah sudah masuk interval berikutnya
        if (SimClock.getTime() - lastSave >= interval) {
            lastSave = (int) SimClock.getTime();
            timeSeries.add(lastSave);

            for (DTNHost h : hosts) {
                List<Tuple<Integer, Integer>> history = transferCount.getOrDefault(h, new ArrayList<>());
                int totalTransfersInInterval = transferBuf.getOrDefault(h, 0);
                
                history.add(new Tuple<>(lastSave, totalTransfersInInterval));
                transferCount.put(h, history);
                
                // Reset buffer untuk interval berikutnya
                transferBuf.put(h, 0);
            }
        }
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
        if (isWarmup()) return;

        // Transfer sukses dari pengirim
        int currentCount = transferBuf.getOrDefault(from, 0);
        transferBuf.put(from, currentCount + 1);
    }

    public void newMessage(Message m) {}
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}

    @Override
    public void done() {
        write("=== MESSAGE TRANSFERS PER NODE PER INTERVAL ===");
        write("Interval: " + interval);
        write("");

        // Membuat Header (Time;Node0;Node1;Node2...)
        StringBuilder header = new StringBuilder();
        header.append(String.format("%-10s", "Time"));
        for (DTNHost h : hostList) {
            header.append(String.format(";%-10s", h));
        }
        write(header.toString());

        for (int i = 0; i < timeSeries.size(); i++) {
            int currentTime = timeSeries.get(i);
            StringBuilder row = new StringBuilder();
            row.append(String.format("%-10d", currentTime));

            for (DTNHost h : hostList) {
                List<Tuple<Integer, Integer>> history = transferCount.get(h);
                int count = 0;

                if (history != null && i < history.size()) {
                    count = history.get(i).getValue();
                }
                row.append(String.format(";%-10d", count));
            }
            write(row.toString());
        }
        
        super.done();
    }
}