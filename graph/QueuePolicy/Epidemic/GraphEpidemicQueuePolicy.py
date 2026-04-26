import matplotlib.pyplot as plt
import numpy as np

# Data dari hasil simulasi Michael (Valk Laptop)
scenarios = ['SHLI_SP', 'SHLI_RWP', 'MOFO_SP', 'MOFO_RWP', 'FIFO_SP', 'FIFO_RWP']
delivery_prob = [0.3773, 0.1497, 0.3773, 0.1497, 0.3773, 0.1404]
latency_avg = [2884.77, 7622.04, 2884.77, 7622.04, 2884.77, 2239.91]

x = np.arange(len(scenarios))
width = 0.4

fig, ax1 = plt.subplots(figsize=(12, 6))

# Plot Delivery Probability (Sumbu Y Kiri)
color = '#3498db'
ax1.set_xlabel('Scenario (Policy & Movement)')
ax1.set_ylabel('Delivery Probability', color=color, fontweight='bold')
bars1 = ax1.bar(x - width/2, delivery_prob, width, label='Delivery Prob', color=color, alpha=0.8)
ax1.tick_params(axis='y', labelcolor=color)
ax1.set_ylim(0, 0.5)

# Plot Average Latency (Sumbu Y Kanan)
ax2 = ax1.twinx()
color = '#e74c3c'
ax2.set_ylabel('Average Latency (s)', color=color, fontweight='bold')
bars2 = ax2.bar(x + width/2, latency_avg, width, label='Avg Latency', color=color, alpha=0.8)
ax2.tick_params(axis='y', labelcolor=color)

# Tambah Label & Style
plt.title('Analisis Perbandingan Queue Policy & Movement Model', fontsize=14, fontweight='bold')
ax1.set_xticks(x)
ax1.set_xticklabels(scenarios, rotation=15)
ax1.grid(axis='y', linestyle='--', alpha=0.3)

# Tambah Legend
fig.tight_layout()
plt.show()