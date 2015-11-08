package org.reveno.atp.clustering.core.fastcast;

import org.nustaq.fastcast.api.FastCast;
import org.reveno.atp.utils.Exceptions;

import java.lang.reflect.Field;

public class FastCastEx extends FastCast {

    @Override
    /**
     * Have to do since FastCast autogenerating part of ID in
     * runtime, which is not good for our case.
     *
     * NOTICE!!! Must update to never versions of FC VERY CAREFULLY!
     */
    public void setNodeId(String nodeName) {
        try {
            Field nodeIdField = FastCast.class.getDeclaredField("nodeId");
            nodeIdField.setAccessible(true);

            nodeIdField.set(this, nodeName);
        } catch (Throwable t) {
            throw Exceptions.runtime(t);
        }
    }
}
