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
@SuppressFBWarnings("SE_BAD_FIELD")
public final class NativeJacksonProtobufModule extends SimpleModule {

    /**
     * The configuration options for this module.
     */
    private final Options options;

    private final JsonSerializer<MessageOrBuilder> messageSerializer;
    private final JsonSerializer<?> enumSerializer;

    /**
     * Creates a new NativeJacksonProtobufModule with default options.
     *
     * @see Options#DEFAULT
     */
    public NativeJacksonProtobufModule() {
        this(Options.DEFAULT);
    }

    /**
     * Creates a new NativeJacksonProtobufModule with the specified options.
     *
     * @param options the configuration options for JSON serialization and deserialization
     */
    public NativeJacksonProtobufModule(Options options) {
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
     * @param serializeEnumAsInt whether to serialize protobuf enums as integers (default: false)
     */
    @Builder(toBuilder = true)
    public record Options(boolean serializeEnumAsInt) {

        /**
         * Default options instance with serializeEnumAsInt set to false.
         */
        public static final Options DEFAULT =
                Options.builder().serializeEnumAsInt(false).build();
    }
}
