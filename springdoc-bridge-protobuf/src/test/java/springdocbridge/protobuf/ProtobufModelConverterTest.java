package springdocbridge.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.schema.naming.v1.SchemaNamingTestMessage;
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
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.lang.reflect.Type;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import springdocbridge.protobuf.SpringDocBridgeProtobufProperties.SchemaNamingStrategy;
import types.v1.MapTestMessage;
import types.v1.OptionalTestMessage;
import user.v1.User;

@DisplayName("ProtobufWellKnownTypeModelConverter Tests")
class ProtobufModelConverterTest {

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
        @DisplayName("Should remove UNRECOGNIZED from enum values")
        void shouldRemoveUnrecognizedFromEnumValues() {
            // Given
            var schema = resolve(User.Status.class);

            // Then
            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getEnum()).doesNotContain("UNRECOGNIZED");
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
            assertThat(arraySchema.getItems()).isInstanceOf(JsonSchema.class);
            assertThat(arraySchema.getProperties()).isNull();
            assertThat(arraySchema.getItems().getTypes()).containsExactly("integer");
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
    @DisplayName("Required fields tests")
    class RequiredFieldsTests {
        @Test
        @DisplayName("Should mark required fields by default")
        void shouldMarkRequiredFieldsByDefault() {
            var schema = resolve(OptionalTestMessage.class);

            assertThat(schema.getRequired()).containsExactlyInAnyOrder("requiredString", "requiredMessage");
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
            assertThat(additionalPropertiesSchema.getTypes()).containsExactly("string");
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
            assertThat(additionalPropertiesSchema.getTypes()).containsExactly("integer");
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

    @Nested
    @DisplayName("Protobuf List Fields Tests")
    class ListFieldsTests {

        @Test
        @DisplayName("Return null when resolve List without schemaProperty")
        void nullWhenResolveListWithoutSchemaProperty() {

            // This is why we need to set schemaProperty to true
            // see springdocbridge.protobuf.ProtobufModelConverter.createSchemaForMessage

            // Given
            var type = ResolvableType.forType(new ParameterizedTypeReference<List<String>>() {})
                    .getType();

            // When
            var schema = resolveAnnotatedType(new AnnotatedType(type));

            // Then
            assertThat(schema).isNull();
        }

        @Test
        @DisplayName("Should resolve List when set schemaProperty to true")
        void shouldResolveListWhenSetSchemaPropertyToTrue() {
            // Given
            var type = ResolvableType.forType(new ParameterizedTypeReference<List<String>>() {})
                    .getType();

            // When
            var schema = resolveAnnotatedType(new AnnotatedType(type).schemaProperty(true));

            // Then
            assertThat(schema).isInstanceOf(ArraySchema.class);
            var arraySchema = (ArraySchema) schema;
            assertThat(arraySchema.getItems()).isInstanceOf(JsonSchema.class);
            assertThat(arraySchema.getItems().getTypes()).containsExactly("string");
        }
    }

    @Nested
    @DisplayName("Schema Naming Strategy Tests")
    class SchemaNamingStrategyTests {

        @Test
        @DisplayName("Should use simple name when using Springdoc naming strategy and not using FQN")
        void shouldUseSimpleNameWhenUsingSpringdocNamingStrategyAndNotUsingFqn() {
            // Arrange
            var context = getModelConverterContext(SchemaNamingStrategy.SPRINGDOC, false);

            // Act
            context.resolve(new AnnotatedType(SchemaNamingTestMessage.class));

            // Then
            assertThat(context.getDefinedModels()).containsKey("SchemaNamingTestMessage");
        }

        @Test
        @DisplayName("Should use FQN when using Springdoc naming strategy and using FQN")
        void shouldUseFqnWhenUsingSpringdocNamingStrategyAndUsingFqn() {
            // Arrange
            var context = getModelConverterContext(SchemaNamingStrategy.SPRINGDOC, true);

            // Act
            context.resolve(new AnnotatedType(SchemaNamingTestMessage.class));

            // Then
            assertThat(context.getDefinedModels()).containsKey("com.example.schema.naming.v1.SchemaNamingTestMessage");
        }

        @Test
        @DisplayName("Should use protobuf name when using Protobuf naming strategy")
        void shouldUseProtobufNameWhenUsingProtobufNamingStrategy() {
            // Arrange
            var context = getModelConverterContext(SchemaNamingStrategy.PROTOBUF, true);

            // Act
            context.resolve(new AnnotatedType(SchemaNamingTestMessage.class));

            // Assert
            assertThat(context.getDefinedModels()).containsKey("schema_naming.v1.SchemaNamingTestMessage");
        }
    }

    private static Schema<?> resolve(Type type) {
        return resolveAnnotatedType(new AnnotatedType(type));
    }

    private static Schema<?> resolveAnnotatedType(AnnotatedType annotatedType) {
        var context = getModelConverterContext();
        return resolveAnnotatedType(context, annotatedType);
    }

    private static Schema<?> resolveAnnotatedType(ModelConverterContext context, AnnotatedType annotatedType) {
        var schema = context.resolve(annotatedType);
        if (schema == null) {
            return null;
        }
        if (schema.get$ref() != null) {
            var schemaName = schema.get$ref().substring(Components.COMPONENTS_SCHEMAS_REF.length());
            return context.getDefinedModels().get(schemaName);
        }
        return schema;
    }

    private static ModelConverterContext getModelConverterContext() {
        return getModelConverterContext(SchemaNamingStrategy.SPRINGDOC, true);
    }

    private static ModelConverterContext getModelConverterContext(
            SchemaNamingStrategy schemaNamingStrategy, boolean useFqn) {

        var objectMapper = new ObjectMapper();

        var objectMapperProvider = mock(ObjectMapperProvider.class);
        when(objectMapperProvider.jsonMapper()).thenReturn(objectMapper);

        var modelConverters = ModelConverters.getInstance(true);
        modelConverters.addConverter(new ProtobufModelConverter(
                objectMapperProvider, new ProtobufNameResolver(schemaNamingStrategy, useFqn)));

        return new ModelConverterContextImpl(modelConverters.getConverters());
    }
}
