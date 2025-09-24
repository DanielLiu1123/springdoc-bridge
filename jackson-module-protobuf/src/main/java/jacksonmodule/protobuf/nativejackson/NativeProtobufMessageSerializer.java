package jacksonmodule.protobuf.nativejackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.ProtocolMessageEnum;
import java.io.IOException;
import java.util.List;

/**
 * Native Jackson serializer for protobuf messages that doesn't depend on JsonFormat.
 * This implementation uses protobuf reflection API to serialize messages directly to JSON.
 *
 * @param <T> protobuf message type
 */
final class NativeProtobufMessageSerializer<T extends MessageOrBuilder> extends JsonSerializer<T> {

    private final NativeJacksonProtobufModule.Options options;

    public NativeProtobufMessageSerializer(NativeJacksonProtobufModule.Options options) {
        this.options = options;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        Message message = (value instanceof Message) ? (Message) value : ((Message.Builder) value).build();
        Descriptors.Descriptor descriptor = message.getDescriptorForType();

        for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
            if (shouldIncludeField(message, field)) {
                writeField(gen, serializers, message, field);
            }
        }

        gen.writeEndObject();
    }

    private boolean shouldIncludeField(Message message, Descriptors.FieldDescriptor field) {
        if (field.isRepeated()) {
            return message.getRepeatedFieldCount(field) > 0;
        }

        if (message.hasField(field)) {
            return true;
        }

        // Default behavior: include default value fields (similar to JsonFormat default)
        return true;
    }

    private void writeField(
            JsonGenerator gen, SerializerProvider serializers, Message message, Descriptors.FieldDescriptor field)
            throws IOException {
        String fieldName = getFieldName(field);
        gen.writeFieldName(fieldName);

        if (field.isRepeated()) {
            if (field.isMapField()) {
                writeMapField(gen, serializers, message, field);
            } else {
                writeRepeatedField(gen, serializers, message, field);
            }
        } else {
            Object value = message.getField(field);
            writeFieldValue(gen, serializers, field, value);
        }
    }

    private String getFieldName(Descriptors.FieldDescriptor field) {
        // Default behavior: use JSON names (camelCase) instead of proto field names (snake_case)
        return field.getJsonName();
    }

    private void writeRepeatedField(
            JsonGenerator gen, SerializerProvider serializers, Message message, Descriptors.FieldDescriptor field)
            throws IOException {
        gen.writeStartArray();
        int count = message.getRepeatedFieldCount(field);
        for (int i = 0; i < count; i++) {
            Object value = message.getRepeatedField(field, i);
            writeFieldValue(gen, serializers, field, value);
        }
        gen.writeEndArray();
    }

    private void writeMapField(
            JsonGenerator gen, SerializerProvider serializers, Message message, Descriptors.FieldDescriptor field)
            throws IOException {
        gen.writeStartObject();

        @SuppressWarnings("unchecked")
        List<Message> entries = (List<Message>) message.getField(field);

        Descriptors.FieldDescriptor keyField = field.getMessageType().findFieldByName("key");
        Descriptors.FieldDescriptor valueField = field.getMessageType().findFieldByName("value");

        for (Message entry : entries) {
            Object key = entry.getField(keyField);
            Object value = entry.getField(valueField);

            gen.writeFieldName(key.toString());
            writeFieldValue(gen, serializers, valueField, value);
        }

        gen.writeEndObject();
    }

    private void writeFieldValue(
            JsonGenerator gen, SerializerProvider serializers, Descriptors.FieldDescriptor field, Object value)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        switch (field.getType()) {
            case BOOL -> gen.writeBoolean((Boolean) value);
            case INT32, SINT32, SFIXED32 -> serializers.defaultSerializeValue(value, gen);
            case INT64, SINT64, SFIXED64, UINT64 -> gen.writeNumber((Long) value);
            case UINT32, FIXED32 -> gen.writeNumber(((Integer) value) & 0xFFFFFFFFL);
            case FIXED64 -> gen.writeNumber((Long) value);
            case FLOAT -> gen.writeNumber((Float) value);
            case DOUBLE -> gen.writeNumber((Double) value);
            case STRING -> gen.writeString((String) value);
            case BYTES -> {
                com.google.protobuf.ByteString bytes = (com.google.protobuf.ByteString) value;
                gen.writeString(java.util.Base64.getEncoder().encodeToString(bytes.toByteArray()));
            }
            case ENUM -> {
                if (value instanceof Descriptors.EnumValueDescriptor enumDescriptor) {
                    if (options.serializeEnumAsInt()) {
                        gen.writeNumber(enumDescriptor.getNumber());
                    } else {
                        gen.writeString(enumDescriptor.getName());
                    }
                } else if (value instanceof ProtocolMessageEnum enumValue) {
                    if (options.serializeEnumAsInt()) {
                        gen.writeNumber(enumValue.getNumber());
                    } else {
                        gen.writeString(((Enum<?>) enumValue).name());
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported enum type: " + value.getClass());
                }
            }
            case MESSAGE -> {
                // Handle well-known types specially
                Message messageValue = (Message) value;
                if (isWellKnownType(messageValue)) {
                    writeWellKnownType(gen, messageValue);
                } else {
                    // Recursively serialize nested messages
                    serializers.defaultSerializeValue(messageValue, gen);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported field type: " + field.getType());
        }
    }

    private boolean isWellKnownType(Message message) {
        String typeName = message.getDescriptorForType().getFullName();
        return typeName.startsWith("google.protobuf.");
    }

    private void writeWellKnownType(JsonGenerator gen, Message message) throws IOException {
        String typeName = message.getDescriptorForType().getFullName();

        switch (typeName) {
            case "google.protobuf.Timestamp" -> writeTimestamp(gen, message);
            case "google.protobuf.Duration" -> writeDuration(gen, message);
            case "google.protobuf.Value" -> writeValue(gen, message);
            case "google.protobuf.Struct" -> writeStruct(gen, message);
            case "google.protobuf.ListValue" -> writeListValue(gen, message);
            case "google.protobuf.NullValue" -> gen.writeNull();
            case "google.protobuf.BoolValue" -> {
                boolean value = (Boolean)
                        message.getField(message.getDescriptorForType().findFieldByName("value"));
                gen.writeBoolean(value);
            }
            case "google.protobuf.StringValue" -> {
                String value =
                        (String) message.getField(message.getDescriptorForType().findFieldByName("value"));
                gen.writeString(value);
            }
            case "google.protobuf.BytesValue" -> {
                com.google.protobuf.ByteString value = (com.google.protobuf.ByteString)
                        message.getField(message.getDescriptorForType().findFieldByName("value"));
                gen.writeString(java.util.Base64.getEncoder().encodeToString(value.toByteArray()));
            }
            case "google.protobuf.Int32Value", "google.protobuf.UInt32Value" -> {
                int value = (Integer)
                        message.getField(message.getDescriptorForType().findFieldByName("value"));
                gen.writeNumber(value);
            }
            case "google.protobuf.Int64Value", "google.protobuf.UInt64Value" -> {
                long value =
                        (Long) message.getField(message.getDescriptorForType().findFieldByName("value"));
                gen.writeString(String.valueOf(value)); // JSON doesn't support 64-bit integers natively
            }
            case "google.protobuf.FloatValue" -> {
                float value =
                        (Float) message.getField(message.getDescriptorForType().findFieldByName("value"));
                gen.writeNumber(value);
            }
            case "google.protobuf.DoubleValue" -> {
                double value =
                        (Double) message.getField(message.getDescriptorForType().findFieldByName("value"));
                gen.writeNumber(value);
            }
            default -> {
                // For other well-known types, fall back to regular message serialization
                gen.writeStartObject();
                for (Descriptors.FieldDescriptor field :
                        message.getDescriptorForType().getFields()) {
                    if (message.hasField(field)) {
                        gen.writeFieldName(getFieldName(field));
                        writeFieldValue(gen, null, field, message.getField(field));
                    }
                }
                gen.writeEndObject();
            }
        }
    }

    private void writeTimestamp(JsonGenerator gen, Message timestamp) throws IOException {
        long seconds =
                (Long) timestamp.getField(timestamp.getDescriptorForType().findFieldByName("seconds"));
        int nanos =
                (Integer) timestamp.getField(timestamp.getDescriptorForType().findFieldByName("nanos"));

        java.time.Instant instant = java.time.Instant.ofEpochSecond(seconds, nanos);
        gen.writeString(instant.toString());
    }

    private void writeDuration(JsonGenerator gen, Message duration) throws IOException {
        long seconds = (Long) duration.getField(duration.getDescriptorForType().findFieldByName("seconds"));
        int nanos = (Integer) duration.getField(duration.getDescriptorForType().findFieldByName("nanos"));

        StringBuilder sb = new StringBuilder();
        if (seconds < 0 || nanos < 0) {
            sb.append('-');
            seconds = Math.abs(seconds);
            nanos = Math.abs(nanos);
        }
        sb.append(seconds);
        if (nanos != 0) {
            sb.append('.');
            sb.append(String.format("%09d", nanos).replaceAll("0+$", ""));
        }
        sb.append('s');
        gen.writeString(sb.toString());
    }

    private void writeValue(JsonGenerator gen, Message value) throws IOException {
        Descriptors.Descriptor descriptor = value.getDescriptorForType();

        for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
            if (value.hasField(field)) {
                Object fieldValue = value.getField(field);
                writeFieldValue(gen, null, field, fieldValue);
                return;
            }
        }

        gen.writeNull();
    }

    private void writeStruct(JsonGenerator gen, Message struct) throws IOException {
        gen.writeStartObject();

        Descriptors.FieldDescriptor fieldsField = struct.getDescriptorForType().findFieldByName("fields");
        @SuppressWarnings("unchecked")
        List<Message> entries = (List<Message>) struct.getField(fieldsField);

        for (Message entry : entries) {
            String key = (String) entry.getField(entry.getDescriptorForType().findFieldByName("key"));
            Message valueMsg =
                    (Message) entry.getField(entry.getDescriptorForType().findFieldByName("value"));

            gen.writeFieldName(key);
            writeValue(gen, valueMsg);
        }

        gen.writeEndObject();
    }

    private void writeListValue(JsonGenerator gen, Message listValue) throws IOException {
        gen.writeStartArray();

        Descriptors.FieldDescriptor valuesField =
                listValue.getDescriptorForType().findFieldByName("values");
        @SuppressWarnings("unchecked")
        List<Message> values = (List<Message>) listValue.getField(valuesField);

        for (Message valueMsg : values) {
            writeValue(gen, valueMsg);
        }

        gen.writeEndArray();
    }
}
