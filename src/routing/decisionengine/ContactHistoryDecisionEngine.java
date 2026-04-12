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

public class ContactHistoryDecisionEngine implements RoutingDecisionEngine {

    // waktu mulai kontak
    private Map<DTNHost, Double> contactStart;

    // total durasi kontak
    private Map<DTNHost, Double> contactDuration;

    public ContactHistoryDecisionEngine(Settings s) {

        contactStart = new HashMap<>();
        contactDuration = new HashMap<>();
    }

    public ContactHistoryDecisionEngine(ContactHistoryDecisionEngine r) {

        this.contactStart = new HashMap<>(r.contactStart);
        this.contactDuration = new HashMap<>(r.contactDuration);
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        contactStart.put(peer, SimClock.getTime());
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double start = contactStart.getOrDefault(peer, SimClock.getTime());

        double duration = SimClock.getTime() - start;

        contactDuration.put(peer,
                contactDuration.getOrDefault(peer, 0.0) + duration);

        contactStart.remove(peer);
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
        // jika peer adalah tujuan langsung
        if (m.getTo() == otherHost) {
            return true;
        }

        DTNHost dest = m.getTo();

        double myHistory = contactDuration.getOrDefault(dest, 0.0);

        // DecisionEngineRouter otherRouter = (DecisionEngineRouter)
        // otherHost.getRouter();

        ContactHistoryDecisionEngine otherEngine = getOtherContactHistoryDecisionEngine(otherHost);

        double otherHistory = otherEngine.contactDuration.getOrDefault(dest, 0.0);

        return otherHistory > myHistory;
    }

    private ContactHistoryDecisionEngine getOtherContactHistoryDecisionEngine(DTNHost host) {
        DecisionEngineRouter otherRouter = (DecisionEngineRouter) host.getRouter();

        return (ContactHistoryDecisionEngine) otherRouter.getDecisionEngine();
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
        return new ContactHistoryDecisionEngine(this);
    }

}
