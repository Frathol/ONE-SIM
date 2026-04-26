package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;

public class ProphetForwardingQueuePolicy extends ProphetRouter {

  public static final String QUEUE_POLICY_S = "queuePolicy";
  public static final String FORWARDING_POLICY_S = "forwardingPolicy";

  public enum QueuePolicy {
    FIFO, MOFO, MOPR, SHLI, LEPR, RSHLI
  }

  public enum ForwardingPolicy {
    GRTRMAX, GRTRSORT, GRTR, COIN
  }

  protected QueuePolicy currentQueuePolicy;
  protected ForwardingPolicy currentForwardingPolicy;
  private Random coinRandom; // Untuk strategi COIN

  public ProphetForwardingQueuePolicy(Settings s) {
    super(s);
    if (s.contains(QUEUE_POLICY_S)) {
      String policyStr1 = s.getSetting(QUEUE_POLICY_S).toUpperCase();
      try {
        currentQueuePolicy = QueuePolicy.valueOf(policyStr1);
      } catch (IllegalArgumentException e) {
        System.err.println("Warning: Unknown policy '" + policyStr1 + "', using FIFO.");
        currentQueuePolicy = QueuePolicy.FIFO;
      }
    } else {
      currentQueuePolicy = QueuePolicy.FIFO;
    }

    if (s.contains(FORWARDING_POLICY_S)) {
      String policyStr2 = s.getSetting(FORWARDING_POLICY_S).toUpperCase();
      try {
        currentForwardingPolicy = ForwardingPolicy.valueOf(policyStr2);
      } catch (IllegalArgumentException e) {
        System.err.println("Warning: Unknown Forwarding policy '" + policyStr2 + "', using GRTRMAX.");
        currentForwardingPolicy = ForwardingPolicy.GRTRMAX;
      }
    } else {
      currentForwardingPolicy = ForwardingPolicy.GRTRMAX;
    }
    
    this.coinRandom = new Random(1); // Seed statis untuk konsistensi simulasi, bisa diubah
  }

  protected ProphetForwardingQueuePolicy(ProphetForwardingQueuePolicy r) {
    super(r);
    this.currentQueuePolicy = r.currentQueuePolicy;
    this.currentForwardingPolicy = r.currentForwardingPolicy;
    this.coinRandom = new Random(1);
  }

  @Override
  protected Message getOldestMessage(boolean excludeMsgBeingSent) {
    Collection<Message> messages = this.getMessageCollection();
    Message oldest = null;
    for (Message m : messages) {

      if (excludeMsgBeingSent && isSending(m.getId())) {
        continue; 
      }

      if (oldest == null) {
        oldest = m;
        continue;
      }
      
      switch (currentQueuePolicy) {
        case SHLI: 
          if (getRemainingTTL(m) < getRemainingTTL(oldest)) oldest = m;
          break;
        case RSHLI: 
          if (m.getTtl() > oldest.getTtl()) oldest = m;
          break;
        case MOFO: 
          if (m.getHopCount() > oldest.getHopCount()) oldest = m;
          break;
        case MOPR: 
          if (getFPValue(m) > getFPValue(oldest)) oldest = m;
          break;
        case LEPR: 
          if (getPredictabilityToDestination(m.getTo()) < getPredictabilityToDestination(oldest.getTo())) oldest = m;
          break;
        case FIFO:
        default:
          if (m.getReceiveTime() < oldest.getReceiveTime()) oldest = m;
          break;
      }
    }
    return oldest;
  }

  @Override
  public void update() {
    super.update();
    if (!canStartTransfer() || isTransferring()) {
      return; 
    }

    if (exchangeDeliverableMessages() != null) {
      return;
    }
    tryOtherMessages();
  }

  private Tuple<Message, Connection> tryOtherMessages() {
    List<Tuple<Message, Connection>> messages = new ArrayList<>();
    Collection<Message> msgCollection = getMessageCollection();

    for (Connection con : getConnections()) {
      DTNHost other = con.getOtherNode(getHost());
      ProphetRouter othRouter = (ProphetRouter) other.getRouter();

      if (othRouter.isTransferring()) continue;

      for (Message m : msgCollection) {
        if (othRouter.hasMessage(m.getId())) continue;

        double pA = getPredFor(m.getTo());
        double pB = othRouter.getPredFor(m.getTo());

        if (currentForwardingPolicy == ForwardingPolicy.COIN) {
            // COIN: Forward dengan peluang 50%
            if (coinRandom.nextDouble() < 0.5) {
                messages.add(new Tuple<>(m, con));
            }
        } else {
            // GRTR, GRTRMAX, GRTRSORT: Harus P(B) > P(A)
            if (pB > pA) {
                messages.add(new Tuple<>(m, con));
            }
        }
      }
    }

    if (messages.isEmpty()) return null;

    Collections.sort(messages, new TupleComparator());
    return tryMessagesForConnected(messages); 
  }

  private class TupleComparator implements Comparator<Tuple<Message, Connection>> {

    public int compare(Tuple<Message, Connection> tuple1, Tuple<Message, Connection> tuple2) {
      Message m1 = tuple1.getKey();
      Message m2 = tuple2.getKey();
      
      ProphetForwardingQueuePolicy r1 = (ProphetForwardingQueuePolicy) tuple1.getValue().getOtherNode(getHost()).getRouter();
      ProphetForwardingQueuePolicy r2 = (ProphetForwardingQueuePolicy) tuple2.getValue().getOtherNode(getHost()).getRouter();

      double pS1 = getPredFor(m1.getTo());
      double pR1 = r1.getPredFor(m1.getTo());

      double pS2 = getPredFor(m2.getTo());
      double pR2 = r2.getPredFor(m2.getTo());

      int result = 0;

      // Pengurutan berdasarkan strategi
      switch (currentForwardingPolicy) {
        case GRTRMAX:
          result = Double.compare(pR2, pR1); // Tertinggi dulu
          break;
        case GRTRSORT:
          double diff1 = pR1 - pS1;
          double diff2 = pR2 - pS2;
          result = Double.compare(diff2, diff1); // Selisih tertinggi dulu
          break;
        case GRTR:
        case COIN:
        default:
          result = 0; // Tidak diurutkan berdasarkan probabilitas
          break;
      }

      // Tie-breaker menggunakan antrean (Queue Policy)
      if (result == 0) {
        return compareByQueueMode(m1, m2); 
      }
      return result;
    }
  }

  protected double getFPValue(Message m) {
    Object prop = m.getProperty("FP_VALUE");
    return (prop != null) ? (Double) prop : 0.0;
  }

  protected double getRemainingTTL(Message m) {
    return m.getTtl() - (SimClock.getTime() - m.getCreationTime());
  }

  protected double getPredictabilityToDestination(DTNHost dest) {
    return this.getPredFor(dest);
  }

  @Override
  protected void transferDone(Connection con) {
    Message m = con.getMessage();
    DTNHost other = con.getOtherNode(getHost());

    double pValue = ((ProphetForwardingQueuePolicy) other.getRouter()).getPredFor(m.getTo());

    double currentFP = getFPValue(m);
    m.updateProperty("FP_VALUE", currentFP + pValue);
  }

  @Override
  public ProphetForwardingQueuePolicy replicate() {
    return new ProphetForwardingQueuePolicy(this);
  }
}