package jacksonmodule.protobuf;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.ProtocolMessageEnum;
import com.google.protobuf.util.JsonFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Builder;

/**
 * Jackson module that provides comprehensive support for Protocol Buffers (protobuf) message and enum
 * serialization and deserialization.
 *
 * <p>This module automatically registers custom serializers and deserializers for:
 * <ul>
 *   <li>Protobuf messages implementing {@link MessageOrBuilder}</li>
 *   <li>Protobuf enums implementing {@link ProtocolMessageEnum}</li>
 * </ul>
 *
 * <p>The module uses Google's {@link JsonFormat} internally to ensure
 * compatibility with the official protobuf JSON mapping specification.
 *
 * <p> Usage Example:
 * <pre>{@code
 * // Register the module with ObjectMapper
 * ObjectMapper mapper = new ObjectMapper();
 * mapper.registerModule(new ProtobufModule());
 *
 * // Serialize protobuf message to JSON
 * MyProtoMessage message = MyProtoMessage.newBuilder()
 *     .setName("example")
 *     .setValue(42)
 *     .build();
 * String json = mapper.writeValueAsString(message);
 *
 * // Deserialize JSON back to protobuf message
 * MyProtoMessage restored = mapper.readValue(json, MyProtoMessage.class);
 * }</pre>
 *
 * @author Freeman
 * @see JsonFormat
 * @see com.google.protobuf.Message
 * @see ProtocolMessageEnum
 * @since 0.1.0
 */
@SuppressFBWarnings("SE_BAD_FIELD")
public final class ProtobufModule extends SimpleModule {

    /**
     * The configuration options for this module.
     */
    private final Options options;

    private final ProtobufEnumSerializer<?> enumSerializer;
    private final ProtobufMessageSerializer<?> messageSerializer;
    private final ConcurrentMap<Class<?>, ProtobufEnumDeserializer<?>> enumDeserializers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, ProtobufMessageDeserializer<?>> messageDeserializers =
            new ConcurrentHashMap<>();

    /**
     * Creates a new ProtobufModule with default options.
     *
     * @see Options#DEFAULT
     */
    public ProtobufModule() {
        this(Options.DEFAULT);
    }

    /**
     * Creates a new ProtobufModule with the specified options.
     *
     * @param options the configuration options for JSON serialization and deserialization
     */
    public ProtobufModule(Options options) {
        this.options = options;
        this.enumSerializer = new ProtobufEnumSerializer<>(options);
        this.messageSerializer = new ProtobufMessageSerializer<>(options);
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new Serializers.Base() {
            @Override
            public JsonSerializer<?> findSerializer(
                    SerializationConfig config, JavaType type, BeanDescription beanDesc) {
                if (MessageOrBuilder.class.isAssignableFrom(type.getRawClass())) {
                    return messageSerializer;
                }
                if (ProtocolMessageEnum.class.isAssignableFrom(type.getRawClass()) && type.isEnumType()) {
                    return enumSerializer;
                }
                return super.findSerializer(config, type, beanDesc);
            }
        });
        context.addDeserializers(new Deserializers.Base() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public JsonDeserializer<?> findEnumDeserializer(
                    Class<?> type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
                if (ProtocolMessageEnum.class.isAssignableFrom(type) && type.isEnum()) {
                    return enumDeserializers.computeIfAbsent(type, k -> new ProtobufEnumDeserializer(k));
                }
                return super.findEnumDeserializer(type, config, beanDesc);
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public JsonDeserializer<?> findBeanDeserializer(
                    JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
                Class<?> clz = type.getRawClass();
                if (MessageOrBuilder.class.isAssignableFrom(clz)) {
                    return messageDeserializers.computeIfAbsent(clz, k -> new ProtobufMessageDeserializer(k, options));
                }
                return super.findBeanDeserializer(type, config, beanDesc);
            }
        });
    }

    /**
     * @param serializeEnumAsInt whether to serialize protobuf enums as integers (default: false)
     * @param parser             JsonFormat.Parser instance for parsing JSON to protobuf messages
     * @param printer            JsonFormat.Printer instance for printing protobuf messages to JSON
     */
    @Builder(toBuilder = true)
    public record Options(boolean serializeEnumAsInt, JsonFormat.Parser parser, JsonFormat.Printer printer) {
        /**
         * Compact constructor for Options record that initializes default values for null parameters.
         * If parser is null, creates a default parser that ignores unknown fields.
         * If printer is null, creates a default printer that omits whitespace and includes default value fields.
         * When serializeEnumAsInt is true and printer is null, configures the default printer to print enums as integers.
         *
         * @param serializeEnumAsInt whether to serialize protobuf enums as integers
         * @param parser             the JSON parser for converting JSON to protobuf messages
         * @param printer            the JSON printer for converting protobuf messages to JSON
         */
        public Options {
            if (parser == null) {
                parser = JsonFormat.parser().ignoringUnknownFields();
            }
            if (printer == null) {
                printer = JsonFormat.printer().omittingInsignificantWhitespace().includingDefaultValueFields();
                if (serializeEnumAsInt) {
                    printer = printer.printingEnumsAsInts();
                }
            }
        }

        /**
         * Default options instance with serializeEnumAsInt set to false and default parser/printer.
         */
        public static final Options DEFAULT = Options.builder().build();
    }
}
