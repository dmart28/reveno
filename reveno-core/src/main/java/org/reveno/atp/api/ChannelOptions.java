package org.reveno.atp.api;

public enum ChannelOptions {
    /**
     * The fastest option for {@link org.reveno.atp.core.api.channel.Channel}
     * operations, but at the same time the least fault tolerant. Means some
     * direct buffer will be created for the write operations, with eventual
     * batched flushes on disk.
     * <p>
     * In case of VM crash, significant amount of transactions might be lost.
     */
    BUFFERING_VM,
    /**
     * Compromise between fault tolerance and write speed. This option
     * relies on internal OS page caching. All written data from complete transactions
     * will survive VM crash, but still some data might be lost in case of OS system crash.
     */
    BUFFERING_OS,
    /**
     * Compromise between fault tolerance and write speed. This option
     * relies on mmap ability of OS. All written data from complete transactions
     * will survive VM crash, but still some data might be lost in case of OS system crash.
     */
    BUFFERING_MMAP_OS,
    /**
     * The slowest option, where each transaction data is written directly to disk synchronously.
     * Guarantees fault tolerant in case either VM or OS crash.
     */
    UNBUFFERED_IO
}
