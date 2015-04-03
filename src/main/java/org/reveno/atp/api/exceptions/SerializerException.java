package org.reveno.atp.api.exceptions;

public class SerializerException extends RuntimeException {

	public SerializerException(Action action, Class<?> serializer, Throwable e) {
		super(String.format("Can't perform %s [ser=%s]", action, serializer.getSimpleName()), e);
	}
	
	private static final long serialVersionUID = 493243568356327661L;

	
	public static enum Action {
		SERIALIZATION, DESERIALIZATION
	}
	
}
