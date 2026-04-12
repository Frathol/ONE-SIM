package routing.decisionengine;

import java.util.HashMap;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import routing.RoutingDecisionEngine;

public class ContactDurationWFrequencyDecisionEngine implements RoutingDecisionEngine {
    // waktu mulai kontak
    private Map<DTNHost, Double> contactStart;

    // total durasi kontak
    private Map<DTNHost, Double> totalDuration;

    // jumlah kontak
    private Map<DTNHost, Integer> contactCount;

    // parameter bobot
    private double alpha = 0.5;
    private double beta = 0.5;

    public ContactDurationWFrequencyDecisionEngine(Settings s) {
        contactStart = new HashMap<>();
        totalDuration = new HashMap<>();
        contactCount = new HashMap<>();

        /* kalau mau configurable dari cfg */
        if (s.contains("alpha")) {
            alpha = s.getDouble("alpha");
        }

        if (s.contains("beta")) {
            beta = s.getDouble("beta");
        }
    }

    public ContactDurationWFrequencyDecisionEngine(ContactDurationWFrequencyDecisionEngine r) {
        contactStart = new HashMap<>();
        totalDuration = new HashMap<>();
        contactCount = new HashMap<>();

        alpha = r.alpha;
        beta = r.beta;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        contactStart.put(peer, SimClock.getTime());
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double start = contactStart.getOrDefault(peer, SimClock.getTime());

        double duration = SimClock.getTime() - start;

        totalDuration.put(peer, totalDuration.getOrDefault(peer, 0.0) + duration);

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
        // kalau peer adalah destination/
        if (m.getTo() == otherHost) {
            return true;
        }

        double myScore = getScore(thisHost);
        double otherScore = getScore(otherHost);

        return otherScore > myScore;
    }

    private double getAverageDuration(DTNHost host) {

        double total = totalDuration.getOrDefault(host, 0.0);
        int count = contactCount.getOrDefault(host, 0);

        if (count == 0) {
            return 0;
        }

        return total / count;
    }

    private double getScore(DTNHost host) {

        double avgDuration = getAverageDuration(host);
        int frequency = contactCount.getOrDefault(host, 0);

        return alpha * avgDuration + beta * frequency;
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
        return new ContactDurationWFrequencyDecisionEngine(this);
    }

}
