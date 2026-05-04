import os
import re
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
REPORT_DIR = os.path.abspath(os.path.join(BASE_DIR, "../../reports/Batch12_Helsinki/%%Group.forwardingPolicy%%_%%Group.queuePolicy%%"))
OUTPUT_DIR = os.path.join(BASE_DIR, "output")

os.makedirs(OUTPUT_DIR, exist_ok=True)
print(f"[*] Menjelajahi file report di: {REPORT_DIR}\n")

data = []

if not os.path.exists(REPORT_DIR):
    print(f"[!] ALARM: Folder {REPORT_DIR} tidak ditemukan!")
    exit()

for file_name in os.listdir(REPORT_DIR):
    if "MessageStatsReport" in file_name:
        file_path = os.path.join(REPORT_DIR, file_name)
        parts = file_name.split('_')
        
        if len(parts) >= 4:
            forwarding = parts[1]
            queue = parts[2]
            
            with open(file_path, 'r') as f:
                content = f.read()
                try:
                    # Ekstrak 6 Metrik sekaligus
                    del_prob_str = re.search(r'delivery_prob[\s:,]+([A-Za-z\d.]+)', content).group(1)
                    overhead_str = re.search(r'overhead_ratio[\s:,]+([A-Za-z\d.]+)', content).group(1)
                    latency_str = re.search(r'latency_avg[\s:,]+([A-Za-z\d.]+)', content).group(1)
                    hopcount_str = re.search(r'hopcount_avg[\s:,]+([A-Za-z\d.]+)', content).group(1)
                    dropped_str = re.search(r'dropped[\s:,]+([A-Za-z\d.]+)', content).group(1)
                    buffertime_str = re.search(r'buffertime_avg[\s:,]+([A-Za-z\d.]+)', content).group(1)
                    
                    # Convert ke float, paksa 0.0 jika 'NaN'
                    data.append({
                        'Forwarding': forwarding,
                        'Queue': queue,
                        'Delivery_Prob': 0.0 if 'NaN' in del_prob_str else float(del_prob_str),
                        'Overhead_Ratio': 0.0 if 'NaN' in overhead_str else float(overhead_str),
                        'Latency_Avg': 0.0 if 'NaN' in latency_str else float(latency_str),
                        'Hopcount_Avg': 0.0 if 'NaN' in hopcount_str else float(hopcount_str),
                        'Dropped': 0.0 if 'NaN' in dropped_str else float(dropped_str),
                        'Buffertime_Avg': 0.0 if 'NaN' in buffertime_str else float(buffertime_str)
                    })
                    print(f"[+] Berhasil diekstrak: {forwarding} + {queue}")
                except Exception as e:
                    print(f"[!] Gagal mengekstrak angka di {file_name}: {e}")

df = pd.DataFrame(data)

if df.empty:
    print("\n[!] FATAL ERROR: DataFrame kosong.")
    exit()

print("\n[*] Menyiapkan kanvas grafik (Grid 2x3)...")

sns.set_theme(style="whitegrid", context="talk")
# Ubah kanvas menjadi 2 baris, 3 kolom
fig, axes = plt.subplots(2, 3, figsize=(24, 14))
fig.suptitle('Analisis Komprehensif PRoPHET: Forwarding vs Queue Policy (Shortestpath Map Based Movement)', fontsize=24, fontweight='bold')

# Baris 1
sns.barplot(x='Forwarding', y='Delivery_Prob', hue='Queue', data=df, ax=axes[0, 0], palette='viridis')
axes[0, 0].set_title('Delivery Probability (Tinggi = Baik)')

sns.barplot(x='Forwarding', y='Overhead_Ratio', hue='Queue', data=df, ax=axes[0, 1], palette='magma')
axes[0, 1].set_title('Overhead Ratio (Rendah = Baik)')

sns.barplot(x='Forwarding', y='Latency_Avg', hue='Queue', data=df, ax=axes[0, 2], palette='rocket')
axes[0, 2].set_title('Average Latency (Rendah = Baik)')

# Baris 2
sns.barplot(x='Forwarding', y='Hopcount_Avg', hue='Queue', data=df, ax=axes[1, 0], palette='crest')
axes[1, 0].set_title('Average Hop Count')

sns.barplot(x='Forwarding', y='Dropped', hue='Queue', data=df, ax=axes[1, 1], palette='flare')
axes[1, 1].set_title('Total Dropped Messages')

sns.barplot(x='Forwarding', y='Buffertime_Avg', hue='Queue', data=df, ax=axes[1, 2], palette='mako')
axes[1, 2].set_title('Average Buffer Time (Detik)')

plt.tight_layout(rect=[0, 0.03, 1, 0.95])

output_path = os.path.join(OUTPUT_DIR, 'Grafik_Prophet.png')
plt.savefig(output_path, dpi=300)
print(f"[*] SUKSES! Grafik 6 metrik tersimpan di: {output_path}")