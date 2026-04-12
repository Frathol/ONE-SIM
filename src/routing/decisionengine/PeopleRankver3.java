package routing.decisionengine;

import java.util.*;
import core.*;
import routing.*;

public class PeopleRankver3 implements RoutingDecisionEngine {
    public static final String PEOPLERANK_NS = "PeopleRankEngine";
    public static final String MSG_PEOPLERANK_PROP = "PeopleRank";

    // Variabel untuk menyimpan skor popularitas node ini
    private double myRank;
    // Faktor redaman untuk menstabilkan perhitungan rank
    private double dampingFactor;
    private Set<DTNHost> friends;

    /**
     * Konstruktor utama
     */
    public PeopleRankver3(Settings s) {
        // Membaca nilai dari file .txt, jika tidak memakai default 0.1
        this.myRank = s.getDouble("initialRank", 0.1);

        // Membaca faktor redaman dari file .txt, jika tidak memakai default 0.85
        this.dampingFactor = s.getDouble("dampingFactor", 0.85);
    }

    /**
     * Copy Constructor
     */
    private PeopleRankver3(PeopleRankver3 prototype) {
        this.myRank = prototype.myRank;
        this.dampingFactor = prototype.dampingFactor;
        this.friends = new HashSet<>();
    }

    // while i is in contact with j d
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        PeopleRankver3 peerEngine = getOtherEngine(peer);

        // if j ∈ F(i) then
        if (peerEngine != null) {
            // receive(PeR(j), |F(j)|)
            double peerRank = peerEngine.getRank();
            int peerFriendsCount = peerEngine.getFriendsCount();

            /*
             * (Eq. 2 di paper)
             */
            double peerInfluence = (peerFriendsCount > 0) ? (peerRank / peerFriendsCount) : 0;

            // update(PeR(i))
            this.myRank = (1 - dampingFactor) + (dampingFactor * peerInfluence);
        }
    }
    // @Override
    // public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    // MessageRouter peerRouter = peer.getRouter();

    // // Memastikan tetangga menggunakan DecisionEngineRouter
    // if (peerRouter instanceof DecisionEngineRouter) {
    // DecisionEngineRouter deRouter = (DecisionEngineRouter) peerRouter;

    // // Ambil engine spesifik PeopleRank dari dalam router tetangga
    // RoutingDecisionEngine engine = deRouter.getDecisionEngine();

    // if (engine instanceof PeopleRankEngine) {
    // PeopleRankEngine peerEngine = (PeopleRankEngine) engine;

    // /*
    // * * RUMUS PEOPLERANK:
    // * Rank baru = (1 - d) + d * (Rank Tetangga)
    // */
    // this.myRank = (1 - dampingFactor) + dampingFactor * (peerEngine.getRank());
    // }
    // }
    // }

    // while ∃ m ∈ buffer(i) do
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        // if j = destination(m)
        if (m.getTo().equals(otherHost)) {
            return true;
        }

        PeopleRankver3 otherEngine = getOtherEngine(otherHost);

        if (otherEngine != null) {
            /*
             * if PeR(j) >= PeR(i)
             */
            // Forward(m,j)
            return otherEngine.getRank() >= this.myRank;
        }

        return false;
    }
    // @Override
    // public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost
    // thisHost) {
    // // Jika bertemu langsung dengan pemilik pesan, langsung kirim
    // if (m.getTo().equals(otherHost)) {
    // return true;
    // }

    // MessageRouter otherRouter = otherHost.getRouter();

    // if (otherRouter instanceof DecisionEngineRouter) {
    // DecisionEngineRouter deRouter = (DecisionEngineRouter) otherRouter;
    // RoutingDecisionEngine engine = deRouter.getDecisionEngine();

    // if (engine instanceof PeopleRankEngine) {
    // PeopleRankEngine otherEngine = (PeopleRankEngine) engine;

    // /*
    // * Hanya kirim pesan ke tetangga jika tetangga tersebut lebih 'populer'
    // * atau memiliki konektivitas yang lebih tinggi (Rank lebih besar).
    // */
    // return otherEngine.getRank() > this.myRank;
    // }
    // }

    // Jika tetangga tidak punya engine PeopleRank, tolak
    // return false;

    // }

    /**
     * Mengembalikan nilai rank saat ini
     */
    public double getRank() {
        return this.myRank;
    }

    /**
     * Mengembalikan jumlah teman (neighbors) yang pernah bertemu
     */
    public int getFriendsCount() {
        return this.friends.size();
    }

    @Override
    public RoutingDecisionEngine replicate() {
        // Membuat salinan engine untuk node-node lain di simulator
        return new PeopleRankver3(this);
    }

    private PeopleRankver3 getOtherEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        if (otherRouter instanceof DecisionEngineRouter) {
            DecisionEngineRouter deRouter = (DecisionEngineRouter) otherRouter;
            RoutingDecisionEngine engine = deRouter.getDecisionEngine();
            if (engine instanceof PeopleRankver3) {
                return (PeopleRankver3) engine;
            }
        }
        return null;
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        friends.add(peer);
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    public boolean newMessage(Message m) {
        m.addProperty(MSG_PEOPLERANK_PROP, 0.0);
        return true;
    }

    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo().equals(aHost);
    }

    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return true;
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    public void update(DTNHost thisHost) {
    }
}