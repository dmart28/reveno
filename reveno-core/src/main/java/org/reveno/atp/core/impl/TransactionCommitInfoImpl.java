package org.reveno.atp.core.impl;

import org.reveno.atp.core.api.TransactionCommitInfo;

import java.util.List;

public class TransactionCommitInfoImpl implements TransactionCommitInfo {
    private long transactionId;
    private int version;
    private long time;
    private long flag;
    private long tag;
    private List<Object> transactionCommits;

    public long transactionId() {
        return transactionId;
    }

    public TransactionCommitInfo transactionId(final long transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public int version() {
        return version;
    }

    public TransactionCommitInfo version(final int version) {
        this.version = version;
        return this;
    }

    public long time() {
        return time;
    }

    public TransactionCommitInfo time(final long time) {
        this.time = time;
        return this;
    }

    public long flag() {
        return flag;
    }

    public TransactionCommitInfo flag(long flag) {
        this.flag = flag;
        return this;
    }

    public long tag() {
        return tag;
    }

    public TransactionCommitInfo tag(long tag) {
        this.tag = tag;
        return this;
    }

    public List<Object> transactionCommits() {
        return transactionCommits;
    }

    public TransactionCommitInfo transactionCommits(List<Object> transactionCommits) {
        this.transactionCommits = transactionCommits;
        return this;
    }


    public static class PojoBuilder implements TransactionCommitInfo.Builder {
        @Override
        public TransactionCommitInfo create() {
            return new TransactionCommitInfoImpl();
        }
    }

}
