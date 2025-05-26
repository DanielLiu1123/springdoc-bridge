package springdocbridge.protobuf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.google.protobuf.Message;

/**
 * @author Freeman
 */
final class ProtobufAnnotationIntrospector extends NopAnnotationIntrospector {

    @Override
    public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac, VisibilityChecker<?> checker) {
        if (Message.class.isAssignableFrom(ac.getRawType())) {
            return checker.withFieldVisibility(JsonAutoDetect.Visibility.ANY);
        }
        return super.findAutoDetectVisibility(ac, checker);
    }

    @Override
    public Object findNamingStrategy(AnnotatedClass ac) {
        if (Message.class.isAssignableFrom(ac.getRawType())) {
            return new PropertyNamingStrategies.NamingBase() {
                @Override
                public String translate(String propertyName) {
                    if (propertyName.endsWith("_")) {
                        return propertyName.substring(0, propertyName.length() - 1);
                    }
                    return propertyName;
                }
            };
        }
        return super.findNamingStrategy(ac);
    }
}
