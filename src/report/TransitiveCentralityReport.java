package report;

import java.util.List;
import core.Connection;
import core.DTNHost;
import core.SimClock;
import core.UpdateListener;
import routing.DecisionEngineRouter;
import routing.community.CentralityDetectionEngine;

public class TransitiveCentralityReport extends Report implements UpdateListener {
    private int interval = 100;
    private double lastSave = 0;

    public TransitiveCentralityReport() {
        super();
        init();
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        if (SimClock.getTime() - lastSave >= interval) {
            lastSave = SimClock.getTime();
            write("\n=== ANALISIS SENTRALITAS TETANGGA PADA DETIK " + lastSave + " ===");
            write(String.format("%-10s | %-15s | %-20s", "Node ID", "My Global Cent", "Max Neighbor Cent"));
            write("-------------------------------------------------------");

            for (DTNHost h : hosts) {
                if (h.getRouter() instanceof DecisionEngineRouter) {
                    CentralityDetectionEngine myEngine = (CentralityDetectionEngine) ((DecisionEngineRouter) h.getRouter()).getDecisionEngine();
                    
                    int myCent = myEngine.getArrayCentrality()[0]; 
                    
                    // 3. Persiapan mencari tetangga paling populer
                    int maxNeighborCent = 0;
                    
                    // 4. Report ngecek siapa aja tetangga yang lagi KONEK langsung (UP)
                    for (Connection con : h.getConnections()) {
                        DTNHost peer = con.getOtherNode(h);
                        
                        if (peer.getRouter() instanceof DecisionEngineRouter) {
                            CentralityDetectionEngine peerEngine = (CentralityDetectionEngine) ((DecisionEngineRouter) peer.getRouter()).getDecisionEngine();
                            
                            // Tanya ke Engine Tetangga: "Berapa sentralitas Global-mu?"
                            int peerCent = peerEngine.getArrayCentrality()[0];
                            
                            // Bandingkan, cari yang paling tinggi
                            if (peerCent > maxNeighborCent) {
                                maxNeighborCent = peerCent;
                            }
                        }
                    }
                    
                    // 5. Cetak hasilnya ke layar!
                    write(String.format("%-10d | %-15d | %-20d", h.getAddress(), myCent, maxNeighborCent));
                }
            }
        }
    }

    @Override
    public void done() {
        super.done();
    }
}