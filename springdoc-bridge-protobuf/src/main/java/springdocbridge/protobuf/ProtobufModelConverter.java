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
import jakarta.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.beans.BeanUtils;
import springdocbridge.protobuf.SpringDocBridgeProtobufProperties.OneofBehavior;

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

    @SuppressWarnings("rawtypes")
    private static final Map<Class<?>, Schema> SPECIAL_TYPE_SCHEMAS = createSpecialTypeSchemas();

    private final ObjectMapperProvider springDocObjectMapper;
    private final ProtobufNameResolver protobufNameResolver;
    private final OneofBehavior oneofBehavior;

    public ProtobufModelConverter(
            ObjectMapperProvider springDocObjectMapper, ProtobufNameResolver protobufNameResolver) {
        this(springDocObjectMapper, protobufNameResolver, OneofBehavior.FLATTEN);
    }

    public ProtobufModelConverter(
            ObjectMapperProvider springDocObjectMapper,
            ProtobufNameResolver protobufNameResolver,
            OneofBehavior oneofBehavior) {
        this.springDocObjectMapper = springDocObjectMapper;
        this.protobufNameResolver = protobufNameResolver;
        this.oneofBehavior = oneofBehavior;
    }

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        JavaType javaType = springDocObjectMapper.jsonMapper().constructType(type.getType());
        if (javaType == null) {
            return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        }

        Class<?> cls = javaType.getRawClass();

        Schema<?> schemaForSpecialType = createSchemaForSpecialType(cls, context);
        if (schemaForSpecialType != null) {
            return schemaForSpecialType;
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
        var ref = RefUtils.constructRef(schemaName);
        if (context.getDefinedModels().containsKey(schemaName)) {
            return new Schema<>().$ref(ref);
        }

        var useOneOf = oneofBehavior == OneofBehavior.ONE_OF;
        var realOneofs = useOneOf ? descriptor.getRealOneofs() : List.<Descriptors.OneofDescriptor>of();

        // Object schema holding all "regular" fields: every field in FLATTEN mode, or every
        // non-oneof field in ONE_OF mode (synthetic oneofs for proto3 'optional' have
        // getRealContainingOneof() == null and are treated as regular fields here).
        var objectSchema = new ObjectSchema();
        for (var field : descriptor.getFields()) {
            if (useOneOf && field.getRealContainingOneof() != null) {
                continue;
            }

            var fieldName = underlineToCamel(field.getName());
            var fieldSchema = resolveFieldSchema(cls, field, context);

            objectSchema.addProperty(fieldName, fieldSchema);

            if (!isOptional(field)) {
                objectSchema.addRequiredItem(fieldName);
            }
        }

        Schema<?> schema;
        if (realOneofs.isEmpty()) {
            schema = objectSchema;
        } else {
            // Compose the regular fields and one oneOf per oneof group under a single allOf, instead
            // of putting oneOf/allOf as siblings of 'properties'. Keeping 'properties' at the same
            // level as 'allOf' is valid JSON Schema, but several renderers (e.g. Redoc/Swagger UI)
            // only render one of them and silently drop the regular properties. Nesting the regular
            // fields as the first allOf member keeps them visible alongside the oneof variants.
            schema = new Schema<>();
            schema.addAllOfItem(objectSchema);
            for (var oneof : realOneofs) {
                schema.addAllOfItem(createSchemaForOneof(cls, oneof, context));
            }
        }

        if (descriptor.getOptions().getDeprecated()) {
            schema.setDeprecated(true);
        }

        // Register the schema in the context
        context.defineModel(schemaName, schema);

        // Return a $ref to the registered schema
        return new Schema<>().$ref(ref);
    }

    /**
     * Builds an OpenAPI {@code oneOf} schema for a single protobuf {@code oneof} group. Each member
     * becomes a branch that requires exactly that field, documenting the mutual exclusion between
     * the members.
     */
    private Schema<?> createSchemaForOneof(
            Class<?> cls, Descriptors.OneofDescriptor oneof, ModelConverterContext context) {
        var oneOfSchema = new Schema<>();
        for (var field : oneof.getFields()) {
            var fieldName = underlineToCamel(field.getName());
            var fieldSchema = resolveFieldSchema(cls, field, context);

            var branch = new ObjectSchema();
            branch.addProperty(fieldName, fieldSchema);
            branch.addRequiredItem(fieldName);

            oneOfSchema.addOneOfItem(branch);
        }
        return oneOfSchema;
    }

    /**
     * Resolves the OpenAPI schema for a single protobuf field, applying the {@code deprecated} flag
     * when the field is marked deprecated in the {@code .proto} definition.
     */
    private Schema<?> resolveFieldSchema(
            Class<?> cls, Descriptors.FieldDescriptor field, ModelConverterContext context) {
        var fieldType = getGetterReturnType(cls, field);
        var fieldSchema = context.resolve(
                new AnnotatedType(fieldType).schemaProperty(true).resolveAsRef(true));
        if (field.getOptions().getDeprecated()) {
            fieldSchema = newSchema(fieldSchema).deprecated(true);
        }
        return fieldSchema;
    }

    private static boolean isOptional(Descriptors.FieldDescriptor field) {
        // Proto3 optional fields and oneof fields are always optional
        if (field.toProto().getProto3Optional() || field.getContainingOneof() != null) {
            return true;
        }
        if (field.hasPresence()) {
            // edition version default is EXPLICIT, which means fields have has_xxx() methods
            // Non-optional message type for proto3 considered as required
            return field.getFile().toProto().getSyntax().equals("editions");
        }
        return false;
    }

    @Nullable
    private Schema<?> createSchemaForSpecialType(Class<?> cls, ModelConverterContext context) {

        for (var en : SPECIAL_TYPE_SCHEMAS.entrySet()) {
            if (en.getKey().isAssignableFrom(cls)) {
                return en.getValue();
            }
        }

        // Any: object with @type field
        // example:
        //  - {"@type": "type.googleapis.com/google.protobuf.Timestamp", "value": "2021-01-01T00:00:00Z"}
        //  - {"@type": "type.googleapis.com/google.type.Date", "year": 2021, "month": 1, "day": 1}
        if (Any.class.isAssignableFrom(cls)) {
            return createSchema(cls, context, () -> new ObjectSchema()
                    .additionalProperties(true)
                    .addProperty("@type", new StringSchema())
                    .addRequiredItem("@type"));
        }

        if (Empty.class.isAssignableFrom(cls)) {
            return createSchema(cls, context, ObjectSchema::new);
        }

        return null;
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
            getterMethodName = "get" + fieldNameToGetterInfix(fieldName) + "Map";
        } else if (fieldDescriptor.isRepeated()) {
            getterMethodName = "get" + fieldNameToGetterInfix(fieldName) + "List";
        } else {
            getterMethodName = "get" + fieldNameToGetterInfix(fieldName);
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

    /**
     * Converts a protobuf field name into the capitalized camelCase infix used by the generated
     * Java getter/setter names (e.g. {@code "a1b64"} -> {@code "A1B64"}, so the getter is
     * {@code getA1B64()}).
     *
     * <p>This replicates protobuf's own {@code UnderscoresToCamelCase} name mangling used by the
     * Java code generator: the first letter is capitalized, the letter following an underscore is
     * capitalized, and — crucially — the letter following a digit is also capitalized. A naive
     * snake_case-to-PascalCase conversion misses the digit rule and produces a getter name that
     * does not exist, which previously caused such fields to fall back to a generic schema.
     *
     * @see <a href="https://github.com/DanielLiu1123/springdoc-bridge/issues/21">Issue #21</a>
     */
    private static String fieldNameToGetterInfix(String name) {
        var sb = new StringBuilder(name.length());
        var capNext = true;
        for (var i = 0; i < name.length(); i++) {
            var c = name.charAt(i);
            if (c >= 'a' && c <= 'z') {
                sb.append(capNext ? Character.toUpperCase(c) : c);
                capNext = false;
            } else if (c >= 'A' && c <= 'Z') {
                // Existing capitals are left as-is (protobuf only lowercases a leading capital
                // when it is explicitly told not to capitalize the first letter).
                sb.append(c);
                capNext = false;
            } else if (c >= '0' && c <= '9') {
                sb.append(c);
                capNext = true;
            } else {
                // Underscore or any other separator: capitalize the next letter.
                capNext = true;
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("rawtypes")
    private static Map<Class<?>, Schema> createSpecialTypeSchemas() {
        var result = new HashMap<Class<?>, Schema>();

        // Wrapper types
        result.put(BoolValue.class, new BooleanSchema());
        result.put(Int32Value.class, new IntegerSchema().format("int32"));
        result.put(UInt32Value.class, new IntegerSchema().format("int32").minimum(BigDecimal.ZERO));
        result.put(Int64Value.class, new IntegerSchema().format("int64"));
        result.put(UInt64Value.class, new IntegerSchema().format("int64").minimum(BigDecimal.ZERO));
        result.put(FloatValue.class, new NumberSchema().format("float"));
        result.put(DoubleValue.class, new NumberSchema().format("double"));
        result.put(StringValue.class, new StringSchema());
        result.put(BytesValue.class, new StringSchema().format("byte"));

        // JSON types
        result.put(Struct.class, new ObjectSchema().additionalProperties(true));
        result.put(Value.class, new JsonSchema());
        result.put(ListValue.class, new ArraySchema().items(new JsonSchema()));
        // For compatibility reasons, do not use the typesItem(..) method.
        // It’s too new, and many services have not yet upgraded Swagger to 2.2.30.
        var nullSchema = new JsonSchema();
        nullSchema.addType("null");
        result.put(NullValue.class, nullSchema);

        // Special types

        // Timestamp: RFC 3339 string
        result.put(Timestamp.class, new StringSchema().format("date-time"));
        // Duration: string with "s" suffix
        // example: "1s", "1.5s", "-1s", "-1.5s"
        result.put(Duration.class, new StringSchema().pattern("^-?\\d+(\\.\\d+)?s$"));
        // FieldMask: string
        // example: "user.name,user.email"
        result.put(FieldMask.class, new StringSchema());
        // ByteString: base64 string
        result.put(ByteString.class, new StringSchema().format("byte"));
        // ProtocolStringList: repeated string
        result.put(ProtocolStringList.class, new ArraySchema().items(new StringSchema()));

        return result;
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

        var enumDescriptor = ProtobufNameResolver.getEnumDescriptor(protobufEnumClass);
        if (enumDescriptor == null) {
            throw new IllegalStateException("No enum descriptor found for class " + protobufEnumClass);
        }

        String enumSchemaName = protobufNameResolver.getNameOfClass(protobufEnumClass);
        var ref = RefUtils.constructRef(enumSchemaName);
        if (context.getDefinedModels().containsKey(enumSchemaName)) {
            return new Schema<>().$ref(ref);
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

        // Return a $ref to the registered schema
        return new Schema<>().$ref(ref);
    }

    private Schema<?> createSchema(Class<?> cls, ModelConverterContext context, Supplier<Schema<?>> schemaSupplier) {
        var schemaName = protobufNameResolver.getNameOfClass(cls);
        var ref = RefUtils.constructRef(schemaName);

        if (context.getDefinedModels().containsKey(schemaName)) {
            return new Schema<>().$ref(ref);
        }

        context.defineModel(schemaName, schemaSupplier.get());

        return new Schema<>().$ref(ref);
    }
}
