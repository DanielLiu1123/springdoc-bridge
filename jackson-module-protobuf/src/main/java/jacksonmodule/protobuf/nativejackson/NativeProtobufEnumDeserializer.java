package jacksonmodule.protobuf.nativejackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.protobuf.NullValue;
import com.google.protobuf.ProtocolMessageEnum;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Native Jackson deserializer for protobuf enums that doesn't depend on JsonFormat.
 */
@SuppressWarnings("rawtypes")
final class NativeProtobufEnumDeserializer extends JsonDeserializer<ProtocolMessageEnum> {

    private static final String UNRECOGNIZED = "UNRECOGNIZED";

    private final Class clazz;
    private final Map<Integer, Object> numberToEnum;
    private final Map<String, Object> nameToEnum;
    private final Object unrecognizedEnum;

    @SuppressWarnings("unchecked")
    public NativeProtobufEnumDeserializer(Class clazz) {
        this.clazz = clazz;
        this.numberToEnum = getNumberMap(clazz);
        this.nameToEnum = getNameMap(clazz);
        this.unrecognizedEnum = getUnrecognizedEnum(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ProtocolMessageEnum deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        var treeNode = p.readValueAsTree();

        if (treeNode.isValueNode()) {
            if (treeNode instanceof NumericNode numericNode) {
                return (ProtocolMessageEnum) numberToEnum.getOrDefault(numericNode.intValue(), unrecognizedEnum);
            }
            if (treeNode instanceof TextNode textNode) {
                return (ProtocolMessageEnum) nameToEnum.getOrDefault(textNode.textValue(), unrecognizedEnum);
            }
        }

        throw new IllegalArgumentException(
                "Can't deserialize protobuf enum '" + clazz.getSimpleName() + "' from " + treeNode);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ProtocolMessageEnum getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        if (NullValue.class.isAssignableFrom(clazz)) {
            return NullValue.NULL_VALUE;
        }
        return (ProtocolMessageEnum) super.getNullValue(ctxt);
    }

    @SuppressWarnings("unchecked")
    private Object[] getEnumConstants(Class clazz) {
        var enumConstants = clazz.getEnumConstants();
        if (enumConstants == null) {
            throw new IllegalArgumentException("Class " + clazz + " is not an enum");
        }
        return enumConstants;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Object> getNumberMap(Class clazz) {
        var enumConstants = getEnumConstants(clazz);
        Map<Integer, Object> result = new HashMap<>();
        for (var e : enumConstants) {
            ProtocolMessageEnum enumValue = (ProtocolMessageEnum) e;
            Enum<?> enumInstance = (Enum<?>) e;
            if (!Objects.equals(enumInstance.name(), UNRECOGNIZED)) {
                result.put(enumValue.getNumber(), e);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getNameMap(Class clazz) {
        var enumConstants = getEnumConstants(clazz);
        Map<String, Object> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var e : enumConstants) {
            Enum<?> enumInstance = (Enum<?>) e;
            if (!Objects.equals(enumInstance.name(), UNRECOGNIZED)) {
                result.put(enumInstance.name(), e);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object getUnrecognizedEnum(Class clazz) {
        var enumConstants = getEnumConstants(clazz);
        for (var e : enumConstants) {
            Enum<?> enumInstance = (Enum<?>) e;
            if (Objects.equals(enumInstance.name(), UNRECOGNIZED)) {
                return e;
            }
        }
        throw new IllegalStateException("No UNRECOGNIZED enum constant found for class " + clazz);
    }
}
