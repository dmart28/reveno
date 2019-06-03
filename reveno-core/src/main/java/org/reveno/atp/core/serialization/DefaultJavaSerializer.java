package org.reveno.atp.core.serialization;

import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.exceptions.BufferOutOfBoundsException;
import org.reveno.atp.api.exceptions.SerializerException;
import org.reveno.atp.api.exceptions.SerializerException.Action;
import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.TransactionCommitInfo.Builder;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.channel.RevenoBufferInputStream;
import org.reveno.atp.core.api.channel.RevenoBufferOutputStream;
import org.reveno.atp.core.api.serialization.RepositoryDataSerializer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;

import java.io.*;
import java.util.List;

public class DefaultJavaSerializer implements RepositoryDataSerializer,
        TransactionInfoSerializer {
    protected static final int DEFAULT_TYPE = 0x111;
    private ClassLoader classLoader;

    public DefaultJavaSerializer() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public DefaultJavaSerializer(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public int getSerializerType() {
        return DEFAULT_TYPE;
    }

    @Override
    public boolean isRegistered(Class<?> type) {
        return true;
    }

    @Override
    public void registerTransactionType(Class<?> txDataType) {
    }

    @Override
    public void serialize(TransactionCommitInfo info, Buffer buffer) {
        buffer.writeLong(info.transactionId());
        buffer.writeLong(info.time());

        try (RevenoBufferOutputStream ba = new RevenoBufferOutputStream(buffer);
             ObjectOutputStream os = new ObjectOutputStream(ba)) {
            os.writeObject(info.transactionCommits());
        } catch (IOException e) {
            throw new SerializerException(Action.SERIALIZATION, getClass(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public TransactionCommitInfo deserialize(Builder builder, Buffer buffer) {
        long transactionId = buffer.readLong();
        long time = buffer.readLong();
        if (transactionId == 0 && time == 0) {
            throw new BufferOutOfBoundsException();
        }

        try (RevenoBufferInputStream is = new RevenoBufferInputStream(buffer);
             ObjectInputStreamEx os = new ObjectInputStreamEx(is, classLoader)) {
            return builder.create().transactionId(transactionId).time(time).transactionCommits((List<Object>) os.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializerException(Action.DESERIALIZATION, getClass(), e);
        }
    }

    @Override
    public void serialize(RepositoryData repository, Buffer buffer) {
        try (RevenoBufferOutputStream ba = new RevenoBufferOutputStream(buffer);
             ObjectOutputStream os = new ObjectOutputStream(ba)) {
            os.writeObject(repository);
        } catch (IOException e) {
            throw new SerializerException(Action.SERIALIZATION, getClass(), e);
        }
    }

    @Override
    public RepositoryData deserialize(Buffer buffer) {
        try (RevenoBufferInputStream is = new RevenoBufferInputStream(buffer);
             ObjectInputStreamEx os = new ObjectInputStreamEx(is,
                     classLoader)) {
            return (RepositoryData) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializerException(Action.DESERIALIZATION, getClass(), e);
        }
    }

    @Override
    public void serializeCommands(List<Object> commands, Buffer buffer) {
        try (RevenoBufferOutputStream ba = new RevenoBufferOutputStream(buffer);
             ObjectOutputStream os = new ObjectOutputStream(ba)) {
            os.writeObject(commands);
        } catch (IOException e) {
            throw new SerializerException(Action.SERIALIZATION, getClass(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Object> deserializeCommands(Buffer buffer) {
        try (RevenoBufferInputStream is = new RevenoBufferInputStream(buffer);
             ObjectInputStreamEx os = new ObjectInputStreamEx(is, classLoader)) {
            return (List<Object>) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializerException(Action.DESERIALIZATION, getClass(), e);
        }
    }

    @Override
    public void serializeObject(Buffer buffer, Object tc) {
        try (ByteArrayOutputStream ba = new ByteArrayOutputStream();
             ObjectOutputStream os = new ObjectOutputStream(ba)) {
            os.writeObject(tc);
            buffer.writeInt(ba.size());
            buffer.writeBytes(ba.toByteArray());
        } catch (IOException e) {
            throw new SerializerException(Action.SERIALIZATION, getClass(), e);
        }
    }

    @Override
    public Object deserializeObject(Buffer buffer) {
        try (ByteArrayInputStream is = new ByteArrayInputStream(
                buffer.readBytes(buffer.readInt()));
             ObjectInputStreamEx os = new ObjectInputStreamEx(is,
                     classLoader)) {
            return os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializerException(Action.DESERIALIZATION, getClass(), e);
        }
    }

    /*
     * We need OSGI compliance here
     */
    public static class ObjectInputStreamEx extends ObjectInputStream {
        private ClassLoader classLoader;

        public ObjectInputStreamEx(InputStream input, ClassLoader classLoader)
                throws IOException {
            super(input);
            this.classLoader = classLoader;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc)
                throws IOException, ClassNotFoundException {
            try {
                return classLoader.loadClass(desc.getName());
            } catch (Throwable t) {
                return super.resolveClass(desc);
            }
        }
    }

}
