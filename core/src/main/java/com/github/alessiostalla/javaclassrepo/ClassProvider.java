package com.github.alessiostalla.javaclassrepo;

public interface ClassProvider {
    Resource getResourceForClass(String className);

    Resource getResource(String path);
}
