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
    /**
     * Customize the naming strategy for protobuf schemas.
     *
     * <p> Example:
     *
     * <pre>{@code
     * package user.v1;
     *
     * option java_package = "com.example.user.v1";
     *
     * message User {
     *   string name = 1;
     * }
     * }</pre>
     *
     * <ul> Generated schema name will be:
     *  <li> {@code User} if SPRINGDOC naming strategy is used, and 'springdoc.use-fqn' is set to false.
     *  <li> {@code com.example.user.v1.User} if SPRINGDOC naming strategy is used, and 'springdoc.use-fqn' is set to true.
     *  <li> {@code user.v1.User} if PROTOBUF naming strategy is used.
     * </ul>
     *
     * <p> Default is {@link SchemaNamingStrategy#SPRINGDOC}.
     *
     * @since 0.3.0
     */
    private SchemaNamingStrategy schemaNamingStrategy = SchemaNamingStrategy.SPRINGDOC;

    public enum SchemaNamingStrategy {
        /**
         * Use Springdoc's naming strategy.
         *
         * <p> By default, Springdoc uses the class simple name as the schema name. Naming conflicts
         * may occur if multiple messages have the same name in different packages.
         *
         * <p> It's recommended to set 'springdoc.use-fqn: true' to avoid naming conflicts.
         */
        SPRINGDOC,
        /**
         * Use protobuf's naming strategy.
         *
         * <p> Protobuf uses the fully qualified name as the schema name.
         */
        PROTOBUF
    }
}
