package org.reveno.atp.core.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.List;

import org.reveno.atp.api.domain.RepositoryData;
import org.reveno.atp.api.exceptions.SerializerException;
import org.reveno.atp.api.exceptions.SerializerException.Action;
import org.reveno.atp.core.api.TransactionCommitInfo;
import org.reveno.atp.core.api.TransactionCommitInfo.Builder;
import org.reveno.atp.core.api.channel.Buffer;
import org.reveno.atp.core.api.serialization.RepositoryDataSerializer;
import org.reveno.atp.core.api.serialization.TransactionInfoSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultJavaSerializer implements RepositoryDataSerializer,
		TransactionInfoSerializer {

	@Override
	public int getSerializerType() {
		return DEFAULT_TYPE;
	}

	@Override
	public void registerTransactionType(Class<?> txDataType) {
	}

	@Override
	public void serialize(TransactionCommitInfo info, Buffer buffer) {
		buffer.writeLong(info.getTransactionId());
		buffer.writeLong(info.getTime());
		buffer.writeInt(info.getVersion());

		try (ByteArrayOutputStream ba = new ByteArrayOutputStream(); // TODO global
				ObjectOutputStream os = new ObjectOutputStream(ba)) {
			os.writeObject(info.getTransactionCommits());
			buffer.writeInt(ba.size());
			buffer.writeBytes(ba.toByteArray());
		} catch (IOException e) {
			log.error("", e);
			throw new SerializerException(Action.SERIALIZATION, getClass(), e);
		}
	}

	@Override
	public TransactionCommitInfo deserialize(Builder builder, Buffer buffer) {
		long transactionId = buffer.readLong();
		long time = buffer.readLong();
		int version = buffer.readInt();

		try (ByteArrayInputStream is = new ByteArrayInputStream(
				buffer.readBytes(buffer.readInt()));
				ObjectInputStreamEx os = new ObjectInputStreamEx(is,
						classLoader)) {
			return builder.create(transactionId, version, time,
					(Object[]) os.readObject());
		} catch (IOException | ClassNotFoundException e) {
			log.error("", e);
			throw new SerializerException(Action.DESERIALIZATION, getClass(), e);
		}
	}

	@Override
	public void serialize(RepositoryData repository, Buffer buffer) {
		try (ByteArrayOutputStream ba = new ByteArrayOutputStream(); // TODO global
				ObjectOutputStream os = new ObjectOutputStream(ba)) {
			os.writeObject(repository);
			buffer.writeInt(ba.size());
			buffer.writeBytes(ba.toByteArray());
		} catch (IOException e) {
			log.error("", e);
			throw new SerializerException(Action.SERIALIZATION, getClass(), e);
		}
	}

	@Override
	public RepositoryData deserialize(Buffer buffer) {
		try (ByteArrayInputStream is = new ByteArrayInputStream(
				buffer.readBytes(buffer.readInt()));
				ObjectInputStreamEx os = new ObjectInputStreamEx(is,
						classLoader)) {
			return (RepositoryData) os.readObject();
		} catch (IOException | ClassNotFoundException e) {
			log.error("", e);
			throw new SerializerException(Action.DESERIALIZATION, getClass(), e);
		}
	}

	@Override
	public void serializeCommands(List<Object> commands, Buffer buffer) {
		try (ByteArrayOutputStream ba = new ByteArrayOutputStream(); 
				ObjectOutputStream os = new ObjectOutputStream(ba)) {
			os.writeObject(commands);
			buffer.writeInt(ba.size());
			buffer.writeBytes(ba.toByteArray());
		} catch (IOException e) {
			log.error("", e);
			throw new SerializerException(Action.SERIALIZATION, getClass(), e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Object> deserializeCommands(Buffer buffer) {
		try (ByteArrayInputStream is = new ByteArrayInputStream(
				buffer.readBytes(buffer.readInt()));
				ObjectInputStreamEx os = new ObjectInputStreamEx(is,
						classLoader)) {
			return (List<Object>) os.readObject();
		} catch (IOException | ClassNotFoundException e) {
			log.error("", e);
			throw new SerializerException(Action.DESERIALIZATION, getClass(), e);
		}
	}

	public DefaultJavaSerializer() {
		this(Thread.currentThread().getContextClassLoader());
	}

	public DefaultJavaSerializer(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	private ClassLoader classLoader;
	private static final Logger log = LoggerFactory
			.getLogger(DefaultJavaSerializer.class);
	protected static final int DEFAULT_TYPE = 0x111;

	/*
	 * We need OSGi compliancy here
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
