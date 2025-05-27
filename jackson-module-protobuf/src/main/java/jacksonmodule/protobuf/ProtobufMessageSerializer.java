package jacksonmodule.protobuf;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;

/**
 * Protobuf message serializer, use {@link JsonFormat#printer()} to serialize protobuf message.
 *
 * @param <T> protobuf message type
 */
final class ProtobufMessageSerializer<T extends MessageOrBuilder> extends JsonSerializer<T> {

    private final ProtobufModule.Options options;

    public ProtobufMessageSerializer(ProtobufModule.Options options) {
        this.options = options;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeRawValue(options.printer().print(value));
    }
}
