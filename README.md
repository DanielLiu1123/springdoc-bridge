# Springdoc Bridge

[![Maven Central](https://img.shields.io/maven-central/v/io.github.danielliu1123/springdoc-bridge-protobuf)](https://central.sonatype.com/artifact/io.github.danielliu1123/springdoc-bridge-protobuf)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Springdoc Bridge** provides seamless integration between [SpringDoc OpenAPI](https://springdoc.org/) and additional data serialization formats. 
This project is designed to support multiple data formats through dedicated modules, 
enabling developers to generate accurate OpenAPI documentation for APIs that use different serialization technologies.

## 🎯 Project Vision

While SpringDoc OpenAPI excels at documenting standard Java objects and JSON APIs, 
modern applications often use specialized data formats like Protocol Buffers, Apache Avro, MessagePack, and others. 
Springdoc Bridge fills this gap by providing format-specific modules that ensure these data types are properly represented in OpenAPI schemas.

## 📦 Supported Data Formats

| Format               | Module                                                     | Status          | Description               |
|----------------------|------------------------------------------------------------|-----------------|---------------------------|
| **Protocol Buffers** | [`springdoc-bridge-protobuf`](./springdoc-bridge-protobuf) | ✅ **Available** | Complete protobuf support |

## 📄 License

The MIT License.
