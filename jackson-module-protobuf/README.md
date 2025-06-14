# Jackson Module - Protocol Buffers

**Jackson Module Protobuf** provides comprehensive Jackson serialization and deserialization support for [Protocol Buffers](https://protobuf.dev/) messages and enums, following the official [Protobuf JSON Mapping](https://protobuf.dev/programming-guides/json/) specification.

## 🎯 Features

- **🔄 Bidirectional Conversion**: Serialize protobuf to JSON and deserialize JSON to protobuf
- **📋 Well-Known Types**: Full support for `Timestamp`, `Duration`, `Any`, `Struct`, `Value`, etc.
- **🏷️ Enum Support**: Configurable enum serialization (string names or integer values)
- **⚙️ Flexible Configuration**: Customizable `JsonFormat.Parser` and `JsonFormat.Printer`
- **🚀 High Performance**: Optimized serialization with caching for better performance
- **📖 Spec Compliant**: Follows Google's official protobuf JSON mapping rules

## 🚀 Installation

### Maven

```xml
<dependency>
    <groupId>io.github.danielliu1123</groupId>
    <artifactId>jackson-module-protobuf</artifactId>
    <version>${jackson-module-protobuf.version}</version>
</dependency>
```

### Gradle

```groovy
implementation "io.github.danielliu1123:jackson-module-protobuf:${jacksonModuleProtobufVersion}"
```

## 📖 Usage Examples

### Basic Usage

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import jacksonmodule.protobuf.ProtobufModule;

// Register the module with ObjectMapper
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new ProtobufModule());

// Serialize protobuf message to JSON
User user = User.newBuilder()
    .setUserId("user-123")
    .setUsername("john_doe")
    .setEmail("john@example.com")
    .setStatus(User.UserStatus.ACTIVE)
    .setCreatedAt(Timestamps.now())
    .build();

String json = mapper.writeValueAsString(user);
System.out.println(json);
// Output: {"userId":"user-123","username":"john_doe","email":"john@example.com","status":"ACTIVE","createdAt":"2023-12-07T10:30:00Z"}

// Deserialize JSON back to protobuf message
User restored = mapper.readValue(json, User.class);
```

### Custom Configuration

```java
import com.google.protobuf.util.JsonFormat;
import jacksonmodule.protobuf.ProtobufModule;

// Create custom options
ProtobufModule.Options options = ProtobufModule.Options.builder()
    .parser(JsonFormat.parser()
        .ignoringUnknownFields()
        .usingRecursionLimit(100))
    .printer(JsonFormat.printer()
        .omittingInsignificantWhitespace()
        .includingDefaultValueFields())
    .build();

// Register module with custom options
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new ProtobufModule(options));
```

### Spring Boot Integration

```java
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer protobufCustomizer() {
        return builder -> builder.modules(new ProtobufModule());
    }
}
```

## 🧪 Testing

```bash
./gradlew :jackson-module-protobuf:test
```

## 🔗 Related Links

- [Jackson Documentation](https://github.com/FasterXML/jackson-docs)
- [Protocol Buffers](https://protobuf.dev/)
- [Protobuf JSON Mapping](https://protobuf.dev/programming-guides/json/)
- [SpringDoc Bridge Protobuf](../springdoc-bridge-protobuf) - SpringDoc integration using this module
