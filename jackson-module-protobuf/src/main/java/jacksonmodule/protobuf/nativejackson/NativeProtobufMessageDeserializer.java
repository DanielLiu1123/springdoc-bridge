package jacksonmodule.protobuf.nativejackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.NullValue;
import com.google.protobuf.Value;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Native Jackson deserializer for protobuf messages that doesn't depend on JsonFormat.
 * This implementation uses protobuf reflection API to deserialize JSON directly to messages.
 *
 * @param <T> protobuf message type
 */
final class NativeProtobufMessageDeserializer<T extends MessageOrBuilder> extends JsonDeserializer<T> {

    private final NativeJacksonProtobufModule.Options options;
    private final Message defaultInstance;
    private final Map<String, Descriptors.FieldDescriptor> fieldMap;

    public NativeProtobufMessageDeserializer(Class<T> clazz, NativeJacksonProtobufModule.Options options) {
        this.options = options;
        this.defaultInstance = getDefaultInstance(clazz);
        this.fieldMap = buildFieldMap(defaultInstance.getDescriptorForType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode treeNode = p.readValueAsTree();

        if (treeNode.isNull()) {
            return getNullValue(ctxt);
        }

        if (!treeNode.isObject()) {
            throw new IllegalArgumentException(
                    "Expected JSON object for protobuf message, got: " + treeNode.getNodeType());
        }

        Message.Builder builder = defaultInstance.toBuilder();
        ObjectNode objectNode = (ObjectNode) treeNode;

        objectNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();

            Descriptors.FieldDescriptor field = findField(fieldName);
            if (field != null) {
                try {
                    setFieldValue(builder, field, fieldValue);
                } catch (Exception e) {
                    if (!options.ignoringUnknownFields()) {
                        throw new RuntimeException("Failed to set field " + fieldName, e);
                    }
                }
            } else if (!options.ignoringUnknownFields()) {
                throw new RuntimeException("Unknown field: " + fieldName);
            }
        });

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

    private Map<String, Descriptors.FieldDescriptor> buildFieldMap(Descriptors.Descriptor descriptor) {
        Map<String, Descriptors.FieldDescriptor> map = new HashMap<>();

        for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
            // Add both proto name and JSON name
            map.put(field.getName(), field);
            if (!field.getName().equals(field.getJsonName())) {
                map.put(field.getJsonName(), field);
            }
        }

