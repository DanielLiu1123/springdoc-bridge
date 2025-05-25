package example;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import test.v1.EnumTypesTest;
import test.v1.ScalarTypesTest;
import test.v1.TestEnum;
import test.v1.WellKnownTypesRequest;
import test.v1.WellKnownTypesResponse;
import test.v1.WellKnownTypesTest;

/**
 * Controller for testing protobuf well-known types and their OpenAPI schema generation
 */
@RestController
@RequestMapping("/test/well-known-types")
@Tag(name = "Well-Known Types Test", description = "APIs for testing protobuf well-known types JSON mapping")
public class WellKnownTypesController {

    @PostMapping("/process")
    @Operation(
            summary = "Process well-known types",
            description = "Tests all protobuf well-known types in request and response")
    public WellKnownTypesResponse processWellKnownTypes(@RequestBody WellKnownTypesRequest request) {
        return WellKnownTypesResponse.getDefaultInstance();
    }

    @PostMapping("/well-known")
    @Operation(
            summary = "Test well-known types",
            description = "Tests timestamp, duration, wrappers, Any, Struct, etc.")
    public WellKnownTypesTest testWellKnownTypes(@RequestBody WellKnownTypesTest request) {
        return WellKnownTypesTest.getDefaultInstance();
    }

    @PostMapping("/scalar-types")
    @Operation(summary = "Test scalar types", description = "Tests int64 as string, bytes as base64, etc.")
    public ScalarTypesTest testScalarTypes(@RequestBody ScalarTypesTest request) {
        return ScalarTypesTest.getDefaultInstance();
    }

    @PostMapping("/enum-types")
    @Operation(summary = "Test enum types", description = "Tests protobuf enums as strings")
    public EnumTypesTest testEnumTypes(@RequestBody EnumTypesTest request) {
        return EnumTypesTest.getDefaultInstance();
    }

    @GetMapping("/enum-values")
    @Operation(summary = "Get enum values", description = "Returns enum values to test enum schema generation")
    public TestEnum[] getEnumValues() {
        return TestEnum.values();
    }
}
