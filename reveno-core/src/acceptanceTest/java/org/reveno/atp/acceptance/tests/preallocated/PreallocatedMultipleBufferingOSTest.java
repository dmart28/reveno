package org.reveno.atp.acceptance.tests.preallocated;

import org.junit.Test;
import org.reveno.atp.api.ChannelOptions;

public class PreallocatedMultipleBufferingOSTest extends BasePreallocatedTest {

    @Test
    public void test() throws Exception {
        testPreallocatedJournals(EXTRA_SMALL_FILE, ChannelOptions.BUFFERING_OS, r -> {});
    }

}
