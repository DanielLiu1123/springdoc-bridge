package jacksonmodule.protobuf.nativejackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pet.v1.Address;
import pet.v1.Owner;
import pet.v1.Pet;
import pet.v1.PetStatus;
import pet.v1.PetType;

@DisplayName("NativeJacksonProtobufModule Tests")
class NativeJacksonProtobufModuleTest {

    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder()
                .addModule(new NativeJacksonProtobufModule())
                .build();
    }

    @Nested
    @DisplayName("Message Serialization Tests")
    class MessageSerializationTests {

        @Test
        void shouldSerializeSimplePetMessage() {
            // Arrange
            var pet = Pet.newBuilder()
                    .setId("pet-123")
                    .setName("Fluffy")
                    .setType(PetType.CAT)
                    .setStatus(PetStatus.AVAILABLE)
                    .build();

            // Act
            var actual = writeValueAsString(pet);

            // Assert
            var expected = "{\"id\":\"pet-123\",\"name\":\"Fluffy\",\"type\":\"CAT\",\"status\":\"AVAILABLE\"}";
            assertThat(actual).contains("\"id\":\"pet-123\"");
            assertThat(actual).contains("\"name\":\"Fluffy\"");
            assertThat(actual).contains("\"type\":\"CAT\"");
            assertThat(actual).contains("\"status\":\"AVAILABLE\"");
        }

        @Test
        void shouldSerializePetWithOwner() {
            // Arrange
            var address = Address.newBuilder()
                    .setStreet("123 Main St")
                    .setCity("Anytown")
                    .setState("CA")
                    .setZipCode("12345")
                    .setCountry("USA")
                    .build();

            var owner = Owner.newBuilder()
                    .setId("owner-456")
                    .setName("John Doe")
                    .setEmail("john@example.com")
                    .setPhone("555-1234")
                    .setAddress(address)
                    .build();

            var pet = Pet.newBuilder()
                    .setId("pet-789")
                    .setName("Buddy")
                    .setType(PetType.DOG)
                    .setStatus(PetStatus.SOLD)
                    .setOwner(owner)
                    .build();

            // Act
            var actual = writeValueAsString(pet);

            // Assert
            assertThat(actual).contains("\"id\":\"pet-789\"");
            assertThat(actual).contains("\"name\":\"Buddy\"");
            assertThat(actual).contains("\"type\":\"DOG\"");
            assertThat(actual).contains("\"status\":\"SOLD\"");
            assertThat(actual).contains("\"owner\":");
            assertThat(actual).contains("\"id\":\"owner-456\"");
            assertThat(actual).contains("\"name\":\"John Doe\"");
            assertThat(actual).contains("\"email\":\"john@example.com\"");
        }

        @Test
        void shouldSerializePetWithRepeatedFields() {
            // Arrange
            var pet = Pet.newBuilder()
                    .setId("pet-repeat")
                    .setName("Multi")
                    .setType(PetType.BIRD)
                    .setStatus(PetStatus.AVAILABLE)
                    .addTags("friendly")
                    .addTags("trained")
                    .addTags("colorful")
                    .build();

            // Act
            var actual = writeValueAsString(pet);

            // Assert
            assertThat(actual).contains("\"tags\":[\"friendly\",\"trained\",\"colorful\"]");
        }

        @Test
        void shouldSerializePetWithMapFields() {
            // Arrange
            var pet = Pet.newBuilder()
                    .setId("pet-map")
                    .setName("Mapped")
                    .setType(PetType.FISH)
                    .setStatus(PetStatus.PENDING)
                    .putMetadata("color", "blue")
                    .putMetadata("size", "small")
                    .build();

            // Act
            var actual = writeValueAsString(pet);

            // Assert
            assertThat(actual).contains("\"metadata\":");
            assertThat(actual).contains("\"color\":\"blue\"");
            assertThat(actual).contains("\"size\":\"small\"");
        }
    }

    @Nested
    @DisplayName("Message Deserialization Tests")
    class MessageDeserializationTests {

        @Test
        void shouldDeserializeSimplePetMessage() {
            // Arrange
            var inputJson =
                    """
                    {
                        "id": "pet-456",
                        "name": "Whiskers",
                        "type": "CAT",
                        "status": "PENDING"
                    }
                    """;

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            var expected = Pet.newBuilder()
                    .setId("pet-456")
                    .setName("Whiskers")
                    .setType(PetType.CAT)
                    .setStatus(PetStatus.PENDING)
                    .build();
            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getName()).isEqualTo(expected.getName());
            assertThat(actual.getType()).isEqualTo(expected.getType());
            assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
        }

        @Test
        void shouldDeserializePetWithNestedMessage() {
            // Arrange
            var inputJson =
                    """
                    {
                        "id": "pet-nested",
                        "name": "Nested Pet",
                        "type": "DOG",
                        "status": "AVAILABLE",
                        "owner": {
                            "id": "owner-123",
                            "name": "Jane Smith",
                            "email": "jane@example.com",
                            "address": {
                                "street": "456 Oak Ave",
                                "city": "Springfield",
                                "state": "IL",
                                "zipCode": "62701",
                                "country": "USA"
                            }
                        }
                    }
                    """;

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            assertThat(actual.getId()).isEqualTo("pet-nested");
            assertThat(actual.getName()).isEqualTo("Nested Pet");
            assertThat(actual.getType()).isEqualTo(PetType.DOG);
            assertThat(actual.getStatus()).isEqualTo(PetStatus.AVAILABLE);
            assertThat(actual.hasOwner()).isTrue();
            assertThat(actual.getOwner().getId()).isEqualTo("owner-123");
            assertThat(actual.getOwner().getName()).isEqualTo("Jane Smith");
            assertThat(actual.getOwner().getEmail()).isEqualTo("jane@example.com");
            assertThat(actual.getOwner().getAddress().getStreet()).isEqualTo("456 Oak Ave");
        }

        @Test
        void shouldDeserializePetWithRepeatedFields() {
            // Arrange
            var inputJson =
                    """
                    {
                        "id": "pet-array",
                        "name": "Array Pet",
                        "type": "BIRD",
                        "status": "AVAILABLE",
                        "tags": ["smart", "talkative", "colorful"]
                    }
                    """;

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            assertThat(actual.getTagsList()).containsExactly("smart", "talkative", "colorful");
        }

        @Test
        void shouldDeserializePetWithMapFields() {
            // Arrange
            var inputJson =
                    """
                    {
                        "id": "pet-map",
                        "name": "Map Pet",
                        "type": "FISH",
                        "status": "PENDING",
                        "metadata": {
                            "tank": "saltwater",
                            "temperature": "24C"
                        }
                    }
                    """;

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            assertThat(actual.getMetadataMap()).containsEntry("tank", "saltwater");
            assertThat(actual.getMetadataMap()).containsEntry("temperature", "24C");
        }

        @Test
        void shouldIgnoreUnknownFieldsByDefault() {
            // Arrange
            var inputJson =
                    """
                    {
                        "id": "pet-unknown",
                        "name": "Unknown Fields Pet",
                        "type": "CAT",
                        "status": "AVAILABLE",
                        "unknownField": "should be ignored",
                        "anotherUnknown": 123
                    }
                    """;

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            assertThat(actual.getId()).isEqualTo("pet-unknown");
            assertThat(actual.getName()).isEqualTo("Unknown Fields Pet");
            assertThat(actual.getType()).isEqualTo(PetType.CAT);
            assertThat(actual.getStatus()).isEqualTo(PetStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("Enum Serialization Tests")
    class EnumSerializationTests {

        @ParameterizedTest
        @ValueSource(strings = {"DOG", "CAT", "BIRD", "FISH"})
        void shouldSerializeEnumAsStringByDefault(String enumName) {
            // Arrange
            var petType = PetType.valueOf(enumName);
            var pet = Pet.newBuilder()
                    .setId("enum-test")
                    .setName("Enum Test")
                    .setType(petType)
                    .setStatus(PetStatus.AVAILABLE)
                    .build();

            // Act
            var actual = writeValueAsString(pet);

            // Assert
            var expected = "\"type\":\"" + enumName + "\"";
            assertThat(actual).contains(expected);
        }

        @Test
        void shouldSerializeEnumAsIntegerWhenConfigured() {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.withEnumAsInt();
            var mapper = JsonMapper.builder()
                    .addModule(new NativeJacksonProtobufModule(options))
                    .build();

            var pet = Pet.newBuilder()
                    .setId("enum-int-test")
                    .setName("Enum Int Test")
                    .setType(PetType.CAT) // CAT = 2
                    .setStatus(PetStatus.PENDING) // PENDING = 2
                    .build();

            // Act
            var actual = writeValueAsString(mapper, pet);

            // Assert
            assertThat(actual).contains("\"type\":2");
            assertThat(actual).contains("\"status\":2");
        }
    }

    @Nested
    @DisplayName("Enum Deserialization Tests")
    class EnumDeserializationTests {

        @ParameterizedTest
        @ValueSource(strings = {"DOG", "CAT", "BIRD", "FISH"})
        void shouldDeserializeEnumFromString(String enumName) {
            // Arrange
            var inputJson = String.format(
                    """
                    {
                        "id": "enum-test",
                        "name": "Enum Test",
                        "type": "%s",
                        "status": "AVAILABLE"
                    }
                    """,
                    enumName);

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            var expected = PetType.valueOf(enumName);
            assertThat(actual.getType()).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4})
        void shouldDeserializeEnumFromInteger(int enumValue) {
            // Arrange
            var inputJson = String.format(
                    """
                    {
                        "id": "enum-int-test",
                        "name": "Enum Int Test",
                        "type": %d,
                        "status": "AVAILABLE"
                    }
                    """,
                    enumValue);

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            var expected = PetType.forNumber(enumValue);
            assertThat(actual.getType()).isEqualTo(expected);
        }

        @Test
        void shouldHandleUnrecognizedEnumValue() {
            // Arrange
            var inputJson =
                    """
                    {
                        "id": "unrecognized-enum",
                        "name": "Unrecognized Enum",
                        "type": "UNKNOWN_TYPE",
                        "status": "AVAILABLE"
                    }
                    """;

            // Act
            var actual = readValue(inputJson, Pet.class);

            // Assert
            assertThat(actual.getType()).isEqualTo(PetType.UNRECOGNIZED);
        }
    }

    @Nested
    @DisplayName("Module Configuration Tests")
    class ModuleConfigurationTests {

        @Test
        void shouldWorkWithDefaultOptions() {
            // Arrange
            var module = new NativeJacksonProtobufModule();
            var mapper = JsonMapper.builder().addModule(module).build();
            var pet = Pet.newBuilder()
                    .setId("default-test")
                    .setName("Default Test")
                    .setType(PetType.DOG)
                    .setStatus(PetStatus.AVAILABLE)
                    .build();

            // Act
            var json = writeValueAsString(mapper, pet);
            var deserializedPet = readValue(mapper, json, Pet.class);

            // Assert
            assertThat(deserializedPet.getId()).isEqualTo(pet.getId());
            assertThat(deserializedPet.getName()).isEqualTo(pet.getName());
            assertThat(deserializedPet.getType()).isEqualTo(pet.getType());
            assertThat(deserializedPet.getStatus()).isEqualTo(pet.getStatus());
        }

        @Test
        void shouldWorkWithCustomOptions() {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.builder()
                    .serializeEnumAsInt(true)
                    .ignoringUnknownFields(false)
                    .includingDefaultValueFields(false)
                    .preservingProtoFieldNames(true)
                    .build();
            var module = new NativeJacksonProtobufModule(options);
            var mapper = JsonMapper.builder().addModule(module).build();
            var pet = Pet.newBuilder()
                    .setId("custom-test")
                    .setName("Custom Test")
                    .setType(PetType.CAT)
                    .setStatus(PetStatus.PENDING)
                    .build();

            // Act
            var actual = writeValueAsString(mapper, pet);

            // Assert
            assertThat(actual).contains("\"type\":2"); // CAT as integer
            assertThat(actual).contains("\"status\":2"); // PENDING as integer
        }
    }

    @SneakyThrows
    private String writeValueAsString(Object value) {
        return jsonMapper.writeValueAsString(value);
    }

    @SneakyThrows
    private String writeValueAsString(JsonMapper mapper, Object value) {
        return mapper.writeValueAsString(value);
    }

    @SneakyThrows
    private <T> T readValue(String json, Class<T> clazz) {
        return jsonMapper.readValue(json, clazz);
    }

    @SneakyThrows
    private <T> T readValue(JsonMapper mapper, String json, Class<T> clazz) {
        return mapper.readValue(json, clazz);
    }
}
