package routing;

import java.util.Set;

import core.DTNHost;

public interface EventDatasetInterface {

	public Set<DTNHost> getNeighborNodes(DTNHost nNode);

	public DTNHost getSelectedForwarder(); 

	public double getPredCurrentHost(); 
	
	public double getPredDestHost(); 
}
