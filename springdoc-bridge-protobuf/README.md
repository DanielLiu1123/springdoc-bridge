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

| Property                                             | Type      | Default     | Description                                      |
|------------------------------------------------------|-----------|-------------|--------------------------------------------------|
| `springdoc-bridge.protobuf.enabled`                  | `boolean` | `true`      | Enable or disable protobuf support               |
| `springdoc-bridge.protobuf.register-protobuf-module` | `boolean` | `true`      | Auto-register Jackson ProtobufModule             |
| `springdoc-bridge.protobuf.schema-naming-strategy`   | `enum`    | `SPRINGDOC` | Schema naming strategy (`SPRINGDOC`, `PROTOBUF`) |

### Schema Naming Strategies

- **`SPRINGDOC`**: Uses SpringDoc's default naming (respects `springdoc.use-fqn` setting)
- **`PROTOBUF`**: Uses protobuf's full type name (e.g., `user.v1.User`)

## Testing

```bash
./gradlew :springdoc-bridge-protobuf:test
```

## Examples

- [Protobuf Example](../examples/protobuf) - Full Spring Boot application with protobuf integration
