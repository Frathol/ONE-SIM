package report;

import java.util.List;
import java.util.Set;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimScenario;
import routing.ProphetRouter;

public class EventDatasetReport extends Report implements MessageListener {
  private Set<DTNHost> neighborNodes;

  public EventDatasetReport() {
    init();
  }

  @Override
  protected void init() {
    super.init();
    write(String.format("%-15s %-15s %-15s %-25s %-25s %-25s %-10s %s", "Key", "CurrentNode", "MessageID",
        "PredCurrentHost", "PredDestHost",
        "SelectedForwarder", "DestNode", "Neighbor_Node"));
  }

  @Override
  public void newMessage(Message m) {
  }

  @Override
  public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    // if (isWarmupID(m.getId())) {
    // return;
    // }

    // List<DTNHost> semuaHost = SimScenario.getInstance().getHosts();

    // for (DTNHost host : semuaHost) {

    // if (host.getRouter() instanceof ProphetRouter) {

    // ProphetRouter cRouter = (ProphetRouter) host.getRouter();
    // neighborNodes = cRouter.getNeighborNodes(host);

    // Set<DTNHost> nNode = cRouter.getNeighborNodes(host);

    // String row = String.format("%-15.2f %-15s %-15s %-25s %-25s %-25s %-10s %s",
    // getSimTime(),
    // from.getAddress(),
    // m.getId(),
    // cRouter.getPredCurrentHost(),
    // cRouter.getPredDestHost(),
    // cRouter.getSelectedForwarder() != null ?
    // cRouter.getSelectedForwarder().toString() : "null",
    // to.getAddress(),
    // nNode.toString());

    // write(row);
    // }
    // }
    // double predCurrentHost = 0.0;
    // double predDestHost = 0.0;
    // // DTNHost selectedForwarder = null;

    // if (isWarmupID(m.getId())) {
    // return;
    // }

    // if (from.getRouter() instanceof ProphetRouter) {

    // ProphetRouter cRouter = (ProphetRouter) from.getRouter();
    // predCurrentHost = cRouter.getPredFor(m.getTo());
    // neighborNodes = cRouter.getNeighborNodes();
    // }
    // if (to.getRouter() instanceof ProphetRouter) {
    // ProphetRouter cRouter = (ProphetRouter) to.getRouter();
    // predDestHost = cRouter.getPredFor(m.getTo());
    // }

    // // Set<DTNHost> nNode = cRouter.getNeighborNodes();

    // String row = String.format("%-15.2f %-15s %-15s %-25s %-25s %-25s %-10s %s",
    // getSimTime(),
    // from.getAddress(),
    // m.getId(),
    // predCurrentHost,
    // predDestHost,
    // to.toString(),
    // to.getAddress(),
    // neighborNodes.toString());

    // write(row);
  }

  @Override
  public void messageDeleted(Message m, DTNHost where, boolean dropped) {
  }

  @Override
  public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
  }

  @Override
  public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
    double predCurrentHost = 0.0;
    double predDestHost = 0.0;
    // DTNHost selectedForwarder = null;

    if (isWarmupID(m.getId())) {
      return;
    }

    if (from.getRouter() instanceof ProphetRouter) {

      ProphetRouter cRouter = (ProphetRouter) from.getRouter();
      predCurrentHost = cRouter.getPredFor(m.getTo());
      neighborNodes = cRouter.getNeighborNodes();
    }
    if (to.getRouter() instanceof ProphetRouter) {
      ProphetRouter cRouter = (ProphetRouter) to.getRouter();
      predDestHost = cRouter.getPredFor(m.getTo());
    }

    // Set<DTNHost> nNode = cRouter.getNeighborNodes();

    String row = String.format("%-15.2f %-15s %-15s %-25s %-25s %-25s %-10s %s",
        getSimTime(),
        from.getAddress(),
        m.getId(),
        predCurrentHost,
        predDestHost,
        to.toString(),
        to.getAddress(),
        neighborNodes.toString());

    write(row);
  }

  @Override
  public void done() {
    super.done();
  }

}
