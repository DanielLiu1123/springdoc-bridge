# Springdoc Bridge

[![Maven Central](https://img.shields.io/maven-central/v/io.github.danielliu1123/springdoc-bridge-protobuf)](https://central.sonatype.com/artifact/io.github.danielliu1123/springdoc-bridge-protobuf)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Springdoc Bridge is a library that provides additional data format support for [springdoc-openapi](https://springdoc.org/).

Currently, it supports following data formats:

- [x] Protobuf

## Quick Start

### 1. Add Dependency

#### Maven
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>${springBootVersion}</version>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdocVersion}</version>
</dependency>
<dependency>
    <groupId>io.github.danielliu1123</groupId>
    <artifactId>springdoc-bridge-protobuf</artifactId>
    <version>latest</version>
</dependency>
```

#### Gradle
```groovy
implementation("org.springframework.boot:spring-boot-starter-web:${springBootVersion}")
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${springdocVersion}")
implementation("io.github.danielliu1123:springdoc-bridge-protobuf:<latest>")
```

### 2. Define Protobuf Messages

```protobuf
syntax = "proto3";

package user.v1;

import "google/protobuf/timestamp.proto";

option java_multiple_files = true;

message User {
  string user_id = 1;
  string username = 2;
  string email = 3;
  Status status = 4;
  google.protobuf.Timestamp created_at = 5;

  enum Status {
    STATUS_UNSPECIFIED = 0;
    ACTIVE = 1;
    INACTIVE = 2;
    SUSPENDED = 3;
  }
}
```

### 3. Create REST Controller

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/{userId}")
    public User getUser(@PathVariable("userId") String userId) {
        return User.newBuilder()
            .setUserId(userId)
            .setUsername("Freeman")
            .setEmail("freeman@example.com")
            .setStatus(User.Status.ACTIVE)
            .build();
    }
}
```

Visit http://localhost:8080/swagger-ui.html to view the generated documentation.

Refer to the [protobuf example](examples/protobuf) for a comprehensive example.

## License

The MIT License.

## Related Links

- [SpringDoc OpenAPI](https://springdoc.org/)
- [Protocol Buffers](https://protobuf.dev/)
- [Protobuf JSON Mapping](https://protobuf.dev/programming-guides/json/)
- [Jackson](https://github.com/FasterXML/jackson)
