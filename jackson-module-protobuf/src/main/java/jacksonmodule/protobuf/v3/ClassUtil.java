package jacksonmodule.protobuf.v3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class ClassUtil {

    public static List<Class<?>> getClasses(List<String> basePackages, Predicate<Clazz> filter) {
        List<Clazz> classNames = new ArrayList<>();
        for (String basePackage : basePackages) {
            classNames.addAll(findClassesInPackage(basePackage));
        }
        List<Class<?>> result = new ArrayList<>();
        for (var clazz : classNames) {
            if (filter.test(clazz)) {
                try {
                    result.add(Class.forName(clazz.fqn()));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Failed to load class: " + clazz.fqn(), e);
                }
            }
        }
        return result;
    }

    public static List<Clazz> findClassesInPackage(String packageName) {
        List<Clazz> classes = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        try {
            var resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    classes.addAll(getClassesInPath(resource.getPath()));
                } else if ("jar".equals(resource.getProtocol())) {
                    classes.addAll(getClassesInJar(resource));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return classes;
    }

    private static List<Clazz> getClassesInPath(String packagePath) {
        List<Clazz> classes = new ArrayList<>();

        File directory = new File(packagePath);
        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(getClassesInPath(file.getAbsolutePath()));
            } else if (file.isFile() && file.getName().endsWith(".class")) {
                Clazz clazz;
                try (var is = new FileInputStream(file)) {
                    clazz = ClassReader.read(is);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to read class file: " + file.getAbsolutePath(), e);
                }
                classes.add(clazz);
            }
        }

        return classes;
    }

    private static List<Clazz> getClassesInJar(URL resource) throws IOException {
        List<Clazz> classes = new ArrayList<>();
        try (JarFile jar = ((JarURLConnection) resource.openConnection()).getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                var en = entries.nextElement();
                if (en.getName().endsWith(".class")) {
                    Clazz clazz;
                    try (var is = jar.getInputStream(en)) {
                        clazz = ClassReader.read(is);
                    }
                    classes.add(clazz);
                }
            }
        }
        return classes;
    }
}
