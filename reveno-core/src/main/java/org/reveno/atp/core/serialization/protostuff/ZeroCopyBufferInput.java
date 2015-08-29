package org.reveno.atp.core.serialization.protostuff;

import io.protostuff.*;
import org.reveno.atp.core.api.channel.Buffer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Some customized implementation of ZeroCopyInput, most
 * parts of it is kindly taken from Protostuff source code.
 */
public final class ZeroCopyBufferInput implements Input {
    private final Buffer buffer;
    private int lastTag = 0;
    private int packedLimit = 0;
    public final boolean decodeNestedMessageAsGroup;

    public ZeroCopyBufferInput(Buffer buffer, boolean protostuffMessage) {
        this.buffer = buffer;
        this.decodeNestedMessageAsGroup = protostuffMessage;
    }

    public int currentOffset() {
        return this.buffer.readerPosition();
    }

    public boolean isCurrentFieldPacked() {
        return this.packedLimit != 0 && this.packedLimit != this.buffer.readerPosition();
    }

    public int getLastTag() {
        return this.lastTag;
    }

    public int readTag() throws IOException {
        if (!(this.buffer.remaining() > 0)) {
            this.lastTag = 0;
            return 0;
        } else {
            int tag = this.readRawVarint32();
            if (tag >>> 3 == 0) {
                throw new ProtobufException("Protocol message contained an invalid tag (zero).");
            } else {
                this.lastTag = tag;
                return tag;
            }
        }
    }

    public void checkLastTagWas(int value) throws ProtobufException {
        if (this.lastTag != value) {
            throw new ProtobufException("Protocol message end-group tag did not match expected tag.");
        }
    }

    public boolean skipField(int tag) throws IOException {
        switch (WireFormat.getTagWireType(tag)) {
            case 0:
                this.readInt32();
                return true;
            case 1:
                this.readRawLittleEndian64();
                return true;
            case 2:
                int size = this.readRawVarint32();
                if (size < 0) {
                    throw new ProtobufException("CodedInput encountered an embedded string or message which claimed to have negative size.");
                }

                this.buffer.setReaderPosition(this.buffer.readerPosition() + size);
                return true;
            case 3:
                this.skipMessage();
                this.checkLastTagWas(WireFormat.makeTag(WireFormat.getTagFieldNumber(tag), 4));
                return true;
            case 4:
                return false;
            case 5:
                this.readRawLittleEndian32();
                return true;
            default:
                throw new ProtobufException("Protocol message tag had invalid wire type.");
        }
    }

    public void skipMessage() throws IOException {
        int tag;
        do {
            tag = this.readTag();
        } while (tag != 0 && this.skipField(tag));

    }

    public <T> void handleUnknownField(int fieldNumber, Schema<T> schema) throws IOException {
        this.skipField(this.lastTag);
    }

    public <T> int readFieldNumber(Schema<T> schema) throws IOException {
        if (!(this.buffer.remaining() > 0)) {
            this.lastTag = 0;
            return 0;
        } else if (this.isCurrentFieldPacked()) {
            if (this.packedLimit < this.buffer.readerPosition()) {
                throw new ProtobufException("CodedInput encountered an embedded string or bytes that misreported its size.");
            } else {
                return this.lastTag >>> 3;
            }
        } else {
            this.packedLimit = 0;
            int tag = this.readRawVarint32();
            int fieldNumber = tag >>> 3;
            if (fieldNumber == 0) {
                if (this.decodeNestedMessageAsGroup && 7 == (tag & 7)) {
                    this.lastTag = 0;
                    return 0;
                } else {
                    throw new ProtobufException("Protocol message contained an invalid tag (zero).");
                }
            } else if (this.decodeNestedMessageAsGroup && 4 == (tag & 7)) {
                this.lastTag = 0;
                return 0;
            } else {
                this.lastTag = tag;
                return fieldNumber;
            }
        }
    }

    private void checkIfPackedField() throws IOException {
        if (this.packedLimit == 0 && WireFormat.getTagWireType(this.lastTag) == 2) {
            int length = this.readRawVarint32();
            if (length < 0) {
                throw new ProtobufException("CodedInput encountered an embedded string or message which claimed to have negative size.");
            }

            if (this.buffer.readerPosition() + length > this.buffer.limit()) {
                throw new ProtobufException("CodedInput encountered an embedded string or bytes that misreported its size.");
            }

            this.packedLimit = (int)this.buffer.readerPosition() + length;
        }

    }

    public double readDouble() throws IOException {
        this.checkIfPackedField();
        return Double.longBitsToDouble(this.readRawLittleEndian64());
    }

    public float readFloat() throws IOException {
        this.checkIfPackedField();
        return Float.intBitsToFloat(this.readRawLittleEndian32());
    }

    public long readUInt64() throws IOException {
        this.checkIfPackedField();
        return this.readRawVarint64();
    }

    public long readInt64() throws IOException {
        this.checkIfPackedField();
        return this.readRawVarint64();
    }

    public int readInt32() throws IOException {
        this.checkIfPackedField();
        return this.readRawVarint32();
    }

    public long readFixed64() throws IOException {
        this.checkIfPackedField();
        return this.readRawLittleEndian64();
    }

    public int readFixed32() throws IOException {
        this.checkIfPackedField();
        return this.readRawLittleEndian32();
    }

    public boolean readBool() throws IOException {
        this.checkIfPackedField();
        return this.buffer.readByte() != 0;
    }

    public int readUInt32() throws IOException {
        this.checkIfPackedField();
        return this.readRawVarint32();
    }

    public int readEnum() throws IOException {
        this.checkIfPackedField();
        return this.readRawVarint32();
    }

    public int readSFixed32() throws IOException {
        this.checkIfPackedField();
        return this.readRawLittleEndian32();
    }

