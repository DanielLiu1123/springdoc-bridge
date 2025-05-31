package springdocbridge.protobuf;

import com.fasterxml.jackson.databind.JavaType;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.ListValue;
import com.google.protobuf.MapField;
import com.google.protobuf.Message;
import com.google.protobuf.NullValue;
import com.google.protobuf.ProtocolMessageEnum;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
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
import org.springframework.util.StringUtils;

/**
 * OpenAPI model converter that provides specialized schema generation for Protocol Buffers (protobuf)
 * well-known types and custom protobuf messages.
 *
 * <p>This converter implements the official protobuf JSON mapping rules as specified in the
 * <a href="https://protobuf.dev/programming-guides/json/">Protobuf JSON Mapping Guide</a>.
 * It ensures that protobuf types are correctly represented in OpenAPI documentation with
 * appropriate schemas, examples, and constraints.
 *
 * <p> Supported Well-Known Types:
 * <ul>
 *   <li><strong>Timestamp</strong> - RFC 3339 date-time string (e.g., "1970-01-01T00:00:00Z")</li>
 *   <li><strong>Duration</strong> - String with "s" suffix (e.g., "1.000340012s")</li>
 *   <li><strong>Wrapper Types</strong> - Nullable primitive types (BoolValue, Int32Value, etc.)</li>
 *   <li><strong>Any</strong> - Object with "@type" field for type information</li>
 *   <li><strong>Struct</strong> - Arbitrary JSON object</li>
 *   <li><strong>ListValue</strong> - Array of arbitrary values</li>
 *   <li><strong>Value</strong> - Any JSON value</li>
 *   <li><strong>FieldMask</strong> - String representing field paths</li>
 *   <li><strong>Empty</strong> - Empty object</li>
 *   <li><strong>ByteString</strong> - Base64-encoded string</li>
 * </ul>
 *
 * <p> Usage Example:
 * <pre>{@code
 * // This converter is automatically registered by SpringDocBridgeProtobufAutoConfiguration
 * // No manual configuration is required
 *
 * @RestController
 * public class TimeController {
 *
 *     @GetMapping("/current-time")
 *     public Timestamp getCurrentTime() {
 *         // Will be documented as:
 *         // {
 *         //   "type": "string",
 *         //   "format": "date-time",
 *         //   "example": "1970-01-01T00:00:00Z"
 *         // }
 *         return Timestamps.now();
 *     }
 * }
 * }</pre>
 *
 * @author Freeman
 * @since 0.1.0
 * @see ModelConverter
 * @see <a href="https://protobuf.dev/programming-guides/json/">Protobuf JSON Mapping</a>
 * @see SpringDocBridgeProtobufAutoConfiguration
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
        if (javaType == null) {
            return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        }

        Class<?> cls = javaType.getRawClass();

        // Handle well-known types
        if (WELL_KNOWN_TYPE_SCHEMAS.containsKey(cls)) {
            return WELL_KNOWN_TYPE_SCHEMAS.get(cls);
        }
        if (SPECIAL_TYPE_SCHEMAS.containsKey(cls)) {
            return SPECIAL_TYPE_SCHEMAS.get(cls);
        }

        // Handle protobuf enums
        if (ProtocolMessageEnum.class.isAssignableFrom(cls) && cls.isEnum()) {
            return createProtobufEnumSchemaWithRef(cls, context);
        }

        // Handle protobuf MapField - convert to simple object with additionalProperties
        if (MapField.class.isAssignableFrom(cls)) {
            return createMapFieldSchema(javaType);
        }

        // Parse protobuf message
        var schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        if (schema == null) {
            return null;
        }

        // Handle fields, set required fields
        if (Message.class.isAssignableFrom(cls)) {
            var descriptor = ProtobufNameResolver.getDescriptor(cls);
            if (descriptor != null) {
                processFields(schema, descriptor, context);
            }
        }

        return schema;
    }

    private static void processFields(
            Schema<?> schema, com.google.protobuf.Descriptors.Descriptor descriptor, ModelConverterContext context) {
        if (StringUtils.hasText(schema.get$ref())) {
            String ref = schema.get$ref();
            // Extract schema name from $ref (e.g., "#/components/schemas/user.v1.PatchUserRequest")
            String schemaName = ref.substring(ref.lastIndexOf('/') + 1);

            var resolvedSchema = context.getDefinedModels().get(schemaName);
            if (resolvedSchema != null) {
                handleField(descriptor, resolvedSchema);
            }
        } else if (schema.getProperties() != null) {
            handleField(descriptor, schema);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void handleField(Descriptors.Descriptor descriptor, Schema resolvedSchema) {
        Map<String, Schema> properties = resolvedSchema.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        for (var field : descriptor.getFields()) {
            if (field.toProto().getProto3Optional()) {
                continue;
            }

            String propertyName = field.getName();
            var propertyValue = properties.get(propertyName);
            if (propertyValue == null) {
                propertyName = field.getJsonName();
                propertyValue = properties.get(propertyName);
            }
            if (propertyValue != null) {
                // OpenAPI 3.x all fields are optional by default, so we need to add required fields manually
                // see https://spec.openapis.org/oas/v3.0.0.html#schema
                resolvedSchema.addRequiredItem(propertyName);
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

        return Map.copyOf(map);
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
        schema.setExample(false);
        return schema;
    }

    private static Schema<?> createNullableInt32Schema() {
        IntegerSchema schema = new IntegerSchema();
        schema.setFormat("int32");
        schema.setNullable(true);
        schema.setExample(0);
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
        schema.setExample(0);
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
        schema.setExample(0.0);
        return schema;
    }

    private static Schema<?> createNullableDoubleSchema() {
        NumberSchema schema = new NumberSchema();
        schema.setFormat("double");
        schema.setNullable(true);
        schema.setExample(0.0);
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

    /**
     * Creates a reusable protobuf enum schema with $ref reference.
     *
     * <p> This method generates enum schemas that are registered in the OpenAPI components/schemas
     * section and returns a $ref to enable reuse across the API documentation.
     *
     * @see <a href="https://github.com/DanielLiu1123/springdoc-bridge/issues/5">Reuse enum</a>
     */
    private static Schema<?> createProtobufEnumSchemaWithRef(
            Class<?> protobufEnumClass, ModelConverterContext context) {
        // Generate a unique schema name for the enum
        String enumSchemaName = protobufEnumClass.getCanonicalName();

        // Check if the enum schema is already defined
        if (context.getDefinedModels().containsKey(enumSchemaName)) {
            // Return a $ref to the existing schema
            return new Schema<>().$ref("#/components/schemas/" + enumSchemaName);
        }

        // Create the enum schema
        StringSchema enumSchema = new StringSchema();

        // Get enum values
        if (protobufEnumClass.isEnum()) {
            Object[] enumConstants = protobufEnumClass.getEnumConstants();
            if (enumConstants != null) {
                List<String> enumValues = Arrays.stream(enumConstants)
                        .map(Object::toString)
                        .filter(s -> !Objects.equals(s, "UNRECOGNIZED"))
                        .toList();
                enumSchema.setEnum(enumValues);
            }
        }

        // Register the enum schema in the context
        context.defineModel(enumSchemaName, enumSchema);

        // Return a $ref to the registered schema
        return new Schema<>().$ref("#/components/schemas/" + enumSchemaName);
    }

    /**
     * Creates a simplified schema for protobuf MapField types.
     * Instead of exposing the internal MapField structure, this generates
     * a clean object schema with additionalProperties.
     */
    private static Schema<?> createMapFieldSchema(JavaType javaType) {
        ObjectSchema schema = new ObjectSchema();
        schema.setAdditionalProperties(true);

        // Try to determine the value type from the MapField generic parameters
        if (javaType.containedTypeCount() >= 2) {
            JavaType valueType = javaType.containedType(1);
            if (valueType != null) {
                Class<?> valueClass = valueType.getRawClass();

                // Set additionalProperties to the appropriate schema based on value type
                if (String.class.equals(valueClass)) {
                    schema.setAdditionalProperties(new StringSchema());
                } else if (Integer.class.equals(valueClass) || int.class.equals(valueClass)) {
                    schema.setAdditionalProperties(new IntegerSchema().format("int32"));
                } else if (Long.class.equals(valueClass) || long.class.equals(valueClass)) {
                    schema.setAdditionalProperties(new IntegerSchema().format("int64"));
                } else if (Boolean.class.equals(valueClass) || boolean.class.equals(valueClass)) {
                    schema.setAdditionalProperties(new BooleanSchema());
                } else if (Double.class.equals(valueClass) || double.class.equals(valueClass)) {
                    schema.setAdditionalProperties(new NumberSchema().format("double"));
                } else if (Float.class.equals(valueClass) || float.class.equals(valueClass)) {
                    schema.setAdditionalProperties(new NumberSchema().format("float"));
                } else {
                    // For complex types, just use true to allow any value
                    schema.setAdditionalProperties(true);
                }
            }
        }

        return schema;
    }
}
