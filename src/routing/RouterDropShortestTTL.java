package routing;

import java.util.Collection;

import core.Message;
import core.Settings;

// Router khusus yang akan menghapus pesan dengan TTL paling pendek saat buffer penuh
public class RouterDropShortestTTL extends ActiveRouter {

  public RouterDropShortestTTL(Settings s) {
    super(s);
  }

  protected RouterDropShortestTTL(RouterDropShortestTTL r) {
    super(r);
  }

  private Message getMessageRemainingTTL(boolean excludeMsgBeingSent) {
    Collection<Message> messages = this.getMessageCollection();
    Message shortest = null;
    for (Message m : messages) {

      if (excludeMsgBeingSent && isSending(m.getId())) {
        continue; // skip the message(s) that router is sending
      }

      if (shortest == null) {
        shortest = m;
      }

      else if (shortest.getTtl() > m.getTtl()) {
        shortest = m;
      }
    }
    return shortest;
  }

  @Override
  protected boolean makeRoomForMessage(int size) {
    if (size > this.getBufferSize()) {
      return false; // message too big for the buffer
    }

    int freeBuffer = this.getFreeBufferSize();
    /* delete messages from the buffer until there's enough space */
    while (freeBuffer < size) {
      Message m = getMessageRemainingTTL(true);
      if (m == null) {
        return false; // couldn't remove any more messages
      }

      deleteMessage(m.getId(), true);
      freeBuffer += m.getSize();
    }
    return true;
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
  public RouterDropShortestTTL replicate() {
    return new RouterDropShortestTTL(this);
  }

}
