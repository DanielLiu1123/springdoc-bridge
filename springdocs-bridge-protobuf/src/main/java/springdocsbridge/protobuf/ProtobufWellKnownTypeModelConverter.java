package springdocsbridge.protobuf;

import com.fasterxml.jackson.databind.JavaType;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.NullValue;
import com.google.protobuf.ProtocolMessageEnum;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import com.google.type.Date;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.util.ClassUtils;

/**
 * Protobuf type schema customizer that implements the protobuf JSON mapping rules
 * according to https://protobuf.dev/programming-guides/json/
 *
 * @author Freeman
 */
public class ProtobufWellKnownTypeModelConverter implements ModelConverter {

    private static final Map<Class<?>, Schema<?>> WELL_KNOWN_TYPE_SCHEMAS = createWellKnownTypeSchemas();
    private static final Map<Class<?>, Schema<?>> SPECIAL_TYPE_SCHEMAS = createSpecialTypeSchemas();

    private final ObjectMapperProvider springDocObjectMapper;

    public ProtobufWellKnownTypeModelConverter(ObjectMapperProvider springDocObjectMapper) {
        this.springDocObjectMapper = springDocObjectMapper;
    }

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        JavaType javaType = springDocObjectMapper.jsonMapper().constructType(type.getType());
        if (javaType != null) {
            Class<?> cls = javaType.getRawClass();
            if (WELL_KNOWN_TYPE_SCHEMAS.containsKey(cls)) {
                return WELL_KNOWN_TYPE_SCHEMAS.get(cls);
            }
            if (SPECIAL_TYPE_SCHEMAS.containsKey(cls)) {
                return SPECIAL_TYPE_SCHEMAS.get(cls);
            }
            if (ProtocolMessageEnum.class.isAssignableFrom(cls) && cls.isEnum()) {
                return createProtobufEnumSchema(cls);
            }
            if (Message.class.isAssignableFrom(cls)) {
                var schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
                if (schema == null) {
                    return null;
                }

                var descriptor = ProtobufTypeNameResolver.getDescriptor(cls);
                if (descriptor == null) {
                    return schema;
                }

                // Handle optional fields
                processOptionalFields(schema, descriptor, context, cls);

                return schema;
            }
        }
        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }

    private void processOptionalFields(
            Schema<?> schema,
            com.google.protobuf.Descriptors.Descriptor descriptor,
            ModelConverterContext context,
            Class<?> cls) {
        // If schema has properties directly, modify them
        if (schema.getProperties() != null) {
            Map<String, Schema> properties = schema.getProperties();
            for (var field : descriptor.getFields()) {
                if (!field.toProto().getProto3Optional()) {
                    continue;
                }
                var s = properties.get(field.getName());
                if (s == null) {
                    s = properties.get(field.getJsonName());
                }
                if (s != null) {
                    s.setNullable(true);
                }
            }
        }
        // If schema is a reference, try to resolve and modify the referenced schema
        else if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            // Extract schema name from $ref (e.g., "#/components/schemas/user.v1.PatchUserRequest")
            String schemaName = ref.substring(ref.lastIndexOf('/') + 1);

            // Get the resolved schema from context
            var resolvedSchema = context.getDefinedModels().get(schemaName);
            if (resolvedSchema != null && resolvedSchema.getProperties() != null) {
                Map<String, Schema> properties = resolvedSchema.getProperties();
                for (var field : descriptor.getFields()) {
                    if (!field.toProto().getProto3Optional()) {
                        continue;
                    }
                    var s = properties.get(field.getName());
                    if (s == null) {
                        s = properties.get(field.getJsonName());
                    }
                    if (s != null) {
                        s.setNullable(true);
                    }
                }
            }
        }
    }

    private static Map<Class<?>, Schema<?>> createWellKnownTypeSchemas() {
        Map<Class<?>, Schema<?>> map = new HashMap<>();

        // Timestamp: RFC 3339 string
        map.put(Timestamp.class, createTimestampSchema());

        // Duration: string with "s" suffix
        map.put(Duration.class, createDurationSchema());

        // Wrapper types: same as wrapped primitive type, but nullable
        map.put(BoolValue.class, createNullableBooleanSchema());
        map.put(Int32Value.class, createNullableInt32Schema());
        map.put(Int64Value.class, createNullableInt64Schema());
        map.put(UInt32Value.class, createNullableUInt32Schema());
        map.put(UInt64Value.class, createNullableUInt64Schema());
        map.put(FloatValue.class, createNullableFloatSchema());
        map.put(DoubleValue.class, createNullableDoubleSchema());
        map.put(StringValue.class, createNullableStringSchema());
        map.put(BytesValue.class, createNullableBytesSchema());

        return Map.copyOf(map);
    }

    private static Map<Class<?>, Schema<?>> createSpecialTypeSchemas() {
        Map<Class<?>, Schema<?>> map = new HashMap<>();

        // Any: object with @type field
        map.put(Any.class, createAnySchema());

        // Struct: any JSON object
        map.put(Struct.class, createStructSchema());

        // ListValue: array
        map.put(ListValue.class, createListValueSchema());

        // Value: any JSON value
        map.put(Value.class, createValueSchema());

        // NullValue: null
        map.put(NullValue.class, createNullValueSchema());

        // FieldMask: string
        map.put(FieldMask.class, createFieldMaskSchema());

        // Empty: empty object
        map.put(Empty.class, createEmptySchema());

        // ByteString: base64 string
        map.put(ByteString.class, createByteStringSchema());

        // Date: string with "yyyy-MM-dd" format
        if (ClassUtils.isPresent("com.google.type.Date", null)) {
            map.put(Date.class, createDateSchema());
        }

        return Map.copyOf(map);
    }

    private static Schema<?> createDateSchema() {
        StringSchema schema = new StringSchema();
        schema.setFormat("date");
        schema.setExample("1970-01-01");
        return schema;
    }

    static Schema<?> createTimestampSchema() {
        StringSchema schema = new StringSchema();
        schema.setFormat("date-time");
        schema.setExample("1970-01-01T00:00:00Z");
        return schema;
    }

    private static Schema<?> createDurationSchema() {
        StringSchema schema = new StringSchema();
        schema.setPattern("^-?\\d+(\\.\\d+)?s$");
        schema.setExample("1.000340012s");
        return schema;
    }

    private static Schema<?> createNullableBooleanSchema() {
        BooleanSchema schema = new BooleanSchema();
        schema.setNullable(true);
        schema.setExample("false");
        return schema;
    }

    private static Schema<?> createNullableInt32Schema() {
        IntegerSchema schema = new IntegerSchema();
        schema.setFormat("int32");
        schema.setNullable(true);
        schema.setExample("0");
        return schema;
    }

    private static Schema<?> createNullableInt64Schema() {
        StringSchema schema = new StringSchema();
        schema.setNullable(true);
        schema.setExample("0");
        return schema;
    }

    private static Schema<?> createNullableUInt32Schema() {
        IntegerSchema schema = new IntegerSchema();
        schema.setFormat("int32");
        schema.setMinimum(java.math.BigDecimal.ZERO);
        schema.setNullable(true);
        schema.setExample("0");
        return schema;
    }

    private static Schema<?> createNullableUInt64Schema() {
        StringSchema schema = new StringSchema();
        schema.setNullable(true);
        schema.setExample("0");
        return schema;
    }

    private static Schema<?> createNullableFloatSchema() {
        NumberSchema schema = new NumberSchema();
        schema.setFormat("float");
        schema.setNullable(true);
        schema.setExample("0.0");
        return schema;
    }

    private static Schema<?> createNullableDoubleSchema() {
        NumberSchema schema = new NumberSchema();
        schema.setFormat("double");
        schema.setNullable(true);
        schema.setExample("0.0");
        return schema;
    }

    private static Schema<?> createNullableStringSchema() {
        StringSchema schema = new StringSchema();
        schema.setNullable(true);
        schema.setExample("");
        return schema;
    }

    private static Schema<?> createNullableBytesSchema() {
        StringSchema schema = new StringSchema();
        schema.setFormat("byte");
        schema.setNullable(true);
        schema.setExample("YWJjMTIzIT8kKiYoKSctPUB+");
        return schema;
    }

    private static Schema<?> createAnySchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("@type", new StringSchema().example("type.googleapis.com/google.type.Date"));
        schema.setAdditionalProperties(true);
        schema.setExample(Map.of("@type", "type.googleapis.com/google.type.Date", "day", 1, "month", 1, "year", 1970));
        return schema;
    }

    private static Schema<?> createStructSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.setAdditionalProperties(true);
        schema.setExample(Map.of("key1", "value1", "key2", 2, "key3", true));
        return schema;
    }

    private static Schema<?> createListValueSchema() {
        ArraySchema schema = new ArraySchema();
        schema.setItems(createValueSchema());
        schema.setExample(List.of("v1", "v2"));
        return schema;
    }

    private static Schema<?> createValueSchema() {
        Schema<?> schema = new Schema<>();
        schema.setExample("any value");
        return schema;
    }

    private static Schema<?> createNullValueSchema() {
        Schema<?> schema = new Schema<>();
        schema.setNullable(true);
        schema.setExample(null);
        return schema;
    }

    private static Schema<?> createFieldMaskSchema() {
        StringSchema schema = new StringSchema();
        schema.setExample("f.fooBar,h");
        return schema;
    }

    private static Schema<?> createEmptySchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.setExample(Map.of());
        return schema;
    }

    private static Schema<?> createByteStringSchema() {
        StringSchema schema = new StringSchema();
        schema.setFormat("byte");
        schema.setExample("YWJjMTIzIT8kKiYoKSctPUB+");
        return schema;
    }

    private static Schema<?> createProtobufEnumSchema(Class<?> protobufEnumClass) {
        StringSchema schema = new StringSchema();

        // Get enum values
        if (protobufEnumClass.isEnum()) {
            Object[] enumConstants = protobufEnumClass.getEnumConstants();
            if (enumConstants != null) {
                List<String> enumValues = Arrays.stream(enumConstants)
                        .map(Object::toString)
                        .filter(s -> !Objects.equals(s, "UNRECOGNIZED"))
                        .toList();
                schema.setEnum(enumValues);
            }
        }

        return schema;
    }
}
