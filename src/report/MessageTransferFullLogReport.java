package report;

import java.util.ArrayList;
import java.util.List;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Report untuk mencatat setiap kejadian transfer pesan secara detail.
 * Data dikumpulkan selama simulasi dan ditulis sekaligus ke file saat selesai (done).
 */
public class MessageTransferFullLogReport extends Report implements MessageListener {
    // List untuk menyimpan seluruh baris record selama simulasi berjalan
    private List<String> transferRecords;

    public MessageTransferFullLogReport() {
        init();
    }

    @Override
    public void init() {
        super.init();
        transferRecords = new ArrayList<>();
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
        if (isWarmup()) return;

        double time = getSimTime();
        String msgId = m.getId();
        int fromAddr = from.getAddress();
        int toAddr = to.getAddress();
        int hopCount = m.getHops().size() - 1;
        int ttl = m.getTtl();
        int size = m.getSize();
        int destAddr = m.getTo().getAddress();

        String row = String.format("%.2f;%s;%d;%d;%d;%d;%d;%d",
                time, msgId, fromAddr, toAddr, hopCount, ttl, size, destAddr);

        transferRecords.add(row);
    }

    public void newMessage(Message m) {}
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}

    @Override
    public void done() {
        StringBuilder header = new StringBuilder();
        header.append("Time;Message_ID;From_Node;To_Node;Hop_Count;Remaining_TTL;Msg_Size;Final_Dest");
        write(header.toString());

        for (String record : transferRecords) {
            write(record);
        }

        super.done();
    }
}