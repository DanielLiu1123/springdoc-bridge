package springdocsbridge.protobuf;

import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * @author Freeman
 */
@AutoConfiguration
@ConditionalOnClass(
        name = {
            "org.springdoc.core.configuration.SpringDocConfiguration", // springdoc-openapi-starter-common
            "com.google.protobuf.util.JsonFormat" // protobuf-java-util
        })
public class SpringDocsBridgeProtobufAutoConfiguration implements SmartInitializingSingleton {

    private final ObjectMapperProvider objectMapperProvider;
    private final SpringDocConfigProperties springDocConfigProperties;

    public SpringDocsBridgeProtobufAutoConfiguration(
            ObjectMapperProvider objectMapperProvider, SpringDocConfigProperties springDocConfigProperties) {
        this.objectMapperProvider = objectMapperProvider;
        this.springDocConfigProperties = springDocConfigProperties;
    }

    /**
     * Make Jackson support protobuf message.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer springDocsBridgeProtobufJackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder.modules(new ProtobufModule());
    }

    @Override
    public void afterSingletonsInstantiated() {
        var objectMapper = objectMapperProvider.jsonMapper();
        objectMapper.registerModule(new ProtobufModule());
    }
}
