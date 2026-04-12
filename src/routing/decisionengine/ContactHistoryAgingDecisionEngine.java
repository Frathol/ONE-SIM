package routing.decisionengine;

import java.util.HashMap;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import routing.DecisionEngineRouter;
import routing.RoutingDecisionEngine;

public class ContactHistoryAgingDecisionEngine implements RoutingDecisionEngine {
    // waktu mulai kontak
    private Map<DTNHost, Double> contactStart;

    // total durasi kontak
    private Map<DTNHost, Double> contactDuration;

    private double lastAgeUpdate;

    private int secondsInTimeUnit;

    private static final double GAMMA = 0.98;

    public ContactHistoryAgingDecisionEngine(Settings s) {

        contactStart = new HashMap<>();
        contactDuration = new HashMap<>();

        secondsInTimeUnit = 30;
    }

    public ContactHistoryAgingDecisionEngine(ContactHistoryAgingDecisionEngine r) {

        contactStart = new HashMap<>();
        contactDuration = new HashMap<>();

        secondsInTimeUnit = r.secondsInTimeUnit;
    }

    // Aging
    private void ageContactDurations() {

        double timeDiff = (SimClock.getTime() - lastAgeUpdate) / secondsInTimeUnit;

        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(GAMMA, timeDiff);

        for (Map.Entry<DTNHost, Double> e : contactDuration.entrySet()) {

            e.setValue(e.getValue() * mult);
        }

        lastAgeUpdate = SimClock.getTime();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        contactStart.put(peer, SimClock.getTime());
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {

        double start = contactStart.getOrDefault(peer, SimClock.getTime());

        double duration = SimClock.getTime() - start;

        contactDuration.put(peer, contactDuration.getOrDefault(peer, 0.0) + duration);
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
        ageContactDurations();

        DTNHost dest = m.getTo();

        double myDuration = contactDuration.getOrDefault(dest, 0.0);

        DecisionEngineRouter otherRouter = (DecisionEngineRouter) otherHost.getRouter();

        ContactHistoryAgingDecisionEngine otherEngine = (ContactHistoryAgingDecisionEngine) otherRouter
                .getDecisionEngine();

        double otherDuration = otherEngine.contactDuration.getOrDefault(dest, 0.0);

        return otherDuration > myDuration;
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
        return new ContactHistoryAgingDecisionEngine(this);
    }

}
