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

    /**
     * How protobuf {@code oneof} groups are represented in the generated OpenAPI schema.
     *
     * <p> Given the following message:
     *
     * <pre>{@code
     * message Pet {
     *   string name = 1;
     *   oneof pet_type {
     *     Cat cat = 2;
     *     Dog dog = 3;
     *   }
     * }
     * }</pre>
     *
     * <ul> The generated schema for {@code Pet} will be:
     *  <li> With {@link OneofBehavior#FLATTEN}, all oneof members are emitted as sibling optional
     *       properties ({@code cat} and {@code dog}), losing the mutual-exclusion constraint.
     *  <li> With {@link OneofBehavior#ONE_OF}, each oneof group is emitted as an OpenAPI
     *       {@code oneOf} so that the mutual exclusion is documented.
     * </ul>
     *
     * <p> Default is {@link OneofBehavior#FLATTEN} to keep backward compatibility.
     *
     * @since 0.4.0
     * @see <a href="https://github.com/DanielLiu1123/springdoc-bridge/issues/23">Issue #23</a>
     */
    private OneofBehavior oneofBehavior = OneofBehavior.FLATTEN;

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

    /**
     * Strategy for representing protobuf {@code oneof} groups in OpenAPI.
     *
     * @since 0.4.0
     */
    public enum OneofBehavior {
        /**
         * Emit every oneof member as a sibling optional property.
         *
         * <p> This is the historical behavior: the mutual exclusion between oneof members is not
         * represented in the schema, but the output stays flat and simple.
         */
        FLATTEN,
        /**
         * Emit each oneof group as an OpenAPI {@code oneOf}.
         *
         * <p> Every member becomes a branch that requires exactly that field. The regular
         * (non-oneof) fields and the oneof groups are composed under a single {@code allOf}
         * (so a message may contain multiple oneof groups, and renderers keep the common fields
         * visible next to the oneof variants). This documents the mutual exclusion between members.
         *
         * <p> Note: protobuf allows a oneof to be entirely unset ("at most one"), whereas OpenAPI
         * {@code oneOf} requires exactly one branch to match. The generated schema therefore models
         * the slightly stricter "exactly one" contract; it is meant as documentation of intent
         * rather than a strict validation contract.
         *
         * <p> Synthetic oneofs (used to implement proto3 {@code optional} fields) are not affected
         * and continue to be treated as regular optional properties.
         */
        ONE_OF
    }
}
