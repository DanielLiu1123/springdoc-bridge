package springdocsbridge.protobuf;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.introspect.BasicBeanDescription;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
@SuppressFBWarnings("SE_BAD_FIELD")
final class ProtobufClassIntrospector extends BasicClassIntrospector {

    private final Map<Class<?>, Map<String, Descriptors.FieldDescriptor>> cache = new ConcurrentHashMap<>();

    @Override
    public BasicBeanDescription forDeserialization(DeserializationConfig cfg, JavaType type, MixInResolver r) {
        BasicBeanDescription desc = super.forDeserialization(cfg, type, r);

        if (Message.class.isAssignableFrom(type.getRawClass())) {
            return filterBeanDescriptionForProtobuf(cfg, type, r, desc);
        }

        return desc;
    }

    @Override
    public BasicBeanDescription forSerialization(SerializationConfig cfg, JavaType type, MixInResolver r) {
        BasicBeanDescription desc = super.forSerialization(cfg, type, r);

        if (Message.class.isAssignableFrom(type.getRawClass())) {
            return filterBeanDescriptionForProtobuf(cfg, type, r, desc);
        }

        return desc;
    }

    private BasicBeanDescription filterBeanDescriptionForProtobuf(
            MapperConfig<?> cfg, JavaType type, MixInResolver r, BasicBeanDescription baseDesc) {
        var types = cache.computeIfAbsent(type.getRawClass(), ProtobufClassIntrospector::getDescriptorForType);

        var annotatedClass = AnnotatedClassResolver.resolve(cfg, type, r);

        List<BeanPropertyDefinition> props = new ArrayList<>();

        var properties = baseDesc.findProperties();
        for (BeanPropertyDefinition p : properties) {
            String name = p.getName();
            if (!types.containsKey(name)) {
                continue;
            }

            Descriptors.FieldDescriptor fieldDescriptor = types.get(name);

            if (p.hasField()
                    && p.getField().getType().isJavaLangObject()
                    && fieldDescriptor.getType().equals(Descriptors.FieldDescriptor.Type.STRING)) {
                addStringFormatAnnotation(p);
            }

            props.add(p.withSimpleName(name));
        }

        return new BasicBeanDescription(cfg, type, annotatedClass, new ArrayList<>(props)) {};
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private static class StringFormatAnnotationHelper {}

    private static void addStringFormatAnnotation(BeanPropertyDefinition p) {
        JsonFormat annotation = StringFormatAnnotationHelper.class.getAnnotation(JsonFormat.class);
        if (annotation != null) {
            var field = p.getField();
            if (field != null) {
                field.getAllAnnotations().addIfNotPresent(annotation);
            }
        }
    }

    private static Map<String, Descriptors.FieldDescriptor> getDescriptorForType(Class<?> type) {
        var getDescriptorMethod = ReflectionUtils.findMethod(type, "getDescriptor");
        if (getDescriptorMethod == null) {
            throw new IllegalStateException("No getDescriptor method found for class " + type);
        }
        Descriptors.Descriptor descriptor =
                (Descriptors.Descriptor) ReflectionUtils.invokeMethod(getDescriptorMethod, null);
        if (descriptor == null) {
            throw new IllegalStateException("No descriptor found for class " + type);
        }

        var descriptorsForType = new HashMap<String, Descriptors.FieldDescriptor>();
        for (Descriptors.FieldDescriptor fieldDescriptor : descriptor.getFields()) {
            descriptorsForType.put(fieldDescriptor.getName(), fieldDescriptor);
            descriptorsForType.put(fieldDescriptor.getJsonName(), fieldDescriptor);
        }
        return descriptorsForType;
    }
}
