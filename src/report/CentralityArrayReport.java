package report;

import java.util.List;
import core.DTNHost;
import routing.MessageRouter;
import core.SimScenario;
import routing.DecisionEngineRouter;
import routing.RoutingDecisionEngine;
import routing.community.CentralityDetectionEngine;

/**
 * Report khusus untuk mengekstrak nilai Centrality dari Router 
 * yang mengimplementasikan CentralityDetectionEngine.
 */
public class CentralityArrayReport extends Report {

    public CentralityArrayReport() {
        super();
        init();
    }

    @Override
    public void done() {
        // Menulis header laporan
        write("==================================================");
        write("    ARRAY CENTRALITY   ");
        write("==================================================");
        write(String.format("%-10s | %-20s", "Node ID", "Centrality Value (from Array)"));
        write("--------------------------------------------------");

        List<DTNHost> nodes = SimScenario.getInstance().getHosts();

        for (DTNHost h : nodes) {
            MessageRouter r = h.getRouter();
            
            if (r instanceof DecisionEngineRouter) {
                RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
                
                if (de instanceof CentralityDetectionEngine) {
                    
                    CentralityDetectionEngine cde = (CentralityDetectionEngine) de;
                    
                    int[] arrayHasil = cde.getArrayCentrality();
                    
                    if (arrayHasil != null && arrayHasil.length > 0) {
                        write(String.format("%-10d | %-20d", h.getAddress(), arrayHasil[0]));
                    }
                }
            }
        }
        
        write("--------------------------------------------------");
        write("Selesai pada: " + SimScenario.getInstance().getEndTime() + " detik.");
        write("==================================================");
        
        super.done();
    }
}