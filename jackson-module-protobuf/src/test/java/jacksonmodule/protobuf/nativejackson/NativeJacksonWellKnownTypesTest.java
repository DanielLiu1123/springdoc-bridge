package jacksonmodule.protobuf.nativejackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import java.time.Instant;
import java.util.Base64;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pet.v1.Pet;
import pet.v1.PetStatus;
import pet.v1.PetType;
import types.v1.TypesTest;

@DisplayName("Native Jackson Well-Known Types Tests")
class NativeJacksonWellKnownTypesTest {

    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder()
                .addModule(new NativeJacksonProtobufModule())
                .build();
    }

    @Nested
    @DisplayName("Timestamp Tests")
    class TimestampTests {

        @Test
        void shouldSerializeTimestamp() {
            // Arrange
            var instant = Instant.parse("2024-01-15T10:30:45.123456789Z");
            var timestamp = Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();
            var pet = Pet.newBuilder()
                    .setId("timestamp-test")
                    .setName("Timestamp Pet")
                    .setType(PetType.DOG)
                    .setStatus(PetStatus.AVAILABLE)
                    .setBirthDate(timestamp)
                    .build();

            // Act
            var actual = writeValueAsString(pet);

            // Assert
            var expected = "\"birthDate\":\"2024-01-15T10:30:45.123456789Z\"";
            assertThat(actual).contains(expected);
        }

        @Test
        void shouldDeserializeTimestamp() {
            // Arrange
            var inputJson =
                    """
                    {
                        "id": "timestamp-deser",
                        "name": "Timestamp Deser",
                        "type": "CAT",
                        "status": "AVAILABLE",
                        "birthDate": "2024-01-15T10:30:45.123456789Z"
                    }
                    """;

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            var expectedInstant = Instant.parse("2024-01-15T10:30:45.123456789Z");
            assertThat(actual.getBirthDate().getSeconds()).isEqualTo(expectedInstant.getEpochSecond());
            assertThat(actual.getBirthDate().getNanos()).isEqualTo(expectedInstant.getNano());
        }
    }

    @Nested
    @DisplayName("Duration Tests")
    class DurationTests {

        @Test
        void shouldSerializeDuration() {
            // Arrange
            var duration =
                    Duration.newBuilder().setSeconds(3661).setNanos(123456789).build();
            var pet = Pet.newBuilder()
                    .setId("duration-test")
                    .setName("Duration Pet")
                    .setType(PetType.BIRD)
                    .setStatus(PetStatus.AVAILABLE)
                    .setLifeExpectancy(duration)
                    .build();

            // Act
            var actual = writeValueAsString(pet);

            // Assert
            var expected = "\"lifeExpectancy\":\"3661.123456789s\"";
            assertThat(actual).contains(expected);
        }

        @Test
        void shouldDeserializeDuration() {
            // Arrange
            var inputJson =
                    """
                    {
                        "id": "duration-deser",
                        "name": "Duration Deser",
                        "type": "FISH",
                        "status": "AVAILABLE",
                        "lifeExpectancy": "3661.123456789s"
                    }
                    """;

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            assertThat(actual.getLifeExpectancy().getSeconds()).isEqualTo(3661);
            assertThat(actual.getLifeExpectancy().getNanos()).isEqualTo(123456789);
        }

        @Test
        void shouldSerializeNegativeDuration() {
            // Arrange
            var duration =
                    Duration.newBuilder().setSeconds(-3661).setNanos(-123456789).build();
            var pet = Pet.newBuilder()
                    .setId("negative-duration")
                    .setName("Negative Duration")
                    .setType(PetType.DOG)
                    .setStatus(PetStatus.AVAILABLE)
                    .setLifeExpectancy(duration)
                    .build();

            // Act
            var actual = writeValueAsString(pet);

            // Assert
            var expected = "\"lifeExpectancy\":\"-3661.123456789s\"";
            assertThat(actual).contains(expected);
        }
    }

    @Nested
    @DisplayName("Wrapper Types Tests")
    class WrapperTypesTests {

        @Test
        void shouldSerializeBoolValue() {
            // Arrange
            var pet = Pet.newBuilder()
                    .setId("bool-test")
                    .setName("Bool Test")
                    .setType(PetType.CAT)
                    .setStatus(PetStatus.AVAILABLE)
                    .setIsVaccinated(BoolValue.of(true))
                    .build();

            // Act
            var actual = writeValueAsString(pet);

            // Assert
            var expected = "\"isVaccinated\":true";
            assertThat(actual).contains(expected);
        }

        @Test
        void shouldDeserializeBoolValue() {
            // Arrange
            var inputJson =
                    """
                    {
                        "id": "bool-deser",
                        "name": "Bool Deser",
                        "type": "DOG",
                        "status": "AVAILABLE",
                        "isVaccinated": false
                    }
                    """;

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            assertThat(actual.getIsVaccinated().getValue()).isFalse();
        }

        @Test
        void shouldSerializeDoubleValue() {
            // Arrange
            var pet = Pet.newBuilder()
                    .setId("double-test")
                    .setName("Double Test")
                    .setType(PetType.FISH)
                    .setStatus(PetStatus.AVAILABLE)
                    .setWeight(DoubleValue.of(2.5))
                    .build();

            // Act
            var actual = writeValueAsString(pet);

            // Assert
            var expected = "\"weight\":2.5";
            assertThat(actual).contains(expected);
        }

        @Test
        void shouldDeserializeDoubleValue() {
            // Arrange
            var inputJson =
                    """
                    {
                        "id": "double-deser",
                        "name": "Double Deser",
                        "type": "BIRD",
                        "status": "AVAILABLE",
                        "weight": 1.8
                    }
                    """;

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            assertThat(actual.getWeight().getValue()).isEqualTo(1.8);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 42, -123, Integer.MAX_VALUE, Integer.MIN_VALUE})
        void shouldSerializeAndDeserializeInt32Value(int value) {
            // Arrange
            var typesTest =
                    TypesTest.newBuilder().setInt32Wrapper(Int32Value.of(value)).build();

            // Act
            var json = writeValueAsString(typesTest);
            var actual = readValue(json, TypesTest.class);

            // Assert
            var expected = value;
            assertThat(actual.getInt32Wrapper().getValue()).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 42L, -123L, Long.MAX_VALUE, Long.MIN_VALUE})
        void shouldSerializeAndDeserializeInt64Value(long value) {
            // Arrange
            var typesTest =
                    TypesTest.newBuilder().setInt64Wrapper(Int64Value.of(value)).build();

            // Act
            var json = writeValueAsString(typesTest);
            var actual = readValue(json, TypesTest.class);

            // Assert
            var expected = String.valueOf(value); // Int64 serialized as string
            assertThat(json).contains("\"" + expected + "\"");
            assertThat(actual.getInt64Wrapper().getValue()).isEqualTo(value);
        }

        @Test
        void shouldSerializeAndDeserializeStringValue() {
            // Arrange
            var typesTest = TypesTest.newBuilder()
                    .setStringWrapper(StringValue.of("Hello, World!"))
                    .build();

            // Act
            var json = writeValueAsString(typesTest);
            var actual = readValue(json, TypesTest.class);

            // Assert
            var expected = "Hello, World!";
            assertThat(json).contains("\"stringWrapper\":\"" + expected + "\"");
            assertThat(actual.getStringWrapper().getValue()).isEqualTo(expected);
        }

        @Test
        void shouldSerializeAndDeserializeBytesValue() {
            // Arrange
            var data = "Hello, Bytes!".getBytes();
            var typesTest = TypesTest.newBuilder()
                    .setBytesWrapper(BytesValue.of(ByteString.copyFrom(data)))
                    .build();

            // Act
            var json = writeValueAsString(typesTest);
            var actual = readValue(json, TypesTest.class);

            // Assert
            var expectedBase64 = Base64.getEncoder().encodeToString(data);
            assertThat(json).contains("\"bytesWrapper\":\"" + expectedBase64 + "\"");
            assertThat(actual.getBytesWrapper().getValue().toByteArray()).isEqualTo(data);
        }

        @ParameterizedTest
        @ValueSource(floats = {0.0f, 3.14f, -2.71f, Float.MAX_VALUE, Float.MIN_VALUE})
        void shouldSerializeAndDeserializeFloatValue(float value) {
            // Arrange
            var typesTest =
                    TypesTest.newBuilder().setFloatWrapper(FloatValue.of(value)).build();

            // Act
            var json = writeValueAsString(typesTest);
            var actual = readValue(json, TypesTest.class);

            // Assert
            var expected = value;
            assertThat(actual.getFloatWrapper().getValue()).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 42, Integer.MAX_VALUE})
        void shouldSerializeAndDeserializeUInt32Value(int value) {
            // Arrange
            var typesTest = TypesTest.newBuilder()
                    .setUint32Wrapper(UInt32Value.of(value))
                    .build();

            // Act
            var json = writeValueAsString(typesTest);
            var actual = readValue(json, TypesTest.class);

            // Assert
            var expected = value;
            assertThat(actual.getUint32Wrapper().getValue()).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 42L, Long.MAX_VALUE})
        void shouldSerializeAndDeserializeUInt64Value(long value) {
            // Arrange
            var typesTest = TypesTest.newBuilder()
                    .setUint64Wrapper(UInt64Value.of(value))
                    .build();

            // Act
            var json = writeValueAsString(typesTest);
            var actual = readValue(json, TypesTest.class);

            // Assert
            var expected = String.valueOf(value); // UInt64 serialized as string
            assertThat(json).contains("\"" + expected + "\"");
            assertThat(actual.getUint64Wrapper().getValue()).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("Struct and Value Tests")
    class StructAndValueTests {

        @Test
        void shouldSerializeNullValue() {
            // Arrange
            var typesTest = TypesTest.newBuilder()
                    .setNullValueField(NullValue.NULL_VALUE)
                    .build();

            // Act
            var actual = writeValueAsString(typesTest);

            // Assert
            assertThat(actual).contains("\"nullValueField\":null");
        }

        @Test
        void shouldSerializeBooleanValue() {
            // Arrange
            var value = Value.newBuilder().setBoolValue(true).build();
            var typesTest = TypesTest.newBuilder().setValueField(value).build();

            // Act
            var actual = writeValueAsString(typesTest);

            // Assert
            assertThat(actual).contains("\"valueField\":true");
        }

        @Test
        void shouldSerializeNumberValue() {
            // Arrange
            var value = Value.newBuilder().setNumberValue(42.5).build();
            var typesTest = TypesTest.newBuilder().setValueField(value).build();

            // Act
            var actual = writeValueAsString(typesTest);

            // Assert
            assertThat(actual).contains("\"valueField\":42.5");
        }

        @Test
        void shouldSerializeStringValue() {
            // Arrange
            var value = Value.newBuilder().setStringValue("Hello, Value!").build();
            var typesTest = TypesTest.newBuilder().setValueField(value).build();

            // Act
            var actual = writeValueAsString(typesTest);

            // Assert
            assertThat(actual).contains("\"valueField\":\"Hello, Value!\"");
        }

        @Test
        void shouldSerializeListValue() {
            // Arrange
            var listValue = ListValue.newBuilder()
                    .addValues(Value.newBuilder().setStringValue("item1"))
                    .addValues(Value.newBuilder().setNumberValue(42))
                    .addValues(Value.newBuilder().setBoolValue(true))
                    .build();
            var typesTest = TypesTest.newBuilder().setListValueField(listValue).build();

            // Act
            var actual = writeValueAsString(typesTest);

            // Assert
            assertThat(actual).contains("\"listValueField\":[\"item1\",42,true]");
        }

        @Test
        void shouldSerializeStruct() {
            // Arrange
            var struct = Struct.newBuilder()
                    .putFields("name", Value.newBuilder().setStringValue("John").build())
                    .putFields("age", Value.newBuilder().setNumberValue(30).build())
                    .putFields("active", Value.newBuilder().setBoolValue(true).build())
                    .build();
            var typesTest = TypesTest.newBuilder().setStructField(struct).build();

            // Act
            var actual = writeValueAsString(typesTest);

            // Assert
            assertThat(actual).contains("\"structField\":");
            assertThat(actual).contains("\"name\":\"John\"");
            assertThat(actual).contains("\"age\":30");
            assertThat(actual).contains("\"active\":true");
        }
    }

    @SneakyThrows
    private String writeValueAsString(Object value) {
        return jsonMapper.writeValueAsString(value);
    }

    @SneakyThrows
    private <T> T readValue(String json, Class<T> clazz) {
        return jsonMapper.readValue(json, clazz);
    }
}
