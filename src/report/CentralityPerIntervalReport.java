package report;

import java.util.List;
import core.DTNHost;
import core.SimClock;
import core.UpdateListener;
import routing.MessageRouter;
import routing.DecisionEngineRouter;
import routing.RoutingDecisionEngine;
import routing.community.CentralityDetectionEngine;

/**
 * Report Time-Series untuk merekam pertumbuhan nilai Centrality 
 * dari setiap node setiap X detik menggunakan UpdateListener.
 */
public class CentralityPerIntervalReport extends Report implements UpdateListener {

    private int interval = 100; // Catat data setiap 100 detik
    private double lastSave = 0;
    private boolean headerPrinted = false;

    public CentralityPerIntervalReport() {
        super();
        init();
    }

    @Override
    protected void init() {
        super.init();
        interval = 100; // Interval bisa kamu ubah sesuai soal UTS
        lastSave = 0;
        headerPrinted = false;
    }

    // =====================================================================
    // UPDATE LISTENER: CCTV yang merekam setiap Interval waktu
    // =====================================================================
    @Override
    public void updated(List<DTNHost> hosts) {
        
        // Cek apakah waktu simulator sudah melewati interval yang ditentukan
        if (SimClock.getTime() - lastSave >= interval) {
            lastSave = SimClock.getTime();

            // Cetak Header tabel cukup satu kali saja di awal file
            if (!headerPrinted) {
                write("=== PERTUMBUHAN CENTRALITY PER INTERVAL ===");
                write(String.format("%-10s | %-10s | %-15s | %-15s", "Waktu(s)", "Node ID", "Global Cent", "Local Cent"));
                write("---------------------------------------------------------------");
                headerPrinted = true;
            }

            // Kunjungi semua node pada detik ini
            for (DTNHost h : hosts) {
                MessageRouter r = h.getRouter();
                
                if (r instanceof DecisionEngineRouter) {
                    RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
                    
                    // Cek Sertifikat Engine
                    if (de instanceof CentralityDetectionEngine) {
                        CentralityDetectionEngine cde = (CentralityDetectionEngine) de;
                        
                        // Tarik Array-nya [Global, Local]
                        int[] arrayHasil = cde.getArrayCentrality();
                        
                        if (arrayHasil != null && arrayHasil.length >= 2) {
                            // Cetak Baris Datanya!
                            // Format: Waktu Saat Ini | ID Node | Global | Local
                            write(String.format("%-10d | %-10d | %-15d | %-15d", 
                                (int)lastSave, h.getAddress(), arrayHasil[0], arrayHasil[1]));
                        }
                    }
                }
            }
            // Beri jarak antar interval agar mudah dibaca di file .txt
            write("---------------------------------------------------------------");
        }
    }

    @Override
    public void done() {
        write("\n=== SIMULASI SELESAI PADA DETIK KE " + SimClock.getTime() + " ===");
        super.done();
    }
}