package routing;

import java.util.Collection;

import core.Message;
import core.Settings;
import core.SimClock;

/**
 * Custom Epidemic Router with Queue Management Policies (Drop Policies).
 * Extends ActiveRouter directly to avoid parent-class overriding bugs.
 */
public class EpidemicQPolicy extends ActiveRouter {

    public static final String QUEUE_POLICY_S = "dropPolicy";

    public enum QueuePolicy {
        FIFO, MOFO, MOPR, SHLI, LEPR, RSHLI
    }

    protected QueuePolicy currentQueuePolicy;

    public EpidemicQPolicy(Settings s) {
        super(s);
        if (s.contains(QUEUE_POLICY_S)) {
            String policyStr = s.getSetting(QUEUE_POLICY_S).toUpperCase();
            try {
                currentQueuePolicy = QueuePolicy.valueOf(policyStr);
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Unknown drop policy '" + policyStr + "', using FIFO.");
                currentQueuePolicy = QueuePolicy.FIFO;
            }
        } else {
            currentQueuePolicy = QueuePolicy.FIFO;
        }
    }

    protected EpidemicQPolicy(EpidemicQPolicy r) {
        super(r);
        this.currentQueuePolicy = r.currentQueuePolicy;
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
        
        tryAllMessagesToAllConnections();
    }

    @Override
    protected Message getOldestMessage(boolean excludeMsgBeingSent) {
        Collection<Message> messages = this.getMessageCollection();
        Message victim = null;

        for (Message m : messages) {
            if (excludeMsgBeingSent && isSending(m.getId())) {
                continue;
            }

            if (victim == null) {
                victim = m;
                continue;
            }

            switch (currentQueuePolicy) {
                case SHLI: // Drop yang TTL-nya paling cepat habis
                    if (getRemainingTTL(m) < getRemainingTTL(victim)) victim = m;
                    break;
                case RSHLI: // Drop yang TTL-nya masih lama (Reverse)
                    if (m.getTtl() > victim.getTtl()) victim = m;
                    break;
                case MOFO: // Drop yang sudah paling banyak pindah tangan (hop tertinggi)
                    if (m.getHopCount() > victim.getHopCount()) victim = m;
                    break;
                case FIFO:
                default: // Drop yang paling lama mendekam di buffer node ini
                    if (m.getReceiveTime() < victim.getReceiveTime()) victim = m;
                    break;
            }
        }
        return victim;
    }

    protected double getRemainingTTL(Message m) {
        return m.getTtl() - (SimClock.getTime() - m.getCreationTime());
    }

    @Override
    public EpidemicQPolicy replicate() {
        return new EpidemicQPolicy(this);
    }
}