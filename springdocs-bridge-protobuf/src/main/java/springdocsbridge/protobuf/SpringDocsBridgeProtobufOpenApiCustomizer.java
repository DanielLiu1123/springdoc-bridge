package springdocsbridge.protobuf;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.ObjectMapperProvider;

/**
 * @author Freeman
 */
public class SpringDocsBridgeProtobufOpenApiCustomizer implements OpenApiCustomizer {
    private static final Logger log = LoggerFactory.getLogger(SpringDocsBridgeProtobufOpenApiCustomizer.class);

    private final ModelConverters modelConverters;

    public SpringDocsBridgeProtobufOpenApiCustomizer(
            SpringDocConfigProperties springDocConfigProperties, ObjectMapperProvider objectMapperProvider) {
        this.modelConverters = buildModelConverters(springDocConfigProperties, objectMapperProvider);
    }

    @Override
    public void customise(OpenAPI openApi) {}

    private static ModelConverters buildModelConverters(
            SpringDocConfigProperties springDocConfigProperties, ObjectMapperProvider objectMapperProvider) {
        var result = ModelConverters.getInstance(springDocConfigProperties.isOpenapi31());

        var mapper = objectMapperProvider.jsonMapper();
        mapper.registerModules(new ProtobufModule(), new ProtobufPropertiesModule());

        result.addConverter(new ModelResolver(mapper, new ProtobufTypeNameResolver()));
        result.addConverter(new ProtobufModelConverter(objectMapperProvider));

        return result;
    }
}
