package routing;

import java.util.Collection;

import core.DTNHost;
import core.Message;
import core.Settings;

//Middleware Router untuk menangani berbagai Queueing Policies.
public class UniversalQueueRouter extends ActiveRouter {
  public static final String QUEUE_POLICY_S = "queuePolicy";

  public enum QueuePolicy {
    FIFO, MOFO, MOPR, SHLI, LEPR
  }

  // public static final String POLICY_FIFO = "FIFO";
  // public static final String POLICY_MOFO = "MOFO";
  // public static final String POLICY_MOPR = "MOPR";
  // public static final String POLICY_SHLI = "SHLI";
  // public static final String POLICY_LEPR = "LEPR";

  protected QueuePolicy currentPolicy;

  public UniversalQueueRouter(Settings s) {
        super(s);
        if (s.contains(QUEUE_POLICY_S)) {
            String policyStr = s.getSetting(QUEUE_POLICY_S).toUpperCase();
            try {
                // Konversi String dari .cfg menjadi Enum constant
                currentPolicy = QueuePolicy.valueOf(policyStr);
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Unknown policy '" + policyStr + "', using FIFO.");
                currentPolicy = QueuePolicy.FIFO;
            }
        } else {
            currentPolicy = QueuePolicy.FIFO;
        }
    }

    protected UniversalQueueRouter(UniversalQueueRouter r) {
        super(r);
        this.currentPolicy = r.currentPolicy;
    }

    @Override
    protected boolean makeRoomForMessage(int size) {
        if (size > this.getBufferSize()) return false;

        int freeBuffer = this.getFreeBufferSize();
        while (freeBuffer < size) {
            Message m = getVictimMessage();
            if (m == null) return false;

            deleteMessage(m.getId(), true);
            freeBuffer += m.getSize();
        }
        return true;
    }

    private Message getVictimMessage() {
        Collection<Message> messages = this.getMessageCollection();
        Message victim = null;

        for (Message m : messages) {
            if (isSending(m.getId())) continue;

            if (victim == null) {
                victim = m;
                continue;
            }

            switch (currentPolicy) {
                case SHLI: // Shortest Life Time (TTL terendah) 
                    if (m.getTtl() < victim.getTtl()) victim = m;
                    break;

                case MOFO: // Most Forwarded First (Hop count tertinggi) 
                    if (m.getHopCount() > victim.getHopCount()) victim = m;
                    break;

                case MOPR: // Most Favorably Forwarded (FP Value tertinggi) 
                    if (getFPValue(m) > getFPValue(victim)) victim = m;
                    break;

                case LEPR:  // Least Probable First (P value terendah ke tujuan) 
                    if (getPredictabilityToDestination(m.getTo()) < getPredictabilityToDestination(victim.getTo()))
                        victim = m;
                    break;

                case FIFO: // First In First Out 
                default:
                    if (m.getReceiveTime() < victim.getReceiveTime()) victim = m;
                    break;
            }
        }
        return victim;
    }

    protected double getFPValue(Message m) {
        Object prop = m.getProperty("FP_VALUE");
        return (prop != null) ? (Double) prop : 0.0;
    }

    protected double getPredictabilityToDestination(DTNHost destination) {
        return 0.0;
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public UniversalQueueRouter replicate() {
        return new UniversalQueueRouter(this);
    }
}