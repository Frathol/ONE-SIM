package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.HashMap;
import java.util.Map;
public class DropPerNodeReport extends Report implements MessageListener{
    private Map<DTNHost, Integer> dropCount = new HashMap<>();

    public DropPerNodeReport() {
        init();
    }

    @Override
    public void init() {
        super.init();
        dropCount = new HashMap<>();
    }

    @Override
    public void newMessage(Message m) {
        throw new UnsupportedOperationException("Unimplemented method 'newMessage'");
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        throw new UnsupportedOperationException("Unimplemented method 'messageTransferStarted'");
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) {
            return;
        }

        if(dropCount.containsKey(where)){
            dropCount.put(where, 1);
        } else {
            dropCount.put(where, dropCount.get(where) + 1);
        }
    }

    @Override
    public void done() {
        write("=== DROP PER NODE STATISTICS ===");
        for (Map.Entry<DTNHost, Integer> entry : dropCount.entrySet()) {
            write("Host " + entry.getKey() + ": " + entry.getValue() + " drops");
        }
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        throw new UnsupportedOperationException("Unimplemented method 'messageTransferAborted'");
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        throw new UnsupportedOperationException("Unimplemented method 'messageTransferred'");
    }
    
}
