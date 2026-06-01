package report;

import java.util.HashMap;
import java.util.Map;

import core.ConnectionListener;
import core.DTNHost;

public class UselessContactReport extends Report implements ConnectionListener {
  private Map<String, Double> contactDur;

  public UselessContactReport() {
    init();
  }

  @Override
  public void init() {
    super.init();
    contactDur = new HashMap<>();
  }

  @Override
  public void hostsConnected(DTNHost host1, DTNHost host2) {
    String id = host1.toString() + " - " + host2.toString();
    contactDur.put(id, getSimTime());
  }

  @Override
  public void hostsDisconnected(DTNHost host1, DTNHost host2) {
    String id = host1.toString() + " - " + host2.toString();
    if(contactDur.containsKey(id)){
      double durasi = getSimTime() - contactDur.get(id);
      if(durasi < 5){
        write("Pertemuan antara " + id + " (Durasi " + durasi + ")");
      }
    }
  }

}
