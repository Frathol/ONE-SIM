import pandas as pd
import re

def parse_one_report(file_path):
    stats = {}
    with open(file_path, 'r') as f:
        content = f.read()
        # Ambil metrik utama
        stats['delivery_prob'] = float(re.search(r'delivery_prob: ([\d.]+)', content).group(1))
        stats['overhead_ratio'] = float(re.search(r'overhead_ratio: ([\d.]+)', content).group(1))
        stats['latency_avg'] = float(re.search(r'latency_avg: ([\d.]+)', content).group(1))
        stats['hopcount_avg'] = float(re.search(r'hopcount_avg: ([\d.]+)', content).group(1))
    return stats

# List skenario Michael
mobility_models = ['ShortestPath', 'RWP']
policies = ['FIFO', 'MOFO', 'SHLI']

all_data = []

for mob in mobility_models:
    for pol in policies:
        file_name = f"Report_Epidemic_{mob}_{pol}.txt"
        try:
            res = parse_one_report(file_name)
            res['Mobility'] = mob
            res['Policy'] = pol
            all_data.append(res)
        except FileNotFoundError:
            print(f"File {file_name} tidak ditemukan, skip.")

df = pd.DataFrame(all_data)

pivot_df = df.pivot(index='Mobility', columns='Policy', values=['delivery_prob', 'overhead_ratio', 'latency_avg'])

print(pivot_df)