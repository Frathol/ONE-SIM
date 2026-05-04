package report;

import java.util.HashMap;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class GroupBasedStatsReport extends Report implements MessageListener {

    // Menyimpan jumlah pesan yang dibuat per pasangan grup
    private Map<String, Integer> createdPerGroup;
    // Menyimpan jumlah pesan yang sukses terkirim per pasangan grup
    private Map<String, Integer> deliveredPerGroup;

    public GroupBasedStatsReport() {
        super();
        init();
    }

    @Override
    protected void init() {
        super.init();
        createdPerGroup = new HashMap<>();
        deliveredPerGroup = new HashMap<>();
    }

    // Fungsi bantuan untuk mengambil nama grup dari Host (Membuang angkanya)
    // Contoh: "p45" menjadi "p"
    private String getGroupPrefix(DTNHost host) {
        return host.toString().replaceAll("[0-9]", "");
    }

    @Override
    public void newMessage(Message m) {
        String fromGroup = getGroupPrefix(m.getFrom());
        String toGroup = getGroupPrefix(m.getTo());
        String key = fromGroup + " -> " + toGroup;

        // Tambah counter untuk pesan yang dibuat di rute grup ini
        createdPerGroup.put(key, createdPerGroup.getOrDefault(key, 0) + 1);
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        // Hanya hitung jika pesan sampai ke tujuan akhir (First Delivery)
        if (firstDelivery) {
            String fromGroup = getGroupPrefix(m.getFrom());
            String toGroup = getGroupPrefix(m.getTo()); // Ini tujuan akhirnya, bukan sekadar 'to' node perantara
            String key = fromGroup + " -> " + toGroup;

            deliveredPerGroup.put(key, deliveredPerGroup.getOrDefault(key, 0) + 1);
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
        write("=== LAPORAN STATISTIK ANTAR GRUP ===");
        write(String.format("%-15s | %-10s | %-10s | %-15s", "Rute Grup", "Dibuat", "Sampai", "Delivery Prob"));
        write("---------------------------------------------------------------");

        for (String key : createdPerGroup.keySet()) {
            int created = createdPerGroup.get(key);
            int delivered = deliveredPerGroup.getOrDefault(key, 0);
            double prob = (double) delivered / created;

            write(String.format("%-15s | %-10d | %-10d | %.4f", key, created, delivered, prob));
        }
        super.done();
    }
}