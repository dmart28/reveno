package org.reveno.atp.core.impl;

import org.reveno.atp.core.api.EventsCommitInfo;

public class EventsCommitInfoImpl implements EventsCommitInfo {
	private long transactionId;
	private long time;
	private long flag;

	public long transactionId() {
		return transactionId;
	}

	public void setTransactionId(final long transactionId) {
		this.transactionId = transactionId;
	}

	public long time() {
		return time;
	}

	public void setTime(final long time) {
		this.time = time;
	}

	public long flag() {
		return flag;
	}

	public void setFlag(final long flag) {
		this.flag = flag;
	}


	public static class PojoBuilder implements EventsCommitInfo.Builder {
		@Override
		public EventsCommitInfo create(long txId, long time, long flag) {
			EventsCommitInfoImpl impl = new EventsCommitInfoImpl();
			impl.setTransactionId(txId);
			impl.setTime(time);
			impl.setFlag(flag);
			return impl;
		}
	}
	
}
