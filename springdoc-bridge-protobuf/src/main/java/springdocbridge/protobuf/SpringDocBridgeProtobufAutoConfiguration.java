package springdocbridge.protobuf;

import com.google.protobuf.util.JsonFormat;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(com.fasterxml.jackson.databind.Module.class)
    @ConditionalOnProperty(
            prefix = SpringDocBridgeProtobufProperties.PREFIX,
            name = "register-protobuf-module",
            matchIfMissing = true)
    static class Jackson2 {
        @Bean
        @ConditionalOnMissingBean
        public jacksonmodule.protobuf.ProtobufModule jacksonProtobufModule() {
            return new jacksonmodule.protobuf.ProtobufModule();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(tools.jackson.databind.JacksonModule.class)
    @ConditionalOnProperty(
            prefix = SpringDocBridgeProtobufProperties.PREFIX,
            name = "register-protobuf-module",
            matchIfMissing = true)
    static class Jackson3 {
        @Bean
        @ConditionalOnMissingBean
        public jacksonmodule.protobuf.v3.ProtobufModule jackson3ProtobufModule() {
            return new jacksonmodule.protobuf.v3.ProtobufModule();
        }
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
