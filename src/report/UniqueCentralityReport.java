package report;

import java.util.*;

import core.*;
import routing.*;
import routing.community.CentralityDetectionEngine;

public class UniqueCentralityReport extends Report {
  // private int interval = 86400;
  // private double lastInterval = 0;
  // private boolean header = false;
  // private List<Integer> timeSeries;
  // private List<DTNHost> hostList;

  public UniqueCentralityReport() {
    init();
  }

  @Override
  protected void init() {
    super.init();
    // interval = 86400;
    // lastInterval = 0;
  }

  @Override
  public void done() {
    // Menulis header laporan
    write(String.format("%-10s | %-20s", "Node ID", "Centrality Value "));

    List<DTNHost> nodes = SimScenario.getInstance().getHosts();

    for (DTNHost h : nodes) {
      MessageRouter r = h.getRouter();

      if (!(r instanceof DecisionEngineRouter)) {
        continue;
      }

      RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();

      if (!(de instanceof CentralityDetectionEngine)) {
        continue;
      }

      CentralityDetectionEngine cde = (CentralityDetectionEngine) de;

      int[] arrayHasil = cde.getArrayCentrality();

      String printtext = h.toString() + "";
      for (int i = 0; i < arrayHasil.length; i++) {
        printtext = printtext + ":" + arrayHasil[i];
        // write(String.format("%-10d | %-20d", h.toString(), arrayHasil[0]));
      }
      write(printtext);

      // if (arrayHasil != null && arrayHasil.length > 0) {
      // write(String.format("%-10d | %-20d", h.getAddress(), arrayHasil[0]));
      // }
    }

    write("--------------------------------------------------");
    write("Selesai pada: " + SimScenario.getInstance().getEndTime() + " detik.");
    write("==================================================");

    super.done();
  }

}
