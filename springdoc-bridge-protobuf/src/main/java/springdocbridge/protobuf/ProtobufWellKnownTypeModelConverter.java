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
import com.google.protobuf.Internal;
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
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.math.BigDecimal;
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
 * @see ModelConverter
 * @see <a href="https://protobuf.dev/programming-guides/json/">Protobuf JSON Mapping</a>
 * @see SpringDocBridgeProtobufAutoConfiguration
 * @since 0.1.0
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
            return createMapFieldSchema(javaType, context);
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

        // Handle protobuf List type
        if (Internal.ProtobufList.class.isAssignableFrom(cls)) {
            // There some additional properties added by springdoc, we need to remove them
            // see AbstractProtobufList
            schema.properties(null);
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
                handleField(descriptor, resolvedSchema, context);
            }
        } else if (schema.getProperties() != null) {
            handleField(descriptor, schema, context);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void handleField(
            Descriptors.Descriptor descriptor, Schema resolvedSchema, ModelConverterContext context) {
        Map<String, Schema> properties = resolvedSchema.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        for (var field : descriptor.getFields()) {
            if (field.toProto().getProto3Optional()) {
                continue;
            }

            String propertyName = field.getName();
            var schema = properties.get(propertyName);
            if (schema == null) {
                propertyName = field.getJsonName();
                schema = properties.get(propertyName);
            }

            if (schema == null) {
                continue;
            }

            // Handle repeated enum fields
            if (field.isRepeated() && field.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
                handleFieldForEnum(field, schema, context);
            }

            // Handle map fields with enum values
            if (field.isMapField() && field.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                // For map fields, we need to check the value type of the map entry
                var mapEntryDescriptor = field.getMessageType();
                if (mapEntryDescriptor.getFields().size() == 2) {
                    var valueField = mapEntryDescriptor.getFields().get(1); // value field is always at index 1
                    if (valueField.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
                        handleFieldForEnum(valueField, schema, context);
                    }
                }
            }

            // Handle deprecated fields - set deprecated flag in OpenAPI schema
            if (field.getOptions().getDeprecated()) {
                schema.setDeprecated(true);
            }

            // OpenAPI 3.x all fields are optional by default, so we need to add required fields manually
            // see https://spec.openapis.org/oas/v3.0.0.html#schema
            resolvedSchema.addRequiredItem(propertyName);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void handleFieldForEnum(
            Descriptors.FieldDescriptor fieldDescriptor, Schema schema, ModelConverterContext context) {

        var enumDescriptor = fieldDescriptor.getEnumType();

        String enumSchemaName = getEnumClassName(enumDescriptor);

        if (!context.getDefinedModels().containsKey(enumSchemaName)) {
            StringSchema enumSchema = new StringSchema();

            List<String> enumValues = enumDescriptor.getValues().stream()
                    .map(Descriptors.EnumValueDescriptor::getName)
                    .toList();
            enumSchema.setEnum(enumValues);

            context.defineModel(enumSchemaName, enumSchema);
        }

        Schema<?> enumRefSchema = new Schema<>().$ref("#/components/schemas/" + enumSchemaName);

        if (schema instanceof ObjectSchema objectSchema) {
            objectSchema.setAdditionalProperties(enumRefSchema);
        } else if (schema instanceof MapSchema mapSchema) {
            mapSchema.setAdditionalProperties(enumRefSchema);
        } else if (schema instanceof JsonSchema jsonSchema) {
            jsonSchema.setAdditionalProperties(enumRefSchema);
        } else if (schema instanceof ArraySchema arraySchema) {
            arraySchema.setItems(enumRefSchema);
        } else {
            var type = schema.getType();
            if (type == null && schema.getTypes() != null && !schema.getTypes().isEmpty()) {
                type = (String) schema.getTypes().iterator().next();
            }
            if (type != null) {
                switch (type) {
                    case "object" -> schema.setAdditionalProperties(enumRefSchema);
                    case "array" -> schema.setItems(enumRefSchema);
                    default -> {}
                }
            }
        }
    }

    private static String getEnumClassName(Descriptors.EnumDescriptor enumDescriptor) {
        // Build the full class name for the enum
        String packageName = enumDescriptor.getFile().getOptions().getJavaPackage();
        if (packageName.isEmpty()) {
            packageName = enumDescriptor.getFile().getPackage();
        }

        // Handle nested enums
        String className;
        Descriptors.Descriptor containingType = enumDescriptor.getContainingType();
        if (containingType != null) {
            className = containingType.getName() + "." + enumDescriptor.getName();
        } else {
            className = enumDescriptor.getName();
        }

        return packageName + "." + className;
    }

    private static Map<Class<?>, Schema<?>> createWellKnownTypeSchemas() {
        Map<Class<?>, Schema<?>> map = new HashMap<>();

        // Timestamp: RFC 3339 string
        map.put(Timestamp.class, new StringSchema().format("date-time"));

        // Duration: string with "s" suffix
        map.put(Duration.class, new StringSchema().pattern("^-?\\d+(\\.\\d+)?s$"));

        // Wrapper types: same as wrapped primitive type, but nullable
        map.put(BoolValue.class, new BooleanSchema());
        map.put(Int32Value.class, new IntegerSchema());
        map.put(Int64Value.class, new IntegerSchema().format("int64"));
        map.put(UInt32Value.class, new IntegerSchema().minimum(BigDecimal.ONE));
        map.put(UInt64Value.class, new IntegerSchema().format("int64").minimum(BigDecimal.ONE));
        map.put(FloatValue.class, new NumberSchema().format("float"));
        map.put(DoubleValue.class, new NumberSchema().format("double"));
        map.put(StringValue.class, new StringSchema());
        map.put(BytesValue.class, new StringSchema().format("byte"));

        return Map.copyOf(map);
    }

    private static Map<Class<?>, Schema<?>> createSpecialTypeSchemas() {
        Map<Class<?>, Schema<?>> map = new HashMap<>();

        // Any: object with @type field
        map.put(Any.class, new ObjectSchema().additionalProperties(true).addProperty("@type", new StringSchema()));

        // Struct: any JSON object
        map.put(Struct.class, new ObjectSchema().additionalProperties(true));

        // ListValue: array
        map.put(ListValue.class, new ArraySchema().items(new Schema<>()));

        // Value: any JSON value
        map.put(Value.class, new Schema<>());

        // NullValue: null
        map.put(NullValue.class, new Schema<>());

        // FieldMask: string
        map.put(FieldMask.class, new Schema<>()); // TODO(Freeman): StringSchema?

        // Empty: empty object
        map.put(Empty.class, new ObjectSchema());

        // ByteString: base64 string
        map.put(ByteString.class, new StringSchema().format("byte"));

        return Map.copyOf(map);
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
    private static Schema<?> createMapFieldSchema(JavaType javaType, ModelConverterContext context) {
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
                } else if (ProtocolMessageEnum.class.isAssignableFrom(valueClass) && valueClass.isEnum()) {
                    // Handle protobuf enum values - create $ref to enum schema
                    Schema<?> enumSchema = createProtobufEnumSchemaWithRef(valueClass, context);
                    schema.setAdditionalProperties(enumSchema);
                } else if (Message.class.isAssignableFrom(valueClass)) {
                    // Handle protobuf message values - create $ref to message schema
                    ProtobufNameResolver nameResolver = new ProtobufNameResolver();
                    String messageSchemaName = nameResolver.getNameOfClass(valueClass);
                    Schema<?> messageRefSchema = new Schema<>().$ref("#/components/schemas/" + messageSchemaName);
                    schema.setAdditionalProperties(messageRefSchema);
                } else {
                    // For other complex types, just use true to allow any value
                    schema.setAdditionalProperties(true);
                }
            }
        }

        return schema;
    }
}
