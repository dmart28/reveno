package org.reveno.atp.commons;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import org.reveno.atp.core.api.channel.Buffer;

/**
 * Makes it possible to have byte array as a key in Map.
 * More importantly, it doesn't require costly things like
 * creating wrappers for byte[] and making unnecessary garbage
 * and computations. It supports completely Zero-Copy get operation.
 *
 * @param <T>
 */
public class ByteArrayObjectMap<T> {
    private Node root = new Node();

    public void put(byte[] key, T value) {
        Node n = root;
        for (int i = 0; i < key.length; i++) {
            Node nn = n.map.get(key[i]);
            if (nn == null) {
                nn = new Node();
                n.map.put(key[i], nn);
            }
            n = nn;
            if (i == key.length - 1) {
                n.value = value;
            }
        }
    }

    public boolean containsKey(byte[] key) {
        return get(key) != null;
    }

    public T get(byte[] key) {
        Node n = root;
        for (int i = 0; i < key.length; i++) {
            if ((n = n.map.get(key[i])) == null) {
                return null;
            }
        }
        return n.value;
    }

    public T get(Buffer buffer, int count) {
        Node n = root;
        for (int i = 0; i < count; i++) {
            if ((n = n.map.get(buffer.readByte())) == null) {
                return null;
            }
        }
        return n.value;
    }

    protected class Node {
        public Byte2ObjectOpenHashMap<Node> map = new Byte2ObjectOpenHashMap<>(256);
        public T value;
    }

}
