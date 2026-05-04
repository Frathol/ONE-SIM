package report;

import java.util.List;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;
import core.UpdateListener;

public class DropReasonPerIntervalReport extends Report implements MessageListener, UpdateListener {

    private int interval = 5;
    private double lastSave = 0;
    private boolean headerPrinted = false;

    // Wadah sementara per interval
    private int dropDueToTTLBuf = 0;
    private int dropDueToBufferBuf = 0;
    private int dropOtherBuf = 0;

    public DropReasonPerIntervalReport() {
        super();
        init();
    }

    @Override
    protected void init() {
        super.init();
        interval = 5;
        lastSave = 0;
        headerPrinted = false;
        
        dropDueToTTLBuf = 0;
        dropDueToBufferBuf = 0;
        dropOtherBuf = 0;
    }

    // =====================================================================
    // BAGIAN 1: UPDATE LISTENER (Mencetak dan Mereset Interval)
    // =====================================================================
    @Override
    public void updated(List<DTNHost> hosts) {
        if (SimClock.getTime() - lastSave >= interval) {
            lastSave = SimClock.getTime();

            // Cetak header hanya satu kali di awal
            if (!headerPrinted) {
                write("=== TREN ALASAN DROP PESAN PER INTERVAL ===");
                write(String.format("%-10s %-15s %-15s %-15s", "Time(s)", "TTL_Drop", "Buffer_Drop", "Other_Drop"));
                headerPrinted = true;
            }

            // Cetak status interval saat ini
            write(String.format("%-10d %-15d %-15d %-15d", 
                (int) lastSave, dropDueToTTLBuf, dropDueToBufferBuf, dropOtherBuf));

            // RESET WADAH UNTUK INTERVAL BERIKUTNYA
            dropDueToTTLBuf = 0;
            dropDueToBufferBuf = 0;
            dropOtherBuf = 0;
        }
    }

    // =====================================================================
    // BAGIAN 2: MESSAGE LISTENER (Melacak Kejadian)
    // =====================================================================
    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        double remainingTtl = m.getTtl() - (SimClock.getTime() - m.getCreationTime());

        if (remainingTtl <= 0) {
            dropDueToTTLBuf++;
        } else if (dropped) {
            dropDueToBufferBuf++;
        } else {
            dropOtherBuf++;
        }
    }

    @Override
    public void newMessage(Message m) {}
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {}
    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}

    @Override
    public void done() {
        write("\n=== SIMULASI SELESAI ===");
        super.done();
    }
}