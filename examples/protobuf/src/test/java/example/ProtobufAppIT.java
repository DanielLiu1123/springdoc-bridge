package example;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Integration tests for ProtobufApp OpenAPI documentation generation
 *
 * <p>
 * Test focus:
 * <p> - Correct mapping of Protobuf message types
 * <p> - Handling of well-known types
 * <p> - String mapping of enum types
 * <p> - Schema generation for nested messages
 * <p> - Handling of mixed Java and Protobuf types
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProtobufAppIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("API Documentation Generation Tests")
    class ApiDocsGenerationTest {

        @Test
        @DisplayName("Application starts successfully")
        void applicationStartsSuccessfully() {
            // Verify application has started successfully
            assertThat(port).isGreaterThan(0);
        }

        @Test
        @DisplayName("API docs endpoint is accessible")
        void apiDocsEndpointIsAccessible() {
            // Given
            String url = "http://localhost:" + port + "/v3/api-docs";

            // When
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).contains("openapi");
        }

        @Test
        @DisplayName("Generated API docs match expected content")
        void generatedApiDocsMatchExpected() throws IOException {
            // Given
            String url = "http://localhost:" + port + "/v3/api-docs";

            // When
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode actualApiDocs = objectMapper.readTree(response.getBody());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Verify basic structure
            assertThat(actualApiDocs.get("openapi").asText()).isEqualTo("3.1.0");
            assertThat(actualApiDocs.has("info")).isTrue();
            assertThat(actualApiDocs.has("paths")).isTrue();
            assertThat(actualApiDocs.has("components")).isTrue();

            // Verify key paths exist
            JsonNode paths = actualApiDocs.get("paths");
            assertThat(paths.has("/v1/users")).isTrue();
            assertThat(paths.has("/test/well-known-types/process")).isTrue();

            // Verify components/schemas structure
            JsonNode schemas = actualApiDocs.get("components").get("schemas");
            assertThat(schemas).isNotNull();

            // Verify existence and structure of core schemas
            verifyProtobufSchemas(schemas);
            verifyWellKnownTypesSchemas(schemas);
            verifyJavaProtobufMixedSchemas(schemas);
        }
    }

    @Nested
    @DisplayName("Protobuf Schema Validation Tests")
    class ProtobufSchemaValidationTest {

        @Test
        @DisplayName("User protobuf message schema is generated correctly")
        void userProtobufMessageSchemaIsCorrect() throws IOException {
            // Given
            JsonNode apiDocs = getApiDocs();
            JsonNode userSchema = apiDocs.get("components").get("schemas").get("user.v1.User");

            // Then
            assertThat(userSchema).isNotNull();
            assertThat(userSchema.get("type").asText()).isEqualTo("object");

            JsonNode properties = userSchema.get("properties");
            assertThat(properties).isNotNull();

            // Verify basic fields
            assertThat(properties.has("userId")).isTrue();
            assertThat(properties.has("username")).isTrue();
            assertThat(properties.has("email")).isTrue();

            // Verify enum fields are mapped to strings
            JsonNode genderField = properties.get("gender");
            assertThat(genderField.get("type").asText()).isEqualTo("string");
            assertThat(genderField.has("enum")).isTrue();
            assertThat(genderField.get("enum").toString()).contains("GENDER_UNSPECIFIED", "MALE", "FEMALE", "OTHER");

            // Verify nested message references
            JsonNode addressField = properties.get("address");
            assertThat(addressField.has("$ref")).isTrue();
            assertThat(addressField.get("$ref").asText()).isEqualTo("#/components/schemas/user.v1.User.Address");

            // Verify timestamp fields
            JsonNode createdAtField = properties.get("createdAt");
            assertThat(createdAtField.get("type").asText()).isEqualTo("string");
            assertThat(createdAtField.get("format").asText()).isEqualTo("date-time");
        }

        @Test
        @DisplayName("Nested message schema is generated correctly")
        void nestedMessageSchemaIsCorrect() throws IOException {
            // Given
            JsonNode apiDocs = getApiDocs();
            JsonNode addressSchema = apiDocs.get("components").get("schemas").get("user.v1.User.Address");

            // Then
            assertThat(addressSchema).isNotNull();
            assertThat(addressSchema.get("type").asText()).isEqualTo("object");

            JsonNode properties = addressSchema.get("properties");
            assertThat(properties).isNotNull();
            assertThat(properties.has("street")).isTrue();
            assertThat(properties.has("city")).isTrue();
            assertThat(properties.has("type")).isTrue();

            // Verify nested enums
            JsonNode typeField = properties.get("type");
            assertThat(typeField.get("type").asText()).isEqualTo("string");
            assertThat(typeField.has("enum")).isTrue();
        }
    }

    @Nested
    @DisplayName("Well-Known Types Schema Validation Tests")
    class WellKnownTypesSchemaValidationTest {

        @Test
        @DisplayName("Well-known types schema is generated correctly")
        void wellKnownTypesSchemaIsCorrect() throws IOException {
            // Given
            JsonNode apiDocs = getApiDocs();
            JsonNode wellKnownSchema = apiDocs.get("components").get("schemas").get("test.v1.WellKnownTypesTest");

            // Then
            assertThat(wellKnownSchema).isNotNull();

            JsonNode properties = wellKnownSchema.get("properties");
            assertThat(properties).isNotNull();

            // Verify Timestamp fields
            JsonNode timestampField = properties.get("timestampField");
            assertThat(timestampField.get("type").asText()).isEqualTo("string");
            assertThat(timestampField.get("format").asText()).isEqualTo("date-time");

            // Verify Duration fields
            JsonNode durationField = properties.get("durationField");
            assertThat(durationField.get("type").asText()).isEqualTo("string");
            assertThat(durationField.has("pattern")).isTrue();

            // Verify wrapper types
            JsonNode int64Wrapper = properties.get("int64Wrapper");
            assertThat(int64Wrapper.get("type").asText()).isEqualTo("string");

            JsonNode int32Wrapper = properties.get("int32Wrapper");
            assertThat(int32Wrapper.get("type").asText()).isEqualTo("integer");
            assertThat(int32Wrapper.get("format").asText()).isEqualTo("int32");

            // Verify Struct fields
            JsonNode structField = properties.get("structField");
            assertThat(structField.get("type").asText()).isEqualTo("object");
            assertThat(structField.has("additionalProperties")).isTrue();

            // Verify Any fields
            JsonNode anyField = properties.get("anyField");
            assertThat(anyField.get("type").asText()).isEqualTo("object");
            assertThat(anyField.has("additionalProperties")).isTrue();
        }

        @Test
        @DisplayName("Scalar types schema is generated correctly")
        void scalarTypesSchemaIsCorrect() throws IOException {
            // Given
            JsonNode apiDocs = getApiDocs();
            JsonNode scalarSchema = apiDocs.get("components").get("schemas").get("test.v1.ScalarTypesTest");

            // Then
            assertThat(scalarSchema).isNotNull();

            JsonNode properties = scalarSchema.get("properties");
            assertThat(properties).isNotNull();

            // Verify 64-bit integers are mapped to strings
            JsonNode int64Field = properties.get("int64Field");
            assertThat(int64Field.get("type").asText()).isIn("integer", "string");

            // Verify 32-bit integers
            JsonNode int32Field = properties.get("int32Field");
            assertThat(int32Field.get("type").asText()).isEqualTo("integer");
            assertThat(int32Field.get("format").asText()).isEqualTo("int32");

            // Verify bytes fields
            JsonNode bytesField = properties.get("bytesField");
            assertThat(bytesField.get("type").asText()).isEqualTo("string");
            assertThat(bytesField.get("format").asText()).isEqualTo("byte");
        }
    }

    @Nested
    @DisplayName("Java and Protobuf Mixed Types Validation Tests")
    class JavaProtobufMixedTypesValidationTest {

        @Test
        @DisplayName("Java POJO with protobuf fields schema is generated correctly")
        void javaPojoWithProtobufFieldsSchemaIsCorrect() throws IOException {
            // Given
            JsonNode apiDocs = getApiDocs();
            JsonNode userWrapperSchema =
                    apiDocs.get("components").get("schemas").get("example.UserController.UserWrapper");

            // Then
            assertThat(userWrapperSchema).isNotNull();

            JsonNode properties = userWrapperSchema.get("properties");
            assertThat(properties).isNotNull();

            // Verify Java fields
            assertThat(properties.has("wrapperName")).isTrue();
            assertThat(properties.has("createdAt")).isTrue();
            assertThat(properties.has("isActive")).isTrue();

            // Verify protobuf field references
            JsonNode userField = properties.get("user");
            assertThat(userField.has("$ref")).isTrue();
            assertThat(userField.get("$ref").asText()).isEqualTo("#/components/schemas/user.v1.User");

            // Verify Java LocalDateTime mapping
            JsonNode createdAtField = properties.get("createdAt");
            assertThat(createdAtField.get("type").asText()).isEqualTo("string");
            assertThat(createdAtField.get("format").asText()).isEqualTo("date-time");
        }

        @Test
        @DisplayName("Complex nested structure schema is generated correctly")
        void complexNestedStructureSchemaIsCorrect() throws IOException {
            // Given
            JsonNode apiDocs = getApiDocs();
            JsonNode nestedSchema =
                    apiDocs.get("components").get("schemas").get("example.UserController.NestedComplexStructure");

            // Then
            assertThat(nestedSchema).isNotNull();

            JsonNode properties = nestedSchema.get("properties");
            assertThat(properties).isNotNull();

            // Verify nested references
            assertThat(properties.has("userWrapper")).isTrue();
            assertThat(properties.has("contactInfoList")).isTrue();
            assertThat(properties.has("locationMap")).isTrue();
            assertThat(properties.has("nestedLevel1")).isTrue();

            // Verify array types
            JsonNode contactInfoList = properties.get("contactInfoList");
            assertThat(contactInfoList.get("type").asText()).isEqualTo("array");
            assertThat(contactInfoList.get("items").has("$ref")).isTrue();

            // Verify Map types
            JsonNode locationMap = properties.get("locationMap");
            assertThat(locationMap.get("type").asText()).isEqualTo("object");
            assertThat(locationMap.get("additionalProperties").has("$ref")).isTrue();
        }
    }

    // Helper methods
    private JsonNode getApiDocs() throws IOException {
        String url = "http://localhost:" + port + "/v3/api-docs";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return objectMapper.readTree(response.getBody());
    }

    private void verifyProtobufSchemas(JsonNode schemas) {
        // Verify core protobuf schemas exist
        assertThat(schemas.has("user.v1.User")).isTrue();
        assertThat(schemas.has("user.v1.User.Address")).isTrue();
        assertThat(schemas.has("user.v1.User.PhoneNumber")).isTrue();
        assertThat(schemas.has("user.v1.CreateUserRequest")).isTrue();
        assertThat(schemas.has("user.v1.CreateUserResponse")).isTrue();
    }

    private void verifyWellKnownTypesSchemas(JsonNode schemas) {
        // Verify well-known types schemas exist
        assertThat(schemas.has("test.v1.WellKnownTypesTest")).isTrue();
        assertThat(schemas.has("test.v1.ScalarTypesTest")).isTrue();
        assertThat(schemas.has("test.v1.EnumTypesTest")).isTrue();
    }

    private void verifyJavaProtobufMixedSchemas(JsonNode schemas) {
        // Verify Java and protobuf mixed type schemas exist
        assertThat(schemas.has("example.UserController.UserWrapper")).isTrue();
        assertThat(schemas.has("example.UserController.ContactInfo")).isTrue();
        assertThat(schemas.has("example.UserController.LocationInfo")).isTrue();
        assertThat(schemas.has("example.UserController.NestedComplexStructure")).isTrue();
    }
}
