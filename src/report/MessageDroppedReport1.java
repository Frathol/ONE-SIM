package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimScenario;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Reports drops that happened during message deletions per nodes. Ignores warmup period.
 *
 * @author narwa
 * */
public class MessageDroppedReport1 extends Report implements MessageListener {

	private Map<DTNHost, Integer> nodeDrops;

	public MessageDroppedReport1() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.nodeDrops = new HashMap<>();
		SimScenario.getInstance()
			.getHosts()
			.forEach((host) -> nodeDrops.put(host, 0));
	}

	@Override
	public void newMessage(Message m) {

	}

	@Override
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {

	}

	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		// Ignore reporting during warmup period
		if (isWarmupID(m.getId())) return;

		if (dropped) {
			this.nodeDrops.compute(
				where,
				(host, nrofDropped) -> (nrofDropped == null || nrofDropped == 0) ? 1 : nrofDropped + 1
			);
		}
	}

	@Override
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {

	}

	@Override
	public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {

	}

	@Override
	public void done() {
		StringBuilder sb = new StringBuilder();

		sb.append("message dropped per node: (ID, nrofDrops)\n");

		LinkedList<Map.Entry<DTNHost, Integer>> entriesLL = new LinkedList<>(nodeDrops.entrySet());
		var sortedEntriesLL = entriesLL.stream().sorted(Map.Entry.comparingByKey());
		sortedEntriesLL.forEach((entry) ->
			sb.append(String.format("%s; %d\n", entry.getKey().toString(), entry.getValue()))
		);

		write(sb.toString());
		super.done();
	}
}
