package jacksonmodule.protobuf.v3;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Protobuf message serializer, use {@link JsonFormat#printer()} to serialize protobuf message.
 *
 * @param <T> protobuf message type
 */
final class ProtobufMessageSerializer<T extends MessageOrBuilder> extends ValueSerializer<T> {

    private final ProtobufModule.Options options;

    public ProtobufMessageSerializer(ProtobufModule.Options options) {
        this.options = options;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        try {
            gen.writeRawValue(options.printer().print(value));
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Failed to serialize protobuf message", e);
        }
    }
}
