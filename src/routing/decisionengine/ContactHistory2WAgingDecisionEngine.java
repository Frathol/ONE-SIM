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

public class ContactHistory2WAgingDecisionEngine implements RoutingDecisionEngine {

    public static final String TYPECONTACT = "InterContact";
    public static final String AGING_FACTOR = "agingFactor";

    // Menyimpan waktu awal koneksi dimulai
    protected Map<DTNHost, Double> startTimestamps;

    // Menyimpan total durasi pertemuan akumulatif dengan setiap host
    protected Map<DTNHost, Double> contactDuration;

    protected Map<DTNHost, List<Duration>> connHistory;

    protected String typeContact;
    protected double agingFactor;
    protected double lastUpdateTime;

    public ContactHistory2WAgingDecisionEngine(Settings s) {
        if (s.contains(TYPECONTACT)) {
            typeContact = s.getSetting(TYPECONTACT);
        } else {
            typeContact = "ContactHistory";
        }

        if (s.contains(AGING_FACTOR)) {
            agingFactor = s.getDouble(AGING_FACTOR);
        } else {
            agingFactor = 0; // Default aging factor
        }

        startTimestamps = new HashMap<>();
        contactDuration = new HashMap<>();
        connHistory = new HashMap<>();
        lastUpdateTime = SimClock.getTime();
    }

    public ContactHistory2WAgingDecisionEngine(ContactHistory2WAgingDecisionEngine de) {
        startTimestamps = new HashMap<>();
        contactDuration = new HashMap<>();
        connHistory = new HashMap<>();
        this.typeContact = de.typeContact;
        this.agingFactor = de.agingFactor;
        this.lastUpdateTime = SimClock.getTime();
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
        ContactHistory2WAgingDecisionEngine de = getOtherDecisionEngine(peer);

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
        ContactHistory2WAgingDecisionEngine otherEngine = getOtherDecisionEngine(otherHost);

        if (typeContact.equals("ContactHistory")) {
            return otherEngine.getTotalDuration(dest) > this.getTotalDuration(dest);
        } else {
            return otherEngine.getTotalInter(dest) > this.getTotalInter(dest);
        }
    }

    // Menghitung total durasi kontak
    public double getTotalDuration(DTNHost dest) {
        if (!connHistory.containsKey(dest)) {
            return 0.0;
        }

        double total = 0.0;
        double now = SimClock.getTime();
        List<Duration> history = connHistory.get(dest);
        Iterator<Duration> it = history.iterator();

        while (it.hasNext()) {
            Duration dur = it.next();
            double durationVal = dur.end - dur.start;
            double age = now - dur.end; // Seberapa lama pertemuan ini berakhir

            // Aging
            // Rumus: duration * (agingFactor ^ usia_pertemuan_dalam_detik)
            total += durationVal * Math.pow(agingFactor, age);

            // Hapus durasi yang sudah sangat kecil
            if (durationVal * Math.pow(agingFactor, age) < 0.01) {
                // it.remove();
            }
        }
        return total;
    }

    // Menghitung total inter-contact time dengan host tertentu (Jeda dari akhir
    // koneksi terakhir hingga sekarang)
    public double getTotalInter(DTNHost dest) {
        if (!connHistory.containsKey(dest)) {
            return 0.0;
        }

        double total = 0.0;
        double now = SimClock.getTime();
        List<Duration> history = connHistory.get(dest);
        double lastEnd = 0.0;

        for (Duration dur : history) {
            double interTime = dur.start - lastEnd;
            double age = now - dur.start;

            total += interTime * Math.pow(agingFactor, age);
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
        double currentTime = SimClock.getTime();
        double timeSinceLastUpdate = currentTime - lastUpdateTime;

        // Lakukan aging setiap detik
        if (timeSinceLastUpdate >= 1.0) {
            for (DTNHost h : contactDuration.keySet()) {
                double oldVal = contactDuration.get(h);
                contactDuration.put(h, oldVal * agingFactor);
            }

            lastUpdateTime = currentTime;
        }
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new ContactHistory2WAgingDecisionEngine(this);
    }

    private ContactHistory2WAgingDecisionEngine getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        if (!(otherRouter instanceof DecisionEngineRouter)) {
            throw new RuntimeException("Router harus DecisionEngineRouter!");
        } // testttt
        return (ContactHistory2WAgingDecisionEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }
}