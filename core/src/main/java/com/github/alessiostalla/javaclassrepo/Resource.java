package com.github.alessiostalla.javaclassrepo;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface Resource extends Closeable {

    boolean exists();

    boolean isNewerThan(long timestamp);

    InputStream getInputStream();

    boolean isClass();

    Class[] loadClasses(ClassRepository repository) throws ClassNotFoundException;

    void close() throws IOException;

    ClassProvider getProvider();

    String getName();
}
