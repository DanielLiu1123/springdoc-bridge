package jacksondataformat.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pet.v1.Address;
import pet.v1.Owner;
import pet.v1.Pet;
import pet.v1.PetStatus;
import pet.v1.PetType;

@DisplayName("ProtobufModule Tests")
class ProtobufModuleTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new ProtobufModule());
    }

    @Nested
    @DisplayName("Message Serialization Tests")
    class MessageSerializationTests {

        @Test
        @DisplayName("Should serialize simple Pet message to JSON")
        void shouldSerializeSimplePetMessage() {
            // Arrange
            Pet pet = Pet.newBuilder()
                    .setId("pet-123")
                    .setName("Fluffy")
                    .setType(PetType.CAT)
                    .setStatus(PetStatus.AVAILABLE)
                    .build();

            String expectedJson =
                    "{\"id\":\"pet-123\",\"name\":\"Fluffy\",\"type\":\"CAT\",\"status\":\"AVAILABLE\",\"tags\":[],\"metadata\":{},\"previousAddresses\":[]}";

            // Act
            String actualJson = writeValueAsString(pet);

            // Assert
            assertThat(actualJson).isEqualTo(expectedJson);
        }

        @Test
        @DisplayName("Should serialize Pet with nested objects")
        void shouldSerializePetWithNestedObjects() {
            // Arrange
            Address address = Address.newBuilder()
                    .setStreet("123 Main St")
                    .setCity("Springfield")
                    .setState("IL")
                    .setZipCode("62701")
                    .setCountry("USA")
                    .build();

            Owner owner = Owner.newBuilder()
                    .setId("owner-456")
                    .setName("John Doe")
                    .setEmail("john@example.com")
                    .setPhone("555-1234")
                    .setAddress(address)
                    .build();

            Pet pet = Pet.newBuilder()
                    .setId("pet-123")
                    .setName("Buddy")
                    .setType(PetType.DOG)
                    .setStatus(PetStatus.SOLD)
                    .setOwner(owner)
                    .build();

            String expectedJson =
                    "{\"id\":\"pet-123\",\"name\":\"Buddy\",\"type\":\"DOG\",\"status\":\"SOLD\",\"owner\":{\"id\":\"owner-456\",\"name\":\"John Doe\",\"email\":\"john@example.com\",\"phone\":\"555-1234\",\"address\":{\"street\":\"123 Main St\",\"city\":\"Springfield\",\"state\":\"IL\",\"zipCode\":\"62701\",\"country\":\"USA\"}},\"tags\":[],\"metadata\":{},\"previousAddresses\":[]}";

            // Act
            String actualJson = writeValueAsString(pet);

            // Assert
            assertThat(actualJson).isEqualTo(expectedJson);
        }

        @Test
        @DisplayName("Should serialize Pet with complex fields")
        void shouldSerializePetWithComplexFields() {
            // Arrange
            Instant birthDate = Instant.parse("2020-01-15T10:30:00Z");
            Pet pet = Pet.newBuilder()
                    .setId("pet-123")
                    .setName("Max")
                    .setType(PetType.DOG)
                    .setStatus(PetStatus.AVAILABLE)
                    .addAllTags(List.of("friendly", "trained", "vaccinated"))
                    .setBirthDate(Timestamp.newBuilder()
                            .setSeconds(birthDate.getEpochSecond())
                            .setNanos(birthDate.getNano())
                            .build())
                    .setLifeExpectancy(Duration.newBuilder()
                            .setSeconds(12 * 365 * 24 * 60 * 60) // 12 years
                            .build())
                    .setWeight(DoubleValue.of(25.5))
                    .setIsVaccinated(BoolValue.of(true))
                    .putAllMetadata(Map.of(
                            "breed", "Golden Retriever",
                            "color", "Golden",
                            "microchip", "123456789"))
                    .build();

            // Act
            String actualJson = writeValueAsString(pet);

            // Assert
            assertThat(actualJson).contains("\"id\":\"pet-123\"");
            assertThat(actualJson).contains("\"name\":\"Max\"");
            assertThat(actualJson).contains("\"type\":\"DOG\"");
            assertThat(actualJson).contains("\"status\":\"AVAILABLE\"");
            assertThat(actualJson).contains("\"tags\":[\"friendly\",\"trained\",\"vaccinated\"]");
            assertThat(actualJson).contains("\"birthDate\":\"2020-01-15T10:30:00Z\"");
            assertThat(actualJson).contains("\"lifeExpectancy\":\"378432000s\"");
            assertThat(actualJson).contains("\"weight\":25.5");
            assertThat(actualJson).contains("\"isVaccinated\":true");
            assertThat(actualJson).contains("\"metadata\":");
            assertThat(actualJson).contains("\"breed\":\"Golden Retriever\"");
            assertThat(actualJson).contains("\"color\":\"Golden\"");
            assertThat(actualJson).contains("\"microchip\":\"123456789\"");
        }
    }

    @Nested
    @DisplayName("Message Deserialization Tests")
    class MessageDeserializationTests {

        @Test
        @DisplayName("Should deserialize simple Pet message from JSON")
        void shouldDeserializeSimplePetMessage() {
            // Arrange
            String inputJson =
                    """
                    {
                        "id": "pet-456",
                        "name": "Whiskers",
                        "type": "CAT",
                        "status": "PENDING"
                    }
                    """;

            Pet expectedPet = Pet.newBuilder()
                    .setId("pet-456")
                    .setName("Whiskers")
                    .setType(PetType.CAT)
                    .setStatus(PetStatus.PENDING)
                    .build();

            // Act
            Pet actualPet = readValue(inputJson, Pet.class);

            // Assert
            assertThat(actualPet).isEqualTo(expectedPet);
        }

        @Test
        @DisplayName("Should deserialize Pet with nested objects")
        void shouldDeserializePetWithNestedObjects() {
            // Arrange
            String inputJson =
                    """
                    {
                        "id": "pet-789",
                        "name": "Rex",
                        "type": "DOG",
                        "status": "SOLD",
                        "owner": {
                            "id": "owner-123",
                            "name": "Alice Smith",
                            "email": "alice@example.com",
                            "phone": "555-9876",
                            "address": {
                                "street": "456 Oak Ave",
                                "city": "Portland",
                                "state": "OR",
                                "zipCode": "97201",
                                "country": "USA"
                            }
                        }
                    }
                    """;

            Address expectedAddress = Address.newBuilder()
                    .setStreet("456 Oak Ave")
                    .setCity("Portland")
                    .setState("OR")
                    .setZipCode("97201")
                    .setCountry("USA")
                    .build();

            Owner expectedOwner = Owner.newBuilder()
                    .setId("owner-123")
                    .setName("Alice Smith")
                    .setEmail("alice@example.com")
                    .setPhone("555-9876")
                    .setAddress(expectedAddress)
                    .build();

            Pet expectedPet = Pet.newBuilder()
                    .setId("pet-789")
                    .setName("Rex")
                    .setType(PetType.DOG)
                    .setStatus(PetStatus.SOLD)
                    .setOwner(expectedOwner)
                    .build();

            // Act
            Pet actualPet = readValue(inputJson, Pet.class);

            // Assert
            assertThat(actualPet).isEqualTo(expectedPet);
        }

        @Test
        @DisplayName("Should deserialize Pet with complex fields")
        void shouldDeserializePetWithComplexFields() {
            // Arrange
            String inputJson =
                    """
                    {
                        "id": "pet-999",
                        "name": "Luna",
                        "type": "DOG",
                        "status": "AVAILABLE",
                        "tags": ["playful", "energetic", "house-trained"],
                        "birthDate": "2019-03-20T14:30:00Z",
                        "lifeExpectancy": "473040000s",
                        "weight": 18.7,
                        "isVaccinated": true,
                        "metadata": {
                            "breed": "Border Collie",
                            "color": "Black and White",
                            "registration": "AKC-12345"
                        }
                    }
                    """;

            Instant expectedBirthDate = Instant.parse("2019-03-20T14:30:00Z");
            Pet expectedPet = Pet.newBuilder()
                    .setId("pet-999")
                    .setName("Luna")
                    .setType(PetType.DOG)
                    .setStatus(PetStatus.AVAILABLE)
                    .addAllTags(List.of("playful", "energetic", "house-trained"))
                    .setBirthDate(Timestamp.newBuilder()
                            .setSeconds(expectedBirthDate.getEpochSecond())
                            .setNanos(expectedBirthDate.getNano())
                            .build())
                    .setLifeExpectancy(
                            Duration.newBuilder().setSeconds(473040000).build())
                    .setWeight(DoubleValue.of(18.7))
                    .setIsVaccinated(BoolValue.of(true))
                    .putAllMetadata(Map.of(
                            "breed", "Border Collie",
                            "color", "Black and White",
                            "registration", "AKC-12345"))
                    .build();

            // Act
            Pet actualPet = readValue(inputJson, Pet.class);

            // Assert
            assertThat(actualPet).isEqualTo(expectedPet);
        }
    }

    @Nested
    @DisplayName("Enum Serialization and Deserialization Tests")
    class EnumTests {

        @Test
        @DisplayName("Should serialize and deserialize protobuf enums")
        void shouldSerializeAndDeserializeProtobufEnums() {
            // Arrange
            PetType dogType = PetType.DOG;
            PetStatus availableStatus = PetStatus.AVAILABLE;
            String expectedDogTypeJson = "\"DOG\"";
            String expectedStatusJson = "\"AVAILABLE\"";
            PetType expectedDeserializedType = PetType.CAT;
            PetStatus expectedDeserializedStatus = PetStatus.PENDING;

            // Act - Serialize
            String actualDogTypeJson = writeValueAsString(dogType);
            String actualStatusJson = writeValueAsString(availableStatus);

            // Act - Deserialize
            PetType actualDeserializedType = readValue("\"CAT\"", PetType.class);
            PetStatus actualDeserializedStatus = readValue("\"PENDING\"", PetStatus.class);

            // Assert
            assertThat(actualDogTypeJson).isEqualTo(expectedDogTypeJson);
            assertThat(actualStatusJson).isEqualTo(expectedStatusJson);
            assertThat(actualDeserializedType).isEqualTo(expectedDeserializedType);
            assertThat(actualDeserializedStatus).isEqualTo(expectedDeserializedStatus);
        }

        @Test
        @DisplayName("Should handle enum deserialization from numbers")
        void shouldDeserializeEnumsFromNumbers() {
            // Arrange
            PetType expectedTypeFromNumber = PetType.CAT; // CAT = 2
            PetStatus expectedStatusFromNumber = PetStatus.AVAILABLE; // AVAILABLE = 1

            // Act
            PetType actualTypeFromNumber = readValue("2", PetType.class);
            PetStatus actualStatusFromNumber = readValue("1", PetStatus.class);

            // Assert
            assertThat(actualTypeFromNumber).isEqualTo(expectedTypeFromNumber);
            assertThat(actualStatusFromNumber).isEqualTo(expectedStatusFromNumber);
        }

        @Test
        @DisplayName("Should handle unknown enum values as UNRECOGNIZED")
        void shouldHandleUnknownEnumValues() {
            // Arrange
            PetType expectedUnknownType = PetType.UNRECOGNIZED;
            PetStatus expectedUnknownStatus = PetStatus.UNRECOGNIZED;

            // Act
            PetType actualUnknownType = readValue("999", PetType.class);
            PetStatus actualUnknownStatus = readValue("\"UNKNOWN_STATUS\"", PetStatus.class);

            // Assert
            assertThat(actualUnknownType).isEqualTo(expectedUnknownType);
            assertThat(actualUnknownStatus).isEqualTo(expectedUnknownStatus);
        }
    }

    @Nested
    @DisplayName("Round Trip Consistency Tests")
    class RoundTripTests {

        @Test
        @DisplayName("Should maintain data integrity through serialize-deserialize cycle")
        void shouldMaintainDataIntegrityThroughRoundTrip() {
            // Arrange
            Pet originalPet = Pet.newBuilder()
                    .setId("round-trip-test")
                    .setName("Consistency Pet")
                    .setType(PetType.BIRD)
                    .setStatus(PetStatus.SOLD)
                    .addAllTags(List.of("test", "round-trip"))
                    .putAllMetadata(Map.of("test", "value"))
                    .build();

            String expectedJson =
                    "{\"id\":\"round-trip-test\",\"name\":\"Consistency Pet\",\"type\":\"BIRD\",\"status\":\"SOLD\",\"tags\":[\"test\",\"round-trip\"],\"metadata\":{\"test\":\"value\"},\"previousAddresses\":[]}";

            // Act
            String actualJson = writeValueAsString(originalPet);
            Pet actualDeserializedPet = readValue(actualJson, Pet.class);

            // Assert
            assertThat(actualJson).isEqualTo(expectedJson);
            assertThat(actualDeserializedPet).isEqualTo(originalPet);
        }
    }

    @Nested
    @DisplayName("Module Configuration Tests")
    class ModuleConfigurationTests {

        @Test
        @DisplayName("Should work correctly with ProtobufModule registered")
        void shouldWorkWithProtobufModuleRegistered() {
            // Arrange
            Pet pet = Pet.newBuilder()
                    .setId("config-test")
                    .setName("Module Test Pet")
                    .setType(PetType.FISH)
                    .setStatus(PetStatus.AVAILABLE)
                    .build();

            String expectedJson =
                    "{\"id\":\"config-test\",\"name\":\"Module Test Pet\",\"type\":\"FISH\",\"status\":\"AVAILABLE\",\"tags\":[],\"metadata\":{},\"previousAddresses\":[]}";

            // Act
            String actualJson = writeValueAsString(pet);
            Pet actualDeserializedPet = readValue(actualJson, Pet.class);

            // Assert
            assertThat(actualJson).isEqualTo(expectedJson);
            assertThat(actualDeserializedPet).isEqualTo(pet);
        }

        @Test
        @DisplayName("Should fail without ProtobufModule for protobuf types")
        void shouldFailWithoutProtobufModuleForProtobufTypes() {
            // Arrange
            ObjectMapper mapperWithoutModule = new ObjectMapper();
            Pet pet = Pet.newBuilder()
                    .setId("fail-test")
                    .setName("Should Fail")
                    .setType(PetType.DOG)
                    .setStatus(PetStatus.AVAILABLE)
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> mapperWithoutModule.writeValueAsString(pet))
                    .isInstanceOf(Exception.class);
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
}
