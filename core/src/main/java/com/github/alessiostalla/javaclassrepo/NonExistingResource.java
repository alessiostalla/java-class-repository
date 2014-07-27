package com.github.alessiostalla.javaclassrepo;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by alessio on 02/07/14.
 */
public class NonExistingResource implements Resource {

    private final ClassProvider provider;
    private final String name;

    public NonExistingResource(ClassProvider provider, String name) {
        this.provider = provider;
        this.name = name;
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
    public Class[] loadClasses(ClassRepository repository) {
        return new Class[0];
    }

    @Override
    public void close() {}

    @Override
    public ClassProvider getProvider() {
        return provider;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + name;
    }
}
