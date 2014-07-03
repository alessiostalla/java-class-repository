package com.github.alessiostalla.javaclassrepo;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by alessio on 02/07/14.
 */
public class NonExistingResource implements Resource {

    private final ClassProvider provider;

    public NonExistingResource(ClassProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public boolean isNewerThan(long timestamp) {
        return false;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public boolean isClass() {
        return false;
    }

    @Override
    public Class loadClass(ClassRepository repository) {
        return null;
    }

    @Override
    public void close() {}

    @Override
    public ClassProvider getProvider() {
        return provider;
    }
}
