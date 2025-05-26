package springdocbridge.protobuf;

import com.google.protobuf.util.JsonFormat;
import jacksondataformat.protobuf.ProtobufModule;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * @author Freeman
 */
@AutoConfiguration(after = SpringDocConfiguration.class)
@ConditionalOnClass({
    Jackson2ObjectMapperBuilderCustomizer.class, // spring-boot-autoconfigure
    SpringDocConfiguration.class, // springdoc-openapi-starter-common
    JsonFormat.class // protobuf-java-util
})
@ConditionalOnBean(SpringDocConfigProperties.class) // springdoc enabled
public class SpringDocBridgeProtobufAutoConfiguration implements InitializingBean {

    private final ObjectMapperProvider objectMapperProvider;

    public SpringDocBridgeProtobufAutoConfiguration(ObjectMapperProvider objectMapperProvider) {
        this.objectMapperProvider = objectMapperProvider;
    }

    /**
     * Make Jackson support protobuf message for serialization and deserialization.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer springDocBridgeProtobufJackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder.modules(new ProtobufModule());
    }

    @Bean
    public ProtobufWellKnownTypeModelConverter protobufWellKnownTypeModelConverter() {
        return new ProtobufWellKnownTypeModelConverter(objectMapperProvider);
    }

    @Override
    public void afterPropertiesSet() {
        var objectMapper = objectMapperProvider.jsonMapper();

        // Make SpringDoc support protobuf message for generating OpenAPI schema.
        objectMapper.registerModules(new ProtobufSchemaModule());
    }
}
