# Springdoc Bridge

[![Maven Central](https://img.shields.io/maven-central/v/io.github.danielliu1123/springdoc-bridge-protobuf)](https://central.sonatype.com/artifact/io.github.danielliu1123/springdoc-bridge-protobuf)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Springdoc Bridge provides integration between [SpringDoc OpenAPI](https://springdoc.org/) and additional data
serialization formats, enabling accurate OpenAPI documentation for APIs using different serialization technologies.

## Overview

SpringDoc OpenAPI handles standard Java objects and JSON APIs. For specialized data formats like Protocol Buffers,
Apache Avro, or MessagePack, Springdoc Bridge provides format-specific modules to ensure proper OpenAPI schema
representation.

## Supported Modules

| Format               | Module                                                     | Description                             |
|----------------------|------------------------------------------------------------|-----------------------------------------|
| **Protocol Buffers** | [`springdoc-bridge-protobuf`](./springdoc-bridge-protobuf) | Protobuf support with Jackson 2.x & 3.x |

## License

MIT License.
