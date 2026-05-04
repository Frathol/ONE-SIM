package report;

import java.util.List;
import core.DTNHost;
import core.SimScenario;
import routing.MessageRouter;
import routing.DecisionEngineRouter;
import routing.RoutingDecisionEngine;
import routing.community.CentralityDetectionEngine;

/**
 * Report untuk mencetak Array 2D (Ego Network) dari setiap node.
 */
public class CentralityArray2DReport extends Report {

    public CentralityArray2DReport() {
        super();
        init();
    }

    @Override
    public void done() {
        write("==================================================");
        write("    LAPORAN EGO NETWORK 2D ARRAY (UTS)            ");
        write("==================================================");

        List<DTNHost> nodes = SimScenario.getInstance().getHosts();

        for (DTNHost h : nodes) {
            MessageRouter r = h.getRouter();
            
            if (r instanceof DecisionEngineRouter) {
                RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
                
                if (de instanceof CentralityDetectionEngine) {
                    CentralityDetectionEngine cde = (CentralityDetectionEngine) de;
                    
                    // 1. Tarik Matriks 2D dari Engine
                    // int[][] matrixHasil = cde.getArray2DCentrality();
                    
                    // 2. Format hasil cetakannya
                    write("\n>> Node ID [" + h.getAddress() + "] Ego Network:");
                    
                    // if (matrixHasil != null && matrixHasil.length > 0) {
                    //     // Loop untuk membaca isi array 2 dimensi
                    //     for (int i = 0; i < matrixHasil.length; i++) {
                    //         int peerID = matrixHasil[i][0];
                    //         int encounterCount = matrixHasil[i][1];
                            
                    //         write(String.format("   -> Bertemu Node %-3d sebanyak %d kali", peerID, encounterCount));
                    //     }
                    // } else {
                    //     write("   -> (Belum ada interaksi dengan node manapun)");
                    // }
                }
            }
        }
        
        write("\n==================================================");
        super.done();
    }
}