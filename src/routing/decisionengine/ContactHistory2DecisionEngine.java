package routing.decisionengine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.Duration;

public class ContactHistory2DecisionEngine implements RoutingDecisionEngine {

    public static final String TYPECONTACT = "InterContact";

    // Menyimpan waktu awal koneksi dimulai
    protected Map<DTNHost, Double> startTimestamps;

    // Menyimpan total durasi pertemuan akumulatif dengan setiap host
    protected Map<DTNHost, Double> contactDuration;

    protected Map<DTNHost, List<Duration>> connHistory;

    protected String typeContact;

    public ContactHistory2DecisionEngine(Settings s) {
        if (s.contains(TYPECONTACT)) {
            typeContact = s.getSetting(TYPECONTACT);
        } else {
            typeContact = "ContactHistory";
        }

        startTimestamps = new HashMap<>();
        contactDuration = new HashMap<>();
        connHistory = new HashMap<>();
    }

    public ContactHistory2DecisionEngine(ContactHistory2DecisionEngine de) {
        startTimestamps = new HashMap<>();
        contactDuration = new HashMap<>();
        connHistory = new HashMap<>();
        this.typeContact = de.typeContact;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        if (!startTimestamps.containsKey(peer)) {
            return;
        }

        // double sTime = startTimestamps.get(peer);
        // double eTime = SimClock.getTime();
        // double totalTime = eTime - sTime;

        double time = cek(thisHost, peer);
        double etime = SimClock.getTime();

        // Find or create the connection history list
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else
            history = connHistory.get(peer);

        // add this connection to the list
        if (etime - time > 0)
            history.add(new Duration(time, etime));

        startTimestamps.remove(peer);
    }

    public double cek(DTNHost thisHost, DTNHost peer) {
        if (startTimestamps.containsKey(thisHost)) {
            startTimestamps.get(peer);
        }
        return 0;
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        ContactHistory2DecisionEngine de = getOtherDecisionEngine(peer);

        double now = SimClock.getTime();
        this.startTimestamps.put(peer, now);
        de.startTimestamps.put(myHost, now);
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
        ContactHistory2DecisionEngine otherEngine = getOtherDecisionEngine(otherHost);

        if (typeContact.equals("ContactHistory")) {
            return otherEngine.getTotalDuration(dest) > this.getTotalDuration(dest);
        } else {
            return otherEngine.getTotalInter(dest) > this.getTotalInter(dest);
        }
    }

    // Menghitung total durasi kontak
    public double getTotalDuration(DTNHost dest) {
        Double total = 0.0;

        // Jika tidak ada riwayat kontak dengan host tersebut, kembalikan 0
        if (!connHistory.containsKey(dest)) {
            return 0.0;
        }

        Iterator<Duration> it = connHistory.get(dest).iterator();

        while (it.hasNext()) {
            Duration dur = it.next();
            total += dur.end - dur.start;
        }

        return total;
    }

    // Menghitung total inter-contact time dengan host tertentu (Jeda dari akhir
    // koneksi terakhir hingga sekarang)
    public double getTotalInter(DTNHost dest) {
        Double total = 0.0;

        // Jika tidak ada riwayat kontak dengan host tersebut, kembalikan 0
        if (!connHistory.containsKey(dest)) {
            return 0.0;
        }

        Iterator<Duration> it = connHistory.get(dest).iterator();
        Double lastEnd = 0.0;

        while (it.hasNext()) {
            Duration dur = it.next();
            total += dur.start - lastEnd; // Jeda dari akhir koneksi terakhir hingga awal koneksi berikutnya
            lastEnd = dur.end;
        }

        return total;
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
        return new ContactHistory2DecisionEngine(this);
    }

    private ContactHistory2DecisionEngine getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        if (!(otherRouter instanceof DecisionEngineRouter)) {
            throw new RuntimeException("Router harus DecisionEngineRouter!");
        } // testttt
        return (ContactHistory2DecisionEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }
}