package jacksonmodule.protobuf.nativejackson;

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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;

/**
 * Native Jackson module that provides comprehensive support for Protocol Buffers (protobuf) message and enum
 * serialization and deserialization without depending on JsonFormat (which uses gson internally).
 *
 * <p>This module automatically registers custom serializers and deserializers for:
 * <ul>
 *   <li>Protobuf messages implementing {@link MessageOrBuilder}</li>
 *   <li>Protobuf enums implementing {@link ProtocolMessageEnum}</li>
 * </ul>
 *
 * <p>Unlike the standard ProtobufModule, this implementation uses pure Jackson APIs
 * to avoid the dual JSON library dependency issue.
 *
 * <p>Usage Example:
 * <pre>{@code
 * // Register the module with ObjectMapper
 * ObjectMapper mapper = new ObjectMapper();
 * mapper.registerModule(new NativeJacksonProtobufModule());
 *
 * // Serialize protobuf message to JSON
 * String json = mapper.writeValueAsString(protoMessage);
 *
 * // Deserialize JSON to protobuf message
 * MyProtoMessage message = mapper.readValue(json, MyProtoMessage.class);
 * }</pre>
 *
 * @author Freeman
 * @since 0.1.0
 */
public class NativeJacksonProtobufModule extends SimpleModule {

    private static final String MODULE_NAME = "NativeJacksonProtobufModule";

    private final Options options;
    private final JsonSerializer<MessageOrBuilder> messageSerializer;
    private final JsonSerializer<?> enumSerializer;

    /**
     * Creates a new module with default options.
     */
    public NativeJacksonProtobufModule() {
        this(Options.defaults());
    }

    /**
     * Creates a new module with the specified options.
     *
     * @param options the configuration options
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public NativeJacksonProtobufModule(Options options) {
        super(MODULE_NAME);
        this.options = options;
        this.messageSerializer = new NativeProtobufMessageSerializer<>(options);
        this.enumSerializer = new NativeProtobufEnumSerializer(options);
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
            public JsonDeserializer<?> findBeanDeserializer(
                    JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
                if (MessageOrBuilder.class.isAssignableFrom(type.getRawClass())) {
                    @SuppressWarnings("unchecked")
                    var clazz = (Class<MessageOrBuilder>) type.getRawClass();
                    return new NativeProtobufMessageDeserializer<>(clazz, options);
                }
                return super.findBeanDeserializer(type, config, beanDesc);
            }

            @Override
            public JsonDeserializer<?> findEnumDeserializer(
                    Class<?> type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
                if (ProtocolMessageEnum.class.isAssignableFrom(type)) {
                    @SuppressWarnings("unchecked")
                    var clazz = (Class) type;
                    return new NativeProtobufEnumDeserializer(clazz);
                }
                return super.findEnumDeserializer(type, config, beanDesc);
            }
        });
    }

    /**
     * Configuration options for the native Jackson protobuf module.
     *
     * @param serializeEnumAsInt whether to serialize protobuf enums as integers (default: false)
     * @param ignoringUnknownFields whether to ignore unknown fields during deserialization (default: true)
     * @param includingDefaultValueFields whether to include fields with default values during serialization (default: true)
     * @param preservingProtoFieldNames whether to preserve original proto field names instead of converting to camelCase (default: false)
     */
    @Builder(toBuilder = true)
    public record Options(
            boolean serializeEnumAsInt,
            boolean ignoringUnknownFields,
            boolean includingDefaultValueFields,
            boolean preservingProtoFieldNames) {

        /**
         * Compact constructor for Options record that initializes default values.
         *
         * @param serializeEnumAsInt whether to serialize protobuf enums as integers
         * @param ignoringUnknownFields whether to ignore unknown fields during deserialization
         * @param includingDefaultValueFields whether to include fields with default values during serialization
         * @param preservingProtoFieldNames whether to preserve original proto field names
         */
        public Options {
            // Default values are handled by the builder defaults
        }

        /**
         * Creates default options.
         *
         * @return default options instance
         */
        public static Options defaults() {
            return Options.builder()
                    .serializeEnumAsInt(false)
                    .ignoringUnknownFields(true)
                    .includingDefaultValueFields(true)
                    .preservingProtoFieldNames(false)
                    .build();
        }

        /**
         * Creates options with enum serialization as integers.
         *
         * @return options with enum serialization as integers
         */
        public static Options withEnumAsInt() {
            return defaults().toBuilder().serializeEnumAsInt(true).build();
        }

        /**
         * Creates options that preserve proto field names.
         *
         * @return options that preserve proto field names
         */
        public static Options withPreservedFieldNames() {
            return defaults().toBuilder().preservingProtoFieldNames(true).build();
        }
    }
}
