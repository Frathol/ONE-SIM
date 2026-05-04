package report;

import java.util.List;
import core.DTNHost;
import core.SimClock;
import core.UpdateListener;
import routing.DecisionEngineRouter;
import routing.community.CentralityDetectionEngine;

/**
 * Report untuk memanen Array Centrality dari setiap Node 
 * setiap interval waktu tertentu (Time-Series).
 */
public class CentralitySuperReport extends Report implements UpdateListener {
    
    private int interval = 100; // Catat data setiap 100 detik
    private double lastSave = 0;
    private boolean headerPrinted = false;

    public CentralitySuperReport() {
        super();
        init();
    }

    @Override
    protected void init() {
        super.init();
        interval = 100; // Sesuaikan dengan permintaan soal UTS
        lastSave = 0;
        headerPrinted = false;
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        // 1. REM WAKTU: Pastikan hanya mencatat setiap 'interval' detik
        if (SimClock.getTime() - lastSave >= interval) {
            lastSave = SimClock.getTime();

            // Cetak judul tabel HANYA SATU KALI di paling atas file
            if (!headerPrinted) {
                write("==========================================================");
                write("   LAPORAN SUPER CENTRALITY (GLOBAL & LOCAL) PER INTERVAL ");
                write("==========================================================");
                write(String.format("%-10s | %-10s | %-12s | %-12s", "Waktu(s)", "Node ID", "Global Cent", "Local Cent"));
                write("----------------------------------------------------------");
                headerPrinted = true;
            }

            // 2. KELILING KOTA: Kunjungi setiap node satu per satu
            for (DTNHost h : hosts) {
                
                // 3. CEK SERTIFIKAT 1: Apakah dia DecisionEngineRouter?
                if (h.getRouter() instanceof DecisionEngineRouter) {
                    Object de = ((DecisionEngineRouter) h.getRouter()).getDecisionEngine();
                    
                    // 4. CEK SERTIFIKAT 2: Apakah otaknya punya fungsi Centrality?
                    if (de instanceof CentralityDetectionEngine) {
                        
                        // Casting wujudnya!
                        CentralityDetectionEngine cde = (CentralityDetectionEngine) de;
                        
                        // 5. AMBIL KOTAK BEKAL (ARRAY)!
                        // Di sinilah keajaiban terjadi, kita tinggal panggil 1 method
                        int[] paketData = cde.getArrayCentrality();
                        
                        // Pastikan isi lokernya aman (tidak error)
                        if (paketData != null && paketData.length >= 2) {
                            int global = paketData[0]; // Buka laci 0
                            int local = paketData[1];  // Buka laci 1
                            
                            // 6. CETAK HASILNYA
                            write(String.format("%-10d | %-10d | %-12d | %-12d", 
                                  (int)lastSave, h.getAddress(), global, local));
                        }
                    }
                }
            }
            // Kasih garis pembatas tiap ganti interval waktu biar rapi
            write("----------------------------------------------------------");
        }
    }

    @Override
    public void done() {
        write("\n=== SIMULASI SELESAI PADA DETIK " + SimClock.getTime() + " ===");
        super.done();
    }
}