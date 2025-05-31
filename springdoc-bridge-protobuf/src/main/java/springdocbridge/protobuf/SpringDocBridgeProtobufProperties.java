package springdocbridge.protobuf;

import jacksonmodule.protobuf.ProtobufModule;
import jacksonmodule.protobuf.ProtobufModule.Options;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Freeman
 * @since 0.3.0
 */
@Data
@ConfigurationProperties(SpringDocBridgeProtobufProperties.PREFIX)
public class SpringDocBridgeProtobufProperties {

    public static final String PREFIX = "springdoc-bridge.protobuf";

    /**
     * Whether to enable protobuf support for Springdoc.
     */
    private boolean enabled = true;

    /**
     * Whether to register {@link ProtobufModule} for protobuf serialization and deserialization.
     *
     * <p> Set to {@code false} if you want to manually register the {@link ProtobufModule} with custom
     * {@link Options}.
     */
    private boolean registerProtobufModule = true;
}
