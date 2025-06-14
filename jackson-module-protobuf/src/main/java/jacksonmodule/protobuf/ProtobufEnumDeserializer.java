package jacksonmodule.protobuf;

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

final class ProtobufEnumDeserializer<T extends Enum<T> & ProtocolMessageEnum> extends JsonDeserializer<T> {

    private static final String UNRECOGNIZED = "UNRECOGNIZED";

    private final Class<T> clazz;
    private final Map<Integer, T> numberToEnum;
    private final Map<String, T> nameToEnum;
    private final T unrecognizedEnum;

    public ProtobufEnumDeserializer(Class<T> clazz) {
        this.clazz = clazz;
        this.numberToEnum = getNumberMap(clazz);
        this.nameToEnum = getNameMap(clazz);
        this.unrecognizedEnum = getUnrecognizedEnum(clazz);
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        var treeNode = p.readValueAsTree();

        if (treeNode.isValueNode()) {
            if (treeNode instanceof NumericNode numericNode) {
                return numberToEnum.getOrDefault(numericNode.intValue(), unrecognizedEnum);
            }
            if (treeNode instanceof TextNode textNode) {
                return nameToEnum.getOrDefault(textNode.textValue(), unrecognizedEnum);
            }
        }

        throw new IllegalArgumentException(
                "Can't deserialize protobuf enum '" + clazz.getSimpleName() + "' from " + treeNode);
    }

    @Override
    public T getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        if (NullValue.class.isAssignableFrom(clazz)) {
            return clazz.cast(NullValue.NULL_VALUE);
        }
        return super.getNullValue(ctxt);
    }

    private T[] getEnumConstants(Class<T> clazz) {
        var enumConstants = clazz.getEnumConstants();
        if (enumConstants == null) {
            throw new IllegalStateException("No enum constants found for class " + clazz);
        }
        return enumConstants;
    }

    private Map<Integer, T> getNumberMap(Class<T> clazz) {
        var enumConstants = getEnumConstants(clazz);
        Map<Integer, T> result = new HashMap<>();
        for (var e : enumConstants) {
            if (!Objects.equals(e.name(), UNRECOGNIZED)) { // UNRECOGNIZED getNumber() will throw exception
                result.put(e.getNumber(), e);
            }
        }
        return result;
    }

    private Map<String, T> getNameMap(Class<T> clazz) {
        var enumConstants = getEnumConstants(clazz);
        Map<String, T> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var e : enumConstants) {
            if (!Objects.equals(e.name(), UNRECOGNIZED)) {
                result.put(e.name(), e);
            }
        }
        return result;
    }

    private T getUnrecognizedEnum(Class<T> clazz) {
        var enumConstants = getEnumConstants(clazz);
        for (var e : enumConstants) {
            if (Objects.equals(e.name(), UNRECOGNIZED)) {
                return e;
            }
        }
        throw new IllegalStateException("No UNRECOGNIZED enum constant found for class " + clazz);
    }
}
