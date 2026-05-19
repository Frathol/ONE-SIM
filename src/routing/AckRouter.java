package routing;

import java.util.HashSet;
import java.util.Set;

import core.DTNHost;
import core.Message;
import core.Settings;

public class AckRouter extends EpidemicRouter {

  private Set<String> curedMessages;
  private static final String RECEIPT_PREFIX = "ACK_";
  private static final String ACK_TTL = "ack_ttl";
  private static final int RECEIPT_SIZE = 1;

  private int ackTtl;

  public AckRouter(Settings s) {
    super(s);
    this.curedMessages = new HashSet<String>();

    if (s.contains(ACK_TTL)) {
      this.ackTtl = s.getInt(ACK_TTL);
      if (ackTtl <= 0) {
        throw new IllegalArgumentException("Ack TTL must be positive");
      } else {
        this.ackTtl = 300;
      }
    }
  }

  protected AckRouter(AckRouter r) {
    super(r);
    this.curedMessages = new HashSet<String>(r.curedMessages);
    this.ackTtl = r.ackTtl;
  }

  @Override
  protected int checkReceiving(Message m) {
    String msgId = m.getId();
    if (!msgId.startsWith(RECEIPT_PREFIX) && curedMessages.contains(msgId)) {
      return DENIED_OLD;
    }

    return super.checkReceiving(m);
  }

  @Override
  public Message messageTransferred(String id, DTNHost from) {
    Message m = super.messageTransferred(id, from);
    String msgId = m.getId();

    if (msgId.startsWith(RECEIPT_PREFIX)) {
      String orgMsgId = msgId.substring(RECEIPT_PREFIX.length());

      if (hasMessage(orgMsgId)) {
        deleteMessage(orgMsgId, false);
      }
      curedMessages.add(orgMsgId);
      return m;
    }
    if (m.getTo() == getHost()) {
      String receiptId = RECEIPT_PREFIX + msgId;

      if (!hasMessage(receiptId) && !curedMessages.contains(msgId)) {

        Message receiptMsg = new Message(getHost(), m.getFrom(), receiptId, RECEIPT_SIZE);

        receiptMsg.setTtl(ackTtl);

        addToMessages(receiptMsg, true);

        curedMessages.add(msgId);
        System.out.println("Host " + getHost() + " received message " + msgId + " and created receipt " + receiptId);
      }
    }
    return m;
  }

  @Override
  public AckRouter replicate() {
    return new AckRouter(this);
  }
}
