
/*
 * Bubble Rap (by Antok)
 */
package routing.community;

import java.util.*;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

public class BubbleRapv2 implements RoutingDecisionEngine, CommunityDetectionEngine, CentralityDetectionEngine {

  // Start-initialisation
  public static final String COMMUNITY_ALG_SETTING = "communityDetectAlg"; // added
  public static final String CENTRALITY_ALG_SETTING = "centralityAlg";

  protected Map<DTNHost, Double> startTimestamps;
  protected Map<DTNHost, List<Duration>> connHistory;

  protected CommunityDetection community; // added
  protected Centrality centrality;

  // End-initialisation
  // Constructor based on the settings
  public BubbleRapv2(Settings s) {
    if (s.contains(COMMUNITY_ALG_SETTING)) // added
    {
      this.community = (CommunityDetection) s.createIntializedObject(s.getSetting(COMMUNITY_ALG_SETTING));
    } else {
      this.community = new SimpleCommunityDetection(s);
    }

    if (s.contains(CENTRALITY_ALG_SETTING)) {
      this.centrality = (Centrality) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
    } else {
      this.centrality = new AverageWinCentrality(s);
    }
  }

  // Constructor based on the argument prototype
  public BubbleRapv2(BubbleRapv2 proto) {
    this.community = proto.community.replicate(); // added
    this.centrality = proto.centrality.replicate();
    startTimestamps = new HashMap<DTNHost, Double>();
    connHistory = new HashMap<DTNHost, List<Duration>>();
  }

  public void connectionUp(DTNHost thisHost, DTNHost peer) {
  }

  @Override
  public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    DTNHost myHost = con.getOtherNode(peer);
    BubbleRapv2 de = this.getOtherDecisionEngine(peer);

    this.startTimestamps.put(peer, SimClock.getTime());
    de.startTimestamps.put(myHost, SimClock.getTime());

    this.community.newConnection(myHost, peer, de.community); // added
  }

  @Override
  public void connectionDown(DTNHost thisHost, DTNHost peer) {
    // double time = startTimestamps.get(peer);
    double time = cek(thisHost, peer);
    double etime = SimClock.getTime();

    // Find or create the connection history list
    List<Duration> history;
    if (!connHistory.containsKey(peer)) {
      history = new LinkedList<Duration>();
      connHistory.put(peer, history);
    } else {
      history = connHistory.get(peer);
    }

    // add this connection to the list
    if (etime - time > 0) {
      history.add(new Duration(time, etime));
    }

    CommunityDetection peerCD = this.getOtherDecisionEngine(peer).community; // added
    community.connectionLost(thisHost, peer, peerCD, history); // added

    startTimestamps.remove(peer);
  }

  public double cek(DTNHost thisHost, DTNHost peer) {
    if (startTimestamps.containsKey(thisHost)) {
      startTimestamps.get(peer);
    }
    return 0;
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
      return true; // deliver to final destination
    }
    // now we decide where to forward a message to relay node
    DTNHost dest = m.getTo();
    BubbleRapv2 de = getOtherDecisionEngine(otherHost);

    boolean peerInCommunity = de.commumesWithHost(dest); // Is peer in dest'community
    boolean meInCommunity = this.commumesWithHost(dest); // Is THIS in dest'community

    if (peerInCommunity && !meInCommunity) // peer is in dest's community, but THIS is not
    {
      return true;
    } else if (!peerInCommunity && meInCommunity) // THIS is in dest'community, but peer is not
    {
      return false;
    } else if (peerInCommunity) // We're both in dest'community
    {
      // Forward to the one with the higher local centrality (in dest'community)
      if (de.getLocalCentrality() > this.getLocalCentrality()) {
        return true;
      } else {
        return false;
      }
    } else if (de.getGlobalCentrality() > this.getGlobalCentrality()) {
      return true;
    }

    return false;
  }

  @Override
  public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
    // delete the message once it is forwarded to the node in the dest'community

    BubbleRapv2 de = this.getOtherDecisionEngine(otherHost);
    return de.commumesWithHost(m.getTo())
        && !this.commumesWithHost(m.getTo());
  }

  @Override
  public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
    // BubbleRap de = this.getOtherDecisionEngine(hostReportingOld);
    // return de.commumesWithHost(m.getTo()) &&
    // !this.commumesWithHost(m.getTo());

    return true;
  }

  @Override
  public RoutingDecisionEngine replicate() {
    return new BubbleRapv2(this);
  }

  protected boolean commumesWithHost(DTNHost h) {
    return community.isHostInCommunity(h);
  }

  protected double getLocalCentrality() {
    return this.centrality.getLocalCentrality(connHistory, community);
  }

  protected double getGlobalCentrality() {
    return this.centrality.getGlobalCentrality(connHistory);
  }

  private BubbleRapv2 getOtherDecisionEngine(DTNHost h) {
    MessageRouter otherRouter = h.getRouter();
    assert otherRouter instanceof DecisionEngineRouter : "This router only works "
        + " with other routers of same type";

    return (BubbleRapv2) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
  }

  // for REPORT purpose: CommunityDetectionReport
  @Override
  public Set<DTNHost> getLocalCommunity() {
    return this.community.getLocalCommunity();
  }

  @Override
  public void update(DTNHost thisHost) {
  }

  @Override
  public double getGlobalDegreeCentrality() {
    return this.getGlobalCentrality();
  }

  @Override
  public double getLocalDegreeCentrality() {
    return this.getLocalCentrality();
  }

