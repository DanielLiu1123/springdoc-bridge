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
import com.google.protobuf.NullValue;
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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.core.ResolvableType;
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
public class ProtobufModelConverter implements ModelConverter {

    private static final Map<Class<?>, Schema<?>> WELL_KNOWN_TYPE_SCHEMAS = createWellKnownTypeSchemas();
    private static final Map<Class<?>, Schema<?>> SPECIAL_TYPE_SCHEMAS = createSpecialTypeSchemas();

    private final ObjectMapperProvider springDocObjectMapper;
    private final ProtobufNameResolver protobufNameResolver;

    public ProtobufModelConverter(
            ObjectMapperProvider springDocObjectMapper, ProtobufNameResolver protobufNameResolver) {
        this.springDocObjectMapper = springDocObjectMapper;
        this.protobufNameResolver = protobufNameResolver;
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

        if (ProtobufNameResolver.isProtobufEnum(cls)) {
            return createSchemaForEnum(cls, context);
        }

        if (ProtobufNameResolver.isProtobufMessage(cls)) {
            return createSchemaForMessage(cls, context);
        }

        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }

    private Schema<?> createSchemaForMessage(Class<?> cls, ModelConverterContext context) {
        var descriptor = ProtobufNameResolver.getDescriptor(cls);
        if (descriptor == null) {
            throw new IllegalStateException("No descriptor found for class " + cls);
        }

        var schemaName = protobufNameResolver.getNameOfClass(cls);
        var ref = "#/components/schemas/" + schemaName;
        if (context.getDefinedModels().containsKey(schemaName)) {
            return new Schema<>().$ref(ref);
        }

        var schema = new ObjectSchema();

        for (var field : descriptor.getFields()) {
            var fieldType = getGetterReturnType(cls, field);
            var fieldSchema = context.resolve(new AnnotatedType(fieldType));

            schema.addProperty(field.getName(), fieldSchema);

            if (!field.toProto().getProto3Optional()) {
                schema.addRequiredItem(field.getName());
            }

            if (field.getOptions().getDeprecated()) {
                fieldSchema.setDeprecated(true);
            }
        }

        // Register the enum schema in the context
        context.defineModel(schemaName, schema);

        // Return a $ref to the registered schema
        return new Schema<>().$ref(ref);
    }

    /**
     * Find the getter method for the given field descriptor.
     */
    public static Type getGetterReturnType(Class<?> clazz, Descriptors.FieldDescriptor fieldDescriptor) {
        var name = underlineToCamel(fieldDescriptor.getName());

        String[] possibleMethodNames = {
            "get" + StringUtils.capitalize(name),
            "get" + StringUtils.capitalize(name) + "List", // repeated fields
            "get" + StringUtils.capitalize(name) + "Map" // map fields
        };

        for (String methodName : possibleMethodNames) {
            try {
                Method method = clazz.getMethod(methodName);
                var type = method.getGenericReturnType();
                if (type instanceof Class<?> clz) {
                    if (List.class.isAssignableFrom(clz)) { // repeated string returns ProtocolStringList
                        return ResolvableType.forType(type).as(List.class).getType();
                    }
                }
                return type;
            } catch (NoSuchMethodException e) {
                // no-op
            }
        }

        throw new IllegalStateException("No getter method found for " + name + " in " + clazz);
    }

    private static String underlineToCamel(String name) {
        var sb = new StringBuilder();
        for (var i = 0; i < name.length(); i++) {
            var c = name.charAt(i);
            if (c == '_') {
                sb.append(Character.toUpperCase(name.charAt(++i)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Map<Class<?>, Schema<?>> createWellKnownTypeSchemas() {
        Map<Class<?>, Schema<?>> map = new HashMap<>();

        // Timestamp: RFC 3339 string
        map.put(Timestamp.class, new StringSchema().format("date-time"));

        // Duration: string with "s" suffix
        map.put(Duration.class, new StringSchema().pattern("^-?\\d+(\\.\\d+)?s$"));

        // Wrapper types: same as wrapped primitive type, but nullable
        map.put(BoolValue.class, new BooleanSchema());
        map.put(Int32Value.class, new IntegerSchema().format("int32"));
        map.put(UInt32Value.class, new IntegerSchema().format("int32").minimum(BigDecimal.ZERO));
        map.put(Int64Value.class, new IntegerSchema().format("int64"));
        map.put(UInt64Value.class, new IntegerSchema().format("int64").minimum(BigDecimal.ZERO));
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
        map.put(FieldMask.class, new StringSchema());

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
    private Schema<?> createSchemaForEnum(Class<?> protobufEnumClass, ModelConverterContext context) {
        // Generate a unique schema name for the enum based on naming strategy
        String enumSchemaName = protobufNameResolver.getNameOfClass(protobufEnumClass);

        if (context.getDefinedModels().containsKey(enumSchemaName)) {
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
}
