/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import core.DTNHost;
import core.Message;
import core.Settings;

/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class EpidemicBottleneckRouter extends ActiveRouter {
  private int deniedFullCount = 0;

  /**
   * Constructor. Creates a new message router based on the settings in
   * the given Settings object.
   * 
   * @param s The settings object
   */
  public EpidemicBottleneckRouter(Settings s) {
    super(s);
  }

  /**
   * Copy constructor.
   * 
   * @param r The router prototype where setting values are copied from
   */
  protected EpidemicBottleneckRouter(EpidemicBottleneckRouter r) {
    super(r);
  }

  @Override
  public int receiveMessage(Message m, DTNHost from) {
    int hasilCek = super.receiveMessage(m, from);

    if (hasilCek == DENIED_NO_SPACE) {
      this.deniedFullCount++;
    }

    return hasilCek;
  }

  public int getTotalNolakKarenaPenuh() {
    return this.deniedFullCount;
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
  public EpidemicBottleneckRouter replicate() {
    return new EpidemicBottleneckRouter(this);
  }

}