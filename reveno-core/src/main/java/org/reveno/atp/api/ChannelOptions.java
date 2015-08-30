/**
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.api;

public enum ChannelOptions {
    /**
     * The fastest option for {@link org.reveno.atp.core.api.channel.Channel}
     * operations, but at the same time the least fault tolerant. Means some
     * direct buffer will be created for the write operations, with eventual
     * batched flushes on disk.
     *
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
