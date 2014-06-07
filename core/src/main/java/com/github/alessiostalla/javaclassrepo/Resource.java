package com.github.alessiostalla.javaclassrepo;

import java.io.InputStream;

public interface Resource {

    boolean exists();

    boolean isNewerThan(long timestamp);

    InputStream getInputStream();

    boolean isClass();

    Class loadClass(ClassRepository repository);

}
