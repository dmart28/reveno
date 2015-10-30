package org.reveno.atp.clustering.core.fastcast;

import org.nustaq.fastcast.api.FastCast;
import org.reveno.atp.clustering.api.ClusterBuffer;
import org.reveno.atp.clustering.util.ResourceLoader;
import org.reveno.atp.clustering.util.Utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Properties;
import java.util.function.Function;

/**
 * fast-cast {@link ClusterBuffer} implementation, which is based on reliable
 * message multicasting. Still, it is impossible to build {@link org.reveno.atp.clustering.api.Cluster}
 * on top of fast-cast, so {@link org.reveno.atp.clustering.core.jgroups.JGroupsCluster} or another
 * implementation must be used.
 */
public class FastCastBuffer implements ClusterBuffer {

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void messageNotifier(Function<ClusterBuffer, Boolean> listener) {

    }

    @Override
    public void lockIncoming() {

    }

    @Override
    public void unlockIncoming() {

    }

    @Override
    public void erase() {

    }

    @Override
    public void prepare() {

    }

    @Override
    public boolean replicate() {
        return false;
    }

    @Override
    public int readerPosition() {
        return 0;
    }

    @Override
    public int writerPosition() {
        return 0;
    }

    @Override
    public int limit() {
        return 0;
    }

    @Override
    public long capacity() {
        return 0;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public int remaining() {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public void release() {

    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void setReaderPosition(int position) {

    }

    @Override
    public void setWriterPosition(int position) {

    }

    @Override
    public void writeByte(byte b) {

    }

    @Override
    public void writeBytes(byte[] bytes) {

    }

    @Override
    public void writeBytes(byte[] buffer, int offset, int count) {

    }

    @Override
    public void writeLong(long value) {

    }

    @Override
    public void writeInt(int value) {

    }

    @Override
    public void writeShort(short s) {

    }

    @Override
    public void writeFromBuffer(ByteBuffer buffer) {

    }

    @Override
    public ByteBuffer writeToBuffer() {
        return null;
    }

    @Override
    public byte readByte() {
        return 0;
    }

    @Override
    public byte[] readBytes(int length) {
        return new byte[0];
    }

    @Override
    public void readBytes(byte[] data, int offset, int length) {

    }

    @Override
    public long readLong() {
        return 0;
    }

    @Override
    public int readInt() {
        return 0;
    }

    @Override
    public short readShort() {
        return 0;
    }

    @Override
    public void markReader() {

    }

    @Override
    public void markWriter() {

    }

    @Override
    public void resetReader() {

    }

    @Override
    public void resetWriter() {

    }

    @Override
    public void limitNext(int count) {

    }

    @Override
    public void resetNextLimit() {

    }

    @Override
    public void markSize() {

    }

    @Override
    public int sizeMarkPosition() {
        return 0;
    }

    @Override
    public void writeSize() {

    }

    public FastCastBuffer(FastCastConfiguration config) throws Exception {
        fastCast = FastCast.getFastCast();
        fastCast.setNodeId(config.nodeId());
        if (config.configFile().isPresent()) {
            fastCast.loadConfig(config.configFile().get().getAbsolutePath());
        } else {
            Properties props = new Properties();
            if (!Utils.isNullOrEmpty(config.mcastHost())) {
                props.put("fastcast.mcast.addr", config.mcastHost());
            }
            if (config.mcastPort() != 0) {
                props.put("fastcast.mcast.port", config.mcastPort());
            }
            if (!Utils.isNullOrEmpty(config.networkInterface())) {
                props.put("fastcast.interface", config.networkInterface());
            }
            String configStr = ResourceLoader.loadResource(
                    getClass().getClassLoader().getResourceAsStream("fastcast_default.kson"), props);
            File configFile = Files.createTempFile("cfg", ".kson").toFile();
            Utils.write(configStr, configFile);
            fastCast.loadConfig(configFile.getAbsolutePath());
            configFile.delete();
        }

    }

    protected FastCast fastCast;
}
