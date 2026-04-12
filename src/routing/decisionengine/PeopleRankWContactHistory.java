package routing.decisionengine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
import routing.community.Duration;

public class PeopleRankWContactHistory implements RoutingDecisionEngine {

    public static final String D_FACTOR = "dampingFactor";
    private double dampingFactor;

    // Tambahkan konstanta di bagian atas class
    public static final String AGING_FACTOR = "agingFactor";
    private double agingFactor;
    private double lastUpdateTime;

    // PeR(i) -> this Host Rank / My Rank
    private double tHRank;

    // F(i) -> Set of Neighbor / Social Graph / Kumpulan Tetangga
    private Set<DTNHost> socNeighbor;

    // Contact History Data
    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;

    public PeopleRankWContactHistory(Settings s) {
        if (s.contains(D_FACTOR)) {
            dampingFactor = s.getDouble(D_FACTOR);
        } else {
            dampingFactor = 0.85; // Default
        }

        if (s.contains(AGING_FACTOR)) {
            agingFactor = s.getDouble(AGING_FACTOR);
        } else {
            agingFactor = 0; // Default
        }

        this.tHRank = 0; // PeR(i) <- 0 (Alg 1)
        this.socNeighbor = new HashSet<>();
        this.startTimestamps = new HashMap<>();
        this.connHistory = new HashMap<>();
        this.lastUpdateTime = SimClock.getTime();
    }

    public PeopleRankWContactHistory(PeopleRankWContactHistory r) {
        this.dampingFactor = r.dampingFactor;
        this.agingFactor = r.agingFactor;
        this.tHRank = 0;
        this.socNeighbor = new HashSet<>();
        this.startTimestamps = new HashMap<>();
        this.connHistory = new HashMap<>();
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

        double sTime = startTimestamps.get(peer);
        double eTime = SimClock.getTime();

        // Find or create the connection history list
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else
            history = connHistory.get(peer);

        // add this connection to the list
        if (eTime - sTime > 0)
            history.add(new Duration(sTime, eTime));

        startTimestamps.remove(peer);
    }

    /*
     ** update PeR(i) (Eq.2 di paper)
     */
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        PeopleRankWContactHistory de = getOtherDecisionEngine(peer);

        // Catat Start Time untuk Contact History
        double now = SimClock.getTime();
        this.startTimestamps.put(peer, now);
        de.startTimestamps.put(myHost, now);

        // Tambahkan peer ke F(i) / Social graph
        this.socNeighbor.add(peer);
        de.socNeighbor.add(myHost);

        // Ambil info untuk PeR sekarang dan jumlah tetangga |F|
        double perJ = de.tHRank;
        int fJ = de.socNeighbor.size();
        double perI = this.tHRank;
        int fI = this.socNeighbor.size();

        /**
         * Update PeR(i)
         * PeR(Ni) = (1 - d) + d * (PeR(Nj) / |F(Nj)|)
         * Update secara akumulatif setiap kali bertemu (siapapun nodenya)
         */
        if (fJ > 0) {
            this.tHRank = (1 - dampingFactor) + (dampingFactor * (perJ / fJ));
        }

        if (fI > 0) {
            de.tHRank = (1 - dampingFactor) + (dampingFactor * (perI / fI));
        }

        // System.out.println("Node " + myHost + " Rank: " + tHRank + " |F|: " + fI);
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

    // Bagian -> if PeR(j) >= PeR(i) OR j = destination(m) then Forward(m,j)
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true;
        }

        PeopleRankWContactHistory de = getOtherDecisionEngine(otherHost);
        DTNHost dest = m.getTo();

        // Cek PeopleRank (Apakah Node peer lebih penting)
        boolean peerIsMoreImportant = de.tHRank >= this.tHRank;

        // Cek Contact History (Apakah Node peer lebih kenal tujuan)
        double myDurWithDest = getTotalDuration(dest);
        double peerDurWithDest = de.getTotalDuration(dest);
        boolean peerKnowsDestBetter = peerDurWithDest > myDurWithDest;

        /**
         * PeR(j) >= PeR(i)
         * Kirim jika node yg sekrang lebih penting secara sosial atau lebih kenal node
         * tujuan *
         */
        return peerIsMoreImportant || peerKnowsDestBetter;

    }

    private double getTotalDuration(DTNHost dest) {
        List<Duration> history = connHistory.get(dest);
        double total = 0;

        if (history == null) {
            return 0.0;
        }

        for (Duration d : history) {
            total += (d.end - d.start);
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

    // Kalau aging factor > 0 update dipakai, jika tidak maka update tidak dilakukan
    @Override
    public void update(DTNHost thisHost) {
        double currentTime = SimClock.getTime();
        double timeDiff = currentTime - lastUpdateTime;

        // Lakukan aging setiap 1 unit waktu (misal 1 detik)
        if (timeDiff >= 1.0) {
            this.tHRank *= Math.pow(agingFactor, timeDiff);

            // Memastikan PeR tidak turun dibawah nilai minimum agar node masih punya kesempatan dapat pesan dan naik rank
            if (this.tHRank < 0.1) {
                this.tHRank = 0.1;
            }

            this.lastUpdateTime = currentTime;
        }
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRankWContactHistory(this);
    }

    private PeopleRankWContactHistory getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        return (PeopleRankWContactHistory) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }
}