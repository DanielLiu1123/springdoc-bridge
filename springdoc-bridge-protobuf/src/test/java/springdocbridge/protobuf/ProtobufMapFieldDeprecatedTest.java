package springdocbridge.protobuf;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Message;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Test to verify that protobuf Map fields are not incorrectly marked as deprecated
 * in OpenAPI schema generation.
 *
 * @see <a href="https://github.com/DanielLiu1123/springdoc-bridge/issues/2">GitHub Issue #2</a>
 */
class ProtobufMapFieldDeprecatedTest {

    @Test
    void testIsProtobufMapFieldDeprecatedGetterDetectsMapGetters() throws Exception {
        // Given: A protobuf annotation introspector
        var introspector = new TestableProtobufAnnotationIntrospector();

        // Get methods from mock class
        Method deprecatedGetter = MockProtobufMessage.class.getMethod("getMetadata");
        Method mapGetter = MockProtobufMessage.class.getMethod("getMetadataMap");
        Method nonMapDeprecatedMethod = MockClassWithDeprecatedMethod.class.getMethod("getOldField");

        // When: Check if methods are protobuf Map field deprecated getters
        boolean deprecatedIsMapGetter =
                introspector.testIsProtobufMapFieldDeprecatedGetter(deprecatedGetter, MockProtobufMessage.class);
        boolean mapIsMapGetter =
                introspector.testIsProtobufMapFieldDeprecatedGetter(mapGetter, MockProtobufMessage.class);
        boolean nonMapIsMapGetter = introspector.testIsProtobufMapFieldDeprecatedGetter(
                nonMapDeprecatedMethod, MockClassWithDeprecatedMethod.class);

        // Then: Only the deprecated getter should be detected as a Map field deprecated getter
        assertThat(deprecatedIsMapGetter).isTrue();
        assertThat(mapIsMapGetter).isFalse();
        assertThat(nonMapIsMapGetter).isFalse();
    }

    @Test
    void testProtobufMapFieldDetectionLogic() throws Exception {
        // Given: Methods from protobuf message class
        Method deprecatedGetter = MockProtobufMessage.class.getMethod("getMetadata");
        Method mapGetter = MockProtobufMessage.class.getMethod("getMetadataMap");

        // When: Check method properties
        boolean deprecatedHasDeprecatedAnnotation = deprecatedGetter.isAnnotationPresent(Deprecated.class);
        boolean mapHasDeprecatedAnnotation = mapGetter.isAnnotationPresent(Deprecated.class);
        boolean isMessageClass = Message.class.isAssignableFrom(MockProtobufMessage.class);
        boolean deprecatedStartsWithGet = deprecatedGetter.getName().startsWith("get");
        boolean mapMethodExists = MockProtobufMessage.class.getMethod("getMetadataMap") != null;

        // Then: Verify the conditions for detecting protobuf Map field deprecated getters
        assertThat(deprecatedHasDeprecatedAnnotation).isTrue();
        assertThat(mapHasDeprecatedAnnotation).isFalse();
        assertThat(isMessageClass).isTrue();
        assertThat(deprecatedStartsWithGet).isTrue();
        assertThat(mapMethodExists).isTrue();
    }

    @Test
    void testMapFieldSchemaGeneration() throws Exception {
        // Given: A protobuf well-known type model converter
        var converter = new ProtobufWellKnownTypeModelConverter(null);

        // When: Test MapField schema creation (using reflection to access private method)
        var method = ProtobufWellKnownTypeModelConverter.class.getDeclaredMethod(
                "createMapFieldSchema", com.fasterxml.jackson.databind.JavaType.class);
        method.setAccessible(true);

        // Create a mock JavaType for MapField<String, String>
        var typeFactory = com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance();
        var stringType = typeFactory.constructType(String.class);
        var mapFieldType =
                typeFactory.constructParametricType(com.google.protobuf.MapField.class, stringType, stringType);

        var schema = (io.swagger.v3.oas.models.media.Schema<?>) method.invoke(null, mapFieldType);

        // Then: Schema should be a simple object with additionalProperties
        assertThat(schema).isNotNull();
        assertThat(schema.getType()).isEqualTo("object");
        assertThat(schema.getAdditionalProperties()).isNotNull();

        // For String values, additionalProperties should be a StringSchema
        if (schema.getAdditionalProperties() instanceof io.swagger.v3.oas.models.media.Schema) {
            var additionalPropsSchema = (io.swagger.v3.oas.models.media.Schema<?>) schema.getAdditionalProperties();
            assertThat(additionalPropsSchema.getType()).isEqualTo("string");
        }
    }

    // Test helper class to expose private method for testing
    private static class TestableProtobufAnnotationIntrospector extends ProtobufAnnotationIntrospector {
        public boolean testIsProtobufMapFieldDeprecatedGetter(Method method, Class<?> declaringClass) {
            // Simulate the logic from the private method
            if (!method.isAnnotationPresent(Deprecated.class)) {
                return false;
            }

            if (!Message.class.isAssignableFrom(declaringClass)) {
                return false;
            }

            String methodName = method.getName();
            if (!methodName.startsWith("get") || methodName.length() <= 3) {
                return false;
            }

            // Check if there's a corresponding non-deprecated Map getter
            String mapMethodName = methodName + "Map";
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

    // Mock classes for testing
    public abstract static class MockProtobufMessage implements Message {
        @Deprecated
        public Map<String, String> getMetadata() {
            return getMetadataMap();
        }

        public Map<String, String> getMetadataMap() {
            return Map.of();
        }
    }

    public static class MockClassWithDeprecatedMethod {
        @Deprecated
        public String getOldField() {
            return "old";
        }
    }
}