        return map;
    }

    private Descriptors.FieldDescriptor findField(String fieldName) {
        return fieldMap.get(fieldName);
    }

    private void setFieldValue(Message.Builder builder, Descriptors.FieldDescriptor field, JsonNode value) {
        if (value.isNull()) {
            // Don't set null values for proto fields
            return;
        }

        if (field.isRepeated()) {
            setRepeatedField(builder, field, value);
        } else if (field.isMapField()) {
            setMapField(builder, field, value);
        } else {
            Object convertedValue = convertValue(field, value);
            builder.setField(field, convertedValue);
        }
    }

    private void setRepeatedField(Message.Builder builder, Descriptors.FieldDescriptor field, JsonNode value) {
        if (!value.isArray()) {
            throw new IllegalArgumentException("Expected array for repeated field " + field.getName());
        }

        ArrayNode arrayNode = (ArrayNode) value;
        for (JsonNode element : arrayNode) {
            Object convertedValue = convertValue(field, element);
            builder.addRepeatedField(field, convertedValue);
        }
    }

    private void setMapField(Message.Builder builder, Descriptors.FieldDescriptor field, JsonNode value) {
        if (!value.isObject()) {
            throw new IllegalArgumentException("Expected object for map field " + field.getName());
        }

        ObjectNode objectNode = (ObjectNode) value;
        Descriptors.FieldDescriptor keyField = field.getMessageType().findFieldByName("key");
        Descriptors.FieldDescriptor valueField = field.getMessageType().findFieldByName("value");

        objectNode.fields().forEachRemaining(entry -> {
            Object keyValue = convertValue(keyField, entry.getKey());
            Object valueValue = convertValue(valueField, entry.getValue());

            Message.Builder entryBuilder = builder.newBuilderForField(field);
            entryBuilder.setField(keyField, keyValue);
            entryBuilder.setField(valueField, valueValue);
            builder.addRepeatedField(field, entryBuilder.build());
        });
    }

    private Object convertValue(Descriptors.FieldDescriptor field, JsonNode value) {
        if (value.isNull()) {
            return null;
        }

        return switch (field.getType()) {
            case BOOL -> value.asBoolean();
            case INT32, SINT32, SFIXED32 -> value.asInt();
            case INT64, SINT64, SFIXED64 -> value.asLong();
            case UINT32, FIXED32 -> value.asInt();
            case UINT64, FIXED64 -> value.asLong();
            case FLOAT -> (float) value.asDouble();
            case DOUBLE -> value.asDouble();
            case STRING -> value.asText();
            case BYTES -> com.google.protobuf.ByteString.copyFrom(
                    java.util.Base64.getDecoder().decode(value.asText()));
            case ENUM -> convertEnum(field, value);
            case MESSAGE -> convertMessage(field, value);
            default -> throw new IllegalArgumentException("Unsupported field type: " + field.getType());
        };
    }

    private Object convertValue(Descriptors.FieldDescriptor field, String stringValue) {
        return switch (field.getType()) {
            case STRING -> stringValue;
            case INT32, SINT32, SFIXED32 -> Integer.parseInt(stringValue);
            case INT64, SINT64, SFIXED64 -> Long.parseLong(stringValue);
            case UINT32, FIXED32 -> Integer.parseUnsignedInt(stringValue);
            case UINT64, FIXED64 -> Long.parseUnsignedLong(stringValue);
            case FLOAT -> Float.parseFloat(stringValue);
            case DOUBLE -> Double.parseDouble(stringValue);
            case BOOL -> Boolean.parseBoolean(stringValue);
            default -> throw new IllegalArgumentException("Cannot convert string to field type: " + field.getType());
        };
    }

    private Object convertEnum(Descriptors.FieldDescriptor field, JsonNode value) {
        Descriptors.EnumDescriptor enumDescriptor = field.getEnumType();

        if (value.isNumber()) {
            int enumValue = value.asInt();
            Descriptors.EnumValueDescriptor enumValueDescriptor = enumDescriptor.findValueByNumber(enumValue);
            if (enumValueDescriptor != null) {
                return enumValueDescriptor;
            }
        } else if (value.isTextual()) {
            String enumName = value.asText();
            Descriptors.EnumValueDescriptor enumValueDescriptor = enumDescriptor.findValueByName(enumName);
            if (enumValueDescriptor != null) {
                return enumValueDescriptor;
            }
        }

        // Return UNRECOGNIZED if available
        Descriptors.EnumValueDescriptor unrecognized = enumDescriptor.findValueByName("UNRECOGNIZED");
        if (unrecognized != null) {
            return unrecognized;
        }

        throw new IllegalArgumentException("Unknown enum value: " + value + " for enum " + enumDescriptor.getName());
    }

    private Object convertMessage(Descriptors.FieldDescriptor field, JsonNode value) {
        Descriptors.Descriptor messageDescriptor = field.getMessageType();
        String typeName = messageDescriptor.getFullName();

        // Handle well-known types specially
        if (typeName.startsWith("google.protobuf.")) {
            return convertWellKnownType(typeName, value);
        }

        // For regular messages, we need to recursively deserialize
        try {
            Class<?> messageClass = Class.forName(getJavaClassName(messageDescriptor));
            Message defaultInstance = getDefaultInstance(messageClass);

            @SuppressWarnings("unchecked")
            NativeProtobufMessageDeserializer<MessageOrBuilder> deserializer =
                    new NativeProtobufMessageDeserializer<>((Class<MessageOrBuilder>) messageClass, options);

            // Create a temporary parser for the nested message
            JsonParser nestedParser = value.traverse();
            nestedParser.nextToken(); // Move to the first token

            return deserializer.deserialize(nestedParser, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize nested message", e);
        }
    }

    private Object convertWellKnownType(String typeName, JsonNode value) {
        return switch (typeName) {
            case "google.protobuf.Timestamp" -> convertTimestamp(value);
            case "google.protobuf.Duration" -> convertDuration(value);
            case "google.protobuf.Value" -> convertValue(value);
            case "google.protobuf.Struct" -> convertStruct(value);
            case "google.protobuf.ListValue" -> convertListValue(value);
            case "google.protobuf.NullValue" -> com.google.protobuf.NullValue.NULL_VALUE;
            case "google.protobuf.BoolValue" -> com.google.protobuf.BoolValue.of(value.asBoolean());
            case "google.protobuf.StringValue" -> com.google.protobuf.StringValue.of(value.asText());
            case "google.protobuf.BytesValue" -> com.google.protobuf.BytesValue.of(
                    com.google.protobuf.ByteString.copyFrom(
                            java.util.Base64.getDecoder().decode(value.asText())));
            case "google.protobuf.Int32Value" -> com.google.protobuf.Int32Value.of(value.asInt());
            case "google.protobuf.UInt32Value" -> com.google.protobuf.UInt32Value.of(value.asInt());
            case "google.protobuf.Int64Value" -> com.google.protobuf.Int64Value.of(value.asLong());
            case "google.protobuf.UInt64Value" -> com.google.protobuf.UInt64Value.of(value.asLong());
            case "google.protobuf.FloatValue" -> com.google.protobuf.FloatValue.of((float) value.asDouble());
            case "google.protobuf.DoubleValue" -> com.google.protobuf.DoubleValue.of(value.asDouble());
            default -> throw new IllegalArgumentException("Unsupported well-known type: " + typeName);
        };
    }

    private Object convertTimestamp(JsonNode value) {
        if (!value.isTextual()) {
            throw new IllegalArgumentException("Timestamp must be a string");
        }

        java.time.Instant instant = java.time.Instant.parse(value.asText());
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private Object convertDuration(JsonNode value) {
        if (!value.isTextual()) {
            throw new IllegalArgumentException("Duration must be a string");
        }

        String durationStr = value.asText();
        if (!durationStr.endsWith("s")) {
            throw new IllegalArgumentException("Duration must end with 's'");
        }

        durationStr = durationStr.substring(0, durationStr.length() - 1);

        boolean negative = durationStr.startsWith("-");
        if (negative) {
            durationStr = durationStr.substring(1);
        }

        String[] parts = durationStr.split("\\.");
        long seconds = Long.parseLong(parts[0]);
        int nanos = 0;

        if (parts.length > 1) {
            String nanoStr = parts[1];
            // Pad with zeros to make it 9 digits
            nanoStr = String.format("%-9s", nanoStr).replace(' ', '0');
            nanos = Integer.parseInt(nanoStr);
        }

        if (negative) {
            seconds = -seconds;
            nanos = -nanos;
        }

        return com.google.protobuf.Duration.newBuilder()
                .setSeconds(seconds)
                .setNanos(nanos)
                .build();
    }

    private Object convertValue(JsonNode value) {
        com.google.protobuf.Value.Builder builder = com.google.protobuf.Value.newBuilder();

        if (value.isNull()) {
            builder.setNullValue(com.google.protobuf.NullValue.NULL_VALUE);
        } else if (value.isBoolean()) {
            builder.setBoolValue(value.asBoolean());
        } else if (value.isNumber()) {
            builder.setNumberValue(value.asDouble());
        } else if (value.isTextual()) {
            builder.setStringValue(value.asText());
        } else if (value.isArray()) {
            builder.setListValue((com.google.protobuf.ListValue) convertListValue(value));
        } else if (value.isObject()) {
            builder.setStructValue((com.google.protobuf.Struct) convertStruct(value));
        }

        return builder.build();
    }

    private Object convertStruct(JsonNode value) {
        if (!value.isObject()) {
            throw new IllegalArgumentException("Struct must be an object");
        }

        com.google.protobuf.Struct.Builder builder = com.google.protobuf.Struct.newBuilder();
        ObjectNode objectNode = (ObjectNode) value;

        objectNode.fields().forEachRemaining(entry -> {
            com.google.protobuf.Value fieldValue = (com.google.protobuf.Value) convertValue(entry.getValue());
            builder.putFields(entry.getKey(), fieldValue);
        });

        return builder.build();
    }

    private Object convertListValue(JsonNode value) {
        if (!value.isArray()) {
            throw new IllegalArgumentException("ListValue must be an array");
        }

        com.google.protobuf.ListValue.Builder builder = com.google.protobuf.ListValue.newBuilder();
        ArrayNode arrayNode = (ArrayNode) value;

        for (JsonNode element : arrayNode) {
            com.google.protobuf.Value elementValue = (com.google.protobuf.Value) convertValue(element);
            builder.addValues(elementValue);
        }

        return builder.build();
    }

    private String getJavaClassName(Descriptors.Descriptor descriptor) {
        // Convert protobuf descriptor to Java class name
        // This is a simplified implementation
        String packageName = descriptor.getFile().getOptions().getJavaPackage();
        String outerClassName = descriptor.getFile().getOptions().getJavaOuterClassname();
        String messageName = descriptor.getName();

        if (packageName.isEmpty()) {
            return outerClassName + "$" + messageName;
        } else {
            return packageName + "." + outerClassName + "$" + messageName;
        }
    }

    /**
     * Gets the default instance of a protobuf message class using reflection.
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
