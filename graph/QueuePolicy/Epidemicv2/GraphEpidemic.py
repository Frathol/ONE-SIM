import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

data = {
    'Mobility': ['ShortestPath', 'ShortestPath', 'ShortestPath', 'RWP', 'RWP', 'RWP'],
    'Policy': ['FIFO', 'MOFO', 'SHLI', 'FIFO', 'MOFO', 'SHLI'],
    'Delivery_Prob': [0.5629, 0.8163, 0.8120, 0.5629, 0.5629, 0.5629],
    'Overhead_Ratio': [140.04, 3578.99, 3463.08, 140.04, 140.04, 140.04],
    'Latency_Avg': [6384.15, 8081.90, 5929.29, 6384.15, 6384.15, 6384.15]
}

df = pd.DataFrame(data)

sns.set_theme(style="whitegrid")
fig, axes = plt.subplots(1, 3, figsize=(18, 6))
fig.suptitle('Analisis Performa Queue Policy: ShortestPath vs RWP', fontsize=18, fontweight='bold')

## --- PLOT 1: DELIVERY PROBABILITY ---
sns.barplot(x='Mobility', y='Delivery_Prob', hue='Policy', data=df, ax=axes[0], palette='viridis')
axes[0].set_title('Delivery Probability')
axes[0].set_xlabel('Model Mobilitas')
axes[0].set_ylabel('Probabilitas (0-1)') 
axes[0].set_ylim(0, 1.1) 

# --- PLOT 2: OVERHEAD RATIO ---
sns.barplot(x='Mobility', y='Overhead_Ratio', hue='Policy', data=df, ax=axes[1], palette='magma')
axes[1].set_title('Overhead Ratio')
axes[1].set_xlabel('Model Mobilitas')
axes[1].set_ylabel('Rasio Overhead') 
# axes[1].set_yscale('log') 

# --- PLOT 3: AVERAGE LATENCY ---
sns.barplot(x='Mobility', y='Latency_Avg', hue='Policy', data=df, ax=axes[2], palette='rocket')
axes[2].set_title('Average Latency')
axes[2].set_xlabel('Model Mobilitas')
axes[2].set_ylabel('Rata-rata Delay (Detik)') 

# Rapikan layout
plt.tight_layout(rect=[0, 0.03, 1, 0.95])

# Simpan ke PNG 
# plt.savefig('hasil_simulasi_valk.png', dpi=300)
plt.show()