package routing;

import core.*;
import java.util.*;

public class universalDropRouter extends ActiveRouter {

    public static final String DROP_POLICY_S = "dropPolicy";

    public enum dropPolicy {
        FIFO, DO, DY, DL, DS, SHLI, LHLI, MOFO, MOPR, LEPR
    }

    protected dropPolicy currentPolicy;

    public universalDropRouter(Settings s) {
        super(s);
        if (s.contains(DROP_POLICY_S)) {
            String policyStr = s.getSetting(DROP_POLICY_S).toUpperCase();
            try {
                // Konversi String dari .cfg menjadi Enum constant
                currentPolicy = dropPolicy.valueOf(policyStr);
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Unknown policy '" + policyStr + "', using FIFO.");
                currentPolicy = dropPolicy.FIFO;
            }
        } else {
            currentPolicy = dropPolicy.FIFO;
        }
    }

    protected universalDropRouter(universalDropRouter r) {
        super(r);
        this.currentPolicy = r.currentPolicy;
    }

    /**
     * Helper untuk probabilitas pengiriman ke tujuan.
     */
    protected double getDeliveryProbability(Message m) {
        return 0.0;
    }

    /**
     * Menghitung sisa waktu hidup pesan (Remaining TTL)
     */
    protected double getRemainingTTL(Message m) {
        return m.getTtl() - (SimClock.getTime() - m.getCreationTime());
    }

    /**
     * Memilih pesan untuk dihapus berdasarkan kebijakan (Policy)
     */
    protected Message getMessageToDrop() {
        Collection<Message> messages = getMessageCollection();
        Message victim = null;

        for (Message m : messages) {
            if (isSending(m.getId()))
                continue;

            if (victim == null) {
                victim = m;
                continue;
            }

            switch (currentPolicy) {
                case FIFO:

                case DO:
                    // Drop Oldest: Hapus yang pertama kali masuk ke buffer node ini.
                    if (m.getReceiveTime() < victim.getReceiveTime())
                        victim = m;
                    break;

                case DY:
                    // Drop Youngest: Hapus yang paling terakhir masuk ke buffer.
                    if (m.getReceiveTime() > victim.getReceiveTime())
                        victim = m;
                    break;

                case DL:
                    // Drop Largest: Hapus pesan yang ukurannya paling besar (byte).
                    if (m.getSize() > victim.getSize())
                        victim = m;
                    break;

                case DS :
                    // Drop Smallest: Hapus pesan yang ukurannya paling kecil (byte).
                    if (m.getSize() < victim.getSize())
                        victim = m;
                    break;

                case SHLI:
                    // Shortest Remaining Life: Hapus pesan yang TTL-nya paling cepat habis.
                    if (getRemainingTTL(m) < getRemainingTTL(victim))
                        victim = m;
                    break;

                case LHLI:
                    // Longest Remaining Life: Hapus pesan yang TTL-nya paling lama habis.
                    if (getRemainingTTL(m) > getRemainingTTL(victim))
                        victim = m;
                    break;

                case MOFO:

                case MOPR:
                    // Most Frequently Forwarded: Hapus yang sudah sering "melompat" (hop count tinggi).
                    if (m.getHopCount() > victim.getHopCount())
                        victim = m;
                    break;

                case LEPR:
                    // Least Probable First: Asumsikan hop count rendah.
                    if (m.getHopCount() < victim.getHopCount())
                        victim = m;
                    break;

                default:
                    // Fallback ke FIFO jika policy tidak dikenal
                    if (m.getReceiveTime() < victim.getReceiveTime())
                        victim = m;
                    break;
            }
        }
        return victim;
    }

    @Override
    protected boolean makeRoomForMessage(int size) {
        if (size > this.getBufferSize())
            return false;

        while (getFreeBufferSize() < size) {
            Message m = getMessageToDrop();
            if (m == null)
                return false;
            deleteMessage(m.getId(), true);
        }
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (!canStartTransfer() || isTransferring())
            return;
        if (exchangeDeliverableMessages() != null)
            return;
        tryAllMessagesToAllConnections();
    }

    @Override
    public MessageRouter replicate() {
        return new universalDropRouter(this);
    }
}