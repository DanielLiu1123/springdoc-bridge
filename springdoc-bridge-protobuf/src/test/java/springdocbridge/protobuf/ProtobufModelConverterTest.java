package springdocbridge.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.schema.naming.v1.SchemaNamingTestMessage;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import springdocbridge.protobuf.SpringDocBridgeProtobufProperties.SchemaNamingStrategy;
import types.v1.DeprecatedTestMessage;
import types.v1.EnumTestMessage;
import types.v1.FieldNamingTestMessage;
import types.v1.MapTestMessage;
import types.v1.OneofTestMessage;
import types.v1.OptionalTestMessage;
import types.v1.RepeatedTestMessage;

class ProtobufModelConverterTest {

    @Nested
    class WellKnownTypesSchemaTests {

        @Test
        void shouldConvertTimestampToDateTimeStringSchema() {
            var schema = resolve(Timestamp.class);

            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getFormat()).isEqualTo("date-time");
        }

        @Test
        void shouldConvertDurationToStringSchemaWithPattern() {
            var schema = resolve(Duration.class);

            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getPattern()).isEqualTo("^-?\\d+(\\.\\d+)?s$");
        }

        @Test
        void shouldConvertBoolValueToBooleanSchema() {
            var schema = resolve(BoolValue.class);

            assertThat(schema).isInstanceOf(BooleanSchema.class);
        }

        @Test
        void shouldConvertInt32ValueToIntegerSchema() {
            var schema = resolve(Int32Value.class);

            assertThat(schema).isInstanceOf(IntegerSchema.class);
            assertThat(schema.getType()).isEqualTo("integer");
            assertThat(schema.getFormat()).isEqualTo("int32");
        }

        @Test
        void shouldConvertStringValueToStringSchema() {
            var schema = resolve(StringValue.class);

            assertThat(schema).isInstanceOf(StringSchema.class);
        }
    }

    @Nested
    class SpecialTypesSchemaTests {

        @Test
        void shouldConvertAnyToObjectSchemaWithTypeField() {
            var schema = resolve(Any.class);

            assertThat(schema).isInstanceOf(ObjectSchema.class);
            var objectSchema = (ObjectSchema) schema;
            assertThat(objectSchema.getProperties()).containsKey("@type");
            assertThat(objectSchema.getAdditionalProperties()).isEqualTo(true);
        }

        @Test
        void shouldConvertStructToObjectSchemaWithAdditionalProperties() {
            var schema = resolve(Struct.class);

            assertThat(schema).isInstanceOf(ObjectSchema.class);
            var objectSchema = (ObjectSchema) schema;
            assertThat(objectSchema.getAdditionalProperties()).isEqualTo(true);
        }

        @Test
        void shouldConvertListValueToArraySchema() {
            var schema = resolve(ListValue.class);

            assertThat(schema).isInstanceOf(ArraySchema.class);
            var arraySchema = (ArraySchema) schema;
            assertThat(arraySchema.getItems()).isNotNull();
        }

        @Test
        void shouldConvertFieldMaskToStringSchema() {
            var schema = resolve(FieldMask.class);

            assertThat(schema).isInstanceOf(StringSchema.class);
        }

        @Test
        void shouldConvertEmptyToEmptyObjectSchema() {
            var schema = resolve(Empty.class);

            assertThat(schema).isInstanceOf(ObjectSchema.class);
        }

        @Test
        void shouldConvertByteStringToBase64StringSchema() {
            var schema = resolve(ByteString.class);

            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getFormat()).isEqualTo("byte");
        }
    }

    @Nested
    class ProtobufEnumSchemaTests {

        @Test
        void shouldRemoveUnrecognizedFromEnumValues() {
            // Given
            var schema = resolve(EnumTestMessage.Enum.class);

            // Then
            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getEnum()).containsExactly("ENUM_UNSPECIFIED", "VALUE_1");
        }
    }

    @Nested
    class ProtobufRepeatedFieldsTests {

        @Test
        void shouldConvertRepeatedStringFieldToArraySchema() {
            var schema = resolve(RepeatedTestMessage.class);

            var repeatedStringSchema = (ArraySchema) schema.getProperties().get("repeatedString");
            assertThat(repeatedStringSchema.getItems().getTypes()).containsExactly("string");

            var repeatedIntSchema = (ArraySchema) schema.getProperties().get("repeatedInt");
            assertThat(repeatedIntSchema.getItems().getTypes()).containsExactly("integer");

            var repeatedMessageSchema = (ArraySchema) schema.getProperties().get("repeatedMessage");
            assertThat(repeatedMessageSchema.getItems().get$ref())
                    .isEqualTo("#/components/schemas/types.v1.RepeatedTestMessage.Message");

            var repeatedEnumSchema = (ArraySchema) schema.getProperties().get("repeatedEnum");
            assertThat(repeatedEnumSchema.getItems().get$ref())
                    .isEqualTo("#/components/schemas/types.v1.RepeatedTestMessage.Enum");
        }
    }

    @Nested
    class RequiredFieldsTests {
        @Test
        void shouldMarkRequiredFieldsByDefault() {
            var schema = resolve(OptionalTestMessage.class);

            assertThat(schema.getRequired()).containsExactlyInAnyOrder("requiredString", "requiredMessage");
        }

        @Test
        void shouldMarkOneofFieldsAsOptional() {
            var schema = resolve(OneofTestMessage.class);

            // oneof fields should NOT be in required list
            var required = schema.getRequired();
            assertThat(required).isNotNull().doesNotContain("referralCode", "promoCode", "source1", "source2");

            // Verify the properties exist
            assertThat(schema.getProperties()).containsKeys("referralCode", "promoCode", "source1", "source2");
        }
    }

    @Nested
    class FieldNamingTests {

        // https://github.com/DanielLiu1123/springdoc-bridge/issues/21
        @Test
        void shouldResolveFieldSchemasWhenLetterFollowsDigit() {
            var schema = resolve(FieldNamingTestMessage.class);

            // Protobuf capitalizes the letter after a digit for the generated getter
            // (a1b64 -> getA1B64), which previously broke schema resolution and fell back to string.
            assertThat(schema.getProperties()).containsKeys("a1b64", "a1b48", "my2ndValue", "normalField");
            assertThat(schema.getProperties().get("a1b64").getTypes()).containsExactly("string");
            assertThat(schema.getProperties().get("a1b48").getTypes()).containsExactly("string");
            assertThat(schema.getProperties().get("my2ndValue").getTypes()).containsExactly("integer");
            assertThat(schema.getProperties().get("normalField").getTypes()).containsExactly("string");
        }
    }

    @Nested
    class DeprecatedFieldsTests {
        @Test
        void shouldMarkDeprecatedFields() {
            // Given
            @SuppressWarnings("deprecation")
            var schema = resolve(DeprecatedTestMessage.class);

            // Then
            assertThat(schema.getDeprecated()).isTrue();
            assertThat(schema.getProperties().get("deprecatedString").getDeprecated())
                    .isTrue();
            assertThat(schema.getProperties().get("notDeprecatedString").getDeprecated())
                    .isNull();
        }
    }

    @Nested
    class ProtobufMapFieldsTests {

        @Test
        void shouldConvertMapWithStringValuesToObjectSchema() {
            var schema = resolve(MapTestMessage.class);

            var metadataSchema = (Schema<?>) schema.getProperties().get("metadata");
            assertThat(metadataSchema.getAdditionalProperties()).isInstanceOf(Schema.class);
            var additionalPropertiesSchema = (Schema<?>) metadataSchema.getAdditionalProperties();
            assertThat(additionalPropertiesSchema.getTypes()).containsExactly("string");
        }

        @Test
        void shouldConvertMapWithEnumValuesToObjectSchema() {
            var schema = resolve(MapTestMessage.class);

            var statusMapSchema = (Schema<?>) schema.getProperties().get("statusMap");
            var additionalPropertiesSchema = (Schema<?>) statusMapSchema.getAdditionalProperties();
            assertThat(additionalPropertiesSchema.get$ref())
                    .isEqualTo("#/components/schemas/types.v1.MapTestMessage.Status");
        }

        @Test
        void shouldConvertMapWithMessageValuesToObjectSchema() {
            var schema = resolve(MapTestMessage.class);

            var addressMapSchema = (Schema<?>) schema.getProperties().get("addressMap");
            var additionalPropertiesSchema = (Schema<?>) addressMapSchema.getAdditionalProperties();
            assertThat(additionalPropertiesSchema.get$ref())
                    .isEqualTo("#/components/schemas/types.v1.MapTestMessage.Address");
        }

        @Test
        void showBeConvertMapWithIntValuesToObjectSchema() {
            var schema = resolve(MapTestMessage.class);

            var scoreMapSchema = (Schema<?>) schema.getProperties().get("scoreMap");
            var additionalPropertiesSchema = (Schema<?>) scoreMapSchema.getAdditionalProperties();
            assertThat(additionalPropertiesSchema.getTypes()).containsExactly("integer");
            assertThat(additionalPropertiesSchema.getFormat()).isEqualTo("int32");
        }

        @Test
        void shouldBeDeprecatedWhenUsingDeprecatedInProto() {
            var schema = resolve(MapTestMessage.class);

            var metadataSchema = (Schema<?>) schema.getProperties().get("metadata");
            assertThat(metadataSchema.getDeprecated()).isNull();

            var deprecatedMapSchema = (Schema<?>) schema.getProperties().get("deprecatedMap");
            assertThat(deprecatedMapSchema.getDeprecated()).isTrue();
        }
    }

    @Nested
    class ResolveListTests {

        @Test
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
    class SchemaNamingStrategyTests {

        @Test
        void shouldUseSimpleNameWhenUsingSpringdocNamingStrategyAndNotUsingFqn() {
            // Arrange
            var context = getModelConverterContext(SchemaNamingStrategy.SPRINGDOC, false);

            // Act
            context.resolve(new AnnotatedType(SchemaNamingTestMessage.class));

            // Then
            assertThat(context.getDefinedModels()).containsKey("SchemaNamingTestMessage");
        }

        @Test
        void shouldUseFqnWhenUsingSpringdocNamingStrategyAndUsingFqn() {
            // Arrange
            var context = getModelConverterContext(SchemaNamingStrategy.SPRINGDOC, true);

            // Act
            context.resolve(new AnnotatedType(SchemaNamingTestMessage.class));

            // Then
            assertThat(context.getDefinedModels()).containsKey("com.example.schema.naming.v1.SchemaNamingTestMessage");
        }

        @Test
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

        var jsonMapper = JsonMapper.builder().build();

        var objectMapperProvider = mock(ObjectMapperProvider.class);
        when(objectMapperProvider.jsonMapper()).thenReturn(jsonMapper);

        var modelConverters = ModelConverters.getInstance(true);
        modelConverters.addConverter(new ProtobufModelConverter(
                objectMapperProvider, new ProtobufNameResolver(schemaNamingStrategy, useFqn)));

        return new ModelConverterContextImpl(modelConverters.getConverters());
    }
}
