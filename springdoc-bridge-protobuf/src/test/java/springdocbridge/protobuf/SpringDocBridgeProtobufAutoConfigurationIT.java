package springdocbridge.protobuf;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

class SpringDocBridgeProtobufAutoConfigurationIT {

    @Test
    void testDefaultBehavior() {
        try (var ctx = new SpringApplicationBuilder(Cfg.class).run()) {
            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufAutoConfiguration.class))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void testDisableSpringDoc() {
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("springdoc.api-docs.enabled=false")
                .run()) {
            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufAutoConfiguration.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
