package routing;

import core.Connection;
import core.Settings;
import core.SimClock;
import core.Tuple;
import core.Message;

import java.util.List;

/**
 * Epidemic Router dengan interval pengiriman
 */
public class IntervalEpidemicRouter extends EpidemicRouter {

    /** interval kirim (detik simulasi) */
    private double sendInterval;

    /** terakhir kali router mengirim pesan */
    private double lastSendTime = 0;

    public IntervalEpidemicRouter(Settings s) {
        super(s);

        // ambil dari config
        this.sendInterval = s.getDouble("sendInterval", 120.0);
    }

    /** copy constructor (WAJIB di ONE) */
    protected IntervalEpidemicRouter(IntervalEpidemicRouter r) {
        super(r);
        this.sendInterval = r.sendInterval;
        this.lastSendTime = r.lastSendTime;
    }

    @Override
    public IntervalEpidemicRouter replicate() {
        return new IntervalEpidemicRouter(this);
    }

    @Override
    public void update() {
        super.update();

        // kalau sedang transfer, jangan mulai baru
        if (isTransferring() || !canStartTransfer()) {
            return;
        }

        double now = SimClock.getTime();

        // cek interval
        if (now - lastSendTime < sendInterval) {
            return;
        }

        // coba kirim ke tujuan akhir
        Connection con = exchangeDeliverableMessages();

        if (con != null) {
            lastSendTime = now;
            return;
        }

        // epidemic biasa
        con = tryAllMessagesToAllConnections();

        if (con != null) {
            lastSendTime = now;
        }
    }
}