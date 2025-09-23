package jacksonmodule.protobuf.nativejackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.protobuf.ProtocolMessageEnum;
import java.io.IOException;

/**
 * Native Jackson serializer for protobuf enums that doesn't depend on JsonFormat.
 */
@SuppressWarnings("rawtypes")
final class NativeProtobufEnumSerializer extends JsonSerializer<ProtocolMessageEnum> {

    private final NativeJacksonProtobufModule.Options options;

    public NativeProtobufEnumSerializer(NativeJacksonProtobufModule.Options options) {
        this.options = options;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(ProtocolMessageEnum value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (options.serializeEnumAsInt()) {
            // Handle UNRECOGNIZED enum values which don't have a valid number
            String enumName = ((Enum<?>) value).name();
            if ("UNRECOGNIZED".equals(enumName)) {
                // For UNRECOGNIZED enums, we can't get the number, so serialize as -1 or throw an exception
                throw new IllegalArgumentException("Cannot serialize UNRECOGNIZED enum value as integer");
            }
            gen.writeNumber(value.getNumber());
        } else {
            gen.writeString(((Enum<?>) value).name());
        }
    }
}
