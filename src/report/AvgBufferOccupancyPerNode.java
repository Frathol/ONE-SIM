package report;

import core.UpdateListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Settings;
import core.SimClock;

public class AvgBufferOccupancyPerNode extends Report implements UpdateListener {

    private Map<DTNHost, Double> bufferoccupancy;

    public AvgBufferOccupancyPerNode() {
        init();
    }

    public void init() {
        super.init();
        bufferoccupancy = new HashMap<>();
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        if (isWarmup())
            return;

        for (DTNHost h : hosts) {
            // if (hosts.contains(h)) {
                bufferoccupancy.put(h, (bufferoccupancy.getOrDefault(h, h.getBufferOccupancy()) + h.getBufferOccupancy())/2);
            // } else {
                // bufferoccupancy.put(h, bufferoccupancy.getOrDefault(h, 0.0) + h.getBufferOccupancy() );
            // }
        }
        printLine(hosts);
    }

	/**
	 * Prints a snapshot of the average buffer occupancy
	 * @param hosts The list of hosts in the simulation
	 */
	private void printLine(List<DTNHost> hosts) {
		double bufferOccupancy = 0.0;
		double bo2 = 0.0;

		for (DTNHost h : hosts) {
			double tmp = h.getBufferOccupancy();
			
			tmp = (tmp<=100.0)?(tmp):(100.0);
			bufferOccupancy += tmp;

			bo2 += (tmp*tmp)/100.0;
		}


		double E_X = bufferOccupancy / hosts.size();
		double Var_X = bo2 / hosts.size() - (E_X*E_X)/100.0;

		String output = format(SimClock.getTime()) + " " + format(E_X) + " " +
			format(Var_X);
		write(output);
	}

    @Override
    public void done() {
        write("=== Buffer Occupancy Report ===");
        for (DTNHost h : bufferoccupancy.keySet()) {
            write(h + ": " + " average buffer occupancy: " + bufferoccupancy.get(h));
        }

    }
}