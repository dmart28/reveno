package org.reveno.atp.api.transaction;

public enum TransactionStage {
	REPLICATION((byte) 1), TRANSACTION((byte) 2), JOURNALING((byte) 3);

	private byte type;

	TransactionStage(byte type) {
		this.type = type;
	}

	public byte getType() {
		return type;
	}
}
