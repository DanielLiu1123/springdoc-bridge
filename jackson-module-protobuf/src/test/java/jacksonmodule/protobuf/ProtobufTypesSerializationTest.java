package jacksonmodule.protobuf;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Timestamps;
import java.time.Instant;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pet.v1.Pet;
import pet.v1.PetStatus;
import pet.v1.PetType;
import types.v1.TypesTest;

@DisplayName("Protobuf Types Serialization and Deserialization Tests")
class ProtobufTypesSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var typeRegistry = TypeRegistry.newBuilder()
                .add(Timestamp.getDescriptor())
                .add(Pet.getDescriptor())
                .build();

        var printer = JsonFormat.printer()
                .omittingInsignificantWhitespace()
                .includingDefaultValueFields()
                .usingTypeRegistry(typeRegistry);

        var parser = JsonFormat.parser().ignoringUnknownFields().usingTypeRegistry(typeRegistry);

        var options =
                ProtobufModule.Options.builder().printer(printer).parser(parser).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new ProtobufModule(options));
    }

    @Nested
    @DisplayName("Timestamp Field Tests")
    class TimestampFieldTests {

        @Test
        @DisplayName("Should serialize and deserialize Timestamp field correctly")
        void shouldSerializeAndDeserializeTimestampField() {
            // Arrange
            Instant now = Instant.parse("2024-01-15T10:30:45.123456789Z");
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .setNanos(now.getNano())
                    .build();

            TypesTest typesTest =
                    TypesTest.newBuilder().setTimestampField(timestamp).build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"timestampField\":\"2024-01-15T10:30:45.123456789Z\"");
            assertThat(deserializedTypesTest.getTimestampField()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("Should handle empty Timestamp field")
        void shouldHandleEmptyTimestampField() {
            // Arrange
            TypesTest typesTest = TypesTest.newBuilder().build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(deserializedTypesTest.hasTimestampField()).isFalse();
        }
    }

    @Nested
    @DisplayName("Duration Field Tests")
    class DurationFieldTests {

        @Test
        @DisplayName("Should serialize and deserialize Duration field correctly")
        void shouldSerializeAndDeserializeDurationField() {
            // Arrange
            Duration duration = Duration.newBuilder()
                    .setSeconds(3661) // 1 hour, 1 minute, 1 second
                    .setNanos(123456789)
                    .build();

            TypesTest typesTest =
                    TypesTest.newBuilder().setDurationField(duration).build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"durationField\":\"3661.123456789s\"");
            assertThat(deserializedTypesTest.getDurationField()).isEqualTo(duration);
        }

        @Test
        @DisplayName("Should handle negative Duration")
        void shouldHandleNegativeDuration() {
            // Arrange
            Duration duration =
                    Duration.newBuilder().setSeconds(-30).setNanos(-500000000).build();

            TypesTest typesTest =
                    TypesTest.newBuilder().setDurationField(duration).build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert - protobuf JSON format may trim trailing zeros
            assertThat(json).contains("\"durationField\":\"-30.500s\"");
            assertThat(deserializedTypesTest.getDurationField()).isEqualTo(duration);
        }
    }

    @Nested
    @DisplayName("Wrapper Types Tests")
    class WrapperTypesTests {

        @Test
        @DisplayName("Should serialize and deserialize BoolValue wrapper")
        void shouldSerializeAndDeserializeBoolValue() {
            // Arrange
            TypesTest typesTest =
                    TypesTest.newBuilder().setBoolWrapper(BoolValue.of(true)).build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"boolWrapper\":true");
            assertThat(deserializedTypesTest.getBoolWrapper().getValue()).isTrue();
        }

        @Test
        @DisplayName("Should serialize and deserialize Int32Value wrapper")
        void shouldSerializeAndDeserializeInt32Value() {
            // Arrange
            TypesTest typesTest =
                    TypesTest.newBuilder().setInt32Wrapper(Int32Value.of(42)).build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"int32Wrapper\":42");
            assertThat(deserializedTypesTest.getInt32Wrapper().getValue()).isEqualTo(42);
        }

        @Test
        @DisplayName("Should serialize and deserialize Int64Value wrapper")
        void shouldSerializeAndDeserializeInt64Value() {
            // Arrange
            TypesTest typesTest = TypesTest.newBuilder()
                    .setInt64Wrapper(Int64Value.of(9223372036854775807L))
                    .build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"int64Wrapper\":\"9223372036854775807\"");
            assertThat(deserializedTypesTest.getInt64Wrapper().getValue()).isEqualTo(9223372036854775807L);
        }

        @Test
        @DisplayName("Should serialize and deserialize UInt32Value wrapper")
        void shouldSerializeAndDeserializeUInt32Value() {
            // Arrange - Using max int value for UInt32
            TypesTest typesTest = TypesTest.newBuilder()
                    .setUint32Wrapper(UInt32Value.of(2147483647)) // Max int value
                    .build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"uint32Wrapper\":2147483647");
            assertThat(deserializedTypesTest.getUint32Wrapper().getValue()).isEqualTo(2147483647);
        }

        @Test
        @DisplayName("Should serialize and deserialize UInt64Value wrapper")
        void shouldSerializeAndDeserializeUInt64Value() {
            // Arrange - Using a large but valid long value for UInt64
            TypesTest typesTest = TypesTest.newBuilder()
                    .setUint64Wrapper(UInt64Value.of(9223372036854775807L)) // Max long value
                    .build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"uint64Wrapper\":\"9223372036854775807\"");
            assertThat(deserializedTypesTest.getUint64Wrapper().getValue()).isEqualTo(9223372036854775807L);
        }

        @Test
        @DisplayName("Should serialize and deserialize FloatValue wrapper")
        void shouldSerializeAndDeserializeFloatValue() {
            // Arrange
            TypesTest typesTest = TypesTest.newBuilder()
                    .setFloatWrapper(FloatValue.of(3.14159f))
                    .build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"floatWrapper\":3.14159");
            assertThat(deserializedTypesTest.getFloatWrapper().getValue()).isEqualTo(3.14159f);
        }

        @Test
        @DisplayName("Should serialize and deserialize DoubleValue wrapper")
        void shouldSerializeAndDeserializeDoubleValue() {
            // Arrange
            TypesTest typesTest = TypesTest.newBuilder()
                    .setDoubleWrapper(DoubleValue.of(2.718281828459045))
                    .build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"doubleWrapper\":2.718281828459045");
            assertThat(deserializedTypesTest.getDoubleWrapper().getValue()).isEqualTo(2.718281828459045);
        }

        @Test
        @DisplayName("Should serialize and deserialize StringValue wrapper")
        void shouldSerializeAndDeserializeStringValue() {
            // Arrange
            TypesTest typesTest = TypesTest.newBuilder()
                    .setStringWrapper(StringValue.of("Hello, World!"))
                    .build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"stringWrapper\":\"Hello, World!\"");
            assertThat(deserializedTypesTest.getStringWrapper().getValue()).isEqualTo("Hello, World!");
        }

        @Test
        @DisplayName("Should serialize and deserialize BytesValue wrapper")
        void shouldSerializeAndDeserializeBytesValue() {
            // Arrange
            byte[] data = "Hello, Bytes!".getBytes();
            TypesTest typesTest = TypesTest.newBuilder()
                    .setBytesWrapper(BytesValue.of(com.google.protobuf.ByteString.copyFrom(data)))
                    .build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"bytesWrapper\":\"SGVsbG8sIEJ5dGVzIQ==\""); // Base64 encoded
            assertThat(deserializedTypesTest.getBytesWrapper().getValue().toByteArray())
                    .isEqualTo(data);
        }
    }

    @SneakyThrows
    private String writeValueAsString(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    @SneakyThrows
    private <T> T readValue(String json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }

    @Nested
    @DisplayName("Complex Types Tests")
    class ComplexTypesTests {

        @Test
        @DisplayName("Should serialize and deserialize Any field")
        void shouldSerializeAndDeserializeAnyField() {
            // Arrange - Any field with default instance (empty Any)
            // Note: Full Any type support requires TypeRegistry configuration
            TypesTest typesTest =
                    TypesTest.newBuilder().setAnyField(Any.getDefaultInstance()).build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert - Verify basic Any field serialization
            assertThat(json).contains("\"anyField\":");
            assertThat(deserializedTypesTest.getAnyField()).isEqualTo(Any.getDefaultInstance());
        }

        @Test
        @DisplayName("Should serialize and deserialize Any field with Pet message")
        void shouldSerializeAndDeserializeAnyFieldWithPetMessage() throws InvalidProtocolBufferException {
            // Arrange
            Pet pet = Pet.newBuilder()
                    .setId("any-test")
                    .setName("Any Test Pet")
                    .setType(PetType.DOG)
                    .setStatus(PetStatus.AVAILABLE)
                    .build();

            TypesTest typesTest =
                    TypesTest.newBuilder().setAnyField(Any.pack(pet)).build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json)
                    .isEqualTo(
                            """
                    {"anyField":{"@type":"type.googleapis.com/pet.v1.Pet","id":"any-test","name":"Any Test Pet","type":"DOG","status":"AVAILABLE","tags":[],"metadata":{},"previousAddresses":[]},"nullValueField":null}""");
            assertThat(deserializedTypesTest.getAnyField().unpack(Pet.class)).isEqualTo(pet);
        }

        @Test
        @DisplayName("Should serialize and deserialize Struct field")
        void shouldSerializeAndDeserializeStructField() {
            // Arrange
            Struct struct = Struct.newBuilder()
                    .putFields(
                            "string_field",
                            Value.newBuilder().setStringValue("test").build())
                    .putFields(
                            "number_field",
                            Value.newBuilder().setNumberValue(42.5).build())
                    .putFields(
                            "bool_field", Value.newBuilder().setBoolValue(true).build())
                    .build();

            TypesTest typesTest = TypesTest.newBuilder().setStructField(struct).build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"structField\":{");
            assertThat(json).contains("\"string_field\":\"test\"");
            assertThat(json).contains("\"number_field\":42.5");
            assertThat(json).contains("\"bool_field\":true");
            assertThat(deserializedTypesTest.getStructField()).isEqualTo(struct);
        }

        @Test
        @DisplayName("Should serialize and deserialize Value field")
        void shouldSerializeAndDeserializeValueField() {
            // Arrange
            Value value = Value.newBuilder().setStringValue("test value").build();

            TypesTest typesTest = TypesTest.newBuilder().setValueField(value).build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"valueField\":\"test value\"");
            assertThat(deserializedTypesTest.getValueField()).isEqualTo(value);
        }

        @Test
        @DisplayName("Should serialize and deserialize ListValue field")
        void shouldSerializeAndDeserializeListValueField() {
            // Arrange
            ListValue listValue = ListValue.newBuilder()
                    .addValues(Value.newBuilder().setStringValue("item1").build())
                    .addValues(Value.newBuilder().setNumberValue(123).build())
                    .addValues(Value.newBuilder().setBoolValue(false).build())
                    .build();

            TypesTest typesTest =
                    TypesTest.newBuilder().setListValueField(listValue).build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert - protobuf JSON may format numbers as 123.0
            assertThat(json).contains("\"listValueField\":[\"item1\",123");
            assertThat(json).contains(",false]");
            assertThat(deserializedTypesTest.getListValueField()).isEqualTo(listValue);
        }

        @Test
        @DisplayName("Should serialize and deserialize NullValue field")
        void shouldSerializeAndDeserializeNullValueField() {
            // Arrange
            TypesTest typesTest = TypesTest.newBuilder()
                    .setNullValueField(NullValue.NULL_VALUE)
                    .build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"nullValueField\":null");
            assertThat(deserializedTypesTest.getNullValueField()).isEqualTo(NullValue.NULL_VALUE);
        }

        @Test
        @DisplayName("Should serialize and deserialize FieldMask field")
        void shouldSerializeAndDeserializeFieldMaskField() {
            // Arrange
            FieldMask fieldMask = FieldMask.newBuilder()
                    .addPaths("user.name")
                    .addPaths("user.email")
                    .addPaths("user.phone_number")
                    .build();

            TypesTest typesTest = TypesTest.newBuilder().setFieldMask(fieldMask).build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"fieldMask\":\"user.name,user.email,user.phoneNumber\"");
            assertThat(deserializedTypesTest.getFieldMask()).isEqualTo(fieldMask);
        }

        @Test
        @DisplayName("Should serialize and deserialize Empty field")
        void shouldSerializeAndDeserializeEmptyField() {
            // Arrange
            TypesTest typesTest = TypesTest.newBuilder()
                    .setEmptyField(Empty.getDefaultInstance())
                    .build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(json).contains("\"emptyField\":{}");
            assertThat(deserializedTypesTest.getEmptyField()).isEqualTo(Empty.getDefaultInstance());
        }
    }

    @Nested
    @DisplayName("Complete Message Tests")
    class CompleteMessageTests {

        @Test
        @DisplayName("Should serialize and deserialize complete TypesTest message")
        void shouldSerializeAndDeserializeCompleteTypesTestMessage() {
            // Arrange
            Instant now = Instant.parse("2024-01-15T10:30:45.123456789Z");
            var ts = Timestamps.fromMillis(now.getEpochSecond());

            TypesTest typesTest = TypesTest.newBuilder()
                    .setTimestampField(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .setDurationField(Duration.newBuilder()
                            .setSeconds(3661)
                            .setNanos(123456789)
                            .build())
                    .setBoolWrapper(BoolValue.of(true))
                    .setInt32Wrapper(Int32Value.of(42))
                    .setInt64Wrapper(Int64Value.of(9223372036854775807L))
                    .setUint32Wrapper(UInt32Value.of(2147483647)) // Max int value
                    .setUint64Wrapper(UInt64Value.of(9223372036854775807L)) // Max long value
                    .setFloatWrapper(FloatValue.of(3.14159f))
                    .setDoubleWrapper(DoubleValue.of(2.718281828459045))
                    .setStringWrapper(StringValue.of("Hello, World!"))
                    .setBytesWrapper(BytesValue.of(com.google.protobuf.ByteString.copyFromUtf8("Hello, Bytes!")))
                    .setAnyField(Any.pack(ts))
                    .setStructField(Struct.newBuilder()
                            .putFields(
                                    "test",
                                    Value.newBuilder().setStringValue("value").build())
                            .build())
                    .setValueField(
                            Value.newBuilder().setStringValue("test value").build())
                    .setListValueField(ListValue.newBuilder()
                            .addValues(
                                    Value.newBuilder().setStringValue("item1").build())
                            .build())
                    .setNullValueField(NullValue.NULL_VALUE)
                    .setFieldMask(FieldMask.newBuilder().addPaths("user.name").build())
                    .setEmptyField(Empty.getDefaultInstance())
                    .build();

            // Act
            String json = writeValueAsString(typesTest);
            TypesTest deserializedTypesTest = readValue(json, TypesTest.class);

            // Assert
            assertThat(deserializedTypesTest).isEqualTo(typesTest);

            // Verify some key JSON structure
            assertThat(json).contains("\"timestampField\":\"2024-01-15T10:30:45.123456789Z\"");
            assertThat(json).contains("\"durationField\":\"3661.123456789s\"");
            assertThat(json).contains("\"boolWrapper\":true");
            assertThat(json).contains("\"stringWrapper\":\"Hello, World!\"");
            assertThat(json).contains("\"emptyField\":{}");
        }
    }
}
