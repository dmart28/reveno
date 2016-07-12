package org.reveno.atp.acceptance.tests.preallocated;

import org.junit.Test;
import org.reveno.atp.api.ChannelOptions;

public class PreallocatedUnbufferedIOTest extends BasePreallocatedTest {

    @Test
    public void testProtostuff() throws Exception {
        testPreallocatedJournals(LARGE_FILE, ChannelOptions.UNBUFFERED_IO, LARGE_CHECKS);
    }

    @Test
    public void testJavaSerializer() throws Exception {
        testPreallocatedJournals(LARGE_FILE, ChannelOptions.UNBUFFERED_IO, r -> {}, true);
    }

}
