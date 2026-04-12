package routing.decisionengine;

import java.util.HashMap;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.RoutingDecisionEngine;

public class SimpleContactDecisionEngine implements RoutingDecisionEngine {

    // Menyimpan jumlah koneksi tiap node
    private Map<DTNHost, Integer> contactCount;

    public SimpleContactDecisionEngine(Settings s) {
        contactCount = new HashMap<>();
    }

    public SimpleContactDecisionEngine(SimpleContactDecisionEngine r) {
        contactCount = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        int count = contactCount.getOrDefault(thisHost, 0);

        contactCount.put(thisHost, count + 1);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        // Jika peer adalah tujuan langsung
        if (m.getTo() == otherHost) {
            return true;
        }

        int myContacts = contactCount.getOrDefault(thisHost, 0);
        int otherContacts = contactCount.getOrDefault(otherHost, 0);

        // forward jika peer lebih sering koneksi
        return otherContacts > myContacts;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SimpleContactDecisionEngine(this);
    }

}
