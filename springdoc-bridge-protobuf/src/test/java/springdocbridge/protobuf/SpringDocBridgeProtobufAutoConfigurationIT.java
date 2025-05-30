package springdocbridge.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import jacksonmodule.protobuf.ProtobufModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

class SpringDocBridgeProtobufAutoConfigurationIT {

    @Test
    @DisplayName("Should auto-configure when all conditions are met")
    void shouldAutoConfigureWhenAllConditionsMet() {
        try (var ctx = new SpringApplicationBuilder(TestConfig.class).run()) {
            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufAutoConfiguration.class))
                    .doesNotThrowAnyException();

            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufAutoConfiguration.class))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(ProtobufWellKnownTypeModelConverter.class))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(Jackson2ObjectMapperBuilderCustomizer.class))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufProperties.class))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Should not auto-configure when SpringDoc is disabled")
    void shouldNotAutoConfigureWhenSpringDocDisabled() {
        try (var ctx = new SpringApplicationBuilder(TestConfig.class)
                .properties("springdoc.api-docs.enabled=false")
                .run()) {
            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufAutoConfiguration.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Test
    @DisplayName("Should not auto-configure when protobuf support is disabled")
    void shouldNotAutoConfigureWhenProtobufSupportDisabled() {
        try (var ctx = new SpringApplicationBuilder(TestConfig.class)
                .properties("springdoc-bridge.protobuf.enabled=false")
                .run()) {
            assertThatCode(() -> ctx.getBean(SpringDocBridgeProtobufAutoConfiguration.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Test
    @DisplayName("Should register ProtobufModule with Jackson when enabled")
    void shouldRegisterProtobufModuleWhenEnabled() {
        try (var ctx = new SpringApplicationBuilder(TestConfig.class).run()) {
            assertThatCode(() -> ctx.getBean("springDocBridgeProtobufJackson2ObjectMapperBuilderCustomizer"))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Should not register ProtobufModule when disabled")
    void shouldNotRegisterProtobufModuleWhenDisabled() {
        try (var ctx = new SpringApplicationBuilder(TestConfig.class)
                .properties("springdoc-bridge.protobuf.register-protobuf-module=false")
                .run()) {
            assertThatCode(() -> ctx.getBean("springDocBridgeProtobufJackson2ObjectMapperBuilderCustomizer"))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Test
    @DisplayName("Should register ProtobufWellKnownTypeModelConverter")
    void shouldRegisterProtobufWellKnownTypeModelConverter() {
        try (var ctx = new SpringApplicationBuilder(TestConfig.class).run()) {
            assertThatCode(() -> ctx.getBean(ProtobufWellKnownTypeModelConverter.class))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Should register ProtobufSchemaModule with ObjectMapper")
    void shouldRegisterProtobufSchemaModuleWithObjectMapper() {
        try (var ctx = new SpringApplicationBuilder(TestConfig.class).run()) {
            var objectMapperProvider = ctx.getBean(ObjectMapperProvider.class);
            var objectMapper = objectMapperProvider.jsonMapper();

            // Verify ProtobufSchemaModule is registered
            var registeredModules = objectMapper.getRegisteredModuleIds();
            assertThat(registeredModules).contains(ProtobufSchemaModule.class.getName());
        }
    }

    @Test
    @DisplayName("Should have ProtobufModule registered when enabled")
    void shouldHaveProtobufModuleRegisteredWhenEnabled() {
        try (var ctx = new SpringApplicationBuilder(TestConfig.class).run()) {
            var objectMapperProvider = ctx.getBean(ObjectMapperProvider.class);
            var objectMapper = objectMapperProvider.jsonMapper();

            // Verify ProtobufModule is registered
            var registeredModules = objectMapper.getRegisteredModuleIds();
            assertThat(registeredModules).contains(ProtobufModule.class.getName());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestConfig {}
}