    public long readSFixed64() throws IOException {
        this.checkIfPackedField();
        return this.readRawLittleEndian64();
    }

    public int readSInt32() throws IOException {
        this.checkIfPackedField();
        int n = this.readRawVarint32();
        return n >>> 1 ^ -(n & 1);
    }

    public long readSInt64() throws IOException {
        this.checkIfPackedField();
        long n = this.readRawVarint64();
        return n >>> 1 ^ -(n & 1L);
    }

    public String readString() throws IOException {
        int length = this.readRawVarint32();
        if (length < 0) {
            throw new ProtobufException("CodedInput encountered an embedded string or message which claimed to have negative size.");
        } else {
            byte[] tmp = new byte[length];
            this.buffer.readBytes(tmp, 0, tmp.length);
            return StringSerializer.STRING.deser(tmp);
        }
    }

    public ByteString readBytes() throws IOException {
        return ByteString.copyFrom(this.readByteArray());
    }

    public byte[] readByteArray() throws IOException {
        int length = this.readRawVarint32();
        if (length < 0) {
            throw new ProtobufException("CodedInput encountered an embedded string or message which claimed to have negative size.");
        } else {
            byte[] copy = new byte[length];
            this.buffer.readBytes(copy, 0, copy.length);
            return copy;
        }
    }

    public <T> T mergeObject(T value, Schema<T> schema) throws IOException {
        if (this.decodeNestedMessageAsGroup) {
            return this.mergeObjectEncodedAsGroup(value, schema);
        } else {
            int length = this.readRawVarint32();
            if (length < 0) {
                throw new ProtobufException("CodedInput encountered an embedded string or message which claimed to have negative size.");
            } else {

                ByteBuffer dup = this.buffer.writeToBuffer();
                dup.limit(length);
                if (value == null) {
                    value = schema.newMessage();
                }

                ByteBufferInput nestedInput = new ByteBufferInput(dup, this.decodeNestedMessageAsGroup);
                schema.mergeFrom(nestedInput, value);
                if (!schema.isInitialized(value)) {
                    throw new UninitializedMessageException(value, schema);
                } else {
                    nestedInput.checkLastTagWas(0);
                    this.buffer.setReaderPosition(this.buffer.readerPosition() + length);
                    return value;
                }
            }
        }
    }

    private <T> T mergeObjectEncodedAsGroup(T value, Schema<T> schema) throws IOException {
        if (value == null) {
            value = schema.newMessage();
        }

        schema.mergeFrom(this, value);
        if (!schema.isInitialized(value)) {
            throw new UninitializedMessageException(value, schema);
        } else {
            this.checkLastTagWas(0);
            return value;
        }
    }

    public int readRawVarint32() throws IOException {
        byte tmp = this.buffer.readByte();
        if (tmp >= 0) {
            return tmp;
        } else {
            int result = tmp & 127;
            if ((tmp = this.buffer.readByte()) >= 0) {
                result |= tmp << 7;
            } else {
                result |= (tmp & 127) << 7;
                if ((tmp = this.buffer.readByte()) >= 0) {
                    result |= tmp << 14;
                } else {
                    result |= (tmp & 127) << 14;
                    if ((tmp = this.buffer.readByte()) >= 0) {
                        result |= tmp << 21;
                    } else {
                        result |= (tmp & 127) << 21;
                        result |= (tmp = this.buffer.readByte()) << 28;
                        if (tmp < 0) {
                            for (int i = 0; i < 5; ++i) {
                                if (this.buffer.readByte() >= 0) {
                                    return result;
                                }
                            }

                            throw new ProtobufException("CodedInput encountered a malformed varint.");
                        }
                    }
                }
            }

            return result;
        }
    }

    public long readRawVarint64() throws IOException {
        int shift = 0;

        for (long result = 0L; shift < 64; shift += 7) {
            byte b = this.buffer.readByte();
            result |= (long) (b & 127) << shift;
            if ((b & 128) == 0) {
                return result;
            }
        }

        throw new ProtobufException("CodedInput encountered a malformed varint.");
    }

    public int readRawLittleEndian32() throws IOException {
        byte[] bs = new byte[4];
        this.buffer.readBytes(bs, 0, 4);
        return bs[0] & 255 | (bs[1] & 255) << 8 | (bs[2] & 255) << 16 | (bs[3] & 255) << 24;
    }

    public long readRawLittleEndian64() throws IOException {
        byte[] bs = new byte[8];
        this.buffer.readBytes(bs, 0, 8);
        return (long) bs[0] & 255L | ((long) bs[1] & 255L) << 8 | ((long) bs[2] & 255L) << 16 | ((long) bs[3] & 255L) << 24 | ((long) bs[4] & 255L) << 32 | ((long) bs[5] & 255L) << 40 | ((long) bs[6] & 255L) << 48 | ((long) bs[7] & 255L) << 56;
    }

    public void transferByteRangeTo(Output output, boolean utf8String, int fieldNumber, boolean repeated) throws IOException {
        int length = this.readRawVarint32();
        if (length < 0) {
            throw new ProtobufException("CodedInput encountered an embedded string or message which claimed to have negative size.");
        } else {
            if (this.buffer.remaining() < length) {
                throw new ProtobufException("CodedInput encountered an embedded string or bytes that misreported its size.");
            }

            ByteBuffer dup1 = this.buffer.writeToBuffer();
            dup1.limit(length);
            output.writeBytes(fieldNumber, dup1, repeated);
            this.buffer.setReaderPosition(this.buffer.readerPosition() + length);
        }
    }

    public ByteBuffer readByteBuffer() throws IOException {
        return ByteBuffer.wrap(this.readByteArray());
    }

}
