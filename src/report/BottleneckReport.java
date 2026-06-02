package report;

import java.util.List;
import core.DTNHost;
import core.SimScenario;
import routing.EpidemicBottleneckRouter;

public class BottleneckReport extends Report {

  public BottleneckReport() {
    init();
  }

  @Override
  protected void init() {
    super.init();
    write("=== LAPORAN KEMACETAN JARINGAN (BOTTLENECK) ===");
    write(String.format("%-10s | %-20s", "Nama Node", "Total Nolak Gara-Gara Penuh"));
  }

  @Override
  public void done() {
    int sumAll = 0;

    List<DTNHost> semuaHost = SimScenario.getInstance().getWorld().getHosts();

    for (DTNHost host : semuaHost) {

      if (host.getRouter() instanceof EpidemicBottleneckRouter) {

        EpidemicBottleneckRouter epiRouter = (EpidemicBottleneckRouter) host.getRouter();

        int jumlahTolak = epiRouter.getTotalNolakKarenaPenuh();

        sumAll += jumlahTolak;

        if (jumlahTolak > 0) {
          write(String.format("%-10s | %-20d", host.toString(), jumlahTolak));
        }
      }
    }

    write("------------------------------------------------");
    write("Total Penolakan Se-Kota: " + sumAll);
    super.done();
  }
}