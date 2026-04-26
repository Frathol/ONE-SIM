import matplotlib.pyplot as plt
import numpy as np

# Data
labels = ['RWP', 'Reality', 'Haggle']
prophet_prob = [0.2923, 0.0215, 0.2237]
epidemic_prob = [0.4350, 0.0224, 0.2360]

prophet_overhead = [103.44, 578.63, 781.51]
epidemic_overhead = [1570.24, 368.45, 626.38]

x = np.arange(len(labels))
width = 0.35

# Plot 1: Delivery Probability
fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5))

ax1.bar(x - width/2, prophet_prob, width, label='Prophet', color='#3498db')
ax1.bar(x + width/2, epidemic_prob, width, label='Epidemic', color='#e74c3c')
ax1.set_ylabel('Probability')
ax1.set_title('Delivery Probability (Semakin Tinggi Semakin Baik)')
ax1.set_xticks(x)
ax1.set_xticklabels(labels)
ax1.legend()

# Plot 2: Overhead Ratio
ax2.bar(x - width/2, prophet_overhead, width, label='Prophet', color='#3498db')
ax2.bar(x + width/2, epidemic_overhead, width, label='Epidemic', color='#e74c3c')
ax2.set_ylabel('Ratio')
ax2.set_title('Overhead Ratio (Semakin Rendah Semakin Efisien)')
ax2.set_xticks(x)
ax2.set_xticklabels(labels)
ax2.legend()

plt.tight_layout()
plt.show()