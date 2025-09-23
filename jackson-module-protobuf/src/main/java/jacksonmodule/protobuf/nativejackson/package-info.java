/**
 * Native Jackson implementation for Protocol Buffers support that doesn't depend on JsonFormat.
 *
 * <p>This package provides a pure Jackson-based implementation for Protocol Buffers serialization
 * and deserialization, avoiding the dual JSON library dependency issue that occurs when using
 * Google's JsonFormat (which internally uses gson).
 *
 * <p>Key features:
 * <ul>
 *   <li>Pure Jackson implementation - no dependency on JsonFormat/gson</li>
 *   <li>Full protobuf JSON mapping specification compliance</li>
 *   <li>Support for all protobuf field types and well-known types</li>
 *   <li>Configurable serialization options</li>
 *   <li>High performance with direct field access</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper();
 * mapper.registerModule(new NativeJacksonProtobufModule());
 *
 * // Serialize protobuf to JSON
 * String json = mapper.writeValueAsString(protoMessage);
 *
 * // Deserialize JSON to protobuf
 * MyProtoMessage message = mapper.readValue(json, MyProtoMessage.class);
 * }</pre>
 *
 * @author Freeman
 * @since 0.1.0
 */
package jacksonmodule.protobuf.nativejackson;
