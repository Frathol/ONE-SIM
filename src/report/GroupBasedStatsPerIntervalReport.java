package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;
import core.UpdateListener;

public class GroupBasedStatsPerIntervalReport extends Report implements MessageListener, UpdateListener {

    private int interval = 5;
    private double lastSave = 0;

    // Wadah pencatatan sementara per interval
    private Map<String, Integer> createdBuf;
    private Map<String, Integer> deliveredBuf;
    
    // Untuk melacak rute apa saja yang pernah terbentuk
    private List<String> knownRoutes;

    public GroupBasedStatsPerIntervalReport() {
        super();
        init();
    }

    @Override
    protected void init() {
        super.init();
        interval = 5;
        lastSave = 0;
        createdBuf = new HashMap<>();
        deliveredBuf = new HashMap<>();
        knownRoutes = new ArrayList<>();
    }

    private String getGroupPrefix(DTNHost host) {
        return host.toString().replaceAll("[0-9]", "");
    }

    // =====================================================================
    // BAGIAN 1: UPDATE LISTENER (Mencetak dan Mereset Interval)
    // =====================================================================
    @Override
    public void updated(List<DTNHost> hosts) {
        if (SimClock.getTime() - lastSave >= interval) {
            lastSave = SimClock.getTime();

            // Hanya cetak pembatas waktu jika ada aktivitas di jaringan
            boolean hasActivity = false;
            for (String route : knownRoutes) {
                if (createdBuf.getOrDefault(route, 0) > 0 || deliveredBuf.getOrDefault(route, 0) > 0) {
                    hasActivity = true;
                    break;
                }
            }

            if (hasActivity) {
                write("--- Waktu: " + (int) lastSave + " detik ---");
                write(String.format("%-15s | %-10s | %-10s", "Rute Grup", "Dibuat", "Sampai"));
                
                for (String route : knownRoutes) {
                    int created = createdBuf.getOrDefault(route, 0);
                    int delivered = deliveredBuf.getOrDefault(route, 0);
                    
                    // Cetak rute yang aktif pada interval ini saja
                    if (created > 0 || delivered > 0) {
                        write(String.format("%-15s | %-10d | %-10d", route, created, delivered));
                    }
                }
                write(""); // Baris kosong untuk spasi interval berikutnya
            }

            // RESET WADAH UNTUK INTERVAL BERIKUTNYA
            createdBuf.clear();
            deliveredBuf.clear();
        }
    }

    // =====================================================================
    // BAGIAN 2: MESSAGE LISTENER (Melacak Kejadian)
    // =====================================================================
    @Override
    public void newMessage(Message m) {
        String route = getGroupPrefix(m.getFrom()) + " -> " + getGroupPrefix(m.getTo());
        createdBuf.put(route, createdBuf.getOrDefault(route, 0) + 1);
        
        if (!knownRoutes.contains(route)) knownRoutes.add(route);
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        if (firstDelivery) {
            String route = getGroupPrefix(m.getFrom()) + " -> " + getGroupPrefix(m.getTo());
            deliveredBuf.put(route, deliveredBuf.getOrDefault(route, 0) + 1);
            
            if (!knownRoutes.contains(route)) knownRoutes.add(route);
        }
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {}

    @Override
    public void done() {
        write("=== SIMULASI SELESAI ===");
        super.done();
    }
}