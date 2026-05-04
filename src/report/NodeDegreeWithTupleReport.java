package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.SimClock;
import core.Tuple;
import core.UpdateListener;

public class NodeDegreeWithTupleReport extends Report implements UpdateListener {
    
    private int interval = 5;
    private int lastSave = 0;
    
    // Map untuk menyimpan riwayat jumlah koneksi per node per interval.
    // Key: DTNHost, Value: List dari Tuple <Waktu, JumlahKoneksi>
    private Map<DTNHost, List<Tuple<Integer, Integer>>> degreeCount = new HashMap<>();
    
    // Menyimpan daftar waktu pencatatan (sumbu Y di tabel nanti)
    private List<Integer> timeSeries;
    // Menyimpan urutan host (sumbu X di tabel nanti)
    private List<DTNHost> hostList;

    public NodeDegreeWithTupleReport() {
        init();
    }

    @Override
    public void init() {
        super.init();
        degreeCount = new HashMap<>();
        timeSeries = new ArrayList<>();
        hostList = new ArrayList<>();
        interval = 5;
        lastSave = 0;
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        // 1. Simpan urutan host pada iterasi pertama
        if (hostList.isEmpty()) {
            hostList.addAll(hosts);
        }
        
        // 2. Cek apakah interval waktu sudah terpenuhi
        if (SimClock.getTime() - lastSave >= interval) {
            lastSave = (int) SimClock.getTime();
            
            // Catat waktu saat ini ke dalam time series
            timeSeries.add(lastSave);
            
            // 3. Catat status koneksi setiap host pada detik ini
            for (DTNHost h : hosts) {
                // Ambil list riwayat host ini (jika belum ada, buat list baru)
                List<Tuple<Integer, Integer>> history = degreeCount.getOrDefault(h, new ArrayList<>());
                
                // INI INTI LOGIKANYA: Hitung jumlah node yang sedang terhubung
                int currentDegree = h.getConnections().size();
                
                // Tambahkan data baru (Waktu, Jumlah Koneksi) ke riwayat
                history.add(new Tuple<>(lastSave, currentDegree));
                
                // Simpan kembali ke Map
                degreeCount.put(h, history);
            }
        }
    }

    @Override
    public void done() {
        write("=== NODE DEGREE PER INTERVAL (HASHMAP & TUPLE VERSION) ===");
        write("");

        // 1. CETAK HEADER (Baris Atas: Time, Host1, Host2, ...)
        String header = String.format("%-8s", "Time");
        for (DTNHost h : hostList) {
            header += String.format("%-10s", h);
        }
        write(header);

        // 2. CETAK BARIS DATA (Iterasi berdasarkan waktu)
        for (int i = 0; i < timeSeries.size(); i++) {
            int time = timeSeries.get(i);
            String row = String.format("%-8d", time);

            // Iterasi untuk masing-masing host pada waktu ke-i
            for (DTNHost h : hostList) {
                List<Tuple<Integer, Integer>> history = degreeCount.get(h);
                int connections = 0;

                // Ekstrak data dari Tuple jika datanya ada
                if (history != null && i < history.size()) {
                    connections = history.get(i).getValue();
                }

                row += String.format("%-10d", connections);
            }

            // Tulis baris tersebut ke file report
            write(String.format("%-8s", row));
        }
        
        super.done();
    }
}
