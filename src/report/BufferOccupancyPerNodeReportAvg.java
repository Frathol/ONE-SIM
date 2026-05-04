package report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Settings;
import core.SimClock;
import core.UpdateListener;

public class BufferOccupancyPerNodeReportAvg extends Report implements UpdateListener {

    public static final String INTERVAL_SETTING = "occupancyInterval";
    public static final int DEFAULT_INTERVAL = 5;

    private double lastRecord;
    private int interval;

    // untuk ambil rata-rata per node
    private Map<DTNHost, Double> sumPerNode;
    private Map<DTNHost, Integer> countPerNode;

    public BufferOccupancyPerNodeReportAvg() {
        super();

        Settings s = getSettings();

        if (s.contains(INTERVAL_SETTING)) {
            interval = s.getInt(INTERVAL_SETTING);
        } else {
            interval = DEFAULT_INTERVAL;
        }

        init();
    }

    @Override
    public void init() {
        super.init();

        lastRecord = Double.MIN_VALUE;

        sumPerNode = new HashMap<>();
        countPerNode = new HashMap<>();
    }

    @Override
    public void updated(List<DTNHost> hosts) {

        if (SimClock.getTime() - lastRecord >= interval) {

            lastRecord = SimClock.getTime();

            // update sum dan count pernode untuk rata-ratanya
            for (DTNHost h : hosts) {

                double bo = h.getBufferOccupancy();

                if (bo > 100.0) {
                    bo = 100.0;
                }

                sumPerNode.put(h, sumPerNode.getOrDefault(h, 0.0) + bo);

                countPerNode.put(h, countPerNode.getOrDefault(h, 0) + 1);
            }
            printLine(hosts);
        }
    }

    private void printLine(List<DTNHost> hosts) {

        write(String.format("Time %.0f\n", SimClock.getTime()));

        for (DTNHost h : hosts) {

            double bo = h.getBufferOccupancy();

            if (bo > 100.0) {
                bo = 100.0;
            }
            write(String.format("%-15s : %6.2f %%\n", h.toString(), bo));
        }
        write("\n");
    }

    @Override
    public void done() {

        write("\n=== AVERAGE BUFFER OCCUPANCY PER NODE ===");

        for (DTNHost h : sumPerNode.keySet()) {

            double sum = sumPerNode.get(h);
            int count = countPerNode.get(h);

            double avg = sum / count;

            write(String.format("%-15s : %6.2f %%\n", h.toString(), avg));
        }
        super.done();
    }
}

// package report;

// import java.util.ArrayList;
// import java.util.List;

// import core.DTNHost;
// import core.SimClock;
// import core.UpdateListener;

// /**
//  * Report untuk memantau seberapa penuh Buffer (dalam persentase) 
//  * pada setiap node per interval waktu tertentu.
//  */
// public class BufferOccupancyReport extends Report implements UpdateListener {
    
//     private int interval = 10; // Cek setiap 10 detik
//     private double lastSave = 0;
//     private List<DTNHost> hostList;
    
//     // Kita langsung mencetak ke file per baris agar tidak boros RAM
//     // (Tidak perlu HashMap besar jika kita langsung 'write')
//     private boolean headerPrinted = false;

//     public BufferOccupancyReport() {
//         super();
//         init();
//     }

//     @Override
//     protected void init() {
//         super.init();
//         hostList = new ArrayList<>();
//         interval = 10; 
//         lastSave = 0;
//         headerPrinted = false;
//     }

//     @Override
//     public void updated(List<DTNHost> hosts) {
//         // Ambil daftar host pada detik pertama
//         if (hostList.isEmpty()) {
//             hostList.addAll(hosts);
//         }

//         // Cek apakah sudah waktunya mencatat (melewati interval)
//         if (SimClock.getTime() - lastSave >= interval) {
//             lastSave = SimClock.getTime();

//             // Cetak Header satu kali saja
//             if (!headerPrinted) {
//                 printHeader();
//                 headerPrinted = true;
//             }

//             // Mulai bangun baris teks untuk detik ini
//             StringBuilder row = new StringBuilder();
//             row.append(String.format("%-10d", (int) lastSave));

//             // Patroli ke semua host, intip buffernya
//             for (DTNHost h : hostList) {
//                 // getBufferOccupancy() mengembalikan nilai 0.0 (kosong) hingga 100.0 (penuh)
//                 double occupancy = h.getRouter().getBufferOccupancy();
//                 row.append(String.format("%-10.2f", occupancy));
//             }

//             // Tulis baris tersebut ke file txt
//             write(row.toString());
//         }
//     }

//     private void printHeader() {
//         write("=== PERSENTASE KAPASITAS BUFFER PER INTERVAL ===");
//         StringBuilder header = new StringBuilder();
//         header.append(String.format("%-10s", "Time(s)"));
        
//         for (DTNHost h : hostList) {
//             header.append(String.format("%-10s", h.toString()));
//         }
//         write(header.toString());
//     }

//     @Override
//     public void done() {
//         write("\n=== SIMULASI SELESAI ===");
//         super.done();
//     }
// }