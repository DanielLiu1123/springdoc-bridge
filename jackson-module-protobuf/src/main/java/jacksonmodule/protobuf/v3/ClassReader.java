package jacksonmodule.protobuf.v3;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;

final class ClassReader {

    private record ConstantPoolEntry(int tag, Object value) {}

    static final int CONSTANT_Utf8 = 1;
    static final int CONSTANT_Unicode = 2;
    static final int CONSTANT_Integer = 3;
    static final int CONSTANT_Float = 4;
    static final int CONSTANT_Long = 5;
    static final int CONSTANT_Double = 6;
    static final int CONSTANT_Class = 7;
    static final int CONSTANT_String = 8;
    static final int CONSTANT_Fieldref = 9;
    static final int CONSTANT_Methodref = 10;
    static final int CONSTANT_InterfaceMethodref = 11;
    static final int CONSTANT_NameandType = 12;
    static final int CONSTANT_MethodHandle = 15;
    static final int CONSTANT_MethodType = 16;
    static final int CONSTANT_Dynamic = 17;
    static final int CONSTANT_InvokeDynamic = 18;
    static final int CONSTANT_Module = 19;
    static final int CONSTANT_Package = 20;

    public static Clazz read(InputStream is) {
        var dis = is instanceof DataInputStream d ? d : new DataInputStream(is);
        try {
            // 1. magic number (0xCAFEBABE)
            int magic = dis.readInt();
            if (magic != 0xCAFEBABE) {
                throw new IllegalStateException("Invalid class file format");
            }

            // 2. version
            int minorVersion = dis.readUnsignedShort();
            int majorVersion = dis.readUnsignedShort();

            // 3. constant pool
            int constantPoolCount = dis.readUnsignedShort();
            ConstantPoolEntry[] constantPool = readConstantPool(dis, constantPoolCount);

            // 4. access flags
            int accessFlags = dis.readUnsignedShort();

            // 5. this class
            int thisClass = dis.readUnsignedShort();
            String fqn = resolveClassName(constantPool, thisClass);

            // 6. super class
            int superClass = dis.readUnsignedShort();
            Optional<String> superClassName =
                    superClass == 0 ? Optional.empty() : Optional.of(resolveClassName(constantPool, superClass));

            // 7. interfaces
            var interfaces = new ArrayList<String>();
            int interfacesCount = dis.readUnsignedShort();
            for (int i = 0; i < interfacesCount; i++) {
                int interfaceIndex = dis.readUnsignedShort();
                interfaces.add(resolveClassName(constantPool, interfaceIndex));
            }

            return new Clazz(fqn, superClassName, interfaces, accessFlags);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read class from input stream", e);
        }
    }

    private static String resolveClassName(ConstantPoolEntry[] constantPool, int index) {
        ConstantPoolEntry classInfo = constantPool[index];
        if (classInfo == null || classInfo.tag != CONSTANT_Class) {
            throw new IllegalStateException("Invalid class reference at index " + index);
        }
        int nameIndex = (Integer) classInfo.value;
        ConstantPoolEntry utf8Info = constantPool[nameIndex];
        if (utf8Info == null || utf8Info.tag != CONSTANT_Utf8) {
            throw new IllegalStateException("Invalid UTF8 reference at index " + nameIndex);
        }
        return ((String) utf8Info.value).replace('/', '.');
    }

    private static ConstantPoolEntry[] readConstantPool(DataInputStream dis, int constantPoolCount) throws IOException {
        ConstantPoolEntry[] pool = new ConstantPoolEntry[constantPoolCount];

        for (int i = 1; i < constantPoolCount; i++) {
            int tag = dis.readUnsignedByte();

            switch (tag) {
                case CONSTANT_Utf8 -> {
                    String utf8Value = dis.readUTF();
                    pool[i] = new ConstantPoolEntry(tag, utf8Value);
                }
                case CONSTANT_Integer -> {
                    int intValue = dis.readInt();
                    pool[i] = new ConstantPoolEntry(tag, intValue);
                }
                case CONSTANT_Float -> {
                    float floatValue = dis.readFloat();
                    pool[i] = new ConstantPoolEntry(tag, floatValue);
                }
                case CONSTANT_Long -> {
                    long longValue = dis.readLong();
                    pool[i] = new ConstantPoolEntry(tag, longValue);
                    i++; // Long 占用两个槽位
                }
                case CONSTANT_Double -> {
                    double doubleValue = dis.readDouble();
                    pool[i] = new ConstantPoolEntry(tag, doubleValue);
                    i++; // Double 占用两个槽位
                }
                case CONSTANT_Class -> {
                    int nameIndex = dis.readUnsignedShort();
                    pool[i] = new ConstantPoolEntry(tag, nameIndex);
                }
                case CONSTANT_String -> {
                    int stringIndex = dis.readUnsignedShort();
                    pool[i] = new ConstantPoolEntry(tag, stringIndex);
                } // 9
                    // 10
                case CONSTANT_Fieldref, CONSTANT_Methodref, CONSTANT_InterfaceMethodref -> {
                    int classIndex = dis.readUnsignedShort();
                    int nameAndTypeIndex = dis.readUnsignedShort();
                    pool[i] = new ConstantPoolEntry(tag, new int[] {classIndex, nameAndTypeIndex});
                }
                case CONSTANT_NameandType -> {
                    int nameIdx = dis.readUnsignedShort();
                    int descriptorIdx = dis.readUnsignedShort();
                    pool[i] = new ConstantPoolEntry(tag, new int[] {nameIdx, descriptorIdx});
                }
                case CONSTANT_MethodHandle -> {
                    int referenceKind = dis.readUnsignedByte();
                    int referenceIndex = dis.readUnsignedShort();
                    pool[i] = new ConstantPoolEntry(tag, new int[] {referenceKind, referenceIndex});
                }
                case CONSTANT_MethodType -> {
                    int descriptorIndex = dis.readUnsignedShort();
                    pool[i] = new ConstantPoolEntry(tag, descriptorIndex);
                } // 17
                case CONSTANT_Dynamic, CONSTANT_InvokeDynamic -> {
                    int bootstrapMethodAttrIndex = dis.readUnsignedShort();
                    int nameAndTypeIdx = dis.readUnsignedShort();
                    pool[i] = new ConstantPoolEntry(tag, new int[] {bootstrapMethodAttrIndex, nameAndTypeIdx});
                } // 19
                case CONSTANT_Module, CONSTANT_Package -> {
                    int moduleNameIndex = dis.readUnsignedShort();
                    pool[i] = new ConstantPoolEntry(tag, moduleNameIndex);
                }
                default -> throw new IllegalStateException("Unknown constant pool tag: " + tag);
            }
        }

        return pool;
    }
}
