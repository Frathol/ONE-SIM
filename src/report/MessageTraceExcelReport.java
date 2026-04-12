package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

 // Report untuk mencatat setiap transmisi pesan ke dalam format CSV.
public class MessageTraceExcelReport extends Report implements MessageListener {

  public MessageTraceExcelReport() {
    super();
    write("Time;Message_ID;From_Node;To_Node;Hop_Count;Remaining_TTL;Msg_Size;Final_Dest");
  }

  @Override
  public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
    if (isWarmup())
      return;

    double time = getSimTime();
    String msgId = m.getId();
    int fromAddr = from.getAddress();
    int toAddr = to.getAddress();
    int hopCount = m.getHops().size() - 1;
    int ttl = m.getTtl();
    int size = m.getSize();

    int destAddr = m.getTo().getAddress();

    String line = String.format("%.2f;%s;%d;%d;%d;%d;%d;%d",
        time, msgId, fromAddr, toAddr, hopCount, ttl, size, destAddr);

    write(line);
  }

  @Override
  public void newMessage(Message m) {
  }

  @Override
  public void messageDeleted(Message m, DTNHost where, boolean dropped) {
  }

  @Override
  public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
  }

  @Override
  public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
  }

  @Override
  public void done() {
    super.done();
  }
}