# SpringDoc Bridge - Protocol Buffers

[![Maven Central](https://img.shields.io/maven-central/v/io.github.danielliu1123/springdoc-bridge-protobuf)](https://central.sonatype.com/artifact/io.github.danielliu1123/springdoc-bridge-protobuf)

SpringDoc Bridge Protobuf provides integration between [SpringDoc OpenAPI](https://springdoc.org/)
and [Protocol Buffers](https://protobuf.dev/), enabling automatic generation of accurate OpenAPI documentation for APIs
using protobuf messages and enums.

## Features

- Automatic conversion of protobuf messages to OpenAPI schemas
- Full support for well-known types (`Timestamp`, `Duration`, `Any`, `Struct`, etc.)
- Proper enum documentation with value mappings
- Customizable schema naming and serialization options
- Works out-of-the-box with Spring Boot auto-configuration
- Compliant with official [Protobuf JSON Mapping](https://protobuf.dev/programming-guides/json/) specification

## Installation

### Maven

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
<groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
<dependency>
<groupId>io.github.danielliu1123</groupId>
    <artifactId>springdoc-bridge-protobuf</artifactId>
    <version>${springdoc-bridge.version}</version>
</dependency>
```

### Gradle

```groovy
implementation "org.springframework.boot:spring-boot-starter-web"
implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:${springdocVersion}"
implementation "io.github.danielliu1123:springdoc-bridge-protobuf:${springdocBridgeVersion}"
```

## Usage Examples

### Basic Setup

1. **Define Protobuf Messages**

    ```protobuf
    syntax = "proto3";
    
    package user.v1;
    
    import "google/protobuf/timestamp.proto";
    
    option java_multiple_files = true;
    option java_package = "com.example.user.v1";
    
    message User {
      string user_id = 1;
      string username = 2;
      string email = 3;
      UserStatus status = 4;
      google.protobuf.Timestamp created_at = 5;
      repeated string tags = 6;
    
      enum UserStatus {
        USER_STATUS_UNSPECIFIED = 0;
        ACTIVE = 1;
        INACTIVE = 2;
        SUSPENDED = 3;
      }
    }
    ```

2. **Create REST Controller**

    ```java
    
    @RestController
    @RequestMapping("/api/v1/users")
    public class UserController {
        @GetMapping("/{userId}")
        public User getUser(@PathVariable("userId") String userId) {
            // ...
            return User.getDefaultInstance();
        }
    }
    ```

3. **Access Documentation**

    Start your application and visit:
    
    - **Swagger UI**: `http://localhost:8080/swagger-ui.html`
    - **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### Advanced Usage

#### Custom Protobuf Serialization/Deserialization

`ProtobufModule`
is [auto-registered by default](../springdoc-bridge-protobuf/src/main/java/springdocbridge/protobuf/SpringDocBridgeProtobufAutoConfiguration.java),
but you can customize it if needed.

First, disable auto-registration:

```yaml
springdoc-bridge:
  protobuf:
    register-protobuf-module: false
```

Then, register it with custom options:

```java
import jacksonmodule.protobuf.ProtobufModule; // For Jackson 2.x
// import jacksonmodule.protobuf.v3.ProtobufModule; // For Jackson 3.x

@Configuration(proxyBeanMethods = false)
public class ProtobufConfig {
    @Bean
    public ProtobufModule jacksonProtobufModule() {
        return new ProtobufModule(ProtobufModule.Options.builder()
                .serializeEnumAsInt(true) // Serialize enums as integers
                .build());
    }
}
```

For more details, refer
to [jackson-module-protobuf#custom-configuration](../jackson-module-protobuf/README.md#custom-configuration)

## Configuration

### Configuration Properties

| Property                                             | Type      | Default     | Description                                       |
|------------------------------------------------------|-----------|-------------|---------------------------------------------------|
| `springdoc-bridge.protobuf.enabled`                  | `boolean` | `true`      | Enable or disable protobuf support                |
| `springdoc-bridge.protobuf.register-protobuf-module` | `boolean` | `true`      | Auto-register Jackson ProtobufModule              |
| `springdoc-bridge.protobuf.schema-naming-strategy`   | `enum`    | `SPRINGDOC` | Schema naming strategy (`SPRINGDOC`, `PROTOBUF`)  |
| `springdoc-bridge.protobuf.oneof-behavior`           | `enum`    | `FLATTEN`   | How `oneof` groups are rendered (`FLATTEN`, `ONE_OF`) |

### Schema Naming Strategies

- **`SPRINGDOC`**: Uses SpringDoc's default naming (respects `springdoc.use-fqn` setting)
- **`PROTOBUF`**: Uses protobuf's full type name (e.g., `user.v1.User`)

### Oneof Behavior

Controls how protobuf [`oneof`](https://protobuf.dev/programming-guides/proto3/#oneof) groups are represented in the generated OpenAPI schema.

Given the message:

```protobuf
message Pet {
  string name = 1;
  oneof pet_type {
    Cat cat = 2;
    Dog dog = 3;
  }
}
```

- **`FLATTEN`** (default): every oneof member is emitted as a sibling optional property (`cat` and `dog`). Simple and backward-compatible, but the mutual exclusion is not documented.

  ```yaml
  Pet:
    type: object
    properties:
      name: { type: string }
      cat: { $ref: "#/components/schemas/Cat" }
      dog: { $ref: "#/components/schemas/Dog" }
    required: [ name ]
  ```

- **`ONE_OF`**: each oneof group is emitted as an OpenAPI `oneOf`, documenting the mutual exclusion between members. The regular fields and the oneof groups are composed under a single `allOf` (so a message may contain multiple groups, and renderers keep the common fields visible next to the oneof variants).

  ```yaml
  Pet:
    allOf:
      - type: object
        properties:
          name: { type: string }
        required: [ name ]
      - oneOf:
          - { type: object, properties: { cat: { $ref: "#/components/schemas/Cat" } }, required: [ cat ] }
          - { type: object, properties: { dog: { $ref: "#/components/schemas/Dog" } }, required: [ dog ] }
  ```

  > **Note:** protobuf allows a oneof to be entirely unset ("at most one"), whereas OpenAPI `oneOf` requires exactly one branch to match. The generated schema therefore documents the slightly stricter "exactly one" intent rather than acting as a strict validation contract. Synthetic oneofs used to implement proto3 `optional` fields are unaffected and stay regular optional properties.
  >
  > **Multiple oneof groups:** a message may contain several oneof groups; each becomes its own `oneOf` member under the shared `allOf`, which is the semantically correct "one from each group". Be aware that some renderers (e.g. Swagger UI) merge multiple `oneOf` members into a single combined selector, so the independent groups may not display distinctly even though the schema is correct.

## Testing

```bash
./gradlew :springdoc-bridge-protobuf:test
```

## Examples

- [Protobuf Example](../examples/protobuf) - Full Spring Boot application with protobuf integration
