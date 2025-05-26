package springdocbridge.protobuf;

import com.fasterxml.jackson.databind.module.SimpleModule;

final class ProtobufSchemaModule extends SimpleModule {
    @Override
    public void setupModule(SetupContext context) {
        context.setClassIntrospector(new ProtobufClassIntrospector());
        context.insertAnnotationIntrospector(new ProtobufAnnotationIntrospector());
    }
}
