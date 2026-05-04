package report;

import java.util.HashMap;
import java.util.Map;

import core.ConnectionListener;
import core.DTNHost;

/**
 * Report untuk menghitung berapa kali sebuah pasangan Node 
 * saling bertemu (terhubung) selama simulasi.
 */
public class EncounterReport extends Report implements ConnectionListener {

    // Menyimpan jumlah pertemuan. Key: "NodeA-NodeB", Value: Jumlah Pertemuan
    private Map<String, Integer> encounterCount;

    public EncounterReport() {
        super();
        init();
    }

    @Override
    protected void init() {
        super.init();
        encounterCount = new HashMap<>();
    }

    /**
     * Fungsi bantuan untuk menstandarkan nama pasangan agar tidak double count.
     * Host 1 ketemu Host 2 akan menghasilkan key yang sama dengan Host 2 ketemu Host 1.
     */
    private String getPairKey(DTNHost h1, DTNHost h2) {
        String name1 = h1.toString();
        String name2 = h2.toString();
        
        // Urutkan berdasarkan alfabet agar konsisten
        if (name1.compareTo(name2) < 0) {
            return name1 + " <-> " + name2;
        } else {
            return name2 + " <-> " + name1;
        }
    }

    // =====================================================================
    // CONNECTION LISTENER
    // =====================================================================
    
    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        // Event ini dipanggil SATU KALI oleh simulator saat dua node masuk jangkauan radio
        String pair = getPairKey(host1, host2);
        
        // Tambahkan jumlah pertemuan untuk pasangan ini
        encounterCount.put(pair, encounterCount.getOrDefault(pair, 0) + 1);
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        // Tidak perlu melakukan apa-apa di sini karena kita cuma menghitung frekuensi pertemuannya,
        // bukan durasi berpisahnya. (Tapi method ini wajib ada karena kita implements ConnectionListener).
    }

    // =====================================================================
    // DONE
    // =====================================================================
    @Override
    public void done() {
        write("=== FREKUENSI PERTEMUAN ANTAR NODE (ENCOUNTER MATRIX) ===");
        write(String.format("%-25s | %-10s", "Pasangan Node", "Total Bertemu"));
        write("----------------------------------------");

        for (Map.Entry<String, Integer> entry : encounterCount.entrySet()) {
            write(String.format("%-25s | %-10d", entry.getKey(), entry.getValue()));
        }
        
        super.done();
    }
}