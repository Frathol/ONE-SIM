package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;

/**
 * Custom PRoPHET Router with 4 Forwarding Policies and 3 Queue Policies.
 */
public class ProphetForwardingQueuePolicy extends ActiveRouter {

  public static final double P_INIT = 0.75;
  public static final double DEFAULT_BETA = 0.25;
  public static final double GAMMA = 0.98;
  public static final String PROPHET_NS = "ProphetRouter";
  public static final String SECONDS_IN_UNIT_S = "secondsInTimeUnit";
  public static final String BETA_S = "beta";

  public static final String QUEUE_POLICY_S = "queuePolicy";
  public static final String FORWARDING_POLICY_S = "forwardingPolicy";
  public static final String COIN_PROB_S = "coinProbability";

  public enum QueuePolicy {
    FIFO, MOFO, MOPR, SHLI, LEPR, RSHLI
  }

  public enum ForwardingPolicy {
    GRTRMAX, GRTRSORT, GRTR, COIN
  }

  protected QueuePolicy currentQueuePolicy;
  protected ForwardingPolicy currentForwardingPolicy;
  protected double coinProbability;
  private Random coinRandom;

  private int secondsInTimeUnit;
  private double beta;
  private Map<DTNHost, Double> preds;
  private double lastAgeUpdate;

  public ProphetForwardingQueuePolicy(Settings s) {
    super(s);

    Settings prophetSettings = new Settings(PROPHET_NS);
    secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
    beta = prophetSettings.contains(BETA_S) ? prophetSettings.getDouble(BETA_S) : DEFAULT_BETA;

    if (s.contains(QUEUE_POLICY_S)) {
      String qp = s.getSetting(QUEUE_POLICY_S).toUpperCase();
      try {
        currentQueuePolicy = QueuePolicy.valueOf(qp);
      } catch (Exception e) {
        currentQueuePolicy = QueuePolicy.FIFO;
      }
    } else {
      currentQueuePolicy = QueuePolicy.FIFO;
    }

    if (s.contains(FORWARDING_POLICY_S)) {
      String fp = s.getSetting(FORWARDING_POLICY_S).toUpperCase();
      try {
        currentForwardingPolicy = ForwardingPolicy.valueOf(fp);
      } catch (Exception e) {
        currentForwardingPolicy = ForwardingPolicy.GRTRMAX;
      }
    } else {
      currentForwardingPolicy = ForwardingPolicy.GRTRMAX;
    }

    if (s.contains(COIN_PROB_S)) {
      this.coinProbability = s.getDouble(COIN_PROB_S);
    } else {
      this.coinProbability = 0.5;
    }

    this.coinRandom = new Random(1);
    initPreds();
  }

  protected ProphetForwardingQueuePolicy(ProphetForwardingQueuePolicy r) {
    super(r);
    this.currentQueuePolicy = r.currentQueuePolicy;
    this.currentForwardingPolicy = r.currentForwardingPolicy;
    this.coinProbability = r.coinProbability;
    this.coinRandom = new Random(1);

    this.secondsInTimeUnit = r.secondsInTimeUnit;
    this.beta = r.beta;
    initPreds();
  }

  private void initPreds() {
    this.preds = new HashMap<>();
  }

  @Override
  public void changedConnection(Connection con) {
    if (con.isUp()) {
      DTNHost otherHost = con.getOtherNode(getHost());
      updateDeliveryPredFor(otherHost);
      updateTransitivePreds(otherHost);
    }
  }

  private void updateDeliveryPredFor(DTNHost host) {
    double oldValue = getPredFor(host);
    preds.put(host, oldValue + (1 - oldValue) * P_INIT);
  }

  public double getPredFor(DTNHost host) {
    ageDeliveryPreds();
    return preds.getOrDefault(host, 0.0);
  }

