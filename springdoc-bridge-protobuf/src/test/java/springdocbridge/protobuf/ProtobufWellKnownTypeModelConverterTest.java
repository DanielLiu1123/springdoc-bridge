package springdocbridge.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int32Value;
import com.google.protobuf.ListValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springdoc.core.providers.ObjectMapperProvider;
import user.v1.MapTestMessage;
import user.v1.User;

@DisplayName("ProtobufWellKnownTypeModelConverter Tests")
class ProtobufWellKnownTypeModelConverterTest {

    @Nested
    @DisplayName("Well-Known Types Schema Tests")
    class WellKnownTypesSchemaTests {

        @Test
        @DisplayName("Should convert Timestamp to date-time string schema")
        void shouldConvertTimestampToDateTimeStringSchema() {
            var schema = resolve(Timestamp.class);

            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getFormat()).isEqualTo("date-time");
        }

        @Test
        @DisplayName("Should convert Duration to string schema with pattern")
        void shouldConvertDurationToStringSchemaWithPattern() {
            var schema = resolve(Duration.class);

            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getPattern()).isEqualTo("^-?\\d+(\\.\\d+)?s$");
        }

        @Test
        @DisplayName("Should convert BoolValue to boolean schema")
        void shouldConvertBoolValueToBooleanSchema() {
            var schema = resolve(BoolValue.class);

            assertThat(schema).isInstanceOf(BooleanSchema.class);
        }

        @Test
        @DisplayName("Should convert Int32Value to integer schema")
        void shouldConvertInt32ValueToIntegerSchema() {
            var schema = resolve(Int32Value.class);

            assertThat(schema).isInstanceOf(IntegerSchema.class);
            assertThat(schema.getType()).isEqualTo("integer");
            assertThat(schema.getFormat()).isEqualTo("int32");
        }

        @Test
        @DisplayName("Should convert StringValue to string schema")
        void shouldConvertStringValueToStringSchema() {
            var schema = resolve(StringValue.class);

            assertThat(schema).isInstanceOf(StringSchema.class);
        }
    }

    @Nested
    @DisplayName("Special Types Schema Tests")
    class SpecialTypesSchemaTests {

        @Test
        @DisplayName("Should convert Any to object schema with @type field")
        void shouldConvertAnyToObjectSchemaWithTypeField() {
            var schema = resolve(Any.class);

            assertThat(schema).isInstanceOf(ObjectSchema.class);
            var objectSchema = (ObjectSchema) schema;
            assertThat(objectSchema.getProperties()).containsKey("@type");
            assertThat(objectSchema.getAdditionalProperties()).isEqualTo(true);
        }

        @Test
        @DisplayName("Should convert Struct to object schema with additional properties")
        void shouldConvertStructToObjectSchemaWithAdditionalProperties() {
            var schema = resolve(Struct.class);

            assertThat(schema).isInstanceOf(ObjectSchema.class);
            var objectSchema = (ObjectSchema) schema;
            assertThat(objectSchema.getAdditionalProperties()).isEqualTo(true);
        }

        @Test
        @DisplayName("Should convert ListValue to array schema")
        void shouldConvertListValueToArraySchema() {
            var schema = resolve(ListValue.class);

            assertThat(schema).isInstanceOf(ArraySchema.class);
            var arraySchema = (ArraySchema) schema;
            assertThat(arraySchema.getItems()).isNotNull();
        }

        @Test
        @DisplayName("Should convert FieldMask to string schema")
        void shouldConvertFieldMaskToStringSchema() {
            var schema = resolve(FieldMask.class);

            assertThat(schema).isInstanceOf(StringSchema.class);
        }

        @Test
        @DisplayName("Should convert Empty to empty object schema")
        void shouldConvertEmptyToEmptyObjectSchema() {
            var schema = resolve(Empty.class);

            assertThat(schema).isInstanceOf(ObjectSchema.class);
        }

        @Test
        @DisplayName("Should convert ByteString to base64 string schema")
        void shouldConvertByteStringToBase64StringSchema() {
            var schema = resolve(ByteString.class);

            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getFormat()).isEqualTo("byte");
        }
    }

    @Nested
    @DisplayName("Protobuf Enum Schema Tests")
    class ProtobufEnumSchemaTests {

        @Test
        @DisplayName("Should convert protobuf enum to $ref schema")
        void shouldConvertProtobufEnumToRefSchema() {
            // Given
            var schema = resolve(User.Status.class);

            // Then
            assertThat(schema).isNotNull();
            assertThat(schema.get$ref()).isEqualTo("#/components/schemas/user.v1.User.Status");
        }
    }

    @Nested
    @DisplayName("Protobuf repeated fields tests")
    class ProtobufRepeatedFieldsTests {

        @Test
        @DisplayName("Should convert repeated string field to array schema")
        void shouldConvertRepeatedStringFieldToArraySchema() {
            var schema = resolve(User.class);

            var tagsSchema = schema.getProperties().get("tags");
            assertThat(tagsSchema).isInstanceOf(ArraySchema.class);
            var arraySchema = (ArraySchema) tagsSchema;
            assertThat(arraySchema.getItems()).isInstanceOf(StringSchema.class);
            assertThat(arraySchema.getProperties()).isNull();
        }

        @Test
        @DisplayName("Should convert repeated int field to array schema")
        void shouldConvertRepeatedIntFieldToArraySchema() {
            var schema = resolve(User.class);

            var ageSchema = schema.getProperties().get("tagIds");
            assertThat(ageSchema).isInstanceOf(ArraySchema.class);
            var arraySchema = (ArraySchema) ageSchema;
            assertThat(arraySchema.getItems()).isInstanceOf(IntegerSchema.class);
            assertThat(arraySchema.getProperties()).isNull();
        }

        @Test
        @DisplayName("Should convert repeated enum field to array schema")
        void shouldConvertRepeatedEnumFieldToArraySchema() {
            var schema = resolve(User.class);

            var statusHistorySchema = schema.getProperties().get("statusHistory");
            assertThat(statusHistorySchema).isInstanceOf(ArraySchema.class);
            var arraySchema = (ArraySchema) statusHistorySchema;
            assertThat(arraySchema.getItems().get$ref()).isEqualTo("#/components/schemas/user.v1.User.Status");
            assertThat(arraySchema.getProperties()).isNull();
        }

        @Test
        @DisplayName("Should convert repeated message field to array schema")
        void shouldConvertRepeatedMessageFieldToArraySchema() {
            var schema = resolve(User.class);

            var phoneNumbersSchema = schema.getProperties().get("phoneNumbers");
            assertThat(phoneNumbersSchema).isInstanceOf(ArraySchema.class);
            var arraySchema = (ArraySchema) phoneNumbersSchema;
            assertThat(arraySchema.getItems().get$ref()).isNotNull();
            assertThat(arraySchema.getItems().getProperties()).isNull();
        }
    }

    @Nested
    @DisplayName("Protobuf map fields tests")
    class ProtobufMapFieldsTests {

        @Test
        @DisplayName("Should convert map with string values to object schema")
        void shouldConvertMapWithStringValuesToObjectSchema() {
            var schema = resolve(MapTestMessage.class);

            var metadataSchema = (Schema<?>) schema.getProperties().get("metadata");
            assertThat(metadataSchema.getAdditionalProperties()).isInstanceOf(Schema.class);
            var additionalPropertiesSchema = (Schema<?>) metadataSchema.getAdditionalProperties();
            assertThat(additionalPropertiesSchema.getType()).isEqualTo("string");
        }

        @Test
        @DisplayName("Should convert map with enum values to object schema")
        void shouldConvertMapWithEnumValuesToObjectSchema() {
            var schema = resolve(MapTestMessage.class);

            var statusMapSchema = (Schema<?>) schema.getProperties().get("statusMap");
            var additionalPropertiesSchema = (Schema<?>) statusMapSchema.getAdditionalProperties();
            assertThat(additionalPropertiesSchema.get$ref())
                    .isEqualTo("#/components/schemas/user.v1.MapTestMessage.Status");
        }

        @Test
        @DisplayName("Should convert map with message values to object schema")
        void shouldConvertMapWithMessageValuesToObjectSchema() {
            var schema = resolve(MapTestMessage.class);

            var addressMapSchema = (Schema<?>) schema.getProperties().get("addressMap");
            var additionalPropertiesSchema = (Schema<?>) addressMapSchema.getAdditionalProperties();
            assertThat(additionalPropertiesSchema.get$ref())
                    .isEqualTo("#/components/schemas/user.v1.MapTestMessage.Address");
        }

        @Test
        @DisplayName("Should convert map with int values to object schema")
        void showBeConvertMapWithIntValuesToObjectSchema() {
            var schema = resolve(MapTestMessage.class);

            var scoreMapSchema = (Schema<?>) schema.getProperties().get("scoreMap");
            var additionalPropertiesSchema = (Schema<?>) scoreMapSchema.getAdditionalProperties();
            assertThat(additionalPropertiesSchema.getType()).isEqualTo("integer");
            assertThat(additionalPropertiesSchema.getFormat()).isEqualTo("int32");
        }

        @Test
        @DisplayName("Should be deprecated when using [deprecated = true] in proto")
        void shouldBeDeprecatedWhenUsingDeprecatedInProto() {
            var schema = resolve(MapTestMessage.class);

            var metadataSchema = (Schema<?>) schema.getProperties().get("metadata");
            assertThat(metadataSchema.getDeprecated()).isNull();

            var deprecatedMapSchema = (Schema<?>) schema.getProperties().get("deprecatedMap");
            assertThat(deprecatedMapSchema.getDeprecated()).isTrue();
        }
    }

    private static Schema<?> resolve(Class<?> clazz) {
        return getModelConverterContext().resolve(new AnnotatedType(clazz));
    }

    private static ModelConverterContext getModelConverterContext() {

        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new ProtobufSchemaModule());

        var objectMapperProvider = mock(ObjectMapperProvider.class);
        when(objectMapperProvider.jsonMapper()).thenReturn(objectMapper);

        var modelConverters = ModelConverters.getInstance(true);
        modelConverters.addConverter(new ModelResolver(objectMapperProvider.jsonMapper()));
        modelConverters.addConverter(new ProtobufWellKnownTypeModelConverter(objectMapperProvider));

        return new ModelConverterContextImpl(modelConverters.getConverters());
    }
}
