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

public class ContactHistoryAgingWFreqDecisionEngine implements RoutingDecisionEngine {
    // waktu mulai kontak
    private Map<DTNHost, Double> contactStart;

    // total durasi kontak
    private Map<DTNHost, Double> contactDuration;

    // jumlah frekuensi kontak
    private Map<DTNHost, Integer> contactFrequency;

    // waktu terakhir kali durasi kontak di-aging
    private double lastAgeUpdate;

    // satuan waktu untuk aging
    private int secondsInTimeUnit;

    // faktor pengurangan untuk aging
    private static final double GAMMA = 0.98;

    public ContactHistoryAgingWFreqDecisionEngine(Settings s) {

        contactStart = new HashMap<>();
        contactDuration = new HashMap<>();
        contactFrequency = new HashMap<>();

        secondsInTimeUnit = 30;
    }

    public ContactHistoryAgingWFreqDecisionEngine(ContactHistoryAgingWFreqDecisionEngine r) {

        contactStart = new HashMap<>();
        contactDuration = new HashMap<>();
        contactFrequency = new HashMap<>();

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

    // contact score
    private double getContactScore(DTNHost host) {

        double duration = contactDuration.getOrDefault(host, 0.0);
        int frequency = contactFrequency.getOrDefault(host, 0);

        // log untuk menormalkan duration
        double durationScore = Math.log(duration + 1);

        return durationScore + frequency;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {

        contactStart.put(peer, SimClock.getTime());

        // tambah frequency
        contactFrequency.put(peer,
                contactFrequency.getOrDefault(peer, 0) + 1);
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

        if (m.getTo() == otherHost) {
            return true;
        }

        ageContactDurations();

        DTNHost dest = m.getTo();

        double myScore = getContactScore(dest);

        DecisionEngineRouter otherRouter = (DecisionEngineRouter) otherHost.getRouter();

        ContactHistoryAgingWFreqDecisionEngine otherEngine = (ContactHistoryAgingWFreqDecisionEngine) otherRouter
                .getDecisionEngine();

        double otherScore = otherEngine.getContactScore(dest);

        return otherScore > myScore;
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
        return new ContactHistoryAgingWFreqDecisionEngine(this);
    }

}
