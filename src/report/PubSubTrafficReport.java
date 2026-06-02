package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class PubSubTrafficReport extends Report implements MessageListener {
    
    private int jumlahPesanPull = 0; // Subscribe / Unsubscribe
    private int jumlahPesanPush = 0; // Data / Create

    public PubSubTrafficReport() {
        init();
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        Integer tipePesan = (Integer) m.getProperty("PubSub-msgType");
        
        if (tipePesan != null) {
            if (tipePesan == 2 || tipePesan == 3) {
                jumlahPesanPull++;
            } 
            else if (tipePesan == 4 || tipePesan == 1) {
                jumlahPesanPush++;
            }
        }
    }

    @Override
    public void done() {
        write("=== LAPORAN TRAFFIC PUB/SUB ===");
        write("Total Pesan PULL (Permintaan Data) : " + jumlahPesanPull);
        write("Total Pesan PUSH (Pengiriman Data) : " + jumlahPesanPush);
        super.done();
    }

    @Override
    public void newMessage(Message m) {
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }
}