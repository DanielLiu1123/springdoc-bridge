package springdocbridge.protobuf;

import com.google.protobuf.util.JsonFormat;
import jacksonmodule.protobuf.ProtobufModule;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for integrating Protocol Buffers (protobuf) support with SpringDoc OpenAPI.
 *
 * <p>This auto-configuration class automatically configures Jackson and SpringDoc to properly handle
 * protobuf messages and enums when generating OpenAPI documentation. It provides seamless integration
 * between protobuf types and OpenAPI schema generation.
 *
 * <p> Usage Example:
 * <pre>{@code
 * // Simply add the dependency to your Spring Boot application
 * // Auto-configuration will be triggered automatically
 *
 * @RestController
 * public class UserController {
 *
 *     @PostMapping("/users")
 *     public CreateUserResponse createUser(@RequestBody CreateUserRequest request) {
 *         // Your protobuf messages will be properly documented in OpenAPI
 *         return userService.createUser(request);
 *     }
 * }
 * }</pre>
 *
 * @author Freeman
 * @see SpringDocConfiguration
 * @see ProtobufModelConverter
 * @see com.google.protobuf.util.JsonFormat
 * @since 0.1.0
 */
@AutoConfiguration(after = SpringDocConfiguration.class)
@ConditionalOnClass({
    Jackson2ObjectMapperBuilderCustomizer.class, // spring-boot-autoconfigure
    SpringDocConfiguration.class, // springdoc-openapi-starter-common
    JsonFormat.class // protobuf-java-util
})
@ConditionalOnBean(SpringDocConfiguration.class) // springdoc enabled
@ConditionalOnProperty(prefix = SpringDocBridgeProtobufProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(SpringDocBridgeProtobufProperties.class)
public class SpringDocBridgeProtobufAutoConfiguration {

    private final ObjectMapperProvider objectMapperProvider;
    private final SpringDocBridgeProtobufProperties springDocBridgeProtobufProperties;

    public SpringDocBridgeProtobufAutoConfiguration(
            ObjectMapperProvider objectMapperProvider,
            SpringDocBridgeProtobufProperties springDocBridgeProtobufProperties) {
        this.objectMapperProvider = objectMapperProvider;
        this.springDocBridgeProtobufProperties = springDocBridgeProtobufProperties;
    }

    /**
     * This customizer registers the {@link ProtobufModule} with Jackson, enabling automatic
     * serialization and deserialization of protobuf messages and enums using Google's official
     * JSON mapping format.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = SpringDocBridgeProtobufProperties.PREFIX,
            name = "register-protobuf-module",
            matchIfMissing = true)
    public Jackson2ObjectMapperBuilderCustomizer springDocBridgeProtobufJackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder.modules(customizers -> {
            if (customizers.stream().noneMatch(c -> c instanceof ProtobufModule)) {
                customizers.add(new ProtobufModule());
            }
        });
    }

    /**
     * This converter handles special protobuf types like Timestamp, Duration, Any, Struct, etc.,
     * and converts them to appropriate OpenAPI schema representations according to the protobuf
     * JSON mapping specification.
     */
    @Bean
    public ProtobufModelConverter springdocBridgeProtobufModelConverter(
            SpringDocConfigProperties springDocConfigProperties) {
        return new ProtobufModelConverter(
                objectMapperProvider,
                new ProtobufNameResolver(
                        springDocBridgeProtobufProperties.getSchemaNamingStrategy(),
                        springDocConfigProperties.isUseFqn()));
    }
}
