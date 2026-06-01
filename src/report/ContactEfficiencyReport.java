package report;

import java.util.HashSet;
import java.util.Set;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;

public class ContactEfficiencyReport extends Report implements ConnectionListener, MessageListener {
    private int totalContact = 0;
    private Set<String> contactSuccess = new HashSet<>();

    public ContactEfficiencyReport() {
        init();
    }

    @Override
    public void init() {
        super.init();
        totalContact = 0;
        contactSuccess.clear();
    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        totalContact++;
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        String koneksiID = from.toString() + "-" + to.toString();
        contactSuccess.add(koneksiID); 
    }

    @Override
    public void done() {
        int totalSukses = contactSuccess.size();
        int pertemuanZonk = totalContact - totalSukses;

        write("Total Contact: " + totalContact);
        write("Contact dengan Transfer: " + totalSukses);
        write("Contact ZONK: " + pertemuanZonk);
        super.done();
    }

    @Override
    public void newMessage(Message m) {
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
    }
}
