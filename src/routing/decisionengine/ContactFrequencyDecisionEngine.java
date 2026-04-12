package routing.decisionengine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.Duration;
import routing.community.FrequencyDecisionEngine;

public class ContactFrequencyDecisionEngine implements RoutingDecisionEngine {

    private Map<DTNHost, Integer> contactCount;
    protected Map<DTNHost, List<Duration>> connHistory;

    public ContactFrequencyDecisionEngine(Settings s) {
        this.contactCount = new HashMap<>();
        this.connHistory = new HashMap<>();
    }

    public ContactFrequencyDecisionEngine(ContactFrequencyDecisionEngine other) {
        this.contactCount = new HashMap<>();
        this.contactCount = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        contactCount.put(peer, contactCount.getOrDefault(peer, 0) + 1);
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
        if (m.getTo() == otherHost) {
            return true;
        }

        DTNHost dest = m.getTo();
        ContactFrequencyDecisionEngine otherEngine = getOtherDecisionEngine(otherHost);

        int thisCount = contactCount.getOrDefault(dest, 0);
        int peerCount = otherEngine.contactCount.getOrDefault(dest, 0);

        return peerCount > thisCount;
    }

    public Map<DTNHost, List<Duration>> getFrequency(DTNHost dest) {
        Map<DTNHost, List<Duration>> frequency = new HashMap<>();

        for (Map.Entry<DTNHost, Integer> entry : contactCount.entrySet()) {
            DTNHost host = entry.getKey();
            int count = entry.getValue();
            frequency.put(host, List.of(new Duration(0, count)));
        }

        return frequency;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    private ContactFrequencyDecisionEngine getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        if (!(otherRouter instanceof DecisionEngineRouter)) {
            throw new RuntimeException("Router harus DecisionEngineRouter!");
        }
        return (ContactFrequencyDecisionEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new ContactFrequencyDecisionEngine(this);
    }

    // @Override
    // public Map<DTNHost, List<Duration>> getFrequency() {
    // Map<DTNHost, List<Duration>> frequency = new HashMap<>();

    // for (Map.Entry<DTNHost, Integer> entry : contactCount.entrySet()) {
    // DTNHost host = entry.getKey();
    // int count = entry.getValue();
    // frequency.put(host, List.of(new Duration(0, count)));
    // }

    // return frequency;
    // }
}
