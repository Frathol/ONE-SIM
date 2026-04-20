package routing;

import java.util.Collection;

import core.DTNHost;
import core.Message;
import core.Settings;

//Middleware Router untuk menangani berbagai Queueing Policies.
public class UniversalQueueRouter extends ActiveRouter {
  public static final String QUEUE_POLICY_S = "queuePolicy";

  public static final String POLICY_FIFO = "FIFO";
  public static final String POLICY_MOFO = "MOFO";
  public static final String POLICY_MOPR = "MOPR";
  public static final String POLICY_SHLI = "SHLI";
  public static final String POLICY_LEPR = "LEPR";

  protected String currentPolicy;

  public UniversalQueueRouter(Settings s) {
    super(s);
    if (s.contains(QUEUE_POLICY_S)) {
      currentPolicy = s.getSetting(QUEUE_POLICY_S);
    } else {
      currentPolicy = POLICY_FIFO;
    }
  }

  protected UniversalQueueRouter(UniversalQueueRouter r) {
    super(r);
    this.currentPolicy = r.currentPolicy;
  }

  @Override
  protected boolean makeRoomForMessage(int size) {
    if (size > this.getBufferSize()) {
      return false;
    }

    int freeBuffer = this.getFreeBufferSize();
    while (freeBuffer < size) {
      Message m = getVictimMessage();
      if (m == null)
        return false;

      deleteMessage(m.getId(), true);
      freeBuffer += m.getSize();
    }
    return true;
  }

  private Message getVictimMessage() {
    Collection<Message> messages = this.getMessageCollection();
    Message victim = null;

    for (Message m : messages) {
      if (isSending(m.getId())) {
        continue;
      }

      if (victim == null) {
        victim = m;
        continue;
      }

      switch (currentPolicy) {
        case POLICY_SHLI: // Shortest Life Time (TTL terendah)
          if (m.getTtl() < victim.getTtl()) {
            victim = m;
          }
          break;

        case POLICY_FIFO: // First In First Out
          if (m.getReceiveTime() < victim.getReceiveTime()) {
            victim = m;
          }
          break;

        case POLICY_MOFO: // Most Forwarded First (Hop count tertinggi)
          if (m.getHopCount() > victim.getHopCount()) {
            victim = m;
          }
          break;

        case POLICY_MOPR: // Most Favorably Forwarded (FP Value tertinggi)
          double mFP = getFPValue(m);
          double victimFP = getFPValue(victim);
          if (mFP > victimFP) {
            victim = m;
          }
          break;

        case POLICY_LEPR: // Least Probable First (P value terendah ke tujuan)
          double mP = getPredictabilityToDestination(m.getTo());
          double victimP = getPredictabilityToDestination(victim.getTo());
          if (mP < victimP) {
            victim = m;
          }
          break;

        default: // Default kembali ke FIFO
          if (m.getReceiveTime() < victim.getReceiveTime()) {
            victim = m;
          }
          break;
      }
    }
    return victim;
  }

  // Helper untuk mengambil nilai FP (untuk MOPR) dari properti pesan
  protected double getFPValue(Message m) {
    Object prop = m.getProperty("FP_VALUE");
    return (prop != null) ? (Double) prop : 0.0;
  }

  // Helper untuk mengambil nilai P (untuk LEPR)
  // Fungsi ini akan di-override oleh Prophet nanti
  protected double getPredictabilityToDestination(DTNHost destination) {
    return 0.0;
  }

  @Override
  public void update() {
    super.update();
    // if (isTransferring() || !canStartTransfer())
    //   return;

    // if (exchangeDeliverableMessages() != null)
    //   return;

    // this.tryAllMessagesToAllConnections();
  }

  @Override
  public UniversalQueueRouter replicate() {
    return new UniversalQueueRouter(this);
  }
}