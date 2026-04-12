package routing.decisionengine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

public class PeopleRank implements RoutingDecisionEngine {

    public static final String D_FACTOR = "dampingFactor";

    private double dampingFactor;

    // PeR(i) -> this Host Rank / My Rank
    private double tHRank;

    // F(i) -> Set of Neighbor / Social Graph / Kumpulan Tetangga
    private Set<DTNHost> socNeighbor;

    private Map<Set<DTNHost>, Double> neighborContributions;

    private Map<DTNHost, Integer> contactCount;

    public PeopleRank(Settings s) {

        if (s.contains(D_FACTOR)) {

            dampingFactor = s.getDouble(D_FACTOR);

        } else {

            dampingFactor = 0.85; // Default

        }

        this.tHRank = 0; // PeR(i) <- 0 (Alg 1)
        this.socNeighbor = new HashSet<>();
        this.neighborContributions = new HashMap<>();
        this.contactCount = new HashMap<>();
    }

    public PeopleRank(PeopleRank r) {
        this.dampingFactor = r.dampingFactor;
        this.tHRank = 0;
        this.socNeighbor = new HashSet<>();
        this.neighborContributions = new HashMap<>();
        this.contactCount = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {

    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {

    }

    // Update PeR(i) (Eq.2 di paper)
    @Override

    public void doExchangeForNewConnection(Connection con, DTNHost peer) {

        DTNHost myHost = con.getOtherNode(peer);

        PeopleRank de = getOtherDecisionEngine(peer);

        // Tambahkan peer ke F(i) / Social graph
        // this.socNeighbor.add(peer);
        // de.socNeighbor.add(myHost);

        // Ambil info untuk PeR sekarang dan jumlah tetangga |F|
        double perJ = de.tHRank;
        int fJ = de.socNeighbor.size();
        double perI = this.tHRank;
        int fI = this.socNeighbor.size();

        // Menghitung sum kontribusi dari semua tetangga untuk update PeR(i) dan PeR(j)
        double sumContributionsI = 0;
        double sumContributionsJ = 0;
        int count = contactCount.getOrDefault(peer, 0) + 1;
        contactCount.put(peer, count);

        if (count > 5) {
            this.socNeighbor.add(peer);
            de.socNeighbor.add(myHost);

            // neighborContributions.put(peer, count);

            for (DTNHost neighbor : this.socNeighbor) {
                PeopleRank neighborEngine = getOtherDecisionEngine(neighbor);
                if (neighborEngine != null) {
                    double contrib = neighborEngine.tHRank / (double) neighborEngine.socNeighbor.size();
                    sumContributionsI += contrib;
                    this.neighborContributions.put(this.socNeighbor, contrib);
                }
            }

            for (DTNHost neighbor : de.socNeighbor) {
                PeopleRank neighborEngine = getOtherDecisionEngine(neighbor);
                if (neighborEngine != null) {
                    double contrib = neighborEngine.tHRank / (double) neighborEngine.socNeighbor.size();
                    sumContributionsJ += contrib;
                    de.neighborContributions.put(de.socNeighbor, contrib);
                }
            }
        }

        /**
         * Update PeR(i)
         * PeR(Ni) = (1 - d) + d * (PeR(Nj) / |F(Nj)|)
         * Update secara akumulatif setiap kali bertemu (siapapun nodenya)
         */

        if (fJ > 0) {
            this.tHRank = (1 - dampingFactor) + (dampingFactor * sumContributionsI);
        }
        if (fI > 0) {
            de.tHRank = (1 - dampingFactor) + (dampingFactor * sumContributionsJ);
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

    public double sumContributions(Map<Set<DTNHost>, Double> contributions) {
        double sum = 0;
        for (double c : contributions.values()) {
            sum += c;
        }
        return sum;
    }

    public double sumContributions() {
        double sum = 0;
        for (double c : neighborContributions.values()) {
            sum += c;
        }
        return sum;
    }

    public void updateThisHostRank() {
        double sumContributions = 0;
        for (double c : neighborContributions.values()) {
            sumContributions += c;
        }

        // PeR(Ni) = (1 - d) + d * (PeR(Nj) / |F(Nj)|)
        this.tHRank = (1 - dampingFactor) + (dampingFactor * sumContributions);
    }

    // Bagian -> if PeR(j) >= PeR(i) OR j = destination(m) then Forward(m,j)
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true;
        }

        PeopleRank de = getOtherDecisionEngine(otherHost);

        // DTNHost dest = m.getTo();

        /**
         * PeR(j) > PeR(i)
         * Kirim pesan kalau node yg ditemui punya Rank > dari node sekarang
         */

        return de.tHRank > this.tHRank;

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
        return new PeopleRank(this);
    }

    private PeopleRank getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        return (PeopleRank) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    public double getRank() {
        return this.tHRank;
    }

}