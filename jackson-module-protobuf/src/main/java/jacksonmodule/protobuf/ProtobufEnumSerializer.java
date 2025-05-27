package jacksonmodule.protobuf;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.protobuf.ProtocolMessageEnum;
import java.io.IOException;

final class ProtobufEnumSerializer<T extends Enum<T> & ProtocolMessageEnum> extends JsonSerializer<T> {

    private final ProtobufModule.Options options;

    public ProtobufEnumSerializer(ProtobufModule.Options options) {
        this.options = options;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (options.serializeEnumAsInt()) {
            gen.writeNumber(value.getNumber());
        } else {
            gen.writeString(value.name());
        }
    }
}
