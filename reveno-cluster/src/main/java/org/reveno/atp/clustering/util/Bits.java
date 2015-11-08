package org.reveno.atp.clustering.util;

import java.util.zip.CRC32;

public abstract class Bits {

    public static short readShort(byte b1, byte b2) {
        if ((b1 & 0xff | b2 & 0xff) < 0)
            throw new RuntimeException();
        return (short)((((short)b1 & 0xff) << 8) | (((short)b2 & 0xff) << 0));
    }

    public static int readInt(byte b1, byte b2, byte b3, byte b4) {
        return ((((int)b1 & 0xff) << 24) | (((int)b2 & 0xff) << 16) |
                (((int)b3 & 0xff) << 8) | (((int)b4 & 0xff) << 0));
    }

    public static long readLong(byte b1, byte b2, byte b3, byte b4,
                               byte b5, byte b6, byte b7, byte b8) {
        return ((((long) b1) << 56) | (((long) b2 & 0xff) << 48) | (((long) b3 & 0xff) << 40)
                | (((long) b4 & 0xff) << 32) | (((long) b5 & 0xff) << 24) | (((long) b6 & 0xff) << 16)
                | (((long) b7 & 0xff) << 8) | (((long) b8 & 0xff)));
    }

    public static byte[] intToBytes(int l) {
        byte[] result = new byte[4];
        for (int i = 3; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 4;
        }
        return result;
    }

    public static long crc32(byte[] value) {
        CRC32 crc = new CRC32();
        crc.update(value);
        return crc.getValue();
    }

}
