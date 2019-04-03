package org.reveno.atp.acceptance.tests.preallocated;

import org.junit.Test;
import org.reveno.atp.api.ChannelOptions;

public class PreallocatedSmallBufferingMmapTest extends BasePreallocatedTest {

    @Test
    public void testProtostuff() throws Exception {
        testPreallocatedJournals(SMALL_FILE, ChannelOptions.BUFFERING_MMAP_OS, SMALL_CHECKS);
    }

    @Test
    public void testJavaSerializer() throws Exception {
        testPreallocatedJournals(SMALL_FILE, ChannelOptions.BUFFERING_MMAP_OS, r -> {
        }, true);
    }

}
