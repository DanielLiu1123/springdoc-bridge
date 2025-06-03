package springdocbridge.protobuf;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import io.swagger.v3.core.jackson.TypeNameResolver;
import jakarta.annotation.Nullable;
import org.springframework.util.ReflectionUtils;
import springdocbridge.protobuf.SpringDocBridgeProtobufProperties.SchemaNamingStrategy;

/**
 * Custom type name resolver for protobuf messages that supports different naming strategies.
 *
 * <p>This resolver can use either SpringDoc's default naming strategy or protobuf's full name
 * based on the configured {@link SchemaNamingStrategy}.
 *
 * @author Freeman
 */
public class ProtobufNameResolver extends TypeNameResolver {

    private final SchemaNamingStrategy schemaNamingStrategy;

    public ProtobufNameResolver(SchemaNamingStrategy schemaNamingStrategy, boolean useFqn) {
        this.schemaNamingStrategy = schemaNamingStrategy;
        setUseFqn(useFqn);
    }

    @Override
    public String getNameOfClass(Class<?> cls) {
        if (isProtobufMessage(cls)) {
            var desc = getDescriptor(cls);
            if (desc != null) {
                return switch (schemaNamingStrategy) {
                    case PROTOBUF -> desc.getFullName();
                    case SPRINGDOC -> super.getNameOfClass(cls);
                };
            }
        }

        if (isProtobufEnum(cls)) {
            var desc = getEnumDescriptor(cls);
            if (desc != null) {
                return switch (schemaNamingStrategy) {
                    case PROTOBUF -> desc.getFullName();
                    case SPRINGDOC -> super.getNameOfClass(cls);
                };
            }
        }

        return super.getNameOfClass(cls);
    }

    @Nullable
    static Descriptors.Descriptor getDescriptor(Class<?> cls) {
        if (Message.class.isAssignableFrom(cls)) {
            var m = ReflectionUtils.findMethod(cls, "getDescriptor");
            if (m != null) {
                var result = ReflectionUtils.invokeMethod(m, null);
                if (result instanceof Descriptors.Descriptor desc) {
                    return desc;
                }
            }
        }
        return null;
    }

    @Nullable
    static Descriptors.EnumDescriptor getEnumDescriptor(Class<?> cls) {
        if (isProtobufEnum(cls)) {
            var m = ReflectionUtils.findMethod(cls, "getDescriptor");
            if (m != null) {
                var result = ReflectionUtils.invokeMethod(m, null);
                if (result instanceof Descriptors.EnumDescriptor desc) {
                    return desc;
                }
            }
        }
        return null;
    }

    static boolean isProtobufEnum(Class<?> cls) {
        return ProtocolMessageEnum.class.isAssignableFrom(cls) && cls.isEnum();
    }

    static boolean isProtobufMessage(Class<?> cls) {
        return Message.class.isAssignableFrom(cls);
    }
}
