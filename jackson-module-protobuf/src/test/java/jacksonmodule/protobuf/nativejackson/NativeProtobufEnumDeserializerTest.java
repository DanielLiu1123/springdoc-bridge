package jacksonmodule.protobuf.nativejackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.protobuf.NullValue;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import pet.v1.PetStatus;
import pet.v1.PetType;

@DisplayName("NativeProtobufEnumDeserializer Tests")
@MockitoSettings
class NativeProtobufEnumDeserializerTest {

    @Mock
    private JsonParser jsonParser;

    @Mock
    private DeserializationContext deserializationContext;

    @Nested
    @DisplayName("Deserialize From String Tests")
    class DeserializeFromStringTests {

        @ParameterizedTest
        @ValueSource(strings = {"DOG", "CAT", "BIRD", "FISH"})
        void shouldDeserializeValidEnumFromString(String enumName) throws IOException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(PetType.class);
            var textNode = new TextNode(enumName);
            when(jsonParser.readValueAsTree()).thenReturn(textNode);

            // Act
            var actual = deserializer.deserialize(jsonParser, deserializationContext);

            // Assert
            var expected = PetType.valueOf(enumName);
            assertThat(actual).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(strings = {"AVAILABLE", "PENDING", "SOLD"})
        void shouldDeserializeValidPetStatusFromString(String statusName) throws IOException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(PetStatus.class);
            var textNode = new TextNode(statusName);
            when(jsonParser.readValueAsTree()).thenReturn(textNode);

            // Act
            var actual = deserializer.deserialize(jsonParser, deserializationContext);

            // Assert
            var expected = PetStatus.valueOf(statusName);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldReturnUnrecognizedForInvalidEnumString() throws IOException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(PetType.class);
            var textNode = new TextNode("INVALID_TYPE");
            when(jsonParser.readValueAsTree()).thenReturn(textNode);

            // Act
            var actual = deserializer.deserialize(jsonParser, deserializationContext);

            // Assert
            var expected = PetType.UNRECOGNIZED;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldHandleCaseInsensitiveEnumNames() throws IOException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(PetType.class);
            var textNode = new TextNode("cat"); // lowercase
            when(jsonParser.readValueAsTree()).thenReturn(textNode);

            // Act
            var actual = deserializer.deserialize(jsonParser, deserializationContext);

            // Assert
            var expected = PetType.CAT;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Deserialize From Number Tests")
    class DeserializeFromNumberTests {

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4})
        void shouldDeserializeValidEnumFromNumber(int enumValue) throws IOException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(PetType.class);
            var intNode = new IntNode(enumValue);
            when(jsonParser.readValueAsTree()).thenReturn(intNode);

            // Act
            var actual = deserializer.deserialize(jsonParser, deserializationContext);

            // Assert
            var expected = PetType.forNumber(enumValue);
            assertThat(actual).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3})
        void shouldDeserializeValidPetStatusFromNumber(int statusValue) throws IOException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(PetStatus.class);
            var intNode = new IntNode(statusValue);
            when(jsonParser.readValueAsTree()).thenReturn(intNode);

            // Act
            var actual = deserializer.deserialize(jsonParser, deserializationContext);

            // Assert
            var expected = PetStatus.forNumber(statusValue);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldReturnUnrecognizedForInvalidEnumNumber() throws IOException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(PetType.class);
            var intNode = new IntNode(999); // Invalid enum value
            when(jsonParser.readValueAsTree()).thenReturn(intNode);

            // Act
            var actual = deserializer.deserialize(jsonParser, deserializationContext);

            // Assert
            var expected = PetType.UNRECOGNIZED;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldDeserializeZeroAsUnspecified() throws IOException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(PetType.class);
            var intNode = new IntNode(0);
            when(jsonParser.readValueAsTree()).thenReturn(intNode);

            // Act
            var actual = deserializer.deserialize(jsonParser, deserializationContext);

            // Assert
            var expected = PetType.PET_TYPE_UNSPECIFIED;
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("GetNullValue Tests")
    class GetNullValueTests {

        @Test
        void shouldReturnNullValueForNullValueEnum() throws JsonMappingException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(NullValue.class);

            // Act
            var actual = deserializer.getNullValue(deserializationContext);

            // Assert
            var expected = NullValue.NULL_VALUE;
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void shouldReturnSuperNullValueForRegularEnum() throws JsonMappingException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(PetType.class);

            // Act & Assert
            assertThatThrownBy(() -> deserializer.getNullValue(deserializationContext))
                    .isInstanceOf(JsonMappingException.class);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        void shouldThrowExceptionForNonValueNode() throws IOException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(PetType.class);
            var objectNode = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            when(jsonParser.readValueAsTree()).thenReturn(objectNode);

            // Act & Assert
            assertThatThrownBy(() -> deserializer.deserialize(jsonParser, deserializationContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Can't deserialize protobuf enum 'PetType' from");
        }

        @Test
        void shouldThrowExceptionForArrayNode() throws IOException {
            // Arrange
            var deserializer = new NativeProtobufEnumDeserializer(PetType.class);
            var arrayNode = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
            when(jsonParser.readValueAsTree()).thenReturn(arrayNode);

            // Act & Assert
            assertThatThrownBy(() -> deserializer.deserialize(jsonParser, deserializationContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Can't deserialize protobuf enum 'PetType' from");
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        void shouldCreateDeserializerForValidEnumClass() {
            // Act
            var actual = new NativeProtobufEnumDeserializer(PetType.class);

            // Assert
            assertThat(actual).isNotNull();
        }

        @Test
        void shouldCreateDeserializerForPetStatusClass() {
            // Act
            var actual = new NativeProtobufEnumDeserializer(PetStatus.class);

            // Assert
            assertThat(actual).isNotNull();
        }

        @Test
        void shouldThrowExceptionForNonEnumClass() {
            // Act & Assert
            assertThatThrownBy(() -> new NativeProtobufEnumDeserializer(String.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("is not an enum");
        }

        @Test
        void shouldThrowExceptionForEnumWithoutUnrecognized() {
            // Arrange
            enum TestEnum {
                VALUE1,
                VALUE2
            } // No UNRECOGNIZED

            // Act & Assert
            assertThatThrownBy(() -> new NativeProtobufEnumDeserializer(TestEnum.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No UNRECOGNIZED enum constant found");
        }
    }
}
