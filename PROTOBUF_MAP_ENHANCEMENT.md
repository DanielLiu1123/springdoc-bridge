# Protobuf Map Field Enhancement

## 概述

本次实现为protobuf map类型添加了对message和enum value类型的支持，使OpenAPI schema生成更加准确和完整。

## 实现的功能

### 之前的限制
- protobuf map字段只支持基本类型（String, Integer, Long, Boolean, Double, Float）的value
- 对于复杂类型（message, enum）的value，只是简单地设置`additionalProperties: true`

### 现在的支持
- ✅ **基本类型** - String, Integer, Long, Boolean, Double, Float（已有功能）
- ✅ **Enum类型** - 生成$ref引用到enum schema
- ✅ **Message类型** - 生成$ref引用到message schema
- ✅ **未知类型** - 回退到`additionalProperties: true`

## 代码变更

### 主要修改

1. **`ProtobufWellKnownTypeModelConverter.createMapFieldSchema()`方法**
   - 添加了`ModelConverterContext`参数
   - 新增enum类型处理逻辑
   - 新增message类型处理逻辑

2. **新增测试**
   - `ProtobufMapFieldMessageEnumTest` - 单元测试验证各种map value类型的处理

### 具体实现

```java
// 处理protobuf enum值
if (ProtocolMessageEnum.class.isAssignableFrom(valueClass) && valueClass.isEnum()) {
    Schema<?> enumSchema = createProtobufEnumSchemaWithRef(valueClass, context);
    schema.setAdditionalProperties(enumSchema);
}

// 处理protobuf message值  
else if (Message.class.isAssignableFrom(valueClass)) {
    ProtobufNameResolver nameResolver = new ProtobufNameResolver();
    String messageSchemaName = nameResolver.getNameOfClass(valueClass);
    Schema<?> messageRefSchema = new Schema<>().$ref("#/components/schemas/" + messageSchemaName);
    schema.setAdditionalProperties(messageRefSchema);
}
```

## 示例

### 输入protobuf定义
```protobuf
message TestMessage {
  map<string, string> metadata = 1;           // 基本类型
  map<string, Status> status_map = 2;         // enum类型
  map<string, Address> address_map = 3;       // message类型
  map<string, int32> score_map = 4;           // 基本类型
}

enum Status {
  ACTIVE = 0;
  INACTIVE = 1;
}

message Address {
  string street = 1;
  string city = 2;
}
```

### 生成的OpenAPI Schema
```json
{
  "TestMessage": {
    "type": "object",
    "properties": {
      "metadata": {
        "type": "object",
        "additionalProperties": {
          "type": "string"
        }
      },
      "statusMap": {
        "type": "object", 
        "additionalProperties": {
          "$ref": "#/components/schemas/Status"
        }
      },
      "addressMap": {
        "type": "object",
        "additionalProperties": {
          "$ref": "#/components/schemas/Address"  
        }
      },
      "scoreMap": {
        "type": "object",
        "additionalProperties": {
          "type": "integer",
          "format": "int32"
        }
      }
    }
  }
}
```

## 测试覆盖

- ✅ Map with string values
- ✅ Map with integer values  
- ✅ Map with boolean values
- ✅ Map with enum values
- ✅ Map with message values
- ✅ Map with unknown complex types
- ✅ 回归测试确保现有功能不受影响

## 兼容性

- ✅ 向后兼容 - 现有功能保持不变
- ✅ 所有现有测试通过
- ✅ 遵循OpenAPI 3.x规范
- ✅ 符合protobuf JSON映射规范

## 总结

此次增强使protobuf map字段的OpenAPI文档生成更加准确和完整，特别是对于使用enum和message作为map value的场景。实现遵循了现有的代码模式和最佳实践，确保了高质量和可维护性。
