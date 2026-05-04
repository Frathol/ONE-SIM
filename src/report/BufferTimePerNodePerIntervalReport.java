package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;
import core.Tuple;
import core.UpdateListener;

public class BufferTimePerNodePerIntervalReport extends Report implements MessageListener, UpdateListener {
    
    private int interval = 5;
    private int lastSave = 0;

    // 1. TRACKER KEDATANGAN PESAN (Key: MsgID_NodeAddress, Value: Arrival Time)
    private Map<String, Double> arrivalTimes = new HashMap<>();

    // 2. WADAH SEMENTARA PER INTERVAL (Akan di-reset tiap interval)
    // Menyimpan total akumulasi waktu buffer per node di interval saat ini
    private Map<DTNHost, Double> bufferTimeSumBuf = new HashMap<>();
    // Menyimpan jumlah pesan yang di-forward per node di interval saat ini
    private Map<DTNHost, Integer> forwardCountBuf = new HashMap<>();

    // 3. WADAH PENYIMPANAN PERMANEN (Time-Series untuk output akhir)
    // Key: DTNHost, Value: List of Tuple <Waktu, Rata-Rata Buffer Time>
    private Map<DTNHost, List<Tuple<Integer, Double>>> avgBufferTimeHistory = new HashMap<>();
    
    private List<Integer> timeSeries = new ArrayList<>();
    private List<DTNHost> hostList = new ArrayList<>();

    public BufferTimePerNodePerIntervalReport() {
        init();
    }

    @Override
    public void init() {
        super.init();
        arrivalTimes = new HashMap<>();
        bufferTimeSumBuf = new HashMap<>();
        forwardCountBuf = new HashMap<>();
        avgBufferTimeHistory = new HashMap<>();
        timeSeries = new ArrayList<>();
        hostList = new ArrayList<>();
        interval = 5;
        lastSave = 0;
    }

    // =====================================================================
    // BAGIAN 1: UPDATE LISTENER (Mencatat dan Mereset setiap Interval)
    // =====================================================================
    @Override
    public void updated(List<DTNHost> hosts) {
        if (hostList.isEmpty()) {
            hostList.addAll(hosts);
        }
        
        if (SimClock.getTime() - lastSave >= interval) {
            lastSave = (int) SimClock.getTime();
            timeSeries.add(lastSave);
            
            for (DTNHost h : hosts) {
                // Ambil nilai sementara di interval ini
                double sum = bufferTimeSumBuf.getOrDefault(h, 0.0);
                int count = forwardCountBuf.getOrDefault(h, 0);
                
                // Hitung rata-rata waktu antre di interval ini
                double avgTime = (count > 0) ? (sum / count) : 0.0;
                
                // Simpan ke riwayat permanen
                List<Tuple<Integer, Double>> history = avgBufferTimeHistory.getOrDefault(h, new ArrayList<>());
                history.add(new Tuple<>(lastSave, avgTime));
                avgBufferTimeHistory.put(h, history);
                
                // RESET wadah sementara untuk interval berikutnya
                bufferTimeSumBuf.put(h, 0.0);
                forwardCountBuf.put(h, 0);
            }
        }
    }

    // =====================================================================
    // BAGIAN 2: MESSAGE LISTENER (Melacak Waktu Inap Pesan)
    // =====================================================================
    @Override
    public void newMessage(Message m) {
        String key = m.getId() + "_" + m.getFrom().getAddress();
        arrivalTimes.put(key, SimClock.getTime());
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        // A. Pesan keluar dari node 'from', hitung berapa lama dia menginap!
        String keyFrom = m.getId() + "_" + from.getAddress();
        if (arrivalTimes.containsKey(keyFrom)) {
            double arrivalTime = arrivalTimes.get(keyFrom);
            double timeInBuffer = SimClock.getTime() - arrivalTime;
            
            // Tambahkan ke wadah SEMENTARA milik node 'from'
            bufferTimeSumBuf.put(from, bufferTimeSumBuf.getOrDefault(from, 0.0) + timeInBuffer);
            forwardCountBuf.put(from, forwardCountBuf.getOrDefault(from, 0) + 1);
            
            // Hapus dari tracker agar tidak bocor
            arrivalTimes.remove(keyFrom);
        }

        // B. Pesan masuk ke node 'to', catat waktu kedatangannya
        if (!firstDelivery) {
            String keyTo = m.getId() + "_" + to.getAddress();
            arrivalTimes.put(keyTo, SimClock.getTime());
        }
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        String key = m.getId() + "_" + where.getAddress();
        arrivalTimes.remove(key);
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}

    // =====================================================================
    // BAGIAN 3: DONE (Mencetak Hasil ke dalam bentuk Tabel Time-Series)
    // =====================================================================
    @Override
    public void done() {
        write("=== RATA-RATA WAKTU ANTRE (DETIK) PER NODE PER INTERVAL ===");
        write("");

        // Cetak Header (Sumbu X: Daftar Host)
        String header = String.format("%-8s", "Time");
        for (DTNHost h : hostList) {
            header += String.format("%-10s", h);
        }
        write(header);

        // Cetak Baris (Sumbu Y: Waktu interval)
        for (int i = 0; i < timeSeries.size(); i++) {
            int time = timeSeries.get(i);
            String row = String.format("%-8d", time);

            for (DTNHost h : hostList) {
                List<Tuple<Integer, Double>> history = avgBufferTimeHistory.get(h);
                double avgBufferTime = 0.0;

                // Ambil nilai Double (rata-rata waktu antre) dari Tuple
                if (history != null && i < history.size()) {
                    avgBufferTime = history.get(i).getValue();
                }

                // Format angka di belakang koma agar rapi
                row += String.format("%-10.2f", avgBufferTime);
            }

            write(String.format("%-8s", row));
        }
        super.done();
    }
}