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

public class PeopleRankver2 implements RoutingDecisionEngine {

    public static final String D_FACTOR = "dampingFactor";
    private double dampingFactor;
    private double tHRank; // PeR(i)

    private Set<DTNHost> socNeighbor; // F(i)
    private Map<DTNHost, Integer> contactCount;

    public PeopleRankver2(Settings s) {
        this.dampingFactor = s.contains(D_FACTOR) ? s.getDouble(D_FACTOR) : 0.85;
        this.tHRank = 0; 
        this.socNeighbor = new HashSet<>();
        this.contactCount = new HashMap<>();
    }

    public PeopleRankver2(PeopleRankver2 r) {
        this.dampingFactor = r.dampingFactor;
        this.tHRank = 0;
        this.socNeighbor = new HashSet<>();
        this.contactCount = new HashMap<>();
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        PeopleRankver2 dePeer = getOtherDecisionEngine(peer);

        // Update jumlah pertemuan (Contact Count)
        int count = contactCount.getOrDefault(peer, 0) + 1;
        contactCount.put(peer, count);

        // Logic Threshold kamu: Masuk Social Graph jika > 5 kali bertemu
        if (count > 5) {
            this.socNeighbor.add(peer);
            dePeer.socNeighbor.add(myHost);

            // Menghitung kontribusi dari seluruh tetangga saat ini (Sesuai kode kamu)
            double sumContributionsI = 0;
            for (DTNHost neighbor : this.socNeighbor) {
                PeopleRankver2 nEngine = getOtherDecisionEngine(neighbor);
                if (nEngine != null && nEngine.socNeighbor.size() > 0) {
                    sumContributionsI += nEngine.tHRank / (double) nEngine.socNeighbor.size();
                }
            }

            double sumContributionsJ = 0;
            for (DTNHost neighborJ : dePeer.socNeighbor) {
                PeopleRankver2 nEngineJ = getOtherDecisionEngine(neighborJ);
                if (nEngineJ != null && nEngineJ.socNeighbor.size() > 0) {
                    sumContributionsJ += nEngineJ.tHRank / (double) nEngineJ.socNeighbor.size();
                }
            }

            /**
             * Update Rank secara Akumulatif (Eq.2 di Paper)
             * Hanya update jika peer/host memiliki tetangga sosial
             */
            if (dePeer.socNeighbor.size() > 0) {
                this.tHRank = (1 - dampingFactor) + (dampingFactor * sumContributionsI);
            }
            if (this.socNeighbor.size() > 0) {
                dePeer.tHRank = (1 - dampingFactor) + (dampingFactor * sumContributionsJ);
            }
        }
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) return true;
        
        PeopleRankver2 deOther = getOtherDecisionEngine(otherHost);
        // Forward jika rank lawan lebih tinggi
        return deOther.tHRank > this.tHRank;
    }

    @Override public boolean newMessage(Message m) { return true; }
    @Override public boolean isFinalDest(Message m, DTNHost aHost) { return m.getTo() == aHost; }
    @Override public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) { return m.getTo() != thisHost; }
    @Override public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) { return false; }
    @Override public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) { return m.getTo() == hostReportingOld; }
    @Override public void update(DTNHost thisHost) { }
    @Override public void connectionUp(DTNHost thisHost, DTNHost peer) { }
    @Override public void connectionDown(DTNHost thisHost, DTNHost peer) { }
    @Override public RoutingDecisionEngine replicate() { return new PeopleRankver2(this); }

    private PeopleRankver2 getOtherDecisionEngine(DTNHost h) {
        return (PeopleRankver2) ((DecisionEngineRouter) h.getRouter()).getDecisionEngine();
    }

    public double getRank() { return this.tHRank; }
}