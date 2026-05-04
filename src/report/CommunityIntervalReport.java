package report;

import java.util.List;
import core.DTNHost;
import core.SimClock;
import core.UpdateListener;
import routing.MessageRouter;
import routing.DecisionEngineRouter;
import routing.RoutingDecisionEngine;
import routing.community.CentralityDetectionEngine;

public class CommunityIntervalReport extends Report implements UpdateListener {
    private int interval = 100; 
    private double lastSave = 0;

    public CommunityIntervalReport() {
        super();
        init();
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        if (SimClock.getTime() - lastSave >= interval) {
            lastSave = SimClock.getTime();
            
            write("\n=== DATA KOMUNITAS PADA DETIK: " + lastSave + " ===");

            for (DTNHost h : hosts) {
                MessageRouter r = h.getRouter();
                if (r instanceof DecisionEngineRouter) {
                    RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
                    
                    if (de instanceof CentralityDetectionEngine) {
                        CentralityDetectionEngine cde = (CentralityDetectionEngine) de;
                        
                        // Ambil array anggota komunitas (Hasil dari step 1)
                        int[] anggota = cde.getArrayCentrality();
                        
                        String listAnggota = "";
                        for (int id : anggota) {
                            listAnggota += id + ", ";
                        }

                        // Cetak: Node [ID] -> Anggota Komunitas: [ID, ID, ID]
                        write("Node " + h.getAddress() + " -> Komunitas: [" + listAnggota + "]");
                    }
                }
            }
            write("--------------------------------------------------");
        }
    }

    @Override
    public void done() {
        super.done();
    }
}