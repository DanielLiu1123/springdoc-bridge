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
import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.JsonSchema;
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
import org.springframework.beans.BeanUtils;

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
            return createSchemaForEnum(cls, context, type.isResolveAsRef());
        }

        if (ProtobufNameResolver.isProtobufMessage(cls)) {
            return createSchemaForMessage(cls, context, type.isResolveAsRef());
        }

        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }

    private Schema<?> createSchemaForMessage(Class<?> cls, ModelConverterContext context, boolean resolveAsRef) {

        var descriptor = ProtobufNameResolver.getDescriptor(cls);
        if (descriptor == null) {
            throw new IllegalStateException("No descriptor found for class " + cls);
        }

        var schemaName = protobufNameResolver.getNameOfClass(cls);
        var ref = RefUtils.constructRef(schemaName);
        if (context.getDefinedModels().containsKey(schemaName)) {
            return resolveAsRef
                    ? new Schema<>().$ref(ref)
                    : context.getDefinedModels().get(schemaName);
        }

        var schema = new ObjectSchema();

        if (descriptor.getOptions().getDeprecated()) {
            schema.setDeprecated(true);
        }

        for (var field : descriptor.getFields()) {
            var fieldType = getGetterReturnType(cls, field);
            var fieldName = underlineToCamel(field.getName());

            var fieldSchema = context.resolve(
                    new AnnotatedType(fieldType).schemaProperty(true).resolveAsRef(true));
            if (field.getOptions().getDeprecated()) {
                fieldSchema = newSchema(fieldSchema).deprecated(true);
            }

            schema.addProperty(fieldName, fieldSchema);

            if (!field.toProto().getProto3Optional()) {
                schema.addRequiredItem(fieldName);
            }
        }

        // Register the enum schema in the context
        context.defineModel(schemaName, schema);

        return resolveAsRef ? new Schema<>().$ref(ref) : schema;
    }

    private static Schema<?> newSchema(Schema<?> fieldSchema) {
        var newSchema = new Schema<>();
        BeanUtils.copyProperties(fieldSchema, newSchema);
        return newSchema;
    }

    /**
     * Find the getter method for the given field descriptor.
     */
    private static Type getGetterReturnType(Class<?> javaClass, Descriptors.FieldDescriptor fieldDescriptor) {
        String fieldName = fieldDescriptor.getName();

        String getterMethodName;
        if (fieldDescriptor.isMapField()) {
            getterMethodName = "get" + underlineToPascal(fieldName) + "Map";
        } else if (fieldDescriptor.isRepeated()) {
            getterMethodName = "get" + underlineToPascal(fieldName) + "List";
        } else {
            getterMethodName = "get" + underlineToPascal(fieldName);
        }

        try {
            Method getterMethod = javaClass.getMethod(getterMethodName);
            return getterMethod.getGenericReturnType();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static String underlineToCamel(String name) {
        var sb = new StringBuilder();
        var len = name.length();
        var end = len - 1;
        for (var i = 0; i < len; i++) {
            var c = name.charAt(i);
            if (c == '_' && i < end) {
                sb.append(Character.toUpperCase(name.charAt(++i)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String underlineToPascal(String name) {
        var n = underlineToCamel(name);
        if (n.isBlank()) {
            return n;
        }
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
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
        map.put(NullValue.class, new JsonSchema().typesItem("null"));

        // FieldMask: string
        map.put(FieldMask.class, new StringSchema());

        // Empty: empty object
        map.put(Empty.class, new ObjectSchema());

        // ByteString: base64 string
        map.put(ByteString.class, new StringSchema().format("byte"));

        // ProtocolStringList: repeated string
        map.put(ProtocolStringList.class, new ArraySchema().items(new StringSchema()));

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
    private Schema<?> createSchemaForEnum(
            Class<?> protobufEnumClass, ModelConverterContext context, boolean resolveAsRef) {

        var enumDescriptor = ProtobufNameResolver.getEnumDescriptor(protobufEnumClass);
        if (enumDescriptor == null) {
            throw new IllegalStateException("No enum descriptor found for class " + protobufEnumClass);
        }

        String enumSchemaName = protobufNameResolver.getNameOfClass(protobufEnumClass);
        var ref = RefUtils.constructRef(enumSchemaName);
        if (context.getDefinedModels().containsKey(enumSchemaName)) {
            return resolveAsRef
                    ? new Schema<>().$ref(ref)
                    : context.getDefinedModels().get(enumSchemaName);
        }

        // Create the enum schema
        StringSchema enumSchema = new StringSchema();

        if (enumDescriptor.getOptions().getDeprecated()) {
            enumSchema.setDeprecated(true);
        }

        Object[] enumConstants = protobufEnumClass.getEnumConstants();
        if (enumConstants != null) {
            List<String> enumValues = Arrays.stream(enumConstants)
                    .map(Object::toString)
                    .filter(s -> !Objects.equals(s, "UNRECOGNIZED"))
                    .toList();
            enumSchema.setEnum(enumValues);
        }

        // Register the enum schema in the context
        context.defineModel(enumSchemaName, enumSchema);

        return resolveAsRef
                ? new Schema<>().$ref(ref)
                : context.getDefinedModels().get(enumSchemaName);
    }
}
