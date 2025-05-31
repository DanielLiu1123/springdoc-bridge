package springdocbridge.protobuf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.google.protobuf.Message;

/**
 * @author Freeman
 */
class ProtobufAnnotationIntrospector extends NopAnnotationIntrospector {

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

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        // Ignore deprecated protobuf Map field getters
        if (isDeprecatedGetterForMap(m)) {
            return true;
        }
        return super.hasIgnoreMarker(m);
    }

    /**
     * Checks if the annotated member is a deprecated protobuf Map field getter.
     *
     * <p> Protobuf generates deprecated getters for Map fields (e.g., getMetadata())
     * alongside the recommended getters (e.g., getMetadataMap()).
     *
     * <p> We should ignore the deprecated ones to prevent the entire Map field
     * from being marked as deprecated in OpenAPI documentation.
     *
     * @see <a href="https://github.com/DanielLiu1123/springdoc-bridge/issues/2">Map field showing as deprecated</a>
     */
    private static boolean isDeprecatedGetterForMap(AnnotatedMember member) {
        if (!member.hasAnnotation(Deprecated.class)) {
            return false;
        }

        Class<?> declaringClass = member.getDeclaringClass();
        if (!Message.class.isAssignableFrom(declaringClass)) {
            return false;
        }

        String memberName = member.getName();
        if (!memberName.startsWith("get")) {
            return false;
        }

        // Check if there's a corresponding non-deprecated Map getter
        String mapMethodName = memberName + "Map";
        try {
            var mapMethod = declaringClass.getMethod(mapMethodName);
            // If the Map method exists and is not deprecated, then the original method
            // is likely the deprecated Map field getter
            return !mapMethod.isAnnotationPresent(Deprecated.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
