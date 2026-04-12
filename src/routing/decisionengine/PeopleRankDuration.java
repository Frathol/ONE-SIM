package routing.decisionengine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

public class PeopleRankDuration implements RoutingDecisionEngine {

    public static final String D_FACTOR = "dampingFactor";
    private double dampingFactor;
    private double tHRank;

    private Set<DTNHost> socNeighbor;
    private Map<DTNHost, Double> neighborContributions;
    
    // BARU: Menyimpan total durasi kontak dengan setiap peer
    private Map<DTNHost, Double> contactDurations;
    private Map<DTNHost, Double> startTimestamps;

    public PeopleRankDuration(Settings s) {
        this.dampingFactor = s.contains(D_FACTOR) ? s.getDouble(D_FACTOR) : 0.85;
        // Inisialisasi awal dipengaruhi damping factor
        this.tHRank = 1.0 - dampingFactor; 
        
        this.socNeighbor = new HashSet<>();
        this.neighborContributions = new HashMap<>();
        this.contactDurations = new HashMap<>();
        this.startTimestamps = new HashMap<>();
    }

    public PeopleRankDuration(PeopleRankDuration r) {
        this.dampingFactor = r.dampingFactor;
        this.tHRank = 1.0 - dampingFactor;
        this.socNeighbor = new HashSet<>();
        this.neighborContributions = new HashMap<>();
        this.contactDurations = new HashMap<>();
        this.startTimestamps = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        // Catat waktu mulai koneksi
        this.startTimestamps.put(peer, SimClock.getTime());
        
        this.socNeighbor.add(peer);
        updateThisHostRank();
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        if (startTimestamps.containsKey(peer)) {
            double duration = SimClock.getTime() - startTimestamps.get(peer);
            double totalDur = contactDurations.getOrDefault(peer, 0.0) + duration;
            contactDurations.put(peer, totalDur);
            startTimestamps.remove(peer);
        }
        // Update rank setelah koneksi putus untuk mencatat perubahan durasi
        updateThisHostRank();
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        PeopleRankDuration de = getOtherDecisionEngine(peer);
        DTNHost myHost = con.getOtherNode(peer);

        // Update kontribusi: (PeR(j) / |F(j)|) * log(1 + Durasi)
        // Penggunaan log agar nilai durasi tidak meledak terlalu besar dibandingkan Rank
        double durationWeight = Math.log1p(this.contactDurations.getOrDefault(peer, 0.0));
        double contribJ = (de.tHRank / (double) de.socNeighbor.size()) * (1.0 + durationWeight);
        
        this.neighborContributions.put(peer, contribJ);
        updateThisHostRank();

        // Update untuk peer
        double peerDurationWeight = Math.log1p(de.contactDurations.getOrDefault(myHost, 0.0));
        double contribI = (this.tHRank / (double) this.socNeighbor.size()) * (1.0 + peerDurationWeight);
        
        de.neighborContributions.put(myHost, contribI);
        de.updateThisHostRank();
    }

    private void updateThisHostRank() {
        double sumContributions = 0;
        for (double c : neighborContributions.values()) {
            sumContributions += c;
        }
        // Rumus inti: d dikalikan dengan total kontribusi yang sudah berbobot durasi
        this.tHRank = (1.0 - dampingFactor) + (dampingFactor * sumContributions);
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) return true;
        
        PeopleRankDuration de = getOtherDecisionEngine(otherHost);
        
        // Logika Forwarding: Jika Rank lawan lebih tinggi
        return de.tHRank > this.tHRank;
    }

    // --- Method Standar ---
    @Override public boolean newMessage(Message m) { return true; }
    @Override public boolean isFinalDest(Message m, DTNHost aHost) { return m.getTo() == aHost; }
    @Override public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) { return m.getTo() != thisHost; }
    @Override public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) { return false; }
    @Override public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) { return m.getTo() == hostReportingOld; }
    @Override public void update(DTNHost thisHost) { }
    @Override public RoutingDecisionEngine replicate() { return new PeopleRankDuration(this); }

    private PeopleRankDuration getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        return (PeopleRankDuration) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }
}