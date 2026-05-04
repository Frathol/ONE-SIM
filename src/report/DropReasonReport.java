package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;

public class DropReasonReport extends Report implements MessageListener {

    private int totalCreated = 0;
    private int dropDueToTTL = 0;
    private int dropDueToBuffer = 0;
    private int dropOther = 0;

    public DropReasonReport() {
        super();
        init();
    }

    @Override
    protected void init() {
        super.init();
        totalCreated = 0;
        dropDueToTTL = 0;
        dropDueToBuffer = 0;
        dropOther = 0;
    }

    @Override
    public void newMessage(Message m) {
        totalCreated++;
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        // Hitung sisa TTL pesan saat ini
        double remainingTtl = m.getTtl() - (SimClock.getTime() - m.getCreationTime());

        if (remainingTtl <= 0) {
            // Jika TTL sudah habis atau minus, berarti mati karena kadaluarsa
            dropDueToTTL++;
        } else if (dropped) {
            // Jika TTL masih ada tapi parameter 'dropped' true, berarti dibunuh Queue Policy (Buffer Penuh)
            dropDueToBuffer++;
        } else {
            // Dihapus karena alasan lain (misal: sukses sampai tujuan lalu dihapus dari buffer)
            dropOther++;
        }
    }

    // Method wajib dari interface MessageListener (kosongkan jika tidak dipakai)
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {}
    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}

    @Override
    public void done() {
        write("=== LAPORAN ANALISIS DROP PESAN (DROP REASON) ===");
        write("Total Pesan Dibuat       : " + totalCreated);
        write("Drop Karena TTL Habis    : " + dropDueToTTL);
        write("Drop Karena Buffer Penuh : " + dropDueToBuffer);
        write("Dihapus Normal (Terkirim): " + dropOther);
        super.done();
    }
}