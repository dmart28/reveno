package org.reveno.atp.acceptance.tests.preallocated;

import org.junit.Test;
import org.reveno.atp.api.ChannelOptions;

public class PreallocatedSmallUnbufferedIOTest extends BasePreallocatedTest {

    @Test
    public void testProtostuff() throws Exception {
        testPreallocatedJournals(SMALL_FILE, ChannelOptions.UNBUFFERED_IO, SMALL_CHECKS);
    }

    @Test
    public void testJavaSerializer() throws Exception {
        testPreallocatedJournals(SMALL_FILE, ChannelOptions.UNBUFFERED_IO, r -> {
        }, true);
    }

}
