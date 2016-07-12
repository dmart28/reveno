package org.reveno.atp.acceptance.tests.preallocated;

import org.junit.Test;
import org.reveno.atp.api.ChannelOptions;

public class PreallocatedSmallBufferingVMTest extends BasePreallocatedTest {

    @Test
    public void testProtostuff() throws Exception {
        testPreallocatedJournals(SMALL_FILE, ChannelOptions.BUFFERING_VM, SMALL_CHECKS);
    }

    @Test
    public void testJavaSerializer() throws Exception {
        testPreallocatedJournals(SMALL_FILE, ChannelOptions.BUFFERING_VM, r -> {}, true);
    }

}
