package jacksonmodule.protobuf.v3;

import com.google.protobuf.ProtocolMessageEnum;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

final class ProtobufEnumSerializer<T extends Enum<T> & ProtocolMessageEnum> extends ValueSerializer<T> {

    private final ProtobufModule.Options options;

    public ProtobufEnumSerializer(ProtobufModule.Options options) {
        this.options = options;
    }

    @Override
    public void serialize(T value, tools.jackson.core.JsonGenerator gen, SerializationContext ctxt)
            throws JacksonException {
        if (options.serializeEnumAsInt()) {
            gen.writeNumber(value.getNumber());
        } else {
            gen.writeString(value.name());
        }
    }
}
