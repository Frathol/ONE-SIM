package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.ConnectionListener;
import java.util.ArrayList;
import java.util.List;

// Untuk menggabungkan statistik pesan dengan statistik kontak (berapa kali node saling bertemu)
public class ContactAndMessageReport extends Report implements MessageListener, ConnectionListener {
    private int nrofCreated = 0;
    private int nrofDelivered = 0;
    private int nrofContacts = 0;
    private List<Double> latencies = new ArrayList<>();

    public ContactAndMessageReport() {
        init();
    }

    // Listener untuk menghitung berapa banyak koneksi (CONN) yang terjadi
    @Override
    public void hostsConnected(DTNHost h1, DTNHost h2) {
        nrofContacts++;
    }

    @Override
    public void hostsDisconnected(DTNHost h1, DTNHost h2) {}

    @Override
    public void newMessage(Message m) {
        if (isWarmup()) return;
        nrofCreated++;
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
        if (isWarmup() || !finalTarget) return;
        nrofDelivered++;
        latencies.add(getSimTime() - m.getCreationTime());
    }

    @Override
    public void done() {
        write("=== Report Stationary & CONN ===");
        write("Total Contacts (CONN events) : " + nrofContacts);
        write("Messages Created             : " + nrofCreated);
        write("Messages Delivered           : " + nrofDelivered);
        
        double prob = (nrofCreated > 0) ? (double) nrofDelivered / nrofCreated : 0;
        write("Delivery Probability         : " + format(prob));
        write("Average Latency              : " + getAverage(latencies));
        
        double contactEfficiency = (nrofContacts > 0) ? (double) nrofDelivered / nrofContacts : 0;
        write("Contact Efficiency Ratio     : " + format(contactEfficiency));
        
        super.done();
    }

    public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
}