package org.reveno.atp.api.exceptions;

public class SerializerException extends RuntimeException {
    private static final long serialVersionUID = 493243568356327661L;

    public SerializerException(Action action, Class<?> serializer, Throwable e) {
        super(String.format("Can't perform %s [ser=%s]", action, serializer.getSimpleName()), e);
    }

    public enum Action {
        SERIALIZATION, DESERIALIZATION
    }

}
