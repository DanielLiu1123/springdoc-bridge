package jacksonmodule.protobuf.v3;

import java.util.List;
import java.util.Optional;

record Clazz(String fqn, Optional<String> supperClass, List<String> interfaces, int accessFlags) {}
