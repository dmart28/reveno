package org.reveno.atp.core.api;

import java.util.List;

public interface TransactionCommitInfo {

    long transactionId();

    long time();

    long flag();

    long tag();

    List<Object> transactionCommits();

    TransactionCommitInfo transactionId(long transactionId);

    TransactionCommitInfo time(long time);

    TransactionCommitInfo flag(long flag);

    TransactionCommitInfo tag(long tag);

    TransactionCommitInfo transactionCommits(List<Object> commands);


    interface Builder {
        TransactionCommitInfo create();
    }

}