@Override
  public int[] getArrayCentrality() {
    
    int[] centArray = new int[2];

    centArray[0] = (int) this.getGlobalDegreeCentrality();

    centArray[1] = (int) this.getLocalDegreeCentrality();

    return centArray;








    // int[] centArray = new int[1];

    // if (this.connHistory != null) {
    //   centArray[0] = this.connHistory.size();
    // } else {
    //   centArray[0] = 0;
    // }

    // return centArray;

    // // Sekarang kita bikin array ukuran 2
    // int[] centArray = new int[2];

    // // Index 0: Kita isi dengan Global Degree Centrality
    // if (this.connHistory != null) {
    //   centArray[0] = this.connHistory.size();
    // } else {
    //   centArray[0] = 0;
    // }

    // // Index 1: Kita isi dengan Local Degree Centrality
    // // (Berapa banyak teman yang juga anggota komunitas yang sama)
    // int localCount = 0;
    // if (this.connHistory != null && this.community != null) {
    //     for (DTNHost peer : this.connHistory.keySet()) {
    //         // Jika teman tersebut satu komunitas, hitung!
    //         if (this.community.isHostInCommunity(peer)) {
    //             localCount++;
    //         }
    //     }
    // }
    // centArray[1] = localCount;

    // // Boom! Mengirim 2 data sekaligus dalam satu lemparan array
    // return centArray;
  }

  // @Override
  // public int[] getArrayCentrality() {
  //   // Siapkan array ukuran 2 untuk menyimpan [Global, Local]
  //   int[] centArray = new int[2];

  //   int globalCount = 0; // Penghitung untuk semua teman
  //   int localCount = 0;  // Penghitung khusus teman satu komunitas

  //   // Pastikan sejarah koneksi tidak kosong
  //   if (this.connHistory != null) {
        
  //       // Cek satu per satu semua node yang pernah ditemui
  //       for (DTNHost peer : this.connHistory.keySet()) {
            
  //           // 1. GLOBAL: Karena dia ada di connHistory, pasti pernah ketemu. Langsung tambah!
  //           globalCount++;

  //           // 2. LOCAL: Cek dulu, apakah dia teman satu komunitas?
  //           if (this.community != null && this.community.isHostInCommunity(peer)) {
  //               // Jika iya, tambahkan ke penghitung lokal!
  //               localCount++;
  //           }
  //       }
  //   }

  //   // Masukkan hasil hitungan ke dalam Array
  //   centArray[0] = globalCount;
  //   centArray[1] = localCount;

  //   // Kirim array-nya ke Report!
  //   return centArray;
  // }

  // @Override
  // public int[][] getArray2DCentrality() {
  //   // Jika belum pernah ketemu siapa-siapa, kembalikan matriks kosong
  //   if (this.connHistory == null || this.connHistory.isEmpty()) {
  //     return new int[0][0]; 
  //   }

  //   // Bikin matriks berukuran [Jumlah Teman] baris dan [2] kolom
  //   int[][] matrix = new int[this.connHistory.size()][2];
  //   int rowIndex = 0;

  //   // Ekstrak data dari HashMap connHistory
  //   for (Map.Entry<DTNHost, List<Duration>> entry : this.connHistory.entrySet()) {
  //     matrix[rowIndex][0] = entry.getKey().getAddress(); // Kolom 0: ID Teman (Peer)
  //     matrix[rowIndex][1] = entry.getValue().size();     // Kolom 1: Jumlah Pertemuan
  //     rowIndex++;
  //   }

  //   return matrix;
  // }
// public int[] getActivePeersArray() {
//     // Ambil koneksi yang sedang UP sekarang saja
//     List<Connection> connections = this.getHost().getConnections();
//     int[] activeIDs = new int[connections.size()];
    
//     for (int i = 0; i < connections.size(); i++) {
//         activeIDs[i] = connections.get(i).getOtherNode(this.getHost()).getAddress();
//     }
//     return activeIDs;
// }
// Map<Integer, Integer> centralityMap = new HashMap<>();
// centralityMap.put(h.getAddress(), nilaiCentrality);
}
// @Override
// public int[] getArrayCentrality() {
//     // 1. Siapkan lemari dengan 4 laci
//     int[] kargoData = new int[4];

//     // Laci 0: Global Centrality (Total teman)
//     kargoData[0] = (int) this.getGlobalDegreeCentrality();

//     // Laci 1: Local Centrality (Teman satu sirkel)
//     kargoData[1] = (int) this.getLocalDegreeCentrality();

//     // Laci 2: Ukuran Komunitas
//     if (this.community != null && this.community.getLocalCommunity() != null) {
//         kargoData[2] = this.community.getLocalCommunity().size();
//     } else {
//         kargoData[2] = 0; // Kalau belum punya sirkel, isinya 0
//     }

//     // Laci 3: Jumlah orang yang lagi nempel sekarang (Koneksi Aktif)
//     if (this.startTimestamps != null) {
//         kargoData[3] = this.startTimestamps.size();
//     } else {
//         kargoData[3] = 0;
//     }

//     // Kirim lemarinya ke Report!
//     return kargoData;
// }