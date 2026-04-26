/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.Collection;

import core.Message;
import core.Settings;

/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class EpidemicWQueuePolicy extends ActiveRouter{
	public static final String QUEUE_POLICY_S = "queuePolicy";

  public enum QueuePolicy {
    FIFO, MOFO, MOPR, SHLI, LEPR, RSHLI
  }

  // public static final String POLICY_FIFO = "FIFO";
  // public static final String POLICY_MOFO = "MOFO";
  // public static final String POLICY_MOPR = "MOPR";
  // public static final String POLICY_SHLI = "SHLI";
  // public static final String POLICY_LEPR = "LEPR";

  protected QueuePolicy currentPolicy = QueuePolicy.FIFO;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public EpidemicWQueuePolicy(Settings s) {
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
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected EpidemicWQueuePolicy(EpidemicWQueuePolicy r) {
		super(r);
	}

	@Override
	protected Message getOldestMessage(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		Message oldest = null;
		for (Message m : messages) {
			
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}

			if (oldest == null) {
				oldest = m;
			}
			switch (currentPolicy) {
        case SHLI: // Shortest Life Time (TTL terendah)
          if (m.getTtl() < oldest.getTtl()) {
            oldest = m;
          }
          break;

        case RSHLI: // Reverse Shortest Life Time (TTL Tinggi)
          if (m.getTtl() > oldest.getTtl()) {
            oldest = m;
          }
          break;

        case MOFO: // Most Forwarded First (Hop count tertinggi)
          if (m.getHopCount() > oldest.getHopCount()) {
            oldest = m;
          }
          break;

        case FIFO: // First In First Out
        default:
          if (m.getReceiveTime() < oldest.getReceiveTime()) {
            oldest = m;
          }
          break;
      }
    }
    return oldest;
	}
			
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}
		
		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}
		
		// then try any/all message to any/all connection
		this.tryAllMessagesToAllConnections();
	}
	
	
	@Override
	public EpidemicWQueuePolicy replicate() {
		return new EpidemicWQueuePolicy(this);
	}

}