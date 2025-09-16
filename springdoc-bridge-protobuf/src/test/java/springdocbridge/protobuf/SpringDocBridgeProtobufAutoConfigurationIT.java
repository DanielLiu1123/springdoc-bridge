package springdocbridge.protobuf;

import static org.assertj.core.api.Assertions.assertThatCode;

import jacksonmodule.protobuf.ProtobufModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

class SpringDocBridgeProtobufAutoConfigurationIT {

    @Test
    @DisplayName("Should auto-configure when all conditions are met")
    void shouldAutoConfigureWhenAllConditionsMet() {
        try (var ctx = newAppBuilder().run()) {
            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufAutoConfiguration.class))
                    .doesNotThrowAnyException();

            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufAutoConfiguration.class))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(ProtobufModelConverter.class)).doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(ProtobufModule.class)).doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(jacksonmodule.protobuf.v3.ProtobufModule.class))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufProperties.class))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Should not auto-configure when SpringDoc is disabled")
    void shouldNotAutoConfigureWhenSpringDocDisabled() {
        try (var ctx =
                newAppBuilder().properties("springdoc.api-docs.enabled=false").run()) {
            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufAutoConfiguration.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Test
    @DisplayName("Should not auto-configure when protobuf support is disabled")
    void shouldNotAutoConfigureWhenProtobufSupportDisabled() {
        try (var ctx = newAppBuilder()
                .properties("springdoc-bridge.protobuf.enabled=false")
                .run()) {
            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufAutoConfiguration.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Test
    @DisplayName("Should register ProtobufModule with Jackson when enabled")
    void shouldRegisterProtobufModuleWhenEnabled() {
        try (var ctx = newAppBuilder().run()) {
            assertThatCode(() -> ctx.getBean("jacksonProtobufModule")).doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean("jackson3ProtobufModule")).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Should register ProtobufModelConverter")
    void shouldRegisterSpringdocBridgeProtobufModelConverter() {
        try (var ctx = newAppBuilder().run()) {
            assertThatCode(() -> ctx.getBean(ProtobufModelConverter.class)).doesNotThrowAnyException();
        }
    }

    private static SpringApplicationBuilder newAppBuilder() {
        return new SpringApplicationBuilder(TestConfig.class).properties("server.port=0");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestConfig {}
}
