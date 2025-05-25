package springdocsbridge.protobuf;

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
public class SpringDocsBridgeProtobufAutoConfiguration {

    /**
     * Make Jackson support protobuf message.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer springDocsBridgeProtobufJackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder.modules(new ProtobufModule());
    }
}
