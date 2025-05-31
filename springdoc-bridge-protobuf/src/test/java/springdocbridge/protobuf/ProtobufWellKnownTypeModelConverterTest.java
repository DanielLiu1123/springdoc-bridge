package springdocbridge.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
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
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springdoc.core.providers.ObjectMapperProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProtobufWellKnownTypeModelConverter Tests")
class ProtobufWellKnownTypeModelConverterTest {

    @Mock
    private ObjectMapperProvider objectMapperProvider;

    @Mock
    private ModelConverterContext context;

    @Mock
    private Iterator<ModelConverter> chain;

    private ProtobufWellKnownTypeModelConverter converter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        when(objectMapperProvider.jsonMapper()).thenReturn(objectMapper);
        converter = new ProtobufWellKnownTypeModelConverter(objectMapperProvider);
    }

    @Nested
    @DisplayName("Well-Known Types Schema Tests")
    class WellKnownTypesSchemaTests {

        @Test
        @DisplayName("Should convert Timestamp to date-time string schema")
        void shouldConvertTimestampToDateTimeStringSchema() {
            var annotatedType = createAnnotatedType(Timestamp.class);

            var schema = converter.resolve(annotatedType, context, chain);

            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getFormat()).isEqualTo("date-time");
            assertThat(stringSchema.getExample()).isEqualTo("1970-01-01T00:00:00Z");
        }

        @Test
        @DisplayName("Should convert Duration to string schema with pattern")
        void shouldConvertDurationToStringSchemaWithPattern() {
            var annotatedType = createAnnotatedType(Duration.class);

            var schema = converter.resolve(annotatedType, context, chain);

            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getPattern()).isEqualTo("^-?\\d+(\\.\\d+)?s$");
            assertThat(stringSchema.getExample()).isEqualTo("1.000340012s");
        }

        @Test
        @DisplayName("Should convert BoolValue to nullable boolean schema")
        void shouldConvertBoolValueToNullableBooleanSchema() {
            var annotatedType = createAnnotatedType(BoolValue.class);

            var schema = converter.resolve(annotatedType, context, chain);

            assertThat(schema).isInstanceOf(BooleanSchema.class);
            var booleanSchema = (BooleanSchema) schema;
            assertThat(booleanSchema.getNullable()).isTrue();
            assertThat(booleanSchema.getExample()).isEqualTo(false);
        }

        @Test
        @DisplayName("Should convert Int32Value to nullable integer schema")
        void shouldConvertInt32ValueToNullableIntegerSchema() {
            var annotatedType = createAnnotatedType(Int32Value.class);

            var schema = converter.resolve(annotatedType, context, chain);

            assertThat(schema).isInstanceOf(IntegerSchema.class);
            var integerSchema = (IntegerSchema) schema;
            assertThat(integerSchema.getFormat()).isEqualTo("int32");
            assertThat(integerSchema.getNullable()).isTrue();
            assertThat(integerSchema.getExample()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should convert StringValue to nullable string schema")
        void shouldConvertStringValueToNullableStringSchema() {
            var annotatedType = createAnnotatedType(StringValue.class);

            var schema = converter.resolve(annotatedType, context, chain);

            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getNullable()).isTrue();
            assertThat(stringSchema.getExample()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Special Types Schema Tests")
    class SpecialTypesSchemaTests {

        @Test
        @DisplayName("Should convert Any to object schema with @type field")
        void shouldConvertAnyToObjectSchemaWithTypeField() {
            var annotatedType = createAnnotatedType(Any.class);

            var schema = converter.resolve(annotatedType, context, chain);

            assertThat(schema).isInstanceOf(ObjectSchema.class);
            var objectSchema = (ObjectSchema) schema;
            assertThat(objectSchema.getProperties()).containsKey("@type");
            assertThat(objectSchema.getAdditionalProperties()).isEqualTo(true);
            assertThat(objectSchema.getExample()).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("Should convert Struct to object schema with additional properties")
        void shouldConvertStructToObjectSchemaWithAdditionalProperties() {
            var annotatedType = createAnnotatedType(Struct.class);

            var schema = converter.resolve(annotatedType, context, chain);

            assertThat(schema).isInstanceOf(ObjectSchema.class);
            var objectSchema = (ObjectSchema) schema;
            assertThat(objectSchema.getAdditionalProperties()).isEqualTo(true);
            assertThat(objectSchema.getExample()).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("Should convert ListValue to array schema")
        void shouldConvertListValueToArraySchema() {
            var annotatedType = createAnnotatedType(ListValue.class);

            var schema = converter.resolve(annotatedType, context, chain);

            assertThat(schema).isInstanceOf(ArraySchema.class);
            var arraySchema = (ArraySchema) schema;
            assertThat(arraySchema.getItems()).isNotNull();
            assertThat(arraySchema.getExample()).isInstanceOf(List.class);
        }

        @Test
        @DisplayName("Should convert FieldMask to string schema")
        void shouldConvertFieldMaskToStringSchema() {
            var annotatedType = createAnnotatedType(FieldMask.class);

            var schema = converter.resolve(annotatedType, context, chain);

            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getExample()).isEqualTo("f.fooBar,h");
        }

        @Test
        @DisplayName("Should convert Empty to empty object schema")
        void shouldConvertEmptyToEmptyObjectSchema() {
            var annotatedType = createAnnotatedType(Empty.class);

            var schema = converter.resolve(annotatedType, context, chain);

            assertThat(schema).isInstanceOf(ObjectSchema.class);
            var objectSchema = (ObjectSchema) schema;
            assertThat(objectSchema.getExample()).isEqualTo(Map.of());
        }

        @Test
        @DisplayName("Should convert ByteString to base64 string schema")
        void shouldConvertByteStringToBase64StringSchema() {
            var annotatedType = createAnnotatedType(ByteString.class);

            var schema = converter.resolve(annotatedType, context, chain);

            assertThat(schema).isInstanceOf(StringSchema.class);
            var stringSchema = (StringSchema) schema;
            assertThat(stringSchema.getFormat()).isEqualTo("byte");
            assertThat(stringSchema.getExample()).isEqualTo("YWJjMTIzIT8kKiYoKSctPUB+");
        }
    }

    private AnnotatedType createAnnotatedType(Class<?> clazz) {
        return new AnnotatedType(clazz);
    }
}
