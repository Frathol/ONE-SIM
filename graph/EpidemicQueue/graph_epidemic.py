import os
import re
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

REPORT_DIR = os.path.abspath(os.path.join(BASE_DIR, "../../reports/QueuePolicy/BatchResult2/Epidemic_%%Group.dropPolicy%%_%%Group.bufferSize%%"))
OUTPUT_DIR = os.path.join(BASE_DIR, "output")

os.makedirs(OUTPUT_DIR, exist_ok=True)
print(f"[*] Menjelajahi file report di: {REPORT_DIR}\n")

data = []

if not os.path.exists(REPORT_DIR):
    print(f"[!] ALARM: Folder {REPORT_DIR} tidak ditemukan!")
    exit()

for root, dirs, files in os.walk(REPORT_DIR):
    for file_name in files:
        if "MessageStatsReport" in file_name:
            file_path = os.path.join(root, file_name)
            
            if "Epidemic" in file_name:
                target_name = file_name
            else:
                target_name = os.path.basename(root)
            
            parts = target_name.split('_')
            
            if len(parts) >= 3 and parts[0] == "Epidemic":
                drop_policy = parts[1]
                buffer_size = parts[2]
                
                with open(file_path, 'r') as f:
                    content = f.read()
                    try:
                        del_prob_str = re.search(r'delivery_prob[\s:,]+([A-Za-z\d.]+)', content).group(1)
                        overhead_str = re.search(r'overhead_ratio[\s:,]+([A-Za-z\d.]+)', content).group(1)
                        latency_str = re.search(r'latency_avg[\s:,]+([A-Za-z\d.]+)', content).group(1)
                        hopcount_str = re.search(r'hopcount_avg[\s:,]+([A-Za-z\d.]+)', content).group(1)
                        dropped_str = re.search(r'dropped[\s:,]+([A-Za-z\d.]+)', content).group(1)
                        buffertime_str = re.search(r'buffertime_avg[\s:,]+([A-Za-z\d.]+)', content).group(1)
                        
                        data.append({
                            'Drop_Policy': drop_policy,
                            'Buffer_Size': buffer_size,
                            'Delivery_Prob': 0.0 if 'NaN' in del_prob_str else float(del_prob_str),
                            'Overhead_Ratio': 0.0 if 'NaN' in overhead_str else float(overhead_str),
                            'Latency_Avg': 0.0 if 'NaN' in latency_str else float(latency_str),
                            'Hopcount_Avg': 0.0 if 'NaN' in hopcount_str else float(hopcount_str),
                            'Dropped': 0.0 if 'NaN' in dropped_str else float(dropped_str),
                            'Buffertime_Avg': 0.0 if 'NaN' in buffertime_str else float(buffertime_str)
                        })
                        print(f"[+] Berhasil diekstrak: {drop_policy} + {buffer_size} (dari {target_name})")
                    except Exception as e:
                        print(f"[!] Gagal mengekstrak angka di {file_path}: {e}")

df = pd.DataFrame(data)

if df.empty:
    print("\n[!] FATAL ERROR: DataFrame kosong.")
    print(f"Pastikan simulasi berhasil dan ada file di {REPORT_DIR}")
    exit()

print("\n[*] Menyiapkan kanvas grafik (Grid 2x3)...")

sns.set_theme(style="whitegrid", context="talk")
fig, axes = plt.subplots(2, 3, figsize=(24, 14))
fig.suptitle('Analisis Epidemic: Buffer Size vs Drop Policy', fontsize=24, fontweight='bold')

buffer_order = ['1M', '2M', '5M']

# Baris 1
sns.barplot(x='Buffer_Size', y='Delivery_Prob', hue='Drop_Policy', data=df, ax=axes[0, 0], palette='viridis', order=buffer_order)
axes[0, 0].set_title('Delivery Probability (Tinggi = Baik)')

sns.barplot(x='Buffer_Size', y='Overhead_Ratio', hue='Drop_Policy', data=df, ax=axes[0, 1], palette='magma', order=buffer_order)
axes[0, 1].set_title('Overhead Ratio (Rendah = Baik)')

sns.barplot(x='Buffer_Size', y='Latency_Avg', hue='Drop_Policy', data=df, ax=axes[0, 2], palette='rocket', order=buffer_order)
axes[0, 2].set_title('Average Latency (Rendah = Baik)')

# Baris 2
sns.barplot(x='Buffer_Size', y='Hopcount_Avg', hue='Drop_Policy', data=df, ax=axes[1, 0], palette='crest', order=buffer_order)
axes[1, 0].set_title('Average Hop Count')

sns.barplot(x='Buffer_Size', y='Dropped', hue='Drop_Policy', data=df, ax=axes[1, 1], palette='flare', order=buffer_order)
axes[1, 1].set_title('Total Dropped Messages')

sns.barplot(x='Buffer_Size', y='Buffertime_Avg', hue='Drop_Policy', data=df, ax=axes[1, 2], palette='mako', order=buffer_order)
axes[1, 2].set_title('Average Buffer Time (Detik)')

plt.tight_layout(rect=[0, 0.03, 1, 0.95])

output_path = os.path.join(OUTPUT_DIR, 'Grafik_Epidemic_Lengkap.png')
plt.savefig(output_path, dpi=300)
print(f"[*] SUKSES! Grafik 6 metrik tersimpan di: {output_path}")