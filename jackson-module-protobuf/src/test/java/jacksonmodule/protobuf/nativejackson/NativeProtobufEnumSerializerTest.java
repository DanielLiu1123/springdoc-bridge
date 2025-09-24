package jacksonmodule.protobuf.nativejackson;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import pet.v1.PetStatus;
import pet.v1.PetType;

@DisplayName("NativeProtobufEnumSerializer Tests")
@MockitoSettings
class NativeProtobufEnumSerializerTest {

    @Mock
    private JsonGenerator jsonGenerator;

    @Mock
    private SerializerProvider serializerProvider;

    @Nested
    @DisplayName("Serialize Tests")
    class SerializeTests {

        @ParameterizedTest
        @EnumSource(PetType.class)
        void shouldSerializeEnumAsStringByDefault(PetType petType) throws IOException {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.DEFAULT;
            var serializer = new NativeProtobufEnumSerializer(options);

            // Act
            serializer.serialize(petType, jsonGenerator, serializerProvider);

            // Assert
            var expected = petType.name();
            verify(jsonGenerator).writeString(expected);
        }

        @ParameterizedTest
        @EnumSource(
                value = PetStatus.class,
                names = {"UNRECOGNIZED"},
                mode = EnumSource.Mode.EXCLUDE)
        void shouldSerializeEnumAsIntegerWhenConfigured(PetStatus petStatus) throws IOException {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.builder()
                    .serializeEnumAsInt(true)
                    .build();
            var serializer = new NativeProtobufEnumSerializer(options);

            // Act
            serializer.serialize(petStatus, jsonGenerator, serializerProvider);

            // Assert
            var expected = petStatus.getNumber();
            verify(jsonGenerator).writeNumber(expected);
        }

        @Test
        void shouldThrowExceptionWhenSerializingUnrecognizedEnumAsInteger() {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.builder()
                    .serializeEnumAsInt(true)
                    .build();
            var serializer = new NativeProtobufEnumSerializer(options);

            // Act & Assert
            assertThatThrownBy(() -> serializer.serialize(PetStatus.UNRECOGNIZED, jsonGenerator, serializerProvider))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot serialize UNRECOGNIZED enum value as integer");
        }

        @Test
        void shouldSerializePetTypeAsString() throws IOException {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.DEFAULT;
            var serializer = new NativeProtobufEnumSerializer(options);
            var petType = PetType.CAT;

            // Act
            serializer.serialize(petType, jsonGenerator, serializerProvider);

            // Assert
            var expected = "CAT";
            verify(jsonGenerator).writeString(expected);
        }

        @Test
        void shouldSerializePetTypeAsInteger() throws IOException {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.builder()
                    .serializeEnumAsInt(true)
                    .build();
            var serializer = new NativeProtobufEnumSerializer(options);
            var petType = PetType.CAT;

            // Act
            serializer.serialize(petType, jsonGenerator, serializerProvider);

            // Assert
            var expected = 2; // CAT = 2
            verify(jsonGenerator).writeNumber(expected);
        }

        @Test
        void shouldSerializePetStatusAsString() throws IOException {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.DEFAULT;
            var serializer = new NativeProtobufEnumSerializer(options);
            var petStatus = PetStatus.PENDING;

            // Act
            serializer.serialize(petStatus, jsonGenerator, serializerProvider);

            // Assert
            var expected = "PENDING";
            verify(jsonGenerator).writeString(expected);
        }

        @Test
        void shouldSerializePetStatusAsInteger() throws IOException {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.builder()
                    .serializeEnumAsInt(true)
                    .build();
            var serializer = new NativeProtobufEnumSerializer(options);
            var petStatus = PetStatus.PENDING;

            // Act
            serializer.serialize(petStatus, jsonGenerator, serializerProvider);

            // Assert
            var expected = 2; // PENDING = 2
            verify(jsonGenerator).writeNumber(expected);
        }

        @Test
        void shouldSerializeUnspecifiedEnumAsString() throws IOException {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.DEFAULT;
            var serializer = new NativeProtobufEnumSerializer(options);
            var petType = PetType.PET_TYPE_UNSPECIFIED;

            // Act
            serializer.serialize(petType, jsonGenerator, serializerProvider);

            // Assert
            var expected = "PET_TYPE_UNSPECIFIED";
            verify(jsonGenerator).writeString(expected);
        }

        @Test
        void shouldSerializeUnspecifiedEnumAsInteger() throws IOException {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.builder()
                    .serializeEnumAsInt(true)
                    .build();
            var serializer = new NativeProtobufEnumSerializer(options);
            var petType = PetType.PET_TYPE_UNSPECIFIED;

            // Act
            serializer.serialize(petType, jsonGenerator, serializerProvider);

            // Assert
            var expected = 0; // PET_TYPE_UNSPECIFIED = 0
            verify(jsonGenerator).writeNumber(expected);
        }
    }

    @Nested
    @DisplayName("Options Tests")
    class OptionsTests {

        @Test
        void shouldUseDefaultOptionsWhenSerializeEnumAsIntIsFalse() throws IOException {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.builder()
                    .serializeEnumAsInt(false)
                    .build();
            var serializer = new NativeProtobufEnumSerializer(options);
            var petType = PetType.DOG;

            // Act
            serializer.serialize(petType, jsonGenerator, serializerProvider);

            // Assert
            var expected = "DOG";
            verify(jsonGenerator).writeString(expected);
        }

        @Test
        void shouldUseIntegerSerializationWhenSerializeEnumAsIntIsTrue() throws IOException {
            // Arrange
            var options = NativeJacksonProtobufModule.Options.builder()
                    .serializeEnumAsInt(true)
                    .build();
            var serializer = new NativeProtobufEnumSerializer(options);
            var petType = PetType.DOG;

            // Act
            serializer.serialize(petType, jsonGenerator, serializerProvider);

            // Assert
            var expected = 1; // DOG = 1
            verify(jsonGenerator).writeNumber(expected);
        }
    }
}
