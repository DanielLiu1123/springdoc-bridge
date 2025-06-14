package jacksonmodule.protobuf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.NullValue;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Protobuf message deserializer, use {@link JsonFormat#parser()} to deserialize protobuf message.
 *
 * @param <T> protobuf message type
 */
final class ProtobufMessageDeserializer<T extends MessageOrBuilder> extends JsonDeserializer<T> {

    private final ProtobufModule.Options options;
    private final Message defaultInstance;

    public ProtobufMessageDeserializer(Class<T> clazz, ProtobufModule.Options options) {
        this.options = options;
        this.defaultInstance = getDefaultInstance(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        var treeNode = p.readValueAsTree();

        String json = treeNode.toString();

        var builder = defaultInstance.toBuilder();

        options.parser().merge(json, builder);

        return (T) builder.build();
    }

    @Override
    public T getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        var clazz = defaultInstance.getClass();
        if (Value.class.isAssignableFrom(clazz)) {
            @SuppressWarnings("unchecked")
            var nullValue =
                    (T) Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
            return nullValue;
        }
        return super.getNullValue(ctxt);
    }

    /**
     * Gets the default instance of a protobuf message class using reflection.
     * This method caches the default instance to avoid repeated reflection calls during deserialization.
     *
     * @param clazz the protobuf message class
     * @return the default instance of the message
     * @throws IllegalArgumentException if the class doesn't have a getDefaultInstance method or if invocation fails
     */
    private static Message getDefaultInstance(Class<?> clazz) {
        try {
            Method getDefaultInstanceMethod = clazz.getMethod("getDefaultInstance");
            Object result = getDefaultInstanceMethod.invoke(null);
            if (result instanceof Message message) {
                return message;
            } else {
                throw new IllegalArgumentException(
                        "getDefaultInstance() did not return a Message instance for class " + clazz);
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No getDefaultInstance method found in class " + clazz, e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to invoke getDefaultInstance method for class " + clazz, e);
        }
    }
}