  private void updateTransitivePreds(DTNHost host) {
    if (!(host.getRouter() instanceof ProphetForwardingQueuePolicy))
      return;
    ProphetForwardingQueuePolicy otherRouter = (ProphetForwardingQueuePolicy) host.getRouter();

    double pForHost = getPredFor(host);
    Map<DTNHost, Double> othersPreds = otherRouter.getDeliveryPreds();

    for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
      if (e.getKey() == getHost())
        continue;
      double pOld = getPredFor(e.getKey());
      preds.put(e.getKey(), pOld + (1 - pOld) * pForHost * e.getValue() * beta);
    }
  }

  private void ageDeliveryPreds() {
    double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) / secondsInTimeUnit;
    if (timeDiff <= 0)
      return;
    double mult = Math.pow(GAMMA, timeDiff);
    for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
      e.setValue(e.getValue() * mult);
    }
    this.lastAgeUpdate = SimClock.getTime();
  }

  private Map<DTNHost, Double> getDeliveryPreds() {
    ageDeliveryPreds();
    return this.preds;
  }

  // Queue Policy
  @Override
  protected Message getOldestMessage(boolean excludeMsgBeingSent) {
    Collection<Message> messages = this.getMessageCollection();
    Message victim = null;

    for (Message m : messages) {
      if (excludeMsgBeingSent && isSending(m.getId()))
        continue;
      if (victim == null) {
        victim = m;
        continue;
      }

      switch (currentQueuePolicy) {
        case SHLI:
          if (getRemainingTTL(m) < getRemainingTTL(victim))
            victim = m;
          break;
        case RSHLI:
          if (m.getTtl() > victim.getTtl())
            victim = m;
          break;
        case MOFO:
          if (m.getHopCount() > victim.getHopCount())
            victim = m;
          break;
        case MOPR:
          if (getFPValue(m) > getFPValue(victim))
            victim = m;
          break;
        case LEPR:
          if (getPredictabilityToDestination(m.getTo()) < getPredictabilityToDestination(victim.getTo()))
            victim = m;
          break;
        case FIFO:
        default:
          if (m.getReceiveTime() < victim.getReceiveTime())
            victim = m;
          break;
      }
    }
    return victim;
  }

  @Override
  public void update() {
    super.update();
    if (!canStartTransfer() || isTransferring())
      return;
    if (exchangeDeliverableMessages() != null)
      return;
    tryOtherMessages();
  }

  private Tuple<Message, Connection> tryOtherMessages() {
    List<Tuple<Message, Connection>> messages = new ArrayList<>();
    Collection<Message> msgCollection = getMessageCollection();

    for (Connection con : getConnections()) {
      DTNHost other = con.getOtherNode(getHost());
      if (!(other.getRouter() instanceof ProphetForwardingQueuePolicy))
        continue;

      ProphetForwardingQueuePolicy othRouter = (ProphetForwardingQueuePolicy) other.getRouter();
      if (othRouter.isTransferring())
        continue;

      for (Message m : msgCollection) {
        if (othRouter.hasMessage(m.getId()))
          continue;

        double pA = getPredFor(m.getTo());
        double pB = othRouter.getPredFor(m.getTo());

        if (currentForwardingPolicy == ForwardingPolicy.COIN) {
          if (coinRandom.nextDouble() < this.coinProbability) {
            messages.add(new Tuple<>(m, con));
          }
        } else {
          if (pB > pA) {
            messages.add(new Tuple<>(m, con));
          }
        }
      }
    }

    if (messages.isEmpty())
      return null;

    Collections.sort(messages, new TupleComparator());
    return tryMessagesForConnected(messages);
  }

  private class TupleComparator implements Comparator<Tuple<Message, Connection>> {
    public int compare(Tuple<Message, Connection> tuple1, Tuple<Message, Connection> tuple2) {
      Message m1 = tuple1.getKey();
      Message m2 = tuple2.getKey();

      ProphetForwardingQueuePolicy r1 = (ProphetForwardingQueuePolicy) tuple1.getValue().getOtherNode(getHost())
          .getRouter();
      ProphetForwardingQueuePolicy r2 = (ProphetForwardingQueuePolicy) tuple2.getValue().getOtherNode(getHost())
          .getRouter();

      double pS1 = getPredFor(m1.getTo());
      double pR1 = r1.getPredFor(m1.getTo());
      double pS2 = getPredFor(m2.getTo());
      double pR2 = r2.getPredFor(m2.getTo());

      int result = 0;

      switch (currentForwardingPolicy) {
        case GRTRMAX:
          result = Double.compare(pR2, pR1);
          break;
        case GRTRSORT:
          double diff1 = pR1 - pS1;
          double diff2 = pR2 - pS2;
          result = Double.compare(diff2, diff1);
          break;
        case GRTR:
        case COIN:
        default:
          result = 0;
          break;
      }

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
    if (other.getRouter() instanceof ProphetForwardingQueuePolicy) {
      double pValue = ((ProphetForwardingQueuePolicy) other.getRouter()).getPredFor(m.getTo());
      double currentFP = getFPValue(m);
      m.updateProperty("FP_VALUE", currentFP + pValue);
    }
  }

  @Override
  public RoutingInfo getRoutingInfo() {
    ageDeliveryPreds();
    RoutingInfo top = super.getRoutingInfo();
    RoutingInfo ri = new RoutingInfo(preds.size() + " delivery prediction(s)");
    for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
      ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f", e.getKey(), e.getValue())));
    }
    top.addMoreInfo(ri);
    return top;
  }

  @Override
  public ProphetForwardingQueuePolicy replicate() {
    return new ProphetForwardingQueuePolicy(this);
  }
}