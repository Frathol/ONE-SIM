package report;

import java.util.List;
import core.DTNHost;
import core.Message;
import core.MessageListener;

public class MessagePathReport extends Report implements MessageListener {

  public MessagePathReport() {
    init();
  }

  @Override
  protected void init() {
    super.init();
    write("=== MESSAGE PATH TRACER REPORT ===");
    write(String.format("%-15s %-15s %-10s %s", "SimTime", "MessageID", "HopCount", "RoutingPath"));
  }

  @Override
  public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
    if (isWarmupID(m.getId())) {
      return;
    }

    if (firstDelivery == true) {

      List<DTNHost> jalur = m.getHops();

      int hopCount = jalur.size() - 1;

      StringBuilder ruteString = new StringBuilder();
      for (int i = 0; i < jalur.size(); i++) {
        ruteString.append(jalur.get(i).toString());
        if (i < jalur.size() - 1) {
          ruteString.append(" -> ");
        }
      }

      String row = String.format("%-15.2f %-15s %-10d %s",
          getSimTime(),
          m.getId(),
          hopCount,
          ruteString.toString());

      write(row);
    }
  }

  @Override
  public void newMessage(Message m) {
    if (isWarmup()) {
      addWarmupID(m.getId());
    }
  }

  @Override
  public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
  }

  @Override
  public void messageDeleted(Message m, DTNHost where, boolean dropped) {
  }

  @Override
  public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
  }

  @Override
  public void done() {
    super.done();
  }
}