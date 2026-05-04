package report;

import java.util.HashMap;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;

public class BufferTimeTrackerReport extends Report implements MessageListener {

    // Key: "MessageID_HostAddress", Value: Waktu Masuk (SimClock)
    private Map<String, Double> arrivalTimes;
    
    private double totalBufferTime = 0;
    private int forwardCount = 0;

    public BufferTimeTrackerReport() {
        super();
        init();
    }

    @Override
    protected void init() {
        super.init();
        arrivalTimes = new HashMap<>();
        totalBufferTime = 0;
        forwardCount = 0;
    }

    @Override
    public void newMessage(Message m) {
        // Catat waktu saat pesan baru lahir di node pembuatnya
        String key = m.getId() + "_" + m.getFrom().getAddress();
        arrivalTimes.put(key, SimClock.getTime());
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        // 1. Pesan KELUAR dari node 'from'
        // Hitung berapa lama pesan ini nginap di node 'from'
        String keyFrom = m.getId() + "_" + from.getAddress();
        if (arrivalTimes.containsKey(keyFrom)) {
            double arrivalTime = arrivalTimes.get(keyFrom);
            double timeInBuffer = SimClock.getTime() - arrivalTime;
            
            totalBufferTime += timeInBuffer;
            forwardCount++;
            
            // Hapus dari memori report agar tidak bocor (Memory Leak)
            arrivalTimes.remove(keyFrom);
        }

        // 2. Pesan MASUK ke node 'to'
        // Catat waktu masuknya ke node penerima yang baru
        if (!firstDelivery) {
            String keyTo = m.getId() + "_" + to.getAddress();
            arrivalTimes.put(keyTo, SimClock.getTime());
        }
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        // Bersihkan memori report jika pesan dihapus di node tertentu
        String key = m.getId() + "_" + where.getAddress();
        arrivalTimes.remove(key);
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}

    @Override
    public void done() {
        write("=== LAPORAN LAMA WAKTU ANTRE (BUFFER TIME) ===");
        write("Total Transaksi Forward  : " + forwardCount + " kali");
        
        if (forwardCount > 0) {
            double avgTime = totalBufferTime / forwardCount;
            write("Rata-Rata Waktu Antre    : " + String.format("%.2f", avgTime) + " detik per node");
        } else {
            write("Rata-Rata Waktu Antre    : 0 detik");
        }
        super.done();
    }
}