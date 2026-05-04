package report;

import java.util.ArrayList;
import java.util.List;

import core.DTNHost;
import core.SimClock;
import core.UpdateListener;

/**
 * Report untuk memantau kepadatan (jumlah koneksi aktif / Node Degree) 
 * pada setiap node per interval waktu tertentu.
 */
public class NodeDegreePerIntervalReport extends Report implements UpdateListener {
    
    private int interval = 5; // Interval waktu pencatatan (detik)
    private double lastSave = 0;
    private List<DTNHost> hostList;
    private boolean headerPrinted = false;

    public NodeDegreePerIntervalReport() {
        super();
        init();
    }

    @Override
    protected void init() {
        super.init();
        hostList = new ArrayList<>();
        interval = 5; // Bisa disesuaikan dengan permintaan dosen
        lastSave = 0;
        headerPrinted = false;
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        // 1. Tangkap daftar host di awal simulasi untuk mengunci urutan kolom
        if (hostList.isEmpty()) {
            hostList.addAll(hosts);
        }

        // 2. Cek apakah waktu saat ini sudah melewati batas interval
        if (SimClock.getTime() - lastSave >= interval) {
            lastSave = SimClock.getTime();

            // 3. Cetak Header tabel hanya satu kali
            if (!headerPrinted) {
                printHeader();
                headerPrinted = true;
            }

            // 4. Siapkan baris data (Row) dimulai dengan kolom Waktu
            StringBuilder row = new StringBuilder();
            row.append(String.format("%-8d", (int) lastSave));

            // 5. Looping ke semua host untuk menghitung jumlah koneksi mereka
            for (DTNHost h : hostList) {
                // KUNCI JAWABAN UTS: h.getConnections().size()
                // Ini akan mengembalikan angka berapa banyak node lain yang sedang berada dalam jangkauan radio
                int activeConnections = h.getConnections().size();
                
                row.append(String.format("%-10d", activeConnections));
            }

            // 6. Tulis langsung ke file txt (Lebih hemat RAM daripada disimpan di List/Map)
            write(row.toString());
        }
    }

    private void printHeader() {
        write("=== KEPADATAN KONEKSI (NODE DEGREE) PER INTERVAL ===");
        write(""); // Baris kosong biar rapi
        
        StringBuilder header = new StringBuilder();
        header.append(String.format("%-8s", "Time"));
        
        for (DTNHost h : hostList) {
            header.append(String.format("%-10s", h.toString()));
        }
        write(header.toString());
    }

    @Override
    public void done() {
        write("\n=== SIMULASI SELESAI ===");
        super.done();
    }
}