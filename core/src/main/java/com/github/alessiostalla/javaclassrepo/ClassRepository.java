package com.github.alessiostalla.javaclassrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ClassRepository implements ClassProvider {

    private static final Logger logger = LoggerFactory.getLogger(ClassRepository.class);

    public final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
    public final List<ClassProvider> classProviders = new ArrayList<ClassProvider>();

    protected final Map<String, ClassCacheEntry> classCache = new HashMap<String, ClassCacheEntry>();

    public ClassRepository() {
        this(true);
    }

    public ClassRepository(boolean includeDefaultClassLoaders) {
        if(includeDefaultClassLoaders) {
            classLoaders.add(getClass().getClassLoader());
        }
    }

    public synchronized Class getClass(String className) throws ClassNotFoundException {
        for (ClassLoader classLoader : classLoaders) {
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                logger.debug("Class " + className + " not found in classloader " + classLoader, e);
            }
        }
        long timestamp = System.currentTimeMillis();
        ClassCacheEntry classCacheEntry = classCache.get(className);
        if (classCacheEntry == null) {
            classCacheEntry = loadClass(className, timestamp);
        } else if (classCacheEntry.shouldReload()) {
            classCacheEntry = classCacheEntry.reload(timestamp);
        }
        return classCacheEntry.getLoadedClass();
    }

    public synchronized ClassRepository withClassLoaders(ListOperation<ClassLoader> op) {
        op.execute(classLoaders);
        return this;
    }

    public synchronized ClassRepository withClassProviders(ListOperation<ClassProvider> op) {
        op.execute(classProviders);
        return this;
    }

    public synchronized ClassRepository withClassLoaders(final ClassLoader... classLoaders) {
        return withClassLoaders(new ListOperation<ClassLoader>() {
            @Override
            public void execute(List<ClassLoader> objects) {
                objects.addAll(Arrays.asList(classLoaders));
            }
        });
    }

    public synchronized ClassRepository withClassProviders(final ClassProvider...classProviders) {
        return withClassProviders(new ListOperation<ClassProvider>() {
            @Override
            public void execute(List<ClassProvider> objects) {
                objects.addAll(Arrays.asList(classProviders));
            }
        });
    }

    protected ClassCacheEntry loadClass(String className, long timestamp) throws ClassNotFoundException {
        //TODO record dependencies
        Resource resource = getResourceForClass(className);
        try {
            if (resource.isClass()) {
                Class theClass = resource.loadClass(this);
                return new ClassCacheEntry(className, theClass, timestamp, resource.getProvider());
            }
        } finally {
            try {
                resource.close();
            } catch (IOException e) {
                logger.warn("Could not close resource: " + resource, e);
            }
        }
        return new ClassCacheEntry(className, null, timestamp, this);
    }

    @Override
    public Resource getResourceForClass(String className) {
        for(ClassProvider provider : classProviders) {
            Resource resource = provider.getResourceForClass(className);
            if(resource.isClass()) {
                return resource;
            }
        }
        return new NonExistingResource(this);
    }

    @Override
    public Resource getResource(String path) {
        for(ClassProvider provider : classProviders) {
            Resource resource = provider.getResource(path);
            if(resource.exists()) {
                return resource;
            }
        }
        return new NonExistingResource(this);
    }

    protected class ClassCacheEntry {
        public final String className;
        public final Class loadedClass;
        public final long timestamp;
        public final ClassProvider provider;
        //TODO dependencies

        public ClassCacheEntry(String className, Class loadedClass, long timestamp, ClassProvider provider) {
            this.className = className;
            this.loadedClass = loadedClass;
            this.timestamp = timestamp;
            this.provider = provider;
        }

        public boolean shouldReload() {
            return provider.getResourceForClass(className).isNewerThan(timestamp);
        }

        public ClassCacheEntry reload(long timestamp) throws ClassNotFoundException {
            Class newClass = provider.getResourceForClass(className).loadClass(ClassRepository.this);
            ClassCacheEntry newEntry = new ClassCacheEntry(className, newClass, timestamp, provider);
            classCache.put(className, newEntry);
            return newEntry;
        }

        public Class getLoadedClass() throws ClassNotFoundException {
            if(loadedClass != null) {
                return loadedClass;
            } else {
                throw new ClassNotFoundException("Class " + className + " was cached as not found");
            }
        }
    }

    public ClassLoader asClassLoader() {
        return new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                Class<?> cls = ClassRepository.this.getClass(name);
                if(cls != null) {
                    return cls;
                } else {
                    throw new ClassNotFoundException(name);
                }
            }
        };
    }

}
