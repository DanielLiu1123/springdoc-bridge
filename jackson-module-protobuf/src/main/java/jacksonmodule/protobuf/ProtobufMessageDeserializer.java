package jacksonmodule.protobuf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
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

    private final Class<T> clazz;
    private final ProtobufModule.Options options;

    public ProtobufMessageDeserializer(Class<T> clazz, ProtobufModule.Options options) {
        this.clazz = clazz;
        this.options = options;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        var treeNode = p.readValueAsTree();

        String json = treeNode.toString();

        var newBuilderMethod = findMethod(clazz, "newBuilder");
        if (newBuilderMethod == null) {
            throw new IllegalStateException("No newBuilder method found for class " + clazz);
        }

        try {
            var builder = (Message.Builder) newBuilderMethod.invoke(null);

            options.parser().merge(json, builder);

            @SuppressWarnings("unchecked")
            T result = (T) builder.build();

            return result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to deserialize protobuf message", e);
        }
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
